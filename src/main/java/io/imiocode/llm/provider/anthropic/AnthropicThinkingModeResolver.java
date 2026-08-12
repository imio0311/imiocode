package io.imiocode.llm.provider.anthropic;

import io.imiocode.config.AppConfig;
import io.imiocode.config.ThinkingMode;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;

import java.util.Locale;

/**
 * 根据 Anthropic 模型族和显式配置选择 Thinking 协议模式。
 *
 * <p>无法可靠识别的模型会拒绝自动猜测，要求用户明确配置，避免发送 Provider 不接受的请求结构。</p>
 */
public final class AnthropicThinkingModeResolver {
    public ThinkingMode resolve(AppConfig config) throws LlmException {
        if (!config.thinking().enabled()) {
            return config.thinking().mode();
        }
        ThinkingMode configured = config.thinking().mode();
        if (configured != ThinkingMode.AUTO) {
            validateManual(config, configured);
            return configured;
        }
        String model = config.model().toLowerCase(Locale.ROOT);
        // 模型命名规则只用于已知系列；未知未来型号必须走上面的显式配置路径。
        ThinkingMode resolved;
        if (model.startsWith("claude-3")
                || model.matches(".*-(?:opus|sonnet)-4-(?:[0-5])(?:-|$).*")) {
            resolved = ThinkingMode.MANUAL;
        } else if (model.contains("mythos") || model.contains("fable")
                || model.matches(".*-(?:opus|sonnet)-4-[6-9](?:-|$).*")
                || model.matches(".*-(?:opus|sonnet)-[5-9](?:-|$).*")
                || model.matches("claude-(?:4-[6-9]|[5-9])(?:-|$).*")) {
            resolved = ThinkingMode.ADAPTIVE;
        } else {
            throw new LlmException(
                    LlmErrorType.PROTOCOL,
                    true,
                    null,
                    "无法判断当前 Anthropic 模型的 Thinking 模式，请显式配置 adaptive 或 manual");
        }
        validateManual(config, resolved);
        return resolved;
    }

    private static void validateManual(AppConfig config, ThinkingMode mode) throws LlmException {
        if (mode == ThinkingMode.MANUAL
                && (config.thinking().budgetTokens() < 1024
                || config.thinking().budgetTokens() >= config.maxOutputTokens())) {
            throw new LlmException(
                    LlmErrorType.PROTOCOL,
                    true,
                    null,
                    "Anthropic manual Thinking 预算必须至少为 1024 且小于最大输出 Token");
        }
    }
}
