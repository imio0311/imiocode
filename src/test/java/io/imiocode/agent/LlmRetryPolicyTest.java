package io.imiocode.agent;

import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmRetryPolicyTest {
    private final LlmRetryPolicy policy = new LlmRetryPolicy();

    @Test
    void usesBoundedBackoffAndRetryAfter() {
        assertEquals(Duration.ofSeconds(1), decide(LlmErrorType.NETWORK, 0).delay());
        assertEquals(Duration.ofSeconds(2), decide(LlmErrorType.SERVER_ERROR, 1).delay());
        assertEquals(Duration.ofSeconds(4), decide(LlmErrorType.TIMEOUT, 2).delay());

        LlmException limited = new LlmException(
                LlmErrorType.RATE_LIMIT, true, 429, "限流",
                Duration.ofSeconds(7), null);
        assertEquals(Duration.ofSeconds(7), policy.decide(
                limited, 0, 8_000, Duration.ofSeconds(10), false)
                .orElseThrow().delay());
    }

    @Test
    void doublesOutputLimitAndStopsAtCeiling() {
        RetryDecision decision = policy.decide(
                error(LlmErrorType.OUTPUT_LIMIT),
                0,
                32_000,
                Duration.ofSeconds(10),
                false).orElseThrow();
        assertEquals(64_000, decision.outputTokenLimit());
        assertTrue(decision.delay().isZero());
        assertTrue(policy.decide(
                error(LlmErrorType.OUTPUT_LIMIT),
                1,
                64_000,
                Duration.ofSeconds(10),
                false).isEmpty());
    }

    @Test
    void rejectsUnsafeExhaustedAndPermanentFailures() {
        assertTrue(policy.decide(error(LlmErrorType.NETWORK), 0, 8_000,
                Duration.ofSeconds(10), true).isEmpty());
        assertTrue(policy.decide(error(LlmErrorType.NETWORK), 3, 8_000,
                Duration.ofSeconds(10), false).isEmpty());
        assertTrue(policy.decide(error(LlmErrorType.AUTHENTICATION), 0, 8_000,
                Duration.ofSeconds(10), false).isEmpty());
    }

    private RetryDecision decide(LlmErrorType type, int retries) {
        return policy.decide(error(type), retries, 8_000,
                Duration.ofSeconds(10), false).orElseThrow();
    }

    private static LlmException error(LlmErrorType type) {
        return new LlmException(type, true, null, type.name());
    }
}
