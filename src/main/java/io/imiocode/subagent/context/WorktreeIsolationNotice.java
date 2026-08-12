package io.imiocode.subagent.context;

import io.imiocode.conversation.SystemReminder;
import io.imiocode.worktree.model.WorktreeSession;

/** 为隔离子 Agent 注入 Worktree 路径、分支和禁止跨工作区操作的约束。 */
public final class WorktreeIsolationNotice {
    private WorktreeIsolationNotice() { }
    public static SystemReminder format(WorktreeSession session) {
        return new SystemReminder("""
                你正在独立 Git Worktree 中执行任务。
                Worktree 路径：%s
                Worktree 分支：%s
                原始工作区：%s
                所有相对文件和命令都必须以 Worktree 为根；不要访问或修改原始工作区。
                完成后只报告结果，不要自行 merge、rebase 或删除 Worktree。
                """.formatted(session.worktreePath(), session.worktreeBranch(), session.originalCwd()).strip());
    }
}
