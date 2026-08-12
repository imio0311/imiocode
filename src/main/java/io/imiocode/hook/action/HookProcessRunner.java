package io.imiocode.hook.action;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * 在受限工作区与显式环境变量中运行 Hook 命令。
 *
 * <p>实现负责超时、输出截断和子进程清理，不能把宿主进程的全部环境变量隐式传给命令。</p>
 */
public interface HookProcessRunner extends AutoCloseable {
    ProcessResult run(String command, Path workspace, Map<String, String> environment, Duration timeout);
    @Override void close();
}

record ProcessResult(boolean started, boolean timedOut, int exitCode, String stdout,
                     String stderr, boolean truncated, Duration elapsed) { }
