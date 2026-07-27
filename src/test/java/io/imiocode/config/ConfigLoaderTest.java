package io.imiocode.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {
    private final ConfigLoader loader = new ConfigLoader();

    @TempDir
    Path tempDirectory;

    @Test
    void loadsDefaultsForEveryProvider() {
        assertProvider("openai", "OPENAI_API_KEY", Provider.OPENAI, "https://api.openai.com");
        assertProvider("ANTHROPIC", "ANTHROPIC_API_KEY", Provider.ANTHROPIC, "https://api.anthropic.com");
        assertProvider("DeepSeek", "DEEPSEEK_API_KEY", Provider.DEEPSEEK, "https://api.deepseek.com");
    }

    @Test
    void loadsOptionalOverrides() {
        Map<String, String> environment = baseEnvironment("openai", "OPENAI_API_KEY");
        environment.put("OPENAI_BASE_URL", "http://localhost:8080/");
        environment.put("IMIO_CONNECT_TIMEOUT_SECONDS", "3");
        environment.put("IMIO_REQUEST_TIMEOUT_SECONDS", "7");
        environment.put("IMIO_MAX_OUTPUT_TOKENS", "1234");

        AppConfig config = loader.load(tempDirectory, environment);

        assertEquals(URI.create("http://localhost:8080"), config.baseUri());
        assertEquals(Duration.ofSeconds(3), config.connectTimeout());
        assertEquals(Duration.ofSeconds(7), config.requestTimeout());
        assertEquals(1234, config.maxOutputTokens());
        assertFalse(config.thinking().enabled());
    }

    @Test
    void loadsThinkingConfigurationAndEnvironmentOverrides() throws Exception {
        writeYaml("""
                provider: openai
                model: gpt-5
                max-output-tokens: 4096
                thinking:
                  enabled: true
                  mode: adaptive
                  budget-tokens: 2048
                  effort: medium
                  summary: concise
                providers:
                  openai:
                    api-key: yaml-secret-key
                """);
        Map<String, String> environment = Map.of(
                "IMIO_REASONING_EFFORT", "low",
                "IMIO_REASONING_SUMMARY", "detailed");

        AppConfig config = loader.load(tempDirectory, environment);

        assertTrue(config.thinking().enabled());
        assertEquals(ThinkingMode.ADAPTIVE, config.thinking().mode());
        assertEquals(2048, config.thinking().budgetTokens());
        assertEquals(ReasoningEffort.LOW, config.thinking().effort());
        assertEquals(ReasoningSummary.DETAILED, config.thinking().summary());
    }

    @Test
    void rejectsInvalidThinkingConfiguration() {
        Map<String, String> invalidBoolean = baseEnvironment("openai", "OPENAI_API_KEY");
        invalidBoolean.put("IMIO_THINKING_ENABLED", "yes");
        assertTrue(assertThrows(
                ConfigException.class,
                () -> loader.load(tempDirectory, invalidBoolean)).getMessage()
                .contains("IMIO_THINKING_ENABLED"));

        Map<String, String> invalidMode = baseEnvironment("openai", "OPENAI_API_KEY");
        invalidMode.put("IMIO_THINKING_MODE", "magic");
        assertTrue(assertThrows(
                ConfigException.class,
                () -> loader.load(tempDirectory, invalidMode)).getMessage()
                .contains("IMIO_THINKING_MODE"));

        Map<String, String> invalidBudget = baseEnvironment("anthropic", "ANTHROPIC_API_KEY");
        invalidBudget.put("IMIO_THINKING_ENABLED", "true");
        invalidBudget.put("IMIO_THINKING_MODE", "manual");
        invalidBudget.put("IMIO_THINKING_BUDGET_TOKENS", "512");
        assertTrue(assertThrows(
                ConfigException.class,
                () -> loader.load(tempDirectory, invalidBudget)).getMessage()
                .contains("IMIO_THINKING_BUDGET_TOKENS"));
    }

    @Test
    void rejectsMissingAndInvalidValuesWithoutLeakingKey() {
        ConfigException missing = assertThrows(ConfigException.class, () -> loader.load(tempDirectory, Map.of()));
        assertTrue(missing.getMessage().contains("IMIO_PROVIDER"));

        Map<String, String> invalidProviderEnvironment = baseEnvironment("unknown", "OPENAI_API_KEY");
        invalidProviderEnvironment.put("OPENAI_API_KEY", "secret-config-test-key");
        ConfigException invalidProvider = assertThrows(
                ConfigException.class,
                () -> loader.load(tempDirectory, invalidProviderEnvironment));
        assertFalse(invalidProvider.getMessage().contains("secret-config-test-key"));

        Map<String, String> invalidNumberEnvironment = baseEnvironment("openai", "OPENAI_API_KEY");
        invalidNumberEnvironment.put("IMIO_MAX_OUTPUT_TOKENS", "0");
        ConfigException invalidNumber = assertThrows(
                ConfigException.class,
                () -> loader.load(tempDirectory, invalidNumberEnvironment));
        assertTrue(invalidNumber.getMessage().contains("IMIO_MAX_OUTPUT_TOKENS"));

        Map<String, String> invalidUriEnvironment = baseEnvironment("openai", "OPENAI_API_KEY");
        invalidUriEnvironment.put("OPENAI_BASE_URL", "not-a-uri");
        ConfigException invalidUri = assertThrows(
                ConfigException.class,
                () -> loader.load(tempDirectory, invalidUriEnvironment));
        assertTrue(invalidUri.getMessage().contains("OPENAI_BASE_URL"));
    }

    @Test
    void appConfigToStringRedactsApiKey() {
        AppConfig config = loader.load(tempDirectory, baseEnvironment("openai", "OPENAI_API_KEY"));
        assertFalse(config.toString().contains("secret-config-test-key"));
        assertTrue(config.toString().contains("apiKey=***"));
    }

    @Test
    void loadsYamlOnlyConfiguration() throws Exception {
        writeYaml("""
                provider: deepseek
                model: deepseek-chat
                connect-timeout-seconds: 4
                request-timeout-seconds: 9
                max-output-tokens: 777
                providers:
                  deepseek:
                    api-key: yaml-secret-key
                    base-url: http://localhost:9876
                """);

        AppConfig config = loader.load(tempDirectory, Map.of());

        assertEquals(Provider.DEEPSEEK, config.provider());
        assertEquals("deepseek-chat", config.model());
        assertEquals("yaml-secret-key", config.apiKey());
        assertEquals(URI.create("http://localhost:9876"), config.baseUri());
        assertEquals(Duration.ofSeconds(4), config.connectTimeout());
        assertEquals(Duration.ofSeconds(9), config.requestTimeout());
        assertEquals(777, config.maxOutputTokens());
    }

    @Test
    void environmentOverridesOnlyMatchingYamlFields() throws Exception {
        writeYaml("""
                provider: deepseek
                model: yaml-model
                connect-timeout-seconds: 4
                max-output-tokens: 777
                providers:
                  deepseek:
                    api-key: yaml-secret-key
                    base-url: http://localhost:9876
                """);
        Map<String, String> environment = Map.of(
                "IMIO_MODEL", "environment-model",
                "IMIO_CONNECT_TIMEOUT_SECONDS", "6",
                "DEEPSEEK_BASE_URL", "http://localhost:9999");

        AppConfig config = loader.load(tempDirectory, environment);

        assertEquals(Provider.DEEPSEEK, config.provider());
        assertEquals("environment-model", config.model());
        assertEquals("yaml-secret-key", config.apiKey());
        assertEquals(URI.create("http://localhost:9999"), config.baseUri());
        assertEquals(Duration.ofSeconds(6), config.connectTimeout());
        assertEquals(777, config.maxOutputTokens());
    }

    @Test
    void blankEnvironmentValueDoesNotEraseYamlValue() throws Exception {
        writeYaml("""
                provider: openai
                model: yaml-model
                providers:
                  openai:
                    api-key: yaml-secret-key
                """);

        AppConfig config = loader.load(tempDirectory, Map.of("IMIO_MODEL", "  ", "OPENAI_API_KEY", ""));

        assertEquals("yaml-model", config.model());
        assertEquals("yaml-secret-key", config.apiKey());
    }

    @Test
    void rejectsInvalidYamlUriAndMissingSelectedProviderKeyWithoutLeakingOtherKey() throws Exception {
        writeYaml("""
                provider: deepseek
                model: test
                providers:
                  openai:
                    api-key: other-provider-secret
                  deepseek:
                    base-url: not-a-uri
                """);

        ConfigException invalidUri = assertThrows(
                ConfigException.class,
                () -> loader.load(tempDirectory, Map.of("DEEPSEEK_API_KEY", "deepseek-test-key")));
        assertTrue(invalidUri.getMessage().contains("providers.deepseek.base-url"));

        writeYaml("""
                provider: deepseek
                model: test
                providers:
                  openai:
                    api-key: other-provider-secret
                  deepseek:
                    base-url: https://api.deepseek.com
                """);
        ConfigException missingKey = assertThrows(
                ConfigException.class,
                () -> loader.load(tempDirectory, Map.of()));
        assertTrue(missingKey.getMessage().contains("providers.deepseek.api-key"));
        assertFalse(missingKey.getMessage().contains("other-provider-secret"));
    }

    private void assertProvider(String value, String keyName, Provider expectedProvider, String expectedBaseUrl) {
        AppConfig config = loader.load(tempDirectory, baseEnvironment(value, keyName));
        assertEquals(expectedProvider, config.provider());
        assertEquals("test-model", config.model());
        assertEquals(URI.create(expectedBaseUrl), config.baseUri());
        assertEquals(ConfigLoader.DEFAULT_CONNECT_TIMEOUT, config.connectTimeout());
        assertEquals(ConfigLoader.DEFAULT_REQUEST_TIMEOUT, config.requestTimeout());
        assertEquals(ConfigLoader.DEFAULT_MAX_OUTPUT_TOKENS, config.maxOutputTokens());
    }

    private static Map<String, String> baseEnvironment(String provider, String keyName) {
        Map<String, String> environment = new HashMap<>();
        environment.put("IMIO_PROVIDER", provider);
        environment.put("IMIO_MODEL", "test-model");
        environment.put(keyName, "secret-config-test-key");
        return environment;
    }

    private void writeYaml(String yaml) throws Exception {
        Files.writeString(tempDirectory.resolve("config.yaml"), yaml, StandardCharsets.UTF_8);
    }
}
