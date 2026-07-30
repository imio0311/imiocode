package io.imiocode.permission.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** 只在完整命令能够被证明为确定性只读操作时返回安全。 */
public final class StrictSafeCommandDetector implements SafeCommandDetector {
    private static final Set<String> FILE_COMMANDS = Set.of(
            "ls", "dir", "cat", "type", "head", "tail", "wc", "stat", "file",
            "get-childitem", "get-content", "test-path", "resolve-path",
            "grep", "rg", "select-string");
    private static final Set<String> LOCATOR_COMMANDS = Set.of(
            "which", "where", "get-command");
    private static final Set<String> GIT_QUERY_COMMANDS = Set.of(
            "status", "diff", "log", "show", "rev-parse", "ls-files");
    private static final Set<String> GIT_BLOCKED_OPTIONS = Set.of(
            "--output", "--ext-diff", "--textconv");
    private static final Map<String, Set<String>> VERSION_FORMS = Map.ofEntries(
            Map.entry("java", Set.of("-version", "--version")),
            Map.entry("javac", Set.of("-version", "--version")),
            Map.entry("mvn", Set.of("-version", "--version", "-v")),
            Map.entry("node", Set.of("--version", "-v")),
            Map.entry("npm", Set.of("--version", "-v")),
            Map.entry("python", Set.of("--version", "-v")),
            Map.entry("go", Set.of("version")),
            Map.entry("rustc", Set.of("--version", "-v")),
            Map.entry("cargo", Set.of("--version", "-v")),
            Map.entry("gradle", Set.of("--version", "-v")));
    private static final Pattern WINDOWS_ABSOLUTE =
            Pattern.compile("^[a-zA-Z]:[/\\\\].*");
    private static final Pattern ENVIRONMENT_EXPANSION =
            Pattern.compile(".*(?:\\$|%[^%]+%).*");

    private final Path workspace;
    private final Path workspaceReal;
    private final ShellCommandScanner scanner;

    public StrictSafeCommandDetector(Path workspace) {
        this(workspace, new ShellCommandScanner());
    }

    StrictSafeCommandDetector(Path workspace, ShellCommandScanner scanner) {
        this.workspace = Objects.requireNonNull(workspace, "workspace 不能为空")
                .toAbsolutePath().normalize();
        this.workspaceReal = resolveRealPath(this.workspace);
        this.scanner = Objects.requireNonNull(scanner, "scanner 不能为空");
    }

    @Override
    public SafeCommandResult inspect(String command) {
        ShellCommandScanResult scan = scanner.scan(command);
        if (!scan.eligible()) {
            return SafeCommandResult.uncertain(scan.reason());
        }
        for (String segment : scan.segments()) {
            TokenizeResult tokenized = tokenize(segment);
            if (!tokenized.valid()) {
                return SafeCommandResult.uncertain(tokenized.reason());
            }
            SafeCommandResult segmentResult = inspectTokens(tokenized.tokens());
            if (!segmentResult.safe()) {
                return segmentResult;
            }
        }
        return SafeCommandResult.safe("完整命令仅包含严格白名单中的只读操作");
    }

    private SafeCommandResult inspectTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            return SafeCommandResult.uncertain("命令段不能为空");
        }
        String command = normalize(tokens.getFirst());
        List<String> arguments = tokens.subList(1, tokens.size());

        if (FILE_COMMANDS.contains(command)) {
            return inspectFileCommand(command, arguments);
        }
        if (LOCATOR_COMMANDS.contains(command)) {
            return inspectLocatorCommand(arguments);
        }
        if ("git".equals(command)) {
            return inspectGit(arguments);
        }
        if (VERSION_FORMS.containsKey(command)) {
            return inspectVersion(command, arguments);
        }
        return inspectSystemQuery(command, arguments);
    }

    private SafeCommandResult inspectFileCommand(String command, List<String> arguments) {
        for (String argument : arguments) {
            String normalized = normalize(argument);
            if ("rg".equals(command)
                    && (normalized.equals("--pre") || normalized.startsWith("--pre="))) {
                return SafeCommandResult.uncertain("rg --pre 可以执行外部程序");
            }
            if (!safeArgument(argument, true)) {
                return SafeCommandResult.uncertain("文件读取参数可能越出工作区或包含动态展开");
            }
        }
        return SafeCommandResult.safe("命令属于严格文件读取白名单");
    }

    private SafeCommandResult inspectLocatorCommand(List<String> arguments) {
        if (arguments.isEmpty()) {
            return SafeCommandResult.uncertain("定位命令缺少查询目标");
        }
        for (String argument : arguments) {
            if (!safeArgument(argument, false)) {
                return SafeCommandResult.uncertain("定位命令参数无法证明安全");
            }
        }
        return SafeCommandResult.safe("命令仅查询可执行程序位置");
    }

    private SafeCommandResult inspectGit(List<String> arguments) {
        if (arguments.isEmpty()) {
            return SafeCommandResult.uncertain("git 缺少只读子命令");
        }
        String subcommand = normalize(arguments.getFirst());
        List<String> tail = arguments.subList(1, arguments.size());
        if ("remote".equals(subcommand)) {
            return tail.size() == 1 && "-v".equalsIgnoreCase(tail.getFirst())
                    ? SafeCommandResult.safe("git remote -v 仅查询远程仓库")
                    : SafeCommandResult.uncertain("git remote 仅允许 -v 查询");
        }
        if ("branch".equals(subcommand)) {
            return tail.size() == 1 && "--show-current".equalsIgnoreCase(tail.getFirst())
                    ? SafeCommandResult.safe("git branch --show-current 仅查询当前分支")
                    : SafeCommandResult.uncertain("git branch 仅允许 --show-current");
        }
        if (!GIT_QUERY_COMMANDS.contains(subcommand)) {
            return SafeCommandResult.uncertain("git 子命令不在只读白名单");
        }
        for (String argument : tail) {
            String normalized = normalize(argument);
            if (GIT_BLOCKED_OPTIONS.stream().anyMatch(option ->
                    normalized.equals(option) || normalized.startsWith(option + "="))) {
                return SafeCommandResult.uncertain("git 参数可能写文件或执行外部程序");
            }
            if (!safeArgument(argument, true)) {
                return SafeCommandResult.uncertain("git 查询参数可能越出工作区或动态展开");
            }
        }
        return SafeCommandResult.safe("git 子命令属于严格只读白名单");
    }

    private SafeCommandResult inspectVersion(String command, List<String> arguments) {
        if (arguments.size() == 1
                && VERSION_FORMS.get(command).contains(normalize(arguments.getFirst()))) {
            return SafeCommandResult.safe("命令仅查询开发工具版本");
        }
        return SafeCommandResult.uncertain("开发工具只允许精确的版本查询参数");
    }

    private SafeCommandResult inspectSystemQuery(String command, List<String> arguments) {
        return switch (command) {
            case "pwd" -> arguments.stream().allMatch(this::isQueryOption)
                    ? SafeCommandResult.safe("pwd 仅查询当前目录")
                    : SafeCommandResult.uncertain("pwd 参数不在只读范围");
            case "get-location", "get-date" -> arguments.stream()
                    .allMatch(argument -> safeArgument(argument, false))
                    ? SafeCommandResult.safe("PowerShell 命令仅查询环境信息")
                    : SafeCommandResult.uncertain("PowerShell 查询参数无法证明安全");
            case "uname", "whoami" -> arguments.stream().allMatch(this::isQueryOption)
                    ? SafeCommandResult.safe("命令仅查询系统信息")
                    : SafeCommandResult.uncertain("系统查询命令包含非查询参数");
            case "hostname", "date" -> arguments.isEmpty()
                    ? SafeCommandResult.safe("命令仅查询系统信息")
                    : SafeCommandResult.uncertain("该系统命令带参数时可能修改状态");
            default -> SafeCommandResult.uncertain("命令不在安全只读白名单");
        };
    }

    private boolean isQueryOption(String argument) {
        return (argument.startsWith("-") || argument.startsWith("/"))
                && !containsDynamicExpansion(argument);
    }

    private boolean safeArgument(String argument, boolean checkWorkspacePath) {
        if (argument == null || argument.isBlank() || containsDynamicExpansion(argument)) {
            return false;
        }
        if (argument.startsWith("-")) {
            return safeOption(argument);
        }
        if (argument.startsWith("/") && !"/".equals(argument)) {
            return false;
        }
        return !checkWorkspacePath || safeWorkspaceToken(argument);
    }

    private boolean safeOption(String option) {
        String normalized = normalize(option);
        if (normalized.equals("--pre") || normalized.startsWith("--pre=")) {
            return false;
        }
        int separator = firstSeparator(option);
        if (separator >= 0 && separator + 1 < option.length()) {
            String value = option.substring(separator + 1);
            return !containsDynamicExpansion(value) && !looksEscapingPath(value);
        }
        return true;
    }

    private static int firstSeparator(String option) {
        int equals = option.indexOf('=');
        int colon = option.indexOf(':');
        if (equals < 0) {
            return colon;
        }
        if (colon < 0) {
            return equals;
        }
        return Math.min(equals, colon);
    }

    private boolean safeWorkspaceToken(String token) {
        if (looksEscapingPath(token)) {
            return false;
        }
        String pathCandidate = stripGlobSuffix(token);
        if (pathCandidate.isBlank() || ".".equals(pathCandidate)) {
            return true;
        }
        try {
            Path candidate = workspace.resolve(pathCandidate).normalize();
            if (!candidate.startsWith(workspace)) {
                return false;
            }
            Path existing = nearestExisting(candidate);
            return existing == null || resolveRealPath(existing).startsWith(workspaceReal);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean looksEscapingPath(String token) {
        if (token.startsWith("/")
                || token.startsWith("\\\\")
                || token.startsWith("//")
                || token.startsWith("~")
                || WINDOWS_ABSOLUTE.matcher(token).matches()) {
            return true;
        }
        for (String component : token.split("[/\\\\]+")) {
            if ("..".equals(component)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsDynamicExpansion(String token) {
        return ENVIRONMENT_EXPANSION.matcher(token).matches()
                || token.indexOf('`') >= 0;
    }

    private static String stripGlobSuffix(String token) {
        int wildcard = firstIndexOf(token, '*', '?', '[');
        if (wildcard < 0) {
            return token;
        }
        int slash = Math.max(token.lastIndexOf('/', wildcard), token.lastIndexOf('\\', wildcard));
        return slash < 0 ? "." : token.substring(0, slash);
    }

    private static int firstIndexOf(String text, char... candidates) {
        int first = -1;
        for (char candidate : candidates) {
            int index = text.indexOf(candidate);
            if (index >= 0 && (first < 0 || index < first)) {
                first = index;
            }
        }
        return first;
    }

    private static Path nearestExisting(Path candidate) {
        Path current = candidate;
        while (current != null) {
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static Path resolveRealPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException exception) {
            return path.toAbsolutePath().normalize();
        }
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static TokenizeResult tokenize(String segment) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean tokenStarted = false;

        for (int index = 0; index < segment.length(); index++) {
            char character = segment.charAt(index);
            if (quote == '\'') {
                if (character == '\'') {
                    quote = 0;
                } else {
                    current.append(character);
                }
                tokenStarted = true;
                continue;
            }
            if (quote == '"') {
                if (character == '\\' && index + 1 < segment.length()
                        && segment.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else if (character == '"') {
                    quote = 0;
                } else {
                    current.append(character);
                }
                tokenStarted = true;
                continue;
            }
            if (character == '\'' || character == '"') {
                quote = character;
                tokenStarted = true;
                continue;
            }
            if (Character.isWhitespace(character)) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
                continue;
            }
            if (character == '\\' && index + 1 < segment.length()
                    && Character.isWhitespace(segment.charAt(index + 1))) {
                current.append(segment.charAt(++index));
                tokenStarted = true;
                continue;
            }
            current.append(character);
            tokenStarted = true;
        }
        if (quote != 0) {
            return TokenizeResult.invalid("命令包含未闭合引号");
        }
        if (tokenStarted) {
            tokens.add(current.toString());
        }
        return tokens.isEmpty()
                ? TokenizeResult.invalid("命令段不能为空")
                : TokenizeResult.valid(tokens);
    }

    private record TokenizeResult(boolean valid, List<String> tokens, String reason) {
        private TokenizeResult {
            tokens = List.copyOf(tokens);
        }

        private static TokenizeResult valid(List<String> tokens) {
            return new TokenizeResult(true, tokens, "");
        }

        private static TokenizeResult invalid(String reason) {
            return new TokenizeResult(false, List.of(), reason);
        }
    }
}
