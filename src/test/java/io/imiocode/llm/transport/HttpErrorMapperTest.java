package io.imiocode.llm.transport;

import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HttpErrorMapperTest {
    private final HttpErrorMapper mapper = new HttpErrorMapper();

    @Test
    void mapsKnownProviderContextSignalsToSafeType() {
        for (String signal : List.of(
                "context_length_exceeded",
                "invalid_request_error prompt is too long",
                "maximum context length is 64000 tokens",
                "input tokens exceed model context window")) {
            LlmException exception = mapper.fromStatus(400, signal + " provider-secret-canary");
            assertEquals(LlmErrorType.CONTEXT_LIMIT, exception.type());
            assertFalse(exception.safeMessage().contains("provider-secret-canary"));
        }
    }

    @Test
    void doesNotClassifyUnknownBadRequestAsContextLimit() {
        assertEquals(LlmErrorType.UNKNOWN,
                mapper.fromStatus(400, "invalid_request_error bad parameter").type());
    }
}
