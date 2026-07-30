package io.imiocode.permission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** 将六个核心工具的参数转换成统一权限请求。 */
public final class PermissionRequestFactory {
    private static final int MAX_DISPLAY_CHARS = 160;
    private static final Map<String, PermissionOperation> OPERATIONS = Map.of(
            "read_file", PermissionOperation.READ,
            "glob", PermissionOperation.READ,
            "grep", PermissionOperation.READ,
            "write_file", PermissionOperation.WRITE,
            "edit_file", PermissionOperation.WRITE,
            "bash", PermissionOperation.COMMAND);

    private final SecretRedactor redactor;

    public PermissionRequestFactory(SecretRedactor redactor) {
        this.redactor = Objects.requireNonNull(redactor, "redactor 不能为空");
    }

    public PermissionRequest create(ToolCall call, ToolDefinition definition) {
        Objects.requireNonNull(call, "call 不能为空");
        Objects.requireNonNull(definition, "definition 不能为空");
        String toolName = call.name().toLowerCase(Locale.ROOT);
        PermissionOperation operation = OPERATIONS.get(toolName);
        if (operation == null) {
            throw new IllegalArgumentException("不支持的权限工具: " + call.name());
        }
        ObjectNode arguments = call.arguments();
        String rawTarget = switch (toolName) {
            case "bash" -> requireText(arguments, "command");
            case "glob" -> requireText(arguments, "pattern");
            case "grep" -> optionalText(arguments, "path", ".");
            default -> requireText(arguments, "path");
        };
        String normalized = operation == PermissionOperation.COMMAND
                ? normalizeCommand(rawTarget)
                : normalizePath(rawTarget);
        String display = truncate(redactor.redact(normalized));
        return new PermissionRequest(
                call, definition.risk(), operation, normalized, display);
    }

    private static String requireText(ObjectNode arguments, String field) {
        JsonNode node = arguments.get(field);
        if (node == null || !node.isTextual() || node.textValue().isBlank()) {
            throw new IllegalArgumentException("工具参数 " + field + " 不能为空");
        }
        return node.textValue().trim();
    }

    private static String optionalText(ObjectNode arguments, String field, String fallback) {
        JsonNode node = arguments.get(field);
        if (node == null || node.isNull()) {
            return fallback;
        }
        if (!node.isTextual() || node.textValue().isBlank()) {
            throw new IllegalArgumentException("工具参数 " + field + " 格式无效");
        }
        return node.textValue().trim();
    }

    private static String normalizePath(String target) {
        String normalized = target.replace('\\', '/').trim();
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        return normalized;
    }

    private static String normalizeCommand(String command) {
        return command.trim().replaceAll("\\s+", " ");
    }

    private static String truncate(String value) {
        if (value.length() <= MAX_DISPLAY_CHARS) {
            return value;
        }
        return value.substring(0, MAX_DISPLAY_CHARS - 1) + "…";
    }
}
