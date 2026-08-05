package io.imiocode.terminal;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.Objects;

/** 只补全输入首段的命令主名和别名，不解析参数。 */
public final class SlashCommandCompleter implements Completer {
    private final SlashCompletionSource source;

    public SlashCommandCompleter(SlashCompletionSource source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        Objects.requireNonNull(line, "line");
        Objects.requireNonNull(candidates, "candidates");
        String buffer = line.line();
        int cursor = line.cursor();
        if (cursor != buffer.length() || cursor < 1 || buffer.charAt(0) != '/') return;
        String fragment = buffer.substring(0, cursor);
        if (fragment.chars().anyMatch(Character::isWhitespace)) return;
        source.complete(fragment.substring(1)).stream()
                .map(value -> new Candidate(value, value, null, null, null, null, true))
                .forEach(candidates::add);
    }
}
