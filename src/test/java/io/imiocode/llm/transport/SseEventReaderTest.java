package io.imiocode.llm.transport;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SseEventReaderTest {
    private final SseEventReader reader = new SseEventReader();

    @Test
    void readsEventsCommentsMultilineDataAndUtf8() throws Exception {
        String input = ": keepalive\n"
                + "event: first\n"
                + "data: 你好\n"
                + "data: code();\n\n"
                + "unknown: ignored\n"
                + "data: tail\n";
        List<SseEvent> events = new ArrayList<>();

        reader.read(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), events::add);

        assertEquals(List.of(
                new SseEvent("first", "你好\ncode();"),
                new SseEvent("", "tail")), events);
    }

    @Test
    void propagatesIoFailure() {
        InputStream broken = new InputStream() {
            private int reads;

            @Override
            public int read() throws IOException {
                if (reads++ < 4) {
                    return 'a';
                }
                throw new IOException("broken stream");
            }
        };

        assertThrows(IOException.class, () -> reader.read(broken, event -> { }));
    }
}
