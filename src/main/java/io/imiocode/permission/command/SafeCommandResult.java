package io.imiocode.permission.command;

/** 安全命令检测结果；无法证明安全不等于拒绝，只是不自动放行。 */
public record SafeCommandResult(
        boolean safe,
        String reason
) {
    public SafeCommandResult {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason 不能为空");
        }
        reason = reason.trim();
    }

    public static SafeCommandResult safe(String reason) {
        return new SafeCommandResult(true, reason);
    }

    public static SafeCommandResult uncertain(String reason) {
        return new SafeCommandResult(false, reason);
    }
}
