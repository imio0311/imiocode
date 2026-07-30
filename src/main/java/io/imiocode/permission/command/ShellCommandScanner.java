package io.imiocode.permission.command;

import java.util.ArrayList;
import java.util.List;

/**
 * 引号感知的有限 Shell 扫描器。
 *
 * <p>只识别安全分类所需的命令边界；遇到副作用语法或不完整语法时保守退出。</p>
 */
public final class ShellCommandScanner {
    public ShellCommandScanResult scan(String command) {
        if (command == null || command.isBlank()) {
            return ShellCommandScanResult.ineligible("命令不能为空");
        }

        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Quote quote = Quote.NONE;

        for (int index = 0; index < command.length(); index++) {
            char character = command.charAt(index);

            if (quote == Quote.SINGLE) {
                current.append(character);
                if (character == '\'') {
                    quote = Quote.NONE;
                }
                continue;
            }

            if (quote == Quote.DOUBLE) {
                if (character == '\\' && index + 1 < command.length()
                        && command.charAt(index + 1) == '"') {
                    current.append(character).append(command.charAt(++index));
                    continue;
                }
                if (character == '"') {
                    quote = Quote.NONE;
                    current.append(character);
                    continue;
                }
                if (character == '`') {
                    return ShellCommandScanResult.ineligible("双引号内包含反引号命令替换");
                }
                if (character == '$' && hasNext(command, index, '(')) {
                    return ShellCommandScanResult.ineligible("双引号内包含命令替换");
                }
                current.append(character);
                continue;
            }

            if (character == '\'') {
                quote = Quote.SINGLE;
                current.append(character);
                continue;
            }
            if (character == '"') {
                quote = Quote.DOUBLE;
                current.append(character);
                continue;
            }
            if (character == '`') {
                return ShellCommandScanResult.ineligible("包含反引号命令替换");
            }
            if (character == '$' && hasNext(command, index, '(')) {
                return ShellCommandScanResult.ineligible("包含命令替换");
            }
            if (character == '>' || character == '<') {
                return ShellCommandScanResult.ineligible("包含输入或输出重定向");
            }
            if (character == '(' || character == ')'
                    || character == '{' || character == '}') {
                return ShellCommandScanResult.ineligible("包含 Shell 子进程或表达式语法");
            }
            if (character == '&') {
                if (!hasNext(command, index, '&')) {
                    return ShellCommandScanResult.ineligible("包含后台执行语法");
                }
                if (!appendSegment(segments, current)) {
                    return ShellCommandScanResult.ineligible("命令分隔符前存在空命令段");
                }
                index++;
                continue;
            }
            if (character == '|') {
                if (!appendSegment(segments, current)) {
                    return ShellCommandScanResult.ineligible("命令分隔符前存在空命令段");
                }
                if (hasNext(command, index, '|')) {
                    index++;
                }
                continue;
            }
            if (character == ';') {
                if (!appendSegment(segments, current)) {
                    return ShellCommandScanResult.ineligible("命令分隔符前存在空命令段");
                }
                continue;
            }
            if (character == '\r' || character == '\n') {
                if (character == '\r' && hasNext(command, index, '\n')) {
                    index++;
                }
                if (!appendSegment(segments, current)) {
                    return ShellCommandScanResult.ineligible("换行符前存在空命令段");
                }
                continue;
            }
            current.append(character);
        }

        if (quote != Quote.NONE) {
            return ShellCommandScanResult.ineligible("命令包含未闭合引号");
        }
        if (!appendSegment(segments, current)) {
            return ShellCommandScanResult.ineligible("命令末尾存在空命令段");
        }
        return ShellCommandScanResult.eligible(segments);
    }

    private static boolean hasNext(String command, int index, char expected) {
        return index + 1 < command.length() && command.charAt(index + 1) == expected;
    }

    private static boolean appendSegment(List<String> segments, StringBuilder current) {
        String segment = current.toString().trim();
        current.setLength(0);
        if (segment.isEmpty()) {
            return false;
        }
        segments.add(segment);
        return true;
    }

    private enum Quote {
        NONE,
        SINGLE,
        DOUBLE
    }
}
