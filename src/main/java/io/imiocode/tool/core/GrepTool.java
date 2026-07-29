package io.imiocode.tool.core;

import com.fasterxml.jackson.databind.JsonNode;
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
import io.imiocode.tool.workspace.WorkspaceWalker;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** 在工作区 UTF-8 文本中执行有界正则搜索。 */
public final class GrepTool extends BaseTool {
    private final WorkspacePolicy policy;
    private final WorkspaceWalker walker;

    public GrepTool(WorkspacePolicy policy, ToolLimits limits, SecretRedactor redactor) {
        super(createDefinition(), limits, redactor);
        this.policy = Objects.requireNonNull(policy, "policy");
        this.walker = new WorkspaceWalker(policy);
    }

    @Override
    protected ToolResult executeValidated(ObjectNode arguments) throws Exception {
        rejectUnknownFields(arguments, "pattern", "path");
        Pattern regex;
        try {
            regex = Pattern.compile(requireText(arguments, "pattern"));
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("正则表达式无效");
        }
        JsonNode pathNode = arguments.get("path");
        String input = pathNode == null || pathNode.isNull() ? "." : requireText(arguments, "path");
        Path start = policy.resolveExistingPath(input);
        List<Path> files = new ArrayList<>();
        boolean truncated = false;
        if (Files.isRegularFile(start, LinkOption.NOFOLLOW_LINKS)) {
            files.add(start);
        } else {
            WorkspaceWalker.WalkResult walk = walker.walk(
                    start, limits.maxScannedPaths(), Thread.currentThread()::isInterrupted);
            truncated = walk.truncated();
            walk.paths().stream()
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .forEach(files::add);
        }

        List<String> matches = new ArrayList<>();
        outer:
        for (Path file : files) {
            Utf8TextFile.ReadResult read;
            try {
                read = Utf8TextFile.read(
                        file, limits.maxReadBytes(), limits.maxReadLines(),
                        Thread.currentThread()::isInterrupted);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            truncated |= read.truncated();
            List<String> lines = read.lines();
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                if (regex.matcher(line).find()) {
                    if (matches.size() >= limits.maxGrepResults()) {
                        truncated = true;
                        break outer;
                    }
                    matches.add(policy.relativeUnixPath(file)
                            + ":" + (index + 1) + ":" + limitLine(line));
                }
            }
        }
        String output = String.join("\n", matches);
        if (truncated) {
            output += BaseTool.TRUNCATION_MARKER;
        }
        return ToolResult.success(output, truncated, null);
    }

    private String limitLine(String line) {
        if (line.length() <= limits.maxGrepLineChars()) {
            return line;
        }
        return line.substring(0, limits.maxGrepLineChars()) + "...";
    }

    private static ToolDefinition createDefinition() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("pattern").put("type", "string").put("description", "Java 正则表达式");
        properties.putObject("path").put("type", "string").put("description", "可选文件或目录");
        schema.putArray("required").add("pattern");
        schema.put("additionalProperties", false);
        return new ToolDefinition(
                "grep",
                "用正则表达式搜索工作区文本内容，适合定位符号、调用点和配置。"
                        + "先用 glob 缩小文件范围，命中后用 read_file 查看周边上下文。",
                schema,
                ToolRisk.LOW);
    }
}
