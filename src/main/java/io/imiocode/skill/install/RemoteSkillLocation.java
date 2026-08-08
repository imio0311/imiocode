package io.imiocode.skill.install;

import java.net.URI;
import java.util.Objects;

/** 完成协议、主机和路径校验后的远程 Skill 位置。 */
public record RemoteSkillLocation(
        RemoteSkillKind kind,
        URI sourceUri,
        String owner,
        String repository,
        String revision,
        String path
) {
    public RemoteSkillLocation {
        kind = Objects.requireNonNull(kind, "kind");
        sourceUri = Objects.requireNonNull(sourceUri, "sourceUri");
        owner = require(owner, "owner");
        repository = require(repository, "repository");
        revision = require(revision, "revision");
        path = require(path, "path");
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " 不能为空");
        return value;
    }
}
