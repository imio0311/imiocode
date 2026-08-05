package io.imiocode.command;

import java.util.Objects;

/** 命令执行所需的依赖背包，不绑定具体终端。 */
public record CommandContext(CommandServices services, UIController ui, CommandRegistry commands) {
    public CommandContext {
        Objects.requireNonNull(services, "services");
        Objects.requireNonNull(ui, "ui");
        Objects.requireNonNull(commands, "commands");
    }
}
