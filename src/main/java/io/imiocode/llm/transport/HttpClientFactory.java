package io.imiocode.llm.transport;

import io.imiocode.config.AppConfig;

import java.net.http.HttpClient;

/** 使用统一连接策略创建 Provider HTTP 客户端。 */
public final class HttpClientFactory {
    public HttpClient create(AppConfig config) {
        return HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                // 仅采用 JDK 的普通重定向策略；HTTPS 降级等行为仍由 HttpClient 自身限制。
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
}
