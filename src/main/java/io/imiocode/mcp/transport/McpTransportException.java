package io.imiocode.mcp.transport;

/** 已经过安全裁剪、可向上层传播的传输异常。 */
public final class McpTransportException extends RuntimeException {
    public McpTransportException(String message) {
        super(message);
    }

    public McpTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
