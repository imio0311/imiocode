package io.imiocode.context;

import java.nio.file.Path;
import java.util.Objects;

public record SpilledResult(Path relativePath, int originalCharacters, boolean newlyCreated) {
    public SpilledResult {
        relativePath = Objects.requireNonNull(relativePath, "relativePath");
        if (relativePath.isAbsolute() || originalCharacters < 0) {
            throw new IllegalArgumentException("落盘结果元信息无效");
        }
    }

    public String unixPath() { return relativePath.toString().replace('\\', '/'); }
}
