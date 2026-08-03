package io.imiocode.command;

public record CommandMessage(boolean error, String text) {
    public static CommandMessage info(String text) { return new CommandMessage(false, text); }
    public static CommandMessage error(String text) { return new CommandMessage(true, text); }
}
