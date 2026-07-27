# ImioCode 第四章：Agent Loop Plan

## 架构概览

### 配置层

新增独立的 `AgentConfig`，保存最大循环轮数、任务总超时和安全工具并发数。它作为 `AppConfig` 的一部分由现有配置加载器统一生成，旧配置自动使用默认值。

### Agent 核心层

新增独立 `Agent`，持有：

- `LlmClient`
- `ToolRegistry`
- `AgentConfig`
- 当前 `AgentMode`

`Agent` 同步执行一个前台任务：创建模式快照和任务截止时间，进入循环，调用模型、收集响应、调度工具，再决定继续或停止。它不引用任何终端类型。

### 流式收集与事件层

新增 `StreamingResponseCollector`，接收现有 `LlmEvent`：

- 立即转换并发布对应 `AgentEvent`
- 保存本轮事件和完整 `ChatResponse`
- 验证流完成后才允许 Agent 使用响应

新增密封的 `AgentEvent` 事件模型，覆盖任务、循环轮次、模型流、工具批次、工具执行、模式切换及三种最终状态。终端只消费这些事件。

### 工具选择与 Provider 边界

为 `ChatRequest` 增加请求级工具选择快照：

- 正常模式：当前注册中心内全部已启用工具
- Plan Mode：仅 `read_file`、`glob`、`grep`

三家 Provider 根据请求快照导出工具 Schema，不修改全局注册中心。工具执行前再次检查同一个快照，防止模型伪造未授权工具调用。

### 工具分批调度层

新增任务级 `ToolBatchExecutor`：

- 按原始调用顺序切分连续安全批次和单个顺序屏障
- `LOW` 风险工具在固定大小的虚拟线程池中并发执行
- `MEDIUM/HIGH` 风险工具逐个串行执行
- 并发完成后按原始索引重排结果
- 任务结束、超时或中断时统一取消活动任务和工具

每个 Agent 任务创建独立执行器，任务结束后关闭，避免取消状态污染下一次任务。

### 会话与历史层

`ConversationSession` 不再驱动“两次模型请求”，改为：

1. 取得已提交历史和一次性提醒快照。
2. 委托 `Agent` 完成整个任务。
3. 仅在 Agent 正常得到最终回答后原子提交完整临时轨迹。
4. 停止或失败时丢弃临时轨迹，但保留真实工具副作用提示。

### 终端控制层

`ConversationLoop` 负责：

- 识别 `/plan`、`/do`、退出和空白输入
- 将普通用户任务交给会话
- 把 `AgentEvent` 映射为现有 Thinking、文本、Usage、工具状态和 Agent 停止提示
- 中断时取消当前 Agent 任务并退出

### 任务超时与可恢复取消

Agent 为每个任务建立单独的截止时间和看门任务。为 `LlmClient` 增加“只取消当前请求、不永久关闭客户端”的能力，使任务超时后会话仍可接受下一条用户输入；程序退出仍调用永久关闭。

## 核心数据结构

### AgentConfig

```java
public record AgentConfig(
        int maxIterations,
        Duration taskTimeout,
        int maxParallelTools) {

    public static AgentConfig defaults();
}
```

校验三个值均为正数。默认值分别为 20、600 秒、4。

### AgentMode

```java
public enum AgentMode {
    DO,
    PLAN
}
```

### AgentStopReason

```java
public enum AgentStopReason {
    FINAL_RESPONSE,
    MAX_ITERATIONS,
    TIMEOUT,
    ERROR,
    CANCELLED
}
```

### ToolSelection

```java
public record ToolSelection(
        boolean unrestricted,
        Set<String> allowedNames) {

    public static ToolSelection allEnabled();
    public static ToolSelection only(Set<String> names);
    public boolean allows(String name);
}
```

它随 `ChatRequest` 传给 Provider。旧构造入口默认使用 `allEnabled()`。

### AgentRequest

```java
public record AgentRequest(
        List<ChatMessage> committedHistory,
        ChatMessage userMessage,
        List<SystemReminder> reminders) {
}
```

### AgentError

```java
public record AgentError(
        String safeMessage,
        boolean recoverable,
        Optional<Duration> retryAfter) {
}
```

### AgentResult

```java
public record AgentResult(
        AgentStopReason stopReason,
        List<ChatMessage> trajectory,
        Optional<ChatResponse> finalResponse,
        boolean toolsExecuted,
        boolean sideEffectsPossible,
        Optional<AgentError> error) {

    public boolean completed();
}
```

只有 `FINAL_RESPONSE` 可以携带最终响应并提交轨迹。

### AgentEvent

```java
public sealed interface AgentEvent {
    record TaskStarted(AgentMode mode) implements AgentEvent {}
    record IterationStarted(int iteration) implements AgentEvent {}

    record TextDelta(int iteration, String text) implements AgentEvent {}
    record ThinkingDelta(int iteration, String text) implements AgentEvent {}
    record ThinkingCompleted(int iteration) implements AgentEvent {}
    record ModelToolRequested(
            int iteration, int callIndex, String callId, String toolName)
            implements AgentEvent {}
    record ModelResponseCompleted(
            int iteration, TokenUsage usage, boolean hasToolCalls)
            implements AgentEvent {}

    record ToolBatchStarted(
            int iteration, int batchIndex, ToolBatchKind kind, int size)
            implements AgentEvent {}
    record ToolExecutionChanged(
            int iteration, int callIndex, ToolExecutionEvent event)
            implements AgentEvent {}
    record ToolBatchCompleted(
            int iteration, int batchIndex, ToolBatchKind kind, int size)
            implements AgentEvent {}

    record ModeChanged(AgentMode previous, AgentMode current)
            implements AgentEvent {}

    record TaskCompleted(int iterations) implements AgentEvent {}
    record TaskStopped(
            AgentStopReason reason, int iterations, boolean sideEffectsPossible)
            implements AgentEvent {}
    record TaskFailed(
            int iterations, AgentError error, boolean sideEffectsPossible)
            implements AgentEvent {}
}
```

Thinking 完成事件不携带签名或 encrypted content；工具参数碎片也不进入 Agent 事件。终端仍通过现有安全摘要格式化工具执行事件，不直接显示原始参数或结果。

### AgentEventListener

```java
@FunctionalInterface
public interface AgentEventListener {
    void onEvent(AgentEvent event);

    AgentEventListener NOOP = event -> {};
}
```

### Agent

```java
public final class Agent implements AutoCloseable {
    public Agent(
            LlmClient client,
            ToolRegistry registry,
            AgentConfig config);

    public AgentResult run(
            AgentRequest request,
            AgentEventListener listener);

    public AgentMode mode();
    public void switchMode(
            AgentMode mode,
            AgentEventListener listener);

    public void cancelActive();
    public void close();
}
```

`run()` 同一时间只允许一个活动任务；模式在任务开始时形成快照。

### StreamingResponseCollector

```java
public final class StreamingResponseCollector {
    public ChatResponse collect(
            ChatRequest request,
            int iteration,
            AgentEventListener listener)
            throws LlmException;
}
```

它把安全的模型流事件实时映射为 `AgentEvent`，并返回完整响应给循环。

### 工具分批结构

```java
public enum ToolBatchKind {
    PARALLEL_SAFE,
    SERIAL_BARRIER
}

public record IndexedToolCall(
        int originalIndex,
        ToolCall call) {
}

public record ToolBatch(
        ToolBatchKind kind,
        List<IndexedToolCall> calls) {
}
```

### ToolCallPartitioner

```java
public final class ToolCallPartitioner {
    public List<ToolBatch> partitionToolCalls(
            List<ToolCall> calls,
            ToolRegistry registry,
            ToolSelection selection);
}
```

只有允许执行且风险为 `LOW` 的连续调用进入并发批次；未知、禁用、未允许、`MEDIUM` 和 `HIGH` 调用都按串行屏障处理。

### ToolBatchExecutor

```java
public final class ToolBatchExecutor implements AutoCloseable {
    public List<ToolExecution> execute(
            List<ToolCall> calls,
            ToolSelection selection,
            int iteration,
            AgentEventListener listener);

    public void cancel();
    public void close();
}
```

执行器内部使用固定大小的虚拟线程池，并在返回前按 `originalIndex` 恢复结果顺序。

### LlmClient 取消扩展

```java
public interface LlmClient extends AutoCloseable {
    ChatResponse streamChat(...);

    default void cancelActiveRequest() {}
    void close();
}
```

三家实际 Provider 实现“取消当前请求但保持客户端可继续使用”；`close()` 仍表示永久关闭。

## 模块设计

### 配置模块

**涉及：** `AppConfig`、`ConfigDocument`、`ConfigLoader`、`config.example.yaml`

新增配置：

```yaml
agent:
  max-iterations: 20
  timeout-seconds: 600
  max-parallel-tools: 4
```

对应环境变量：

- `IMIO_AGENT_MAX_ITERATIONS`
- `IMIO_AGENT_TIMEOUT_SECONDS`
- `IMIO_AGENT_MAX_PARALLEL_TOOLS`

环境变量只覆盖各自字段。旧 YAML 没有 `agent` 节点时使用默认配置。

### 请求级工具选择

**涉及：** `ChatRequest`、`ToolSelection`、`ToolRegistry`

`ChatRequest` 增加工具选择快照。`ToolRegistry` 增加：

- 返回当前已启用工具名称
- 按 `ToolSelection` 查询工具
- 按 `ToolSelection` 导出定义

Provider 每次构造请求时读取快照，不再只能导出全局启用工具。旧请求构造方式保持“全部已启用工具”的原行为。

### Agent 核心

**职责：**

- 保证同一时间只有一个活动任务
- 在任务开始时冻结模式、工具选择、提醒和截止时间
- 创建临时轨迹并执行 ReAct `while` 循环
- 每轮调用流式收集器
- 无工具调用时正常完成
- 有工具调用时执行整批工具、追加结果并继续
- 统一处理五种停止条件
- 只发布 Agent 事件，不操作终端

**循环顺序：**

```text
检查取消/超时/轮数
    ↓
发布 IterationStarted
    ↓
构造完整临时 ChatRequest
    ↓
流式调用 LLM
    ↓
追加完整助手响应
    ↓
无工具 ─────────────→ FINAL_RESPONSE
    ↓ 有工具
分批执行工具
    ↓
按原顺序追加 ToolResult 消息
    ↓
进入下一轮
```

Plan Mode 会把固定规划提醒追加到任务提醒快照，并使用只读工具选择。

### 任务生命周期与截止时间

Agent 内部维护一个原子 `ActiveTask`：

- 当前停止原因
- 当前 `ToolBatchExecutor`
- 截止时间
- 是否执行过工具
- 是否可能存在副作用

每个任务安排一个截止时间看门任务。超时时使用原子状态设置 `TIMEOUT`，取消当前模型请求和工具执行器。

用户中断设置 `CANCELLED`；程序关闭先取消任务，再永久关闭模型客户端和看门执行器。最终状态使用 CAS 确保只发布一次。

### 流式收集器

**职责：**

- 调用 `LlmClient.streamChat`
- 将文本和 Thinking 增量实时映射为 Agent 事件
- 将工具参数碎片、Thinking 签名和 encrypted content 留在 LLM 内部结构，不向 UI 事件暴露
- 记录是否收到正常 `StreamCompleted`
- 返回完整 `ChatResponse`
- 缺少完成事件时产生协议错误

`LlmClient` 的旧文本监听兼容桥会在正常返回后补发一次 `StreamCompleted`，实际三家 Provider 继续使用自己的富事件实现。

### 工具分区器

`ToolCallPartitioner` 单次线性扫描工具调用：

```text
LOW, LOW, LOW → PARALLEL_SAFE
MEDIUM        → SERIAL_BARRIER
LOW, LOW      → PARALLEL_SAFE
HIGH          → SERIAL_BARRIER
```

未知、禁用或不在工具选择快照中的调用按 `SERIAL_BARRIER` 处理，并生成失败结果，绝不执行。

### 工具执行模块

扩展现有 `ToolExecutor`：

- 增加单工具执行入口
- 使用线程安全集合记录多个活动工具
- 保留原串行入口用于兼容和回归测试
- 取消时通知所有活动工具

`ToolBatchExecutor` 负责：

- 顺序遍历 `ToolBatch`
- 并发批次提交到固定大小虚拟线程池
- 串行屏障在调用线程逐个执行
- 为每个工具发布带原始索引的状态事件
- 收集后按原始索引排序
- 单个失败只生成失败结果，不取消其他任务

### 会话模块

`ConversationSession` 改为 Agent 外层事务：

- 保存正式历史和待消费提醒
- 普通发送时构造 `AgentRequest`
- Agent 成功后一次性提交 `AgentResult.trajectory`
- 非成功结果转换为带停止原因、副作用和重试信息的 `ConversationException`
- 提供模式查询与切换入口，内部委托 Agent
- `/plan`、`/do` 不创建用户消息

现有简单文本发送入口继续只转发文本事件。

### 终端模块

`ConversationLoop` 不再理解 Agent 内部循环，只处理：

- 用户输入和命令
- `AgentEvent` 到 `TerminalUi` 的映射
- 模式切换提示
- 停止、失败和副作用警告
- 中断与退出

主要映射：

| AgentEvent | 终端行为 |
|---|---|
| TaskStarted / IterationStarted | Thinking 状态 |
| ThinkingDelta | 独立 Thinking 行 |
| TextDelta | 流式助手文本 |
| ModelToolRequested | Tool waiting |
| ToolExecutionChanged | 等待、运行、成功或失败 |
| ModelResponseCompleted | 显示本轮 Usage |
| ModeChanged | 显示 Plan/Do 当前模式 |
| TaskCompleted | Ready |
| TaskStopped | 显示轮数、超时或中断原因 |
| TaskFailed | 显示安全错误和可选 Retry-After |

### 三家 Provider

三家 Provider 只做两项小改动：

1. 根据 `ChatRequest.toolSelection()` 过滤工具 Schema。
2. 实现 `cancelActiveRequest()`，取消当前 future 和响应流，但不设置永久关闭状态。

Agent Loop 不感知 Provider 类型，也不改变现有 Thinking、Usage、工具解析和结果回传协议。

### 应用装配

`ImioCodeApplication` 的启动顺序调整为：

```text
加载 AppConfig
→ 创建六个工具和 ToolRegistry
→ 创建 LlmClient
→ 创建 Agent
→ 创建 ConversationSession
→ 创建终端与 ConversationLoop
```

关闭顺序仍由会话统一触发，重复关闭保持幂等。

## 模块交互

### 正常多步任务

```text
ConversationLoop
    │ 用户输入
    ▼
ConversationSession
    │ 历史 + 用户消息 + 提醒
    ▼
Agent.run
    │
    ├─ TaskStarted
    ├─ IterationStarted(1)
    │
    ▼
StreamingResponseCollector
    │ ChatRequest + ToolSelection
    ▼
LlmClient
    │ LlmEvent 持续返回
    ▼
StreamingResponseCollector
    │ 实时发布安全 AgentEvent
    │ 返回完整 ChatResponse
    ▼
Agent
    │ 追加助手消息
    │
    ├─ 无工具 ──→ TaskCompleted
    │
    └─ 有工具
         ▼
       ToolCallPartitioner
         ▼
       ToolBatchExecutor
         │ 并发安全批次 / 串行屏障
         ▼
       ToolExecution 列表
         │ 按原始顺序生成 ToolResult 消息
         ▼
       IterationStarted(2)
         └─ 再次调用模型，直到最终回答
```

Agent 返回成功结果后，`ConversationSession` 一次性提交整个轨迹。

### Plan Mode 切换

```text
用户输入 /plan
    ↓
ConversationLoop 识别命令
    ↓
ConversationSession.switchMode(PLAN)
    ↓
Agent.switchMode(PLAN)
    ↓
发布 ModeChanged(DO, PLAN)
```

后续任务开始时：

```text
AgentMode.PLAN
    ├─ ToolSelection.only(read_file, glob, grep)
    └─ reminders + 固定 Plan Mode 提醒
```

`/do` 使用相同流程切换回 `DO`，不复用或执行上一次计划文本。

### 工具顺序屏障

模型返回：

```text
read_file(A), grep(B), write_file(C), glob(D), read_file(E), bash(F)
```

分区结果：

```text
Batch 1 PARALLEL_SAFE: read_file(A), grep(B)
    ↓ 全部完成
Batch 2 SERIAL_BARRIER: write_file(C)
    ↓ 完成
Batch 3 PARALLEL_SAFE: glob(D), read_file(E)
    ↓ 全部完成
Batch 4 SERIAL_BARRIER: bash(F)
```

即使 `grep(B)` 比 `read_file(A)` 先结束，回传顺序仍是 A、B、C、D、E、F。

### 工具失败

```text
工具返回失败
    ↓
生成对应 ToolResultPart
    ↓
同批其他工具继续
    ↓
全部结果回传模型
    ↓
模型自行决定修正、换工具或输出最终回答
```

未知、禁用和 Plan Mode 禁止的工具也走相同失败结果路径，不会实际执行。

### 轮数耗尽

最大轮数检查发生在每次模型请求之前。第 20 轮响应若仍包含工具调用：

```text
不再执行第 20 轮请求的工具
→ TaskStopped(MAX_ITERATIONS)
→ 丢弃未完成轨迹
```

这样不会在已确定无法继续调用模型时产生新的工具副作用。

### 超时与中断竞争

```text
看门任务 / 用户中断
    ↓
ActiveTask.compareAndSet(stopReason)
    ↓
取消当前 LLM 请求
取消 ToolBatchExecutor
禁止新工具和新模型请求
    ↓
Agent 主循环读取唯一停止原因
    ↓
发布一次 TaskStopped
```

若最终回答与超时同时到达，由第一个成功写入终态的操作决定结果，另一路只执行清理，不重复发布事件或提交历史。

### 错误路径

```text
LLM / 协议错误
    ↓
转换 AgentError
    ↓
TaskFailed
    ↓
ConversationSession 不提交轨迹
    ↓
ConversationLoop 显示安全错误
```

若此前执行过 `MEDIUM/HIGH` 工具，`sideEffectsPossible=true`，终端追加“部分操作可能已经执行”的提示。

### 提醒生命周期

```text
任务开始：ConversationSession 复制并清空 pendingReminders
    ↓
AgentRequest 保存固定提醒快照
    ↓
每轮 ChatRequest 使用同一快照
    ↓
任务完成 / 停止 / 失败
    ↓
下一任务不再包含旧提醒
```

Plan Mode 固定提醒只存在于当前任务请求中，不写入正式历史。

## 文件组织

```text
src/main/java/io/imiocode/
├── agent/
│   ├── Agent.java
│   ├── AgentMode.java
│   ├── AgentStopReason.java
│   ├── AgentError.java
│   ├── AgentRequest.java
│   ├── AgentResult.java
│   ├── AgentEvent.java
│   ├── AgentEventListener.java
│   ├── AgentTaskContext.java
│   ├── StreamingResponseCollector.java
│   ├── ToolBatchKind.java
│   ├── IndexedToolCall.java
│   ├── ToolBatch.java
│   ├── ToolCallPartitioner.java
│   ├── ToolBatchExecutor.java
│   └── PlanModePrompt.java
├── config/
│   ├── AgentConfig.java
│   ├── AppConfig.java
│   ├── ConfigDocument.java
│   └── ConfigLoader.java
├── conversation/
│   ├── ChatRequest.java
│   ├── ConversationException.java
│   ├── ConversationListener.java
│   ├── ConversationSession.java
│   └── ConversationLoop.java
├── llm/
│   ├── LlmClient.java
│   └── provider/
│       ├── anthropic/AnthropicClient.java
│       ├── openai/OpenAiClient.java
│       └── deepseek/DeepSeekClient.java
├── tool/
│   ├── ToolSelection.java
│   ├── ToolRegistry.java
│   └── ToolExecutor.java
├── terminal/
│   ├── TerminalUi.java
│   └── JLineTerminalUi.java
└── ImioCodeApplication.java

src/test/java/io/imiocode/
├── agent/
│   ├── AgentTest.java
│   ├── AgentEventTest.java
│   ├── StreamingResponseCollectorTest.java
│   ├── ToolCallPartitionerTest.java
│   ├── ToolBatchExecutorTest.java
│   └── AgentCancellationTest.java
├── config/
│   ├── ConfigLoaderTest.java
│   └── YamlConfigLoaderTest.java
├── conversation/
│   ├── ConversationSessionTest.java
│   └── ConversationLoopTest.java
├── llm/
│   ├── LlmClientContractTest.java
│   └── provider/
│       ├── anthropic/AnthropicClientTest.java
│       ├── openai/OpenAiClientTest.java
│       └── deepseek/DeepSeekClientTest.java
├── tool/
│   ├── ToolRegistryTest.java
│   └── ToolExecutorTest.java
└── terminal/
    └── JLineTerminalUiTest.java

docs/ch4/
├── spec.md
├── plan.md
├── task.md
└── checklist.md

config.example.yaml
```

说明：

- `AgentTaskContext` 为包内任务状态，不暴露给会话和 UI。
- `PlanModePrompt` 只保存固定规划提醒文本和只读工具集合。
- 不新增 Provider 专属 Agent 类。
- 不删除 Ch3 的工具和消息模型；现有测试继续作为回归基线。
- `claude.md` 的本地未提交修改继续保持不动。

## 技术决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| Agent 执行模型 | 前台同步任务 | 保持当前终端交互简单；一次只处理一个用户任务 |
| 循环结构 | 显式 `while`，轮次从 1 开始 | 停止检查和事件顺序容易观察、测试 |
| 最大轮数语义 | 第 N 轮仍调用模型；若仍请求工具则停止，不执行该批工具 | 避免执行后已没有下一轮机会处理结果 |
| 总超时 | 单调时钟截止时间 + 看门任务 | 同时覆盖模型阻塞和工具阻塞，不受系统时间调整影响 |
| 模式状态 | Agent 持久保存，任务开始时冻结快照 | `/plan`、`/do` 持续生效，同时避免任务中途变化 |
| 工具可见性 | 请求级 `ToolSelection` | 不修改全局注册中心，不影响其他请求或 Provider |
| Plan Mode 防线 | Schema 过滤 + 执行前二次检查 | 即使模型伪造写工具调用也不会执行 |
| 安全工具判定 | 已允许、已启用且 `ToolRisk.LOW` | 复用现有风险元信息，不硬编码实现类 |
| 分批方式 | 连续安全调用并发，其他调用形成顺序屏障 | 保留模型给出的依赖顺序 |
| 并发实现 | 固定大小虚拟线程池 | 阻塞型文件工具适用，同时严格限制并发数 |
| 结果顺序 | 使用原始索引排序后回传 | 并发完成顺序不影响模型上下文 |
| 工具失败 | 转换为普通失败结果并继续循环 | 让模型根据失败信息调整策略 |
| 未知或禁止工具 | 不执行，生成失败结果 | 保持 Agent Loop 可恢复且满足 Plan Mode |
| 副作用标记 | 不安全工具开始执行前即设为可能存在 | 采用保守提示，避免错误声称没有副作用 |
| 流式响应 | Provider 负责协议组装，Collector 负责事件转发与完成校验 | 避免重复实现三家 JSON 聚合逻辑 |
| Agent 事件 | 同步发布、携带轮次和调用索引 | 保证事件生命周期顺序并简化终端消费 |
| 敏感模型事件 | 不转发签名、encrypted content 和工具参数碎片 | UI 不需要这些字段，减少泄露面 |
| 终态竞争 | 原子 CAS，完成、停止、失败互斥 | 防止超时与响应同时到达时重复终结 |
| 历史策略 | Agent 返回临时轨迹，会话成功后一次性提交 | 失败和中断不会污染正式上下文 |
| 工具结果消息 | 每个模型工具批次生成一条 TOOL 消息 | 保持现有三家 Provider 的历史编码方式 |
| 模型取消 | 新增仅取消当前请求的接口 | 超时后客户端仍可供下一任务使用 |
| 程序关闭 | 取消活动任务后永久关闭 Agent、工具和 LLM | 资源释放完整且重复关闭安全 |
| 错误重试 | 本章不自动重试 | 避免隐藏额外循环、费用和副作用 |
| Provider 适配 | Agent 层完全厂商无关 | 三家协议差异继续封装在现有客户端 |
| 配置单位 | YAML 和环境变量均使用秒及整数 | 与现有超时配置风格一致 |
| UI 事件失败 | 监听器异常立即停止当前任务并转成安全失败 | 避免在用户不可见的情况下继续产生副作用 |
| Java 并发能力 | Java 21 标准 API，不启用 Preview | 保持现有 Maven 构建方式不变 |

## 需求覆盖与架构自检

### Spec 覆盖

| 需求 | 负责模块 |
|---|---|
| F1–F5 | `Agent` 循环、`AgentRequest`、工具结果回传 |
| F6–F7 | `StreamingResponseCollector`、现有 Provider 流组装 |
| F8–F10 | `AgentTaskContext`、截止时间看门任务、取消链路 |
| F11–F13 | `AgentEvent`、`AgentEventListener`、终端事件映射 |
| F14–F18 | `ToolCallPartitioner`、`ToolBatchExecutor`、`ToolExecutor` |
| F19–F24 | `AgentMode`、`PlanModePrompt`、`ToolSelection`、命令处理 |
| F25–F26 | `AgentConfig`、`ConfigLoader`、YAML 和环境变量 |
| F27 | `ConversationSession` 提醒快照、Agent 每轮请求 |
| F28–F30 | `AgentResult` 临时轨迹、会话原子提交、副作用标记 |
| F31 | 厂商无关 Agent 接口、三家请求级工具过滤 |
| F32 | 全量 Ch2、Ch3 回归测试与进程 E2E |

### 依赖方向

```text
配置记录 ───────────────┐
消息领域模型 ───────────┼→ Agent 核心
LLM 接口与 Provider ────┤      │
工具注册与执行 ─────────┘      ▼
                         ConversationSession
                                  │
AgentEvent ───────────────────────┤
                                  ▼
                         ConversationLoop / UI
                                  │
                                  ▼
                         ImioCodeApplication
```

其中 `ChatMessage`、`ChatRequest` 等属于共享消息领域模型，不依赖 Agent；`ConversationSession` 才依赖 Agent，因此不存在类级循环依赖。Agent 不依赖会话、终端或应用启动类。

### 接口完整性检查

- Agent 输入、输出、模式切换、取消和关闭接口已定义。
- 五种停止原因及三种最终事件已定义。
- 流式收集器的输入、输出和错误边界已定义。
- 工具分区、批次、原始索引和执行接口已定义。
- 请求级工具过滤同时覆盖 Schema 导出和实际执行。
- 成功与失败历史事务边界已定义。
- Provider 当前请求取消与永久关闭语义已区分。

### 并发与资源检查

- 安全并发数固定且经过配置校验。
- 不安全工具形成屏障，不与其他批次重叠。
- 并发结果按索引恢复顺序。
- 每个任务独立创建并关闭工具执行器。
- 超时、中断、错误和关闭共用幂等取消链路。
- 终态使用原子竞争，禁止重复事件和重复提交。

### 与 Spec 的一致性

- 未引入权限确认、上下文压缩、自动重试或自动回滚。
- 未增加新工具或修改六个工具参数协议。
- Plan Mode 不会自动执行计划。
- Agent 层没有 Provider 分支。
- 未提前实现多 Agent、MCP、Skill 或自动 Git 操作。

技术设计覆盖全部 F1–F32，没有发现缺口或与 Spec 冲突的决策。
