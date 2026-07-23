package io.imiocode.llm.provider.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.config.AppConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.StreamListener;
import io.imiocode.llm.transport.HttpErrorMapper;
import io.imiocode.llm.transport.SseEvent;
import io.imiocode.llm.transport.SseEventReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private final AtomicReference<CompletableFuture<HttpResponse<InputStream>>> activeRequest = new AtomicReference<>();
    private final AtomicReference<InputStream> activeStream = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public DeepSeekClient(
            AppConfig config,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            SseEventReader eventReader,
            HttpErrorMapper errorMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.eventReader = eventReader;
        this.errorMapper = errorMapper;
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
            AtomicBoolean done = new AtomicBoolean();
            AtomicBoolean finishReasonSeen = new AtomicBoolean();
            try {
                eventReader.read(body, event -> handleEvent(event, listener, content, done, finishReasonSeen));
            } catch (StreamAbort abort) {
                throw abort.exception;
            } finally {
                activeStream.compareAndSet(body, null);
            }
            if (!done.get() || !finishReasonSeen.get()) {
                throw protocolError("DeepSeek 响应流未正常完成", null);
            }
            if (content.isEmpty()) {
                throw protocolError("DeepSeek 完成响应但未返回文本", null);
            }
            return new ChatResponse(content.toString());
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
            item.put("role", message.role().apiValue());
            item.put("content", message.content());
        }
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

    private void handleEvent(
            SseEvent event,
            StreamListener listener,
            StringBuilder content,
            AtomicBoolean done,
            AtomicBoolean finishReasonSeen) {
        if ("[DONE]".equals(event.data())) {
            done.set(true);
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
            JsonNode choices = node.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new StreamAbort(protocolError("DeepSeek 流事件缺少 choices", null));
            }
            JsonNode choice = choices.get(0);
            appendDelta(choice.path("delta").path("content").asText(), listener, content);
            if (!choice.path("finish_reason").isNull() && !choice.path("finish_reason").isMissingNode()) {
                finishReasonSeen.set(true);
            }
        } catch (IOException exception) {
            throw new StreamAbort(protocolError("DeepSeek 返回了无法解析的流事件", exception));
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
