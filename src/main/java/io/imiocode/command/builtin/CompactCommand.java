package io.imiocode.command.builtin;

import io.imiocode.command.*;
import java.util.List;

public final class CompactCommand implements LocalCommand {
    public String name() { return "compact"; }
    public String usage() { return "/compact"; }
    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!arguments.isEmpty()) throw new IllegalArgumentException("/compact 不接受参数");
        var report = context.services().compact();
        String text = report.compacted() ? "[上下文] 手动压缩完成：" + report.beforeTokens() + " → " + report.afterTokens() + " Token"
                : "[上下文] " + report.message();
        return CommandResult.handled(CommandMessage.info(text));
    }
}
