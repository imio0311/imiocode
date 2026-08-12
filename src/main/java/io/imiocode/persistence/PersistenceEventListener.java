package io.imiocode.persistence;

/** 接收指令或记忆持久上下文的加载诊断事件。 */
@FunctionalInterface
public interface PersistenceEventListener {
    void onEvent(PersistenceEvent event);

    static PersistenceEventListener noop() { return ignored -> { }; }
}
