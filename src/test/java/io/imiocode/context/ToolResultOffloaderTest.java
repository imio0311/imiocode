package io.imiocode.context;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolResultOffloaderTest {
    @TempDir Path workspace;

    @Test
    void offloadsSingleLargeResultAndIsIdempotent() throws Exception {
        ToolResultOffloader offloader = offloader();
        ChatMessage message = toolMessage("one", "x".repeat(5_001));

        OffloadResult first = offloader.offload(List.of(message));
        OffloadResult second = offloader.offload(first.messages());

        ToolResultPart result = (ToolResultPart) first.messages().getFirst().parts().getFirst();
        assertTrue(result.result().output().contains(".imiocode/tool-results/"));
        assertTrue(result.result().output().contains("read_file"));
        assertEquals(1, first.spilledCount());
        assertEquals(0, second.spilledCount());
        assertEquals(1, Files.list(workspace.resolve(".imiocode/tool-results")).count());
    }

    @Test
    void exactSingleThresholdStaysInline() {
        ChatMessage original = toolMessage("one", "x".repeat(5_000));
        OffloadResult result = offloader().offload(List.of(original));
        assertSame(original, result.messages().getFirst());
        assertEquals(0, result.spilledCount());
    }

    @Test
    void aggregatePolicyKeepsNewestThreeToolMessages() {
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; i < 7; i++) messages.add(toolMessage("call-" + i, "x".repeat(4_000)));
        OffloadResult result = offloader().offload(messages);
        assertTrue(result.spilledCount() > 0);
        for (int i = 4; i < 7; i++) {
            ToolResultPart part = (ToolResultPart) result.messages().get(i).parts().getFirst();
            assertEquals(4_000, part.result().output().length());
        }
    }

    @Test
    void writeFailurePreservesOriginalResult() throws Exception {
        Files.writeString(workspace.resolve(".imiocode"), "blocked");
        ChatMessage original = toolMessage("one", "x".repeat(6_000));
        OffloadResult result = offloader().offload(List.of(original));
        assertSame(original, result.messages().getFirst());
    }

    private ToolResultOffloader offloader() {
        SecretRedactor redactor = new SecretRedactor("");
        return new ToolResultOffloader(new ToolResultSpillStore(workspace, redactor), redactor);
    }

    private static ChatMessage toolMessage(String id, String output) {
        return new ChatMessage(MessageRole.TOOL,
                List.of(new ToolResultPart(id, "bash", ToolResult.success(output))));
    }
}
