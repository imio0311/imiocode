package io.imiocode.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;

import java.util.Objects;

/** 模型根据摘要按需加载完整 Skill 的系统工具。 */
public final class LoadSkillTool implements Tool {
    private final SkillExecutor executor;
    private final ToolDefinition definition;

    public LoadSkillTool(SkillExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("name").put("type", "string")
                .put("description", "要加载的 Skill 名称");
        properties.putObject("arguments").put("type", "string")
                .put("description", "传给 Skill 的用户参数，可为空");
        schema.putArray("required").add("name");
        schema.put("additionalProperties", false);
        this.definition = new ToolDefinition(
                SkillActivator.LOAD_SKILL_TOOL,
                "按名称加载并激活可用 Skill 的完整 SOP。只有用户意图与 Skill 描述匹配时调用；"
                        + "启动摘要之外的正文和专属工具将在调用后按需提供。",
                schema,
                ToolRisk.LOW);
    }

    @Override public ToolDefinition definition() { return definition; }

    @Override
    public ToolResult execute(ObjectNode arguments) {
        if (arguments == null) return ToolResult.failure("Skill 参数必须是 JSON 对象");
        JsonNode name = arguments.get("name");
        if (name == null || !name.isTextual() || name.textValue().isBlank()) {
            return ToolResult.failure("Skill 名称不能为空");
        }
        JsonNode args = arguments.get("arguments");
        if (args != null && !args.isNull() && !args.isTextual()) {
            return ToolResult.failure("Skill arguments 必须是字符串");
        }
        return executor.executeFromTool(name.textValue(), args == null || args.isNull() ? "" : args.textValue());
    }

    @Override public void cancel() { executor.cancelFork(); }
}
