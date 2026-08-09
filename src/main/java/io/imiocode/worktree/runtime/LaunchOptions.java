package io.imiocode.worktree.runtime;

import io.imiocode.worktree.WorktreeException;

import java.util.List;

public record LaunchOptions(boolean resume) {
    public static LaunchOptions parse(String[] args) {
        List<String> values = args == null ? List.of() : List.of(args);
        if (values.isEmpty()) return new LaunchOptions(false);
        if (values.size() == 1 && "--resume".equals(values.getFirst())) return new LaunchOptions(true);
        throw new WorktreeException("未知启动参数；当前只支持 --resume");
    }
}
