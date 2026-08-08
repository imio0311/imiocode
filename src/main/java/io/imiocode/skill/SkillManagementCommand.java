package io.imiocode.skill;

import io.imiocode.command.Command;
import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandDescriptor;
import io.imiocode.command.CommandMessage;
import io.imiocode.command.CommandResult;
import io.imiocode.command.CommandType;
import io.imiocode.config.UiVerbosity;
import io.imiocode.skill.install.SkillInstallResult;
import io.imiocode.skill.install.SkillInstallStage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** `/skill list|info|reload|install` 本地管理命令。 */
public final class SkillManagementCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "skill", Set.of("skills"), "/skill <list|info NAME|reload|install URL [--force]>",
            "列出、查看或热刷新 Skill", CommandType.LOCAL);

    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public CommandResult execute(CommandContext context, List<String> arguments) {
        String action = arguments.isEmpty() ? "list" : arguments.getFirst().toLowerCase(Locale.ROOT);
        return switch (action) {
            case "list" -> list(context.services().skillCatalog());
            case "info" -> info(context, arguments);
            case "reload" -> reload(context);
            case "install" -> install(context, arguments);
            default -> throw new IllegalArgumentException("未知 Skill 子命令: " + action);
        };
    }

    private static CommandResult list(SkillCatalogSnapshot snapshot) {
        List<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.info("可用 Skill（" + snapshot.skills().size() + "）"));
        snapshot.sorted().forEach(skill -> messages.add(CommandMessage.info("- "
                + skill.metadata().name() + " [" + skill.origin().name().toLowerCase(Locale.ROOT)
                + "/" + skill.metadata().mode().name().toLowerCase(Locale.ROOT) + "] "
                + skill.metadata().description())));
        snapshot.diagnostics().forEach(value -> messages.add(CommandMessage.error(value)));
        return CommandResult.handled(messages);
    }

    private static CommandResult info(CommandContext context, List<String> arguments) {
        if (arguments.size() != 2) throw new IllegalArgumentException("info 需要一个 Skill 名称");
        SkillDescriptor skill = context.services().skillCatalog().find(arguments.get(1))
                .orElseThrow(() -> new IllegalArgumentException("未知 Skill: " + arguments.get(1)));
        SkillMetadata meta = skill.metadata();
        return CommandResult.handled(CommandMessage.info("Skill /" + meta.name()
                + "\n描述: " + meta.description()
                + "\n来源: " + skill.origin().name().toLowerCase(Locale.ROOT)
                + "\n模式: " + meta.mode().name().toLowerCase(Locale.ROOT)
                + "\n历史: " + meta.history().name().toLowerCase(Locale.ROOT)
                + "\n允许工具: " + (meta.allowedTools().isEmpty() ? "无" : String.join(", ", meta.allowedTools()))
                + "\n位置: " + skill.source().id()));
    }

    private static CommandResult reload(CommandContext context) {
        SkillCatalogSnapshot snapshot = context.services().reloadSkills();
        List<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.info("Skill 已刷新：" + snapshot.skills().size()
                + " 个，目录代次 " + snapshot.generation()));
        snapshot.diagnostics().forEach(value -> messages.add(CommandMessage.error(value)));
        return CommandResult.handled(messages);
    }

    private static CommandResult install(CommandContext context, List<String> arguments) {
        if (arguments.size() < 2 || arguments.size() > 3) {
            throw new IllegalArgumentException("用法: /skill install <URL> [--force]");
        }
        boolean force = arguments.size() == 3;
        if (force && !"--force".equals(arguments.get(2))) {
            throw new IllegalArgumentException("安装仅支持可选参数 --force");
        }
        List<CommandMessage> messages = new ArrayList<>();
        SkillInstallResult result = context.services().installSkill(
                arguments.get(1), force,
                (stage, message) -> {
                    if (context.ui().verbosity() == UiVerbosity.VERBOSE
                            || stage == SkillInstallStage.DOWNLOADING
                            || stage == SkillInstallStage.COMPLETED) {
                        messages.add(CommandMessage.info(message));
                    }
                });
        messages.add(CommandMessage.info("可立即使用 /" + result.skillName()));
        return CommandResult.handled(messages);
    }
}
