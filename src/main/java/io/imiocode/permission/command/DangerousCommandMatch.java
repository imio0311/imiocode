package io.imiocode.permission.command;

/** 危险命令命中的稳定规则，不包含原始命令。 */
public record DangerousCommandMatch(String ruleId, String reason) {
    public DangerousCommandMatch {
        if (ruleId == null || ruleId.isBlank() || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("危险命令匹配信息不能为空");
        }
        ruleId = ruleId.trim();
        reason = reason.trim();
    }
}
