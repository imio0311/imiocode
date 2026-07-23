package io.imiocode.tool;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 对应用已知的密钥和认证字段进行保守脱敏。 */
public final class SecretRedactor {
    private static final String REDACTED = "***";
    private static final Pattern BEARER = Pattern.compile(
            "(?i)(\\b(?:authorization\\s*:\\s*)?bearer\\s+)([^\\s,;]+)");
    private static final Pattern API_KEY_HEADER = Pattern.compile(
            "(?i)(\\b(?:x-api-key|api-key)\\s*[:=]\\s*)([^\\s,;]+)");
    private static final Pattern SENSITIVE_ENVIRONMENT = Pattern.compile(
            "(?i).*(KEY|TOKEN|SECRET|PASSWORD|CREDENTIAL).*");

    private final String apiKey;

    public SecretRedactor(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    public String redact(String value) {
        if (value == null || value.isEmpty()) {
            return Objects.requireNonNullElse(value, "");
        }
        String redacted = value;
        if (!apiKey.isBlank()) {
            redacted = redacted.replace(apiKey, REDACTED);
        }
        redacted = replaceSecret(BEARER, redacted);
        return replaceSecret(API_KEY_HEADER, redacted);
    }

    public boolean isSensitiveEnvironmentName(String name) {
        return name != null && SENSITIVE_ENVIRONMENT.matcher(name).matches();
    }

    public void removeSensitiveEnvironment(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment")
                .keySet()
                .removeIf(this::isSensitiveEnvironmentName);
    }

    @Override
    public String toString() {
        return "SecretRedactor[apiKey=***]";
    }

    private static String replaceSecret(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group(1) + REDACTED));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
