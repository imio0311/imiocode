# ImioCode Ch4 Agent Loop 增强 Tasks

## 文件清单

| 操作 | 文件 | 职责 |
|---|---|---|
| 修改 | `src/main/java/io/imiocode/llm/LlmErrorType.java` | 增加统一输出上限错误类型 |
| 修改 | `src/main/java/io/imiocode/conversation/ChatRequest.java` | 增加请求级输出 token 上限并保留旧构造入口 |
| 修改 | `src/main/java/io/imiocode/tool/ToolRegistry.java` | 统一解析工具的存在、启用和模式允许状态 |
| 新建 | `src/main/java/io/imiocode/tool/ToolAvailability.java` | 定义四种工具可用状态 |
| 新建 | `src/main/java/io/imiocode/tool/ToolResolution.java` | 返回工具可用状态及可执行实例 |
| 新建 | `src/main/java/io/imiocode/agent/CircuitObservation.java` | 描述一次熔断观察结果 |
| 新建 | `src/main/java/io/imiocode/agent/UnknownToolCircuitBreaker.java` | 保存任务级连续未知工具计数和尝试快照 |
| 新建 | `src/main/java/io/imiocode/agent/UnknownToolCircuitOpenException.java` | 在内部中断已熔断的模型流 |
| 新建 | `src/main/java/io/imiocode/agent/RetryDecision.java` | 保存下一次重试参数 |
| 新建 | `src/main/java/io/imiocode/agent/LlmRetryPolicy.java` | 固定重试预算、退避和 token 翻倍策略 |
| 新建 | `src/main/java/io/imiocode/agent/RetryWaiter.java` | 定义可测试的重试等待接口 |
| 新建 | `src/main/java/io/imiocode/agent/DefaultRetryWaiter.java` | 实现响应取消和截止时间的生产等待 |
| 修改 | `src/main/java/io/imiocode/agent/StreamingResponseCollector.java` | 透传完整工具调用并区分模型尝试 |
| 新建 | `src/main/java/io/imiocode/agent/StreamingToolScheduler.java` | 执行连续 LOW 前缀并维护串行屏障 |
| 新建 | `src/main/java/io/imiocode/agent/StreamingTurnResult.java` | 返回单轮成功响应和有序工具结果 |
| 新建 | `src/main/java/io/imiocode/agent/StreamingTurnExecutor.java` | 编排单轮模型尝试、重试、熔断和工具调度 |
| 修改 | `src/main/java/io/imiocode/agent/AgentTaskContext.java` | 统一取消当前模型请求、等待和流式工具调度 |
| 修改 | `src/main/java/io/imiocode/agent/Agent.java` | 用单轮执行器驱动外层 ReAct 循环 |
| 修改 | `src/main/java/io/imiocode/agent/AgentEvent.java` | 增加安全的重试事件 |
| 修改 | `src/main/java/io/imiocode/agent/AgentStopReason.java` | 增加未知工具过多停止原因 |
| 修改 | `src/main/java/io/imiocode/conversation/ConversationException.java` | 映射新的停止原因及中文安全提示 |
| 修改 | `src/main/java/io/imiocode/conversation/ConversationLoop.java` | 消费重试事件并驱动终端分隔显示 |
| 修改 | `src/main/java/io/imiocode/terminal/TerminalUi.java` | 定义重试提示接口 |
| 修改 | `src/main/java/io/imiocode/terminal/JLineTerminalUi.java` | 在富模式和 dumb 模式显示重试与熔断 |
| 修改 | `src/main/java/io/imiocode/llm/LlmStreamAssembler.java` | 保持完整工具调用索引和单次完成约束 |
| 修改 | `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java` | 请求级 token 覆盖及 OpenAI 输出上限归一 |
| 修改 | `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicClient.java` | 请求级 token 覆盖及 Anthropic 输出上限归一 |
| 修改 | `src/main/java/io/imiocode/llm/provider/deepseek/DeepSeekClient.java` | 请求级 token 覆盖及 DeepSeek 输出上限归一 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 注入配置中的初始输出 token 上限 |
| 新建 | `src/test/java/io/imiocode/agent/LlmRetryPolicyTest.java` | 覆盖错误分类、退避、预算和 token 上限 |
| 新建 | `src/test/java/io/imiocode/agent/UnknownToolCircuitBreakerTest.java` | 覆盖计数、清零、保持和尝试提交 |
| 新建 | `src/test/java/io/imiocode/agent/StreamingToolSchedulerTest.java` | 覆盖提前执行、屏障、乱序、至多一次和取消 |
| 新建 | `src/test/java/io/imiocode/agent/StreamingTurnExecutorTest.java` | 覆盖重试、熔断和单轮结果收集 |
| 修改 | `src/test/java/io/imiocode/agent/StreamingResponseCollectorTest.java` | 覆盖尝试编号和完整工具回调 |
| 修改 | `src/test/java/io/imiocode/agent/AgentEventTest.java` | 覆盖新事件和停止原因约束 |
| 修改 | `src/test/java/io/imiocode/agent/AgentTest.java` | 覆盖增强后的多轮、最大轮数和历史轨迹 |
| 修改 | `src/test/java/io/imiocode/agent/AgentCancellationTest.java` | 覆盖等待、模型流和活动工具取消竞态 |
| 修改 | `src/test/java/io/imiocode/tool/ToolRegistryTest.java` | 覆盖四种工具解析状态 |
| 修改 | `src/test/java/io/imiocode/llm/LlmStreamAssemblerTest.java` | 覆盖工具索引、重复完成和流完成约束 |
| 修改 | `src/test/java/io/imiocode/llm/provider/openai/OpenAiClientTest.java` | 覆盖 OpenAI token 请求字段和结束信号 |
| 修改 | `src/test/java/io/imiocode/llm/provider/anthropic/AnthropicClientTest.java` | 覆盖 Anthropic token 请求字段和结束信号 |
| 修改 | `src/test/java/io/imiocode/llm/provider/deepseek/DeepSeekClientTest.java` | 覆盖 DeepSeek token 请求字段和结束信号 |
| 修改 | `src/test/java/io/imiocode/conversation/ConversationSessionTest.java` | 覆盖失败尝试不提交及熔断事务语义 |
| 修改 | `src/test/java/io/imiocode/conversation/ConversationLoopTest.java` | 覆盖重试事件到 UI 的转换 |
| 修改 | `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java` | 覆盖富模式和 dumb 模式文案及换行 |

现有 `ToolBatchExecutor`、`ToolCallPartitioner`、`ToolExecutor` 及其测试继续保留，用作调度规则复用和 Ch3/原 Ch4 回归基线。

## T1：扩展统一错误和请求模型

**文件：**

- `src/main/java/io/imiocode/llm/LlmErrorType.java`
- `src/main/java/io/imiocode/conversation/ChatRequest.java`
- `src/test/java/io/imiocode/llm/LlmClientContractTest.java`

**依赖：** 无

**步骤：**

1. 在统一错误类型中增加 `OUTPUT_LIMIT`。
2. 为 `ChatRequest` 增加 `OptionalInt outputTokenLimit`，校验存在时必须为正数。
3. 保留原有一、二、三参数构造入口，使旧调用默认不覆盖 Provider 配置。
4. 增加请求不可变性、默认空覆盖和非法 token 上限测试。

**验证：** 运行 `mvn -q -Dtest=LlmClientContractTest test`，期望请求兼容性测试全部通过。

## T2：实现工具可用状态解析

**文件：**

- `src/main/java/io/imiocode/tool/ToolAvailability.java`
- `src/main/java/io/imiocode/tool/ToolResolution.java`
- `src/main/java/io/imiocode/tool/ToolRegistry.java`
- `src/test/java/io/imiocode/tool/ToolRegistryTest.java`
- `src/test/java/io/imiocode/tool/ToolExecutorTest.java`

**依赖：** T1

**步骤：**

1. 定义 `AVAILABLE`、`UNKNOWN`、`DISABLED`、`DISALLOWED` 四种状态。
2. 在注册中心增加 `resolve(name, selection)`，先判断是否注册，再判断启用状态和模式限制。
3. 仅在 `AVAILABLE` 时返回工具实例；其他状态不泄露可执行实例。
4. 让原有 `findEnabled` 和执行器行为保持兼容。
5. 覆盖不存在、禁用、Plan Mode 禁止、正常可用四种测试场景。

**验证：** 运行 `mvn -q -Dtest=ToolRegistryTest,ToolExecutorTest test`，期望解析状态正确且旧工具执行测试无回归。

## T3：实现未知工具熔断器

**文件：**

- `src/main/java/io/imiocode/agent/CircuitObservation.java`
- `src/main/java/io/imiocode/agent/UnknownToolCircuitBreaker.java`
- `src/main/java/io/imiocode/agent/UnknownToolCircuitOpenException.java`
- `src/test/java/io/imiocode/agent/UnknownToolCircuitBreakerTest.java`

**依赖：** T2

**步骤：**

1. 实现任务级已提交连续计数和 `beginAttempt()` 临时快照。
2. UNKNOWN 增加计数，AVAILABLE 清零，DISABLED 与 DISALLOWED 保持不变。
3. 第三次连续 UNKNOWN 返回 `OPENED`，之后状态保持打开。
4. `commit()` 仅允许一次，并把成功尝试的临时计数提交到任务状态。
5. 未提交快照直接丢弃，验证失败尝试不会污染后续重试。

**验证：** 运行 `mvn -q -Dtest=UnknownToolCircuitBreakerTest test`，期望所有计数、清零、保持、提交和丢弃场景通过。

## T4：实现固定模型重试策略

**文件：**

- `src/main/java/io/imiocode/agent/RetryDecision.java`
- `src/main/java/io/imiocode/agent/LlmRetryPolicy.java`
- `src/test/java/io/imiocode/agent/LlmRetryPolicyTest.java`

**依赖：** T1

**步骤：**

1. 实现初次请求后最多三次重试的固定预算。
2. RATE_LIMIT 优先使用 Retry-After，否则按 1、2、4 秒退避。
3. NETWORK、SERVER_ERROR、TIMEOUT 按 1、2、4 秒退避。
4. OUTPUT_LIMIT 以零延迟翻倍当前请求上限，最高 64000。
5. 工具已启动、剩余时间不足、预算耗尽和不可恢复错误返回空决策。
6. 验证 64000 时不产生相同上限的无效重试。

**验证：** 运行 `mvn -q -Dtest=LlmRetryPolicyTest test`，期望错误矩阵、三次预算、Retry-After 和 token 上限全部通过。

## T5：实现可取消的重试等待

**文件：**

- `src/main/java/io/imiocode/agent/RetryWaiter.java`
- `src/main/java/io/imiocode/agent/DefaultRetryWaiter.java`
- `src/main/java/io/imiocode/agent/AgentTaskContext.java`
- `src/test/java/io/imiocode/agent/AgentCancellationTest.java`

**依赖：** T4

**步骤：**

1. 在任务上下文暴露剩余时间和停止状态查询。
2. 把当前工具执行器引用改为可替换的取消动作，使每次模型尝试都能挂接当前调度器。
3. 实现分段、可中断且受任务截止时间约束的等待。
4. 停止请求同时取消当前 LLM 请求、等待和活动工具。
5. 使用短虚拟等待或可控制线程验证取消与超时，不在测试中真实等待 1、2、4 秒。

**验证：** 运行 `mvn -q -Dtest=AgentCancellationTest test`，期望等待期间取消和超时均快速结束且无遗留活动线程。

## T6：扩展流式收集器的完整工具回调

**文件：**

- `src/main/java/io/imiocode/agent/StreamingResponseCollector.java`
- `src/main/java/io/imiocode/llm/LlmStreamAssembler.java`
- `src/test/java/io/imiocode/agent/StreamingResponseCollectorTest.java`
- `src/test/java/io/imiocode/llm/LlmStreamAssemblerTest.java`

**依赖：** T1

**步骤：**

1. 给收集入口增加 `attempt` 和完整工具调用回调。
2. 收到 `LlmEvent.ToolCallCompleted` 时携带 Provider 原始索引转交调度层。
3. 继续实时发布文本、Thinking、工具请求和 Usage，不发布参数碎片或 Provider 原始元数据。
4. 保持每次模型尝试只能有一个完成事件，重复索引或重复完成按协议错误处理。
5. 给旧收集入口保留兼容委托，降低现有调用方改动范围。

**验证：** 运行 `mvn -q -Dtest=StreamingResponseCollectorTest,LlmStreamAssemblerTest test`，期望完整工具回调索引正确，旧流事件测试继续通过。

## T7：实现流式工具调度的 LOW 前缀

**文件：**

- `src/main/java/io/imiocode/agent/StreamingToolScheduler.java`
- `src/test/java/io/imiocode/agent/StreamingToolSchedulerTest.java`

**依赖：** T2、T3、T6

**步骤：**

1. 按原始索引缓存完整工具调用，并记录已观察、已生成结果和已启动索引。
2. 仅从索引 0 开始推进连续的 AVAILABLE + LOW 前缀。
3. 使用固定并发上限执行 LOW 工具，并转发原有工具生命周期事件。
4. UNKNOWN、DISABLED、DISALLOWED 生成确定的失败结果但不执行。
5. 对重复索引抛出协议错误，保证同一索引至多处理一次。
6. 覆盖两个 LOW 工具在流未结束时启动且并发数不越界的场景。

**验证：** 运行 `mvn -q -Dtest=StreamingToolSchedulerTest#startsOnlyContinuousLowPrefix test`，期望连续 LOW 前缀提前启动且无重复执行。

## T8：完成写工具屏障、乱序收集和取消

**文件：**

- `src/main/java/io/imiocode/agent/StreamingToolScheduler.java`
- `src/main/java/io/imiocode/agent/ToolCallPartitioner.java`
- `src/main/java/io/imiocode/agent/ToolBatchExecutor.java`
- `src/test/java/io/imiocode/agent/StreamingToolSchedulerTest.java`
- `src/test/java/io/imiocode/agent/ToolCallPartitionerTest.java`
- `src/test/java/io/imiocode/agent/ToolBatchExecutorTest.java`

**依赖：** T7

**步骤：**

1. 遇到 MEDIUM/HIGH 工具后停止提前推进，并阻止后续 LOW 越过屏障。
2. 流成功后等待前置 LOW 完成，串行执行非 LOW，再开放下一 LOW 批次。
3. 对乱序和索引缺口保守缓存；流结束后按完整索引排序处理。
4. `awaitResults()` 按原始索引返回，不按完成时间返回。
5. `cancel()` 停止活动 LOW、禁止排队工具启动并关闭线程池。
6. 复用现有批次划分规则，保持旧批次执行器行为和测试不变。

**验证：** 运行 `mvn -q -Dtest=StreamingToolSchedulerTest,ToolCallPartitionerTest,ToolBatchExecutorTest test`，期望 LOW→写→LOW、乱序、取消和旧 Ch4 分批测试全部通过。

## T9：实现单轮重试编排

**文件：**

- `src/main/java/io/imiocode/agent/StreamingTurnResult.java`
- `src/main/java/io/imiocode/agent/StreamingTurnExecutor.java`
- `src/test/java/io/imiocode/agent/StreamingTurnExecutorTest.java`

**依赖：** T4、T5、T6、T8

**步骤：**

1. 每次尝试创建独立熔断快照和流式工具调度器。
2. 成功时提交快照、开放流结束屏障并返回完整响应和有序工具结果。
3. 失败时取消调度器，只有尚未启动工具且策略允许时才重试。
4. 重试前发布安全的重试事件并调用可注入等待器。
5. 每次重试只替换请求级 token 上限，不修改初始配置或下一个 Agent 轮次。
6. 保证失败尝试的响应不会成为下一次请求消息。

**验证：** 运行 `mvn -q -Dtest=StreamingTurnExecutorTest#retriesRecoverableFailureBeforeAnyToolStarts test`，期望网络失败后按计划重试且仅成功尝试产生结果。

## T10：完成单轮熔断和工具启动失败边界

**文件：**

- `src/main/java/io/imiocode/agent/StreamingTurnExecutor.java`
- `src/main/java/io/imiocode/agent/StreamingToolScheduler.java`
- `src/test/java/io/imiocode/agent/StreamingTurnExecutorTest.java`
- `src/test/java/io/imiocode/agent/StreamingToolSchedulerTest.java`

**依赖：** T9

**步骤：**

1. 第三个连续 UNKNOWN 立即取消当前模型请求和活动 LOW 工具。
2. 将内部熔断异常与普通模型错误区分，禁止下一次模型尝试。
3. 任一工具开始后模型流失败时禁止重试并取消活动工具。
4. `toolsMayExecute=false` 时只收集调用，不提前执行也不在流结束后补执行。
5. 覆盖同一响应后续工具不启动、跨轮计数和重试快照丢弃场景。

**验证：** 运行 `mvn -q -Dtest=StreamingTurnExecutorTest,StreamingToolSchedulerTest test`，期望熔断、工具后失败和最大轮数保护场景全部通过。

## T11：归一 OpenAI 输出上限

**文件：**

- `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java`
- `src/test/java/io/imiocode/llm/provider/openai/OpenAiClientTest.java`
- `src/test/java/io/imiocode/llm/provider/openai/OpenAiRichEventTest.java`

**依赖：** T1、T6

**步骤：**

1. 请求优先使用 `ChatRequest.outputTokenLimit`，为空时使用配置的 `maxOutputTokens`。
2. 解析 `response.incomplete` 和 `incomplete_details.reason`。
3. 达到输出上限时抛出安全的 `OUTPUT_LIMIT`，不构造正常响应完成事件。
4. 其他 incomplete、failed 和 error 保持协议或已归一错误行为。
5. 验证请求 JSON、正常文本、Thinking、工具、Usage 和取消行为无回归。

**验证：** 运行 `mvn -q -Dtest=OpenAiClientTest,OpenAiRichEventTest test`，期望 token 覆盖和输出上限归一通过，OpenAI 富事件测试无回归。

## T12：归一 Anthropic 输出上限

**文件：**

- `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicClient.java`
- `src/test/java/io/imiocode/llm/provider/anthropic/AnthropicClientTest.java`
- `src/test/java/io/imiocode/llm/provider/anthropic/AnthropicRichEventTest.java`

**依赖：** T1、T6

**步骤：**

1. 请求优先使用 `ChatRequest.outputTokenLimit` 写入 `max_tokens`。
2. 在消息停止事件中把 `stop_reason=max_tokens` 映射为 `OUTPUT_LIMIT`。
3. 输出上限时不发布正常完成事件。
4. 保持 adaptive thinking、固定 thinking、工具调用、Usage 和取消行为不变。

**验证：** 运行 `mvn -q -Dtest=AnthropicClientTest,AnthropicRichEventTest test`，期望输出上限测试和 Anthropic 原有流式回归全部通过。

## T13：归一 DeepSeek 输出上限

**文件：**

- `src/main/java/io/imiocode/llm/provider/deepseek/DeepSeekClient.java`
- `src/test/java/io/imiocode/llm/provider/deepseek/DeepSeekClientTest.java`
- `src/test/java/io/imiocode/llm/provider/deepseek/DeepSeekRichEventTest.java`

**依赖：** T1、T6

**步骤：**

1. 请求优先使用 `ChatRequest.outputTokenLimit` 写入 `max_tokens`。
2. 把 `finish_reason=length` 映射为 `OUTPUT_LIMIT`。
3. `stop` 和 `tool_calls` 继续正常完成，其他结束原因继续按协议错误处理。
4. 保持 reasoning、工具调用、Usage 和取消行为不变。

**验证：** 运行 `mvn -q -Dtest=DeepSeekClientTest,DeepSeekRichEventTest test`，期望 length 归一和 DeepSeek 原有富事件测试全部通过。

## T14：扩展 Agent 事件与停止模型

**文件：**

- `src/main/java/io/imiocode/agent/AgentEvent.java`
- `src/main/java/io/imiocode/agent/AgentStopReason.java`
- `src/main/java/io/imiocode/agent/AgentResult.java`
- `src/test/java/io/imiocode/agent/AgentEventTest.java`

**依赖：** T4

**步骤：**

1. 增加 `RetryScheduled`，校验轮次、下一尝试、错误类型、等待和 token 上限。
2. 事件只携带安全枚举和数值，不包含 Provider 原始响应。
3. 增加 `TOO_MANY_UNKNOWN_TOOLS` 停止原因，并允许其生成非错误停止结果。
4. 保持 Completed、Stopped、Failed 三种终态约束互斥。

**验证：** 运行 `mvn -q -Dtest=AgentEventTest test`，期望新事件字段约束和终态约束全部通过。

## T15：将单轮执行器接入 Agent Loop

**文件：**

- `src/main/java/io/imiocode/agent/Agent.java`
- `src/main/java/io/imiocode/agent/AgentTaskContext.java`
- `src/main/java/io/imiocode/agent/StreamingTurnExecutor.java`
- `src/test/java/io/imiocode/agent/AgentTest.java`
- `src/test/java/io/imiocode/agent/AgentCancellationTest.java`

**依赖：** T10、T14

**步骤：**

1. Agent 构造时创建或注入单轮执行器和初始输出 token 上限，并保留旧构造入口。
2. 每个用户任务创建一个未知工具熔断器，各轮复用该任务级实例。
3. 每轮调用单轮执行器，成功后再把助手响应和工具结果加入临时轨迹。
4. 最大轮数末轮传入 `toolsMayExecute=false`，仍显示模型响应但不执行工具。
5. 将熔断转换成唯一 `TaskStopped(TOO_MANY_UNKNOWN_TOOLS)`。
6. 继续由任务上下文仲裁完成、错误、超时、取消和熔断竞态。

**验证：** 运行 `mvn -q -Dtest=AgentTest,AgentCancellationTest test`，期望多轮、最大轮数、熔断、超时和取消测试全部通过。

## T16：保持会话事务语义

**文件：**

- `src/main/java/io/imiocode/conversation/ConversationException.java`
- `src/main/java/io/imiocode/conversation/ConversationSession.java`
- `src/test/java/io/imiocode/conversation/ConversationSessionTest.java`

**依赖：** T15

**步骤：**

1. 为未知工具熔断生成独立中文安全提示和停止原因。
2. 正常完成时仍一次性提交用户、成功助手响应和工具结果。
3. 重试中的半截输出、熔断、耗尽、超时和取消均不提交临时轨迹。
4. 已执行工具和可能副作用标志继续准确透传。

**验证：** 运行 `mvn -q -Dtest=ConversationSessionTest test`，期望成功事务提交和所有失败/停止回滚场景通过。

## T17：扩展终端的重试与熔断显示

**文件：**

- `src/main/java/io/imiocode/terminal/TerminalUi.java`
- `src/main/java/io/imiocode/terminal/JLineTerminalUi.java`
- `src/main/java/io/imiocode/conversation/ConversationLoop.java`
- `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java`
- `src/test/java/io/imiocode/conversation/ConversationLoopTest.java`

**依赖：** T14、T16

**步骤：**

1. 给终端接口增加显示重试提示的方法。
2. 收到重试事件时结束开放的 Thinking 和助手行，再显示尝试次数、原因与等待时间。
3. 下一次文本增量从新助手行开始，不清除失败尝试内容。
4. 为未知工具熔断显示明确中文原因。
5. 在 ANSI 富模式和 dumb 模式分别验证换行、分隔和无敏感原文。

**验证：** 运行 `mvn -q -Dtest=JLineTerminalUiTest,ConversationLoopTest test`，期望两种终端模式下重试与熔断文案清晰且状态转换正确。

## T18：接入应用初始 token 配置

**文件：**

- `src/main/java/io/imiocode/ImioCodeApplication.java`
- `src/test/java/io/imiocode/config/ConfigLoaderTest.java`
- `src/test/java/io/imiocode/config/YamlConfigLoaderTest.java`

**依赖：** T15

**步骤：**

1. 创建 Agent 时注入 `AppConfig.maxOutputTokens()` 作为每轮初始值。
2. 不增加 YAML、环境变量或命令行配置字段。
3. 保证旧配置仍按原默认值启动，重试覆盖不写回 `AppConfig`。

**验证：** 运行 `mvn -q -Dtest=ConfigLoaderTest,YamlConfigLoaderTest,AgentTest test`，期望旧配置兼容且 Agent 初始 token 上限正确。

## T19：补齐并发竞态与至多一次回归

**文件：**

- `src/test/java/io/imiocode/agent/StreamingToolSchedulerTest.java`
- `src/test/java/io/imiocode/agent/StreamingTurnExecutorTest.java`
- `src/test/java/io/imiocode/agent/AgentCancellationTest.java`
- `src/test/java/io/imiocode/conversation/ConversationSessionTest.java`

**依赖：** T16、T18

**步骤：**

1. 用闩锁构造“模型流失败与 LOW 启动”竞争，验证工具至多一次且不错误重试。
2. 构造“工具完成与熔断”竞争，验证第三个未知工具后无后续工具启动。
3. 构造“取消与流失败”竞争，验证只发布一个终态。
4. 检查测试结束后无活动调度器、等待任务或工具 future。
5. 重复执行竞态测试，确认结果确定而非依赖时序运气。

**验证：** 连续运行三次 `mvn -q -Dtest=StreamingToolSchedulerTest,StreamingTurnExecutorTest,AgentCancellationTest,ConversationSessionTest test`，期望三次均通过且无挂起。

## T20：执行完整编译、测试和打包

**文件：**

- `pom.xml`
- `src/main/java/io/imiocode/**`
- `src/test/java/io/imiocode/**`

**依赖：** T11–T19

**步骤：**

1. 运行全量单元与集成测试，记录测试总数、失败数和跳过数。
2. 运行打包，确认 Java 21 编译和 shaded JAR 生成成功。
3. 检查工作树，只保留本章文件，不覆盖或提交用户的无关修改。
4. 检查 diff 中没有 API Key、认证头、Provider 原始响应或调试输出。

**验证：**

- 运行 `mvn -q test`，期望零失败、零错误。
- 运行 `mvn -q package`，期望生成 `target/imiocode-0.2.0-SNAPSHOT-all.jar`。
- 运行 `git diff --check`，期望无空白错误。

## T21：按 checklist 执行 tmux 端到端验收

**文件：**

- `docs/ch4-enhancement/checklist.md`

**依赖：** T20、checklist.md 已批准

**步骤：**

1. 检测本机、WSL 或可用类 Unix 环境中的 tmux。
2. 在 tmux 会话中启动打包后的 ImioCode。
3. 输入真实对话请求，观察 LOW 工具提前执行、正常多轮工具调用和最终回复。
4. 使用可控制的模拟 Provider 场景观察重试提示与未知工具熔断。
5. 对照 checklist 逐项记录实际命令、输出和通过/未通过状态。
6. 若 tmux、真实 API 或环境不可用，记录实际探测命令和错误，不伪造通过。

**验证：** tmux 会话中程序可正常启动、交互和退出，验收报告包含重试、提前 LOW、熔断及正常多轮的真实证据；若被环境阻塞，报告中包含可复现的阻塞证据。

## 执行顺序

```text
T1 ─┬─→ T2 → T3 ───────────────┐
    ├─→ T4 → T5 ────────────┐  │
    └─→ T6 ──────────────┐  │  │
                         ▼  ▼  ▼
                        T7 → T8 → T9 → T10
                                         │
T11 ← T1 + T6                             │
T12 ← T1 + T6                             ├→ T15 → T16 → T17
T13 ← T1 + T6                             │          └→ T19
T14 ← T4 ─────────────────────────────────┘
                                               T18 ───┘
T11 + T12 + T13 + T17 + T19 → T20 → T21
```

## 需求覆盖

| 需求 | 对应任务 |
|---|---|
| F1–F5 | T2、T6–T10 |
| F6–F13 | T1、T4–T5、T9–T13、T18 |
| F14–F17 | T2–T3、T7、T10、T14–T17 |
| F18 | T8–T10、T15 |
| F19 | T11–T13 |
| F20 | T5、T8、T10、T15、T18–T20 |
| N1–N4 | T7–T10、T19 |
| N5–N7 | T4–T5、T9–T10、T19 |
| N8–N10 | T14–T17、T19 |
| N11–N13 | T1–T2、T11–T13、T18、T20 |
| N14–N15 | T4–T5、T7、T9–T10、T19 |
| N16–N17 | T17、T20 |
| N18 | T21 |
