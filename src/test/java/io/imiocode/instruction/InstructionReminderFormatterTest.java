package io.imiocode.instruction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstructionReminderFormatterTest {
    @TempDir Path root;

    @Test
    void formatsLowToHighPriorityStably() {
        var lower = new InstructionSource(root.resolve("MEWCODE.md"), InstructionScope.PROJECT, 10, "根规则");
        var higher = new InstructionSource(root.resolve("sub/MEWCODE.md"), InstructionScope.PROJECT, 11, "近端规则");
        var snapshot = new InstructionSnapshot(List.of(higher, lower), List.of(), Set.of(), 12);

        var formatter = new InstructionReminderFormatter();
        var first = formatter.format(snapshot, root, root);
        var second = formatter.format(snapshot, root, root);

        assertEquals(first, second);
        assertTrue(first.get(0).content().contains("根规则"));
        assertTrue(first.get(1).content().contains("近端规则"));
    }
}
