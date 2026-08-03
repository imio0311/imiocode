package io.imiocode.command;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class LocalCommandRegistry {
    private final CommandParser parser;
    private final Map<String, LocalCommand> commands = new LinkedHashMap<>();

    public LocalCommandRegistry() { this(new CommandParser()); }
    public LocalCommandRegistry(CommandParser parser) { this.parser = parser; }

    public void register(LocalCommand command) {
        registerName(command.name(), command);
        command.aliases().forEach(alias -> registerName(alias, command));
    }

    public Optional<CommandResult> dispatch(String input, CommandContext context) {
        Optional<ParsedCommand> parsed;
        try { parsed = parser.parse(input); }
        catch (IllegalArgumentException exception) {
            return Optional.of(CommandResult.handled(CommandMessage.error(exception.getMessage())));
        }
        if (parsed.isEmpty()) return Optional.empty();
        ParsedCommand command = parsed.orElseThrow();
        LocalCommand handler = commands.get(command.name());
        if (handler == null) return Optional.of(CommandResult.handled(
                CommandMessage.error("未知命令 /" + command.name() + "；输入 /help 查看可用命令")));
        try { return Optional.of(handler.execute(context, command.arguments())); }
        catch (IllegalArgumentException exception) {
            return Optional.of(CommandResult.handled(CommandMessage.error(exception.getMessage() + "；用法：" + handler.usage())));
        } catch (RuntimeException exception) {
            String message = exception.getMessage();
            return Optional.of(CommandResult.handled(CommandMessage.error(
                    message == null || message.isBlank() ? "命令执行失败" : message)));
        }
    }

    private void registerName(String name, LocalCommand command) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("命令名不能为空");
        String normalized = name.toLowerCase(Locale.ROOT);
        if (commands.putIfAbsent(normalized, command) != null) throw new IllegalArgumentException("命令名或别名冲突：" + name);
    }
}
