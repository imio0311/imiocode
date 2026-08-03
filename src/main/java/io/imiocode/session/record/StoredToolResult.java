package io.imiocode.session.record;

public record StoredToolResult(boolean success, String output, String error,
                               boolean truncated, long durationMillis, Integer exitCode) {
}
