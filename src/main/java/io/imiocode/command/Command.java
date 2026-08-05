package io.imiocode.command;

import java.util.List;

/** 所有内置 Slash Command 的统一契约。 */
public interface Command {
    CommandDescriptor descriptor();

    CommandResult execute(CommandContext context, List<String> arguments);
}
