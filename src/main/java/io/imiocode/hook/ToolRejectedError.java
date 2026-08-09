package io.imiocode.hook;

/** 工具被 pre-tool Hook 拒绝；只暴露可安全回传模型的原因。 */
public final class ToolRejectedError extends RuntimeException {
    private final String hookId;
    private final String safeReason;

    public ToolRejectedError(String hookId, String safeReason) {
        super("blocked by hook " + require(hookId, "hookId") + ": " + require(safeReason, "safeReason"),
                null, false, false);
        this.hookId = hookId.trim();
        this.safeReason = safeReason.trim();
    }
    public String hookId() { return hookId; }
    public String safeReason() { return safeReason; }
    private static String require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " 不能为空");
        return value;
    }
}
