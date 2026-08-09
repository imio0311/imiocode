# CH12 Hook 系统 Plan

## 架构概览

CH12 在现有应用装配层、会话协调器、Agent Loop、工具执行器和 UI 之间增加一个独立的 `io.imiocode.hook` 模块。核心业务层只依赖小型 `HookRuntime` 接口；没有 Hook 时注入 `NOOP` 实现，避免在各处散落空判断，也保证现有构造器和测试可平滑迁移。

整体分为六层：

1. **领域模型层**：定义 15 个 `HookEvent` 常量、`Hook`、`Action`、`HookContext`、条件结构、失败策略、动作结果和 `ToolRejectedError`。领域对象构造后不可变，配置加载阶段完成规范化。
2. **表达式层**：`ConditionParser` 将 YAML 中的单行表达式解析为 `ConditionGroup`，`ConditionEvaluator` 从 `HookContext` 解析字段并执行相等、正则、glob 和短路组合。解析与求值分离，使非法表达式在启动期失败，而运行期只处理已编译条件。
3. **动作执行层**：`ActionDispatcher` 按动作类型分派到 command、prompt、http、agent 四个 `HookActionExecutor`。外部副作用通过可替换的进程与 HTTP 端口执行；agent 使用占位执行器返回明确的不支持结果。
4. **调度层**：`DefaultHookEngine` 持有有序 Hook 列表、原子 once 集合、有界异步执行器、通知队列和动作分派器。普通事件走 `runHooks`，工具前置事件走 `runPreToolHooks`；前者隔离默认失败，后者返回允许或 `ToolRejectedError`，不会把工具拒绝抛成 Agent 崩溃。
5. **提示路由层**：prompt 动作不直接修改 System Prompt。共享的 `HookPromptInbox` 按产生顺序保存一次性 `SystemReminder`，Agent 在每次模型请求的 `pre_send` 阶段排空；当前轮的 `turn_start`/`pre_send` 提示立即生效，模型响应或工具执行后产生的提示在下一次模型请求生效。没有后续模型请求的关闭事件仅产生通知，不跨会话泄漏提示。
6. **生命周期适配层**：应用装配负责 `startup`/`shutdown`，`ConversationCoordinator` 负责会话和 compact，`Agent`/`StreamingTurnExecutor` 负责轮次及模型通信，`ToolBatchExecutor` 负责工具前后，`PermissionGate` 负责真实确认，文件与命令核心工具负责成功变更或实际进程启动。适配层把已有对象转换成最小、脱敏的 `HookContext`，并把 Hook 通知交给终端 UI 批量展示。

关键调用链如下：

```text
config.yaml
    → HookConfigLoader + HookValidator
    → DefaultHookEngine
        ├─ ConditionEvaluator
        ├─ ActionDispatcher → command / prompt / http / agent-placeholder
        ├─ HookPromptInbox → Agent 下一次 pre_send 的 system-reminder
        └─ HookNotificationQueue → ConversationLoop → TerminalUi

LLM tool_use
    → runPreToolHooks
        ├─ allow → PermissionGate → Tool.execute → post_tool_use
        └─ reject → ToolResult(error) → 回传 LLM → 下一轮策略调整
```

## 核心数据结构与接口

### 事件与 Hook 定义

```java
public enum HookEvent {
    STARTUP("startup"), SHUTDOWN("shutdown"),
    SESSION_START("session_start"), SESSION_END("session_end"),
    TURN_START("turn_start"), TURN_END("turn_end"),
    PRE_SEND("pre_send"), POST_RECEIVE("post_receive"),
    PRE_TOOL_USE("pre_tool_use"), POST_TOOL_USE("post_tool_use"),
    PERMISSION_REQUEST("permission_request"), COMPACT("compact"),
    FILE_CHANGE("file_change"), COMMAND_EXECUTE("command_execute"),
    ERROR("error");

    public String configName();
    public static HookEvent parse(String value);
}

public enum HookFailurePolicy { IGNORE, FAIL, REJECT }

public record Hook(
        String id,
        HookEvent event,
        Optional<ConditionGroup> condition,
        Action action,
        boolean once,
        boolean async,
        boolean reject,
        Optional<String> rejectMessage,
        HookFailurePolicy onError) { }
```

`Hook.id` 在单份配置内唯一；配置列表顺序就是同步执行顺序。`reject=true` 表示动作成功后也拒绝当前工具；`onError=REJECT` 表示动作失败时拒绝。两者只允许用于 `PRE_TOOL_USE`，且与 `async=true` 互斥。

### 动作模型

```java
public sealed interface Action
        permits CommandAction, PromptAction, HttpAction, AgentAction {
    HookActionType type();
}

public enum HookActionType { COMMAND, PROMPT, HTTP, AGENT }

public record CommandAction(String command, Duration timeout) implements Action { }

public record PromptAction(String message) implements Action { }

public record HttpAction(
        URI url,
        String method,
        Map<String, String> headers,
        Optional<String> body,
        Duration timeout) implements Action { }

public record AgentAction(String prompt) implements Action { }
```

command 默认超时 10 分钟；http 默认方法 POST、超时 10 秒。所有集合在构造时复制为不可变结构。HTTP body 缺失时由执行器序列化脱敏后的上下文。

### 条件模型

```java
public enum ConditionOperator { EQUALS, NOT_EQUALS, REGEX, GLOB }
public enum ConditionConnector { AND, OR }

public record Condition(
        String field,
        ConditionOperator operator,
        String expected,
        Optional<Pattern> compiledRegex) { }

public record ConditionGroup(
        ConditionConnector connector,
        List<Condition> conditions) { }

public interface ConditionParser {
    ConditionGroup parse(String expression) throws ConditionParseException;
}

public interface ConditionEvaluator {
    boolean matches(ConditionGroup group, HookContext context);
}
```

一个不含连接符的表达式解析为只有一个 `Condition` 的 AND 组。解析器只识别未被引号包裹的 `&&` 或 `||`；支持单引号、双引号以及反斜杠转义，因此正则和值可以安全包含空格。组内按声明顺序短路，不设计括号和隐式优先级。

### 运行上下文

```java
public record HookContext(
        HookEvent event,
        Path workspace,
        Optional<String> sessionId,
        OptionalInt iteration,
        Optional<String> toolName,
        Map<String, Object> toolArgs,
        Optional<Path> filePath,
        Optional<String> message,
        Optional<String> error,
        Map<String, Object> data) {

    public static Builder builder(HookEvent event, Path workspace);
    public Optional<String> resolveField(String field);
    public Map<String, Object> safePayload(SecretRedactor redactor);
}
```

`resolveField` 统一解析条件和模板变量，`args.a.b`/`$TOOL_ARGS.a.b` 可沿嵌套 Map 读取，数组索引和任意表达式不在本章支持范围内。上下文只携带当前事件所需的快照，不持有 Agent、Registry、UI 或 LLM 客户端。

### 执行结果、拒绝与通知

```java
public enum HookExecutionStatus {
    SUCCEEDED, SKIPPED_CONDITION, SKIPPED_ONCE,
    FAILED, TIMED_OUT, REJECTED, NOT_IMPLEMENTED, QUEUED
}

public record HookActionResult(
        HookExecutionStatus status,
        String output,
        Optional<String> safeError,
        Duration elapsed) { }

public record HookExecutionRecord(
        String hookId,
        HookEvent event,
        HookActionResult result) { }

public record HookRunResult(
        List<HookExecutionRecord> executions,
        List<SystemReminder> reminders) { }

public record PreToolHookResult(
        List<HookExecutionRecord> executions,
        Optional<ToolRejectedError> rejection) {
    public boolean allowed();
}

public final class ToolRejectedError extends RuntimeException {
    public String hookId();
    public String safeReason();
}

public record HookNotification(
        Instant occurredAt,
        String hookId,
        HookEvent event,
        HookExecutionStatus status,
        String summary) { }
```

`ToolRejectedError` 关闭可写堆栈，不把命令、头信息或原始异常放入 message。转换为 tool result 时使用固定前缀 `blocked by hook <id>:` 加 `safeReason`，便于模型和 UI 稳定识别。

### 核心运行接口

```java
public interface HookRuntime extends AutoCloseable {
    HookRuntime NOOP = ...;

    HookRunResult runHooks(HookEvent event, HookContext context);
    PreToolHookResult runPreToolHooks(HookContext context);
    List<HookNotification> drainNotifications();
    List<SystemReminder> drainPrompts();
    void clearSessionPrompts();
    @Override void close();
}

public interface HookActionExecutor<A extends Action> {
    HookActionType type();
    HookActionResult execute(A action, HookContext context);
}

public interface HookProcessRunner {
    ProcessResult run(String command, Path workspace,
                      Map<String, String> environment, Duration timeout);
}

public interface HookHttpTransport {
    HttpResult send(HttpRequestSpec request, Duration timeout);
}
```

`runHooks` 拒绝接收 `PRE_TOOL_USE`，防止调用方绕过专用语义；`runPreToolHooks` 只接受该事件。`drainNotifications` 和 `drainPrompts` 都是取走式 API。`HookRuntime.NOOP` 返回空结果，使未配置 Hook 的路径不分配线程池或网络客户端。

### YAML 映射结构

根配置采用列表，避免再引入一层无必要包装：

```yaml
hooks:
  - id: format-java
    event: file_change
    if: 'file_path ~= "**/*.java"'
    once: false
    async: true
    on-error: ignore
    action:
      type: command
      command: mvn spotless:apply
      timeout-seconds: 120

  - id: block-secret-write
    event: pre_tool_use
    if: 'tool == "write_file" && args.path ~= "**/.env"'
    reject: true
    reject-message: 不允许通过 Agent 修改环境密钥文件
    action:
      type: prompt
      message: 请改用 .env.example 记录变量名
```

配置层使用 `HookDocument`/`ActionDocument` 接收原始可空 YAML 字段，`HookConfigMapper` 在校验通过后一次性转换成上述不可变领域对象；领域层不依赖 Jackson 注解。

## 模块设计

### 配置加载与校验

**职责：** 把根 `config.yaml` 的 `hooks` 列表转换为完整、不可变且可直接执行的 Hook 集合。

`ConfigDocument` 新增 `List<HookDocument> hooks`，`RuntimeConfig` 新增 `HookConfigLoadResult hooks`。映射流程为：

```text
YAML 原始字段
  → Jackson 严格映射 HookDocument / ActionDocument
  → 环境占位符 ${NAME} 解析
  → HookValidator 汇总结构和交叉约束错误
  → ConditionParser 预编译条件
  → HookConfigMapper 生成 List<Hook>
```

`HookConfigLoadResult` 同时保存 `hooks` 和脱敏 `errors`。只要存在一个错误，`hooks` 必须为空；应用把全部错误输出到终端后继续使用 `HookRuntime.NOOP`。校验规则包括：

- id 非空且唯一，event 必须是 15 个事件之一；
- `if` 可选，但存在时必须完整解析且只使用允许字段；
- action 和 type 必填，不同类型分别校验 command/message/url/prompt；
- command/http timeout 必须为正数并设置实现上限；
- HTTP URI 只能为 `http`/`https`，method 仅允许常见无请求流的方法；
- `reject=true`、`reject-message`、`on-error=reject` 仅允许 `pre_tool_use`；
- `pre_tool_use` 和任何 reject Hook 禁止 `async=true`；
- 未知字段由 Jackson 的严格模式直接转为带 `hooks[index]` 路径的配置错误。

HTTP 头可继续使用项目现有 `${NAME}` 环境占位符；缺失变量只禁用整个 Hook 集。运行时 `$EVENT` 等模板变量保留到动作执行阶段，不在配置加载时展开。

### 条件解析、求值与模板替换

**职责：** 将用户字符串变成预编译条件，并为所有动作提供同一套单次变量替换。

`DefaultConditionParser` 用一次 quote-aware 扫描寻找连接符和操作符。引号、反斜杠和空白只在解析期处理，结果中的 expected 是解码后的原值。表达式出现两类连接符、空条件、多个操作符、未知字段或未闭合引号时立即失败。`=~` 在加载期编译 `Pattern`；`~=` 使用独立 `HookGlobPattern`，统一把路径分隔符归一为 `/`，确保 Windows 与 Unix 行为一致。

`DefaultConditionEvaluator` 对每个条件调用 `HookContext.resolveField`：普通标量直接转字符串，`args.a.b` 只遍历 Map；缺失字段视为空字符串。`==`/`!=` 精确区分大小写，`=~` 使用正则 find，`~=` 匹配完整归一化字符串。AND 遇到 false、OR 遇到 true 立即短路。

`HookTemplateResolver` 使用一次从左到右扫描替换：

```text
$EVENT
$TOOL_NAME
$FILE_PATH
$MESSAGE
$ERROR
$TOOL_ARGS.<dot.path>
```

模板值不会再次进入扫描；不支持的 `$NAME` 在配置校验期报告，合法但当前缺失的值变为空字符串。command、prompt、HTTP URL/header/body 和 reject-message 共享同一实现。

### Command 执行器

**职责：** 跨平台执行本地 shell 命令，限制时间和输出并可靠终止进程树。

`CommandHookExecutor` 先展开模板，再调用 `JdkHookProcessRunner`。Windows 使用 `cmd.exe /d /s /c`，Unix 使用 `/bin/sh -c`。工作目录固定为规范化后的项目根目录。子进程环境只保留 PATH 及平台启动所需变量，并加入：

```text
MEWCODE_EVENT
MEWCODE_TOOL_NAME
MEWCODE_FILE_PATH
MEWCODE_MESSAGE
MEWCODE_ERROR
MEWCODE_TOOL_ARGS   # 脱敏 JSON
```

stdout/stderr 并行读取并分别限制为 64 KiB，避免管道死锁和内存膨胀。超时或取消时先终止全部 descendants，再终止主进程；等待短暂宽限期后强制销毁仍存活的进程。返回结果区分成功、非零退出码、超时和启动失败。

### Prompt 执行器与收件箱

**职责：** 生成一次性 `SystemReminder`，不直接调用模型。

`PromptHookExecutor` 只负责模板替换和非空校验。普通 Hook 的成功文本由 Engine 放入有界 `HookPromptInbox`；Agent 在下一次 `pre_send` 完成后统一 drain，并追加到该请求的 reminders。同步 `turn_start`/`pre_send` 提示在当前请求生效，`post_receive`、工具后置和 turn-end 提示在下一次请求生效。

当 prompt action 属于显式 reject Hook 时，其输出只作为拒绝原因候选，不进入收件箱；`reject-message` 的优先级更高。`session_end`/`shutdown` 后没有合法注入目标，产生“无后续模型请求”的通知并丢弃提示；切换会话时清空旧会话未消费提示。

### HTTP 执行器

**职责：** 发送有界、可取消且可测试的 HTTP 请求。

`HttpHookExecutor` 展开 URI、header 和可选 body；没有 body 时用 Jackson 序列化 `HookContext.safePayload`。`JdkHookHttpTransport` 使用独立 `HttpClient`、禁用自动重定向，以配置 timeout 发送请求。仅 2xx 成功；响应体最多读取 1 MiB，超限、无效 URI、网络失败或超时均返回安全失败。日志和通知只显示 method、脱敏 host、状态码和截断摘要，不显示 header value。

### Agent 占位执行器

**职责：** 保留后续 SubAgent 章节的配置契约。

`AgentPlaceholderHookExecutor` 不创建线程、不调用 LLM，始终返回 `NOT_IMPLEMENTED` 和固定安全提示。该结果按 Hook 的 `on-error` 策略处理：默认 ignore 只通知，fail 可令当前生命周期操作失败，pre-tool 的 reject 可转为工具拒绝。

### DefaultHookEngine

**职责：** 统一匹配、排序、once、同步/异步调度、失败策略、提示与通知。

Engine 构造时把 Hook 按事件放入保持声明顺序的 `EnumMap<HookEvent, List<Hook>>`。`ConcurrentHashMap.newKeySet()` 保存运行期 fired id；条件匹配后、动作调度前用 `add(id)` 原子领取 once，动作失败也视为本次进程已经触发。

普通 `runHooks` 的流程：

1. 验证 event 与 context 一致，拒绝 `PRE_TOOL_USE`；
2. 依次条件匹配，记录 condition/once 跳过；
3. 同步动作立即执行，异步动作提交到最多 4 个任务、队列容量 128 的有界执行器；
4. 成功 prompt 写入收件箱；失败按 ignore/fail 处理；
5. 每次状态变化写入容量 256 的通知队列，满时丢最旧项并加入一条汇总告警；
6. 返回本次同步记录和 reminders，异步结果稍后只通过收件箱/通知可见。

`runPreToolHooks` 对单个工具调用串行执行匹配 Hook，永不提交后台任务。显式 `reject=true` 在动作完成后立即 short-circuit；动作失败时分别按 ignore、fail、reject 处理。reject 返回 `ToolRejectedError`，fail 返回安全的 Hook 执行失败记录，由工具适配层转换为普通失败 tool result。并行安全工具仍可各自在独立任务中运行前置链；once 领取和通知队列保持线程安全。

异步 Hook 的异常全部在任务边界捕获，不触发 `error` Hook。`close()` 原子拒绝新调度，取消队列任务及在途进程/HTTP，最多等待 2 秒后强制关闭；所有工作线程使用虚拟线程工厂且不会阻止 JVM 退出。

## 生命周期集成与模块交互

### 事件接入矩阵

| 事件 | 责任位置 | 精确触发时机 | 主要上下文 |
|---|---|---|---|
| `startup` | `ImioCodeApplication` | Hook Engine 与终端创建后、其他外部组件启动前，仅一次 | workspace、启动消息 |
| `shutdown` | `ImioCodeApplication` | 会话结束后、Hook Engine 关闭前，仅一次 | workspace、退出原因 |
| `session_start` | `ConversationCoordinator` | 初始会话建立、new 或 resume 完成后 | sessionId、恢复/新建信息 |
| `session_end` | `ConversationCoordinator` | 切换离开当前会话或 coordinator 关闭前 | sessionId、消息数 |
| `turn_start` | `Agent.run` | 用户任务已取得互斥执行权、第一次模型请求前 | sessionId、用户消息、mode |
| `turn_end` | `Agent.run` | completed/stopped/failed 任一结果确定后，在 finally 中一次 | iteration、stopReason、错误摘要 |
| `pre_send` | `StreamingTurnExecutor` | 每次实际 Provider 尝试调用前，包括重试 | iteration、attempt、最后消息摘要 |
| `post_receive` | `StreamingTurnExecutor` | 一次 Provider 流完整成功收集后 | iteration、attempt、assistant 文本摘要、tool call 数 |
| `pre_tool_use` | `StreamingToolScheduler` | 工具已解析可用后、权限 evaluate 前；每个 call 一次 | toolName、完整结构化 toolArgs |
| `post_tool_use` | `StreamingToolScheduler` | 工具实现真正执行并返回后，无论业务成功失败 | toolName、toolArgs、成功标记、脱敏结果摘要 |
| `permission_request` | `StreamingToolScheduler` | 权限 decision 确为 ASK 后、发布 UI 请求并阻塞前 | toolName、risk、操作目标 |
| `compact` | `Agent` | 手动、自动或恢复压缩确实改变上下文后 | beforeTokens、afterTokens、reason |
| `file_change` | `WriteFileTool` / `EditFileTool` 的监听器 | 原子写入成功后；失败不触发 | toolName、规范化 filePath |
| `command_execute` | `BashTool` 的监听器 | shell 子进程成功 start 后；启动失败不触发 | toolName、脱敏 command 摘要 |
| `error` | `Agent` / 应用边界 | 主流程错误最终确定且准备对用户报告时 | safe error、阶段；Hook 自身错误除外 |

Hook command 自己启动的 shell 不再次触发 `command_execute`，否则会形成递归。`file_change` 只对 ImioCode 可确认的 WriteFile/EditFile 原子写入负责，不猜测 Bash、MCP 或外部进程可能造成的文件变化。

### 应用装配与关闭顺序

`ImioCodeApplication` 在读取 `RuntimeConfig`、创建终端后构造 Hook 依赖：`HookTemplateResolver`、四个执行器、`ActionDispatcher`、`DefaultHookEngine`。配置有错误时逐条 `terminal.printError("[Hook] ...")` 并使用 NOOP；合法空列表同样使用 NOOP，不创建执行线程。

应用顺序：

```text
terminal ready
  → startup Hook
  → tools / MCP / LLM / Agent / session 装配
  → session_start Hook
  → ConversationLoop
  → coordinator.close → session_end
  → shutdown Hook
  → hookRuntime.close
  → MCP / LLM / terminal close
```

finally 中每一步独立保护，前一个 close 失败不跳过后续资源释放；关闭错误只使用 stderr 的安全摘要。`HookRuntime` 传给主 Agent 和 inline Skill；fork Skill 默认共享同一个 Engine 与 once 状态，但 fork Agent 不拥有 Engine 的关闭权。

### 会话与轮次

`ConversationCoordinator` 新增 `HookRuntime` 和当前 session 生命周期状态。构造完成后触发首个 `session_start`；`newSession`/`resumeSession` 先对旧会话触发 `session_end`，再清空旧 prompt inbox，完成历史切换后触发新 `session_start`。重复 close、失败的 resume 或删除非当前会话不会重复产生事件。

`Agent` 的最终构造器新增 `HookRuntime`，所有旧构造器委托 `HookRuntime.NOOP` 保持测试与调用兼容。`run` 在 `TaskStarted` 前触发 `turn_start`，并保存一次 `TurnHookState`；所有 return/exception 最终在 finally 根据实际结果触发一次 `turn_end`。`error` 在 `failed(...)` 形成前安全触发，error Hook 的 fail 不覆盖原始 AgentError。

prompt 的唯一注入点是模型请求前：`StreamingTurnExecutor` 先运行 `pre_send`，随后调用 `drainPrompts()`，把 inbox 内容追加为 `ChatRequest.reminders()`；`HookRunResult.reminders` 仅作为本次执行的可观察副本，不由调用方再次注入。同步 pre_send prompt 因而在当前请求可见；异步 prompt 如果来不及在本次 drain 前完成，则自然进入下一次请求。

`post_receive` 只在完整响应成功收集后触发；失败并进入 Provider retry 时，下一次尝试会再次产生 `pre_send`，但不会产生假的 `post_receive`。最终不可恢复的 Provider 错误由 Agent 产生一个 `error`。

### 工具前置拦截、权限与后置事件

`StreamingToolScheduler` 新增：

```java
private final HookRuntime hooks;
private final Map<Integer, PreToolHookResult> preToolResults;
```

`preparePreTool(index, call)` 使用 `computeIfAbsent`，在工具可用性解析后执行一次前置链。流式 LOW 工具的 eager prefix、流结束后的串行工具和并行 safe batch 都必须先调用它，再调用 `permissionEvaluation`。因此前置 Hook 可能对当前模型流施加同步背压，这是拦截语义的必要代价，但不会重复执行。

处理顺序固定为：

```text
resolve tool
  → pre_tool_use hooks
      ├─ reject → ToolResult.failure("blocked by hook ...")
      ├─ fail   → ToolResult.failure("hook execution failed ...")
      └─ allow
          → permission evaluate
              ├─ deny → permission failure result
              ├─ ask → permission_request Hook → HITL confirm
              └─ allow
                  → actual ToolExecutor.execute
                  → post_tool_use Hook
```

Hook 拒绝结果直接放进 scheduler 的有序 results，不设置 `toolsStarted`、不调用权限层、不触发 post-tool。它会被 `Agent.toToolMessage` 正常回传模型；若仍有迭代额度，模型继续下一轮。post-tool Hook 的 fail 不能撤销已经发生的工具副作用：工具原始结果仍优先回传，同时追加通知并标记 `sideEffectsPossible`；只有工具执行前的 pre-hook 可以硬拦截。

### 文件、命令和权限监听器

为避免核心工具直接依赖 Hook 包，在 `io.imiocode.tool` 增加：

```java
public interface ToolLifecycleListener {
    ToolLifecycleListener NOOP = ...;
    void onFileChanged(String toolName, Path path);
    void onCommandStarted(String toolName, String command);
}
```

WriteFile/EditFile 在成功原子写入后通知规范化路径；Bash 在 `ProcessBuilder.start()` 成功后通知已脱敏命令。监听器异常被工具边界隔离为 Hook 通知，不把已经成功的写入或已启动的进程伪装为工具失败。应用装配时注入 `HookToolLifecycleAdapter`，测试和旧构造器继续使用 NOOP。

`permission_request` 由 scheduler 在调用 `PermissionGate.confirm` 前触发，而不是监听所有权限 evaluate；因此 allow/deny 快速路径不显示确认事件。Hook fail 时安全拒绝该次工具，避免在自动化检查失败后继续进入 HITL。

### 压缩与错误

Agent 把 `ContextManager.manage` 和 `forceCompactHistory` 的结果统一交给 `emitCompactIfChanged`。只有 `ContextResult.compacted()==true` 才触发事件；自动阈值、context-limit recovery 和 `/compact` 分别写入 reason。compact prompt 在下一次模型请求使用，不写进已提交的压缩摘要。

`error` 只代表准备报告给用户的主流程错误。Hook 执行器、条件求值和通知渲染的错误在 Engine 内部转换为通知，设置 thread-local/reentrancy guard，绝不再次触发 error Hook。若 error Hook 自身配置 `fail`，原错误保持主因，Hook 错误只作为安全通知。

### UI 通知回收

`ConversationCoordinator` 暴露 `drainHookNotifications()`；`ConversationLoop` 在以下安全点调用：读取下一条输入前、Slash Command 完成后、每个 AgentEvent 后、Agent 返回或失败后、退出前。`TerminalUi.showHookNotification` 在 compact 模式只显示失败/超时/拒绝，在 verbose 模式显示所有状态；不新增交互式确认。

异步 Hook 在 UI 阻塞读取输入时完成，不直接从后台线程绘制终端，而是在下一个安全点被取走，避免破坏 JLine 输入框。通知取走后不会重复显示。

## 文件组织

```text
src/main/java/io/imiocode/
├── hook/
│   ├── HookEvent.java                  — 15 个事件及 YAML 名称解析
│   ├── Hook.java                       — 有序 Hook 定义与执行属性
│   ├── HookFailurePolicy.java          — ignore / fail / reject
│   ├── HookContext.java                — 不可变上下文、字段解析与安全载荷
│   ├── HookExecutionStatus.java        — 执行状态枚举
│   ├── HookActionResult.java           — 单个动作结果
│   ├── HookExecutionRecord.java        — Hook id + event + 动作结果
│   ├── HookRunResult.java              — 普通调度结果
│   ├── PreToolHookResult.java          — 工具前置允许/拒绝结果
│   ├── ToolRejectedError.java          — 安全工具拒绝原因
│   ├── HookNotification.java           — UI 可观察通知
│   ├── HookRuntime.java                — runHooks / runPreToolHooks / drain / close
│   ├── DefaultHookEngine.java          — 匹配、once、同步/异步、失败策略
│   ├── HookPromptInbox.java            — 有界一次性 system-reminder 队列
│   ├── HookNotificationQueue.java      — 有界通知队列
│   ├── HookExecutionException.java     — fail 策略的安全异常
│   ├── action/
│   │   ├── Action.java                 — sealed 动作契约
│   │   ├── HookActionType.java         — command/prompt/http/agent
│   │   ├── CommandAction.java
│   │   ├── PromptAction.java
│   │   ├── HttpAction.java
│   │   ├── AgentAction.java
│   │   ├── HookActionExecutor.java
│   │   ├── ActionDispatcher.java
│   │   ├── CommandHookExecutor.java
│   │   ├── PromptHookExecutor.java
│   │   ├── HttpHookExecutor.java
│   │   ├── AgentPlaceholderHookExecutor.java
│   │   ├── HookProcessRunner.java
│   │   ├── JdkHookProcessRunner.java
│   │   ├── HookHttpTransport.java
│   │   └── JdkHookHttpTransport.java
│   ├── condition/
│   │   ├── Condition.java
│   │   ├── ConditionGroup.java
│   │   ├── ConditionOperator.java
│   │   ├── ConditionConnector.java
│   │   ├── ConditionParser.java
│   │   ├── DefaultConditionParser.java
│   │   ├── ConditionEvaluator.java
│   │   ├── DefaultConditionEvaluator.java
│   │   ├── HookGlobPattern.java
│   │   └── ConditionParseException.java
│   ├── config/
│   │   ├── HookDocument.java            — 原始 YAML Hook 字段
│   │   ├── ActionDocument.java          — 原始 YAML action 字段
│   │   ├── HookConfigError.java
│   │   ├── HookConfigLoadResult.java
│   │   ├── HookValidator.java
│   │   └── HookConfigMapper.java
│   ├── template/
│   │   └── HookTemplateResolver.java    — 单次上下文变量展开
│   └── integration/
│       ├── HookContextFactory.java      — 各层对象到 HookContext
│       └── HookToolLifecycleAdapter.java
├── config/
│   ├── ConfigDocument.java              — 新增 hooks YAML 字段
│   ├── ConfigLoader.java                — 调用 Hook mapper 并汇总 notice
│   └── RuntimeConfig.java               — 暴露 HookConfigLoadResult
├── agent/
│   ├── Agent.java                       — turn/error/compact 与共享 runtime
│   ├── StreamingTurnExecutor.java       — pre_send/post_receive/prompt drain
│   └── StreamingToolScheduler.java      — pre/post tool、permission_request
├── runtime/
│   ├── ConversationCoordinator.java     — session 生命周期、通知 drain
│   └── ConversationLoop.java            — UI 安全点渲染通知
├── tool/
│   ├── ToolLifecycleListener.java       — 文件/命令的无 Hook 依赖监听器
│   └── core/
│       ├── WriteFileTool.java
│       ├── EditFileTool.java
│       └── BashTool.java
├── tui/
│   ├── TerminalUi.java                  — showHookNotification
│   └── JLineTerminalUi.java             — compact/verbose 渲染
└── ImioCodeApplication.java             — Hook 装配、startup/shutdown

src/test/java/io/imiocode/
├── hook/
│   ├── HookEventTest.java
│   ├── HookContextTest.java
│   ├── DefaultHookEngineTest.java
│   ├── HookTemplateResolverTest.java
│   ├── condition/ConditionParserTest.java
│   ├── condition/ConditionEvaluatorTest.java
│   ├── config/HookConfigMapperTest.java
│   └── action/
│       ├── CommandHookExecutorTest.java
│       ├── HttpHookExecutorTest.java
│       ├── PromptHookExecutorTest.java
│       └── AgentPlaceholderHookExecutorTest.java
├── agent/
│   ├── AgentHookIntegrationTest.java
│   └── StreamingToolSchedulerHookTest.java
├── runtime/ConversationHookIntegrationTest.java
└── tool/core/ToolLifecycleHookTest.java

docs/ch12/
├── spec.md
├── plan.md
├── task.md
└── checklist.md
```

小型只在包内使用的结果类型（例如 `ProcessResult`、`HttpResult`、`HttpRequestSpec`）与对应端口放在同一文件中，避免为纯实现细节制造额外公开 API。

## 技术决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 配置来源 | 根 `config.yaml` 的 `hooks` 列表 | 延续统一配置策略，声明顺序天然表达执行顺序 |
| 配置错误 | 汇总全部错误，整组降级为 NOOP | 不带着半套自动化运行，也不因可选 Hook 阻止进入 UI |
| 领域模型 | Java 21 record + sealed interface，不依赖 Jackson | 不可变、易测试，隔离 YAML 可空字段 |
| 条件处理 | 启动期解析/预编译，运行期只求值 | 提前暴露错误，降低每个生命周期节点开销 |
| glob 实现 | 自有 `/` 归一化 matcher | 避免 `PathMatcher` 在 Windows/Unix 上的分隔符差异 |
| Prompt 通道 | 有界 inbox，在 pre_send 统一转 SystemReminder | 不污染 System Prompt/cache，保证只消费一次 |
| 工具拦截位置 | 工具解析后、权限检查前 | 既能拿到结构化参数，又能在任何权限或副作用前硬拦截 |
| 拒绝反馈 | 普通失败 tool result，携带稳定 Hook 前缀 | LLM 可读并继续 ReAct，Agent 不崩溃，UI 可区分权限拒绝 |
| post-tool 失败 | 不覆盖已产生的工具结果，只通知 | 后置自动化无法撤销副作用，必须如实保留原工具结果 |
| once 领取 | 条件匹配后、调度前原子标记 | 并发安全；动作失败也不会在同一进程反复触发 |
| async 执行 | 有界虚拟线程执行器，pre-tool 禁止 async | 控制资源，同时保持硬拦截的同步确定性 |
| command shell | Windows cmd、Unix `/bin/sh`，最小环境 | 跨平台且不把 Provider API Key 等环境变量泄露给 Hook |
| 进程取消 | descendants → parent → 强制终止 | 超时时不遗留 Maven、Node 等子进程 |
| HTTP | JDK HttpClient、无重定向、1 MiB 上限 | 不增加依赖，限制隐藏跳转和内存占用 |
| agent 动作 | 可校验的占位执行器 | 固定未来契约，但不越过 CH12 范围 |
| Hook 错误递归 | Engine 内部错误只进通知，不触发 error Hook | 防止错误 Hook 无限递归 |
| 文件变化 | 核心工具成功点的中性监听器 | 精确、低成本，不通过目录扫描猜测外部副作用 |
| UI 更新 | 主线程安全点 drain，不在异步线程绘制 | 保持 JLine 输入稳定，通知只展示一次 |
| 兼容方式 | 旧构造器全部委托 NOOP | 现有调用者和测试无需一次性重写，未配置路径接近零成本 |

## Spec 覆盖检查

| Spec | 设计归属 |
|---|---|
| F1 生命周期事件 | `HookEvent` + 事件接入矩阵 |
| F2 统一 Hook 模型 | `Hook`、`Action`、`HookContext`、结果与错误类型 |
| F3 条件表达式 | condition 包的 parser/evaluator/glob |
| F4 变量替换 | `HookTemplateResolver` + `HookContext.resolveField` |
| F5 command | `CommandHookExecutor` + `JdkHookProcessRunner` |
| F6 prompt/http/agent | 三个执行器 + inbox + placeholder |
| F7 once/async/失败策略 | `DefaultHookEngine` 调度状态与有界执行器 |
| F8 工具拦截 | scheduler pre-tool 缓存、`ToolRejectedError`、tool result |
| F9 普通调度 | `runHooks` 有序调度和通知隔离 |
| F10 生命周期集成 | 应用、会话、Agent、工具、权限、压缩、UI 适配 |
| F11 配置校验 | config document/validator/mapper/load result |
| F12 可观测通知 | 有界通知队列 + UI 安全点 drain |

覆盖检查无缺口，模块依赖方向为 `config/integration → hook core → action/condition`，Agent、tool、runtime 只依赖 `HookRuntime` 或中性监听器，不形成对 UI、LLM 或配置层的反向依赖。

