package io.imiocode.subagent.definition;

import io.imiocode.permission.PermissionMode;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** 已完整校验、可直接执行的 Agent 定义。 */
public record AgentDefinition(
        String name,
        String description,
        String prompt,
        Optional<String> model,
        PermissionMode permissionMode,
        int maxTurns,
        Duration timeout,
        Set<String> tools,
        Set<String> disallowedTools,
        boolean backgroundAllowed,
        Optional<String> initialPrompt,
        List<String> skills,
        List<String> mcpServers,
        List<String> hooks,
        boolean memory,
        AgentIsolation isolation,
        AgentDefinitionSource source,
        Optional<Path> sourcePath) {
    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9-]{0,63}");
    private static final Pattern CAPABILITY = Pattern.compile("[A-Za-z0-9_.:-]{1,128}");

    public AgentDefinition {
        name = require(name, "Agent 名称").toLowerCase(java.util.Locale.ROOT);
        if (!NAME.matcher(name).matches()) throw new AgentDefinitionException("Agent 名称格式无效: " + name);
        description = require(description, "Agent 描述");
        prompt = require(prompt, "Agent Markdown 正文");
        model = clean(model);
        permissionMode = Objects.requireNonNullElse(permissionMode, PermissionMode.AUTO_EDIT);
        if (maxTurns <= 0 || maxTurns > 200) throw new AgentDefinitionException("maxTurns 必须在 1..200 之间");
        timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative() || timeout.compareTo(Duration.ofHours(1)) > 0)
            throw new AgentDefinitionException("timeout 必须在 1 秒到 1 小时之间");
        tools = capabilities(tools, "tools");
        disallowedTools = capabilities(disallowedTools, "disallowedTools");
        Set<String> overlap = new LinkedHashSet<>(tools);
        overlap.retainAll(disallowedTools);
        if (!overlap.isEmpty()) throw new AgentDefinitionException("tools 与 disallowedTools 冲突: " + overlap);
        initialPrompt = clean(initialPrompt);
        skills = capabilityList(skills, "skills");
        mcpServers = capabilityList(mcpServers, "mcpServers");
        hooks = capabilityList(hooks, "hooks");
        isolation = Objects.requireNonNullElse(isolation, AgentIsolation.NONE);
        source = Objects.requireNonNull(source, "source");
        sourcePath = (sourcePath == null ? Optional.<Path>empty() : sourcePath)
                .map(path -> path.toAbsolutePath().normalize());
    }

    public boolean unrestrictedTools() { return tools.isEmpty(); }

    public AgentDefinition withModel(String override) {
        return new AgentDefinition(name, description, prompt, Optional.ofNullable(override), permissionMode,
                maxTurns, timeout, tools, disallowedTools, backgroundAllowed, initialPrompt,
                skills, mcpServers, hooks, memory, isolation, source, sourcePath);
    }

    public AgentDefinition withAdditionalDeniedTools(Set<String> additional) {
        Set<String> extra = capabilities(additional, "disallowedTools");
        LinkedHashSet<String> nextAllowed = new LinkedHashSet<>(tools);
        nextAllowed.removeAll(extra);
        LinkedHashSet<String> nextDenied = new LinkedHashSet<>(disallowedTools);
        nextDenied.addAll(extra);
        return new AgentDefinition(name, description, prompt, model, permissionMode, maxTurns, timeout,
                nextAllowed, nextDenied, backgroundAllowed, initialPrompt, skills, mcpServers, hooks,
                memory, isolation, source, sourcePath);
    }

    public AgentDefinition withIsolation(AgentIsolation nextIsolation) {
        return new AgentDefinition(name, description, prompt, model, permissionMode, maxTurns, timeout,
                tools, disallowedTools, backgroundAllowed, initialPrompt, skills, mcpServers, hooks,
                memory, Objects.requireNonNull(nextIsolation), source, sourcePath);
    }

    private static String require(String value, String label) {
        if (value == null || value.isBlank()) throw new AgentDefinitionException(label + "不能为空");
        return value.trim();
    }
    private static Optional<String> clean(Optional<String> value) {
        Optional<String> checked = value == null ? Optional.empty() : value;
        return checked.map(String::trim).filter(v -> !v.isEmpty());
    }
    private static Set<String> capabilities(Set<String> values, String field) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : Objects.requireNonNullElse(values, Set.<String>of())) {
            String normalized = require(value, field + " 项");
            if (!CAPABILITY.matcher(normalized).matches())
                throw new AgentDefinitionException(field + " 名称格式无效: " + normalized);
            result.add(normalized);
        }
        return Set.copyOf(result);
    }
    private static List<String> capabilityList(List<String> values, String field) {
        return List.copyOf(capabilities(new LinkedHashSet<>(Objects.requireNonNullElse(values, List.of())), field));
    }
}
