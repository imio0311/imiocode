package io.imiocode.command.builtin;

import io.imiocode.command.Command;
import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandDescriptor;
import io.imiocode.command.CommandResult;
import io.imiocode.command.CommandType;

import java.util.List;
import java.util.Set;

public final class ReviewCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "review", Set.of("rv"), "/review [focus]", "让 Agent 审查当前代码变更", CommandType.PROMPT);
    private final ReviewPromptBuilder promptBuilder;

    public ReviewCommand() {
        this(new ReviewPromptBuilder());
    }

    ReviewCommand(ReviewPromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
    }

    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public CommandResult execute(CommandContext context, List<String> arguments) {
        return CommandResult.forwardToAgent(promptBuilder.build(String.join(" ", arguments)));
    }
}
