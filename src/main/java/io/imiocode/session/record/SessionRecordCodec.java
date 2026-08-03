package io.imiocode.session.record;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.imiocode.session.SessionException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** 版本化 JSONL 记录的稳定单行编解码。 */
public final class SessionRecordCodec {
    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

    public String encode(Object record) {
        try {
            return mapper.writeValueAsString(record);
        } catch (JsonProcessingException exception) {
            throw new SessionException("无法编码会话记录", exception);
        }
    }

    public Object decode(String line) {
        try {
            JsonNode node = mapper.readTree(line);
            if (node == null || !node.isObject()) throw new SessionException("会话记录必须是 JSON 对象");
            String type = required(node, "type");
            return switch (type) {
                case "session_header" -> mapper.treeToValue(node, SessionHeaderRecord.class);
                case "transaction_begin" -> mapper.treeToValue(node, TransactionBeginRecord.class);
                case "message" -> mapper.treeToValue(node, MessageRecord.class);
                case "transaction_commit" -> mapper.treeToValue(node, TransactionCommitRecord.class);
                default -> throw new SessionException("未知会话记录类型");
            };
        } catch (JsonProcessingException exception) {
            throw new SessionException("会话 JSONL 格式损坏", exception);
        }
    }

    public String transactionSha256(List<String> payloadLines) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String line : payloadLines) {
                digest.update(line.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK 缺少 SHA-256", exception);
        }
    }

    private static String required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new SessionException("会话记录缺少字段 " + field);
        }
        return value.asText();
    }
}
