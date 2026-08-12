package io.imiocode.permission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.Tool;
import io.imiocode.permission.command.CommandRiskAssessment;
import io.imiocode.permission.command.CommandRiskClassifier;
import io.imiocode.tool.ToolRisk;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** 将六个核心工具的参数转换成统一权限请求。 */
public final class PermissionRequestFactory {
    private static final int MAX_DISPLAY_CHARS = 160;
    private static final Map<String, PermissionOperation> OPERATIONS = Map.ofEntries(
            Map.entry("read_file", PermissionOperation.READ),
            Map.entry("glob", PermissionOperation.READ),
            Map.entry("grep", PermissionOperation.READ),
            Map.entry("write_file", PermissionOperation.WRITE),
            Map.entry("edit_file", PermissionOperation.WRITE),
            Map.entry("load_skill", PermissionOperation.READ),
            Map.entry("agent", PermissionOperation.READ),
            Map.entry("bash", PermissionOperation.COMMAND),
            Map.entry("teamcreate", PermissionOperation.WRITE),
            Map.entry("teamconverge", PermissionOperation.READ),
            Map.entry("teamdelete", PermissionOperation.WRITE),
            Map.entry("sendmessage", PermissionOperation.WRITE),
            Map.entry("taskcreate", PermissionOperation.WRITE),
            Map.entry("taskget", PermissionOperation.READ),
            Map.entry("tasklist", PermissionOperation.READ),
            Map.entry("taskupdate", PermissionOperation.WRITE),
            Map.entry("taskstop", PermissionOperation.WRITE),
            Map.entry("coordinatormode", PermissionOperation.READ),
            Map.entry("coordinatoradvance", PermissionOperation.READ));

    private final SecretRedactor redactor;
    private final CommandRiskClassifier commandRiskClassifier;

    public PermissionRequestFactory(SecretRedactor redactor) {
        this(redactor, command -> new CommandRiskAssessment(
                ToolRisk.HIGH, "命令未启用动态风险分类，按高风险处理"));
    }

    public PermissionRequestFactory(
            SecretRedactor redactor, CommandRiskClassifier commandRiskClassifier) {
        this.redactor = Objects.requireNonNull(redactor, "redactor 不能为空");
        this.commandRiskClassifier = Objects.requireNonNull(
                commandRiskClassifier, "commandRiskClassifier 不能为空");
    }

    public PermissionRequest create(ToolCall call, ToolDefinition definition) {
        Objects.requireNonNull(call, "call 不能为空");
        Objects.requireNonNull(definition, "definition 不能为空");
        String toolName = call.name().toLowerCase(Locale.ROOT);
        PermissionOperation operation = toolName.startsWith("mcp_")
                ? PermissionOperation.COMMAND
                : OPERATIONS.get(toolName);
        if (operation == null) {
            throw new IllegalArgumentException("不支持的权限工具: " + call.name());
        }
        ObjectNode arguments = call.arguments();
        String rawTarget = toolName.startsWith("mcp_") ? call.name() : switch (toolName) {
            case "bash" -> requireText(arguments, "command");
            case "glob" -> requireText(arguments, "pattern");
            case "grep" -> optionalText(arguments, "path", ".");
            case "load_skill" -> ".";
            case "agent" -> ".";
            case "teamcreate" -> ".imiocode/teams/team.json";
            case "teamdelete" -> ".imiocode/teams";
            case "sendmessage" -> ".imiocode/teams/mailbox.jsonl";
            case "taskcreate", "taskupdate", "taskstop" -> ".imiocode/teams/tasks.json";
            case "taskget", "tasklist", "teamconverge", "coordinatormode", "coordinatoradvance" -> ".";
            default -> requireText(arguments, "path");
        };
        String normalized = operation == PermissionOperation.COMMAND
                ? normalizeCommand(rawTarget)
                : normalizePath(rawTarget);
        String display = truncate(redactor.redact(normalized));
        CommandRiskAssessment assessment = "bash".equals(toolName)
                ? commandRiskClassifier.classify(normalized)
                : new CommandRiskAssessment(definition.risk(),
                "工具声明的静态风险等级为 " + definition.risk());
        return new PermissionRequest(
                call, assessment.risk(), operation, normalized, display, assessment.reason());
    }

    /** 动态工具可提供渲染后的真实目标，确保仍经过危险命令与规则检查。 */
    public PermissionRequest create(ToolCall call, Tool tool) {
        Objects.requireNonNull(tool, "tool 不能为空");
        if (!(tool instanceof PermissionTargetProvider provider)) {
            return create(call, tool.definition());
        }
        String rawTarget = provider.permissionTarget(call.arguments());
        String normalized = provider.permissionOperation() == PermissionOperation.COMMAND
                ? normalizeCommand(rawTarget) : normalizePath(rawTarget);
        ToolRisk risk = provider.permissionRisk(call.arguments(), tool.definition().risk());
        return new PermissionRequest(
                call,
                risk,
                provider.permissionOperation(),
                normalized,
                truncate(redactor.redact(normalized)),
                "工具根据本次调用参数标记为 " + risk);
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
