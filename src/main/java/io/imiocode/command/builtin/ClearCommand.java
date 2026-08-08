package io.imiocode.command.builtin;

import io.imiocode.command.Command;
import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandDescriptor;
import io.imiocode.command.CommandResult;
import io.imiocode.command.CommandType;

import java.util.List;
import java.util.Set;

public final class ClearCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "clear", Set.of("cls"), "/clear", "清空终端显示，不删除会话历史", CommandType.UI);

    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!arguments.isEmpty()) throw new IllegalArgumentException("/clear 不接受参数");
        context.services().cancelActiveWork();
        context.ui().clearScreen();
        return CommandResult.handled();
    }
}
