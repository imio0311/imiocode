package io.imiocode.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionIdTest {
    @Test
    void generatesAndRejectsPathInput() {
        SessionId id = SessionId.generate();
        assertEquals(24, id.value().length());
        assertThrows(IllegalArgumentException.class, () -> new SessionId("../session"));
        assertThrows(IllegalArgumentException.class, () -> new SessionId("abc.jsonl"));
    }
}
