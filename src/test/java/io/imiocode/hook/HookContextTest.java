package io.imiocode.hook;

import io.imiocode.tool.SecretRedactor;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HookContextTest {
    @Test void resolvesNestedArgumentsAndRedactsPayload() {
        HookContext context = HookContext.builder(HookEvent.PRE_TOOL_USE, Path.of("."))
                .toolName("write_file")
                .toolArgs(Map.of("target", Map.of("path", ".env"), "token", "secret-value"))
                .build();
        assertEquals(".env", context.resolveField("args.target.path").orElseThrow());
        assertTrue(context.resolveField("args.missing").isEmpty());
        @SuppressWarnings("unchecked") Map<String, Object> args =
                (Map<String, Object>) context.safePayload(new SecretRedactor("secret-value")).get("tool_args");
        assertEquals("***", args.get("token"));
    }

    @Test void rejectionHasNoWritableStackTrace() {
        ToolRejectedError error = new ToolRejectedError("deny-env", "不要修改密钥");
        assertEquals("blocked by hook deny-env: 不要修改密钥", error.getMessage());
        assertEquals(0, error.getStackTrace().length);
    }
}
