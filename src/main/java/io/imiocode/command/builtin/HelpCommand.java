package io.imiocode.command.builtin;

import io.imiocode.command.*;
import java.util.List;
import java.util.Set;

public final class HelpCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "help", Set.of("h", "?"), "/help", "显示命令目录和用法", CommandType.LOCAL);

    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }

    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!arguments.isEmpty()) throw new IllegalArgumentException("/help 不接受参数");
        StringBuilder text = new StringBuilder("核心命令：\n");
        appendGroup(text, context.commands().listCommands().stream()
                .filter(item -> !item.compatibility()).toList());
        text.append("\n兼容命令：\n");
        appendGroup(text, context.commands().listCommands().stream()
                .filter(CommandDescriptor::compatibility).toList());
        return CommandResult.handled(CommandMessage.info(text.toString().stripTrailing()));
    }

    private static void appendGroup(StringBuilder text, List<CommandDescriptor> commands) {
        commands.forEach(item -> text.append("  ").append(item.usage())
                .append(" — ").append(item.description()).append('\n'));
    }
}
