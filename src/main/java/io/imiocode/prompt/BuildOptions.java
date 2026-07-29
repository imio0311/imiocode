package io.imiocode.prompt;

import java.util.Optional;

/** 后续章节可填充的稳定 Prompt 内容插槽。 */
public record BuildOptions(
        Optional<String> customInstructions,
        Optional<String> skillContent,
        Optional<String> memoryContent
) {
    public BuildOptions {
        customInstructions = normalize(customInstructions);
        skillContent = normalize(skillContent);
        memoryContent = normalize(memoryContent);
    }

    public BuildOptions(
            String customInstructions,
            String skillContent,
            String memoryContent
    ) {
        this(
                Optional.ofNullable(customInstructions),
                Optional.ofNullable(skillContent),
                Optional.ofNullable(memoryContent));
    }

    public static BuildOptions empty() {
        return new BuildOptions(
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static Optional<String> normalize(Optional<String> value) {
        return value == null
                ? Optional.empty()
                : value.map(String::trim).filter(content -> !content.isEmpty());
    }
}
