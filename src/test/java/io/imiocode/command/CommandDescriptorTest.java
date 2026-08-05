package io.imiocode.command;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandDescriptorTest {
    @Test
    void normalizesNamesAndKeepsMetadata() {
        CommandDescriptor descriptor = new CommandDescriptor(
                "/Help", Set.of("H", "?"), "/help", "显示帮助", CommandType.LOCAL);

        assertEquals("help", descriptor.name());
        assertEquals(Set.of("h", "?"), descriptor.aliases());
        assertEquals(CommandType.LOCAL, descriptor.type());
    }

    @Test
    void rejectsInvalidOrDuplicateNames() {
        assertThrows(IllegalArgumentException.class, () -> new CommandDescriptor(
                "", Set.of(), "/x", "x", CommandType.LOCAL));
        assertThrows(IllegalArgumentException.class, () -> new CommandDescriptor(
                "bad name", Set.of(), "/x", "x", CommandType.LOCAL));
        assertThrows(IllegalArgumentException.class, () -> new CommandDescriptor(
                "help", Set.of("H", "h"), "/help", "x", CommandType.LOCAL));
        assertThrows(IllegalArgumentException.class, () -> new CommandDescriptor(
                "help", Set.of("help"), "/help", "x", CommandType.LOCAL));
    }
}
