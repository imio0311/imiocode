package io.imiocode.hook.action;

import io.imiocode.hook.HookActionResult;
import io.imiocode.hook.HookContext;
import io.imiocode.hook.template.HookTemplateResolver;
import java.time.Duration;
import java.util.Objects;

/** 将模板展开为下一次模型请求使用的受控 System Reminder 文本。 */
public final class PromptHookExecutor implements HookActionExecutor<PromptAction> {
    private final HookTemplateResolver templates;
    public PromptHookExecutor(HookTemplateResolver templates) { this.templates = Objects.requireNonNull(templates); }
    @Override public HookActionType type() { return HookActionType.PROMPT; }
    @Override public HookActionResult execute(PromptAction action, HookContext context) {
        long start = System.nanoTime();
        String output = templates.resolve(action.message(), context).trim();
        if (output.isEmpty()) return HookActionResult.failure("Prompt Hook 展开后为空", elapsed(start));
        return HookActionResult.success(output, elapsed(start));
    }
    private static Duration elapsed(long start) { return Duration.ofNanos(System.nanoTime() - start); }
}
