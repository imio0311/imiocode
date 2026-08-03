package io.imiocode.command;

import io.imiocode.terminal.TerminalUi;
import java.util.Objects;

public record CommandContext(CommandServices services, TerminalUi terminal) {
    public CommandContext { Objects.requireNonNull(services); Objects.requireNonNull(terminal); }
}
