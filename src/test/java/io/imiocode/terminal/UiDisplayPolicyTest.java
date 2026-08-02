package io.imiocode.terminal;

import io.imiocode.config.UiVerbosity;
import io.imiocode.context.ContextEvent;
import io.imiocode.context.ContextManageMode;
import io.imiocode.mcp.manager.McpEventType;
import io.imiocode.tool.ToolExecutionState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiDisplayPolicyTest {
    @Test
    void compactHidesProcessNoiseAndKeepsOutcomes() {
        UiDisplayPolicy policy = new UiDisplayPolicy(UiVerbosity.COMPACT);

        assertFalse(policy.showStateTransitions());
        assertFalse(policy.showThinking());
        assertFalse(policy.showUsage());
        assertFalse(policy.showToolEvent(ToolExecutionState.QUEUED));
        assertFalse(policy.showToolEvent(ToolExecutionState.RUNNING));
        assertTrue(policy.showToolEvent(ToolExecutionState.SUCCEEDED));
        assertTrue(policy.showToolEvent(ToolExecutionState.FAILED));

        for (McpEventType type : McpEventType.values()) {
            boolean expected = type == McpEventType.DENIED || type == McpEventType.SERVER_FAILED;
            if (expected) {
                assertTrue(policy.showMcpEvent(type));
            } else {
                assertFalse(policy.showMcpEvent(type));
            }
        }

        assertFalse(policy.showContextEvent(
                new ContextEvent.Started(ContextManageMode.AUTO, 100)));
        assertTrue(policy.showContextEvent(
                new ContextEvent.Completed(ContextManageMode.AUTO, 100, 40)));
        assertTrue(policy.showContextEvent(
                new ContextEvent.Failed(ContextManageMode.AUTO, "压缩失败")));
    }

    @Test
    void verboseShowsEveryExistingProcessEvent() {
        UiDisplayPolicy policy = new UiDisplayPolicy(UiVerbosity.VERBOSE);

        assertTrue(policy.showStateTransitions());
        assertTrue(policy.showThinking());
        assertTrue(policy.showUsage());
        for (ToolExecutionState state : ToolExecutionState.values()) {
            assertTrue(policy.showToolEvent(state));
        }
        for (McpEventType type : McpEventType.values()) {
            assertTrue(policy.showMcpEvent(type));
        }
        assertTrue(policy.showContextEvent(
                new ContextEvent.Started(ContextManageMode.AUTO, 100)));
    }
}
