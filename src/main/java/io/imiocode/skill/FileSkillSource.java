package io.imiocode.skill;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 文件系统 Skill；所有附件读取都被限制在包根目录内。 */
final class FileSkillSource implements SkillSource {
    private static final long MAX_REFERENCE_BYTES = 256 * 1024;
    private final Path root;
    private final Path markdown;
    private final boolean directoryPackage;

    FileSkillSource(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        this.directoryPackage = Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS);
        this.root = directoryPackage ? normalized : normalized.getParent();
        this.markdown = directoryPackage ? normalized.resolve("SKILL.md") : normalized;
    }

    @Override public String id() { return markdown.toString(); }

    @Override
    public String readFrontmatter() throws IOException {
        ensureSafe(markdown);
        try (BufferedReader reader = Files.newBufferedReader(markdown, StandardCharsets.UTF_8)) {
            String first = reader.readLine();
            if (!"---".equals(first)) throw new SkillException("Skill 缺少 YAML frontmatter: " + id());
            StringBuilder yaml = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if ("---".equals(line)) return yaml.toString();
                yaml.append(line).append('\n');
            }
            throw new SkillException("Skill frontmatter 未闭合: " + id());
        }
    }

    @Override
    public String readMarkdown() throws IOException {
        ensureSafe(markdown);
        return Files.readString(markdown, StandardCharsets.UTF_8);
    }

    @Override
    public Optional<String> readToolJson() throws IOException {
        if (!directoryPackage) return Optional.empty();
        Path file = root.resolve("tool.json");
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
        ensureSafe(file);
        return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
    }

    @Override
    public List<SkillReference> readReferences() throws IOException {
        if (!directoryPackage) return List.of();
        Path references = root.resolve("references");
        if (!Files.exists(references, LinkOption.NOFOLLOW_LINKS)) return List.of();
        ensureSafe(references);
        List<SkillReference> result = new ArrayList<>();
        long[] total = {0};
        try (var paths = Files.walk(references)) {
            for (Path path : paths.sorted().toList()) {
                if (Files.isSymbolicLink(path)) {
                    throw new SkillException("Skill references 不允许符号链接: " + path.getFileName());
                }
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) continue;
                ensureSafe(path);
                long size = Files.size(path);
                total[0] += size;
                if (total[0] > MAX_REFERENCE_BYTES) {
                    throw new SkillException("Skill references 总大小超过 256 KiB: " + id());
                }
                result.add(new SkillReference(
                        references.relativize(path).toString().replace('\\', '/'),
                        Files.readString(path, StandardCharsets.UTF_8)));
            }
        }
        return List.copyOf(result);
    }

    private void ensureSafe(Path path) throws IOException {
        ensureLexicallyContained(root, path);
        Path normalized = path.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(normalized)) throw new SkillException("Skill 不允许符号链接: " + path);
        Path realRoot = root.toRealPath(LinkOption.NOFOLLOW_LINKS);
        Path real = normalized.toRealPath(LinkOption.NOFOLLOW_LINKS);
        if (!real.startsWith(realRoot)) throw new SkillException("Skill 路径逃逸: " + path);
    }

    static void ensureLexicallyContained(Path root, Path path) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(normalizedRoot)) {
            throw new SkillException("Skill 路径越界: " + path);
        }
    }
}
