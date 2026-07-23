package io.imiocode.tool;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretRedactorTest {
    @Test
    void redactsConfiguredKeyAndAuthenticationHeaders() {
        SecretRedactor redactor = new SecretRedactor("sk-test-中文");

        String output = redactor.redact("""
                key=sk-test-中文
                Authorization: Bearer another-secret
                x-api-key: third-secret
                normal=value
                """);

        assertFalse(output.contains("sk-test-中文"));
        assertFalse(output.contains("another-secret"));
        assertFalse(output.contains("third-secret"));
        assertTrue(output.contains("normal=value"));
    }

    @Test
    void removesSensitiveEnvironmentNamesWithoutRemovingOrdinaryValues() {
        SecretRedactor redactor = new SecretRedactor("x");
        Map<String, String> environment = new HashMap<>();
        environment.put("OPENAI_API_KEY", "secret");
        environment.put("ACCESS_TOKEN", "secret");
        environment.put("DB_PASSWORD", "secret");
        environment.put("PATH", "kept");
        environment.put("LANG", "zh_CN.UTF-8");

        redactor.removeSensitiveEnvironment(environment);

        assertEquals(Map.of("PATH", "kept", "LANG", "zh_CN.UTF-8"), environment);
    }

    @Test
    void toStringNeverContainsKey() {
        assertEquals("SecretRedactor[apiKey=***]", new SecretRedactor("top-secret").toString());
    }
}
