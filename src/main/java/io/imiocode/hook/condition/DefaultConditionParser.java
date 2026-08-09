package io.imiocode.hook.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** 解析不带括号、不可混合连接符的 Hook 条件。 */
public final class DefaultConditionParser implements ConditionParser {
    private static final List<String> OPERATORS = List.of("==", "!=", "=~", "~=");

    @Override
    public ConditionGroup parse(String expression) throws ConditionParseException {
        if (expression == null || expression.isBlank()) throw new ConditionParseException("条件不能为空");
        ScanResult scan = split(expression.trim());
        ConditionConnector connector = scan.connector == null ? ConditionConnector.AND : scan.connector;
        List<Condition> conditions = scan.parts.stream().map(this::parseCondition).toList();
        return new ConditionGroup(connector, conditions);
    }

    private Condition parseCondition(String text) {
        OperatorPosition found = null;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (ch == '\\') { escaped = true; continue; }
            if (quote != 0) { if (ch == quote) quote = 0; continue; }
            if (ch == '\'' || ch == '"') { quote = ch; continue; }
            if (i + 1 >= text.length()) continue;
            String pair = text.substring(i, i + 2);
            if (OPERATORS.contains(pair)) {
                if (found != null) throw new ConditionParseException("单个条件只能包含一个操作符: " + text);
                found = new OperatorPosition(i, pair);
                i++;
            }
        }
        if (quote != 0) throw new ConditionParseException("条件引号未闭合: " + text);
        if (found == null) throw new ConditionParseException("条件缺少操作符: " + text);
        String field = text.substring(0, found.index).trim();
        String expected = decode(text.substring(found.index + 2).trim());
        validateField(field);
        ConditionOperator operator = switch (found.operator) {
            case "==" -> ConditionOperator.EQUALS;
            case "!=" -> ConditionOperator.NOT_EQUALS;
            case "=~" -> ConditionOperator.REGEX;
            case "~=" -> ConditionOperator.GLOB;
            default -> throw new AssertionError(found.operator);
        };
        Optional<Pattern> regex = Optional.empty();
        Optional<HookGlobPattern> glob = Optional.empty();
        try {
            if (operator == ConditionOperator.REGEX) regex = Optional.of(Pattern.compile(expected));
            if (operator == ConditionOperator.GLOB) glob = Optional.of(HookGlobPattern.compile(expected));
        } catch (IllegalArgumentException exception) {
            throw new ConditionParseException("条件模式无效: " + exception.getMessage(), exception);
        }
        return new Condition(field, operator, expected, regex, glob);
    }

    private static ScanResult split(String expression) {
        List<String> parts = new ArrayList<>();
        ConditionConnector connector = null;
        int start = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (ch == '\\') { escaped = true; continue; }
            if (quote != 0) { if (ch == quote) quote = 0; continue; }
            if (ch == '\'' || ch == '"') { quote = ch; continue; }
            if (i + 1 >= expression.length()) continue;
            String pair = expression.substring(i, i + 2);
            ConditionConnector next = "&&".equals(pair) ? ConditionConnector.AND
                    : "||".equals(pair) ? ConditionConnector.OR : null;
            if (next == null) continue;
            if (connector != null && connector != next)
                throw new ConditionParseException("同一条件表达式不能混用 && 和 ||");
            connector = next;
            String part = expression.substring(start, i).trim();
            if (part.isEmpty()) throw new ConditionParseException("连接符两侧必须有条件");
            parts.add(part);
            start = i + 2;
            i++;
        }
        if (quote != 0) throw new ConditionParseException("条件引号未闭合");
        String last = expression.substring(start).trim();
        if (last.isEmpty()) throw new ConditionParseException("连接符两侧必须有条件");
        parts.add(last);
        return new ScanResult(parts, connector);
    }

    private static void validateField(String field) {
        boolean valid = switch (field) {
            case "event", "tool", "tool_name", "file_path", "message", "error" -> true;
            default -> field.matches("(?:args|data)\\.[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*");
        };
        if (!valid) throw new ConditionParseException("未知条件字段: " + field);
    }

    private static String decode(String value) {
        if (value.isEmpty()) throw new ConditionParseException("条件比较值不能为空");
        if (value.charAt(0) != '\'' && value.charAt(0) != '"') return value;
        char quote = value.charAt(0);
        if (value.length() < 2 || value.charAt(value.length() - 1) != quote)
            throw new ConditionParseException("条件比较值引号未闭合");
        String raw = value.substring(1, value.length() - 1);
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\\' && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                if (next == quote || next == '\\') {
                    output.append(next);
                    i++;
                    continue;
                }
            }
            output.append(ch);
        }
        return output.toString();
    }

    private record ScanResult(List<String> parts, ConditionConnector connector) { }
    private record OperatorPosition(int index, String operator) { }
}
