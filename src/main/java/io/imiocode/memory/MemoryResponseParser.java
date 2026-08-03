package io.imiocode.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** 只接受唯一 memories XML 边界内的严格 JSON。 */
public final class MemoryResponseParser {
    private static final String OPEN = "<memories>";
    private static final String CLOSE = "</memories>";
    private static final Set<String> ITEM_FIELDS = Set.of("scope", "category", "content", "replaces_id");
    private final ObjectMapper mapper = new ObjectMapper();

    public List<MemoryCandidate> parse(String response) {
        if (response == null) throw new MemoryException("记忆提取响应为空");
        int start = response.indexOf(OPEN), end = response.indexOf(CLOSE);
        if (start < 0 || end < start || response.indexOf(OPEN, start + OPEN.length()) >= 0
                || response.indexOf(CLOSE, end + CLOSE.length()) >= 0) {
            throw new MemoryException("记忆提取响应缺少唯一 memories 边界");
        }
        try {
            JsonNode root = mapper.readTree(response.substring(start + OPEN.length(), end).trim());
            if (root == null || !root.isObject() || root.size() != 1 || !root.has("items") || !root.get("items").isArray()) {
                throw new MemoryException("记忆提取 JSON 必须只包含 items 数组");
            }
            if (root.get("items").size() > 20) throw new MemoryException("单轮记忆候选过多");
            List<MemoryCandidate> result = new ArrayList<>();
            for (JsonNode item : root.get("items")) {
                if (!item.isObject()) throw new MemoryException("记忆候选必须是对象");
                Set<String> fields = new HashSet<>(); item.fieldNames().forEachRemaining(fields::add);
                if (!ITEM_FIELDS.containsAll(fields)) throw new MemoryException("记忆候选包含未知字段");
                MemoryScope scope = MemoryScope.parse(required(item, "scope"));
                MemoryCategory category;
                try { category = MemoryCategory.valueOf(required(item, "category").toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException exception) { throw new MemoryException("未知记忆类别", exception); }
                result.add(new MemoryCandidate(scope, category, required(item, "content"),
                        Optional.ofNullable(optional(item, "replaces_id"))));
            }
            return List.copyOf(result);
        } catch (JsonProcessingException exception) { throw new MemoryException("记忆提取 JSON 无法解析", exception); }
    }

    private static String required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) throw new MemoryException("记忆候选缺少 " + field);
        return value.asText();
    }

    private static String optional(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }
}
