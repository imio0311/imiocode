package io.imiocode.command.builtin;

import io.imiocode.command.*;
import io.imiocode.config.UiVerbosity;
import java.util.List;
import java.util.Set;

/** 在当前终端进程内切换精简或详细输出，不持久化配置。 */
public final class VerbosityCommand implements Command {
    private final CommandDescriptor descriptor;
    private final UiVerbosity verbosity;
    public VerbosityCommand(String name, UiVerbosity verbosity) {
        this.verbosity = verbosity;
        this.descriptor = new CommandDescriptor(name, Set.of(), "/" + name,
                verbosity == UiVerbosity.VERBOSE ? "显示详细执行过程" : "切换到精简输出",
                CommandType.UI, true);
    }
    @Override public CommandDescriptor descriptor() { return descriptor; }
    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!arguments.isEmpty()) throw new IllegalArgumentException(descriptor.usage() + " 不接受参数");
        context.ui().setVerbosity(verbosity);
        return CommandResult.handled(CommandMessage.info(verbosity == UiVerbosity.VERBOSE
                ? "[UI] 详细模式" : "[UI] 精简模式"));
    }
}
