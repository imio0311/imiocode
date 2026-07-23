package io.imiocode.tool.core;

import java.util.regex.Pattern;

/** 把统一使用正斜杠的 Glob 模式转换成跨平台正则。 */
public final class GlobPattern {
    private final Pattern regex;

    private GlobPattern(Pattern regex) {
        this.regex = regex;
    }

    public static GlobPattern compile(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("Glob 模式不能为空");
        }
        String normalized = pattern.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("\0")) {
            throw new IllegalArgumentException("Glob 模式无效");
        }
        StringBuilder regex = new StringBuilder("^");
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current == '*') {
                boolean doubleStar = index + 1 < normalized.length() && normalized.charAt(index + 1) == '*';
                if (doubleStar) {
                    index++;
                    if (index + 1 < normalized.length() && normalized.charAt(index + 1) == '/') {
                        index++;
                        regex.append("(?:.*/)?");
                    } else {
                        regex.append(".*");
                    }
                } else {
                    regex.append("[^/]*");
                }
            } else if (current == '?') {
                regex.append("[^/]");
            } else if (current == '/') {
                regex.append('/');
            } else {
                if (".[]{}()+-^$|".indexOf(current) >= 0) {
                    regex.append('\\');
                }
                regex.append(current);
            }
        }
        regex.append('$');
        return new GlobPattern(Pattern.compile(regex.toString()));
    }

    public boolean matches(String unixPath) {
        return regex.matcher(unixPath.replace('\\', '/')).matches();
    }
}
