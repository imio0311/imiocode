package io.imiocode.team.backend;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@FunctionalInterface
public interface ProcessExecutor {
    ProcessResult run(Path cwd,List<String> command,Duration timeout);
}
