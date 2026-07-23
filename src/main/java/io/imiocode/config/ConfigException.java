package io.imiocode.config;

public final class ConfigException extends RuntimeException {
    public ConfigException(String safeMessage) {
        super(safeMessage);
    }

    public ConfigException(String safeMessage, Throwable cause) {
        super(safeMessage, cause);
    }
}
