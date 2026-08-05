package io.imiocode.command;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandResultTest {
    @Test
    void factoriesCreateThreeExclusiveOutcomes() {
        CommandResult handled = CommandResult.handled(CommandMessage.info("ok"));
        CommandResult prompt = CommandResult.forwardToAgent("review");
        CommandResult exit = CommandResult.exit();

        assertEquals(CommandOutcome.HANDLED, handled.outcome());
        assertTrue(handled.prompt().isEmpty());
        assertEquals("review", prompt.prompt().orElseThrow());
        assertEquals(CommandOutcome.EXIT_REQUESTED, exit.outcome());
    }

    @Test
    void rejectsMixedOrEmptyResults() {
        assertThrows(IllegalArgumentException.class, () -> new CommandResult(
                CommandOutcome.FORWARD_TO_AGENT, List.of(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new CommandResult(
                CommandOutcome.FORWARD_TO_AGENT, List.of(CommandMessage.info("x")), Optional.of("p")));
        assertThrows(IllegalArgumentException.class, () -> new CommandResult(
                CommandOutcome.HANDLED, List.of(), Optional.of("p")));
        assertThrows(IllegalArgumentException.class, () -> new CommandResult(
                CommandOutcome.EXIT_REQUESTED, List.of(CommandMessage.info("x")), Optional.empty()));
    }
}
