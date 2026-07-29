package io.imiocode.tool.core;

import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreToolDescriptionTest {

    @Test
    void descriptionsExplainPriorityAndToolRelationships(@TempDir Path workspace) {
        WorkspacePolicy policy = new WorkspacePolicy(workspace);
        ToolLimits limits = ToolLimits.defaults();
        SecretRedactor redactor = new SecretRedactor("");
        Map<String, String> descriptions = List.<Tool>of(
                        new ReadFileTool(policy, limits, redactor),
                        new WriteFileTool(policy, limits, redactor),
                        new EditFileTool(policy, limits, redactor),
                        new BashTool(policy, limits, redactor),
                        new GlobTool(policy, limits, redactor),
                        new GrepTool(policy, limits, redactor))
                .stream()
                .map(Tool::definition)
                .collect(Collectors.toMap(
                        definition -> definition.name(),
                        definition -> definition.description()));

        assertContains(descriptions, "read_file", "修改文件前", "glob", "grep");
        assertContains(descriptions, "write_file", "完整覆盖", "edit_file");
        assertContains(descriptions, "edit_file", "唯一匹配", "read_file");
        assertContains(descriptions, "bash", "构建", "测试", "不要用它替代");
        assertContains(descriptions, "glob", "第一步", "grep", "read_file");
        assertContains(descriptions, "grep", "正则表达式", "glob", "read_file");
    }

    private static void assertContains(
            Map<String, String> descriptions,
            String toolName,
            String... fragments
    ) {
        String description = descriptions.get(toolName);
        for (String fragment : fragments) {
            assertTrue(description.contains(fragment),
                    () -> toolName + " 的描述缺少：" + fragment);
        }
    }
}
