package io.imiocode.hook.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.hook.HookActionResult;
import io.imiocode.hook.HookContext;
import io.imiocode.hook.HookExecutionStatus;
import io.imiocode.hook.template.HookTemplateResolver;
import io.imiocode.tool.SecretRedactor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 展开并执行 Hook 命令，将进程结果脱敏后映射为统一 Hook 状态。
 *
 * <p>子进程只继承启动所需的少量系统变量；模型密钥等敏感环境变量不会默认传播。</p>
 */
public final class CommandHookExecutor implements HookActionExecutor<CommandAction>, AutoCloseable {
    private final HookTemplateResolver templates;
    private final HookProcessRunner runner;
    private final SecretRedactor redactor;
    private final ObjectMapper mapper = new ObjectMapper();

    public CommandHookExecutor(HookTemplateResolver templates, HookProcessRunner runner, SecretRedactor redactor) {
        this.templates = templates; this.runner = runner; this.redactor = redactor;
    }
    @Override public HookActionType type() { return HookActionType.COMMAND; }
    @Override public HookActionResult execute(CommandAction action, HookContext context) {
        String command = templates.resolve(action.command(), context);
        ProcessResult result = runner.run(command, context.workspace(), environment(context), action.timeout());
        String output = redactor.redact(result.stdout()); String error = redactor.redact(result.stderr());
        if (result.timedOut()) return new HookActionResult(HookExecutionStatus.TIMED_OUT, output,
                Optional.of("Hook 命令执行超时" + suffix(error)), result.elapsed());
        if (!result.started()) return HookActionResult.failure(error, result.elapsed());
        if (result.exitCode() != 0) return HookActionResult.failure(
                error.isBlank() ? "Hook 命令退出码 " + result.exitCode() : error, result.elapsed());
        return HookActionResult.success(output, result.elapsed());
    }
    private Map<String, String> environment(HookContext context) {
        // 使用新映射显式构造环境，不能以 System.getenv() 为基础整体复制。
        Map<String, String> env = new LinkedHashMap<>();
        Map<String, String> system = System.getenv();
        for (String name : new String[]{"PATH", "Path", "SystemRoot", "ComSpec", "PATHEXT", "TEMP", "TMP", "HOME"})
            if (system.containsKey(name) && !redactor.isSensitiveEnvironmentName(name)) env.put(name, system.get(name));
        env.put("MEWCODE_EVENT", context.event().configName());
        env.put("MEWCODE_TOOL_NAME", context.toolName().orElse(""));
        env.put("MEWCODE_FILE_PATH", context.filePath().map(Object::toString).orElse(""));
        env.put("MEWCODE_MESSAGE", redactor.redact(context.message().orElse("")));
        env.put("MEWCODE_ERROR", redactor.redact(context.error().orElse("")));
        try { env.put("MEWCODE_TOOL_ARGS", mapper.writeValueAsString(context.safePayload(redactor).get("tool_args"))); }
        catch (JsonProcessingException exception) { env.put("MEWCODE_TOOL_ARGS", "{}"); }
        return env;
    }
    private static String suffix(String value) { return value.isBlank() ? "" : ": " + value; }
    @Override public void close() { runner.close(); }
}
