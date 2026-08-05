package io.imiocode.terminal;

import java.util.List;

/** JLine 无关的 Slash Command 补全数据源。 */
@FunctionalInterface
public interface SlashCompletionSource {
    List<String> complete(String prefix);

    static SlashCompletionSource empty() {
        return prefix -> List.of();
    }
}
