package io.imiocode.terminal;

import com.fasterxml.jackson.databind.JsonNode;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.tool.ToolExecutionState;
import io.imiocode.tool.ToolResult;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** 生成不会泄露写入正文、且最多 240 字符的工具展示摘要。 */
public final class ToolSummaryFormatter {
    public static final int MAX_CHARS = 240;

    private final SecretRedactor redactor;

    public ToolSummaryFormatter(SecretRedactor redactor) {
        this.redactor = Objects.requireNonNull(redactor, "redactor");
    }

    public String inputSummary(ToolCall call) {
        Objects.requireNonNull(call, "call");
        String summary = switch (call.name()) {
            case "read_file" -> "path=" + text(call, "path")
                    + optional(call, "start_line") + optional(call, "end_line");
            case "write_file" -> "path=" + text(call, "path")
                    + "，正文 " + utf8Bytes(call.arguments().path("content").asText("")) + " 字节（已隐藏）";
            case "edit_file" -> "path=" + text(call, "path") + "，替换正文已隐藏";
            case "bash" -> "command=" + text(call, "command");
            case "glob" -> "pattern=" + text(call, "pattern");
            case "grep" -> "pattern=" + text(call, "pattern")
                    + (call.arguments().has("path") ? "，path=" + text(call, "path") : "");
            default -> "参数已隐藏";
        };
        return safeLimit(summary);
    }

    public String resultSummary(ToolExecutionEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.state() == ToolExecutionState.QUEUED) {
            return "等待执行";
        }
        if (event.state() == ToolExecutionState.RUNNING) {
            return "执行中";
        }
        ToolResult result = event.result();
        StringBuilder summary = new StringBuilder(result.success() ? "成功" : "失败");
        if (result.exitCode() != null) {
            summary.append("，退出码 ").append(result.exitCode());
        }
        if ("glob".equals(event.call().name()) || "grep".equals(event.call().name())) {
            long count = result.output().lines()
                    .filter(line -> !line.contains("输出已截断"))
                    .filter(line -> !line.isBlank())
                    .count();
            summary.append("，").append(count).append(" 条结果");
        } else if (!result.output().isEmpty()) {
            summary.append("，返回 ").append(result.output().length()).append(" 字符");
        }
        if (!result.error().isBlank()) {
            summary.append("，").append(result.error().lines().findFirst().orElse("执行失败"));
        }
        if (result.truncated()) {
            summary.append("，输出已截断");
        }
        return safeLimit(summary.toString());
    }

    public String riskLabel(ToolCall call) {
        return switch (call.name()) {
            case "write_file", "edit_file" -> "MEDIUM";
            case "bash" -> "HIGH";
            default -> "LOW";
        };
    }

    private String optional(ToolCall call, String field) {
        JsonNode node = call.arguments().get(field);
        return node == null ? "" : "，" + field + "=" + node.asText();
    }

    private static String text(ToolCall call, String field) {
        return call.arguments().path(field).asText("");
    }

    private String safeLimit(String value) {
        String safe = redactor.redact(value).replace('\r', ' ').replace('\n', ' ');
        if (safe.length() <= MAX_CHARS) {
            return safe;
        }
        int end = MAX_CHARS - 1;
        if (end > 0 && Character.isHighSurrogate(safe.charAt(end - 1))) {
            end--;
        }
        return safe.substring(0, end) + "…";
    }

    private static int utf8Bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
