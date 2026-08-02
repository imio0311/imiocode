package io.imiocode.terminal;

import com.fasterxml.jackson.databind.JsonNode;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.tool.ToolExecutionState;
import io.imiocode.tool.ToolResult;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
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

    /** 为精简 UI 生成不带状态图标的单行完成摘要。 */
    public String compactSummary(ToolExecutionEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.state() != ToolExecutionState.SUCCEEDED
                && event.state() != ToolExecutionState.FAILED) {
            throw new IllegalArgumentException("精简摘要只接受完成态工具事件");
        }
        ToolResult result = event.result();
        StringBuilder summary = new StringBuilder(displayName(event.call()));
        String target = compactTarget(event.call());
        if (!target.isBlank()) {
            summary.append(' ').append(target);
        }
        if ("glob".equals(event.call().name()) || "grep".equals(event.call().name())) {
            summary.append(" · ").append(resultCount(result)).append(" results");
        }
        if (!result.success()) {
            String error = result.error().lines().findFirst().orElse("执行失败");
            if (error.isBlank()) {
                error = "执行失败";
            }
            summary.append(" · ").append(error);
        }
        summary.append(" (").append(formatDuration(result.duration())).append(')');
        return safeLimit(summary.toString());
    }

    public String displayName(ToolCall call) {
        Objects.requireNonNull(call, "call");
        String name = switch (call.name()) {
            case "read_file" -> "Read";
            case "write_file" -> "Write";
            case "edit_file" -> "Edit";
            case "bash" -> "Bash";
            case "glob" -> "Glob";
            case "grep" -> "Grep";
            default -> call.name().replaceAll("_+", " ");
        };
        return safeLimit(name);
    }

    private String optional(ToolCall call, String field) {
        JsonNode node = call.arguments().get(field);
        return node == null ? "" : "，" + field + "=" + node.asText();
    }

    private String compactTarget(ToolCall call) {
        return switch (call.name()) {
            case "read_file", "write_file", "edit_file" -> text(call, "path");
            case "bash" -> text(call, "command");
            case "glob", "grep" -> text(call, "pattern");
            default -> "";
        };
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

    private static long resultCount(ToolResult result) {
        return result.output().lines()
                .filter(line -> !line.contains("输出已截断"))
                .filter(line -> !line.isBlank())
                .count();
    }

    static String formatDuration(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        return String.format(Locale.ROOT, "%.1fs", duration.toNanos() / 1_000_000_000d);
    }

    private static int utf8Bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
