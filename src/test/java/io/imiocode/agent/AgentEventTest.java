package io.imiocode.agent;

import io.imiocode.llm.TokenUsage;
import io.imiocode.llm.LlmErrorType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentEventTest {
    @Test
    void validatesIndexesAndNames() {
        assertDoesNotThrow(() -> new AgentEvent.ModelToolRequested(1, 0, "c1", "read_file"));
        assertThrows(IllegalArgumentException.class,
                () -> new AgentEvent.IterationStarted(0));
        assertThrows(IllegalArgumentException.class,
                () -> new AgentEvent.ModelToolRequested(1, -1, "c1", "read_file"));
        assertThrows(IllegalArgumentException.class,
                () -> new AgentEvent.ModelToolRequested(1, 0, "c1", " "));
        assertDoesNotThrow(() -> new AgentEvent.ModelResponseCompleted(
                1, TokenUsage.unknown(), false));
    }

    @Test
    void modelEventsDoNotExposeSensitiveProviderFields() {
        String fields = Arrays.stream(AgentEvent.class.getPermittedSubclasses())
                .flatMap(type -> Arrays.stream(type.getRecordComponents()))
                .map(RecordComponent::getName)
                .reduce("", (left, right) -> left + " " + right)
                .toLowerCase();

        assertFalse(fields.contains("signature"));
        assertFalse(fields.contains("encrypted"));
        assertFalse(fields.contains("jsonfragment"));
        assertFalse(fields.contains("arguments"));
    }

    @Test
    void validatesSafeRetryEvent() {
        assertDoesNotThrow(() -> new AgentEvent.RetryScheduled(
                1, 2, LlmErrorType.NETWORK, Duration.ofSeconds(1), 8_000));
        assertThrows(IllegalArgumentException.class, () -> new AgentEvent.RetryScheduled(
                1, 5, LlmErrorType.NETWORK, Duration.ZERO, 8_000));
        assertThrows(IllegalArgumentException.class, () -> new AgentEvent.RetryScheduled(
                1, 2, LlmErrorType.NETWORK, Duration.ofSeconds(-1), 8_000));
    }
}
