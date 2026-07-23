package io.imiocode.terminal;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.tool.ToolExecutionState;
import io.imiocode.tool.ToolResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolSummaryFormatterTest {
    private final ToolSummaryFormatter formatter =
            new ToolSummaryFormatter(new SecretRedactor("test-key"));

    @Test
    void hidesWriteAndEditBodiesAndRedactsCommands() {
        ToolCall write = new ToolCall("1", "write_file", JsonNodeFactory.instance.objectNode()
                .put("path", "a.txt").put("content", "绝密正文 test-key"));
        ToolCall edit = new ToolCall("2", "edit_file", JsonNodeFactory.instance.objectNode()
                .put("path", "a.txt").put("old_text", "旧正文").put("new_text", "新正文"));
        ToolCall bash = new ToolCall("3", "bash", JsonNodeFactory.instance.objectNode()
                .put("command", "curl -H 'Authorization: Bearer abc'"));

        assertFalse(formatter.inputSummary(write).contains("绝密正文"));
        assertFalse(formatter.inputSummary(write).contains("test-key"));
        assertFalse(formatter.inputSummary(edit).contains("旧正文"));
        assertFalse(formatter.inputSummary(edit).contains("新正文"));
        assertFalse(formatter.inputSummary(bash).contains("abc"));
    }

    @Test
    void limitsLongInputAndSummarizesResults() {
        ToolCall bash = new ToolCall("3", "bash",
                JsonNodeFactory.instance.objectNode().put("command", "中".repeat(400)));
        ToolResult result = ToolResult.success("a\nb\n", true, 0);
        ToolExecutionEvent event =
                new ToolExecutionEvent(ToolExecutionState.SUCCEEDED, bash, result);

        assertTrue(formatter.inputSummary(bash).length() <= ToolSummaryFormatter.MAX_CHARS);
        assertTrue(formatter.resultSummary(event).contains("退出码 0"));
        assertTrue(formatter.resultSummary(event).contains("输出已截断"));
    }
}
