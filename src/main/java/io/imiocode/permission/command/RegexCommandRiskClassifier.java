package io.imiocode.permission.command;

import io.imiocode.tool.ToolRisk;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** 基于共享 Shell 解析器的保守命令风险分类器。 */
public final class RegexCommandRiskClassifier implements CommandRiskClassifier {
    private static final Set<String> DELETE_COMMANDS = Set.of(
            "rm", "del", "erase", "rmdir", "rd", "remove-item", "clear-content");
    private static final Set<String> OVERWRITE_COMMANDS = Set.of(
            "set-content", "out-file", "tee-object");
    private static final Set<String> SYSTEM_COMMANDS = Set.of(
            "chmod", "chown", "icacls", "takeown", "set-acl", "set-executionpolicy",
            "reg", "sc", "systemctl", "service", "schtasks", "new-scheduledtask",
            "enable-windowsoptionalfeature", "disable-windowsoptionalfeature");
    private static final Set<String> PACKAGE_MANAGERS = Set.of(
            "apt", "apt-get", "dnf", "yum", "pacman", "brew", "choco", "winget", "scoop");
    private static final Set<String> DYNAMIC_EXECUTION = Set.of(
            "eval", "invoke-expression", "iex", "start-process");
    private static final Set<String> DEVELOPMENT_COMMANDS = Set.of(
            "mvn", "mvnw", "gradle", "gradlew", "java", "javac", "go", "cargo",
            "rustc", "dotnet", "pytest", "ruff", "black", "eslint", "prettier");
    private static final Set<String> BENIGN_LOCAL_COMMANDS = Set.of(
            "echo", "printf", "write-output", "sleep", "start-sleep");
    private static final Set<String> FILE_ORGANIZATION_COMMANDS = Set.of(
            "mkdir", "md", "new-item", "touch", "cp", "copy", "copy-item",
            "mv", "move", "move-item", "rename-item");
    private static final Pattern WINDOWS_ABSOLUTE = Pattern.compile("^[a-zA-Z]:[/\\\\].*");

    private final SafeCommandDetector safeDetector;
    private final ShellCommandScanner scanner;
    private final ShellCommandTokenizer tokenizer;

    public RegexCommandRiskClassifier(
            SafeCommandDetector safeDetector,
            ShellCommandScanner scanner,
            ShellCommandTokenizer tokenizer) {
        this.safeDetector = Objects.requireNonNull(safeDetector, "safeDetector");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer");
    }

    @Override
    public CommandRiskAssessment classify(String command) {
        try {
            SafeCommandResult safe = safeDetector.inspect(command);
            if (safe.safe()) {
                return new CommandRiskAssessment(ToolRisk.LOW, "命令仅包含严格只读操作");
            }
            ShellCommandScanResult scan = scanner.scan(command);
            if (!scan.eligible()) {
                return high("命令包含无法安全解析或可能产生副作用的 Shell 语法");
            }
            ToolRisk aggregate = ToolRisk.LOW;
            String reason = "命令仅包含严格只读操作";
            for (String segment : scan.segments()) {
                CommandRiskAssessment current = classifySegment(segment);
                if (current.risk().ordinal() > aggregate.ordinal()) {
                    aggregate = current.risk();
                    reason = current.reason();
                }
            }
            return new CommandRiskAssessment(aggregate, reason);
        } catch (RuntimeException exception) {
            return high("命令风险无法可靠判断");
        }
    }

    private CommandRiskAssessment classifySegment(String segment) {
        SafeCommandResult safe = safeDetector.inspect(segment);
        if (safe.safe()) return new CommandRiskAssessment(ToolRisk.LOW, "命令段属于严格只读操作");
        ShellTokenizeResult tokenized = tokenizer.tokenize(segment);
        if (!tokenized.valid()) return high("命令段无法可靠解析");
        List<String> tokens = tokenized.tokens();
        String executable = executable(tokens.getFirst());
        List<String> arguments = tokens.subList(1, tokens.size());

        if (DELETE_COMMANDS.contains(executable)) return high("命令可能删除数据");
        if (OVERWRITE_COMMANDS.contains(executable) || hasForceOverwrite(executable, arguments)) {
            return high("命令可能覆盖已有数据");
        }
        if (SYSTEM_COMMANDS.contains(executable)) return high("命令可能修改权限或系统配置");
        if (DYNAMIC_EXECUTION.contains(executable)) return high("命令会动态执行其他内容");
        if (PACKAGE_MANAGERS.contains(executable)) return high("命令会修改系统级软件包");
        if ("git".equals(executable)) return classifyGit(arguments);
        if (Set.of("node", "python", "python3").contains(executable)) {
            return classifyInterpreter(executable, arguments);
        }
        if (Set.of("npm", "pnpm", "yarn").contains(executable)) {
            return classifyJavaScriptPackageManager(executable, arguments);
        }
        if (isPackageHighRisk(executable, arguments)) return high("命令会发布内容或执行全局安装");
        if ("docker".equals(executable)) return classifyDocker(arguments);
        if (BENIGN_LOCAL_COMMANDS.contains(executable)) {
            return medium("命令属于普通本地操作");
        }
        if (DEVELOPMENT_COMMANDS.contains(executable)) {
            return medium("命令属于普通本地构建、测试或开发操作");
        }
        if (FILE_ORGANIZATION_COMMANDS.contains(executable)) {
            if (arguments.stream().anyMatch(RegexCommandRiskClassifier::unsafePathToken)) {
                return high("文件操作目标无法证明位于工作区内");
            }
            return medium("命令属于普通工作区文件组织操作");
        }
        return high("命令程序不在可信本地开发清单中");
    }

    private static CommandRiskAssessment classifyGit(List<String> arguments) {
        if (arguments.isEmpty()) return high("Git 命令缺少可判断的子命令");
        String subcommand = normalize(arguments.getFirst());
        List<String> tail = arguments.subList(1, arguments.size());
        if (Set.of("push", "reset", "rebase", "clean").contains(subcommand)) {
            return high("Git 命令会写入远程仓库或改写本地历史");
        }
        if (("checkout".equals(subcommand) || "restore".equals(subcommand))
                && containsAny(tail, "-f", "--force")) {
            return high("Git 强制操作可能覆盖工作区内容");
        }
        if ("branch".equals(subcommand) && containsAny(tail, "-d", "-D", "--delete")) {
            return high("Git 命令会删除分支");
        }
        if ("tag".equals(subcommand) && containsAny(tail, "-d", "--delete")) {
            return high("Git 命令会删除标签");
        }
        if (Set.of("add", "commit", "switch", "checkout", "restore", "merge", "pull", "fetch",
                "stash", "cherry-pick", "remote", "branch", "tag").contains(subcommand)) {
            return medium("Git 命令属于普通本地开发工作流");
        }
        return high("Git 子命令不在可信本地操作清单中");
    }

    private static boolean isPackageHighRisk(String executable, List<String> arguments) {
        if (arguments.isEmpty()) return false;
        String action = normalize(arguments.getFirst());
        if ("cargo".equals(executable)) {
            return Set.of("publish", "yank", "install", "uninstall").contains(action);
        }
        if (Set.of("mvn", "mvnw").contains(executable)) {
            return arguments.stream().map(RegexCommandRiskClassifier::normalize)
                    .anyMatch(value -> value.equals("deploy") || value.endsWith(":deploy"));
        }
        if (Set.of("gradle", "gradlew").contains(executable)) {
            return arguments.stream().map(RegexCommandRiskClassifier::normalize)
                    .anyMatch(value -> value.equals("publish") || value.startsWith("publish")
                            || value.endsWith(":publish"));
        }
        if ("docker".equals(executable)) return Set.of("push", "login").contains(action);
        if ("gh".equals(executable)) return Set.of("release", "repo").contains(action);
        return false;
    }

    private static CommandRiskAssessment classifyInterpreter(
            String executable, List<String> arguments) {
        if (containsAny(arguments, "-c", "-e", "--eval", "--print")) {
            return high("解释器参数会动态执行内联代码");
        }
        if (arguments.isEmpty()) return high("交互式解释器无法预先判断副作用");
        return medium(executable + " 命令执行本地开发文件或模块");
    }

    private static CommandRiskAssessment classifyJavaScriptPackageManager(
            String executable, List<String> arguments) {
        if (arguments.isEmpty()) return medium("包管理器仅显示本地项目信息");
        String action = normalize(arguments.getFirst());
        if (Set.of("publish", "unpublish", "exec", "dlx").contains(action)) {
            return high("包管理器命令会发布内容或动态执行软件包");
        }
        if (Set.of("install", "add", "remove", "uninstall").contains(action)
                && containsAny(arguments, "-g", "--global")) {
            return high("包管理器命令会修改全局软件包");
        }
        if (Set.of("install", "add", "remove", "uninstall", "ci", "test", "run", "start",
                "build", "lint", "format", "check", "list", "outdated").contains(action)
                || "yarn".equals(executable)) {
            return medium("包管理器命令属于普通本地项目操作");
        }
        return high("包管理器子命令不在可信本地操作清单中");
    }

    private static CommandRiskAssessment classifyDocker(List<String> arguments) {
        if (arguments.isEmpty()) return high("Docker 命令缺少可判断的子命令");
        String action = normalize(arguments.getFirst());
        if (Set.of("build", "inspect", "images", "ps", "version").contains(action)) {
            return medium("Docker 命令属于本地构建或查询操作");
        }
        return high("Docker 命令可能修改容器、网络、挂载或远程仓库");
    }

    private static boolean hasForceOverwrite(String executable, List<String> arguments) {
        if (Set.of("cp", "copy", "copy-item", "mv", "move", "move-item").contains(executable)) {
            return arguments.stream().map(RegexCommandRiskClassifier::normalize)
                    .anyMatch(value -> value.equals("-f") || value.equals("--force")
                            || value.startsWith("--force=") || value.equals("/y")
                            || value.equals("-force") || value.startsWith("-force:"));
        }
        return false;
    }

    private static boolean unsafePathToken(String token) {
        if (token == null || token.isBlank()) return false;
        String value = token.trim();
        if (value.startsWith("-")) {
            int equals = value.indexOf('=');
            int colon = value.indexOf(':');
            int separator = equals < 0 ? colon : colon < 0 ? equals : Math.min(equals, colon);
            return separator >= 0 && separator + 1 < value.length()
                    && unsafePathToken(value.substring(separator + 1));
        }
        if (value.startsWith("~") || value.startsWith("/") || value.startsWith("\\\\")
                || WINDOWS_ABSOLUTE.matcher(value).matches()
                || value.contains("$") || value.contains("`") || value.matches(".*%[^%]+%.*")) {
            return true;
        }
        for (String part : value.split("[/\\\\]+")) if ("..".equals(part)) return true;
        return false;
    }

    private static boolean containsAny(List<String> values, String... candidates) {
        for (String value : values) {
            for (String candidate : candidates) {
                if (value.equalsIgnoreCase(candidate)) return true;
            }
        }
        return false;
    }

    private static String executable(String token) {
        String value = normalize(token).replace('\\', '/');
        int slash = value.lastIndexOf('/');
        if (slash >= 0) value = value.substring(slash + 1);
        for (String suffix : List.of(".exe", ".cmd", ".bat", ".ps1")) {
            if (value.endsWith(suffix)) return value.substring(0, value.length() - suffix.length());
        }
        return value;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static CommandRiskAssessment medium(String reason) {
        return new CommandRiskAssessment(ToolRisk.MEDIUM, reason);
    }

    private static CommandRiskAssessment high(String reason) {
        return new CommandRiskAssessment(ToolRisk.HIGH, reason);
    }
}
