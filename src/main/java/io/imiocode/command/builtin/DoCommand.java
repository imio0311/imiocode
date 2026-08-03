package io.imiocode.command.builtin;

import io.imiocode.agent.AgentMode;
import io.imiocode.command.*;
import java.util.List;

public final class DoCommand implements LocalCommand {
    public String name() { return "do"; }
    public String usage() { return "/do"; }
    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!arguments.isEmpty()) throw new IllegalArgumentException("/do 不接受参数");
        context.services().switchMode(AgentMode.DO);
        return CommandResult.handled(CommandMessage.info("[模式] Do：已启用正常工具"));
    }
}
