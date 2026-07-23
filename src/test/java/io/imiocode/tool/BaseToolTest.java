package io.imiocode.tool;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseToolTest {
    @Test
    void convertsParameterAndRuntimeFailuresToSafeResults() {
        ProbeTool tool = new ProbeTool(limits(256), new SecretRedactor("secret-key"));

        ToolResult parameterFailure = tool.execute(JsonNodeFactory.instance.objectNode());
        ObjectNode runtime = JsonNodeFactory.instance.objectNode().put("mode", "throw");
        ToolResult runtimeFailure = tool.execute(runtime);

        assertFalse(parameterFailure.success());
        assertTrue(parameterFailure.error().contains("mode"));
        assertFalse(runtimeFailure.success());
        assertFalse(runtimeFailure.error().contains("secret-key"));
        assertTrue(runtimeFailure.duration().compareTo(Duration.ZERO) >= 0);
    }

    @Test
    void truncatesOnUtf8BoundaryAndMarksResult() {
        ProbeTool tool = new ProbeTool(limits(48), new SecretRedactor(""));
        ObjectNode arguments = JsonNodeFactory.instance.objectNode().put("mode", "long");

        ToolResult result = tool.execute(arguments);

        assertTrue(result.success());
        assertTrue(result.truncated());
        assertTrue(result.output().endsWith(BaseTool.TRUNCATION_MARKER));
        assertFalse(result.output().contains("\uFFFD"));
    }

    @Test
    void redactsKnownKeyFromToolOutput() {
        ProbeTool tool = new ProbeTool(limits(256), new SecretRedactor("secret-key"));
        ObjectNode arguments = JsonNodeFactory.instance.objectNode().put("mode", "secret");

        ToolResult result = tool.execute(arguments);

        assertFalse(result.output().contains("secret-key"));
        assertTrue(result.output().contains("***"));
    }

    private static ToolLimits limits(long resultBytes) {
        return new ToolLimits(
                1024, 100, 1024, 100, 100, 100, 100,
                resultBytes, 1024, 1024, Duration.ofSeconds(1), Duration.ofMillis(100));
    }

    private static final class ProbeTool extends BaseTool {
        private ProbeTool(ToolLimits limits, SecretRedactor redactor) {
            super(new ToolDefinition(
                    "probe",
                    "测试工具",
                    JsonNodeFactory.instance.objectNode().put("type", "object"),
                    ToolRisk.LOW), limits, redactor);
        }

        @Override
        protected ToolResult executeValidated(ObjectNode arguments) {
            String mode = requireText(arguments, "mode");
            return switch (mode) {
                case "throw" -> throw new IllegalStateException("secret-key raw failure");
                case "long" -> ToolResult.success("中文".repeat(100));
                case "secret" -> ToolResult.success("value=secret-key");
                default -> ToolResult.success("ok");
            };
        }
    }
}
