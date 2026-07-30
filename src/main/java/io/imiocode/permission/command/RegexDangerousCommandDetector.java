package io.imiocode.permission.command;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** 面向 PowerShell 与 Bash 常见灾难命令的保守黑名单。 */
public final class RegexDangerousCommandDetector implements DangerousCommandDetector {
    private static final String DOWNLOAD_COMMAND =
            "(?:curl|wget|iwr|irm|invoke-webrequest|invoke-restmethod)";
    private static final String SHELL_COMMAND =
            "(?:sh|bash|zsh|pwsh|powershell(?:\\.exe)?)";
    private static final Pattern UNIX_ROOT_CHMOD = Pattern.compile(
            "(?:^|[;&|]\\s*)(?:sudo\\s+)?chmod\\b"
                    + "(?=[^;&|]*(?:\\s|^)(?:-r|--recursive)(?:\\s|$))"
                    + "(?=[^;&|]*(?:\\s|^)(?:777|a\\+rwx)(?:\\s|$))"
                    + "[^;&|]*\\s[\"']?/(?:\\*|\\.)?[\"']?\\s*(?:$|[;&|])");
    private static final Pattern WINDOWS_ROOT_ICACLS = Pattern.compile(
            "(?:^|[;&|]\\s*)icacls\\b"
                    + "(?=[^;&|]*\\s[\"']?[a-z]:[/\\\\](?:\\*|\\.)?[\"']?(?:\\s|$))"
                    + "(?=[^;&|]*/grant(?::r)?\\b)"
                    + "(?=[^;&|]*\\beveryone\\s*:\\s*f\\b)"
                    + "(?=[^;&|]*/t\\b)[^;&|]*");
    private static final Pattern DOWNLOAD_PIPE_SHELL = Pattern.compile(
            "\\b(?:curl|wget)\\b[^;&]*\\|\\s*(?:sudo\\s+)?" + SHELL_COMMAND + "\\b");
    private static final Pattern POWERSHELL_DOWNLOAD_PIPE = Pattern.compile(
            "\\b(?:iwr|irm|invoke-webrequest|invoke-restmethod)\\b"
                    + "[^;&]*\\|\\s*(?:iex|invoke-expression)\\b");
    private static final Pattern EVAL_DOWNLOAD = Pattern.compile(
            "(?:^|[;&|]\\s*)eval\\b[^;&|]*\\$\\(\\s*" + DOWNLOAD_COMMAND + "\\b");
    private static final Pattern SHELL_DOWNLOAD_SUBSTITUTION = Pattern.compile(
            "(?:^|[;&|]\\s*)" + SHELL_COMMAND
                    + "\\b[^;&|]*\\$\\(\\s*" + DOWNLOAD_COMMAND + "\\b");
    private static final Pattern DIRECT_DOWNLOAD_SUBSTITUTION = Pattern.compile(
            "(?:^|[;&|]\\s*)\\$\\(\\s*" + DOWNLOAD_COMMAND + "\\b");
    private static final Pattern POWERSHELL_DOWNLOAD_CALL = Pattern.compile(
            "(?:^|[;|]\\s*)[&.]\\s*\\(?\\s*"
                    + "(?:iwr|irm|invoke-webrequest|invoke-restmethod)\\b");
    private static final List<Rule> RULES = List.of(
            rule("disk-format", "禁止格式化或清空磁盘",
                    "\\b(?:format(?:\\.com)?\\s+[a-z]:|mkfs(?:\\.[a-z0-9]+)?\\b|"
                            + "clear-disk\\b|initialize-disk\\b|diskpart\\b[^;&|]*\\bclean\\b)"),
            rule("raw-device-write", "禁止写入裸磁盘设备",
                    "\\bdd\\b[^;&|]*\\bof\\s*=\\s*/dev/(?:sd[a-z]|nvme\\d+n\\d+|disk\\d+)"),
            rule("system-power", "禁止通过工具关闭或重启系统",
                    "(?:^|[;&|]\\s*)(?:sudo\\s+)?(?:shutdown|reboot|halt|poweroff|restart-computer|stop-computer)\\b"),
            rule("fork-bomb", "禁止进程 Fork Bomb",
                    ":\\s*\\(\\s*\\)\\s*\\{\\s*:\\s*\\|\\s*:\\s*&\\s*;\\s*}\\s*;?\\s*:"),
            rule("unix-root-delete", "禁止递归删除系统根目录",
                    "\\brm\\b[^;&|]*(?:-\\w*r\\w*f|-\\w*f\\w*r)[^;&|]*(?:\\s/\\s*$|\\s/\\*\\s*$)"),
            rule("powershell-root-delete", "禁止递归删除系统盘或工作区根目录",
                    "\\b(?:remove-item|rm|del|erase|rd|rmdir)\\b[^;&|]*(?:-recurse|/s)[^;&|]*"
                            + "(?:[a-z]:[/\\\\](?:\\*|\\.)?|\\s\\.\\s*$)"),
            rule("workspace-root-delete", "禁止递归删除整个工作区",
                    "(?:^|[;&|]\\s*)rm\\s+(?:-[a-z]*r[a-z]*f|-[a-z]*f[a-z]*r)\\s+(?:\\.|\\.\\/|\\*)\\s*$"),
            rule("git-reset-hard", "禁止不可逆重置 Git 工作区",
                    "\\bgit\\b[^;&|]*\\breset\\s+--hard\\b"));

    @Override
    public Optional<DangerousCommandMatch> inspect(String command, Path workspace) {
        Objects.requireNonNull(workspace, "workspace 不能为空");
        if (command == null || command.isBlank()) {
            return Optional.of(new DangerousCommandMatch("empty-command", "命令不能为空"));
        }
        String normalized = normalize(command);
        for (Rule rule : RULES) {
            String shellDeescaped = normalized.replaceAll("\\\\(?=[a-z])", "");
            if (rule.pattern().matcher(normalized).find()
                    || rule.pattern().matcher(shellDeescaped).find()) {
                return Optional.of(new DangerousCommandMatch(rule.id(), rule.reason()));
            }
        }
        String operatorsVisible = maskQuotedText(normalized);
        if (UNIX_ROOT_CHMOD.matcher(normalized).find()) {
            return Optional.of(new DangerousCommandMatch(
                    "unix-root-chmod", "禁止递归开放系统根目录权限"));
        }
        if (WINDOWS_ROOT_ICACLS.matcher(normalized).find()) {
            return Optional.of(new DangerousCommandMatch(
                    "windows-root-icacls", "禁止递归接管 Windows 系统盘权限"));
        }
        if (DOWNLOAD_PIPE_SHELL.matcher(operatorsVisible).find()) {
            return Optional.of(new DangerousCommandMatch(
                    "download-pipe-shell", "禁止下载远程内容后直接交给 Shell 执行"));
        }
        if (POWERSHELL_DOWNLOAD_PIPE.matcher(operatorsVisible).find()) {
            return Optional.of(new DangerousCommandMatch(
                    "powershell-download-iex", "禁止下载远程内容后交给 Invoke-Expression 执行"));
        }
        if (EVAL_DOWNLOAD.matcher(normalized).find()
                || SHELL_DOWNLOAD_SUBSTITUTION.matcher(normalized).find()
                || DIRECT_DOWNLOAD_SUBSTITUTION.matcher(normalized).find()
                || POWERSHELL_DOWNLOAD_CALL.matcher(operatorsVisible).find()) {
            return Optional.of(new DangerousCommandMatch(
                    "download-expression-execution", "禁止通过表达式直接执行远程下载内容"));
        }
        if (isDestructiveGitClean(normalized)) {
            return Optional.of(new DangerousCommandMatch(
                    "git-clean-fdx", "禁止不可逆清理 Git 未跟踪和忽略文件"));
        }
        return Optional.empty();
    }

    static String normalize(String command) {
        return command
                .replace("`", "")
                .replace("\r\n", ";")
                .replace('\r', ';')
                .replace('\n', ';')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static boolean isDestructiveGitClean(String command) {
        java.util.regex.Matcher matcher = Pattern.compile("\\bgit\\b[^;&|]*?\\bclean\\b([^;&|]*)")
                .matcher(command);
        while (matcher.find()) {
            String tail = matcher.group(1);
            boolean force = false;
            boolean directories = false;
            boolean ignored = false;
            for (String token : tail.trim().split("\\s+")) {
                if (!token.startsWith("-") || token.startsWith("--")) {
                    continue;
                }
                String flags = token.substring(1);
                force |= flags.indexOf('f') >= 0;
                directories |= flags.indexOf('d') >= 0;
                ignored |= flags.indexOf('x') >= 0;
            }
            if (force && directories && ignored) {
                return true;
            }
        }
        return false;
    }

    /**
     * 保留引号边界但遮蔽其中的普通文本，避免把 URL 或提示文字里的管道符当成执行管道。
     */
    private static String maskQuotedText(String command) {
        StringBuilder masked = new StringBuilder(command.length());
        char quote = 0;
        for (int index = 0; index < command.length(); index++) {
            char character = command.charAt(index);
            if (quote == 0 && (character == '\'' || character == '"')) {
                quote = character;
                masked.append(' ');
                continue;
            }
            if (quote != 0 && character == quote) {
                quote = 0;
                masked.append(' ');
                continue;
            }
            masked.append(quote == 0 ? character : ' ');
        }
        return masked.toString();
    }

    private static Rule rule(String id, String reason, String regex) {
        return new Rule(id, reason, Pattern.compile(regex));
    }

    private record Rule(String id, String reason, Pattern pattern) {
    }
}
