package io.imiocode.skill;

import io.imiocode.command.Command;
import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandDescriptor;
import io.imiocode.command.CommandResult;
import io.imiocode.command.CommandType;

import java.util.List;

/** 一个 Skill 自动生成的同名 Slash Command。 */
public final class SkillSlashCommand implements Command {
    private final SkillMetadata metadata;
    private final CommandDescriptor descriptor;

    public SkillSlashCommand(SkillMetadata metadata) {
        this.metadata = metadata;
        this.descriptor = new CommandDescriptor(
                metadata.name(), metadata.aliases(),
                "/" + metadata.name() + " [arguments]",
                "Skill: " + metadata.description(), CommandType.PROMPT);
    }

    @Override public CommandDescriptor descriptor() { return descriptor; }

    @Override
    public CommandResult execute(CommandContext context, List<String> arguments) {
        String joined = String.join(" ", arguments);
        String prompt = context.services().prepareSkillInvocation(metadata.name(), joined);
        return CommandResult.forwardToAgent(prompt);
    }
}
