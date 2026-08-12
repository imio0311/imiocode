package io.imiocode.team.model;

import java.util.Objects;

/** 工具构造时绑定的调用者身份，避免模型伪造 team/sender。 */
public record TeamPrincipal(String teamName, String agentId, TeamRole role) {
    public TeamPrincipal {
        teamName = text(teamName, "teamName"); agentId = text(agentId, "agentId"); Objects.requireNonNull(role);
    }
    public boolean lead() { return role == TeamRole.LEAD; }
    private static String text(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " 不能为空"); return value.trim();
    }
}
