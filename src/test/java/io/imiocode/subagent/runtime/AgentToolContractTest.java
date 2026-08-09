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
        assertEquals(java.util.List.of("none", "worktree"),
                java.util.stream.StreamSupport.stream(
                                tool.definition().inputSchema().path("properties").path("isolation")
                                        .path("enum").spliterator(), false)
                        .map(com.fasterxml.jackson.databind.JsonNode::asText).toList());
        assertTrue(tool.definition().inputSchema().path("properties").path("isolation")
                .path("description").asText().contains("继承 Agent 定义"));
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

    @Test void omittedIsolationInheritsDefinitionAndWorktreeRejectsCwd() {
        java.util.concurrent.atomic.AtomicReference<io.imiocode.subagent.definition.AgentIsolation> captured =
                new java.util.concurrent.atomic.AtomicReference<>();
        SubagentRunner runner = (definition, task, history, background, mode, cancellation) -> {
            captured.set(definition.isolation());
            return new SubagentRunResult(true, "ok", io.imiocode.agent.AgentStopReason.FINAL_RESPONSE,
                    io.imiocode.subagent.trace.TraceTokenUsage.zero(), "trace");
        };
        try (var tasks = new io.imiocode.subagent.task.TaskManager(runner, 1, 8, 8)) {
            var dispatcher = new SubagentDispatcher(
                    new io.imiocode.subagent.definition.AgentDefinitionLoader(
                            java.nio.file.Path.of("target/no-project"), java.nio.file.Path.of("target/no-user")),
                    runner, tasks, java.util.List::of, java.nio.file.Path.of("").toAbsolutePath());
            var tool = new AgentTool(dispatcher, ToolLimits.defaults(), new SecretRedactor(""));
            var args = JsonNodeFactory.instance.objectNode();
            args.put("subagent_type", "explore");
            args.put("description", "inspect");
            args.put("prompt", "inspect README");

            assertTrue(tool.execute(args).success());
            assertEquals(io.imiocode.subagent.definition.AgentIsolation.WORKTREE, captured.get());

            args.put("isolation", "worktree");
            args.put("cwd", "src");
            var blocked = tool.execute(args);
            assertFalse(blocked.success());
            assertTrue(blocked.error().contains("cwd"));
        }
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
