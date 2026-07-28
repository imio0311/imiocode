# ImioCode Ch4 Agent Loop 增强 Plan

## 架构概览

### 单轮流式执行层

新增独立的单轮执行组件，负责一次 Agent 轮次内的全部模型尝试：

- 发起模型流请求。
- 实时透传文本和 Thinking。
- 接收完整工具调用事件。
- 协调只读工具提前执行。
- 根据错误类型决定等待、提高 token 上限或结束。
- 成功后返回完整模型响应和有序工具结果。

`Agent` 仍只负责外层多轮 ReAct 循环，不直接承载重试细节。

### 流式工具调度层

新增任务级流式工具调度器：

- 按 Provider 原始索引缓存完整工具调用。
- 只有从索引 0 开始形成连续、无屏障的 LOW 前缀时才提前执行。
- 如果事件乱序、索引存在缺口或遇到非 LOW 工具，则保守排队。
- 模型流成功后补齐所有未执行调用，复用现有安全并发和串行屏障规则。
- 记录已经启动的调用，保证后续收集阶段不会重复执行。

这种“连续前缀”策略让正常顺序流获得提前执行收益，同时使乱序 Provider 不会让工具越过尚未发现的前置屏障。

### 模型重试层

新增固定重试策略：

- 每轮首次请求后最多重试三次。
- 限流、网络、服务端和请求超时采用固定退避或 Retry-After。
- 输出 token 不足时提高当前请求上限。
- 重试等待由可取消调度器驱动，不阻塞任务取消。
- 任一工具开始后关闭本轮自动重试资格。

### 未知工具熔断层

新增任务级连续计数器：

- 按原始工具顺序观察调用。
- 不存在的工具名累加。
- 当前允许且已启用的注册工具清零。
- 禁用或模式禁止工具保持计数不变。
- 第三次未知工具触发独立停止原因，并取消当前模型流和活动只读工具。

计数器贯穿同一用户任务的所有 Agent 轮次，任务结束后销毁。

### Provider 归一化层

扩展统一错误模型和请求模型：

- 增加“输出 token 上限”错误类别。
- 单次请求可以携带临时输出 token 上限，不修改全局配置。
- 三家 Provider 把各自的结束原因统一映射到该错误类别。
- 现有工具、Thinking、Usage 和正常完成协议保持不变。

### 事件与终端层

统一事件流增加重试事件。终端收到后：

- 结束当前半截 Thinking/回答行。
- 显示尝试次数、原因和等待时间。
- 下一次尝试从新行继续流式显示。

未知工具熔断沿用任务停止事件，但使用新的独立停止原因。

## 核心数据结构

### LlmErrorType

```java
public enum LlmErrorType {
    AUTHENTICATION,
    RATE_LIMIT,
    MODEL_NOT_FOUND,
    SERVER_ERROR,
    NETWORK,
    TIMEOUT,
    OUTPUT_LIMIT,
    PROTOCOL,
    INTERRUPTED,
    UNKNOWN
}
```

### AgentStopReason

```java
public enum AgentStopReason {
    FINAL_RESPONSE,
    MAX_ITERATIONS,
    TIMEOUT,
    ERROR,
    CANCELLED,
    TOO_MANY_UNKNOWN_TOOLS
}
```

`OUTPUT_LIMIT` 表示 Provider 明确因输出 token 上限停止；`TOO_MANY_UNKNOWN_TOOLS` 是独立的非错误停止原因。

### ChatRequest

```java
public record ChatRequest(
        List<ChatMessage> messages,
        List<SystemReminder> reminders,
        ToolSelection toolSelection,
        OptionalInt outputTokenLimit) {
}
```

旧构造方式保持兼容。正常请求由 Agent 注入当前配置的初始上限；重试只替换当前请求的 `outputTokenLimit`，不修改全局配置。

### RetryScheduled

```java
record RetryScheduled(
        int iteration,
        int nextAttempt,
        LlmErrorType reason,
        Duration delay,
        int outputTokenLimit
) implements AgentEvent {}
```

- `nextAttempt` 范围为 2–4。
- token 上限重试的 `delay` 为零。
- 事件只携带安全分类，不携带 Provider 原始错误正文。

### RetryDecision

```java
public record RetryDecision(
        int nextAttempt,
        LlmErrorType reason,
        Duration delay,
        int outputTokenLimit
) {
}
```

### LlmRetryPolicy

```java
public final class LlmRetryPolicy {
    public static final int MAX_RETRIES = 3;
    public static final int OUTPUT_TOKEN_CEILING = 64_000;

    public Optional<RetryDecision> decide(
            LlmException error,
            int completedRetries,
            int currentOutputTokenLimit,
            Duration taskTimeRemaining,
            boolean toolStarted);
}
```

决策结果为空表示当前错误不可重试、预算耗尽、工具已经开始，或剩余任务时间不足。

### ToolAvailability 与 ToolResolution

```java
public enum ToolAvailability {
    AVAILABLE,
    UNKNOWN,
    DISABLED,
    DISALLOWED
}
```

```java
public record ToolResolution(
        ToolAvailability availability,
        Optional<Tool> tool) {
}
```

```java
public ToolResolution resolve(
        String toolName,
        ToolSelection selection);
```

注册中心通过统一解析入口区分“不存在、禁用、模式禁止、可用”，避免熔断器和执行器分别猜测状态。

### UnknownToolCircuitBreaker

```java
public final class UnknownToolCircuitBreaker {
    public Attempt beginAttempt();

    public final class Attempt {
        public CircuitObservation observe(ToolResolution resolution);
        public boolean open();
        public void commit();
    }
}
```

```java
public enum CircuitObservation {
    UNCHANGED,
    INCREMENTED,
    RESET,
    OPENED
}
```

每次模型尝试使用临时快照：

- 流正常完成后提交计数状态。
- 流失败并准备重试时丢弃临时状态，避免同一半截响应被重复计数。
- 临时状态达到三次未知工具时立即熔断，无需等待流结束。

### StreamingTurnResult

```java
public record StreamingTurnResult(
        ChatResponse response,
        List<ToolExecution> toolExecutions,
        boolean toolsStarted) {
}
```

结果只在模型流正常结束时生成。工具结果按模型原始索引排列；没有工具时列表为空。

### StreamingTurnExecutor

```java
public final class StreamingTurnExecutor {
    public StreamingTurnResult execute(
            ChatRequest request,
            int iteration,
            boolean toolsMayExecute,
            AgentTaskContext task,
            UnknownToolCircuitBreaker breaker,
            AgentEventListener listener)
            throws LlmException, UnknownToolCircuitOpenException;
}
```

它负责模型尝试、重试、重试等待、流式工具调度和单轮结果收集。

### StreamingToolScheduler

```java
public final class StreamingToolScheduler implements AutoCloseable {
    public void onToolCallCompleted(int originalIndex, ToolCall call);
    public void onStreamCompleted();
    public List<ToolExecution> awaitResults();
    public boolean toolsStarted();
    public void cancel();
}
```

- `onToolCallCompleted` 缓存调用并推进可安全执行的连续 LOW 前缀。
- `onStreamCompleted` 解锁排队的非 LOW 工具和存在顺序歧义的工具。
- `awaitResults` 等待全部调用并按原始索引返回。
- 同一索引重复事件按协议错误处理，不会重复执行。

### RetryWaiter

```java
@FunctionalInterface
interface RetryWaiter {
    void await(Duration delay, AgentTaskContext task)
            throws InterruptedException;
}
```

生产实现分段或通过可取消条件等待；测试实现使用虚拟时间，不真实等待 1/2/4 秒。

## 模块设计

### 模型协议模块

**职责：**

- 请求级输出 token 上限覆盖全局默认值。
- 把三家 Provider 的输出上限结束信号归一为 `OUTPUT_LIMIT`。
- 输出上限结束不得发布正常流完成事件，也不得构造可提交响应。

**Provider 映射：**

- OpenAI Responses：处理 `response.incomplete`，当 `incomplete_details.reason` 表示达到输出上限时映射为 `OUTPUT_LIMIT`；请求使用 `max_output_tokens`。参考：[OpenAI Responses 流事件](https://platform.openai.com/docs/api-reference/responses-streaming/response/incomplete)。
- Anthropic Messages：`stop_reason=max_tokens` 映射为 `OUTPUT_LIMIT`；请求继续使用 `max_tokens`。参考：[Anthropic stop reason](https://docs.anthropic.com/pt/api/handling-stop-reasons)。
- DeepSeek Chat Completions：`finish_reason=length` 映射为 `OUTPUT_LIMIT`；请求使用 `max_tokens`。由于官方协议不能进一步区分输出上限和上下文过长，连续重试仍失败时按预算耗尽结束，不引入压缩。参考：[DeepSeek Chat Completion](https://api-docs.deepseek.com/api/create-chat-completion)。

### 流式收集模块

**职责：**

- 继续实时发布文本、Thinking、工具开始和 Usage。
- 增加完整工具调用回调；只有收到 `ToolCallCompleted` 才交给调度器。
- 工具参数碎片、Thinking 签名和 Provider 原始错误仍不进入 Agent 事件。
- 收集器只负责验证单次模型流，不负责重试决策。

**接口调整：**

```java
ChatResponse collect(
        ChatRequest request,
        int iteration,
        int attempt,
        AgentEventListener events,
        ToolCallCompletionListener toolCalls)
        throws LlmException;
```

### 单轮流式执行模块

**职责：**

1. 为本轮创建第 1 次模型尝试和临时熔断快照。
2. 为每次尝试创建独立流式工具调度器。
3. 调用流式收集器。
4. 成功时提交熔断快照、开放流结束屏障并收集工具结果。
5. 失败时取消本次调度器。
6. 若工具尚未开始且策略允许，则发布重试事件、可取消等待并开始下一尝试。
7. 达到预算或不可重试时向 Agent 返回安全错误。

为保持原 Ch4 的最大轮数语义，执行入口使用 `toolsMayExecute`。本轮已经是最大轮数时，调度器只收集工具调用，不提前执行也不在流结束后执行。

### 流式工具调度模块

**内部状态：**

- 按原始索引排序的完整工具调用表。
- 已观察熔断状态的索引集合。
- 已启动工具及其 future。
- 下一个可提前推进的连续索引。
- 是否已经遇到串行屏障。
- 模型流是否正常完成。
- 是否允许本轮执行工具。
- 取消和关闭状态。

**提前执行规则：**

1. 索引 0 完成后才可能建立提前执行前缀。
2. 连续索引对应 AVAILABLE + LOW 时立即提交到固定并发池。
3. UNKNOWN、DISABLED、DISALLOWED 生成失败结果但不执行；熔断观察完成后可以继续推进索引。
4. 遇到 AVAILABLE + MEDIUM/HIGH 后停止提前推进。
5. 出现索引缺口或乱序时等待缺失索引；流结束后再按完整排序处理。
6. 流成功后等待所有前置 LOW 工具，再执行串行屏障，随后开启新的 LOW 并发批次。
7. 每个索引进入“已启动或已生成失败结果”状态后不得再次处理。

**熔断：**

- 每个完整调用按原始索引交给本次熔断快照。
- 第三个连续 UNKNOWN 出现时取消当前模型请求和活动 LOW 工具。
- 后续调用不再执行。
- 专用异常只在 Agent 内部转换为 `TOO_MANY_UNKNOWN_TOOLS`，不会作为通用错误暴露。

### 重试策略模块

**决策顺序：**

1. 已启动工具：拒绝重试。
2. 已达到三次重试：拒绝重试。
3. 任务已停止或剩余时间不足：拒绝重试。
4. `OUTPUT_LIMIT`：`min(当前上限 × 2, 64000)`，零等待。
5. `RATE_LIMIT`：优先 Retry-After，否则使用当前重试序号对应的 1/2/4 秒。
6. `NETWORK`、`SERVER_ERROR`、`TIMEOUT`：使用 1/2/4 秒。
7. 其余错误：拒绝重试。

如果 Retry-After 大于任务剩余时间，不开始等待，由任务超时终态接管。

### 未知工具熔断模块

**职责：**

- 保存任务级已提交连续次数。
- 每次模型尝试从已提交状态建立临时快照。
- 正常流完成后提交临时状态。
- 流失败并重试时丢弃临时状态。
- 临时状态在当前响应内达到三次时可以立即打开。
- AVAILABLE 清零；UNKNOWN 增加；DISABLED 和 DISALLOWED 保持不变。

### Agent 核心模块

`Agent` 的每轮逻辑调整为：

1. 判断停止状态、超时和轮数。
2. 调用单轮流式执行器。
3. 追加成功的完整助手响应。
4. 无工具调用时完成任务。
5. 最大轮数仍请求工具时停止，工具保证尚未执行。
6. 将单轮执行器返回的有序工具结果追加为 TOOL 消息。
7. 进入下一轮。

Agent 不再在完整响应返回后直接调用原批次执行器。

### 事件与终端模块

- `RetryScheduled` 结束当前开放的 Thinking/助手行。
- 终端显示类似“第 2 次尝试将在 1 秒后开始：network”。
- 下一次文本增量创建新的助手行。
- `TOO_MANY_UNKNOWN_TOOLS` 显示独立中文原因。
- 重试事件不会触发 Ready、Completed 或历史提交。

### 会话事务模块

无需改变提交原则：

- 只有 Agent 最终成功时提交完整轨迹。
- 失败尝试的文本仅存在于事件输出。
- 重试耗尽、熔断、超时和取消均不提交本轮临时轨迹。
- 已提前执行工具后失败时继续报告可能存在副作用。

## 模块交互

### 正常的流式只读工具提前执行

```text
Agent
  │ 本轮请求，toolsMayExecute=true
  ▼
StreamingTurnExecutor
  │ attempt=1
  ▼
StreamingResponseCollector
  │ TextDelta / ThinkingDelta → AgentEvent → UI
  │ ToolCallCompleted(index=0, ReadFile)
  ▼
StreamingToolScheduler
  │ 连续前缀 + AVAILABLE + LOW
  ├── 立即提交 ReadFile
  │
  │ ToolCallCompleted(index=1, Grep)
  ├── 立即提交 Grep（受并发上限约束）
  │
  │ StreamCompleted
  ▼
等待两个只读工具
  │ 按 index 0、1 排序结果
  ▼
StreamingTurnResult
  ▼
Agent 追加 Assistant + ToolResult，进入下一轮
```

### LOW → 写工具 → LOW 屏障

```text
index 0 ReadFile 完整
  └── 流中立即执行

index 1 WriteFile 完整
  └── 记录串行屏障，不执行

index 2 Grep 完整
  └── 位于屏障之后，不提前执行

StreamCompleted
  ↓
等待 ReadFile
  ↓
串行执行 WriteFile
  ↓
执行 Grep
  ↓
按 0、1、2 返回结果
```

### 乱序工具事件

```text
先收到 index 1 完成
  └── 缺少 index 0，缓存

随后收到 index 0 完成
  ├── 先解析 index 0 的风险
  └── 再决定 index 1 是否可以提前执行
```

如果索引不是从 0 连续出现，则保守等待流结束；不会推测缺失位置，也不会越过未知屏障。

### 网络错误自动重试

```text
attempt 1
  ├── 已显示部分 TextDelta
  └── NETWORK，尚无工具启动
        ↓
丢弃 attempt 1 的响应与临时熔断状态
关闭 attempt 1 工具调度器
发布 RetryScheduled(nextAttempt=2, delay=1s)
        ↓
可取消等待
        ↓
attempt 2 从新终端行开始
        ↓
成功响应进入轨迹
```

失败尝试显示在终端，但不进入下一次模型请求的消息历史。

### 工具启动后的流失败

```text
ToolCallCompleted(ReadFile)
  ↓
ReadFile 已开始
  ↓
模型流 NETWORK
  ↓
取消 ReadFile 和本轮模型请求
  ↓
禁止自动重试
  ↓
TaskFailed(sideEffectsPossible=false)
```

若将来状态竞争中已经启动非 LOW 工具，则 `sideEffectsPossible=true`；本方案正常情况下非 LOW 工具不会在流结束前启动。

### 输出 token 上限重试

```text
当前上限 8,000
  ↓ OUTPUT_LIMIT
RetryScheduled(delay=0, limit=16,000)
  ↓ OUTPUT_LIMIT
RetryScheduled(delay=0, limit=32,000)
  ↓ 成功
继续当前 Agent 轮次
```

最多进行三次重试，上限计算始终使用 `min(当前上限 × 2, 64,000)`；已经为 64000 时不再创建相同上限的无效重试。

### 未知工具熔断

```text
任务已提交计数 = 1
  ↓ 新 attempt 建立临时快照
UNKNOWN → 临时计数 2
DISALLOWED → 保持 2
UNKNOWN → 临时计数 3，OPENED
  ↓
取消模型流和活动 LOW 工具
  ↓
TaskStopped(TOO_MANY_UNKNOWN_TOOLS)
  ↓
丢弃本轮轨迹，不请求下一轮
```

若模型流在计数达到 3 前因网络错误失败并重试，本次临时计数被丢弃，避免同一半截响应重复累计。

### 最大轮数保护

```text
iteration == maxIterations
  ↓
StreamingTurnExecutor(toolsMayExecute=false)
  ↓
流式显示与收集工具调用，但不执行
  ↓
响应仍包含工具
  ↓
TaskStopped(MAX_ITERATIONS)
```

### 超时或用户取消

```text
AgentTaskContext 进入 TIMEOUT / CANCELLED
  ├── 取消当前 LLM 请求
  ├── 取消重试等待
  ├── 取消提前执行的 LOW 工具
  ├── 禁止排队工具启动
  └── 竞争产生唯一 TaskStopped
```

## 文件组织

```text
docs/ch4-enhancement/
├── spec.md
├── plan.md
├── task.md
└── checklist.md

src/main/java/io/imiocode/
├── agent/
│   ├── Agent.java
│   ├── AgentEvent.java
│   ├── AgentStopReason.java
│   ├── AgentTaskContext.java
│   ├── StreamingResponseCollector.java
│   ├── StreamingTurnExecutor.java
│   ├── StreamingTurnResult.java
│   ├── StreamingToolScheduler.java
│   ├── ToolBatchExecutor.java
│   ├── LlmRetryPolicy.java
│   ├── RetryDecision.java
│   ├── RetryWaiter.java
│   ├── DefaultRetryWaiter.java
│   ├── UnknownToolCircuitBreaker.java
│   ├── CircuitObservation.java
│   └── UnknownToolCircuitOpenException.java
├── conversation/
│   ├── ChatRequest.java
│   ├── ConversationException.java
│   └── ConversationLoop.java
├── llm/
│   ├── LlmErrorType.java
│   ├── LlmStreamAssembler.java
│   └── provider/
│       ├── anthropic/AnthropicClient.java
│       ├── openai/OpenAiClient.java
│       └── deepseek/DeepSeekClient.java
├── terminal/
│   ├── TerminalUi.java
│   └── JLineTerminalUi.java
└── tool/
    ├── ToolAvailability.java
    ├── ToolResolution.java
    ├── ToolRegistry.java
    └── ToolExecutor.java

src/test/java/io/imiocode/
├── agent/
│   ├── LlmRetryPolicyTest.java
│   ├── UnknownToolCircuitBreakerTest.java
│   ├── StreamingToolSchedulerTest.java
│   ├── StreamingTurnExecutorTest.java
│   ├── AgentTest.java
│   ├── AgentCancellationTest.java
│   └── StreamingResponseCollectorTest.java
├── conversation/
│   ├── ConversationSessionTest.java
│   └── ConversationLoopTest.java
├── llm/
│   ├── LlmStreamAssemblerTest.java
│   └── provider/
│       ├── anthropic/AnthropicClientTest.java
│       ├── openai/OpenAiClientTest.java
│       └── deepseek/DeepSeekClientTest.java
├── terminal/
│   └── JLineTerminalUiTest.java
└── tool/
    ├── ToolRegistryTest.java
    ├── ToolExecutorTest.java
    └── ToolBatchExecutorTest.java
```

应用装配还会调整 `ImioCodeApplication.java`，把现有全局输出 token 上限注入 Agent；旧构造入口保留兼容默认值。

## 技术决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 单轮职责 | 新增独立单轮流式执行器 | 避免把重试和工具调度塞进外层 Agent 或协议收集器 |
| 提前执行范围 | 仅 AVAILABLE + LOW 工具 | 防止半截模型响应产生不可逆副作用 |
| 乱序处理 | 只提前执行从索引 0 开始的连续安全前缀 | 无法证明前置顺序时保守等待，保证不跨屏障 |
| 写入与命令 | 等流成功后串行执行 | 保留原 Ch4 安全语义 |
| 重试资格 | 任一工具启动后立即失效 | 避免重试生成新调用 ID 导致重复执行 |
| 重试预算 | 初次请求后最多三次重试 | 与已批准范围和截图行为一致 |
| 退避 | Retry-After 优先，否则固定 1/2/4 秒 | 行为确定、容易测试，不引入随机抖动 |
| 等待实现 | 可取消等待接口和生产实现 | 任务超时和 Ctrl+C 可以立即结束，测试无需真实等待 |
| token 上限 | 请求级覆盖，逐次翻倍至 64000 | 不污染全局配置，不影响下一轮或下一任务 |
| Provider 结束映射 | 统一为 OUTPUT_LIMIT | 重试策略不感知厂商协议 |
| DeepSeek `length` | 映射 OUTPUT_LIMIT，预算耗尽后停止 | 官方字段不能区分输出上限和上下文过长，本章不做压缩 |
| 失败尝试文本 | 保留终端显示，不进入历史 | 兼顾实时反馈和会话事务正确性 |
| 重试分隔 | 新增统一重试事件 | UI 不解析异常，也能明确区分多次尝试 |
| 熔断计数 | 任务级提交状态和尝试级临时快照 | 网络重试不会把同一半截响应重复计数 |
| 熔断终态 | 独立停止原因 | UI、测试和上层调用方可以精确识别 |
| 第三次未知工具 | 取消模型流和活动 LOW 工具 | 立即阻止更多浪费和后续工具启动 |
| 最大轮数末轮 | 向单轮执行器传入禁止执行标记 | 防止 LOW 工具在 Agent 判断轮数前提前运行 |
| 已执行索引 | 调度器永久记录 | 同一完整调用不会在流结束补执行时重复运行 |
| 旧批次执行器 | 保留兼容，不再作为 Agent 主入口 | 降低 Ch3 和原 Ch4 回归风险 |
| 时间边界 | 任务总截止时间优先于重试策略 | Retry-After 或退避不能延长任务预算 |
| 终态竞争 | 沿用任务上下文的原子终态 | 超时、熔断、错误和成功只能有一个胜出 |
| 配置范围 | 不新增重试配置 | 遵循 YAGNI 和已批准的不做范围 |
| 协议资料 | 使用三家官方结束字段 | 避免用错误正文猜测输出上限 |

## Spec 覆盖

| Spec | 设计归属 |
|---|---|
| F1–F5 | 流式收集、流式工具调度、单轮流式执行 |
| F6–F13 | 重试策略、请求级 token 上限、Provider 归一化、重试事件 |
| F14–F17 | 工具可用性解析、未知工具熔断、Agent 停止与终端 |
| F18 | 单轮执行重试边界和工具执行器 |
| F19 | 三家 Provider 归一化 |
| F20 | AgentTaskContext、Agent 与回归设计 |
| N1–N9 | 调度状态、取消、唯一终态和会话事务 |
| N10–N13 | 安全事件、兼容接口和 Provider 回归 |
| N14–N18 | 可注入等待、竞争测试、全量回归和 tmux 验收 |
