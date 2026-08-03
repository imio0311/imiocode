package io.imiocode.instruction;

import java.nio.file.Path;
import java.util.Objects;

public record InstructionSource(Path path, InstructionScope scope, int priority, String expandedContent) {
    public InstructionSource {
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        Objects.requireNonNull(scope, "scope");
        if (priority < 0) throw new IllegalArgumentException("priority 不能为负数");
        if (expandedContent == null || expandedContent.isBlank()) throw new IllegalArgumentException("指令内容不能为空");
        expandedContent = expandedContent.strip();
    }
}
