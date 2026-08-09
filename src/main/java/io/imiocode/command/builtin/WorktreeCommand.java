package io.imiocode.command.builtin;

import io.imiocode.command.Command;
import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandDescriptor;
import io.imiocode.command.CommandMessage;
import io.imiocode.command.CommandResult;
import io.imiocode.command.CommandType;
import io.imiocode.command.ConfirmationPrompt;
import io.imiocode.worktree.lifecycle.WorktreeManager;
import io.imiocode.worktree.runtime.WorkspaceTransitionController;

import java.util.List;
import java.util.Set;

/** `/worktree` 的本地生命周期入口。 */
public final class WorktreeCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "worktree", Set.of("wt"),
            "/worktree list|create <slug>|enter <slug>|exit [keep|remove]|remove <slug>",
            "管理 Git Worktree", CommandType.LOCAL);
    private final WorktreeManager manager;
    private final WorkspaceTransitionController transitions;

    public WorktreeCommand(WorktreeManager manager, WorkspaceTransitionController transitions) {
        this.manager = java.util.Objects.requireNonNull(manager);
        this.transitions = java.util.Objects.requireNonNull(transitions);
    }

    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }

    @Override public CommandResult execute(CommandContext context, List<String> arguments) {
        if (arguments.isEmpty()) throw new IllegalArgumentException("缺少 worktree 子命令");
        return switch (arguments.getFirst().toLowerCase(java.util.Locale.ROOT)) {
            case "list" -> list(arguments);
            case "create" -> create(arguments);
            case "enter" -> enter(arguments);
            case "exit" -> exit(context, arguments);
            case "remove" -> remove(context, arguments);
            default -> throw new IllegalArgumentException("未知 worktree 子命令");
        };
    }

    private CommandResult list(List<String> args) {
        requireSize(args, 1); var items = manager.list();
        if (items.isEmpty()) return CommandResult.handled(CommandMessage.info("[Worktree] 暂无受管 Worktree"));
        StringBuilder text = new StringBuilder("[Worktree]\n");
        items.forEach(item -> text.append("- ").append(item.slug()).append(" · ")
                .append(item.branch()).append(" · ").append(item.active() ? "active" : "idle")
                .append(" · ").append(item.changes().summary()).append(" · ").append(item.path()).append('\n'));
        return CommandResult.handled(CommandMessage.info(text.toString().stripTrailing()));
    }

    private CommandResult create(List<String> args) {
        requireSize(args, 2); var created = manager.create(args.get(1));
        StringBuilder text = new StringBuilder("[Worktree] 已创建 ")
                .append(created.session().slug()).append("\nPath: ").append(created.session().worktreePath())
                .append("\nBranch: ").append(created.session().worktreeBranch());
        created.warnings().forEach(value -> text.append("\nWarning: ").append(value));
        return CommandResult.handled(CommandMessage.info(text.toString()));
    }

    private CommandResult enter(List<String> args) {
        requireSize(args, 2); var session = manager.enter(args.get(1));
        transitions.enter(session.worktreePath());
        return CommandResult.restart(CommandMessage.info("[Worktree] 正在进入 " + session.slug()));
    }

    private CommandResult exit(CommandContext context, List<String> args) {
        if (args.size() > 2) throw new IllegalArgumentException("exit 子命令参数数量错误");
        String action = args.size() == 1 ? "keep" : args.get(1).toLowerCase(java.util.Locale.ROOT);
        if (!action.equals("keep") && !action.equals("remove")) throw new IllegalArgumentException("exit 只接受 keep 或 remove");
        boolean remove = action.equals("remove");
        if (remove && !context.ui().confirm(new ConfirmationPrompt(
                "删除当前 Worktree", "当前隔离目录和本地分支", "未合并改动和提交将被永久丢弃"))) {
            return CommandResult.handled(CommandMessage.info("[Worktree] 已取消删除"));
        }
        var report = manager.exit(remove, remove);
        transitions.exit(manager.originalRoot());
        return CommandResult.restart(CommandMessage.info("[Worktree] " + report.reason() + "，正在返回原工作区"));
    }

    private CommandResult remove(CommandContext context, List<String> args) {
        requireSize(args, 2); String slug = args.get(1);
        if (!context.ui().confirm(new ConfirmationPrompt(
                "删除 Worktree", slug, "未合并改动和提交将被永久丢弃"))) {
            return CommandResult.handled(CommandMessage.info("[Worktree] 已取消删除"));
        }
        var report = manager.remove(slug, true);
        return CommandResult.handled(CommandMessage.info("[Worktree] " + report.reason() + ": " + slug));
    }

    private static void requireSize(List<String> args, int expected) {
        if (args.size() != expected) throw new IllegalArgumentException("worktree 子命令参数数量错误");
    }
}
