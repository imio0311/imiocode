package io.imiocode.llm;

public enum LlmErrorType {
    AUTHENTICATION,
    RATE_LIMIT,
    MODEL_NOT_FOUND,
    SERVER_ERROR,
    NETWORK,
    TIMEOUT,
    OUTPUT_LIMIT,
    CONTEXT_LIMIT,
    PROTOCOL,
    INTERRUPTED,
    UNKNOWN
}
