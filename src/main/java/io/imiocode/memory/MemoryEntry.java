package io.imiocode.memory;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

public record MemoryEntry(String id, MemoryCategory category, String content) {
    private static final Pattern ID = Pattern.compile("m_[a-f0-9]{12}");
    private static final SecureRandom RANDOM = new SecureRandom();

    public MemoryEntry {
        if (id == null || !ID.matcher(id).matches()) throw new IllegalArgumentException("记忆 ID 格式无效");
        Objects.requireNonNull(category, "category");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("记忆内容不能为空");
        content = normalizeLine(content);
    }

    public static MemoryEntry create(MemoryCategory category, String content) {
        byte[] bytes = new byte[6]; RANDOM.nextBytes(bytes);
        return new MemoryEntry("m_" + HexFormat.of().formatHex(bytes), category, content);
    }

    static String normalizeLine(String content) {
        return content.strip().replaceAll("\\s*\\R\\s*", " ").replaceAll("[ \\t]+", " ");
    }
}
