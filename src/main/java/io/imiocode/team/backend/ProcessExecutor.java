package io.imiocode.team.backend;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** 以参数数组执行后端探测或控制命令，避免将成员标识拼接成 Shell 代码。 */
@FunctionalInterface
public interface ProcessExecutor {
    ProcessResult run(Path cwd,List<String> command,Duration timeout);
}
