package io.imiocode.command.builtin;

import io.imiocode.command.*;
import io.imiocode.config.UiVerbosity;
import java.util.List;

public final class VerbosityCommand implements LocalCommand {
    private final String name;
    private final UiVerbosity verbosity;
    public VerbosityCommand(String name, UiVerbosity verbosity) { this.name = name; this.verbosity = verbosity; }
    public String name() { return name; }
    public String usage() { return "/" + name; }
    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!arguments.isEmpty()) throw new IllegalArgumentException(usage() + " 不接受参数");
        context.terminal().setVerbosity(verbosity);
        return CommandResult.handled(CommandMessage.info(verbosity == UiVerbosity.VERBOSE
                ? "[UI] 详细模式" : "[UI] 精简模式"));
    }
}
