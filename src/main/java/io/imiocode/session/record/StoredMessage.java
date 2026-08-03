package io.imiocode.session.record;

import java.util.List;

public record StoredMessage(String role, List<StoredPart> parts) {
    public StoredMessage {
        parts = List.copyOf(parts == null ? List.of() : parts);
    }
}
