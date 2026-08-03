package io.imiocode.instruction;

import java.nio.file.Path;
import java.util.Objects;

public record InstructionProblem(Path source, String safeMessage) {
    public InstructionProblem {
        source = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
        if (safeMessage == null || safeMessage.isBlank()) throw new IllegalArgumentException("safeMessage 不能为空");
        safeMessage = safeMessage.trim();
    }
}
