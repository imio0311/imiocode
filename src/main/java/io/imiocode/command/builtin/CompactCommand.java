package io.imiocode.command.builtin;

import io.imiocode.command.*;
import java.util.List;
import java.util.Set;

public final class CompactCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "compact", Set.of(), "/compact", "立即压缩当前会话上下文", CommandType.LOCAL);

    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }

    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!arguments.isEmpty()) throw new IllegalArgumentException("/compact 不接受参数");
        var report = context.services().compact();
        String text = report.compacted() ? "[上下文] 手动压缩完成：" + report.beforeTokens() + " → " + report.afterTokens() + " Token"
                : "[上下文] " + report.message();
        return CommandResult.handled(CommandMessage.info(text));
    }
}
