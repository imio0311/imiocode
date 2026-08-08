package io.imiocode.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.permission.PermissionOperation;
import io.imiocode.permission.PermissionTargetProvider;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.core.BashTool;
import io.imiocode.tool.workspace.WorkspacePolicy;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 把 tool.json 的固定命令模板包装为内部工具。 */
public final class SkillCommandTool implements Tool, PermissionTargetProvider, AutoCloseable {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}");
    private final SkillToolSpec spec;
    private final ToolDefinition definition;
    private final BashTool bash;

    public SkillCommandTool(SkillToolSpec spec, WorkspacePolicy policy,
                            ToolLimits limits, SecretRedactor redactor) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.definition = new ToolDefinition(
                spec.exposedName(), spec.description(), spec.inputSchema(), spec.risk());
        this.bash = new BashTool(policy, limits, redactor);
    }

    @Override public ToolDefinition definition() { return definition; }

    @Override
    public ToolResult execute(ObjectNode arguments) {
        ObjectNode delegated = JsonNodeFactory.instance.objectNode();
        delegated.put("command", render(arguments));
        return bash.execute(delegated);
    }

    @Override public void cancel() { bash.cancel(); }
    @Override public void close() { bash.close(); }
    @Override public PermissionOperation permissionOperation() { return PermissionOperation.COMMAND; }
    @Override public String permissionTarget(ObjectNode arguments) { return renderForPermission(arguments); }

    String render(ObjectNode values) {
        Objects.requireNonNull(values, "工具参数不能为空");
        StringBuilder command = new StringBuilder(renderTemplate(spec.command(), values));
        for (String argument : spec.arguments()) {
            command.append(' ').append(shellQuote(renderTemplate(argument, values)));
        }
        return command.toString();
    }

    String renderForPermission(ObjectNode values) {
        Objects.requireNonNull(values, "工具参数不能为空");
        StringBuilder command = new StringBuilder(renderTemplate(spec.command(), values));
        for (String argument : spec.arguments()) {
            command.append(' ').append(renderTemplate(argument, values));
        }
        return command.toString();
    }

    private static String renderTemplate(String template, ObjectNode values) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            JsonNode value = values.get(matcher.group(1));
            if (value == null || value.isNull() || value.isContainerNode()) {
                throw new SkillException("专属工具缺少字符串参数: " + matcher.group(1));
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(value.asText()));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String shellQuote(String value) {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return "'" + value.replace("'", "''") + "'";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
