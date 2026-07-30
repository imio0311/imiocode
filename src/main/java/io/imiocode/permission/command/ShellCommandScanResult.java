package io.imiocode.permission.command;

import java.util.List;

/** Shell 命令的有限扫描结果；不尝试表达完整的 Shell 语法树。 */
public record ShellCommandScanResult(
        boolean eligible,
        List<String> segments,
        String reason
) {
    public ShellCommandScanResult {
        segments = segments == null ? List.of() : List.copyOf(segments);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason 不能为空");
        }
        reason = reason.trim();
        if (eligible && segments.isEmpty()) {
            throw new IllegalArgumentException("可判断的命令必须包含至少一个命令段");
        }
    }

    public static ShellCommandScanResult eligible(List<String> segments) {
        return new ShellCommandScanResult(true, segments, "命令语法可进行安全分类");
    }

    public static ShellCommandScanResult ineligible(String reason) {
        return new ShellCommandScanResult(false, List.of(), reason);
    }
}
