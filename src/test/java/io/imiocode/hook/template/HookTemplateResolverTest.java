package io.imiocode.hook.template;

import io.imiocode.hook.HookContext;
import io.imiocode.hook.HookEvent;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HookTemplateResolverTest {
    @Test void replacesAllVariablesOnce() {
        HookContext context = HookContext.builder(HookEvent.PRE_TOOL_USE, Path.of("."))
                .toolName("bash").filePath(Path.of("src/App.java"))
                .message("$ERROR").error("boom").toolArgs(Map.of("command", "mvn test"))
                .build();
        String result = new HookTemplateResolver().resolve(
                "$EVENT|$TOOL_NAME|$FILE_PATH|$MESSAGE|$ERROR|$TOOL_ARGS.command", context);
        assertEquals("pre_tool_use|bash|src\\App.java|$ERROR|boom|mvn test", result);
    }

    @Test void missingIsEmptyAndUnknownIsRejected() {
        HookTemplateResolver resolver = new HookTemplateResolver();
        HookContext context = HookContext.builder(HookEvent.STARTUP, Path.of(".")).build();
        assertEquals("x=", resolver.resolve("x=$TOOL_NAME", context));
        assertThrows(IllegalArgumentException.class, () -> resolver.validate("$UNKNOWN"));
    }
}
