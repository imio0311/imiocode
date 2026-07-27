package io.imiocode.llm;

import io.imiocode.config.AppConfig;
import io.imiocode.config.Provider;
import io.imiocode.llm.provider.anthropic.AnthropicClient;
import io.imiocode.llm.provider.deepseek.DeepSeekClient;
import io.imiocode.llm.provider.openai.OpenAiClient;
import io.imiocode.llm.transport.HttpErrorMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmClientContractTest {
    @Test
    void factoryCreatesExactlyTheConfiguredProvider() {
        LlmClientFactory factory = new LlmClientFactory();
        assertInstanceOf(OpenAiClient.class, factory.create(config(Provider.OPENAI)));
        assertInstanceOf(AnthropicClient.class, factory.create(config(Provider.ANTHROPIC)));
        assertInstanceOf(DeepSeekClient.class, factory.create(config(Provider.DEEPSEEK)));
    }

    @Test
    void clientsCanBeClosedRepeatedly() {
        LlmClientFactory factory = new LlmClientFactory();
        for (Provider provider : Provider.values()) {
            LlmClient client = factory.create(config(provider));
            assertDoesNotThrow(client::close);
            assertDoesNotThrow(client::close);
        }
    }

    @Test
    void httpErrorsUseCommonCategories() {
        HttpErrorMapper mapper = new HttpErrorMapper();
        assertEquals(LlmErrorType.AUTHENTICATION, mapper.fromStatus(401, "").type());
        assertEquals(LlmErrorType.RATE_LIMIT, mapper.fromStatus(429, "").type());
        assertEquals(LlmErrorType.MODEL_NOT_FOUND, mapper.fromStatus(404, "model_not_found").type());
        assertEquals(LlmErrorType.SERVER_ERROR, mapper.fromStatus(503, "").type());
    }

    @Test
    void onlyRateLimitCarriesRetryAfter() {
        HttpErrorMapper mapper = new HttpErrorMapper();
        HttpHeaders headers = HttpHeaders.of(
                Map.of("Retry-After", List.of("15")),
                (name, value) -> true);
        Instant now = Instant.parse("2025-01-01T00:00:00Z");

        assertEquals(
                Duration.ofSeconds(15),
                mapper.fromStatus(429, "", headers, now).retryAfter().orElseThrow());
        assertTrue(mapper.fromStatus(401, "", headers, now).retryAfter().isEmpty());
        assertTrue(mapper.fromStatus(500, "", headers, now).retryAfter().isEmpty());
    }

    private static AppConfig config(Provider provider) {
        return new AppConfig(provider, "test-model", "test-key", URI.create("http://127.0.0.1:1"),
                Duration.ofSeconds(1), Duration.ofSeconds(1), 128);
    }
}
