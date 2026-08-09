package io.imiocode.subagent.model;

import io.imiocode.subagent.config.SubagentConfig;
import io.imiocode.subagent.definition.AgentDefinition;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** 将逻辑模型名解析为真实 provider 模型；未配置别名时安全回退父模型。 */
public final class ModelAliasResolver {
    private final SubagentConfig config;

    public ModelAliasResolver(SubagentConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    public ModelResolution resolve(AgentDefinition definition, String parentModel) {
        String requested = definition.model().orElse(parentModel).trim();
        String mapped = config.modelAliases().get(requested);
        if (mapped == null) mapped = config.modelAliases().get(requested.toLowerCase(Locale.ROOT));
        if (mapped != null && !mapped.isBlank()) return new ModelResolution(mapped, Optional.empty());
        if ("haiku".equalsIgnoreCase(requested) && !requested.equals(parentModel)) {
            return new ModelResolution(parentModel, Optional.of(
                    "子 Agent 模型别名 '" + requested + "' 未配置，已回退父模型 " + parentModel));
        }
        return new ModelResolution(requested, Optional.empty());
    }
}
