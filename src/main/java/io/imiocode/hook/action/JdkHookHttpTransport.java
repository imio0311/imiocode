package io.imiocode.hook.action;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** 不跟随重定向、限制响应体大小的 JDK HTTP 传输。 */
public final class JdkHookHttpTransport implements HookHttpTransport, AutoCloseable {
    static final int RESPONSE_LIMIT = 1024 * 1024;
    private final HttpClient client;

    public JdkHookHttpTransport() {
        this(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    JdkHookHttpTransport(HttpClient client) {
        this.client = client;
    }

    @Override
    public HookHttpResult send(HookHttpRequest request) {
        long start = System.nanoTime();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
                    .timeout(request.timeout())
                    .method(request.method(), HttpRequest.BodyPublishers.ofString(request.body(), StandardCharsets.UTF_8));
            request.headers().forEach(builder::header);
            if (!request.headers().keySet().stream().anyMatch(name -> name.equalsIgnoreCase("content-type"))) {
                builder.header("Content-Type", "application/json; charset=utf-8");
            }
            HttpResponse<InputStream> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            LimitedBody body = readLimited(response.body());
            return new HookHttpResult(true, false, body.tooLarge(), response.statusCode(), body.text(), "", elapsed(start));
        } catch (HttpTimeoutException exception) {
            return new HookHttpResult(false, true, false, 0, "", "HTTP Hook 请求超时", elapsed(start));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new HookHttpResult(false, false, false, 0, "", "HTTP Hook 请求被中断", elapsed(start));
        } catch (ConnectException exception) {
            return new HookHttpResult(false, false, false, 0, "", "无法连接 HTTP Hook 服务", elapsed(start));
        } catch (IOException | IllegalArgumentException exception) {
            return new HookHttpResult(false, false, false, 0, "", "HTTP Hook 请求失败", elapsed(start));
        }
    }

    private static LimitedBody readLimited(InputStream input) throws IOException {
        try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            boolean tooLarge = false;
            while ((count = input.read(buffer)) >= 0) {
                int accepted = Math.min(count, Math.max(0, RESPONSE_LIMIT - output.size()));
                output.write(buffer, 0, accepted);
                if (accepted < count) {
                    tooLarge = true;
                    break;
                }
            }
            return new LimitedBody(output.toString(StandardCharsets.UTF_8), tooLarge);
        }
    }

    private static Duration elapsed(long start) {
        return Duration.ofNanos(System.nanoTime() - start);
    }

    @Override public void close() { }

    private record LimitedBody(String text, boolean tooLarge) { }
}
