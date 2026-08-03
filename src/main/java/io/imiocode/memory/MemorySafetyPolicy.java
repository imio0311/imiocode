package io.imiocode.memory;

import io.imiocode.config.MemoryConfig;
import io.imiocode.tool.SecretRedactor;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/** 自动记忆采用白名单，并在所有写入路径上拒绝秘密和过大正文。 */
public final class MemorySafetyPolicy {
    private static final Pattern CREDENTIAL = Pattern.compile(
            "(?i)(api[-_ ]?key|token|password|secret|credential|密码|密钥|令牌)\\s*[:=：]");
    private static final Pattern PERSONAL = Pattern.compile(
            "(?i)(身份证|护照号|银行卡|家庭住址|手机号|phone\\s*number|social\\s*security|\\b1[3-9]\\d{9}\\b)");
    private static final Pattern TEMPORARY = Pattern.compile(
            "(?i)(这次任务|当前任务|临时|今天先|稍后再|for this task|temporary|right now)");
    private final MemoryConfig config;
    private final SecretRedactor redactor;

    public MemorySafetyPolicy(MemoryConfig config, SecretRedactor redactor) {
        this.config = config; this.redactor = redactor;
    }

    public void validate(MemoryScope scope, MemoryCategory category, String content, boolean automatic) {
        if (!scopeEnabled(scope)) throw new MemoryException("目标记忆作用域已关闭");
        String normalized = MemoryEntry.normalizeLine(content);
        if (normalized.length() > config.maxEntryChars()) throw new MemoryException("记忆内容超过单条长度限制");
        if (normalized.getBytes(StandardCharsets.UTF_8).length > config.maxFileBytes()) throw new MemoryException("记忆内容超过文件容量限制");
        if (!redactor.redact(normalized).equals(normalized) || CREDENTIAL.matcher(normalized).find()) {
            throw new MemoryException("记忆内容包含秘密或凭据");
        }
        if (PERSONAL.matcher(normalized).find()) throw new MemoryException("记忆内容包含个人敏感信息");
        if (automatic && TEMPORARY.matcher(normalized).find()) throw new MemoryException("临时信息不写入长期记忆");
        if (automatic && category == MemoryCategory.PREFERENCE && scope != MemoryScope.USER) {
            throw new MemoryException("用户偏好只能写入 user 作用域");
        }
        if (automatic && category == MemoryCategory.PROJECT_FACT && scope != MemoryScope.PROJECT) {
            throw new MemoryException("项目事实只能写入 project 作用域");
        }
    }

    public boolean scopeEnabled(MemoryScope scope) {
        return config.enabled() && (scope == MemoryScope.USER ? config.userScopeEnabled() : config.projectScopeEnabled());
    }
}
