package io.imiocode.worktree.security;

import io.imiocode.worktree.WorktreeException;
import io.imiocode.worktree.config.WorktreeConfig;

import java.nio.file.Path;
import java.util.Objects;

/** 已校验 slug 到受管目录和分支的唯一映射。 */
public final class WorktreeNames {
    public static final String BRANCH_PREFIX = "worktree-";
    private final Path repositoryRoot;
    private final Path managedRoot;
    private final WorktreeSlugValidator validator;

    public WorktreeNames(Path repositoryRoot, WorktreeConfig config, WorktreeSlugValidator validator) {
        this.repositoryRoot = Objects.requireNonNull(repositoryRoot).toAbsolutePath().normalize();
        this.validator = Objects.requireNonNull(validator);
        this.managedRoot = this.repositoryRoot.resolve(config.directory()).normalize();
        if (!managedRoot.startsWith(this.repositoryRoot) || managedRoot.equals(this.repositoryRoot)) {
            throw new WorktreeException("Worktree 受管目录越界");
        }
    }

    public String slug(String input) { return validator.validate(input); }
    public String branch(String slug) { return BRANCH_PREFIX + validator.validate(slug); }

    public Path path(String slug) {
        Path result = managedRoot.resolve(validator.validate(slug)).normalize();
        if (!result.startsWith(managedRoot) || result.equals(managedRoot)) {
            throw new WorktreeException("Worktree 路径越界");
        }
        return result;
    }

    public Path repositoryRoot() { return repositoryRoot; }
    public Path managedRoot() { return managedRoot; }
}
