package io.imiocode.skill.install;

import java.nio.file.Path;
import java.util.Objects;

/** 远程候选包中的一个普通文件。 */
public record RemoteSkillFile(Path relativePath, byte[] content) {
    public RemoteSkillFile {
        relativePath = Objects.requireNonNull(relativePath, "relativePath").normalize();
        if (relativePath.isAbsolute() || relativePath.getNameCount() == 0
                || relativePath.startsWith("..") || relativePath.toString().contains(":")) {
            throw new SkillInstallException("远程 Skill 包含非法文件路径");
        }
        content = Objects.requireNonNull(content, "content").clone();
    }

    @Override public byte[] content() { return content.clone(); }
}
