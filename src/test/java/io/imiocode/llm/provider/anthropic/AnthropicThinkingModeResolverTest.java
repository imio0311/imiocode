package io.imiocode.llm.provider.anthropic;

import io.imiocode.config.AppConfig;
import io.imiocode.config.Provider;
import io.imiocode.config.ReasoningEffort;
import io.imiocode.config.ReasoningSummary;
import io.imiocode.config.ThinkingConfig;
import io.imiocode.config.ThinkingMode;
import io.imiocode.llm.LlmException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnthropicThinkingModeResolverTest {
    private final AnthropicThinkingModeResolver resolver = new AnthropicThinkingModeResolver();

    @Test
    void autoSelectsAdaptiveForNewModels() throws Exception {
        assertEquals(ThinkingMode.ADAPTIVE, resolver.resolve(config("claude-sonnet-4-6")));
    }

    @Test
    void autoSelectsManualForOlderModels() throws Exception {
        assertEquals(ThinkingMode.MANUAL, resolver.resolve(config("claude-sonnet-4-5-20250929")));
    }

    @Test
    void autoRejectsUnknownModels() {
        assertThrows(LlmException.class, () -> resolver.resolve(config("custom-model")));
    }

    private static AppConfig config(String model) {
        ThinkingConfig thinking = new ThinkingConfig(
                true, ThinkingMode.AUTO, 1024, ReasoningEffort.HIGH, ReasoningSummary.AUTO);
        return new AppConfig(
                Provider.ANTHROPIC,
                model,
                "test-key",
                URI.create("https://api.anthropic.com"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                4096,
                thinking);
    }
}
