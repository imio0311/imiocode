package io.imiocode.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class CommandParser {
    public Optional<ParsedCommand> parse(String input) {
        if (input == null || !input.stripLeading().startsWith("/")) return Optional.empty();
        String value = input.strip();
        List<String> tokens = tokenize(value.substring(1));
        if (tokens.isEmpty() || tokens.getFirst().isBlank()) throw new IllegalArgumentException("命令名不能为空");
        return Optional.of(new ParsedCommand(tokens.getFirst().toLowerCase(Locale.ROOT), tokens.subList(1, tokens.size())));
    }

    private static List<String> tokenize(String value) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0; boolean escaping = false; boolean started = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaping) { current.append(ch); escaping = false; started = true; continue; }
            if (ch == '\\') {
                if (i + 1 < value.length() && isEscapable(value.charAt(i + 1))) {
                    escaping = true;
                } else {
                    current.append(ch);
                }
                started = true;
                continue;
            }
            if (quote != 0) {
                if (ch == quote) quote = 0; else current.append(ch);
                started = true; continue;
            }
            if (ch == '\'' || ch == '"') { quote = ch; started = true; continue; }
            if (Character.isWhitespace(ch)) {
                if (started) { tokens.add(current.toString()); current.setLength(0); started = false; }
            } else { current.append(ch); started = true; }
        }
        if (escaping) current.append('\\');
        if (quote != 0) throw new IllegalArgumentException("命令参数引号未闭合");
        if (started) tokens.add(current.toString());
        return List.copyOf(tokens);
    }

    private static boolean isEscapable(char value) {
        return value == '\\' || value == '\'' || value == '"' || Character.isWhitespace(value);
    }
}
