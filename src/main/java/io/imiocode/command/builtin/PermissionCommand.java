package io.imiocode.command.builtin;

import io.imiocode.command.Command;
import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandDescriptor;
import io.imiocode.command.CommandMessage;
import io.imiocode.command.CommandResult;
import io.imiocode.command.CommandType;
import io.imiocode.permission.PermissionMode;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 查询或临时切换当前进程权限模式，不修改磁盘配置和已有规则。 */
public final class PermissionCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "permission", Set.of("perm"), "/permission [ask|auto-edit|read-only|full-access|lockdown]",
            "查看或临时切换当前进程权限模式", CommandType.LOCAL);

    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (arguments.size() > 1) throw new IllegalArgumentException("/permission 参数过多");
        if (arguments.isEmpty()) {
            return CommandResult.handled(CommandMessage.info(
                    "[权限] 当前模式: " + format(context.services().permissionMode())
                            + "\n可用模式: ask、auto-edit、read-only、full-access、lockdown"));
        }
        if (arguments.getFirst().isBlank()) throw new IllegalArgumentException("权限模式不能为空");
        PermissionMode mode = PermissionMode.parse(arguments.getFirst());
        context.services().switchPermissionMode(mode);
        return CommandResult.handled(CommandMessage.info("[权限] 当前模式: " + format(mode)));
    }

    private static String format(PermissionMode mode) {
        return mode.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
