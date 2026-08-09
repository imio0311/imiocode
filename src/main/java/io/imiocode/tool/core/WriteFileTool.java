package io.imiocode.tool.core;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.BaseTool;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolLifecycleListener;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.workspace.AtomicFileWriter;
import io.imiocode.tool.workspace.WorkspacePolicy;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** 创建或完整覆盖工作区内的 UTF-8 文本文件。 */
public final class WriteFileTool extends BaseTool {
    private final WorkspacePolicy policy;
    private final AtomicFileWriter writer;
    private final ToolLifecycleListener lifecycle;

    public WriteFileTool(WorkspacePolicy policy, ToolLimits limits, SecretRedactor redactor) {
        this(policy, limits, redactor, ToolLifecycleListener.NOOP);
    }

    public WriteFileTool(WorkspacePolicy policy, ToolLimits limits, SecretRedactor redactor,
                         ToolLifecycleListener lifecycle) {
        super(createDefinition(), limits, redactor);
        this.policy = Objects.requireNonNull(policy, "policy");
        this.writer = new AtomicFileWriter(policy);
        this.lifecycle = Objects.requireNonNullElse(lifecycle, ToolLifecycleListener.NOOP);
    }

    @Override
    protected ToolResult executeValidated(ObjectNode arguments) throws Exception {
        rejectUnknownFields(arguments, "path", "content");
        String path = requireText(arguments, "path");
        String content = requireString(arguments, "content");
        long bytes = content.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > limits.maxWriteBytes()) {
            throw new IllegalArgumentException("写入内容超过 1 MiB 限制");
        }
        policy.resolveWritableFile(path);
        writer.write(path, content);
        lifecycle.onFileChanged("write_file", policy.workspace().resolve(path).normalize());
        return ToolResult.success("已写入 " + path + "（" + bytes + " 字节）");
    }

    private static ToolDefinition createDefinition() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("path").put("type", "string").put("description", "工作区相对文件路径");
        properties.putObject("content").put("type", "string").put("description", "完整 UTF-8 文件内容");
        schema.putArray("required").add("path").add("content");
        schema.put("additionalProperties", false);
        return new ToolDefinition(
                "write_file",
                "创建新文件或完整覆盖工作区内的文件。已有文件的局部修改优先使用 edit_file；"
                        + "覆盖前必须确认已经读取并理解现有内容。",
                schema,
                ToolRisk.MEDIUM);
    }
}
