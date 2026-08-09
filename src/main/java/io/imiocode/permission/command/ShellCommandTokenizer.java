package io.imiocode.permission.command;

import java.util.ArrayList;
import java.util.List;

/** 引号感知的有限 Shell 分词器，供只读检测和风险分类共享。 */
public final class ShellCommandTokenizer {
    public ShellTokenizeResult tokenize(String segment) {
        if (segment == null || segment.isBlank()) {
            return ShellTokenizeResult.invalid("命令段不能为空");
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean tokenStarted = false;

        for (int index = 0; index < segment.length(); index++) {
            char character = segment.charAt(index);
            if (quote == '\'') {
                if (character == '\'') quote = 0;
                else current.append(character);
                tokenStarted = true;
                continue;
            }
            if (quote == '"') {
                if (character == '\\' && index + 1 < segment.length()
                        && segment.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else if (character == '"') {
                    quote = 0;
                } else {
                    current.append(character);
                }
                tokenStarted = true;
                continue;
            }
            if (character == '\'' || character == '"') {
                quote = character;
                tokenStarted = true;
                continue;
            }
            if (Character.isWhitespace(character)) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
                continue;
            }
            if (character == '\\' && index + 1 < segment.length()
                    && Character.isWhitespace(segment.charAt(index + 1))) {
                current.append(segment.charAt(++index));
                tokenStarted = true;
                continue;
            }
            current.append(character);
            tokenStarted = true;
        }
        if (quote != 0) return ShellTokenizeResult.invalid("命令包含未闭合引号");
        if (tokenStarted) tokens.add(current.toString());
        return tokens.isEmpty()
                ? ShellTokenizeResult.invalid("命令段不能为空")
                : ShellTokenizeResult.valid(tokens);
    }
}
