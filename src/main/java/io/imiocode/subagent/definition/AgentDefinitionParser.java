package io.imiocode.subagent.definition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.imiocode.permission.PermissionMode;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** YAML frontmatter + Markdown body 的严格 Agent 定义解析器。 */
public final class AgentDefinitionParser {
    private static final Set<String> FIELDS = Set.of(
            "name", "description", "model", "permissionMode", "permission-mode", "permission_mode",
            "maxTurns", "max-turns", "max_turns", "timeoutSeconds", "timeout-seconds", "timeout_seconds",
            "tools", "disallowedTools", "disallowed-tools", "disallowed_tools", "backgroundAllowed",
            "background-allowed", "background_allowed", "initialPrompt", "initial-prompt", "initial_prompt",
            "skills", "mcpServers", "mcp-servers", "mcp_servers", "hooks", "memory", "isolation");
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public AgentDefinition parse(String markdown, AgentDefinitionSource source, Path path) {
        Split split = split(markdown, path == null ? source.name() : path.toString());
        try {
            JsonNode node = yaml.readTree(split.frontmatter());
            if (node == null || !node.isObject()) throw new AgentDefinitionException("Agent frontmatter 必须是 YAML 对象");
            node.fieldNames().forEachRemaining(field -> {
                if (!FIELDS.contains(field)) throw new AgentDefinitionException("未知 Agent 字段: " + field);
            });
            String permission = text(node, "permissionMode", "permission-mode", "permission_mode");
            Integer maxTurns = integer(node, "maxTurns", "max-turns", "max_turns");
            Integer timeout = integer(node, "timeoutSeconds", "timeout-seconds", "timeout_seconds");
            return new AgentDefinition(required(node, "name"), required(node, "description"), split.body(),
                    Optional.ofNullable(text(node, "model")),
                    permission == null ? PermissionMode.AUTO_EDIT : PermissionMode.parse(permission),
                    maxTurns == null ? 20 : maxTurns,
                    Duration.ofSeconds(timeout == null ? 600 : timeout),
                    strings(first(node, "tools"), "tools"),
                    strings(first(node, "disallowedTools", "disallowed-tools", "disallowed_tools"), "disallowedTools"),
                    bool(node, true, "backgroundAllowed", "background-allowed", "background_allowed"),
                    Optional.ofNullable(text(node, "initialPrompt", "initial-prompt", "initial_prompt")),
                    List.copyOf(strings(first(node, "skills"), "skills")),
                    List.copyOf(strings(first(node, "mcpServers", "mcp-servers", "mcp_servers"), "mcpServers")),
                    List.copyOf(strings(first(node, "hooks"), "hooks")),
                    bool(node, false, "memory"), AgentIsolation.parse(text(node, "isolation")), source,
                    Optional.ofNullable(path));
        } catch (AgentDefinitionException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new AgentDefinitionException("无法解析 Agent 定义: " + safe(exception), exception);
        }
    }

    private static Split split(String markdown, String source) {
        if (markdown == null) throw new AgentDefinitionException("Agent 内容为空: " + source);
        String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n');
        if (!normalized.startsWith("---\n")) throw new AgentDefinitionException("Agent 缺少 YAML frontmatter: " + source);
        int end = normalized.indexOf("\n---\n", 4);
        if (end < 0) throw new AgentDefinitionException("Agent frontmatter 未闭合: " + source);
        String body = normalized.substring(end + 5).trim();
        if (body.isBlank()) throw new AgentDefinitionException("Agent Markdown 正文不能为空: " + source);
        return new Split(normalized.substring(4, end), body);
    }
    private static JsonNode first(JsonNode node, String... names) {
        for (String name : names) if (node.has(name)) return node.get(name);
        return null;
    }
    private static String text(JsonNode node, String... names) {
        JsonNode value = first(node, names);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw new AgentDefinitionException("字段 " + names[0] + " 必须是字符串");
        return value.textValue().trim();
    }
    private static String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) throw new AgentDefinitionException("字段 " + field + " 必须是非空字符串");
        return value;
    }
    private static Integer integer(JsonNode node, String... names) {
        JsonNode value = first(node, names);
        if (value == null || value.isNull()) return null;
        if (!value.canConvertToInt()) throw new AgentDefinitionException("字段 " + names[0] + " 必须是整数");
        return value.intValue();
    }
    private static boolean bool(JsonNode node, boolean fallback, String... names) {
        JsonNode value = first(node, names);
        if (value == null || value.isNull()) return fallback;
        if (!value.isBoolean()) throw new AgentDefinitionException("字段 " + names[0] + " 必须是布尔值");
        return value.booleanValue();
    }
    private static Set<String> strings(JsonNode node, String field) {
        if (node == null || node.isNull()) return Set.of();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (node.isTextual()) values.add(node.textValue());
        else if (node.isArray()) for (JsonNode item : node) {
            if (!item.isTextual() || item.textValue().isBlank())
                throw new AgentDefinitionException("字段 " + field + " 必须是字符串数组");
            values.add(item.textValue().trim());
        }
        else throw new AgentDefinitionException("字段 " + field + " 必须是字符串或字符串数组");
        return Set.copyOf(values);
    }
    private static String safe(Throwable exception) {
        return exception.getMessage() == null ? "配置无效" : exception.getMessage();
    }
    private record Split(String frontmatter, String body) { }
}
