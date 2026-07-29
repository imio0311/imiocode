package io.imiocode.tool.core;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.BaseTool;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.workspace.AtomicFileWriter;
import io.imiocode.tool.workspace.Utf8TextFile;
import io.imiocode.tool.workspace.WorkspacePolicy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/** 仅当旧文本恰好出现一次时执行精确替换。 */
public final class EditFileTool extends BaseTool {
    private final WorkspacePolicy policy;
    private final AtomicFileWriter writer;

    public EditFileTool(WorkspacePolicy policy, ToolLimits limits, SecretRedactor redactor) {
        super(createDefinition(), limits, redactor);
        this.policy = Objects.requireNonNull(policy, "policy");
        this.writer = new AtomicFileWriter(policy);
    }

    @Override
    protected ToolResult executeValidated(ObjectNode arguments) throws Exception {
        rejectUnknownFields(arguments, "path", "old_text", "new_text");
        String input = requireText(arguments, "path");
        String oldText = requireString(arguments, "old_text");
        if (oldText.isEmpty()) {
            throw new IllegalArgumentException("参数 old_text 不能为空");
        }
        String newText = requireString(arguments, "new_text");
        Path path = policy.resolveExistingFile(input);
        Utf8TextFile.ReadResult read = Utf8TextFile.read(
                path, limits.maxWriteBytes() + 1, Integer.MAX_VALUE, Thread.currentThread()::isInterrupted);
        if (read.truncated()) {
            throw new IllegalArgumentException("文件超过 1 MiB 编辑限制");
        }
        String source = read.text();
        int occurrences = countNonOverlapping(source, oldText);
        if (occurrences != 1) {
            throw new IllegalArgumentException(
                    occurrences == 0 ? "旧文本未找到" : "旧文本出现多次，无法唯一替换");
        }
        String updated = source.replace(oldText, newText);
        if (updated.getBytes(StandardCharsets.UTF_8).length > limits.maxWriteBytes()) {
            throw new IllegalArgumentException("编辑结果超过 1 MiB 限制");
        }
        writer.write(input, updated);
        return ToolResult.success("已编辑 " + input);
    }

    private static int countNonOverlapping(String source, String needle) {
        int count = 0;
        for (int offset = 0; (offset = source.indexOf(needle, offset)) >= 0; offset += needle.length()) {
            count++;
        }
        return count;
    }

    private static ToolDefinition createDefinition() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("path").put("type", "string");
        properties.putObject("old_text").put("type", "string");
        properties.putObject("new_text").put("type", "string");
        schema.putArray("required").add("path").add("old_text").add("new_text");
        schema.put("additionalProperties", false);
        return new ToolDefinition(
                "edit_file",
                "对工作区文件执行一次精确文本替换，旧文本必须唯一匹配。"
                        + "调用前先用 read_file 读取上下文，并提供原样、完整的旧文本。",
                schema,
                ToolRisk.MEDIUM);
    }
}
