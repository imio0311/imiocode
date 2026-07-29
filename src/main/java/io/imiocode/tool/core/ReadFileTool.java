package io.imiocode.tool.core;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.BaseTool;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.workspace.Utf8TextFile;
import io.imiocode.tool.workspace.WorkspacePolicy;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** 读取工作区内 UTF-8 文本文件，并输出一基行号。 */
public final class ReadFileTool extends BaseTool {
    private final WorkspacePolicy policy;

    public ReadFileTool(WorkspacePolicy policy, ToolLimits limits, SecretRedactor redactor) {
        super(createDefinition(), limits, redactor);
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    protected ToolResult executeValidated(ObjectNode arguments) throws Exception {
        rejectUnknownFields(arguments, "path", "start_line", "end_line");
        String input = requireText(arguments, "path");
        Integer start = optionalPositiveInt(arguments, "start_line");
        Integer end = optionalPositiveInt(arguments, "end_line");
        int first = start == null ? 1 : start;
        int last = end == null ? Integer.MAX_VALUE : end;
        if (last < first) {
            throw new IllegalArgumentException("end_line 不能小于 start_line");
        }

        Path path = policy.resolveExistingFile(input);
        Utf8TextFile.ReadResult read = Utf8TextFile.read(
                path, limits.maxReadBytes(), limits.maxReadLines(), Thread.currentThread()::isInterrupted);
        List<String> lines = read.lines();
        if (first > lines.size() && !(lines.isEmpty() && first == 1)) {
            throw new IllegalArgumentException("start_line 超出文件范围");
        }
        StringBuilder output = new StringBuilder();
        for (int line = first; line <= Math.min(last, lines.size()); line++) {
            output.append(line).append(": ").append(lines.get(line - 1)).append('\n');
        }
        if (!output.isEmpty()) {
            output.setLength(output.length() - 1);
        }
        if (read.truncated()) {
            output.append(BaseTool.TRUNCATION_MARKER);
        }
        return ToolResult.success(output.toString(), read.truncated(), null);
    }

    private static ToolDefinition createDefinition() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("path").put("type", "string").put("description", "工作区相对文件路径");
        properties.putObject("start_line").put("type", "integer").put("minimum", 1);
        properties.putObject("end_line").put("type", "integer").put("minimum", 1);
        schema.putArray("required").add("path");
        schema.put("additionalProperties", false);
        return new ToolDefinition(
                "read_file",
                "读取工作区内 UTF-8 文本文件的全部或指定行范围。修改文件前应先读取；"
                        + "通常先用 glob/grep 定位目标，再用本工具查看完整上下文。",
                schema,
                ToolRisk.LOW);
    }
}
