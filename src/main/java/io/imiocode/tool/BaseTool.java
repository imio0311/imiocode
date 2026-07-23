package io.imiocode.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** 统一工具参数、异常、耗时、截断和脱敏处理。 */
public abstract class BaseTool implements Tool {
    public static final String TRUNCATION_MARKER = "\n...[输出已截断]";

    private final ToolDefinition definition;
    protected final ToolLimits limits;
    protected final SecretRedactor redactor;

    protected BaseTool(ToolDefinition definition, ToolLimits limits, SecretRedactor redactor) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
    }

    @Override
    public final ToolDefinition definition() {
        return definition;
    }

    @Override
    public final ToolResult execute(ObjectNode arguments) {
        long started = System.nanoTime();
        ToolResult result;
        try {
            if (arguments == null) {
                result = ToolResult.failure("工具参数必须是 JSON 对象");
            } else {
                result = Objects.requireNonNull(
                        executeValidated(arguments.deepCopy()),
                        "工具实现返回了空结果");
            }
        } catch (IllegalArgumentException exception) {
            result = ToolResult.failure(safeMessage(exception, "工具参数无效"));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            result = ToolResult.interrupted("", "工具执行已中断", false);
        } catch (Exception exception) {
            result = ToolResult.failure("工具执行失败");
        }
        Duration duration = Duration.ofNanos(Math.max(0, System.nanoTime() - started));
        return sanitizeAndLimit(result).withDuration(duration);
    }

    protected abstract ToolResult executeValidated(ObjectNode arguments) throws Exception;

    protected final String requireText(ObjectNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("参数 " + field + " 必须是非空字符串");
        }
        return value.textValue();
    }

    protected final String requireString(ObjectNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException("参数 " + field + " 必须是字符串");
        }
        return value.textValue();
    }

    protected final Integer optionalPositiveInt(ObjectNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.canConvertToInt() || value.intValue() <= 0) {
            throw new IllegalArgumentException("参数 " + field + " 必须是正整数");
        }
        return value.intValue();
    }

    protected final void rejectUnknownFields(ObjectNode arguments, String... allowedFields) {
        Set<String> allowed = new HashSet<>(Set.of(allowedFields));
        arguments.fieldNames().forEachRemaining(field -> {
            if (!allowed.contains(field)) {
                throw new IllegalArgumentException("未知参数: " + field);
            }
        });
    }

    private ToolResult sanitizeAndLimit(ToolResult result) {
        String output = redactor.redact(result.output());
        String error = redactor.redact(result.error());
        boolean truncated = result.truncated();
        long budget = limits.maxResultBytes();

        LimitedText limitedOutput = limitUtf8(output, budget);
        budget = Math.max(0, budget - utf8Length(limitedOutput.text()));
        LimitedText limitedError = limitUtf8(error, budget);
        truncated = truncated || limitedOutput.truncated() || limitedError.truncated();

        return new ToolResult(
                result.success(),
                limitedOutput.text(),
                limitedError.text(),
                truncated,
                result.duration(),
                result.exitCode());
    }

    private static LimitedText limitUtf8(String value, long maxBytes) {
        if (utf8Length(value) <= maxBytes) {
            return new LimitedText(value, false);
        }
        if (maxBytes <= 0) {
            return new LimitedText("", true);
        }
        byte[] marker = TRUNCATION_MARKER.getBytes(StandardCharsets.UTF_8);
        long contentBudget = Math.max(0, maxBytes - marker.length);
        StringBuilder output = new StringBuilder();
        long used = 0;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            String unit = new String(Character.toChars(codePoint));
            int bytes = unit.getBytes(StandardCharsets.UTF_8).length;
            if (used + bytes > contentBudget) {
                break;
            }
            output.append(unit);
            used += bytes;
            offset += Character.charCount(codePoint);
        }
        if (marker.length <= maxBytes) {
            output.append(TRUNCATION_MARKER);
        }
        return new LimitedText(output.toString(), true);
    }

    private String safeMessage(Exception exception, String fallback) {
        String message = exception.getMessage();
        return redactor.redact(message == null || message.isBlank() ? fallback : message);
    }

    private static long utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private record LimitedText(String text, boolean truncated) {
    }
}
