package io.imiocode.subagent.runtime;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.permission.PermissionOperation;
import io.imiocode.permission.PermissionRequestFactory;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolLimits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentToolContractTest {
    @Test void exposesOneStableSchemaAndPermissionLayerRecognizesIt() {
        var tool=new AgentTool(nullDispatcher(),ToolLimits.defaults(),new SecretRedactor(""));
        assertEquals("agent",tool.definition().name());
        assertTrue(tool.definition().inputSchema().path("properties").has("subagent_type"));
        assertTrue(tool.definition().inputSchema().path("properties").has("prompt"));
        assertTrue(tool.definition().inputSchema().path("properties").has("run_in_background"));
        var args= JsonNodeFactory.instance.objectNode(); args.put("subagent_type","explore"); args.put("description","inspect");
        var request=new PermissionRequestFactory(new SecretRedactor(""))
                .create(new ToolCall("1","agent",args),tool);
        assertEquals(PermissionOperation.READ,request.operation());
        assertTrue(new io.imiocode.permission.sandbox.WorkspacePathSandbox(
                new io.imiocode.tool.workspace.WorkspacePolicy(java.nio.file.Path.of("").toAbsolutePath()))
                .inspect(request).allowed());
        args.put("prompt","inspect"); args.put("cwd","../outside");
        var blocked=tool.execute(args);
        assertFalse(blocked.success());
        assertTrue(blocked.error().contains("cwd"));
    }

    private static SubagentDispatcher nullDispatcher() {
        return new SubagentDispatcher(
                new io.imiocode.subagent.definition.AgentDefinitionLoader(
                        java.nio.file.Path.of("target/no-project"),java.nio.file.Path.of("target/no-user")),
                (definition,task,history,background,mode,cancellation)->new SubagentRunResult(true,"ok",
                        io.imiocode.agent.AgentStopReason.FINAL_RESPONSE,
                        io.imiocode.subagent.trace.TraceTokenUsage.zero(),"trace"),
                new io.imiocode.subagent.task.TaskManager(
                        (definition,task,history,background,mode,cancellation)->new SubagentRunResult(true,"ok",
                                io.imiocode.agent.AgentStopReason.FINAL_RESPONSE,
                                io.imiocode.subagent.trace.TraceTokenUsage.zero(),"trace"),1,8,8),
                java.util.List::of);
    }
}
