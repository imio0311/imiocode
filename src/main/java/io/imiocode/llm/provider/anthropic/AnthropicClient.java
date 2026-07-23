package io.imiocode.llm.provider.anthropic;

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
import io.imiocode.conversation.TextPart;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.StreamListener;
import io.imiocode.llm.ToolCallAssembler;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private final ToolRegistry tools;
    private final ToolResultJson resultJson;
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
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.eventReader = Objects.requireNonNull(eventReader, "eventReader");
        this.errorMapper = Objects.requireNonNull(errorMapper, "errorMapper");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.resultJson = new ToolResultJson(objectMapper, new SecretRedactor(config.apiKey()));
    }

    @Override
    public ChatResponse streamChat(ChatRequest request, StreamListener listener) throws LlmException {
        ensureOpen();
        try {
            HttpResponse<InputStream> response = send(buildRequest(request));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                try (InputStream body = response.body()) {
                    throw errorMapper.fromStatus(response.statusCode(), readErrorCode(body));
                }
            }

            InputStream body = response.body();
            activeStream.set(body);
            StringBuilder content = new StringBuilder();
            ToolCallAssembler assembler = new ToolCallAssembler(objectMapper);
            Set<Integer> toolIndexes = new HashSet<>();
            AtomicBoolean completed = new AtomicBoolean();
            try {
                eventReader.read(body,
                        event -> handleEvent(event, listener, content, assembler, toolIndexes, completed));
            } catch (StreamAbort abort) {
                throw abort.exception;
            } finally {
                activeStream.compareAndSet(body, null);
            }
            if (!completed.get()) {
                throw protocolError("Anthropic 响应流未正常完成", null);
            }
            if (!toolIndexes.isEmpty()) {
                throw protocolError("Anthropic 工具内容块未正常结束", null);
            }
            return completedResponse(content, assembler.finish());
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
        ArrayNode messages = root.putArray("messages");
        for (ChatMessage message : request.messages()) {
            ObjectNode item = messages.addObject();
            item.put("role", message.role() == MessageRole.TOOL ? "user" : message.role().apiValue());
            ArrayNode content = item.putArray("content");
            for (MessagePart part : message.parts()) {
                if (part instanceof TextPart text) {
                    content.addObject().put("type", "text").put("text", text.text());
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
        ArrayNode definitions = root.putArray("tools");
        tools.exportEnabled(this::encodeDefinition).forEach(definitions::add);
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
            StreamListener listener,
            StringBuilder content,
            ToolCallAssembler assembler,
            Set<Integer> toolIndexes,
            AtomicBoolean completed) {
        if (event.data().isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(event.data());
            String type = node.path("type").asText(event.event());
            switch (type) {
                case "content_block_start" -> {
                    JsonNode block = node.path("content_block");
                    if ("tool_use".equals(block.path("type").asText())) {
                        int index = requireIndex(node);
                        toolIndexes.add(index);
                        assembler.append(
                                index,
                                requiredText(block, "id"),
                                requiredText(block, "name"),
                                null);
                    }
                }
                case "content_block_delta" -> {
                    JsonNode delta = node.path("delta");
                    if ("text_delta".equals(delta.path("type").asText())) {
                        appendDelta(delta.path("text").asText(), listener, content);
                    } else if ("input_json_delta".equals(delta.path("type").asText())) {
                        int index = requireIndex(node);
                        if (!toolIndexes.contains(index)) {
                            throw new IllegalArgumentException("工具参数碎片没有对应的内容块");
                        }
                        assembler.append(index, null, null, delta.path("partial_json").asText(""));
                    }
                }
                case "content_block_stop" -> {
                    int index = requireIndex(node);
                    if (toolIndexes.contains(index)) {
                        assembler.ensureEmptyArguments(index);
                        toolIndexes.remove(index);
                    }
                }
                case "message_stop" -> completed.set(true);
                case "error" -> throw new StreamAbort(protocolError("Anthropic 未能完成本轮响应", null));
                default -> {
                    // ping、消息和内容块生命周期事件无需向上层暴露。
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new StreamAbort(protocolError("Anthropic 返回了无效的工具流事件", exception));
        }
    }

    private ChatResponse completedResponse(StringBuilder content, List<ToolCall> calls) throws LlmException {
        List<MessagePart> parts = new ArrayList<>();
        if (!content.isEmpty()) {
            parts.add(new TextPart(content.toString()));
        }
        calls.stream().map(ToolCallPart::new).forEach(parts::add);
        if (parts.isEmpty()) {
            throw protocolError("Anthropic 完成响应但没有文本或工具调用", null);
        }
        return new ChatResponse(new ChatMessage(MessageRole.ASSISTANT, parts));
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
            return error.path("type").asText(error.path("code").asText(""));
        } catch (IOException exception) {
            return "";
        }
    }

    private static void appendDelta(String delta, StreamListener listener, StringBuilder content) {
        if (delta != null && !delta.isEmpty()) {
            content.append(delta);
            listener.onTextDelta(delta);
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
    public void close() {
        closed.set(true);
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

    private static final class StreamAbort extends RuntimeException {
        private final LlmException exception;

        private StreamAbort(LlmException exception) {
            this.exception = exception;
        }
    }
}
