package io.imiocode.config;

/** 可安全展示给用户的配置提示。 */
public record ConfigNotice(String code, String safeMessage) {
    public ConfigNotice {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code 不能为空");
        }
        if (safeMessage == null || safeMessage.isBlank()) {
            throw new IllegalArgumentException("safeMessage 不能为空");
        }
        code = code.trim();
        safeMessage = safeMessage.trim();
    }
}
