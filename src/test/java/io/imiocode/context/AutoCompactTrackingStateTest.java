package io.imiocode.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoCompactTrackingStateTest {
    @Test
    void opensAfterThreeConsecutiveFailuresAndSuccessResets() {
        AutoCompactTrackingState state = new AutoCompactTrackingState();
        assertFalse(state.recordFailure());
        assertFalse(state.recordFailure());
        assertTrue(state.recordFailure());
        assertTrue(state.circuitOpen());
        assertEquals(3, state.consecutiveFailures());

        state.recordSuccess();
        assertFalse(state.circuitOpen());
        assertEquals(0, state.consecutiveFailures());
    }

    @Test
    void instancesDoNotShareFailures() {
        AutoCompactTrackingState first = new AutoCompactTrackingState();
        first.recordFailure();
        AutoCompactTrackingState second = new AutoCompactTrackingState();
        assertEquals(0, second.consecutiveFailures());
    }
}
