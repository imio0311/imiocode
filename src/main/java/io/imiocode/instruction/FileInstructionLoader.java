package io.imiocode.instruction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class FileInstructionLoader implements InstructionLoader {
    public static final String FILE_NAME = "MEWCODE.md";
    private final GitProjectLocator projectLocator;
    private final IncludeExpander expander;

    public FileInstructionLoader() { this(new GitProjectLocator(), new IncludeExpander()); }

    public FileInstructionLoader(GitProjectLocator projectLocator, IncludeExpander expander) {
        this.projectLocator = projectLocator; this.expander = expander;
    }

    @Override
    public InstructionSnapshot load(InstructionLoadRequest request) {
        if (!request.config().enabled()) return InstructionSnapshot.empty();
        Path workspace = request.workspace();
        Path projectRoot = projectLocator.locate(workspace);
        Path userRoot = request.userHome().resolve(".imiocode").normalize();
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(new Candidate(userRoot.resolve(FILE_NAME), userRoot, InstructionScope.USER, 0));
        addProjectCandidates(candidates, projectRoot, workspace);

        List<InstructionSource> sources = new ArrayList<>();
        List<InstructionProblem> problems = new ArrayList<>();
        LinkedHashSet<Path> dependencies = new LinkedHashSet<>();
        long total = 0;
        for (Candidate candidate : candidates) {
            if (!Files.exists(candidate.path, LinkOption.NOFOLLOW_LINKS)) continue;
            try {
                long remaining = request.config().maxExpandedBytes() - total;
                if (remaining <= 0) throw new IOException("全部指令展开内容超过总容量限制");
                IncludeExpander.Expansion expansion = expander.expand(candidate.path, candidate.allowedRoot,
                        request.config().maxIncludeDepth(), remaining);
                if (expansion.content().isBlank()) continue;
                sources.add(new InstructionSource(candidate.path, candidate.scope, candidate.priority, expansion.content()));
                dependencies.addAll(expansion.dependencies());
                total += expansion.bytes();
            } catch (IOException | RuntimeException exception) {
                String message = exception.getMessage();
                problems.add(new InstructionProblem(candidate.path,
                        message == null || message.isBlank() ? "无法加载指令文件" : message));
            }
        }
        return new InstructionSnapshot(sources, problems, dependencies, total);
    }

    private static void addProjectCandidates(List<Candidate> target, Path root, Path workspace) {
        Path relative = root.relativize(workspace);
        target.add(new Candidate(root.resolve(FILE_NAME), root, InstructionScope.PROJECT, 10));
        Path current = root;
        for (int i = 0; i < relative.getNameCount(); i++) {
            current = current.resolve(relative.getName(i));
            target.add(new Candidate(current.resolve(FILE_NAME), root, InstructionScope.PROJECT, 11 + i));
        }
    }

    private record Candidate(Path path, Path allowedRoot, InstructionScope scope, int priority) {}
}
