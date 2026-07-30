package io.imiocode.permission;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** 管理 HITL 等待、回复和仅在当前会话生效的精确授权。 */
public final class PermissionCoordinator implements AutoCloseable {
    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentHashMap<String, PendingRequest> pending = new ConcurrentHashMap<>();
    private final Set<SessionGrant> sessionGrants = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    public PermissionDecision confirm(
            PermissionRequest request,
            PermissionDecision askDecision,
            int iteration,
            Consumer<PermissionPrompt> publisher,
            BiConsumer<String, PermissionReply> resolutionPublisher
    ) {
        Objects.requireNonNull(request, "request 不能为空");
        Objects.requireNonNull(askDecision, "askDecision 不能为空");
        Objects.requireNonNull(publisher, "publisher 不能为空");
        BiConsumer<String, PermissionReply> checkedResolution =
                Objects.requireNonNullElse(resolutionPublisher, (ignoredId, ignoredReply) -> { });
        if (askDecision.action() != PermissionAction.ASK) {
            return askDecision;
        }
        SessionGrant key = SessionGrant.from(request);
        if (sessionGrants.contains(key)) {
            return PermissionDecision.allow(
                    PermissionDecisionSource.SESSION, "本次会话已允许相同操作");
        }
        if (closed.get()) {
            return PermissionDecision.deny(
                    PermissionDecisionSource.USER, "权限确认已关闭");
        }

        String requestId = "permission-" + sequence.incrementAndGet();
        CompletableFuture<PermissionReply> future = new CompletableFuture<>();
        PendingRequest waiting = new PendingRequest(future, checkedResolution);
        pending.put(requestId, waiting);
        PermissionPrompt prompt = new PermissionPrompt(
                requestId,
                iteration,
                request.call().name(),
                request.risk(),
                request.displayTarget(),
                askDecision.reason());
        try {
            publisher.accept(prompt);
        } catch (RuntimeException exception) {
            pending.remove(requestId, waiting);
            return PermissionDecision.deny(
                    PermissionDecisionSource.USER, "无法获取用户确认，已拒绝");
        }

        PermissionReply reply = future.join();
        pending.remove(requestId, waiting);
        return switch (reply) {
            case ALLOW_ONCE -> PermissionDecision.allow(
                    PermissionDecisionSource.USER, "用户允许本次操作");
            case ALLOW_SESSION -> {
                sessionGrants.add(key);
                yield PermissionDecision.allow(
                        PermissionDecisionSource.SESSION, "用户允许本次会话中的相同操作");
            }
            case DENY -> PermissionDecision.deny(
                    PermissionDecisionSource.USER, "用户拒绝了该操作");
        };
    }

    public boolean resolve(String requestId, PermissionReply reply) {
        if (requestId == null || requestId.isBlank() || reply == null) {
            return false;
        }
        PendingRequest waiting = pending.get(requestId);
        if (waiting == null || !waiting.future().complete(reply)) {
            return false;
        }
        try {
            waiting.resolutionPublisher().accept(requestId, reply);
        } catch (RuntimeException ignored) {
            // UI 结果事件失败不能改变已经确定的授权结果。
        }
        return true;
    }

    public void cancelAll() {
        for (PendingRequest waiting : pending.values()) {
            waiting.future().complete(PermissionReply.DENY);
        }
        pending.clear();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            cancelAll();
            sessionGrants.clear();
        }
    }

    private record PendingRequest(
            CompletableFuture<PermissionReply> future,
            BiConsumer<String, PermissionReply> resolutionPublisher
    ) {
    }

    private record SessionGrant(String toolName, String normalizedTarget) {
        private static SessionGrant from(PermissionRequest request) {
            return new SessionGrant(request.call().name(), request.normalizedTarget());
        }
    }
}
