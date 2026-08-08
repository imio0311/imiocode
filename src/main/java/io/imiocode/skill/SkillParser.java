package io.imiocode.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.imiocode.tool.ToolRisk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** YAML frontmatter、Markdown 正文和 tool.json 的统一解析器。 */
public final class SkillParser {
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper json = new ObjectMapper();

    public SkillDescriptor parseDescriptor(SkillSource source, SkillOrigin origin) {
        try {
            JsonNode node = yaml.readTree(source.readFrontmatter());
            return new SkillDescriptor(parseMetadata(node), origin, source);
        } catch (SkillException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new SkillException("无法解析 Skill 元信息: " + source.id(), exception);
        }
    }

    public LoadedSkill parseLoaded(SkillDescriptor descriptor, String arguments) {
        try {
            SplitMarkdown split = split(descriptor.source().readMarkdown(), descriptor.source().id());
            SkillMetadata latest = parseMetadata(yaml.readTree(split.frontmatter()));
            if (!latest.name().equals(descriptor.metadata().name())) {
                throw new SkillException("Skill 名称在加载期间发生变化，请先 reload: " + descriptor.source().id());
            }
            String prompt = split.body().replace("$ARGUMENTS", arguments == null ? "" : arguments);
            List<SkillToolSpec> tools = parseTools(
                    descriptor.metadata().name(), descriptor.source().readToolJson().orElse(null));
            Set<String> allowed = new LinkedHashSet<>();
            for (String name : latest.allowedTools()) {
                String effective = tools.stream()
                        .filter(tool -> tool.name().equals(name))
                        .map(SkillToolSpec::exposedName)
                        .findFirst().orElse(name);
                allowed.add(effective);
            }
            tools.forEach(tool -> allowed.add(tool.exposedName()));
            return new LoadedSkill(
                    new SkillDescriptor(latest, descriptor.origin(), descriptor.source()),
                    prompt,
                    descriptor.source().readReferences(),
                    tools,
                    allowed);
        } catch (SkillException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new SkillException("无法完整加载 Skill: " + descriptor.source().id(), exception);
        }
    }

    private SkillMetadata parseMetadata(JsonNode node) {
        if (node == null || !node.isObject()) throw new SkillException("Skill frontmatter 必须是 YAML 对象");
        String name = requiredText(node, "name");
        String description = requiredText(node, "description");
        SkillMode mode = enumValue(node.path("mode").asText("inline"), SkillMode.class, "mode");
        String historyText = firstText(node, "history", "context", "contextMode", "context-mode");
        SkillHistoryMode history = enumValue(
                historyText == null ? "recent" : historyText, SkillHistoryMode.class, "history");
        Set<String> aliases = stringSet(node.get("aliases"), "aliases");
        JsonNode allowedNode = first(node, "allowedTools", "allowed-tools", "allowed_tools");
        Set<String> allowed = stringSet(allowedNode, "allowedTools");
        return new SkillMetadata(name, description, mode, history, aliases, allowed);
    }

    private List<SkillToolSpec> parseTools(String skillName, String content) throws IOException {
        if (content == null || content.isBlank()) return List.of();
        JsonNode root = json.readTree(content);
        JsonNode list = root;
        if (root.isObject() && root.has("tools")) list = root.get("tools");
        List<JsonNode> entries = new ArrayList<>();
        if (list.isArray()) list.forEach(entries::add); else if (list.isObject()) entries.add(list);
        else throw new SkillException("tool.json 必须是工具对象、数组或包含 tools 数组的对象");
        List<SkillToolSpec> result = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();
        for (JsonNode entry : entries) {
            String name = requiredText(entry, "name");
            if (!names.add(name)) throw new SkillException("tool.json 工具名称重复: " + name);
            String exposed = "skill_" + skillName.replace('-', '_') + "_" + name;
            JsonNode schemaNode = entry.get("inputSchema");
            if (schemaNode == null) schemaNode = entry.get("input_schema");
            ObjectNode schema = schemaNode instanceof ObjectNode object
                    ? object.deepCopy() : JsonNodeFactory.instance.objectNode().put("type", "object");
            if (!schema.has("type")) schema.put("type", "object");
            if (!"object".equals(schema.path("type").asText())) {
                throw new SkillException("tool.json inputSchema.type 必须是 object");
            }
            List<String> args = new ArrayList<>();
            JsonNode argsNode = entry.get("args");
            if (argsNode != null) {
                if (!argsNode.isArray()) throw new SkillException("tool.json args 必须是字符串数组");
                for (JsonNode arg : argsNode) {
                    if (!arg.isTextual()) throw new SkillException("tool.json args 只能包含字符串");
                    args.add(arg.textValue());
                }
            }
            ToolRisk risk = enumValue(entry.path("risk").asText("high"), ToolRisk.class, "risk");
            result.add(new SkillToolSpec(name, exposed, requiredText(entry, "description"), schema,
                    requiredText(entry, "command"), args, risk));
        }
        return List.copyOf(result);
    }

    static SplitMarkdown split(String markdown, String source) {
        if (markdown == null) throw new SkillException("Skill 内容为空: " + source);
        String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n');
        if (!normalized.startsWith("---\n")) throw new SkillException("Skill 缺少 YAML frontmatter: " + source);
        int end = normalized.indexOf("\n---\n", 4);
        if (end < 0) throw new SkillException("Skill frontmatter 未闭合: " + source);
        String frontmatter = normalized.substring(4, end);
        String body = normalized.substring(end + 5).trim();
        if (body.isBlank()) throw new SkillException("Skill Markdown 正文不能为空: " + source);
        return new SplitMarkdown(frontmatter, body);
    }

    private static JsonNode first(JsonNode node, String... fields) {
        for (String field : fields) if (node.has(field)) return node.get(field);
        return null;
    }

    private static String firstText(JsonNode node, String... fields) {
        JsonNode value = first(node, fields);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new SkillException("Skill 字段 " + field + " 必须是非空字符串");
        }
        return value.textValue().trim();
    }

    private static Set<String> stringSet(JsonNode node, String field) {
        if (node == null || node.isNull()) return Set.of();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (node.isTextual()) values.add(node.textValue());
        else if (node.isArray()) {
            for (JsonNode item : node) {
                if (!item.isTextual() || item.textValue().isBlank()) {
                    throw new SkillException("Skill 字段 " + field + " 必须是字符串数组");
                }
                values.add(item.textValue());
            }
        } else throw new SkillException("Skill 字段 " + field + " 必须是字符串或字符串数组");
        return Set.copyOf(values);
    }

    private static <T extends Enum<T>> T enumValue(String value, Class<T> type, String field) {
        try { return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT)); }
        catch (RuntimeException exception) { throw new SkillException("Skill 字段 " + field + " 值无效: " + value); }
    }

    record SplitMarkdown(String frontmatter, String body) { }
}
