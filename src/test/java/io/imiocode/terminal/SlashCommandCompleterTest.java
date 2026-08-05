package io.imiocode.terminal;

import org.jline.reader.Candidate;
import org.jline.reader.impl.DefaultParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlashCommandCompleterTest {
    private final SlashCommandCompleter completer = new SlashCommandCompleter(prefix -> switch (prefix) {
        case "perm" -> List.of("/permission");
        case "c" -> List.of("/clear", "/cls", "/compact", "/compact-ui");
        default -> List.of();
    });

    @Test
    void completesSingleAndMultipleCommandNames() {
        assertEquals(List.of("/permission"), complete("/perm"));
        assertEquals(List.of("/clear", "/cls", "/compact", "/compact-ui"), complete("/c"));
    }

    @Test
    void ignoresNormalTextAndArguments() {
        assertTrue(complete("perm").isEmpty());
        assertTrue(complete("/session r").isEmpty());
        assertTrue(complete("say /perm").isEmpty());
    }

    private List<String> complete(String buffer) {
        var parsed = new DefaultParser().parse(buffer, buffer.length());
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsed, candidates);
        return candidates.stream().map(Candidate::value).toList();
    }
}
