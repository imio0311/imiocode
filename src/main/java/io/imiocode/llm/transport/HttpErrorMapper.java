package io.imiocode.llm.transport;

import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpHeaders;
import java.time.Instant;
import java.time.Duration;
import java.util.Locale;

/**
 * 将 HTTP 状态、Provider 错误码和传输异常归一为可安全展示的 LLM 错误。
 *
 * <p>映射结果不包含响应正文，避免上游错误载荷中的凭据或用户数据进入终端。</p>
 */
public final class HttpErrorMapper {
    private final RetryAfterParser retryAfterParser = new RetryAfterParser();

    public LlmException fromStatus(int statusCode, String providerCode) {
        return fromStatus(statusCode, providerCode, HttpHeaders.of(java.util.Map.of(), (a, b) -> true), Instant.now());
    }

    public LlmException fromStatus(
            int statusCode,
            String providerCode,
            HttpHeaders headers,
            Instant now) {
        String normalizedCode = providerCode == null ? "" : providerCode.toLowerCase(Locale.ROOT);
        if (statusCode == 401 || statusCode == 403) {
            return new LlmException(LlmErrorType.AUTHENTICATION, true, statusCode, "认证失败，请检查 API Key");
        }
        if (statusCode == 429) {
            // Retry-After 允许秒数或 HTTP 日期；解析失败时交由通用退避策略决定等待时间。
            Duration retryAfter = headers.firstValue("Retry-After")
                    .flatMap(value -> retryAfterParser.parse(value, now))
                    .orElse(null);
            return new LlmException(
                    LlmErrorType.RATE_LIMIT, true, statusCode, "请求受限，请稍后重试", retryAfter, null);
        }
        if ((statusCode == 400 || statusCode == 413) && isContextLimit(normalizedCode)) {
            return new LlmException(
                    LlmErrorType.CONTEXT_LIMIT,
                    true,
                    statusCode,
                    "输入上下文超过模型窗口限制");
        }
        if (normalizedCode.contains("model") && (normalizedCode.contains("not_found") || normalizedCode.contains("not found"))) {
            return new LlmException(LlmErrorType.MODEL_NOT_FOUND, true, statusCode, "模型不存在或当前账号无权访问");
        }
        if (statusCode >= 500) {
            return new LlmException(LlmErrorType.SERVER_ERROR, true, statusCode, "模型服务暂时不可用");
        }
        return new LlmException(LlmErrorType.UNKNOWN, true, statusCode, "模型服务返回了无法处理的错误");
    }

    private static boolean isContextLimit(String value) {
        return value.contains("context_length_exceeded")
                || value.contains("context window")
                || value.contains("maximum context length")
                || value.contains("prompt is too long")
                || value.contains("input is too long")
                || value.contains("input tokens exceed")
                || value.contains("too many input tokens")
                || value.contains("request_too_large");
    }

    public LlmException fromTransport(Throwable throwable) {
        if (throwable instanceof HttpTimeoutException) {
            return new LlmException(LlmErrorType.TIMEOUT, true, null, "模型请求超时", throwable);
        }
        if (throwable instanceof InterruptedException) {
            // 恢复中断标记，让上层 Agent 能区分主动取消与可重试的网络失败。
            Thread.currentThread().interrupt();
            return new LlmException(LlmErrorType.INTERRUPTED, false, null, "模型请求已中断", throwable);
        }
        if (throwable instanceof IOException) {
            return new LlmException(LlmErrorType.NETWORK, true, null, "无法连接模型服务或响应流已中断", throwable);
        }
        return new LlmException(LlmErrorType.UNKNOWN, true, null, "模型请求发生未知错误", throwable);
    }
}
