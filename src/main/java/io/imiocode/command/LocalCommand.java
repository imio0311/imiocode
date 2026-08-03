package io.imiocode.command;

import java.util.List;
import java.util.Set;

public interface LocalCommand {
    String name();
    default Set<String> aliases() { return Set.of(); }
    String usage();
    CommandResult execute(CommandContext context, List<String> arguments);
}
