package io.imiocode.subagent.runtime;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.agent.Agent;
import io.imiocode.config.AgentConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.permission.PermissionMode;
import io.imiocode.subagent.context.SubagentContextBuilder;
import io.imiocode.subagent.config.SubagentConfig;
import io.imiocode.subagent.definition.AgentDefinition;
import io.imiocode.subagent.definition.AgentDefinitionSource;
import io.imiocode.subagent.definition.AgentIsolation;
import io.imiocode.subagent.filter.SubagentToolFilter;
import io.imiocode.subagent.trace.TraceRegistry;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.worktree.config.WorktreeConfig;
import io.imiocode.worktree.git.GitCommandResult;
import io.imiocode.worktree.git.GitCommandRunner;
import io.imiocode.worktree.lifecycle.WorktreeManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubagentWorktreeIsolationTest {
    @TempDir Path temp;

    @Test
    void parallelAgentsWriteSameRelativePathWithoutTouchingParentWorkspace() throws Exception {
        Path repository = temp.resolve("repo");
        Files.createDirectories(repository);
        GitCommandRunner git = new GitCommandRunner(Duration.ofSeconds(10));
        runGit(git, repository, "init");
        runGit(git, repository, "config", "user.email", "test@example.invalid");
        runGit(git, repository, "config", "user.name", "Test");
        Files.writeString(repository.resolve("tracked.txt"), "root\n");
        Files.writeString(repository.resolve(".gitignore"), ".imiocode/\n");
        runGit(git, repository, "add", ".");
        runGit(git, repository, "commit", "-m", "initial");

        ToolRegistry parentTools = workspaceToolNames();
        ToolLimits limits = ToolLimits.defaults();
        SecretRedactor redactor = new SecretRedactor("");
        SubagentToolRegistryFactory scopedTools = new SubagentToolRegistryFactory(parentTools, limits, redactor);
        CopyOnWriteArrayList<Path> workdirs = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<CapturingClient> clients = new CopyOnWriteArrayList<>();
        AtomicInteger sequence = new AtomicInteger();
        CyclicBarrier barrier = new CyclicBarrier(2);

        try (WorktreeManager manager = new WorktreeManager(repository, WorktreeConfig.defaults())) {
            SubagentAgentFactory factory = new SubagentAgentFactory() {
                @Override
                public SubagentAgentHandle create(AgentDefinition definition,
                                                   io.imiocode.tool.ToolSelection selection) {
                    throw new AssertionError("worktree isolation 必须传入独立工作目录");
                }

                @Override
                public SubagentAgentHandle create(AgentDefinition definition,
                                                   io.imiocode.tool.ToolSelection selection,
                                                   Path workdir) {
                    int id = sequence.incrementAndGet();
                    workdirs.add(workdir);
                    CapturingClient client = new CapturingClient("agent-" + id, barrier);
                    clients.add(client);
                    Agent agent = new Agent(client, scopedTools.create(workdir, null),
                            new AgentConfig(3, Duration.ofSeconds(15), 1));
                    return new SubagentAgentHandle(agent, "test-model", Optional.empty());
                }
            };
            DefaultSubagentRunner runner = new DefaultSubagentRunner(
                    parentTools, new SubagentToolFilter(SubagentConfig.defaults()),
                    new SubagentContextBuilder(), factory,
                    new TraceRegistry(), null, manager);
            AgentDefinition definition = definition();

            try (var executor = Executors.newFixedThreadPool(2)) {
                Future<SubagentRunResult> first = executor.submit(() -> runner.run(
                        definition, "写入第一个结果", List.of(), false,
                        SubagentRunMode.DEFINITION, ignored -> { }));
                Future<SubagentRunResult> second = executor.submit(() -> runner.run(
                        definition, "写入第二个结果", List.of(), false,
                        SubagentRunMode.DEFINITION, ignored -> { }));

                assertTrue(first.get().success());
                assertTrue(second.get().success());
            }

            assertEquals(2, workdirs.size());
            assertNotEquals(workdirs.get(0), workdirs.get(1));
            assertFalse(Files.exists(repository.resolve("conflict.txt")));
            Set<String> contents = Set.of(
                    Files.readString(workdirs.get(0).resolve("conflict.txt")),
                    Files.readString(workdirs.get(1).resolve("conflict.txt")));
            assertEquals(Set.of("agent-1", "agent-2"), contents);
            assertEquals(2, manager.list().size(), "dirty agent worktree 必须保留供用户处理");

            for (int index = 0; index < clients.size(); index++) {
                String workdir = workdirs.get(index).toString();
                assertTrue(clients.get(index).requests.getFirst().reminders().stream()
                        .anyMatch(reminder -> reminder.content().contains(workdir)),
                        "隔离通知必须包含真实 worktree 路径");
            }

            manager.list().forEach(item -> assertTrue(manager.remove(item.slug(), true).removed()));
        }
    }

    private static AgentDefinition definition() {
        return new AgentDefinition("writer", "写文件", "在隔离目录写文件", Optional.empty(),
                PermissionMode.AUTO_EDIT, 3, Duration.ofSeconds(15), Set.of("write_file"), Set.of(),
                false, Optional.empty(), List.of(), List.of(), List.of(), false,
                AgentIsolation.WORKTREE, AgentDefinitionSource.BUILTIN, Optional.empty());
    }

    private static ToolRegistry workspaceToolNames() {
        ToolRegistry registry = new ToolRegistry();
        for (String name : List.of("read_file", "write_file", "edit_file", "bash", "glob", "grep")) {
            registry.register(new Tool() {
                @Override public ToolDefinition definition() {
                    return new ToolDefinition(name, "占位工具",
                            JsonNodeFactory.instance.objectNode().put("type", "object"), ToolRisk.LOW);
                }
                @Override public ToolResult execute(com.fasterxml.jackson.databind.node.ObjectNode arguments) {
                    return ToolResult.success("unused");
                }
            });
        }
        return registry;
    }

    private static void runGit(GitCommandRunner git, Path cwd, String... args) {
        GitCommandResult result = git.run(cwd, List.of(args));
        assertEquals(0, result.exitCode(), result.output());
    }

    private static final class CapturingClient implements LlmClient {
        private final String content;
        private final CyclicBarrier barrier;
        private final List<ChatRequest> requests = new ArrayList<>();

        private CapturingClient(String content, CyclicBarrier barrier) {
            this.content = content;
            this.barrier = barrier;
        }

        @Override
        public synchronized ChatResponse streamChat(ChatRequest request, LlmEventListener listener) {
            requests.add(request);
            ChatResponse response;
            if (requests.size() == 1) {
                try { barrier.await(); }
                catch (Exception exception) { throw new IllegalStateException(exception); }
                var arguments = JsonNodeFactory.instance.objectNode();
                arguments.put("path", "conflict.txt");
                arguments.put("content", content);
                ToolCall call = new ToolCall("write-1", "write_file", arguments);
                response = new ChatResponse(new ChatMessage(MessageRole.ASSISTANT, List.of(new ToolCallPart(call))));
                listener.onEvent(new LlmEvent.ToolCallStarted(0, call.id(), call.name()));
                listener.onEvent(new LlmEvent.ToolCallCompleted(0, call));
            } else {
                response = new ChatResponse("完成");
                listener.onEvent(new LlmEvent.TextDelta(response.text()));
            }
            listener.onEvent(new LlmEvent.StreamCompleted(response.usage()));
            return response;
        }

        @Override public void close() { }
    }
}
