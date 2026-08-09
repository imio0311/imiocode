package io.imiocode.hook.action;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JdkHookTransportTest {
    @Test void httpSendsJsonAndDoesNotFollowRedirects() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        java.util.concurrent.atomic.AtomicReference<String> body = new java.util.concurrent.atomic.AtomicReference<>();
        server.createContext("/ok", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, 2);
            exchange.getResponseBody().write("ok".getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/ok");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        try (JdkHookHttpTransport transport = new JdkHookHttpTransport()) {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            HookHttpResult ok = transport.send(new HookHttpRequest(base.resolve("/ok"), "POST",
                    Map.of("X-Test", "yes"), "{\"hello\":true}", Duration.ofSeconds(2)));
            assertEquals(200, ok.statusCode());
            assertEquals("{\"hello\":true}", body.get());
            HookHttpResult redirect = transport.send(new HookHttpRequest(base.resolve("/redirect"), "POST",
                    Map.of(), "{}", Duration.ofSeconds(2)));
            assertEquals(302, redirect.statusCode());
        } finally {
            server.stop(0);
        }
    }

    @Test void processRunnerExecutesAndTimesOut() {
        try (JdkHookProcessRunner runner = new JdkHookProcessRunner()) {
            ProcessResult ok = runner.run("echo hook-ok", Path.of(".").toAbsolutePath(),
                    minimalEnvironment(), Duration.ofSeconds(2));
            assertTrue(ok.started());
            assertEquals(0, ok.exitCode());
            assertTrue(ok.stdout().contains("hook-ok"));
            String slow = System.getProperty("os.name", "").toLowerCase().contains("win")
                    ? "ping 127.0.0.1 -n 4 > nul" : "sleep 3";
            ProcessResult timeout = runner.run(slow, Path.of(".").toAbsolutePath(),
                    minimalEnvironment(), Duration.ofMillis(100));
            assertTrue(timeout.timedOut());
        }
    }

    private static Map<String, String> minimalEnvironment() {
        Map<String, String> source = System.getenv();
        java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>();
        for (String name : new String[]{"PATH", "Path", "SystemRoot", "ComSpec", "PATHEXT"})
            if (source.containsKey(name)) result.put(name, source.get(name));
        return result;
    }
}
