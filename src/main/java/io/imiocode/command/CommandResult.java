package io.imiocode.command;

import java.util.List;

public record CommandResult(CommandDisposition disposition, List<CommandMessage> messages) {
    public CommandResult { messages = List.copyOf(messages == null ? List.of() : messages); }
    public static CommandResult handled(CommandMessage... messages) {
        return new CommandResult(CommandDisposition.HANDLED, List.of(messages));
    }
    public static CommandResult exit() { return new CommandResult(CommandDisposition.EXIT_REQUESTED, List.of()); }
}
