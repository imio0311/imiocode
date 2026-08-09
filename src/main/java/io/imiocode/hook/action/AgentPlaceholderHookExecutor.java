package io.imiocode.hook.action;

import io.imiocode.hook.HookActionResult;
import io.imiocode.hook.HookContext;
import io.imiocode.hook.HookExecutionStatus;
import java.time.Duration;
import java.util.Optional;

/** CH12 仅保留配置契约，不启动真实子 Agent。 */
public final class AgentPlaceholderHookExecutor implements HookActionExecutor<AgentAction> {
    @Override public HookActionType type() { return HookActionType.AGENT; }
    @Override public HookActionResult execute(AgentAction action, HookContext context) {
        return new HookActionResult(HookExecutionStatus.NOT_IMPLEMENTED, "",
                Optional.of("agent Hook 将在 SubAgent 章节实现"), Duration.ZERO);
    }
}
