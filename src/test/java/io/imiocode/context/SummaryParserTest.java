package io.imiocode.context;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SummaryParserTest {
    private final SummaryParser parser = new SummaryParser();

    @Test
    void parsesStrictSummaryAndDecodesEntities() throws Exception {
        ParsedSummary parsed = parser.parse("<summary><prior_history>A &amp; B</prior_history><active_task>继续</active_task></summary>");
        assertEquals("A & B", parsed.priorHistory());
        assertEquals("继续", parsed.activeTask());
    }

    @Test
    void rejectsMalformedOrAmbiguousOutputs() {
        for (String raw : List.of(
                "```xml\n<summary><prior_history>A</prior_history><active_task>B</active_task></summary>\n```",
                "outside<summary><prior_history>A</prior_history><active_task>B</active_task></summary>",
                "<summary><prior_history></prior_history><active_task>B</active_task></summary>",
                "<summary><prior_history>A</prior_history><prior_history>B</prior_history><active_task>C</active_task></summary>",
                "<summary><active_task>B</active_task><prior_history>A</prior_history></summary>")) {
            assertThrows(ContextException.class, () -> parser.parse(raw));
        }
    }
}
