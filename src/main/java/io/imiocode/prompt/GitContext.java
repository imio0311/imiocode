package io.imiocode.prompt;

import java.util.Objects;
import java.util.Optional;

/** 不包含文件名或命令原始输出的 Git 上下文。 */
public record GitContext(
        Optional<String> branch,
        GitWorkingTreeState state
) {
    public GitContext {
        branch = Objects.requireNonNull(branch, "Git 分支可选值不能为空")
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        state = Objects.requireNonNull(state, "Git 工作区状态不能为空");
    }

    public static GitContext unavailable() {
        return new GitContext(Optional.empty(), GitWorkingTreeState.UNAVAILABLE);
    }
}
