package io.imiocode.command.builtin;

import io.imiocode.command.*;
import java.util.List;
import java.util.Set;

/** 返回进程退出信号，实际资源关闭由最外层运行时统一完成。 */
public final class ExitCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "exit", Set.of("quit"), "/exit 或 /quit", "退出 ImioCode", CommandType.UI, true);

    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }

    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!arguments.isEmpty()) throw new IllegalArgumentException("退出命令不接受参数");
        return CommandResult.exit();
    }
}
