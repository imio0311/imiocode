package io.imiocode.llm.provider.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.config.AppConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.DeepSeekReasoningMetadata;
import io.imiocode.conversation.MessagePart;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.TextPart;
import io.imiocode.conversation.ThinkingPart;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.llm.LlmStreamAssembler;
import io.imiocode.llm.TokenUsageBuilder;
import io.imiocode.llm.ToolResultJson;
import io.imiocode.llm.transport.HttpErrorMapper;
import io.imiocode.llm.transport.SseEvent;
import io.imiocode.llm.transport.SseEventReader;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class DeepSeekClient implements LlmClient {
    private final AppConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SseEventReader eventReader;
    private final HttpErrorMapper errorMapper;
    private final ToolRegistry tools;
    private final ToolResultJson resultJson;
    private final AtomicReference<CompletableFuture<HttpResponse<InputStream>>> activeRequest = new AtomicReference<>();
    private final AtomicReference<InputStream> activeStream = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public DeepSeekClient(
            AppConfig config,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            SseEventReader eventReader,
            HttpErrorMapper errorMapper) {
        this(config, httpClient, objectMapper, eventReader, errorMapper, new ToolRegistry());
    }

    public DeepSeekClient(
            AppConfig config,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            SseEventReader eventReader,
            HttpErrorMapper errorMapper,
            ToolRegistry tools) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.eventReader = Objects.requireNonNull(eventReader, "eventReader");
        this.errorMapper = Objects.requireNonNull(errorMapper, "errorMapper");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.resultJson = new ToolResultJson(objectMapper, new SecretRedactor(config.apiKey()));
    }

    @Override
    public ChatResponse streamChat(ChatRequest request, LlmEventListener listener) throws LlmException {
        ensureOpen();
        try {
            HttpResponse<InputStream> response = send(buildRequest(request));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                try (InputStream body = response.body()) {
                    throw errorMapper.fromStatus(
                            response.statusCode(), readErrorCode(body), response.headers(), Instant.now());
                }
            }

            InputStream body = response.body();
            activeStream.set(body);
            LlmStreamAssembler assembler = new LlmStreamAssembler(objectMapper, listener);
            TokenUsageBuilder usage = new TokenUsageBuilder();
            Set<Integer> toolIndexes = new TreeSet<>();
            AtomicBoolean reasoningStarted = new AtomicBoolean();
            AtomicBoolean finishReasonSeen = new AtomicBoolean();
            AtomicReference<ChatResponse> finalResponse = new AtomicReference<>();
            try {
                eventReader.read(body, event -> handleEvent(
                        event, assembler, usage, toolIndexes, reasoningStarted, finishReasonSeen, finalResponse));
            } catch (StreamAbort abort) {
                throw abort.exception;
            } finally {
                activeStream.compareAndSet(body, null);
            }
            if (finalResponse.get() == null || !finishReasonSeen.get()) {
                throw protocolError("DeepSeek 响应流未正常完成", null);
            }
            return finalResponse.get();
        } catch (LlmException exception) {
            throw exception;
        } catch (IOException exception) {
            if (closed.get()) {
                throw new LlmException(LlmErrorType.INTERRUPTED, false, null, "模型请求已中断", exception);
            }
            throw errorMapper.fromTransport(exception);
        }
    }

    private HttpResponse<InputStream> send(HttpRequest request) throws LlmException {
        CompletableFuture<HttpResponse<InputStream>> future =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
        activeRequest.set(future);
        try {
            return future.get();
        } catch (CancellationException exception) {
            throw new LlmException(LlmErrorType.INTERRUPTED, false, null, "模型请求已中断", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new LlmException(LlmErrorType.INTERRUPTED, false, null, "模型请求已中断", exception);
        } catch (ExecutionException exception) {
            throw errorMapper.fromTransport(exception.getCause());
        } finally {
            activeRequest.compareAndSet(future, null);
        }
    }

    private HttpRequest buildRequest(ChatRequest request) throws LlmException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", config.model());
        root.put("stream", true);
        root.put("max_tokens", config.maxOutputTokens());
        if (config.thinking().enabled()) {
            root.putObject("thinking").put("type", "enabled");
            root.put("reasoning_effort", config.thinking().effort().apiValue());
        }
        ArrayNode messages = root.putArray("messages");
        for (var reminder : request.reminders()) {
            messages.addObject()
                    .put("role", "system")
                    .put("content", "<system-reminder>\n" + reminder.content() + "\n</system-reminder>");
        }
        for (ChatMessage message : request.messages()) {
            appendMessage(messages, message);
        }
        ArrayNode definitions = root.putArray("tools");
        tools.exportEnabled(request.toolSelection(), this::encodeDefinition).forEach(definitions::add);
        try {
            return HttpRequest.newBuilder(endpoint("/chat/completions"))
                    .timeout(config.requestTimeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root)))
                    .build();
        } catch (IOException exception) {
            throw protocolError("无法构造 DeepSeek 请求", exception);
        }
    }

    private void appendMessage(ArrayNode messages, ChatMessage message) {
        if (message.role() == MessageRole.TOOL) {
            for (MessagePart part : message.parts()) {
                ToolResultPart resultPart = (ToolResultPart) part;
                messages.addObject()
                        .put("role", "tool")
                        .put("tool_call_id", resultPart.callId())
                        .put("content", resultJson.encodeString(resultPart.result()));
            }
            return;
        }
        ObjectNode item = messages.addObject();
        item.put("role", message.role().apiValue());
        if (message.content().isEmpty()) {
            item.putNull("content");
        } else {
            item.put("content", message.content());
        }
        if (message.role() == MessageRole.ASSISTANT) {
            ArrayNode calls = null;
            StringBuilder reasoning = new StringBuilder();
            for (MessagePart part : message.parts()) {
                if (part instanceof ThinkingPart thinkingPart) {
                    if (!(thinkingPart.metadata() instanceof DeepSeekReasoningMetadata)) {
                        throw new IllegalArgumentException("DeepSeek 历史包含不兼容的 Thinking 元数据");
                    }
                    reasoning.append(thinkingPart.text());
                } else if (part instanceof ToolCallPart callPart) {
                    if (calls == null) {
                        calls = item.putArray("tool_calls");
                    }
                    ToolCall call = callPart.call();
                    ObjectNode encoded = calls.addObject();
                    encoded.put("id", call.id());
                    encoded.put("type", "function");
                    ObjectNode function = encoded.putObject("function");
                    function.put("name", call.name());
                    function.put("arguments", call.arguments().toString());
                }
            }
            if (!reasoning.isEmpty()) {
                item.put("reasoning_content", reasoning.toString());
            }
        }
    }

    private ObjectNode encodeDefinition(ToolDefinition definition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "function");
        ObjectNode function = node.putObject("function");
        function.put("name", definition.name());
        function.put("description", definition.description());
        function.set("parameters", definition.inputSchema());
        function.put("strict", false);
        return node;
    }

    private void handleEvent(
            SseEvent event,
            LlmStreamAssembler assembler,
            TokenUsageBuilder usage,
            Set<Integer> toolIndexes,
            AtomicBoolean reasoningStarted,
            AtomicBoolean finishReasonSeen,
            AtomicReference<ChatResponse> finalResponse) {
        if ("[DONE]".equals(event.data())) {
            if (finishReasonSeen.get() && finalResponse.get() == null) {
                try {
                    completeThinkingIfNeeded(assembler, reasoningStarted);
                    for (Integer index : Set.copyOf(toolIndexes)) {
                        assembler.ensureEmptyToolArguments(index);
                        assembler.completeTool(index);
                        toolIndexes.remove(index);
                    }
                    finalResponse.set(assembler.complete(usage.build()));
                } catch (LlmException exception) {
                    throw new StreamAbort(exception);
                }
            }
            return;
        }
        if (event.data().isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(event.data());
            JsonNode error = node.path("error");
            if (!error.isMissingNode() && !error.isNull()) {
                throw new StreamAbort(protocolError("DeepSeek 未能完成本轮响应", null));
            }
            readUsage(node.path("usage"), usage);
            JsonNode choices = node.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                if (!node.path("usage").isMissingNode()) {
                    return;
                }
                throw new StreamAbort(protocolError("DeepSeek 流事件缺少 choices", null));
            }
            JsonNode choice = choices.get(0);
            JsonNode delta = choice.path("delta");
            String reasoningDelta = delta.path("reasoning_content").asText("");
            if (!reasoningDelta.isEmpty()) {
                if (reasoningStarted.compareAndSet(false, true)) {
                    assembler.startThinking(0);
                }
                assembler.appendThinking(0, reasoningDelta);
            }
            String contentDelta = delta.path("content").asText("");
            if (!contentDelta.isEmpty()) {
                completeThinkingIfNeeded(assembler, reasoningStarted);
                assembler.emitText(contentDelta);
            }
            JsonNode toolCalls = delta.path("tool_calls");
            if (!toolCalls.isMissingNode() && !toolCalls.isNull()) {
                if (!toolCalls.isArray()) {
                    throw new IllegalArgumentException("tool_calls 必须是数组");
                }
                completeThinkingIfNeeded(assembler, reasoningStarted);
                for (JsonNode toolCall : toolCalls) {
                    JsonNode index = toolCall.get("index");
                    if (index == null || !index.canConvertToInt() || index.intValue() < 0) {
                        throw new IllegalArgumentException("工具调用缺少有效位置");
                    }
                    JsonNode function = toolCall.path("function");
                    int position = index.intValue();
                    if (toolIndexes.add(position)) {
                        assembler.startTool(
                                position,
                                requireText(toolCall.get("id"), "工具调用缺少 id"),
                                requireText(function.get("name"), "工具调用缺少名称"));
                    }
                    String arguments = textOrNull(function.get("arguments"));
                    if (arguments != null) {
                        assembler.appendToolArguments(position, arguments);
                    }
                }
            }
            JsonNode finishReason = choice.path("finish_reason");
            if (!finishReason.isNull() && !finishReason.isMissingNode()) {
                String reason = finishReason.asText();
                if (!"stop".equals(reason) && !"tool_calls".equals(reason)) {
                    throw new StreamAbort(protocolError("DeepSeek 响应因 " + reason + " 未正常完成", null));
                }
                completeThinkingIfNeeded(assembler, reasoningStarted);
                if ("tool_calls".equals(reason)) {
                    for (Integer index : Set.copyOf(toolIndexes)) {
                        assembler.ensureEmptyToolArguments(index);
                        assembler.completeTool(index);
                        toolIndexes.remove(index);
                    }
                }
                finishReasonSeen.set(true);
            }
        } catch (LlmException exception) {
            throw new StreamAbort(exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new StreamAbort(protocolError("DeepSeek 返回了无效的工具流事件", exception));
        }
    }

    private static void completeThinkingIfNeeded(
            LlmStreamAssembler assembler,
            AtomicBoolean reasoningStarted) throws LlmException {
        if (reasoningStarted.compareAndSet(true, false)) {
            assembler.completeThinking(0, new DeepSeekReasoningMetadata());
        }
    }

    private static void readUsage(JsonNode node, TokenUsageBuilder usage) {
        if (node.path("prompt_tokens").canConvertToLong()) {
            usage.input(node.path("prompt_tokens").longValue());
        }
        if (node.path("completion_tokens").canConvertToLong()) {
            usage.output(node.path("completion_tokens").longValue());
        }
        JsonNode details = node.path("completion_tokens_details");
        if (details.path("reasoning_tokens").canConvertToLong()) {
            usage.reasoning(details.path("reasoning_tokens").longValue());
        }
        JsonNode promptDetails = node.path("prompt_tokens_details");
        if (promptDetails.path("cached_tokens").canConvertToLong()) {
            usage.cacheRead(promptDetails.path("cached_tokens").longValue());
        }
    }

    private static String textOrNull(JsonNode node) {
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    private static String requireText(JsonNode node, String message) {
        String value = textOrNull(node);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String readErrorCode(InputStream body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.path("error");
            return error.path("code").asText(error.path("type").asText(""));
        } catch (IOException exception) {
            return "";
        }
    }

    private URI endpoint(String path) {
        return URI.create(config.baseUri().toString() + path);
    }

    private LlmException protocolError(String message, Throwable cause) {
        return new LlmException(LlmErrorType.PROTOCOL, true, null, message, cause);
    }

    private void ensureOpen() throws LlmException {
        if (closed.get()) {
            throw new LlmException(LlmErrorType.INTERRUPTED, false, null, "LLM 客户端已关闭");
        }
    }

    @Override
    public void cancelActiveRequest() {
        CompletableFuture<HttpResponse<InputStream>> request = activeRequest.getAndSet(null);
        if (request != null) {
            request.cancel(true);
        }
        InputStream stream = activeStream.getAndSet(null);
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
                // 关闭过程无需覆盖原始退出原因。
            }
        }
    }

    @Override
    public void close() {
        closed.set(true);
        cancelActiveRequest();
    }

    private static final class StreamAbort extends RuntimeException {
        private final LlmException exception;

        private StreamAbort(LlmException exception) {
            this.exception = exception;
        }
    }
}
