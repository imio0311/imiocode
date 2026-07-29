package io.imiocode.tool.core;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.BaseTool;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.workspace.WorkspacePolicy;
import io.imiocode.tool.workspace.WorkspaceWalker;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 在工作区内按稳定顺序匹配文件路径。 */
public final class GlobTool extends BaseTool {
    private final WorkspacePolicy policy;
    private final WorkspaceWalker walker;

    public GlobTool(WorkspacePolicy policy, ToolLimits limits, SecretRedactor redactor) {
        super(createDefinition(), limits, redactor);
        this.policy = Objects.requireNonNull(policy, "policy");
        this.walker = new WorkspaceWalker(policy);
    }

    @Override
    protected ToolResult executeValidated(ObjectNode arguments) throws Exception {
        rejectUnknownFields(arguments, "pattern");
        GlobPattern pattern = GlobPattern.compile(requireText(arguments, "pattern"));
        WorkspaceWalker.WalkResult walk = walker.walk(
                policy.workspace(), limits.maxScannedPaths(), Thread.currentThread()::isInterrupted);
        List<String> matches = new ArrayList<>();
        boolean truncated = walk.truncated();
        for (Path path : walk.paths()) {
            if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    && pattern.matches(policy.relativeUnixPath(path))) {
                matches.add(policy.relativeUnixPath(path));
            }
        }
        matches.sort(String::compareTo);
        if (matches.size() > limits.maxGlobResults()) {
            matches = new ArrayList<>(matches.subList(0, limits.maxGlobResults()));
            truncated = true;
        }
        String output = String.join("\n", matches);
        if (truncated) {
            output += BaseTool.TRUNCATION_MARKER;
        }
        return ToolResult.success(output, truncated, null);
    }

    private static ToolDefinition createDefinition() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("pattern")
                .put("type", "string")
                .put("description", "使用 / 分隔的 Glob 模式");
        schema.putArray("required").add("pattern");
        schema.put("additionalProperties", false);
        return new ToolDefinition(
                "glob",
                "按文件名或路径 Glob 模式发现工作区中的候选文件，适合作为探索项目结构的第一步。"
                        + "定位后配合 grep 搜索内容、read_file 阅读上下文。",
                schema,
                ToolRisk.LOW);
    }
}
