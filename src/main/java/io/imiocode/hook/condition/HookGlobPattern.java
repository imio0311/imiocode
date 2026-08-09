package io.imiocode.hook.condition;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** 使用统一正斜杠语义的轻量 glob，支持 *、**、?。 */
public final class HookGlobPattern {
    private final String source;
    private final Pattern pattern;

    private HookGlobPattern(String source, Pattern pattern) {
        this.source = source;
        this.pattern = pattern;
    }

    public static HookGlobPattern compile(String glob) {
        if (glob == null || glob.isEmpty()) throw new IllegalArgumentException("glob 不能为空");
        String normalized = normalize(glob);
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch == '*') {
                if (i + 1 < normalized.length() && normalized.charAt(i + 1) == '*') {
                    i++;
                    if (i + 1 < normalized.length() && normalized.charAt(i + 1) == '/') {
                        i++;
                        regex.append("(?:.*/)?");
                    } else regex.append(".*");
                } else regex.append("[^/]*");
            } else if (ch == '?') {
                regex.append("[^/]");
            } else {
                if (".()[]{}+$^|".indexOf(ch) >= 0) regex.append('\\');
                regex.append(ch);
            }
        }
        regex.append('$');
        try {
            return new HookGlobPattern(glob, Pattern.compile(regex.toString()));
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("非法 glob: " + glob, exception);
        }
    }

    public boolean matches(String value) {
        return pattern.matcher(normalize(Objects.requireNonNullElse(value, ""))).matches();
    }

    public String source() { return source; }
    private static String normalize(String value) { return value.replace('\\', '/'); }
}
