package io.imiocode.skill;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/** 随应用发布的内置 Skill 资源。 */
final class ClasspathSkillSource implements SkillSource {
    private final ClassLoader loader;
    private final String root;

    ClasspathSkillSource(ClassLoader loader, String name) {
        this.loader = loader;
        this.root = "skills/" + name + "/";
    }

    @Override public String id() { return "classpath:" + root + "SKILL.md"; }

    @Override
    public String readFrontmatter() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                open("SKILL.md"), StandardCharsets.UTF_8))) {
            if (!"---".equals(reader.readLine())) throw new SkillException("内置 Skill 缺少 frontmatter: " + id());
            StringBuilder yaml = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if ("---".equals(line)) return yaml.toString();
                yaml.append(line).append('\n');
            }
            throw new SkillException("内置 Skill frontmatter 未闭合: " + id());
        }
    }

    @Override public String readMarkdown() throws IOException { return read("SKILL.md"); }

    @Override
    public Optional<String> readToolJson() throws IOException {
        InputStream stream = loader.getResourceAsStream(root + "tool.json");
        if (stream == null) return Optional.empty();
        try (stream) { return Optional.of(new String(stream.readAllBytes(), StandardCharsets.UTF_8)); }
    }

    @Override public List<SkillReference> readReferences() { return List.of(); }

    private String read(String relative) throws IOException {
        try (InputStream stream = open(relative)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private InputStream open(String relative) {
        InputStream stream = loader.getResourceAsStream(root + relative);
        if (stream == null) throw new SkillException("内置 Skill 资源不存在: " + root + relative);
        return stream;
    }
}
