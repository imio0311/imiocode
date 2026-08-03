package io.imiocode.memory;

public final class MemoryException extends RuntimeException {
    public MemoryException(String message) { super(message); }
    public MemoryException(String message, Throwable cause) { super(message, cause); }
}
