package io.imiocode.instruction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 展开 MEWCODE.md 中独占一行的安全相对 include。 */
public final class IncludeExpander {
    private static final Pattern INCLUDE = Pattern.compile("^\\s*@include\\s+(.+?)\\s*$");

    public Expansion expand(Path source, Path allowedRoot, int maxDepth, long maxBytes) throws IOException {
        Path rootReal = allowedRoot.toRealPath();
        State state = new State(rootReal, maxDepth, maxBytes);
        String content = expandFile(source, 0, state);
        long bytes = content.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > maxBytes) throw new IOException("展开内容超过 " + maxBytes + " 字节");
        return new Expansion(content.strip(), state.dependencies, bytes);
    }

    private String expandFile(Path source, int depth, State state) throws IOException {
        if (depth > state.maxDepth) throw new IOException("include 深度超过 " + state.maxDepth);
        Path normalized = source.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) throw new IOException("include 目标不是普通文件");
        Path real = normalized.toRealPath();
        if (!real.startsWith(state.allowedRoot)) throw new IOException("include 路径超出允许范围");
        if (state.stack.contains(real)) throw new IOException("检测到 include 循环");
        state.stack.push(real);
        state.dependencies.add(real);
        try {
            List<String> lines = Files.readAllLines(real, StandardCharsets.UTF_8);
            StringBuilder result = new StringBuilder();
            for (String line : lines) {
                Matcher matcher = INCLUDE.matcher(line);
                String expanded = line;
                if (matcher.matches()) {
                    Path child = real.getParent().resolve(parseRelativePath(matcher.group(1))).normalize();
                    if (!child.startsWith(state.allowedRoot)) throw new IOException("include 路径超出允许范围");
                    expanded = expandFile(child, depth + 1, state).stripTrailing();
                }
                appendMeasured(result, expanded, state);
                appendMeasured(result, "\n", state);
            }
            String value = result.toString();
            String lower = value.toLowerCase(Locale.ROOT);
            if (lower.contains("<system-reminder") || lower.contains("</system-reminder>")) {
                throw new IOException("指令正文不能包含保留的 system-reminder 标签");
            }
            return value;
        } finally {
            state.stack.pop();
        }
    }

    private static Path parseRelativePath(String token) throws IOException {
        String value = token.trim();
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            value = value.substring(1, value.length() - 1);
        } else if (value.startsWith("\"") || value.endsWith("\"") || value.startsWith("'") || value.endsWith("'")) {
            throw new IOException("include 路径引号未闭合");
        }
        if (value.isBlank()) throw new IOException("include 路径不能为空");
        Path path;
        try { path = Path.of(value); } catch (RuntimeException exception) { throw new IOException("include 路径格式无效", exception); }
        if (path.isAbsolute()) throw new IOException("include 不允许绝对路径");
        for (Path part : path) if ("..".equals(part.toString())) throw new IOException("include 不允许 .. 路径段");
        return path;
    }

    private static void appendMeasured(StringBuilder target, String value, State state) throws IOException {
        target.append(value);
        if (target.toString().getBytes(StandardCharsets.UTF_8).length > state.maxBytes) {
            throw new IOException("展开内容超过 " + state.maxBytes + " 字节");
        }
    }

    public record Expansion(String content, Set<Path> dependencies, long bytes) {
        public Expansion { dependencies = Set.copyOf(dependencies); }
    }

    private static final class State {
        private final Path allowedRoot;
        private final int maxDepth;
        private final long maxBytes;
        private final ArrayDeque<Path> stack = new ArrayDeque<>();
        private final LinkedHashSet<Path> dependencies = new LinkedHashSet<>();
        private State(Path allowedRoot, int maxDepth, long maxBytes) {
            this.allowedRoot = allowedRoot; this.maxDepth = maxDepth; this.maxBytes = maxBytes;
        }
    }
}
