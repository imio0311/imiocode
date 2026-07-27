package io.imiocode.llm.transport;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryAfterParserTest {
    private final RetryAfterParser parser = new RetryAfterParser();
    private final Instant now = Instant.parse("2025-01-01T00:00:00Z");

    @Test
    void parsesDelaySeconds() {
        assertEquals(Duration.ofSeconds(15), parser.parse("15", now).orElseThrow());
    }

    @Test
    void parsesHttpDate() {
        assertEquals(
                Duration.ofSeconds(20),
                parser.parse("Wed, 1 Jan 2025 00:00:20 GMT", now).orElseThrow());
    }

    @Test
    void ignoresInvalidNegativeAndPastValues() {
        assertTrue(parser.parse("invalid", now).isEmpty());
        assertTrue(parser.parse("-1", now).isEmpty());
        assertTrue(parser.parse("Tue, 31 Dec 2024 23:59:59 GMT", now).isEmpty());
    }
}
