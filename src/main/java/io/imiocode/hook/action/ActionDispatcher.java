package io.imiocode.hook.action;

import io.imiocode.hook.HookActionResult;
import io.imiocode.hook.HookContext;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 将动作严格分派给唯一执行器。 */
public final class ActionDispatcher implements AutoCloseable {
    private final Map<HookActionType, HookActionExecutor<?>> executors = new EnumMap<>(HookActionType.class);

    public ActionDispatcher(List<HookActionExecutor<?>> values) {
        for (HookActionExecutor<?> executor : Objects.requireNonNull(values, "values")) {
            if (executors.putIfAbsent(executor.type(), executor) != null)
                throw new IllegalArgumentException("重复 Hook executor: " + executor.type());
        }
        for (HookActionType type : HookActionType.values())
            if (!executors.containsKey(type)) throw new IllegalArgumentException("缺少 Hook executor: " + type);
    }

    public HookActionResult execute(Action action, HookContext context) {
        return executeUnchecked(executors.get(action.type()), action, context);
    }

    @SuppressWarnings("unchecked")
    private static <A extends Action> HookActionResult executeUnchecked(
            HookActionExecutor<?> raw, Action action, HookContext context) {
        return ((HookActionExecutor<A>) raw).execute((A) action, context);
    }

    @Override public void close() {
        executors.values().stream().distinct().forEach(executor -> {
            if (executor instanceof AutoCloseable closeable) {
                try { closeable.close(); } catch (Exception ignored) { }
            }
        });
    }
}
