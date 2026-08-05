package io.imiocode.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** 命令的注册、发现、补全和安全分派中心。 */
public final class CommandRegistry {
    private final CommandParser parser;
    private final Map<String, Command> names = new LinkedHashMap<>();
    private final Map<String, Command> aliases = new LinkedHashMap<>();
    private final Set<Command> commands = new LinkedHashSet<>();

    public CommandRegistry() {
        this(new CommandParser());
    }

    public CommandRegistry(CommandParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public synchronized void register(Command command) {
        Objects.requireNonNull(command, "command");
        CommandDescriptor descriptor = Objects.requireNonNull(command.descriptor(), "descriptor");
        List<String> requested = new ArrayList<>();
        requested.add(descriptor.name());
        requested.addAll(descriptor.aliases());
        for (String value : requested) {
            if (names.containsKey(value) || aliases.containsKey(value)) {
                throw new IllegalArgumentException("命令名或别名冲突：" + value);
            }
        }
        names.put(descriptor.name(), command);
        descriptor.aliases().forEach(alias -> aliases.put(alias, command));
        commands.add(command);
    }

    public synchronized Optional<Command> find(String name) {
        String normalized = normalize(name);
        Command command = names.get(normalized);
        return Optional.ofNullable(command == null ? aliases.get(normalized) : command);
    }

    public synchronized List<CommandDescriptor> listCommands() {
        return commands.stream()
                .map(Command::descriptor)
                .sorted(Comparator.comparing(CommandDescriptor::name))
                .toList();
    }

    public synchronized List<String> complete(String prefix) {
        String normalized = normalize(prefix);
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        names.keySet().stream().filter(name -> name.startsWith(normalized)).sorted()
                .map(name -> "/" + name).forEach(candidates::add);
        aliases.keySet().stream().filter(alias -> alias.startsWith(normalized)).sorted()
                .map(alias -> "/" + alias).forEach(candidates::add);
        return candidates.stream().sorted().toList();
    }

    public Optional<CommandResult> dispatch(String input, CommandContext context) {
        Optional<ParsedCommand> parsed;
        try {
            parsed = parser.parse(input);
        } catch (IllegalArgumentException exception) {
            return Optional.of(CommandResult.handled(CommandMessage.error(exception.getMessage())));
        }
        if (parsed.isEmpty()) return Optional.empty();
        ParsedCommand parsedCommand = parsed.orElseThrow();
        Command handler = find(parsedCommand.name()).orElse(null);
        if (handler == null) {
            return Optional.of(CommandResult.handled(CommandMessage.error(
                    "未知命令 /" + parsedCommand.name() + "；输入 /help 查看可用命令")));
        }
        try {
            return Optional.of(Objects.requireNonNull(
                    handler.execute(context, parsedCommand.arguments()), "命令返回了空结果"));
        } catch (IllegalArgumentException exception) {
            return Optional.of(CommandResult.handled(CommandMessage.error(
                    exception.getMessage() + "；用法：" + handler.descriptor().usage())));
        } catch (IllegalStateException exception) {
            return Optional.of(CommandResult.handled(CommandMessage.error(
                    "当前状态不允许执行该命令")));
        } catch (RuntimeException exception) {
            return Optional.of(CommandResult.handled(CommandMessage.error("命令执行失败，请重试")));
        }
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }
}
