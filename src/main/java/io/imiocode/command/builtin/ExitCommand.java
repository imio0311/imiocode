package io.imiocode.command.builtin;

import io.imiocode.command.*;
import java.util.List;
import java.util.Set;

public final class ExitCommand implements LocalCommand {
    public String name() { return "exit"; }
    public Set<String> aliases() { return Set.of("quit"); }
    public String usage() { return "/exit 或 /quit"; }
    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!arguments.isEmpty()) throw new IllegalArgumentException("退出命令不接受参数");
        return CommandResult.exit();
    }
}
