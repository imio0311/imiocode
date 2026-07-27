package io.imiocode.terminal;

import io.imiocode.llm.TokenUsage;
import io.imiocode.llm.TokenUsageBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UsageFormatterTest {
    private final UsageFormatter formatter = new UsageFormatter();

    @Test
    void hidesUnknownUsage() {
        assertEquals("", formatter.format(TokenUsage.unknown()));
    }

    @Test
    void rendersOnlyKnownFields() {
        String formatted = formatter.format(
                new TokenUsageBuilder().input(12).output(7).cacheRead(3).build());

        assertEquals("[usage] input=12 · output=7 · cache-read=3", formatted);
        assertFalse(formatted.contains("reasoning="));
        assertFalse(formatted.contains("cache-write="));
    }
}
