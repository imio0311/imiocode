package io.imiocode.skill.install;

import java.net.URI;
import java.util.Objects;

public record SkillInstallRequest(URI source, boolean force) {
    public SkillInstallRequest {
        source = Objects.requireNonNull(source, "source");
    }
}
