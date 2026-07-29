package io.imiocode.prompt;

/** 可安全注入 Prompt 的 Git 工作区摘要。 */
public enum GitWorkingTreeState {
    CLEAN,
    DIRTY,
    NOT_REPOSITORY,
    UNAVAILABLE
}
