package io.imiocode.llm.transport;

import io.imiocode.config.AppConfig;

import java.net.http.HttpClient;

public final class HttpClientFactory {
    public HttpClient create(AppConfig config) {
        return HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
}
