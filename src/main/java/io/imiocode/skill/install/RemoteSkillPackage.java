package io.imiocode.skill.install;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 下载完成、尚未写入工作区的 Skill 候选包。 */
public record RemoteSkillPackage(URI source, List<RemoteSkillFile> files) {
    public RemoteSkillPackage {
        source = Objects.requireNonNull(source, "source");
        files = List.copyOf(Objects.requireNonNullElse(files, List.of()));
        if (files.isEmpty()) throw new SkillInstallException("远程 Skill 目录为空");
        Set<Path> names = new HashSet<>();
        for (RemoteSkillFile file : files) {
            if (!names.add(file.relativePath())) throw new SkillInstallException("远程 Skill 文件路径重复");
        }
        if (!names.contains(Path.of("SKILL.md"))) throw new SkillInstallException("远程目录缺少 SKILL.md");
    }
}
