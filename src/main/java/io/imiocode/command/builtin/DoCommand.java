package io.imiocode.command.builtin;

import io.imiocode.agent.AgentMode;
import io.imiocode.command.*;
import java.util.List;
import java.util.Set;

public final class DoCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "do", Set.of(), "/do", "切换到正常执行模式", CommandType.LOCAL);

    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }

    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!arguments.isEmpty()) throw new IllegalArgumentException("/do 不接受参数");
        context.services().switchMode(AgentMode.DO);
        return CommandResult.handled(CommandMessage.info("[模式] Do：已启用正常工具"));
    }
}
