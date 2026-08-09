package io.imiocode.permission.command;

import io.imiocode.tool.ToolRisk;

import java.util.Objects;

/** 一次命令文本的确定性风险分类结果。 */
public record CommandRiskAssessment(ToolRisk risk, String reason) {
    public CommandRiskAssessment {
        risk = Objects.requireNonNull(risk, "risk 不能为空");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason 不能为空");
        }
        reason = reason.trim();
    }
}
