package io.imiocode.tool;

import java.nio.file.Path;

/** 核心工具真实副作用发生后的中性生命周期监听器。 */
public interface ToolLifecycleListener {
    void onFileChanged(String toolName, Path path);
    void onCommandStarted(String command);

    ToolLifecycleListener NOOP = new ToolLifecycleListener() {
        @Override public void onFileChanged(String toolName, Path path) { }
        @Override public void onCommandStarted(String command) { }
    };
}
