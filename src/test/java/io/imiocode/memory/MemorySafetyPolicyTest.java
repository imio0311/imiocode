package io.imiocode.memory;

import io.imiocode.config.MemoryConfig;
import io.imiocode.tool.SecretRedactor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemorySafetyPolicyTest {
    private final MemorySafetyPolicy policy = new MemorySafetyPolicy(
            MemoryConfig.defaults(), new SecretRedactor("known-secret"));

    @Test
    void allowsStableInformationInMatchingScopes() {
        assertDoesNotThrow(() -> policy.validate(
                MemoryScope.USER, MemoryCategory.PREFERENCE, "用户偏好中文简洁回答", true));
        assertDoesNotThrow(() -> policy.validate(
                MemoryScope.PROJECT, MemoryCategory.PROJECT_FACT, "项目使用 Java 21", true));
    }

    @Test
    void rejectsSecretsPersonalDataTemporaryFactsAndWrongScope() {
        assertThrows(MemoryException.class, () -> policy.validate(
                MemoryScope.USER, MemoryCategory.PREFERENCE, "api-key: known-secret", false));
        assertThrows(MemoryException.class, () -> policy.validate(
                MemoryScope.USER, MemoryCategory.PREFERENCE, "手机号 13812345678", true));
        assertThrows(MemoryException.class, () -> policy.validate(
                MemoryScope.PROJECT, MemoryCategory.PROJECT_FACT, "当前任务临时使用脚本", true));
        assertThrows(MemoryException.class, () -> policy.validate(
                MemoryScope.PROJECT, MemoryCategory.PREFERENCE, "偏好中文", true));
    }
}
