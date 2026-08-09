package io.imiocode.hook;

import io.imiocode.tool.SecretRedactor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Hook 执行时的最小不可变快照。 */
public record HookContext(HookEvent event, Path workspace, Optional<String> sessionId,
                          OptionalInt iteration, Optional<String> toolName,
                          Map<String, Object> toolArgs, Optional<Path> filePath,
                          Optional<String> message, Optional<String> error,
                          Map<String, Object> data) {
    public HookContext {
        event = Objects.requireNonNull(event, "event");
        workspace = Objects.requireNonNull(workspace, "workspace").toAbsolutePath().normalize();
        sessionId = clean(sessionId);
        iteration = Objects.requireNonNullElse(iteration, OptionalInt.empty());
        toolName = clean(toolName);
        toolArgs = immutableMap(toolArgs);
        filePath = (filePath == null ? Optional.<Path>empty() : filePath).map(Path::normalize);
        message = clean(message);
        error = clean(error);
        data = immutableMap(data);
    }

    public static Builder builder(HookEvent event, Path workspace) {
        return new Builder(event, workspace);
    }

    public Optional<String> resolveField(String field) {
        if (field == null) return Optional.empty();
        return switch (field) {
            case "event" -> Optional.of(event.configName());
            case "tool", "tool_name" -> toolName;
            case "file_path" -> filePath.map(Path::toString);
            case "message" -> message;
            case "error" -> error;
            default -> field.startsWith("args.")
                    ? resolveNested(toolArgs, field.substring(5))
                    : field.startsWith("data.")
                    ? resolveNested(data, field.substring(5)) : Optional.empty();
        };
    }

    public Map<String, Object> safePayload(SecretRedactor redactor) {
        Objects.requireNonNull(redactor, "redactor");
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("event", event.configName());
        sessionId.ifPresent(value -> output.put("session_id", redactor.redact(value)));
        if (iteration.isPresent()) output.put("iteration", iteration.getAsInt());
        toolName.ifPresent(value -> output.put("tool_name", redactor.redact(value)));
        output.put("tool_args", redactValue(toolArgs, redactor));
        filePath.ifPresent(value -> output.put("file_path", redactor.redact(value.toString())));
        message.ifPresent(value -> output.put("message", redactor.redact(value)));
        error.ifPresent(value -> output.put("error", redactor.redact(value)));
        output.put("data", redactValue(data, redactor));
        return Map.copyOf(output);
    }

    private static Optional<String> resolveNested(Map<String, Object> root, String path) {
        if (path == null || path.isBlank()) return Optional.empty();
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map) || !map.containsKey(part)) return Optional.empty();
            current = map.get(part);
        }
        if (current == null) return Optional.empty();
        if (current instanceof Map<?, ?> || current instanceof List<?>) return Optional.of(current.toString());
        return Optional.of(String.valueOf(current));
    }

    private static Optional<String> clean(Optional<String> value) {
        return (value == null ? Optional.<String>empty() : value).map(String::trim)
                .filter(text -> !text.isEmpty());
    }

    private static Map<String, Object> immutableMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        Objects.requireNonNullElse(source, Map.<String, Object>of())
                .forEach((key, value) -> result.put(String.valueOf(key), immutableValue(value)));
        return Map.copyOf(result);
    }

    private static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> copy.put(String.valueOf(key), immutableValue(nested)));
            return Map.copyOf(copy);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            list.forEach(item -> copy.add(immutableValue(item)));
            return List.copyOf(copy);
        }
        return value;
    }

    private static Object redactValue(Object value, SecretRedactor redactor) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> copy.put(String.valueOf(key), redactValue(nested, redactor)));
            return copy;
        }
        if (value instanceof List<?> list) return list.stream().map(item -> redactValue(item, redactor)).toList();
        return value instanceof String text ? redactor.redact(text) : value;
    }

    public static final class Builder {
        private final HookEvent event;
        private final Path workspace;
        private Optional<String> sessionId = Optional.empty();
        private OptionalInt iteration = OptionalInt.empty();
        private Optional<String> toolName = Optional.empty();
        private Map<String, Object> toolArgs = Map.of();
        private Optional<Path> filePath = Optional.empty();
        private Optional<String> message = Optional.empty();
        private Optional<String> error = Optional.empty();
        private final Map<String, Object> data = new LinkedHashMap<>();

        private Builder(HookEvent event, Path workspace) { this.event = event; this.workspace = workspace; }
        public Builder sessionId(String value) { sessionId = Optional.ofNullable(value); return this; }
        public Builder iteration(int value) { iteration = OptionalInt.of(value); return this; }
        public Builder toolName(String value) { toolName = Optional.ofNullable(value); return this; }
        public Builder toolArgs(Map<String, Object> value) { toolArgs = value; return this; }
        public Builder filePath(Path value) { filePath = Optional.ofNullable(value); return this; }
        public Builder message(String value) { message = Optional.ofNullable(value); return this; }
        public Builder error(String value) { error = Optional.ofNullable(value); return this; }
        public Builder data(String key, Object value) { data.put(key, value); return this; }
        public HookContext build() {
            return new HookContext(event, workspace, sessionId, iteration, toolName, toolArgs,
                    filePath, message, error, data);
        }
    }
}
