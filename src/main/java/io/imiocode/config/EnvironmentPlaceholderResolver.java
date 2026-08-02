package io.imiocode.config;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 展开配置值中的 ${NAME} 启动环境变量占位符。 */
public final class EnvironmentPlaceholderResolver {
    private static final Pattern PLACEHOLDER = Pattern.compile(
            "\\$\\{([A-Za-z_][A-Za-z0-9_]*)}");

    public String expand(
            String value,
            Map<String, String> environment,
            Consumer<String> secretRegistrar) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(secretRegistrar, "secretRegistrar");

        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement = environment.get(name);
            if (replacement == null) {
                throw new MissingEnvironmentVariableException(name);
            }
            if (!replacement.isBlank()) {
                secretRegistrar.accept(replacement);
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
