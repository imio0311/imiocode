package io.imiocode.hook.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.hook.HookContext;
import io.imiocode.hook.HookEvent;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** 在应用各层之间共享工作区与当前会话标识。 */
public final class HookContextFactory {
    private final Path workspace;
    private final AtomicReference<String> sessionId = new AtomicReference<>();

    public HookContextFactory(Path workspace) {
        this.workspace = workspace.toAbsolutePath().normalize();
    }

    public HookContext.Builder builder(HookEvent event) {
        HookContext.Builder builder = HookContext.builder(event, workspace);
        Optional.ofNullable(sessionId.get()).ifPresent(builder::sessionId);
        return builder;
    }

    public void setSessionId(String value) { sessionId.set(value); }
    public void clearSessionId() { sessionId.set(null); }
    public Optional<String> sessionId() { return Optional.ofNullable(sessionId.get()); }
    public Path workspace() { return workspace; }

    public static Map<String, Object> toolArgs(ObjectNode arguments) {
        Map<String, Object> result = new LinkedHashMap<>();
        arguments.fields().forEachRemaining(entry -> result.put(entry.getKey(), value(entry.getValue())));
        return result;
    }

    private static Object value(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            node.fields().forEachRemaining(entry -> map.put(entry.getKey(), value(entry.getValue())));
            return map;
        }
        if (node.isArray()) {
            java.util.List<Object> values = new java.util.ArrayList<>();
            node.forEach(item -> values.add(value(item)));
            return values;
        }
        if (node.isBoolean()) return node.booleanValue();
        if (node.isIntegralNumber()) return node.longValue();
        if (node.isFloatingPointNumber()) return node.doubleValue();
        if (node.isNull()) return null;
        return node.asText();
    }
}
