package io.imiocode.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.permission.PermissionOperation;
import io.imiocode.permission.PermissionTargetProvider;
import io.imiocode.skill.install.SkillInstallListener;
import io.imiocode.skill.install.SkillInstallRequest;
import io.imiocode.skill.install.SkillInstallResult;
import io.imiocode.skill.install.SkillInstaller;
import io.imiocode.tool.BaseTool;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;

import java.net.URI;
import java.util.Objects;

/** 从受信 URL 安装项目级 Skill 的系统工具。 */
public final class InstallSkillTool extends BaseTool implements PermissionTargetProvider {
    private final SkillInstaller installer;
    private final SkillInstallListener listener;

    public InstallSkillTool(
            SkillInstaller installer,
            SkillInstallListener listener,
            ToolLimits limits,
            SecretRedactor redactor) {
        super(createDefinition(), limits, redactor);
        this.installer = Objects.requireNonNull(installer, "installer");
        this.listener = listener == null ? SkillInstallListener.NOOP : listener;
    }

    @Override
    protected ToolResult executeValidated(ObjectNode arguments) {
        rejectUnknownFields(arguments, "url", "force");
        String url = requireText(arguments, "url");
        JsonNode forceNode = arguments.get("force");
        if (forceNode != null && !forceNode.isNull() && !forceNode.isBoolean()) {
            throw new IllegalArgumentException("参数 force 必须是布尔值");
        }
        boolean force = forceNode != null && forceNode.asBoolean(false);
        URI source;
        try {
            source = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("参数 url 不是有效 URI");
        }
        SkillInstallResult result = installer.install(new SkillInstallRequest(source, force), listener);
        return ToolResult.success("已安装 Skill /" + result.skillName()
                + " 到 " + result.installedPath() + "，无需重启即可使用");
    }

    @Override public PermissionOperation permissionOperation() { return PermissionOperation.WRITE; }

    @Override public String permissionTarget(ObjectNode arguments) {
        return ".imiocode/skills/.install-request";
    }

    @Override public ToolRisk permissionRisk(ObjectNode arguments, ToolRisk fallback) {
        JsonNode force = arguments == null ? null : arguments.get("force");
        return force != null && force.isBoolean() && force.booleanValue() ? ToolRisk.HIGH : fallback;
    }

    @Override public void cancel() { installer.cancel(); }

    private static ToolDefinition createDefinition() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("url").put("type", "string")
                .put("description", "skills.sh、GitHub tree 或 Raw SKILL.md 的 HTTPS 地址");
        properties.putObject("force").put("type", "boolean").put("default", false)
                .put("description", "显式覆盖已有同名 Skill；覆盖属于高风险写操作");
        schema.putArray("required").add("url");
        schema.put("additionalProperties", false);
        return new ToolDefinition(
                SkillActivator.INSTALL_SKILL_TOOL,
                "从受信 HTTPS URL 安装项目级 Skill。优先使用本工具，不要调用 npx、npm、git 或 Bash 安装；"
                        + "安装后会自动热刷新。仅在用户明确要求覆盖时设置 force=true。",
                schema,
                ToolRisk.MEDIUM);
    }
}
