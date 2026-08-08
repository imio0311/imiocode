package io.imiocode.skill.install;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkSkillRemoteTransportTest {
    private static final URI START = URI.create("https://github.com/acme/repo");

    @Test
    void downloadsBoundedBodyAndHidesFailedResponseContent() {
        ScriptedClient client = new ScriptedClient();
        client.reply(START, 200, Map.of("Content-Length", List.of("2")), "ok");
        JdkSkillRemoteTransport transport = transport(client, SkillInstallConfig.defaults());

        RemoteResponse result = transport.get(START, new SkillDownloadBudget(SkillInstallConfig.defaults()));
        assertEquals("ok", new String(result.body(), StandardCharsets.UTF_8));

        ScriptedClient failed = new ScriptedClient();
        failed.reply(START, 500, Map.of(), "secret-response-body");
        SkillInstallException error = assertThrows(SkillInstallException.class,
                () -> transport(failed, SkillInstallConfig.defaults()).get(
                        START, new SkillDownloadBudget(SkillInstallConfig.defaults())));
        assertFalse(error.getMessage().contains("secret-response-body"));
    }

    @Test
    void validatesEveryRedirectAndRejectsTooManyHops() {
        ScriptedClient crossHost = new ScriptedClient();
        crossHost.reply(START, 302, Map.of("Location", List.of("https://example.com/steal")), "");
        assertThrows(SkillInstallException.class, () -> transport(crossHost, SkillInstallConfig.defaults())
                .get(START, new SkillDownloadBudget(SkillInstallConfig.defaults())));
        assertEquals(1, crossHost.calls());

        ScriptedClient loop = new ScriptedClient();
        URI current = START;
        for (int i = 0; i < 4; i++) {
            URI next = URI.create("https://github.com/acme/repo/r" + i);
            loop.reply(current, 302, Map.of("Location", List.of(next.toString())), "");
            current = next;
        }
        SkillInstallException error = assertThrows(SkillInstallException.class,
                () -> transport(loop, SkillInstallConfig.defaults()).get(
                        START, new SkillDownloadBudget(SkillInstallConfig.defaults())));
        assertTrue(error.getMessage().contains("重定向次数过多"));
        assertEquals(4, loop.calls());
    }

    @Test
    void enforcesDeclaredLengthMapsTimeoutAndCancelsPendingRequest() throws Exception {
        SkillInstallConfig tiny = new SkillInstallConfig(Duration.ofSeconds(1), 1, 4, 4,
                SkillInstallConfig.TRUSTED_HOSTS);
        ScriptedClient oversized = new ScriptedClient();
        oversized.reply(START, 200, Map.of("Content-Length", List.of("2000000")), "x");
        assertThrows(SkillInstallException.class, () -> transport(oversized, tiny)
                .get(START, new SkillDownloadBudget(tiny)));

        ScriptedClient timedOut = new ScriptedClient();
        timedOut.fail(START, new HttpTimeoutException("private endpoint detail"));
        SkillInstallException timeout = assertThrows(SkillInstallException.class,
                () -> transport(timedOut, tiny).get(START, new SkillDownloadBudget(tiny)));
        assertEquals("远程 Skill 下载超时", timeout.getMessage());

        ScriptedClient pending = new ScriptedClient();
        pending.pending(START);
        JdkSkillRemoteTransport cancellable = transport(pending, tiny);
        CompletableFuture<RemoteResponse> task = CompletableFuture.supplyAsync(
                () -> cancellable.get(START, new SkillDownloadBudget(tiny)));
        assertTrue(pending.awaitRequest());
        cancellable.cancel();
        CompletionException cancelled = assertThrows(CompletionException.class, task::join);
        assertTrue(cancelled.getCause() instanceof SkillInstallException);
        assertTrue(cancelled.getCause().getMessage().contains("取消"));
    }

    private static JdkSkillRemoteTransport transport(ScriptedClient client, SkillInstallConfig config) {
        return new JdkSkillRemoteTransport(config, client);
    }

    private static final class ScriptedClient extends HttpClient {
        private final Map<URI, CompletableFuture<HttpResponse<InputStream>>> responses = new LinkedHashMap<>();
        private final CountDownLatch requested = new CountDownLatch(1);
        private int calls;

        void reply(URI uri, int status, Map<String, List<String>> headers, String body) {
            HttpHeaders checkedHeaders = HttpHeaders.of(headers, (name, value) -> true);
            responses.put(uri, CompletableFuture.completedFuture(new Response(status, uri, checkedHeaders,
                    new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)))));
        }

        void fail(URI uri, Throwable error) {
            responses.put(uri, CompletableFuture.failedFuture(error));
        }

        void pending(URI uri) {
            responses.put(uri, new CompletableFuture<>());
        }

        boolean awaitRequest() throws InterruptedException { return requested.await(2, TimeUnit.SECONDS); }
        int calls() { return calls; }

        @Override @SuppressWarnings("unchecked")
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            calls++;
            requested.countDown();
            CompletableFuture<HttpResponse<InputStream>> response = responses.get(request.uri());
            if (response == null) throw new AssertionError("unexpected URI: " + request.uri());
            return (CompletableFuture<HttpResponse<T>>) (CompletableFuture<?>) response;
        }

        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> handler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return sendAsync(request, handler);
        }

        @Override public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
                throws IOException, InterruptedException {
            return sendAsync(request, handler).join();
        }

        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.of(Duration.ofSeconds(1)); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { return defaultSslContext(); }
        @Override public SSLParameters sslParameters() { return new SSLParameters(); }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }

        private static SSLContext defaultSslContext() {
            try { return SSLContext.getDefault(); }
            catch (Exception exception) { throw new AssertionError(exception); }
        }
    }

    private record Response(int statusCode, URI uri, HttpHeaders headers, InputStream body)
            implements HttpResponse<InputStream> {
        @Override public HttpRequest request() { return HttpRequest.newBuilder(uri).build(); }
        @Override public Optional<HttpResponse<InputStream>> previousResponse() { return Optional.empty(); }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
