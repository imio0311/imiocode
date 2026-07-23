package io.imiocode.llm.transport;

import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.Locale;

public final class HttpErrorMapper {
    public LlmException fromStatus(int statusCode, String providerCode) {
        String normalizedCode = providerCode == null ? "" : providerCode.toLowerCase(Locale.ROOT);
        if (statusCode == 401 || statusCode == 403) {
            return new LlmException(LlmErrorType.AUTHENTICATION, true, statusCode, "认证失败，请检查 API Key");
        }
        if (statusCode == 429) {
            return new LlmException(LlmErrorType.RATE_LIMIT, true, statusCode, "请求受限，请稍后重试");
        }
        if (normalizedCode.contains("model") && (normalizedCode.contains("not_found") || normalizedCode.contains("not found"))) {
            return new LlmException(LlmErrorType.MODEL_NOT_FOUND, true, statusCode, "模型不存在或当前账号无权访问");
        }
        if (statusCode >= 500) {
            return new LlmException(LlmErrorType.SERVER_ERROR, true, statusCode, "模型服务暂时不可用");
        }
        return new LlmException(LlmErrorType.UNKNOWN, true, statusCode, "模型服务返回了无法处理的错误");
    }

    public LlmException fromTransport(Throwable throwable) {
        if (throwable instanceof HttpTimeoutException) {
            return new LlmException(LlmErrorType.TIMEOUT, true, null, "模型请求超时", throwable);
        }
        if (throwable instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return new LlmException(LlmErrorType.INTERRUPTED, false, null, "模型请求已中断", throwable);
        }
        if (throwable instanceof IOException) {
            return new LlmException(LlmErrorType.NETWORK, true, null, "无法连接模型服务或响应流已中断", throwable);
        }
        return new LlmException(LlmErrorType.UNKNOWN, true, null, "模型请求发生未知错误", throwable);
    }
}
