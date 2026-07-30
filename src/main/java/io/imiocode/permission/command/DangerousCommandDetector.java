package io.imiocode.permission.command;

import java.nio.file.Path;
import java.util.Optional;

/** 检测不可通过配置或用户确认放行的命令。 */
public interface DangerousCommandDetector {
    Optional<DangerousCommandMatch> inspect(String command, Path workspace);
}
