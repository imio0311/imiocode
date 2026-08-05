package io.imiocode.command;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRegistryTest {
    @Test
    void registersFindsListsAndCompletesDeterministically() {
        CommandRegistry registry = new CommandRegistry();
        Command beta = command("beta", Set.of("b"));
        Command alpha = command("alpha", Set.of("a"));
        registry.register(beta);
        registry.register(alpha);

        assertEquals(alpha, registry.find("/ALPHA").orElseThrow());
        assertEquals(alpha, registry.find("a").orElseThrow());
        assertEquals(List.of("alpha", "beta"), registry.listCommands().stream()
                .map(CommandDescriptor::name).toList());
        assertEquals(List.of("/a", "/alpha"), registry.complete("a"));
        assertThrows(UnsupportedOperationException.class, () -> registry.complete("").add("/x"));

        CommandRegistry reverse = new CommandRegistry();
        reverse.register(command("alpha", Set.of("a")));
        reverse.register(command("beta", Set.of("b")));
        assertEquals(registry.listCommands(), reverse.listCommands());
    }

    @Test
    void rejectsAllConflictsWithoutPartialRegistration() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(command("alpha", Set.of("a")));

        assertThrows(IllegalArgumentException.class,
                () -> registry.register(command("alpha", Set.of("new"))));
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(command("a", Set.of("other"))));
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(command("other", Set.of("alpha"))));
        assertEquals(List.of("alpha"), registry.listCommands().stream()
                .map(CommandDescriptor::name).toList());
        assertFalse(registry.find("new").isPresent());
        assertFalse(registry.find("other").isPresent());
    }

    @Test
    void dispatchConsumesUnknownAndBrokenSlashCommands() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(command("ok", Set.of()));
        CommandContext context = context(registry);

        assertTrue(registry.dispatch("ordinary text", context).isEmpty());
        assertTrue(registry.dispatch("/missing", context).orElseThrow().messages().getFirst().error());
        assertTrue(registry.dispatch("/ok \"", context).orElseThrow().messages().getFirst().error());
        assertEquals(CommandOutcome.HANDLED,
                registry.dispatch("/OK", context).orElseThrow().outcome());
    }

    @Test
    void hidesUnexpectedExceptionDetailsAndAddsUsageForArguments() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new Command() {
            private final CommandDescriptor descriptor = new CommandDescriptor(
                    "broken", Set.of(), "/broken", "test", CommandType.LOCAL);
            @Override public CommandDescriptor descriptor() { return descriptor; }
            @Override public CommandResult execute(CommandContext context, List<String> arguments) {
                if (!arguments.isEmpty()) throw new IllegalArgumentException("参数错误");
                throw new IllegalStateException("secret-value java.lang.IllegalStateException");
            }
        });
        CommandContext context = context(registry);

        String safe = registry.dispatch("/broken", context).orElseThrow().messages().getFirst().text();
        assertFalse(safe.contains("secret-value"));
        assertFalse(safe.contains("IllegalStateException"));
        assertTrue(registry.dispatch("/broken x", context).orElseThrow().messages().getFirst().text()
                .contains("用法：/broken"));
    }

    private static Command command(String name, Set<String> aliases) {
        return new Command() {
            private final CommandDescriptor descriptor = new CommandDescriptor(
                    name, aliases, "/" + name, "test", CommandType.LOCAL);
            @Override public CommandDescriptor descriptor() { return descriptor; }
            @Override public CommandResult execute(CommandContext context, List<String> arguments) {
                return CommandResult.handled(CommandMessage.info("ok"));
            }
        };
    }

    private static CommandContext context(CommandRegistry registry) {
        CommandServices services = (CommandServices) Proxy.newProxyInstance(
                CommandServices.class.getClassLoader(), new Class<?>[]{CommandServices.class},
                (proxy, method, args) -> null);
        UIController ui = (UIController) Proxy.newProxyInstance(
                UIController.class.getClassLoader(), new Class<?>[]{UIController.class},
                (proxy, method, args) -> null);
        return new CommandContext(services, ui, registry);
    }
}
