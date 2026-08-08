package io.imiocode.skill.install;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RemoteResponse(URI uri, int statusCode, Map<String, List<String>> headers, byte[] body) {
    public RemoteResponse {
        uri = Objects.requireNonNull(uri, "uri");
        headers = Map.copyOf(Objects.requireNonNullElse(headers, Map.of()));
        body = Objects.requireNonNullElse(body, new byte[0]).clone();
    }

    @Override public byte[] body() { return body.clone(); }
}
