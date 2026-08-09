package io.imiocode.worktree.security;

import io.imiocode.worktree.WorktreeException;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.regex.Pattern;

/** LLM 输入不可信：slug 不做容错替换，只接受一个严格的 ASCII 名称。 */
public final class WorktreeSlugValidator {
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private final SecureRandom random;

    public WorktreeSlugValidator() { this(new SecureRandom()); }
    WorktreeSlugValidator(SecureRandom random) { this.random = random; }

    public String validate(String input) {
        if (input == null || input.isBlank()) throw new WorktreeException("Worktree slug 不能为空");
        String value = input.trim();
        if (!value.equals(input) || !ALLOWED.matcher(value).matches() || value.endsWith(".")
                || value.contains("..") || value.contains("/") || value.contains("\\")
                || value.indexOf(':') >= 0) {
            throw new WorktreeException("Worktree slug 只能包含字母、数字、点、下划线和连字符，长度为 1..64");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public String uniqueAgentSlug(String agentName) {
        String safe = validate(agentName);
        int maxName = Math.min(46, safe.length());
        String prefix = "agent-" + safe.substring(0, maxName) + "-";
        byte[] bytes = new byte[4];
        random.nextBytes(bytes);
        StringBuilder suffix = new StringBuilder(8);
        for (byte value : bytes) {
            suffix.append(HEX[(value >>> 4) & 0xf]).append(HEX[value & 0xf]);
        }
        return validate(prefix + suffix);
    }
}
