package io.imiocode.skill;

import io.imiocode.command.Command;
import io.imiocode.command.CommandRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 将当前最终 Skill 目录原子同步到 Slash Command 注册中心。 */
public final class SkillCommandRegistrar {
    private final CommandRegistry commands;

    public SkillCommandRegistrar(CommandRegistry commands) {
        this.commands = Objects.requireNonNull(commands, "commands");
    }

    public void sync(SkillCatalogSnapshot snapshot) {
        List<Command> dynamic = new ArrayList<>();
        snapshot.sorted().forEach(skill -> dynamic.add(new SkillSlashCommand(skill.metadata())));
        commands.replaceDynamic(dynamic);
    }
}
