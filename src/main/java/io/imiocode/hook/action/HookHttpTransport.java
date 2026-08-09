package io.imiocode.hook.action;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/** HTTP Hook 的可测试传输边界。 */
public interface HookHttpTransport {
    HookHttpResult send(HookHttpRequest request);
}

record HookHttpRequest(URI uri, String method, Map<String, String> headers,
                       String body, Duration timeout) {
    HookHttpRequest {
        headers = Map.copyOf(headers);
    }
}

record HookHttpResult(boolean sent, boolean timedOut, boolean tooLarge,
                      int statusCode, String body, String safeError, Duration elapsed) { }
