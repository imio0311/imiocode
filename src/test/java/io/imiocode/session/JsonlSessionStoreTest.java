package io.imiocode.session;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.MessageRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonlSessionStoreTest {
    @TempDir Path project;
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-03T10:15:30Z"), ZoneOffset.UTC);

    @Test
    void createsAppendsReplacesAndLoads() {
        JsonlSessionStore store = new JsonlSessionStore(project, clock);
        SessionSnapshot empty = store.create();
        List<ChatMessage> firstHistory = List.of(
                new ChatMessage(MessageRole.USER, "你好"),
                new ChatMessage(MessageRole.ASSISTANT, "你好，有什么可以帮你？"));
        SessionSnapshot first = store.appendCommit(empty, firstHistory);
        List<ChatMessage> replacedHistory = List.of(
                new ChatMessage(MessageRole.USER, "[Compacted conversation summary]"),
                new ChatMessage(MessageRole.ASSISTANT, "继续"));
        SessionSnapshot second = store.appendCommit(first, replacedHistory);

        SessionLoadResult loaded = store.load(empty.metadata().id());

        assertEquals(SessionRecoveryStatus.CLEAN, loaded.status());
        assertEquals(replacedHistory, loaded.snapshot().history());
        assertEquals(2, loaded.snapshot().metadata().commitCount());
        assertEquals(1, store.list().size());
    }

    @Test
    void recoversCorruptTailAndRejectsCorruptFirstTransaction() throws Exception {
        JsonlSessionStore store = new JsonlSessionStore(project, clock);
        SessionSnapshot empty = store.create();
        SessionSnapshot saved = store.appendCommit(empty, List.of(
                new ChatMessage(MessageRole.USER, "hello"),
                new ChatMessage(MessageRole.ASSISTANT, "world")));
        Path file = project.resolve(".imiocode/sessions/" + saved.metadata().id() + ".jsonl");
        Files.writeString(file, "{broken", java.nio.file.StandardOpenOption.APPEND);

        SessionLoadResult recovered = store.load(saved.metadata().id());
        assertEquals(SessionRecoveryStatus.TAIL_RECOVERED, recovered.status());
        assertTrue(Files.exists(recovered.quarantinedTail().orElseThrow()));
        assertEquals(saved.history(), recovered.snapshot().history());

        List<String> lines = new ArrayList<>(Files.readAllLines(file));
        lines.set(2, "{broken-middle");
        Files.write(file, lines);
        assertThrows(SessionException.class, () -> store.load(saved.metadata().id()));
        assertFalse(Files.readString(file).isBlank());
    }
}
