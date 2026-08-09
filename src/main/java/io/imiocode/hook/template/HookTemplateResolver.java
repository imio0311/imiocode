package io.imiocode.hook.template;

import io.imiocode.hook.HookContext;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 对 Hook 文本做一次、不可递归的上下文变量替换。 */
public final class HookTemplateResolver {
    private static final Pattern VARIABLE = Pattern.compile(
            "\\$(EVENT|TOOL_NAME|FILE_PATH|MESSAGE|ERROR|TOOL_ARGS(?:\\.[A-Za-z0-9_-]+)+)");
    private static final Pattern ANY_UPPER_VARIABLE = Pattern.compile("\\$[A-Z][A-Z0-9_]*(?:\\.[A-Za-z0-9_-]+)*");

    public String resolve(String template, HookContext context) {
        Objects.requireNonNull(template, "template"); Objects.requireNonNull(context, "context");
        Matcher matcher = VARIABLE.matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String variable = matcher.group(1);
            String value = switch (variable) {
                case "EVENT" -> context.event().configName();
                case "TOOL_NAME" -> context.toolName().orElse("");
                case "FILE_PATH" -> context.filePath().map(Object::toString).orElse("");
                case "MESSAGE" -> context.message().orElse("");
                case "ERROR" -> context.error().orElse("");
                default -> context.resolveField("args." + variable.substring("TOOL_ARGS.".length())).orElse("");
            };
            matcher.appendReplacement(output, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    public void validate(String template) {
        if (template == null) return;
        Matcher matcher = ANY_UPPER_VARIABLE.matcher(template);
        while (matcher.find()) {
            if (!VARIABLE.matcher(matcher.group()).matches())
                throw new IllegalArgumentException("未知 Hook 模板变量: " + matcher.group());
        }
    }
}
