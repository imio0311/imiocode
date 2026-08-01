package io.imiocode.llm.provider.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.config.AppConfig;
import io.imiocode.config.ThinkingMode;
import io.imiocode.conversation.AnthropicThinkingMetadata;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
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
import io.imiocode.prompt.ApiPayload;
import io.imiocode.prompt.CacheDirective;
import io.imiocode.prompt.PromptAssembler;
import io.imiocode.prompt.SystemPromptBuilder;
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
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class AnthropicClient implements LlmClient {
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final AppConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SseEventReader eventReader;
    private final HttpErrorMapper errorMapper;
    private final PromptAssembler prompts;
    private final ToolResultJson resultJson;
    private final AnthropicThinkingModeResolver thinkingModeResolver = new AnthropicThinkingModeResolver();
    private final AtomicReference<CompletableFuture<HttpResponse<InputStream>>> activeRequest = new AtomicReference<>();
    private final AtomicReference<InputStream> activeStream = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public AnthropicClient(
            AppConfig config,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            SseEventReader eventReader,
            HttpErrorMapper errorMapper) {
        this(config, httpClient, objectMapper, eventReader, errorMapper, new ToolRegistry());
    }

    public AnthropicClient(
            AppConfig config,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            SseEventReader eventReader,
            HttpErrorMapper errorMapper,
            ToolRegistry tools) {
        this(
                config,
                httpClient,
                objectMapper,
                eventReader,
                errorMapper,
                new PromptAssembler(SystemPromptBuilder.defaults(), tools));
    }

    public AnthropicClient(
            AppConfig config,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            SseEventReader eventReader,
            HttpErrorMapper errorMapper,
            PromptAssembler prompts) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.eventReader = Objects.requireNonNull(eventReader, "eventReader");
        this.errorMapper = Objects.requireNonNull(errorMapper, "errorMapper");
        this.prompts = Objects.requireNonNull(prompts, "prompts");
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
            Set<Integer> toolIndexes = new HashSet<>();
            Set<Integer> thinkingIndexes = new HashSet<>();
            Map<Integer, StringBuilder> signatures = new HashMap<>();
            Map<Integer, String> redacted = new HashMap<>();
            AtomicReference<ChatResponse> finalResponse = new AtomicReference<>();
            try {
                eventReader.read(body, event -> handleEvent(
                        event, assembler, usage, toolIndexes, thinkingIndexes, signatures, redacted, finalResponse));
            } catch (StreamAbort abort) {
                throw abort.exception;
            } finally {
                activeStream.compareAndSet(body, null);
            }
            if (finalResponse.get() == null) {
                throw protocolError("Anthropic 响应流未正常完成", null);
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
        ApiPayload payload = prompts.assembleApiPayload(request);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", config.model());
        root.put("stream", true);
        root.put("max_tokens",
                payload.outputTokenLimit().orElse(config.maxOutputTokens()));
        if (config.thinking().enabled()) {
            ThinkingMode mode = thinkingModeResolver.resolve(config);
            ObjectNode thinking = root.putObject("thinking");
            if (mode == ThinkingMode.ADAPTIVE) {
                thinking.put("type", "adaptive");
                root.putObject("output_config").put("effort", config.thinking().effort().apiValue());
            } else {
                thinking.put("type", "enabled");
                thinking.put("budget_tokens", config.thinking().budgetTokens());
            }
        }
        ObjectNode systemBlock = root.putArray("system")
                .addObject()
                .put("type", "text")
                .put("text", payload.systemPrompt());
        if (payload.cacheIntent().system() == CacheDirective.EPHEMERAL) {
            addEphemeralCacheControl(systemBlock);
        }
        ArrayNode messages = root.putArray("messages");
        String previousRole = null;
        ArrayNode currentContent = null;
        for (ChatMessage message : payload.messages()) {
            String role = message.role() == MessageRole.TOOL
                    ? "user"
                    : message.role().apiValue();
            if (!role.equals(previousRole)) {
                ObjectNode item = messages.addObject();
                item.put("role", role);
                currentContent = item.putArray("content");
                previousRole = role;
            }
            appendContent(currentContent, message);
        }
        ArrayNode definitions = root.putArray("tools");
        for (int index = 0; index < payload.tools().size(); index++) {
            ObjectNode definition = encodeDefinition(payload.tools().get(index));
            if (index == payload.tools().size() - 1
                    && payload.cacheIntent().tools() == CacheDirective.EPHEMERAL) {
                addEphemeralCacheControl(definition);
            }
            definitions.add(definition);
        }
        try {
            return HttpRequest.newBuilder(endpoint("/v1/messages"))
                    .timeout(config.requestTimeout())
                    .header("x-api-key", config.apiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root)))
                    .build();
        } catch (IOException exception) {
            throw protocolError("无法构造 Anthropic 请求", exception);
        }
    }

    private ObjectNode encodeDefinition(ToolDefinition definition) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", definition.name());
        node.put("description", definition.description());
        node.set("input_schema", definition.inputSchema());
        return node;
    }

    private void handleEvent(
            SseEvent event,
            LlmStreamAssembler assembler,
            TokenUsageBuilder usage,
            Set<Integer> toolIndexes,
            Set<Integer> thinkingIndexes,
            Map<Integer, StringBuilder> signatures,
            Map<Integer, String> redacted,
            AtomicReference<ChatResponse> finalResponse) {
        if (event.data().isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(event.data());
            String type = node.path("type").asText(event.event());
            switch (type) {
                case "message_start" -> readUsage(node.path("message").path("usage"), usage);
                case "message_delta" -> {
                    readUsage(node.path("usage"), usage);
                    if ("max_tokens".equals(node.path("delta").path("stop_reason").asText(""))) {
                        throw new StreamAbort(new LlmException(
                                LlmErrorType.OUTPUT_LIMIT,
                                true,
                                null,
                                "Anthropic 已达到输出 token 上限"));
                    }
                }
                case "content_block_start" -> {
                    JsonNode block = node.path("content_block");
                    int index = requireIndex(node);
                    switch (block.path("type").asText()) {
                        case "thinking" -> {
                            thinkingIndexes.add(index);
                            signatures.put(index, new StringBuilder(block.path("signature").asText("")));
                            assembler.startThinking(index);
                        }
                        case "redacted_thinking" -> {
                            thinkingIndexes.add(index);
                            redacted.put(index, requiredText(block, "data"));
                            assembler.startThinking(index);
                        }
                        case "tool_use" -> {
                        toolIndexes.add(index);
                            assembler.startTool(
                                index,
                                requiredText(block, "id"),
                                    requiredText(block, "name"));
                        }
                        default -> {
                            // 文本块无需开始事件。
                        }
                    }
                }
                case "content_block_delta" -> {
                    JsonNode delta = node.path("delta");
                    if ("text_delta".equals(delta.path("type").asText())) {
                        assembler.emitText(delta.path("text").asText());
                    } else if ("thinking_delta".equals(delta.path("type").asText())) {
                        assembler.appendThinking(requireIndex(node), delta.path("thinking").asText(""));
                    } else if ("signature_delta".equals(delta.path("type").asText())) {
                        int index = requireIndex(node);
                        StringBuilder signature = signatures.get(index);
                        if (signature == null) {
                            throw new IllegalArgumentException("签名碎片没有对应 Thinking 块");
                        }
                        signature.append(delta.path("signature").asText(""));
                    } else if ("input_json_delta".equals(delta.path("type").asText())) {
                        int index = requireIndex(node);
                        if (!toolIndexes.contains(index)) {
                            throw new IllegalArgumentException("工具参数碎片没有对应的内容块");
                        }
                        assembler.appendToolArguments(index, delta.path("partial_json").asText(""));
                    }
                }
                case "content_block_stop" -> {
                    int index = requireIndex(node);
                    if (toolIndexes.contains(index)) {
                        assembler.ensureEmptyToolArguments(index);
                        assembler.completeTool(index);
                        toolIndexes.remove(index);
                    } else if (thinkingIndexes.remove(index)) {
                        String redactedData = redacted.remove(index);
                        String signature = signatures.containsKey(index)
                                ? signatures.remove(index).toString()
                                : "";
                        assembler.completeThinking(
                                index, new AnthropicThinkingMetadata(signature, redactedData));
                    }
                }
                case "message_stop" -> finalResponse.set(assembler.complete(usage.build()));
                case "error" -> throw new StreamAbort(protocolError("Anthropic 未能完成本轮响应", null));
                default -> {
                    // ping、消息和内容块生命周期事件无需向上层暴露。
                }
            }
        } catch (LlmException exception) {
            throw new StreamAbort(exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new StreamAbort(protocolError("Anthropic 返回了无效的工具流事件", exception));
        }
    }

    private static void readUsage(JsonNode node, TokenUsageBuilder usage) {
        if (node.path("input_tokens").canConvertToLong()) {
            usage.input(node.path("input_tokens").longValue());
        }
        if (node.path("output_tokens").canConvertToLong()) {
            usage.output(node.path("output_tokens").longValue());
        }
        if (node.path("cache_read_input_tokens").canConvertToLong()) {
            usage.cacheRead(node.path("cache_read_input_tokens").longValue());
        }
        if (node.path("cache_creation_input_tokens").canConvertToLong()) {
            usage.cacheWrite(node.path("cache_creation_input_tokens").longValue());
        }
        JsonNode details = node.path("output_tokens_details");
        if (details.path("thinking_tokens").canConvertToLong()) {
            usage.reasoning(details.path("thinking_tokens").longValue());
        }
    }

    private static int requireIndex(JsonNode node) {
        JsonNode value = node.get("index");
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

    private String readErrorCode(InputStream body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.path("error");
            return error.path("type").asText("") + " "
                    + error.path("code").asText("") + " "
                    + error.path("message").asText("");
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

    private void appendContent(ArrayNode content, ChatMessage message)
            throws LlmException {
        for (MessagePart part : message.parts()) {
            if (part instanceof TextPart text) {
                content.addObject().put("type", "text").put("text", text.text());
            } else if (part instanceof ThinkingPart thinkingPart) {
                if (!(thinkingPart.metadata() instanceof AnthropicThinkingMetadata metadata)) {
                    throw protocolError("Anthropic 历史包含不兼容的 Thinking 元数据", null);
                }
                ObjectNode block = content.addObject();
                if (!metadata.redactedData().isBlank()) {
                    block.put("type", "redacted_thinking");
                    block.put("data", metadata.redactedData());
                } else {
                    block.put("type", "thinking");
                    block.put("thinking", thinkingPart.text());
                    block.put("signature", metadata.signature());
                }
            } else if (part instanceof ToolCallPart callPart) {
                ToolCall call = callPart.call();
                ObjectNode block = content.addObject();
                block.put("type", "tool_use");
                block.put("id", call.id());
                block.put("name", call.name());
                block.set("input", call.arguments());
            } else if (part instanceof ToolResultPart resultPart) {
                ObjectNode block = content.addObject();
                block.put("type", "tool_result");
                block.put("tool_use_id", resultPart.callId());
                block.put("content", resultJson.encodeString(resultPart.result()));
                if (!resultPart.result().success()) {
                    block.put("is_error", true);
                }
            }
        }
    }

    private static void addEphemeralCacheControl(ObjectNode node) {
        node.putObject("cache_control").put("type", "ephemeral");
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
