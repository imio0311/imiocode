package io.imiocode.instruction;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public record InstructionSnapshot(List<InstructionSource> sources, List<InstructionProblem> problems,
                                  Set<Path> dependencies, long expandedBytes) {
    public InstructionSnapshot {
        sources = List.copyOf(sources == null ? List.of() : sources);
        problems = List.copyOf(problems == null ? List.of() : problems);
        dependencies = Set.copyOf(dependencies == null ? Set.of() : dependencies);
        if (expandedBytes < 0) throw new IllegalArgumentException("expandedBytes 不能为负数");
    }

    public static InstructionSnapshot empty() {
        return new InstructionSnapshot(List.of(), List.of(), Set.of(), 0);
    }
}
