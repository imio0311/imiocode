package io.imiocode.mcp.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpToolNameTest {
    @Test
    void createsStableSanitizedName() {
        assertEquals(
                "mcp_github-main__issues_list",
                McpToolName.create("github-main", "issues/list"));
        assertEquals("mcp___server__tool_name", McpToolName.create("中文server", "tool name"));
    }

    @Test
    void rejectsBlankAndOverlongNames() {
        assertThrows(IllegalArgumentException.class, () -> McpToolName.create("", "tool"));
        assertThrows(IllegalArgumentException.class, () ->
                McpToolName.create("server", "x".repeat(60)));
    }
}
