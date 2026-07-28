package io.imiocode.llm.provider.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.config.AppConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessagePart;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.OpenAiReasoningMetadata;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class OpenAiClient implements LlmClient {
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

    public OpenAiClient(
            AppConfig config,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            SseEventReader eventReader,
            HttpErrorMapper errorMapper) {
        this(config, httpClient, objectMapper, eventReader, errorMapper, new ToolRegistry());
    }

    public OpenAiClient(
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
        HttpRequest httpRequest = buildRequest(request);
        try {
            HttpResponse<InputStream> response = send(httpRequest);
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
            Set<Integer> toolDeltaSeen = new HashSet<>();
            Map<Integer, String> reasoningIds = new HashMap<>();
            Set<Integer> reasoningDeltaSeen = new HashSet<>();
            AtomicReference<ChatResponse> finalResponse = new AtomicReference<>();
            try {
                eventReader.read(body, event -> handleEvent(
                        event, assembler, usage, toolIndexes, toolDeltaSeen,
                        reasoningIds, reasoningDeltaSeen, finalResponse));
            } catch (StreamAbort abort) {
                throw abort.exception;
            } finally {
                activeStream.compareAndSet(body, null);
            }
            if (finalResponse.get() == null) {
                throw protocolError("OpenAI 响应流未正常完成", null);
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
        root.put("max_output_tokens",
                request.outputTokenLimit().orElse(config.maxOutputTokens()));
        if (config.thinking().enabled()) {
            ObjectNode reasoning = root.putObject("reasoning");
            reasoning.put("effort", config.thinking().effort().apiValue());
            reasoning.put("summary", config.thinking().summary().apiValue());
            root.putArray("include").add("reasoning.encrypted_content");
        }
        if (!request.reminders().isEmpty()) {
            String instructions = request.reminders().stream()
                    .map(reminder -> "<system-reminder>\n" + reminder.content() + "\n</system-reminder>")
                    .collect(java.util.stream.Collectors.joining("\n\n"));
            root.put("instructions", instructions);
        }
        ArrayNode input = root.putArray("input");
        for (ChatMessage message : request.messages()) {
            appendMessage(input, message);
        }
        ArrayNode definitions = root.putArray("tools");
        tools.exportEnabled(request.toolSelection(), this::encodeDefinition).forEach(definitions::add);
        try {
            return HttpRequest.newBuilder(endpoint("/v1/responses"))
                    .timeout(config.requestTimeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root)))
                    .build();
        } catch (IOException exception) {
            throw protocolError("无法构造 OpenAI 请求", exception);
        }
    }

    private void appendMessage(ArrayNode input, ChatMessage message) throws LlmException {
        try {
            for (MessagePart part : message.parts()) {
                if (part instanceof TextPart text) {
                    input.addObject()
                            .put("role", message.role().apiValue())
                            .put("content", text.text());
                } else if (part instanceof ThinkingPart thinkingPart) {
                    if (!(thinkingPart.metadata() instanceof OpenAiReasoningMetadata metadata)) {
                        throw protocolError("OpenAI 历史包含不兼容的 Thinking 元数据", null);
                    }
                    ObjectNode reasoning = input.addObject();
                    reasoning.put("type", "reasoning");
                    reasoning.put("id", metadata.itemId());
                    if (!metadata.encryptedContent().isBlank()) {
                        reasoning.put("encrypted_content", metadata.encryptedContent());
                    }
                    if (!thinkingPart.text().isBlank()) {
                        reasoning.putArray("summary").addObject()
                                .put("type", "summary_text")
                                .put("text", thinkingPart.text());
                    }
                } else if (part instanceof ToolCallPart callPart) {
                    ToolCall call = callPart.call();
                    input.addObject()
                            .put("type", "function_call")
                            .put("call_id", call.id())
                            .put("name", call.name())
                            .put("arguments", objectMapper.writeValueAsString(call.arguments()));
                } else if (part instanceof ToolResultPart resultPart) {
                    input.addObject()
                            .put("type", "function_call_output")
                            .put("call_id", resultPart.callId())
                            .put("output", resultJson.encodeString(resultPart.result()));
                }
            }
        } catch (IOException exception) {
            throw protocolError("无法编码 OpenAI 工具消息", exception);
        }
    }

    private ObjectNode encodeDefinition(ToolDefinition definition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "function");
        node.put("name", definition.name());
        node.put("description", definition.description());
        node.set("parameters", definition.inputSchema());
        node.put("strict", false);
        return node;
    }

    private void handleEvent(
            SseEvent event,
            LlmStreamAssembler assembler,
            TokenUsageBuilder usage,
            Set<Integer> toolIndexes,
            Set<Integer> toolDeltaSeen,
            Map<Integer, String> reasoningIds,
            Set<Integer> reasoningDeltaSeen,
            AtomicReference<ChatResponse> finalResponse) {
        if (event.data().isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(event.data());
            String type = node.path("type").asText(event.event());
            switch (type) {
                case "response.output_text.delta" -> assembler.emitText(node.path("delta").asText());
                case "response.output_item.added" -> {
                    JsonNode item = node.path("item");
                    int index = requireIndex(node, "output_index");
                    if ("function_call".equals(item.path("type").asText())) {
                        toolIndexes.add(index);
                        assembler.startTool(index, requiredText(item, "call_id"), requiredText(item, "name"));
                    } else if ("reasoning".equals(item.path("type").asText())) {
                        reasoningIds.put(index, requiredText(item, "id"));
                        assembler.startThinking(index);
                    }
                }
                case "response.reasoning_summary_text.delta" -> {
                    int index = requireIndex(node, "output_index");
                    reasoningDeltaSeen.add(index);
                    assembler.appendThinking(index, node.path("delta").asText(""));
                }
                case "response.reasoning_summary_text.done" -> {
                    int index = requireIndex(node, "output_index");
                    if (!reasoningDeltaSeen.contains(index)) {
                        assembler.appendThinking(index, node.path("text").asText(""));
                    }
                }
                case "response.output_item.done" -> {
                    JsonNode item = node.path("item");
                    int index = requireIndex(node, "output_index");
                    if ("reasoning".equals(item.path("type").asText()) && reasoningIds.containsKey(index)) {
                        String id = item.path("id").asText(reasoningIds.remove(index));
                        assembler.completeThinking(
                                index,
                                new OpenAiReasoningMetadata(id, item.path("encrypted_content").asText("")));
                    }
                }
                case "response.function_call_arguments.delta" -> {
                    int index = requireIndex(node, "output_index");
                    toolDeltaSeen.add(index);
                    assembler.appendToolArguments(index, node.path("delta").asText(""));
                }
                case "response.function_call_arguments.done" -> {
                    int index = requireIndex(node, "output_index");
                    if (!toolDeltaSeen.contains(index)) {
                        assembler.appendToolArguments(index, node.path("arguments").asText(""));
                    }
                    assembler.ensureEmptyToolArguments(index);
                    assembler.completeTool(index);
                    toolIndexes.remove(index);
                }
                case "response.completed" -> {
                    for (Integer index : Set.copyOf(toolIndexes)) {
                        assembler.ensureEmptyToolArguments(index);
                        assembler.completeTool(index);
                        toolIndexes.remove(index);
                    }
                    for (Map.Entry<Integer, String> entry : Map.copyOf(reasoningIds).entrySet()) {
                        assembler.completeThinking(
                                entry.getKey(), new OpenAiReasoningMetadata(entry.getValue(), ""));
                        reasoningIds.remove(entry.getKey());
                    }
                    readUsage(node.path("response").path("usage"), usage);
                    finalResponse.set(assembler.complete(usage.build()));
                }
                case "response.incomplete" -> {
                    String reason = node.path("response")
                            .path("incomplete_details")
                            .path("reason")
                            .asText("");
                    if ("max_output_tokens".equals(reason)) {
                        throw new StreamAbort(outputLimit("OpenAI 已达到输出 token 上限"));
                    }
                    throw abort("OpenAI 未能完成本轮响应");
                }
                case "response.failed", "error" -> throw abort("OpenAI 未能完成本轮响应");
                default -> {
                    // 本章只消费文本和生命周期事件。
                }
            }
        } catch (LlmException exception) {
            throw new StreamAbort(exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new StreamAbort(protocolError("OpenAI 返回了无效的工具流事件", exception));
        }
    }

    private static void readUsage(JsonNode node, TokenUsageBuilder usage) {
        if (node.path("input_tokens").canConvertToLong()) {
            usage.input(node.path("input_tokens").longValue());
        }
        if (node.path("output_tokens").canConvertToLong()) {
            usage.output(node.path("output_tokens").longValue());
        }
        if (node.path("input_tokens_details").path("cached_tokens").canConvertToLong()) {
            usage.cacheRead(node.path("input_tokens_details").path("cached_tokens").longValue());
        }
        if (node.path("output_tokens_details").path("reasoning_tokens").canConvertToLong()) {
            usage.reasoning(node.path("output_tokens_details").path("reasoning_tokens").longValue());
        }
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

    private static int requireIndex(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToInt() || value.intValue() < 0) {
            throw new IllegalArgumentException("工具事件缺少有效位置");
        }
        return value.intValue();
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("");
        if (value.isBlank()) {
            throw new IllegalArgumentException("工具事件缺少 " + field);
        }
        return value;
    }

    private StreamAbort abort(String message) {
        return new StreamAbort(protocolError(message, null));
    }

    private LlmException protocolError(String message, Throwable cause) {
        return new LlmException(LlmErrorType.PROTOCOL, true, null, message, cause);
    }

    private LlmException outputLimit(String message) {
        return new LlmException(LlmErrorType.OUTPUT_LIMIT, true, null, message);
    }

    private URI endpoint(String path) {
        return URI.create(config.baseUri().toString() + path);
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
