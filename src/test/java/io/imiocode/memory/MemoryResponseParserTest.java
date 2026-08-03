package io.imiocode.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemoryResponseParserTest {
    private final MemoryResponseParser parser = new MemoryResponseParser();

    @Test
    void parsesStrictEnvelope() {
        var result = parser.parse("<memories>{\"items\":[{\"scope\":\"user\",\"category\":\"preference\",\"content\":\"使用中文\",\"replaces_id\":null}]}</memories>");
        assertEquals(1, result.size());
        assertEquals(MemoryScope.USER, result.getFirst().scope());
    }

    @Test
    void rejectsUnknownFieldsAndPlainText() {
        assertThrows(MemoryException.class, () -> parser.parse("nothing"));
        assertThrows(MemoryException.class, () -> parser.parse(
                "<memories>{\"items\":[{\"scope\":\"user\",\"category\":\"preference\",\"content\":\"x\",\"extra\":1}]}</memories>"));
    }
}
