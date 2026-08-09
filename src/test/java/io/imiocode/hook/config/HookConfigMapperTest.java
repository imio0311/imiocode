package io.imiocode.hook.config;

import io.imiocode.hook.HookEvent;
import io.imiocode.hook.action.CommandAction;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HookConfigMapperTest {
    @Test void mapsValidConfigurationInOrderAndExpandsEnvironment() {
        HookDocument first = new HookDocument("first", "turn_start", null, true, false,
                false, null, "ignore",
                new ActionDocument("command", "echo ${SUFFIX}", null, null, null,
                        null, null, null, 5));
        HookDocument second = new HookDocument("second", "pre_tool_use", "tool == 'bash'",
                false, false, true, "不允许命令", "reject",
                new ActionDocument("prompt", null, "请选择安全工具", null, null,
                        null, null, null, null));
        HookConfigLoadResult result = new HookConfigMapper().load(
                List.of(first, second), Map.of("SUFFIX", "ok"), ignored -> { });
        assertTrue(result.errors().isEmpty());
        assertEquals(List.of("first", "second"), result.hooks().stream().map(h -> h.id()).toList());
        assertEquals("echo ok", ((CommandAction) result.hooks().getFirst().action()).command());
        assertEquals(HookEvent.PRE_TOOL_USE, result.hooks().get(1).event());
    }

    @Test void anyErrorsDisableEntireSetAndAreAggregated() {
        HookDocument badEvent = new HookDocument("a", "magic", null, false, false,
                false, null, null, new ActionDocument("prompt", null, "x", null, null,
                null, null, null, null));
        HookDocument badAsync = new HookDocument("b", "pre_tool_use", null, false, true,
                true, null, null, new ActionDocument("prompt", null, "x", null, null,
                null, null, null, null));
        HookConfigLoadResult result = new HookConfigMapper().load(
                List.of(badEvent, badAsync), Map.of(), ignored -> { });
        assertTrue(result.hooks().isEmpty());
        assertEquals(2, result.errors().size());
        assertTrue(result.errors().getFirst().safeMessage().contains("hooks[0]"));
    }
}
