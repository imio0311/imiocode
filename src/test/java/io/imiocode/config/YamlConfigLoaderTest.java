package io.imiocode.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlConfigLoaderTest {
    private static final Pattern API_KEY_PATTERN = Pattern.compile("sk-[A-Za-z0-9]{20,}");
    private final YamlConfigLoader loader = new YamlConfigLoader();

    @TempDir
    Path tempDirectory;

    @Test
    void missingFileProducesEmptyDocument() {
        ConfigDocument document = loader.load(tempDirectory);

        assertEquals(ConfigDocument.empty(), document);
    }

    @Test
    void loadsAllProviderGroups() throws Exception {
        write("""
                provider: anthropic
                model: claude-test
                providers:
                  openai:
                    api-key: openai-secret
                    base-url: https://openai.example
                  anthropic:
                    api-key: anthropic-secret
                    base-url: https://anthropic.example
                  deepseek:
                    api-key: deepseek-secret
                    base-url: https://deepseek.example
                """);

        ConfigDocument document = loader.load(tempDirectory);

        assertEquals("openai-secret", document.providerConfig(Provider.OPENAI).apiKey());
        assertEquals("anthropic-secret", document.providerConfig(Provider.ANTHROPIC).apiKey());
        assertEquals("deepseek-secret", document.providerConfig(Provider.DEEPSEEK).apiKey());
        assertFalse(document.toString().contains("anthropic-secret"));
    }

    @Test
    void rejectsUnknownTopLevelFieldWithoutLeakingValue() throws Exception {
        write("""
                provider: deepseek
                model: test
                secret-typo: should-never-leak
                """);

        ConfigException exception = assertThrows(ConfigException.class, () -> loader.load(tempDirectory));

        assertTrue(exception.getMessage().contains("secret-typo"));
        assertFalse(exception.getMessage().contains("should-never-leak"));
    }

    @Test
    void rejectsUnknownProviderAndWrongScalarType() throws Exception {
        write("""
                provider: deepseek
                model: test
                providers:
                  other-vendor:
                    api-key: hidden
                """);
        ConfigException unknownProvider = assertThrows(ConfigException.class, () -> loader.load(tempDirectory));
        assertTrue(unknownProvider.getMessage().contains("未知厂商"));
        assertFalse(unknownProvider.getMessage().contains("hidden"));

        write("""
                provider: deepseek
                model: test
                max-output-tokens: not-a-number
                """);
        ConfigException wrongType = assertThrows(ConfigException.class, () -> loader.load(tempDirectory));
        assertTrue(wrongType.getMessage().contains("max-output-tokens"));
        assertFalse(wrongType.getMessage().contains("not-a-number"));
    }

    @Test
    void rejectsMalformedYamlWithoutEchoingInput() throws Exception {
        write("provider: [deepseek\napi-key: highly-sensitive-value");

        ConfigException exception = assertThrows(ConfigException.class, () -> loader.load(tempDirectory));

        assertTrue(exception.getMessage().contains("config.yaml 格式错误"));
        assertFalse(exception.getMessage().contains("highly-sensitive-value"));
    }

    @Test
    void exampleContainsNoRealKeyAndGitignoreProtectsLocalConfig() throws Exception {
        String example = Files.readString(Path.of("config.example.yaml"), StandardCharsets.UTF_8);
        String gitignore = Files.readString(Path.of(".gitignore"), StandardCharsets.UTF_8);

        assertFalse(API_KEY_PATTERN.matcher(example).find());
        assertTrue(gitignore.lines().anyMatch("/config.yaml"::equals));
    }

    private void write(String yaml) throws Exception {
        Files.writeString(tempDirectory.resolve("config.yaml"), yaml, StandardCharsets.UTF_8);
    }
}
