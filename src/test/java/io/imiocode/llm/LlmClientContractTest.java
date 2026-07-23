package io.imiocode.llm;

import io.imiocode.config.AppConfig;
import io.imiocode.config.Provider;
import io.imiocode.llm.provider.anthropic.AnthropicClient;
import io.imiocode.llm.provider.deepseek.DeepSeekClient;
import io.imiocode.llm.provider.openai.OpenAiClient;
import io.imiocode.llm.transport.HttpErrorMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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

    private static AppConfig config(Provider provider) {
        return new AppConfig(provider, "test-model", "test-key", URI.create("http://127.0.0.1:1"),
                Duration.ofSeconds(1), Duration.ofSeconds(1), 128);
    }
}
