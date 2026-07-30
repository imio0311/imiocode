package io.imiocode.permission;

import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** 工具调度器唯一使用的权限入口。 */
public final class PermissionGate implements AutoCloseable {
    private final PermissionRequestFactory requestFactory;
    private final PermissionChecker checker;
    private final PermissionCoordinator coordinator;

    public PermissionGate(
            PermissionRequestFactory requestFactory,
            PermissionChecker checker,
            PermissionCoordinator coordinator
    ) {
        this.requestFactory = Objects.requireNonNull(requestFactory, "requestFactory 不能为空");
        this.checker = Objects.requireNonNull(checker, "checker 不能为空");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator 不能为空");
    }

    public PermissionEvaluation evaluate(ToolCall call, ToolDefinition definition) {
        try {
            PermissionRequest request = requestFactory.create(call, definition);
            return new PermissionEvaluation(request, checker.check(request));
        } catch (RuntimeException exception) {
            PermissionRequest fallback = new PermissionRequest(
                    call,
                    definition.risk(),
                    operationFor(call.name()),
                    "<invalid>",
                    "<invalid>");
            return new PermissionEvaluation(
                    fallback,
                    PermissionDecision.deny(
                            PermissionDecisionSource.ERROR,
                            "工具参数无法完成权限验证，已安全拒绝"));
        }
    }

    public PermissionDecision confirm(
            PermissionEvaluation evaluation,
            int iteration,
            Consumer<PermissionPrompt> publisher,
            BiConsumer<String, PermissionReply> resolutionPublisher
    ) {
        Objects.requireNonNull(evaluation, "evaluation 不能为空");
        if (evaluation.decision().action() != PermissionAction.ASK) {
            return evaluation.decision();
        }
        return coordinator.confirm(
                evaluation.request(),
                evaluation.decision(),
                iteration,
                publisher,
                resolutionPublisher);
    }

    public boolean resolve(String requestId, PermissionReply reply) {
        return coordinator.resolve(requestId, reply);
    }

    public void cancelPending() {
        coordinator.cancelAll();
    }

    @Override
    public void close() {
        coordinator.close();
    }

    private static PermissionOperation operationFor(String toolName) {
        return switch (toolName) {
            case "read_file", "glob", "grep" -> PermissionOperation.READ;
            case "write_file", "edit_file" -> PermissionOperation.WRITE;
            default -> PermissionOperation.COMMAND;
        };
    }
}
