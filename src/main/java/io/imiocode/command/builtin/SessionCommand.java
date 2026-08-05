package io.imiocode.command.builtin;

import io.imiocode.command.*;
import io.imiocode.session.SessionId;
import io.imiocode.session.SessionSummary;
import java.util.List;
import java.util.Set;

public final class SessionCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "session", Set.of("sessions"),
            "/session list|current|new|resume <id>|delete <id>",
            "查看、创建、恢复或删除会话", CommandType.LOCAL);

    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }

    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!context.services().sessionsEnabled()) return CommandResult.handled(CommandMessage.error("[会话] 持久化功能已关闭"));
        if (arguments.isEmpty()) throw new IllegalArgumentException("缺少 session 子命令");
        return switch (arguments.getFirst().toLowerCase(java.util.Locale.ROOT)) {
            case "list" -> list(context, arguments);
            case "current" -> current(context, arguments);
            case "new" -> create(context, arguments);
            case "resume" -> resume(context, arguments);
            case "delete" -> delete(context, arguments);
            default -> throw new IllegalArgumentException("未知 session 子命令");
        };
    }

    private CommandResult list(CommandContext context, List<String> args) {
        requireSize(args, 1); List<SessionSummary> sessions = context.services().listSessions();
        if (sessions.isEmpty()) return CommandResult.handled(CommandMessage.info("[会话] 暂无项目会话"));
        StringBuilder text = new StringBuilder("[会话] 项目会话：\n");
        sessions.forEach(item -> text.append("- ").append(item.id()).append(" · ")
                .append(item.updatedAt()).append(" · ").append(item.messageCount()).append(" 条消息\n"));
        return CommandResult.handled(CommandMessage.info(text.toString().stripTrailing()));
    }

    private CommandResult current(CommandContext context, List<String> args) {
        requireSize(args, 1); return summary("[会话] 当前：", context.services().currentSession());
    }
    private CommandResult create(CommandContext context, List<String> args) {
        requireSize(args, 1); return summary("[会话] 已新建：", context.services().newSession());
    }
    private CommandResult resume(CommandContext context, List<String> args) {
        requireSize(args, 2); var result = context.services().resumeSession(new SessionId(args.get(1)));
        return CommandResult.handled(CommandMessage.info("[会话] 已恢复 " + result.snapshot().metadata().id()
                + "，" + result.snapshot().history().size() + " 条消息"));
    }
    private CommandResult delete(CommandContext context, List<String> args) {
        requireSize(args, 2); SessionId id = new SessionId(args.get(1));
        if (id.equals(context.services().currentSession().id())) throw new IllegalArgumentException("不能删除当前会话，请先新建或恢复其他会话");
        boolean confirmed = context.ui().confirm(new ConfirmationPrompt(
                "删除会话", id.value(), "删除后无法从 ImioCode 恢复"));
        if (!confirmed) return CommandResult.handled(CommandMessage.info("[会话] 已取消删除"));
        context.services().deleteSession(id);
        return CommandResult.handled(CommandMessage.info("[会话] 已删除 " + id));
    }
    private static CommandResult summary(String prefix, SessionSummary summary) {
        return CommandResult.handled(CommandMessage.info(prefix + summary.id() + " · " + summary.messageCount() + " 条消息"));
    }
    private static void requireSize(List<String> args, int size) {
        if (args.size() != size) throw new IllegalArgumentException("session 子命令参数数量错误");
    }
}
