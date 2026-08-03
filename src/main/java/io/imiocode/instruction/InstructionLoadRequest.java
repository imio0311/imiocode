package io.imiocode.instruction;

import io.imiocode.config.InstructionsConfig;
import java.nio.file.Path;
import java.util.Objects;

public record InstructionLoadRequest(Path workspace, Path userHome, InstructionsConfig config) {
    public InstructionLoadRequest {
        workspace = Objects.requireNonNull(workspace, "workspace").toAbsolutePath().normalize();
        userHome = Objects.requireNonNull(userHome, "userHome").toAbsolutePath().normalize();
        Objects.requireNonNull(config, "config");
    }
}
