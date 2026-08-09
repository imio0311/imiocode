package io.imiocode.hook;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class HookEventTest {
    @Test void exposesExactlyFifteenStableNames() {
        assertEquals(15, HookEvent.values().length);
        Set<String> names = Arrays.stream(HookEvent.values()).map(HookEvent::configName).collect(Collectors.toSet());
        assertEquals(15, names.size());
        for (HookEvent event : HookEvent.values()) assertSame(event, HookEvent.parse(event.configName()));
    }
    @Test void rejectsUnknownName() {
        assertThrows(IllegalArgumentException.class, () -> HookEvent.parse("before_magic"));
    }
}
