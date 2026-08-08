package io.imiocode.skill;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.ToolRisk;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** 目录型 Skill 声明的命令工具。 */
public record SkillToolSpec(
        String name,
        String exposedName,
        String description,
        ObjectNode inputSchema,
        String command,
        List<String> arguments,
        ToolRisk risk
) {
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,63}");

    public SkillToolSpec {
        name = requireText(name, "专属工具名称");
        exposedName = requireText(exposedName, "专属工具暴露名称");
        description = requireText(description, "专属工具描述");
        command = requireText(command, "专属工具命令");
        if (command.contains("${")) {
            throw new SkillException("专属工具 command 必须固定；动态值只能放在 args 中");
        }
        if (!NAME.matcher(name).matches() || !NAME.matcher(exposedName).matches()) {
            throw new SkillException("专属工具名称格式无效: " + name);
        }
        inputSchema = Objects.requireNonNull(inputSchema, "inputSchema").deepCopy();
        arguments = List.copyOf(Objects.requireNonNullElse(arguments, List.of()));
        risk = Objects.requireNonNullElse(risk, ToolRisk.HIGH);
    }

    @Override
    public ObjectNode inputSchema() {
        return inputSchema.deepCopy();
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new SkillException(label + "不能为空");
        return value.trim();
    }
}
