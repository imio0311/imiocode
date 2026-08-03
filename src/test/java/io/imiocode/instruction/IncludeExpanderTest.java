package io.imiocode.instruction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncludeExpanderTest {
    @TempDir Path root;

    @Test
    void expandsQuotedNestedIncludes() throws Exception {
        Files.createDirectories(root.resolve("docs"));
        Files.writeString(root.resolve("MEWCODE.md"), "before\n@include \"docs/team rules.md\"\nafter\n");
        Files.writeString(root.resolve("docs/team rules.md"), "中文规则\n");

        var result = new IncludeExpander().expand(root.resolve("MEWCODE.md"), root, 8, 131072);

        assertEquals("before\n中文规则\nafter", result.content());
        assertEquals(2, result.dependencies().size());
    }

    @Test
    void rejectsEscapeAndCycle() throws Exception {
        Files.writeString(root.resolve("MEWCODE.md"), "@include ../outside.md\n");
        assertThrows(IOException.class,
                () -> new IncludeExpander().expand(root.resolve("MEWCODE.md"), root, 8, 131072));

        Files.writeString(root.resolve("MEWCODE.md"), "@include loop.md\n");
        Files.writeString(root.resolve("loop.md"), "@include MEWCODE.md\n");
        IOException cycle = assertThrows(IOException.class,
                () -> new IncludeExpander().expand(root.resolve("MEWCODE.md"), root, 8, 131072));
        assertTrue(cycle.getMessage().contains("循环"));
    }

    @Test
    void enforcesExpandedByteBudget() throws Exception {
        Files.writeString(root.resolve("MEWCODE.md"), "中文中文中文\n");
        assertThrows(IOException.class,
                () -> new IncludeExpander().expand(root.resolve("MEWCODE.md"), root, 8, 4));
    }
}
