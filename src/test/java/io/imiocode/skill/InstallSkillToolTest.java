package io.imiocode.skill;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.permission.PermissionOperation;
import io.imiocode.permission.PermissionRequest;
import io.imiocode.permission.PermissionRequestFactory;
import io.imiocode.skill.install.SkillInstallListener;
import io.imiocode.skill.install.SkillInstallRequest;
import io.imiocode.skill.install.SkillInstallResult;
import io.imiocode.skill.install.SkillInstallStage;
import io.imiocode.skill.install.SkillInstaller;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallSkillToolTest {
    @Test
    void installsWithStrictSchemaAndSupportsCancellation() {
        StubInstaller installer = new StubInstaller();
        InstallSkillTool tool = new InstallSkillTool(
                installer, SkillInstallListener.NOOP, ToolLimits.defaults(), new SecretRedactor(""));
        ObjectNode args = JsonNodeFactory.instance.objectNode()
                .put("url", "https://skills.sh/acme/repo/demo")
                .put("force", false);

        ToolResult result = tool.execute(args);

        assertTrue(result.success());
        assertTrue(result.output().contains("/demo"));
        assertFalse(installer.request.force());
        assertFalse(tool.execute(args.deepCopy().put("extra", 1)).success());
        tool.cancel();
        assertTrue(installer.cancelled);
    }

    @Test
    void exposesWorkspaceWriteTargetAndForceAsHighRisk() {
        InstallSkillTool tool = new InstallSkillTool(
                new StubInstaller(), SkillInstallListener.NOOP, ToolLimits.defaults(), new SecretRedactor(""));
        ObjectNode args = JsonNodeFactory.instance.objectNode()
                .put("url", "https://skills.sh/acme/repo/demo").put("force", true);
        ToolCall call = new ToolCall("call-1", "install_skill", args);
        PermissionRequest request = new PermissionRequestFactory(new SecretRedactor(""))
                .create(call, tool);

        assertEquals(PermissionOperation.WRITE, request.operation());
        assertEquals(ToolRisk.HIGH, request.risk());
        assertEquals(".imiocode/skills/.install-request", request.normalizedTarget());
    }

    private static final class StubInstaller implements SkillInstaller {
        private SkillInstallRequest request;
        private boolean cancelled;
        @Override public SkillInstallResult install(SkillInstallRequest request, SkillInstallListener listener) {
            this.request = request;
            return new SkillInstallResult("demo", Path.of(".imiocode/skills/demo"), SkillOrigin.PROJECT,
                    false, List.of(SkillInstallStage.COMPLETED));
        }
        @Override public void cancel() { cancelled = true; }
    }
}
