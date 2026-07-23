package io.imiocode.llm.transport;

public record SseEvent(String event, String data) {
    public SseEvent {
        event = event == null ? "" : event;
        data = data == null ? "" : data;
    }
}
