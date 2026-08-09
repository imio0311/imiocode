package io.imiocode.hook.condition;

import io.imiocode.hook.HookContext;
import io.imiocode.hook.HookEvent;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ConditionParserTest {
    private final DefaultConditionParser parser = new DefaultConditionParser();
    private final DefaultConditionEvaluator evaluator = new DefaultConditionEvaluator();

    @Test void supportsAllOperatorsAndAndComposition() {
        HookContext context = HookContext.builder(HookEvent.PRE_TOOL_USE, Path.of("."))
                .toolName("write_file").toolArgs(Map.of("path", "src/main/App.java"))
                .message("hello hook").build();
        assertTrue(evaluator.matches(parser.parse("tool == 'write_file' && args.path ~= '**/*.java'"), context));
        assertTrue(evaluator.matches(parser.parse("message =~ 'hook$' && tool != 'bash'"), context));
    }

    @Test void supportsQuotedConnectorAndRegexBackslash() {
        HookContext context = HookContext.builder(HookEvent.TURN_START, Path.of("."))
                .message("a && b.java").build();
        assertTrue(evaluator.matches(parser.parse("message =~ 'a && b\\.java$'"), context));
    }

    @Test void rejectsMixedConnectorsAndBadFields() {
        assertThrows(ConditionParseException.class,
                () -> parser.parse("tool == bash && message == x || error == y"));
        assertThrows(ConditionParseException.class, () -> parser.parse("unknown == x"));
        assertThrows(ConditionParseException.class, () -> parser.parse("message =~ '['"));
    }

    @Test void doubleStarMatchesRootAndNestedFiles() {
        HookGlobPattern pattern = HookGlobPattern.compile("**/*.java");
        assertTrue(pattern.matches("App.java"));
        assertTrue(pattern.matches("src\\main\\App.java"));
        assertFalse(pattern.matches("App.kt"));
    }
}
