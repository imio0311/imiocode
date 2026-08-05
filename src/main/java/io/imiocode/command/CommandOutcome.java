package io.imiocode.command;

/** 命令执行后交还给交互循环的互斥结果。 */
public enum CommandOutcome {
    HANDLED,
    FORWARD_TO_AGENT,
    EXIT_REQUESTED
}
