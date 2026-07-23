package io.imiocode.terminal;

import java.nio.file.Path;
import java.util.Objects;

/** 启动面板和输入区渲染所需的非敏感上下文。 */
public record UiContext(
        String productName,
        String version,
        String provider,
        String model,
        Path workingDirectory) {

    public UiContext {
        productName = requireText(productName, "productName");
        version = requireText(version, "version");
        provider = requireText(provider, "provider");
        model = requireText(model, "model");
        workingDirectory = Objects.requireNonNull(workingDirectory, "workingDirectory")
                .toAbsolutePath()
                .normalize();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
