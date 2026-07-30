package io.imiocode.permission.sandbox;

/** 路径沙箱的预检结果。 */
public record SandboxResult(boolean allowed, String reason) {
    public SandboxResult {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("沙箱原因不能为空");
        }
        reason = reason.trim();
    }

    public static SandboxResult allow() {
        return new SandboxResult(true, "目标位于工作区沙箱内");
    }

    public static SandboxResult deny(String reason) {
        return new SandboxResult(false, reason);
    }
}
