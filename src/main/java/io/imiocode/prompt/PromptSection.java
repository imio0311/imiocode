package io.imiocode.prompt;

/** 固定 Prompt 模块的统一契约。 */
@FunctionalInterface
public interface PromptSection {
    Section section();
}
