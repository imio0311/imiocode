package io.imiocode.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.tool.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolCallAssemblerTest {
    @Test
    void restoresInterleavedUnicodeCallsByPosition() throws Exception {
        ToolCallAssembler assembler = new ToolCallAssembler(new ObjectMapper());
        assembler.append(1, "id-", "gr", "{\"pattern\":\"中");
        assembler.append(0, "call-", "read_", "{\"path\":\"");
        assembler.append(1, "2", "ep", "文\"}");
        assembler.append(0, "1", "file", "文件.txt\"}");

        List<ToolCall> calls = assembler.finish();

        assertEquals(List.of("call-1", "id-2"), calls.stream().map(ToolCall::id).toList());
        assertEquals("文件.txt", calls.get(0).arguments().path("path").asText());
        assertEquals("中文", calls.get(1).arguments().path("pattern").asText());
    }

    @Test
    void rejectsMissingInvalidAndNonObjectArguments() {
        ToolCallAssembler missing = new ToolCallAssembler(new ObjectMapper());
        missing.append(0, "id", "tool", null);
        assertThrows(LlmException.class, missing::finish);

        ToolCallAssembler invalid = new ToolCallAssembler(new ObjectMapper());
        invalid.append(0, "id", "tool", "{");
        assertThrows(LlmException.class, invalid::finish);

        ToolCallAssembler array = new ToolCallAssembler(new ObjectMapper());
        array.append(0, "id", "tool", "[]");
        assertThrows(LlmException.class, array::finish);
    }
}
