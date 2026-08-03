package io.imiocode.command.builtin;

import io.imiocode.command.*;
import java.util.List;

public final class HelpCommand implements LocalCommand {
    public String name() { return "help"; }
    public String usage() { return "/help"; }
    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!arguments.isEmpty()) throw new IllegalArgumentException("/help 不接受参数");
        return CommandResult.handled(CommandMessage.info("命令：/plan /do /compact /verbose /compact-ui /session /memory /exit"));
    }
}
