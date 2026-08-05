package io.imiocode.command.builtin;

import io.imiocode.command.Command;
import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandDescriptor;
import io.imiocode.command.CommandMessage;
import io.imiocode.command.CommandResult;
import io.imiocode.command.CommandStatus;
import io.imiocode.command.CommandType;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class StatusCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "status", Set.of("st"), "/status", "显示当前安全运行状态", CommandType.LOCAL);

    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!arguments.isEmpty()) throw new IllegalArgumentException("/status 不接受参数");
        CommandStatus status = context.services().status();
        String text = "[状态]\n"
                + "Provider: " + status.provider() + "\n"
                + "Model: " + status.model() + "\n"
                + "Workspace: " + status.workspace() + "\n"
                + "Agent: " + status.agentMode().name().toLowerCase(Locale.ROOT) + "\n"
                + "Permission: " + status.permissionMode().name().toLowerCase(Locale.ROOT).replace('_', '-') + "\n"
                + "Session: " + status.session() + "\n"
                + "Context: " + compact(status.estimatedTokens()) + "/" + compact(status.contextWindowTokens()) + " tokens\n"
                + "MCP: " + status.connectedMcpServers() + " servers · " + status.registeredMcpTools() + " tools";
        return CommandResult.handled(CommandMessage.info(text));
    }

    static String compact(long value) {
        if (value < 1_000) return Long.toString(value);
        double scaled = value / 1_000.0;
        return scaled >= 100 ? Math.round(scaled) + "k"
                : String.format(Locale.ROOT, "%.1fk", scaled).replace(".0k", "k");
    }
}
