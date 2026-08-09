package io.imiocode.permission.command;

import java.util.List;

/** 单个 Shell 命令段的有限分词结果。 */
public record ShellTokenizeResult(boolean valid, List<String> tokens, String reason) {
    public ShellTokenizeResult {
        tokens = tokens == null ? List.of() : List.copyOf(tokens);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason 不能为空");
        }
        reason = reason.trim();
        if (valid && tokens.isEmpty()) {
            throw new IllegalArgumentException("有效分词结果不能为空");
        }
    }

    public static ShellTokenizeResult valid(List<String> tokens) {
        return new ShellTokenizeResult(true, tokens, "命令段分词成功");
    }

    public static ShellTokenizeResult invalid(String reason) {
        return new ShellTokenizeResult(false, List.of(), reason);
    }
}
