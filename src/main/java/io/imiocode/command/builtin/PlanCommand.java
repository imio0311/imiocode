package io.imiocode.command.builtin;

import io.imiocode.agent.AgentMode;
import io.imiocode.command.*;
import java.util.List;
import java.util.Set;

public final class PlanCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "plan", Set.of(), "/plan", "切换到只读规划模式", CommandType.LOCAL);

    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }

    public CommandResult execute(CommandContext context, List<String> arguments) {
        requireEmpty(arguments); context.services().switchMode(AgentMode.PLAN);
        return CommandResult.handled(CommandMessage.info("[模式] Plan：仅启用只读工具"));
    }
    private static void requireEmpty(List<String> args) { if (!args.isEmpty()) throw new IllegalArgumentException("/plan 不接受参数"); }
}
