package io.imiocode.permission;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionCoordinatorTest {
    @Test
    void supportsOnceSessionAndUnknownReplies() {
        PermissionCoordinator coordinator = new PermissionCoordinator();
        PermissionRequest request = PermissionCheckerTest.command("mvn test");
        PermissionDecision ask = PermissionDecision.ask(
                PermissionDecisionSource.MODE, "需要确认");
        AtomicInteger prompts = new AtomicInteger();

        PermissionDecision first = coordinator.confirm(
                request,
                ask,
                1,
                prompt -> {
                    prompts.incrementAndGet();
                    assertTrue(coordinator.resolve(
                            prompt.requestId(), PermissionReply.ALLOW_SESSION));
                },
                (id, reply) -> { });
        PermissionDecision second = coordinator.confirm(
                request,
                ask,
                2,
                prompt -> prompts.incrementAndGet(),
                (id, reply) -> { });

        assertEquals(PermissionAction.ALLOW, first.action());
        assertEquals(PermissionDecisionSource.SESSION, second.source());
        assertEquals(1, prompts.get());
        assertFalse(coordinator.resolve("missing", PermissionReply.ALLOW_ONCE));
        coordinator.close();
    }

    @Test
    void closeRejectsFutureConfirmation() {
        PermissionCoordinator coordinator = new PermissionCoordinator();
        coordinator.close();
        PermissionDecision decision = coordinator.confirm(
                PermissionCheckerTest.command("mvn test"),
                PermissionDecision.ask(PermissionDecisionSource.MODE, "需要确认"),
                1,
                ignored -> { },
                (id, reply) -> { });
        assertEquals(PermissionAction.DENY, decision.action());
    }
}
