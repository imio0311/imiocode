package io.imiocode.hook;

import io.imiocode.conversation.SystemReminder;
import java.util.List;

/** Hook 系统对 Agent、工具与 UI 暴露的唯一运行时入口。 */
public interface HookRuntime extends AutoCloseable {
    HookRunResult runHooks(HookContext context);
    PreToolHookResult runPreToolHooks(HookContext context);
    List<SystemReminder> drainPrompts();
    List<HookNotification> drainNotifications();
    void clearPrompts();
    @Override default void close() { }

    HookRuntime NOOP = new HookRuntime() {
        @Override public HookRunResult runHooks(HookContext context) {
            if (context.event() == HookEvent.PRE_TOOL_USE)
                throw new IllegalArgumentException("pre_tool_use 必须调用 runPreToolHooks");
            return HookRunResult.EMPTY;
        }
        @Override public PreToolHookResult runPreToolHooks(HookContext context) {
            if (context.event() != HookEvent.PRE_TOOL_USE)
                throw new IllegalArgumentException("runPreToolHooks 只接受 pre_tool_use");
            return PreToolHookResult.ALLOW;
        }
        @Override public List<SystemReminder> drainPrompts() { return List.of(); }
        @Override public List<HookNotification> drainNotifications() { return List.of(); }
        @Override public void clearPrompts() { }
    };
}
