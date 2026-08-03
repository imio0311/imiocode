package io.imiocode.persistence;

@FunctionalInterface
public interface PersistenceEventListener {
    void onEvent(PersistenceEvent event);

    static PersistenceEventListener noop() { return ignored -> { }; }
}
