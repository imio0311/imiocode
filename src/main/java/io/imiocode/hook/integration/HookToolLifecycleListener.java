package io.imiocode.hook.integration;

import io.imiocode.hook.HookEvent;
import io.imiocode.hook.HookRuntime;
import io.imiocode.tool.ToolLifecycleListener;

import java.nio.file.Path;

/** 将核心工具的中性事件适配为 Hook 事件，不让后置 Hook 覆盖工具结果。 */
public final class HookToolLifecycleListener implements ToolLifecycleListener {
    private final HookRuntime hooks;
    private final HookContextFactory contexts;

    public HookToolLifecycleListener(HookRuntime hooks, HookContextFactory contexts) {
        this.hooks = hooks;
        this.contexts = contexts;
    }

    @Override public void onFileChanged(String toolName, Path path) {
        try {
            hooks.runHooks(contexts.builder(HookEvent.FILE_CHANGE)
                    .toolName(toolName).filePath(path).build());
        } catch (RuntimeException ignored) {
            // 文件已经成功写入，Hook 失败只能通过通知报告。
        }
    }

    @Override public void onCommandStarted(String command) {
        try {
            hooks.runHooks(contexts.builder(HookEvent.COMMAND_EXECUTE)
                    .toolName("bash").toolArgs(java.util.Map.of("command", command))
                    .message(command).build());
        } catch (RuntimeException ignored) {
            // 进程已经启动，Hook 失败不能伪装成 Bash 启动失败。
        }
    }
}
