package io.imiocode.command.builtin;

import io.imiocode.command.*;
import io.imiocode.memory.MemoryDocument;
import io.imiocode.memory.MemoryScope;
import java.util.List;
import java.util.Optional;

public final class MemoryCommand implements LocalCommand {
    public String name() { return "memory"; }
    public String usage() { return "/memory list [user|project]|add <scope> <content>|edit <scope> <id> <content>|forget <scope> <id>"; }

    public CommandResult execute(CommandContext context, List<String> arguments) {
        if (!context.services().memoryEnabled()) return CommandResult.handled(CommandMessage.error("[记忆] 功能已关闭"));
        if (arguments.isEmpty()) throw new IllegalArgumentException("缺少 memory 子命令");
        return switch (arguments.getFirst().toLowerCase(java.util.Locale.ROOT)) {
            case "list" -> list(context, arguments);
            case "add" -> add(context, arguments);
            case "edit" -> edit(context, arguments);
            case "forget" -> forget(context, arguments);
            default -> throw new IllegalArgumentException("未知 memory 子命令");
        };
    }

    private CommandResult list(CommandContext context, List<String> args) {
        if (args.size() > 2) throw new IllegalArgumentException("memory list 参数过多");
        Optional<MemoryScope> scope = args.size() == 2 ? Optional.of(MemoryScope.parse(args.get(1))) : Optional.empty();
        List<MemoryDocument> documents = context.services().listMemories(scope);
        StringBuilder text = new StringBuilder("[记忆]\n");
        documents.forEach(document -> {
            text.append(document.scope().name().toLowerCase()).append(":\n");
            document.entries().forEach(entry -> text.append("- ").append(entry.id()).append(" [")
                    .append(entry.category().name().toLowerCase()).append("] ").append(entry.content()).append('\n'));
        });
        return CommandResult.handled(CommandMessage.info(text.toString().stripTrailing()));
    }
    private CommandResult add(CommandContext context, List<String> args) {
        if (args.size() < 3) throw new IllegalArgumentException("memory add 缺少参数");
        var entry = context.services().addMemory(MemoryScope.parse(args.get(1)), join(args, 2));
        return CommandResult.handled(CommandMessage.info("[记忆] 已添加 " + entry.id()));
    }
    private CommandResult edit(CommandContext context, List<String> args) {
        if (args.size() < 4) throw new IllegalArgumentException("memory edit 缺少参数");
        var entry = context.services().editMemory(MemoryScope.parse(args.get(1)), args.get(2), join(args, 3));
        return CommandResult.handled(CommandMessage.info("[记忆] 已更新 " + entry.id()));
    }
    private CommandResult forget(CommandContext context, List<String> args) {
        if (args.size() != 3) throw new IllegalArgumentException("memory forget 参数数量错误");
        context.services().forgetMemory(MemoryScope.parse(args.get(1)), args.get(2));
        return CommandResult.handled(CommandMessage.info("[记忆] 已删除 " + args.get(2)));
    }
    private static String join(List<String> args, int start) { return String.join(" ", args.subList(start, args.size())).trim(); }
}
