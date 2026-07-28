package io.imiocode.agent;

import io.imiocode.tool.ToolAvailability;
import io.imiocode.tool.ToolResolution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnknownToolCircuitBreakerTest {
    @Test
    void availableResetsWhileDisabledAndDisallowedKeepCount() {
        UnknownToolCircuitBreaker breaker = new UnknownToolCircuitBreaker();
        var attempt = breaker.beginAttempt();
        assertEquals(CircuitObservation.INCREMENTED, attempt.observe(unknown()));
        assertEquals(CircuitObservation.UNCHANGED,
                attempt.observe(unavailable(ToolAvailability.DISABLED)));
        assertEquals(CircuitObservation.UNCHANGED,
                attempt.observe(unavailable(ToolAvailability.DISALLOWED)));
        assertEquals(CircuitObservation.RESET, attempt.observe(available()));
        assertFalse(attempt.open());
    }

    @Test
    void thirdUnknownOpensAndCommittedAttemptsCarryAcrossRounds() {
        UnknownToolCircuitBreaker breaker = new UnknownToolCircuitBreaker();
        var first = breaker.beginAttempt();
        first.observe(unknown());
        first.observe(unknown());
        first.commit();

        var second = breaker.beginAttempt();
        assertEquals(CircuitObservation.OPENED, second.observe(unknown()));
        assertTrue(second.open());
    }

    @Test
    void abandonedAttemptDoesNotChangeCommittedCount() {
        UnknownToolCircuitBreaker breaker = new UnknownToolCircuitBreaker();
        breaker.beginAttempt().observe(unknown());
        var next = breaker.beginAttempt();
        next.observe(unknown());
        next.observe(unknown());
        assertFalse(next.open());
    }

    private static ToolResolution unknown() {
        return unavailable(ToolAvailability.UNKNOWN);
    }

    private static ToolResolution unavailable(ToolAvailability availability) {
        return ToolResolution.unavailable(availability);
    }

    private static ToolResolution available() {
        io.imiocode.tool.Tool tool = new io.imiocode.tool.Tool() {
            @Override
            public io.imiocode.tool.ToolDefinition definition() {
                return new io.imiocode.tool.ToolDefinition(
                        "ok", "ok",
                        com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(),
                        io.imiocode.tool.ToolRisk.LOW);
            }

            @Override
            public io.imiocode.tool.ToolResult execute(
                    com.fasterxml.jackson.databind.node.ObjectNode arguments) {
                return io.imiocode.tool.ToolResult.success("ok");
            }
        };
        return ToolResolution.available(tool);
    }
}
