package io.imiocode.worktree;

/** Worktree 对外只暴露不含命令输出和配置内容的安全错误。 */
public final class WorktreeException extends RuntimeException {
    public WorktreeException(String message) { super(message); }
    public WorktreeException(String message, Throwable cause) { super(message, cause); }
}
