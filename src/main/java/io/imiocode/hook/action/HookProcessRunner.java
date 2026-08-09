package io.imiocode.hook.action;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public interface HookProcessRunner extends AutoCloseable {
    ProcessResult run(String command, Path workspace, Map<String, String> environment, Duration timeout);
    @Override void close();
}

record ProcessResult(boolean started, boolean timedOut, int exitCode, String stdout,
                     String stderr, boolean truncated, Duration elapsed) { }
