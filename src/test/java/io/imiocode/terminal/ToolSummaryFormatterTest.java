package io.imiocode.terminal;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.tool.ToolExecutionState;
import io.imiocode.tool.ToolResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void formatsCompactBuiltInToolOutcomes() {
        ToolExecutionEvent read = completed(
                "read_file",
                JsonNodeFactory.instance.objectNode().put("path", "pom.xml"),
                ToolResult.success("content").withDuration(Duration.ofMillis(120)));
        ToolExecutionEvent glob = completed(
                "glob",
                JsonNodeFactory.instance.objectNode().put("pattern", "*.java"),
                ToolResult.success("A.java\nB.java\n").withDuration(Duration.ofMillis(5)));

        assertEquals("Read pom.xml (0.1s)", formatter.compactSummary(read));
        assertEquals("Glob *.java · 2 results (0.0s)", formatter.compactSummary(glob));
    }

    @Test
    void compactSummaryHidesBodiesOutputsRiskAndUnknownArguments() {
        ToolCall writeCall = new ToolCall("secret-call-id", "write_file",
                JsonNodeFactory.instance.objectNode()
                        .put("path", "a.txt")
                        .put("content", "正文 test-key"));
        ToolExecutionEvent write = new ToolExecutionEvent(
                ToolExecutionState.SUCCEEDED,
                writeCall,
                ToolResult.success("完整工具输出 test-key")
                        .withDuration(Duration.ofSeconds(1)));
        ToolExecutionEvent mcp = completed(
                "mcp_context7__query_docs",
                JsonNodeFactory.instance.objectNode().put("token", "test-key"),
                ToolResult.success("secret output").withDuration(Duration.ZERO));

        String writeSummary = formatter.compactSummary(write);
        assertTrue(writeSummary.contains("Write a.txt"));
        assertFalse(writeSummary.contains("正文"));
        assertFalse(writeSummary.contains("完整工具输出"));
        assertFalse(writeSummary.contains("LOW"));
        assertFalse(writeSummary.contains("secret-call-id"));
        String mcpSummary = formatter.compactSummary(mcp);
        assertTrue(mcpSummary.contains("mcp context7 query docs"));
        assertFalse(mcpSummary.contains("test-key"));
        assertFalse(mcpSummary.contains("secret output"));
    }

    @Test
    void compactFailureUsesOnlyRedactedFirstErrorLine() {
        ToolExecutionEvent failed = new ToolExecutionEvent(
                ToolExecutionState.FAILED,
                new ToolCall("1", "bash",
                        JsonNodeFactory.instance.objectNode().put("command", "run test-key")),
                ToolResult.failure("第一行 test-key\n不应显示第二行")
                        .withDuration(Duration.ofMillis(1250)));

        String summary = formatter.compactSummary(failed);

        assertTrue(summary.contains("Bash run ***"));
        assertTrue(summary.contains("第一行 ***"));
        assertTrue(summary.contains("1.3s"));
        assertFalse(summary.contains("第二行"));
    }

    @Test
    void compactSummaryRejectsNonTerminalEventsAndFormatsDurations() {
        ToolCall call = new ToolCall("1", "read_file",
                JsonNodeFactory.instance.objectNode().put("path", "a"));
        assertThrows(IllegalArgumentException.class, () -> formatter.compactSummary(
                new ToolExecutionEvent(ToolExecutionState.RUNNING, call, null)));
        assertEquals("0.0s", ToolSummaryFormatter.formatDuration(Duration.ZERO));
        assertEquals("0.5s", ToolSummaryFormatter.formatDuration(Duration.ofMillis(500)));
        assertEquals("2.0s", ToolSummaryFormatter.formatDuration(Duration.ofSeconds(2)));
    }

    private static ToolExecutionEvent completed(
            String name,
            com.fasterxml.jackson.databind.node.ObjectNode arguments,
            ToolResult result) {
        ToolCall call = new ToolCall("1", name, arguments);
        return new ToolExecutionEvent(
                result.success() ? ToolExecutionState.SUCCEEDED : ToolExecutionState.FAILED,
                call,
                result);
    }
}
