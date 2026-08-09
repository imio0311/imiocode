package io.imiocode.command;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 命令结果；本地处理、转交 Agent 和退出三者严格互斥。 */
public record CommandResult(
        CommandOutcome outcome,
        List<CommandMessage> messages,
        Optional<String> prompt
) {
    public CommandResult {
        outcome = Objects.requireNonNull(outcome, "命令结果不能为空");
        messages = List.copyOf(Objects.requireNonNullElse(messages, List.of()));
        prompt = (prompt == null ? Optional.<String>empty() : prompt)
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        if (outcome == CommandOutcome.FORWARD_TO_AGENT) {
            if (prompt.isEmpty()) throw new IllegalArgumentException("转交 Agent 的 Prompt 不能为空");
            if (!messages.isEmpty()) throw new IllegalArgumentException("转交 Agent 时不能同时返回本地消息");
        } else if (prompt.isPresent()) {
            throw new IllegalArgumentException("本地或退出结果不能携带 Prompt");
        }
        if (outcome == CommandOutcome.EXIT_REQUESTED && !messages.isEmpty()) {
            throw new IllegalArgumentException("退出结果不能携带本地消息");
        }
    }

    public static CommandResult handled(CommandMessage... messages) {
        return new CommandResult(CommandOutcome.HANDLED, List.of(messages), Optional.empty());
    }

    public static CommandResult handled(List<CommandMessage> messages) {
        return new CommandResult(CommandOutcome.HANDLED, messages, Optional.empty());
    }

    public static CommandResult forwardToAgent(String prompt) {
        return new CommandResult(CommandOutcome.FORWARD_TO_AGENT, List.of(), Optional.ofNullable(prompt));
    }

    public static CommandResult exit() {
        return new CommandResult(CommandOutcome.EXIT_REQUESTED, List.of(), Optional.empty());
    }

    public static CommandResult restart(CommandMessage... messages) {
        return new CommandResult(CommandOutcome.RESTART_REQUESTED, List.of(messages), Optional.empty());
    }
}
