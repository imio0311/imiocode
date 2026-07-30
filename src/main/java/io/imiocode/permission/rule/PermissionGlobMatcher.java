package io.imiocode.permission.rule;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** 使用统一斜杠语义的权限 Glob 匹配器。 */
public final class PermissionGlobMatcher {
    public Pattern compile(String glob) {
        if (glob == null || glob.isBlank()) {
            throw new IllegalArgumentException("权限 glob 不能为空");
        }
        String normalized = glob.trim().replace('\\', '/');
        StringBuilder regex = new StringBuilder("^");
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current == '*') {
                if (index + 1 < normalized.length() && normalized.charAt(index + 1) == '*') {
                    regex.append(".*");
                    index++;
                } else {
                    regex.append("[^/]*");
                }
            } else if (current == '?') {
                regex.append("[^/]");
            } else {
                if ("\\.[]{}()+-^$|".indexOf(current) >= 0) {
                    regex.append('\\');
                }
                regex.append(current);
            }
        }
        regex.append('$');
        try {
            return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("权限 glob 无效");
        }
    }

    public boolean matches(String glob, String value) {
        Objects.requireNonNull(value, "匹配目标不能为空");
        return compile(glob).matcher(value.replace('\\', '/')).matches();
    }
}
