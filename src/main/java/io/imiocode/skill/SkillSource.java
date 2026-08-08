package io.imiocode.skill;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/** Skill 内容来源，支持文件系统和 classpath 两种实现。 */
public interface SkillSource {
    String id();

    String readFrontmatter() throws IOException;

    String readMarkdown() throws IOException;

    Optional<String> readToolJson() throws IOException;

    List<SkillReference> readReferences() throws IOException;
}
