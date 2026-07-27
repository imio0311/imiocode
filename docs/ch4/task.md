# ImioCode 第四章：Agent Loop Tasks

## 文件清单

### 新建主源码

| 操作 | 文件 | 职责 |
|---|---|---|
| 新建 | `src/main/java/io/imiocode/config/AgentConfig.java` | Agent 轮数、超时和并发配置 |
| 新建 | `src/main/java/io/imiocode/tool/ToolSelection.java` | 请求级工具选择快照 |
| 新建 | `src/main/java/io/imiocode/agent/Agent.java` | ReAct Agent Loop 主入口 |
| 新建 | `src/main/java/io/imiocode/agent/AgentMode.java` | DO/PLAN 模式 |
| 新建 | `src/main/java/io/imiocode/agent/AgentStopReason.java` | 五种停止原因 |
| 新建 | `src/main/java/io/imiocode/agent/AgentError.java` | 安全错误元信息 |
| 新建 | `src/main/java/io/imiocode/agent/AgentRequest.java` | Agent 任务输入 |
| 新建 | `src/main/java/io/imiocode/agent/AgentResult.java` | Agent 任务结果与临时轨迹 |
| 新建 | `src/main/java/io/imiocode/agent/AgentEvent.java` | UI 无关的统一 Agent 事件 |
| 新建 | `src/main/java/io/imiocode/agent/AgentEventListener.java` | Agent 事件监听器 |
| 新建 | `src/main/java/io/imiocode/agent/AgentTaskContext.java` | 单任务截止时间、取消和副作用状态 |
| 新建 | `src/main/java/io/imiocode/agent/StreamingResponseCollector.java` | LLM 流事件透传与完整响应收集 |
| 新建 | `src/main/java/io/imiocode/agent/ToolBatchKind.java` | 安全并发/串行屏障类型 |
| 新建 | `src/main/java/io/imiocode/agent/IndexedToolCall.java` | 工具调用原始索引 |
| 新建 | `src/main/java/io/imiocode/agent/ToolBatch.java` | 连续工具批次 |
| 新建 | `src/main/java/io/imiocode/agent/ToolCallPartitioner.java` | `partitionToolCalls` 顺序分区 |
| 新建 | `src/main/java/io/imiocode/agent/ToolBatchExecutor.java` | 有界并发和顺序屏障执行 |
| 新建 | `src/main/java/io/imiocode/agent/PlanModePrompt.java` | Plan Mode 提醒及只读工具集合 |

### 修改主源码

| 操作 | 文件 | 职责 |
|---|---|---|
| 修改 | `config.example.yaml` | 增加默认 Agent 配置示例 |
| 修改 | `src/main/java/io/imiocode/config/AppConfig.java` | 持有 `AgentConfig` |
| 修改 | `src/main/java/io/imiocode/config/ConfigDocument.java` | 解析 `agent` YAML 节点 |
| 修改 | `src/main/java/io/imiocode/config/ConfigLoader.java` | 合并默认值、YAML 和环境变量 |
| 修改 | `src/main/java/io/imiocode/conversation/ChatRequest.java` | 携带 `ToolSelection` |
| 修改 | `src/main/java/io/imiocode/conversation/ConversationException.java` | 携带 Agent 停止原因 |
| 修改 | `src/main/java/io/imiocode/conversation/ConversationListener.java` | 接收 `AgentEvent` |
| 修改 | `src/main/java/io/imiocode/conversation/ConversationSession.java` | 委托 Agent 并原子提交轨迹 |
| 修改 | `src/main/java/io/imiocode/conversation/ConversationLoop.java` | `/plan`、`/do` 和 Agent 事件路由 |
| 修改 | `src/main/java/io/imiocode/llm/LlmClient.java` | 完成事件兼容桥和当前请求取消 |
| 修改 | `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicClient.java` | 工具过滤和可恢复取消 |
| 修改 | `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java` | 工具过滤和可恢复取消 |
| 修改 | `src/main/java/io/imiocode/llm/provider/deepseek/DeepSeekClient.java` | 工具过滤和可恢复取消 |
| 修改 | `src/main/java/io/imiocode/tool/ToolRegistry.java` | 按工具选择查询和导出 |
| 修改 | `src/main/java/io/imiocode/tool/ToolExecutor.java` | 单工具执行、多个活动工具和批量取消 |
| 修改 | `src/main/java/io/imiocode/terminal/TerminalUi.java` | 模式和 Agent 停止展示接口 |
| 修改 | `src/main/java/io/imiocode/terminal/JLineTerminalUi.java` | Plan/Do 和停止原因展示 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 装配 Agent |

### 新建测试

| 操作 | 文件 | 职责 |
|---|---|---|
| 新建 | `src/test/java/io/imiocode/agent/AgentTest.java` | 多轮循环、轨迹和停止条件 |
| 新建 | `src/test/java/io/imiocode/agent/AgentEventTest.java` | 事件约束和敏感字段边界 |
| 新建 | `src/test/java/io/imiocode/agent/StreamingResponseCollectorTest.java` | 流透传和完成校验 |
| 新建 | `src/test/java/io/imiocode/agent/ToolCallPartitionerTest.java` | 顺序批次划分 |
| 新建 | `src/test/java/io/imiocode/agent/ToolBatchExecutorTest.java` | 并发、屏障、顺序和失败 |
| 新建 | `src/test/java/io/imiocode/agent/AgentCancellationTest.java` | 超时、中断、终态竞争和资源释放 |

### 修改测试

| 操作 | 文件 | 职责 |
|---|---|---|
| 修改 | `src/test/java/io/imiocode/config/ConfigLoaderTest.java` | Agent 环境变量与默认值 |
| 修改 | `src/test/java/io/imiocode/config/YamlConfigLoaderTest.java` | Agent YAML 和非法字段 |
| 修改 | `src/test/java/io/imiocode/conversation/ConversationSessionTest.java` | Agent 轨迹、提醒和模式事务 |
| 修改 | `src/test/java/io/imiocode/conversation/ConversationLoopTest.java` | 命令、事件和进程 E2E |
| 修改 | `src/test/java/io/imiocode/llm/LlmClientContractTest.java` | 完成事件兼容桥和取消契约 |
| 修改 | `src/test/java/io/imiocode/llm/provider/anthropic/AnthropicClientTest.java` | 过滤和当前请求取消 |
| 修改 | `src/test/java/io/imiocode/llm/provider/openai/OpenAiClientTest.java` | 过滤和当前请求取消 |
| 修改 | `src/test/java/io/imiocode/llm/provider/deepseek/DeepSeekClientTest.java` | 过滤和当前请求取消 |
| 修改 | `src/test/java/io/imiocode/tool/ToolRegistryTest.java` | `ToolSelection` 查询与导出 |
| 修改 | `src/test/java/io/imiocode/tool/ToolExecutorTest.java` | 并发活动工具与取消 |
| 修改 | `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java` | Plan/Do 和停止展示 |

### 文档

| 操作 | 文件 | 职责 |
|---|---|---|
| 已新建 | `docs/ch4/spec.md` | 已批准的需求 |
| 已新建 | `docs/ch4/plan.md` | 已批准的技术设计 |
| 新建 | `docs/ch4/task.md` | 实现任务 |
| 新建 | `docs/ch4/checklist.md` | 行为验收 |

## T1：定义 AgentConfig

**文件：** `src/main/java/io/imiocode/config/AgentConfig.java`  
**依赖：** 无

**步骤：**
1. 定义 `maxIterations`、`taskTimeout`、`maxParallelTools` 三个字段。
2. 校验轮数、时长和并发数均为正数。
3. 提供 20 轮、600 秒、4 并发的默认工厂。

**验证：** `mvn -q -DskipTests compile`，期望主源码编译通过。

## T2：扩展配置文档与 AppConfig

**文件：** `AppConfig.java`、`ConfigDocument.java`  
**依赖：** T1

**步骤：**
1. 在 `AppConfig` 增加非空 `AgentConfig` 字段。
2. 保留旧构造函数并自动填入默认 Agent 配置。
3. 在 `ConfigDocument` 增加 `AgentDocument`，映射三个 kebab-case YAML 字段。
4. 更新安全 `toString()`，不打印 Key。

**验证：** `mvn -q -DskipTests compile`，现有配置调用方继续编译。

## T3：加载 Agent 默认值、YAML 与环境变量

**文件：** `ConfigLoader.java`  
**依赖：** T2

**步骤：**
1. 合并 `agent.max-iterations`、`agent.timeout-seconds`、`agent.max-parallel-tools`。
2. 增加三个 `IMIO_AGENT_*` 环境变量，保持逐字段覆盖。
3. 对零值、负值、非数字和溢出提供带配置项名称的安全错误。
4. 将最终值构造成 `AgentConfig`。

**验证：** `mvn -q -Dtest=ConfigLoaderTest,YamlConfigLoaderTest test`，期望现有测试先保持通过。

## T4：覆盖 Agent 配置并更新示例

**文件：** `ConfigLoaderTest.java`、`YamlConfigLoaderTest.java`、`config.example.yaml`  
**依赖：** T3

**步骤：**
1. 测试旧配置得到 20、600、4。
2. 测试 YAML 完整配置及环境变量局部覆盖。
3. 测试三类非法值和错误信息不泄露 Key。
4. 在示例 YAML 中增加默认 Agent 节点。

**验证：** `mvn -q -Dtest=ConfigLoaderTest,YamlConfigLoaderTest test` 全部通过。

## T5：定义请求级 ToolSelection

**文件：** `src/main/java/io/imiocode/tool/ToolSelection.java`  
**依赖：** 无

**步骤：**
1. 定义 unrestricted 与允许名称集合。
2. 提供 `allEnabled()`、`only()` 和 `allows()`。
3. 对输入集合做不可变复制并校验空白名称。

**验证：** `mvn -q -DskipTests compile`。

## T6：让 ToolRegistry 支持过滤

**文件：** `ToolRegistry.java`  
**依赖：** T5

**步骤：**
1. 增加当前已启用名称的不可变快照。
2. 增加按 `ToolSelection` 查询已启用工具的入口。
3. 增加按选择导出定义的重载，保留旧导出行为。
4. 确保禁用工具永远不会因 selection 再次可见。

**验证：** `mvn -q -Dtest=ToolRegistryTest test`，现有注册中心测试通过。

## T7：覆盖 ToolSelection 与注册中心

**文件：** `ToolRegistryTest.java`  
**依赖：** T6

**步骤：**
1. 覆盖 unrestricted、only 和空集合。
2. 覆盖“允许但已禁用”和“未知名称”。
3. 验证过滤后的定义保持稳定名称顺序。

**验证：** `mvn -q -Dtest=ToolRegistryTest test` 全部通过。

## T8：把工具选择加入 ChatRequest

**文件：** `ChatRequest.java`  
**依赖：** T5

**步骤：**
1. 增加非空 `ToolSelection` 字段。
2. 保留现有一参数、两参数构造方式并默认 `allEnabled()`。
3. 保持消息、提醒和工具集合不可变。

**验证：** `mvn -q -DskipTests test-compile`，现有请求构造代码继续编译。

## T9：Anthropic 按请求过滤工具

**文件：** `AnthropicClient.java`  
**依赖：** T6、T8

**步骤：**
1. 构造 `tools` 时传入请求的 `ToolSelection`。
2. 保持工具定义格式和历史编码不变。
3. 空过滤结果输出空工具数组，不回退到全部工具。

**验证：** `mvn -q -Dtest=AnthropicClientTest test`。

## T10：OpenAI 按请求过滤工具

**文件：** `OpenAiClient.java`  
**依赖：** T6、T8

**步骤：**
1. 构造 function tools 时传入请求选择。
2. 保持 Responses API 格式和历史恢复不变。
3. 空选择不暴露任何工具。

**验证：** `mvn -q -Dtest=OpenAiClientTest test`。

## T11：DeepSeek 按请求过滤工具

**文件：** `DeepSeekClient.java`  
**依赖：** T6、T8

**步骤：**
1. 构造 Chat Completions tools 时传入请求选择。
2. 保持现有 function schema 与历史编码。
3. 空选择不暴露任何工具。

**验证：** `mvn -q -Dtest=DeepSeekClientTest test`。

## T12：覆盖三家请求级工具过滤

**文件：** 三个 `*ClientTest.java`  
**依赖：** T9–T11

**步骤：**
1. 为每家构造只允许 `read_file` 的请求。
2. 捕获请求 JSON，验证只包含该工具。
3. 构造空选择，验证写工具和 Bash 不可见。
4. 回归默认请求仍包含全部已启用工具。

**验证：** `mvn -q -Dtest=AnthropicClientTest,OpenAiClientTest,DeepSeekClientTest test`。

## T13：扩展 LlmClient 完成桥与取消接口

**文件：** `LlmClient.java`  
**依赖：** 无

**步骤：**
1. 增加默认 `cancelActiveRequest()`。
2. 旧 `StreamListener` 实现经富事件入口正常返回后补发一次 `StreamCompleted`。
3. 避免实际覆盖富事件入口的 Provider 产生重复完成事件。

**验证：** `mvn -q -Dtest=LlmClientContractTest test`。

## T14：实现 Anthropic 当前请求取消

**文件：** `AnthropicClient.java`  
**依赖：** T13

**步骤：**
1. 抽取取消 active future 和 stream 的共用逻辑。
2. `cancelActiveRequest()` 只取消当前资源，不设置 `closed`。
3. `close()` 设置永久关闭后复用取消逻辑。

**验证：** `mvn -q -Dtest=AnthropicClientTest test`。

## T15：实现 OpenAI 当前请求取消

**文件：** `OpenAiClient.java`  
**依赖：** T13

**步骤：**
1. 抽取活动请求与流取消逻辑。
2. 区分任务取消和永久关闭。
3. 确保取消后客户端可发送下一请求。

**验证：** `mvn -q -Dtest=OpenAiClientTest test`。

## T16：实现 DeepSeek 当前请求取消

**文件：** `DeepSeekClient.java`  
**依赖：** T13

**步骤：**
1. 抽取活动请求与流取消逻辑。
2. 区分任务取消和永久关闭。
3. 确保取消后客户端可发送下一请求。

**验证：** `mvn -q -Dtest=DeepSeekClientTest test`。

## T17：覆盖当前请求取消契约

**文件：** `LlmClientContractTest.java`、三个 `*ClientTest.java`  
**依赖：** T14–T16

**步骤：**
1. 使用慢速本地流启动活动请求并调用取消。
2. 验证当前调用结束且无正常完成事件。
3. 再次排队正常响应，验证同一客户端可继续使用。
4. 验证永久关闭后仍拒绝新请求。

**验证：** 三家 Provider 与 `LlmClientContractTest` 全部通过。

## T18：定义 Agent 基础领域类型

**文件：** `AgentMode.java`、`AgentStopReason.java`、`AgentError.java`、`AgentRequest.java`、`AgentResult.java`  
**依赖：** T1

**步骤：**
1. 定义两种模式和五种停止原因。
2. 定义请求、错误和结果记录并做不可变复制。
3. 校验只有 `FINAL_RESPONSE` 可以携带最终响应。
4. 实现 `completed()`。

**验证：** `mvn -q -DskipTests compile`。

## T19：定义 AgentEvent 与监听器

**文件：** `AgentEvent.java`、`AgentEventListener.java`  
**依赖：** T18

**步骤：**
1. 定义任务、轮次、模型、工具批次、模式和最终事件。
2. 校验轮次、索引、名称、错误和 Usage。
3. 提供 NOOP 监听器。
4. 不在模型事件中暴露 Thinking 元数据和工具参数碎片。

**验证：** `mvn -q -DskipTests compile`。

## T20：覆盖 Agent 事件约束

**文件：** `AgentEventTest.java`  
**依赖：** T19

**步骤：**
1. 覆盖合法事件字段和不可变性。
2. 拒绝负轮次、负索引、空名称和空错误。
3. 断言事件类型中不存在签名、encrypted content 或参数碎片字段。

**验证：** `mvn -q -Dtest=AgentEventTest test`。

## T21：实现 StreamingResponseCollector

**文件：** `StreamingResponseCollector.java`  
**依赖：** T13、T19

**步骤：**
1. 调用 LLM 富事件入口并记录完成事件数量。
2. 实时映射文本、Thinking、工具开始及 Usage。
3. 忽略工具参数碎片和敏感 Thinking 完成元数据。
4. 缺少或重复完成事件时产生安全协议错误。

**验证：** `mvn -q -DskipTests compile`。

## T22：覆盖流式收集器

**文件：** `StreamingResponseCollectorTest.java`  
**依赖：** T21

**步骤：**
1. 验证文本和 Thinking 在 `collect()` 返回前已经逐片发布。
2. 验证完整响应原样返回。
3. 验证工具参数、签名和 encrypted content 不进入 Agent 事件。
4. 覆盖缺失完成、重复完成和 LLM 异常。

**验证：** `mvn -q -Dtest=StreamingResponseCollectorTest test`。

## T23：定义工具批次记录

**文件：** `ToolBatchKind.java`、`IndexedToolCall.java`、`ToolBatch.java`  
**依赖：** 无

**步骤：**
1. 定义安全并发与串行屏障类型。
2. 保存每个调用的原始索引。
3. 校验索引非负、批次非空并不可变复制。

**验证：** `mvn -q -DskipTests compile`。

## T24：实现 partitionToolCalls

**文件：** `ToolCallPartitioner.java`  
**依赖：** T6、T23

**步骤：**
1. 单次线性扫描原始工具列表。
2. 把连续、允许、启用且 LOW 风险的调用合并为并发批次。
3. 把其余每个调用生成为单元素串行屏障。
4. 保持每个调用恰好出现一次且索引不变。

**验证：** `mvn -q -DskipTests compile`。

## T25：覆盖工具分区

**文件：** `ToolCallPartitionerTest.java`  
**依赖：** T24

**步骤：**
1. 覆盖 LOW/LOW/MEDIUM/LOW/HIGH 混合序列。
2. 覆盖全部安全、全部不安全和空列表。
3. 覆盖未知、禁用和 Plan Mode 禁止工具。
4. 验证批次及调用顺序精确一致。

**验证：** `mvn -q -Dtest=ToolCallPartitionerTest test`。

## T26：扩展 ToolExecutor 单工具执行

**文件：** `ToolExecutor.java`  
**依赖：** T6

**步骤：**
1. 抽取单个工具的 queued、running、result 状态流程。
2. 使用线程安全集合跟踪多个活动工具。
3. 取消时通知所有活动工具并阻止新工具开始。
4. 保留 `executeAll()` 的顺序和兼容行为。

**验证：** `mvn -q -Dtest=ToolExecutorTest test`。

## T27：覆盖 ToolExecutor 并发活动工具

**文件：** `ToolExecutorTest.java`  
**依赖：** T26

**步骤：**
1. 同时启动两个阻塞 LOW 工具并确认均进入 running。
2. 调用 cancel，验证两个工具均收到取消。
3. 验证未知、禁用和单工具异常仍产生失败结果。
4. 回归旧串行执行顺序。

**验证：** `mvn -q -Dtest=ToolExecutorTest test`。

## T28：实现 ToolBatchExecutor 顺序批次

**文件：** `ToolBatchExecutor.java`  
**依赖：** T1、T19、T24、T26

**步骤：**
1. 构造固定大小虚拟线程池和任务级 ToolExecutor。
2. 按分区结果顺序遍历批次。
3. 先实现串行屏障执行及批次开始/结束事件。
4. 未允许工具生成失败结果而不调用工具。

**验证：** `mvn -q -DskipTests compile`。

## T29：实现安全批次并发与结果排序

**文件：** `ToolBatchExecutor.java`  
**依赖：** T28

**步骤：**
1. 为安全批次并发提交单工具任务。
2. 收集成功、失败和异常结果，不因单个失败取消同批。
3. 按 originalIndex 排序后合并所有批次结果。
4. 确保并发数不超过 AgentConfig 上限。

**验证：** `mvn -q -DskipTests compile`。

## T30：实现工具批次取消与清理

**文件：** `ToolBatchExecutor.java`  
**依赖：** T29

**步骤：**
1. `cancel()` 设置幂等标志、取消 futures 并通知 ToolExecutor。
2. 取消后禁止后续批次和屏障启动。
3. `close()` 取消后关闭线程池并等待有限时间。
4. 中断调用线程时恢复中断标记。

**验证：** `mvn -q -DskipTests compile`。

## T31：覆盖并发、屏障、顺序与取消

**文件：** `ToolBatchExecutorTest.java`  
**依赖：** T30

**步骤：**
1. 用 latch 证明连续 LOW 工具真实重叠且并发有上限。
2. 验证 MEDIUM/HIGH 与前后批次没有重叠。
3. 让工具乱序结束，验证结果仍按原索引。
4. 覆盖单个失败、禁止工具、取消和重复关闭。

**验证：** `mvn -q -Dtest=ToolBatchExecutorTest test`。

## T32：定义 Plan Mode 策略

**文件：** `PlanModePrompt.java`  
**依赖：** T5、T18

**步骤：**
1. 定义只读工具名称集合。
2. 定义固定 `<system-reminder>` 内容，要求调查并输出计划。
3. 提供按 AgentMode 生成 ToolSelection 和额外提醒的入口。

**验证：** `mvn -q -DskipTests compile`。

## T33：实现 AgentTaskContext

**文件：** `AgentTaskContext.java`  
**依赖：** T18、T30

**步骤：**
1. 保存单调时钟截止点和原子停止原因。
2. 保存当前工具执行器、工具执行标记和副作用标记。
3. 实现完成、超时、错误、取消的单次终态竞争。
4. 终态设置后取消活动 LLM 和工具资源。

**验证：** `mvn -q -DskipTests compile`。

## T34：搭建 Agent 生命周期与模式

**文件：** `Agent.java`  
**依赖：** T18–T21、T32–T33

**步骤：**
1. 注入 LLM、注册中心、配置和看门执行器。
2. 实现 DO 默认模式、模式查询和 `switchMode()` 事件。
3. 使用原子引用拒绝并行的两个 `run()`。
4. 实现幂等 `cancelActive()` 和 `close()` 框架。

**验证：** `mvn -q -DskipTests compile`。

## T35：实现单轮模型调用与最终回答

**文件：** `Agent.java`  
**依赖：** T34

**步骤：**
1. 创建用户消息和当前任务临时轨迹。
2. 发布任务开始及轮次开始事件。
3. 以完整历史、提醒和工具选择构造 ChatRequest。
4. 通过 Collector 获取响应；无工具时生成成功结果和 TaskCompleted。

**验证：** `mvn -q -Dtest=AgentTest test`，先覆盖纯文本单轮。

## T36：实现多轮工具循环

**文件：** `Agent.java`  
**依赖：** T31、T35

**步骤：**
1. 把助手结构化响应追加到临时轨迹。
2. 有工具时调用 ToolBatchExecutor。
3. 把有序 ToolExecution 转成一条 TOOL 消息。
4. 追加结果后继续下一次 `while` 模型调用。

**验证：** `mvn -q -Dtest=AgentTest test`，两批工具后得到最终回答。

## T37：实现最大轮数停止

**文件：** `Agent.java`  
**依赖：** T36

**步骤：**
1. 每次模型调用前检查轮次预算。
2. 第 N 轮仍请求工具时不执行该批工具。
3. 返回 MAX_ITERATIONS 并发布一次 TaskStopped。
4. 保留临时轨迹用于诊断但不标记可提交。

**验证：** `mvn -q -Dtest=AgentTest#stopsAtMaximumIterations test`。

## T38：实现任务总超时与用户取消

**文件：** `Agent.java`、`AgentTaskContext.java`  
**依赖：** T17、T33、T37

**步骤：**
1. 启动任务时安排截止看门 future。
2. TIMEOUT 时取消当前 LLM 和工具执行器。
3. `cancelActive()` 使用 CANCELLED 原因走同一清理路径。
4. 任务结束时取消看门 future，下一任务可正常运行。

**验证：** `mvn -q -Dtest=AgentCancellationTest test`。

## T39：实现错误、事件异常和唯一终态

**文件：** `Agent.java`、`AgentTaskContext.java`  
**依赖：** T38

**步骤：**
1. 把 LlmException 转换为 AgentError。
2. 把监听器 RuntimeException 转成安全 ERROR 并停止后续副作用。
3. 按是否开始 MEDIUM/HIGH 工具设置 sideEffectsPossible。
4. 用原子终态保证 Completed、Stopped、Failed 只出现一个。

**验证：** `mvn -q -Dtest=AgentTest,AgentCancellationTest test`。

## T40：覆盖 Agent 核心循环

**文件：** `AgentTest.java`  
**依赖：** T39

**步骤：**
1. 覆盖无工具最终回答和两批工具多轮完成。
2. 覆盖工具失败结果回传后模型修正。
3. 覆盖最大轮数末轮工具不执行。
4. 验证每轮请求包含完整临时轨迹、相同提醒和有序结果。
5. 验证成功结果轨迹包含用户、中间响应、工具结果和最终回答。

**验证：** `mvn -q -Dtest=AgentTest test` 全部通过。

## T41：覆盖 Agent 超时、中断和终态竞争

**文件：** `AgentCancellationTest.java`  
**依赖：** T39

**步骤：**
1. 在模型流、并发安全批次和 Bash 替身期间分别取消。
2. 验证不再启动新工具或模型请求。
3. 验证超时后同一 Agent 可完成下一任务。
4. 构造最终回答与超时竞争，断言只有一个最终事件。
5. 重复 cancel/close 不产生挂起线程。

**验证：** `mvn -q -Dtest=AgentCancellationTest test`。

## T42：覆盖 Plan Mode Agent 行为

**文件：** `AgentTest.java`  
**依赖：** T32、T40

**步骤：**
1. 切换 PLAN 后验证每轮只携带三个只读工具和固定提醒。
2. 模拟模型伪造 write_file，验证不执行并回传失败。
3. 验证只读工具可多轮循环后输出计划。
4. 切换 DO 后验证六工具恢复且旧计划不会自动执行。

**验证：** `mvn -q -Dtest=AgentTest test`。

## T43：让会话监听器接收 AgentEvent

**文件：** `ConversationListener.java`  
**依赖：** T19

**步骤：**
1. 增加 `onAgentEvent` 默认入口。
2. 旧 `onTextDelta` 仅过滤 AgentEvent.TextDelta。
3. 保留函数式接口和旧文本调用兼容性。

**验证：** `mvn -q -DskipTests test-compile`。

## T44：把 ConversationSession 改为 Agent 事务

**文件：** `ConversationSession.java`  
**依赖：** T39、T43

**步骤：**
1. 构造 `AgentRequest`，传入历史、用户消息和提醒快照。
2. 委托 Agent 并把事件转给 ConversationListener。
3. 仅 completed 结果提交完整 trajectory。
4. 删除 Ch3 的“两次模型请求”和第二工具批次限制逻辑。

**验证：** `mvn -q -Dtest=ConversationSessionTest test`。

## T45：映射 Agent 停止与错误

**文件：** `ConversationException.java`、`ConversationSession.java`  
**依赖：** T44

**步骤：**
1. 在异常中加入 AgentStopReason。
2. 保留 recoverable、retryAfter、toolsExecuted 和 sideEffectsPossible。
3. MAX_ITERATIONS、TIMEOUT、ERROR、CANCELLED 使用可区分安全消息。
4. 所有非成功路径不提交历史。

**验证：** `mvn -q -Dtest=ConversationSessionTest test`。

## T46：接入会话模式切换与提醒生命周期

**文件：** `ConversationSession.java`  
**依赖：** T42、T45

**步骤：**
1. 增加 `mode()` 和 `switchMode()` 并委托 Agent。
2. 模式命令不生成 ChatMessage。
3. 提醒在任务开始时复制并清空，同一任务每轮复用。
4. 成功、停止、失败和中断后下一任务均不重复旧提醒。

**验证：** `mvn -q -Dtest=ConversationSessionTest test`。

## T47：覆盖会话轨迹、模式和失败事务

**文件：** `ConversationSessionTest.java`  
**依赖：** T46

**步骤：**
1. 验证完整多轮轨迹原子提交。
2. 验证五种停止条件中的非成功路径历史不变。
3. 验证提醒同任务复用、下任务消费。
4. 验证模式切换不进入历史。
5. 验证副作用和 Retry-After 元信息透传。

**验证：** `mvn -q -Dtest=ConversationSessionTest test` 全部通过。

## T48：扩展终端模式与停止展示

**文件：** `TerminalUi.java`、`JLineTerminalUi.java`  
**依赖：** T18

**步骤：**
1. 增加显示当前 AgentMode 的入口。
2. 增加显示 Agent 停止原因和副作用警告的入口。
3. 富终端与 dumb terminal 均提供可读文本。
4. 输出不包含 Thinking 元数据和原始工具参数。

**验证：** `mvn -q -Dtest=JLineTerminalUiTest test`。

## T49：把 AgentEvent 路由到终端

**文件：** `ConversationLoop.java`  
**依赖：** T43、T48

**步骤：**
1. 用 AgentEvent 替换直接 LlmEvent 和 ToolExecutionEvent 编排。
2. 映射每轮 Thinking、文本、工具状态和 Usage。
3. 映射 Completed、Stopped、Failed，并避免重复结束输出行。
4. 保持现有 ToolSummaryFormatter 安全摘要。

**验证：** `mvn -q -Dtest=ConversationLoopTest,JLineTerminalUiTest test`。

## T50：实现 `/plan` 与 `/do` 命令

**文件：** `ConversationLoop.java`  
**依赖：** T46、T49

**步骤：**
1. 在普通输入前识别精确 `/plan` 和 `/do`。
2. 调用会话模式切换并显示 ModeChanged。
3. 命令不请求模型、不增加历史、不自动执行计划。
4. 保持 `/exit`、`/quit`、空白输入和 Ctrl+C 行为。

**验证：** `mvn -q -Dtest=ConversationLoopTest test`。

## T51：覆盖终端事件、命令和敏感信息

**文件：** `ConversationLoopTest.java`、`JLineTerminalUiTest.java`  
**依赖：** T50

**步骤：**
1. 覆盖多轮 Thinking、文本、工具批次和每轮 Usage 显示顺序。
2. 覆盖 `/plan`、`/do` 零请求和持续模式。
3. 覆盖轮数、超时、错误和副作用提示。
4. 使用唯一签名、encrypted content 和写入正文标记，断言终端不显示。
5. 覆盖 dumb terminal 无 ANSI。

**验证：** `mvn -q -Dtest=ConversationLoopTest,JLineTerminalUiTest test`。

## T52：装配 Agent

**文件：** `ImioCodeApplication.java`  
**依赖：** T4、T39、T47、T51

**步骤：**
1. 使用现有六工具构造 ToolRegistry。
2. 使用 LlmClient、Registry 和 AgentConfig 构造 Agent。
3. 使用 Agent 构造 ConversationSession。
4. 保持启动错误、关闭顺序和 API Key 脱敏。

**验证：** `mvn -q -DskipTests compile`。

## T53：实现真实进程多步 E2E 测试

**文件：** `ConversationLoopTest.java`  
**依赖：** T52

**步骤：**
1. 使用 MockLlmServer 依次返回读取、写入、Bash 验证和最终回答。
2. 启动真实 `ImioCodeApplication` Java 进程。
3. 输入一个任务并验证至少四次模型请求、三类工具状态和最终回答。
4. 验证临时目标文件内容、Usage、Plan/Do 命令和 `/exit`。
5. 在 finally 清理临时文件和进程。

**验证：** `mvn -q -Dtest=ConversationLoopTest#applicationProcessCompletesMultiStepAgentTask test`。

## T54：升级公共契约与 Provider 回归

**文件：** `LlmClientContractTest.java`、三个 Provider 测试  
**依赖：** T12、T17、T53

**步骤：**
1. 验证旧文本客户端经兼容桥产生一次完成事件。
2. 验证三家工具选择和当前请求取消一致。
3. 验证原 Thinking、Usage、工具历史和错误测试保持通过。
4. 修复仅由新 ChatRequest 字段造成的测试构造问题。

**验证：** `mvn -q -Dtest=LlmClientContractTest,AnthropicClientTest,OpenAiClientTest,DeepSeekClientTest test`。

## T55：执行 Agent 与 Ch2/Ch3 全量回归

**文件：** 全部主源码与测试源码  
**依赖：** T54

**步骤：**
1. 运行干净测试。
2. 统计 Surefire 测试、失败、错误和跳过数量。
3. 修复回归，保留平台符号链接跳过说明。
4. 执行 `git diff --check`。

**验证：** `mvn -q clean test` 退出码 0，0 failures、0 errors。

## T56：打包与安全扫描

**文件：** `target/imiocode-0.2.0-SNAPSHOT-all.jar`  
**依赖：** T55

**步骤：**
1. 从干净状态生成 shaded JAR。
2. 无配置目录启动，验证安全错误和退出码。
3. 扫描生产源码、测试报告和 JAR 中的本地 Key、签名、encrypted content 和测试正文标记。
4. 确认 `claude.md` 未进入本章提交。

**验证：** `mvn -q clean package` 退出码 0，安全扫描无敏感匹配。

## T57：执行 tmux Agent Loop 验收

**文件：** `docs/ch4/checklist.md`  
**依赖：** T56

**步骤：**
1. 检查 tmux 和可用 WSL/Linux 环境。
2. 在 tmux 启动可执行 JAR，执行多步读、写、验证任务。
3. 验证 Plan Mode、Do Mode、并发安全工具、顺序屏障和中断。
4. 捕获 pane 输出并检查无密钥、签名、正文泄露。
5. 若环境不可用，记录实际命令和阻塞错误，不伪造通过。

**验证：** checklist 中记录自动化证据、tmux capture 或明确环境阻塞。

## 执行顺序

```text
T1 → T2 → T3 → T4

T5 → T6 → T7
 └→ T8 → T9 → T10 → T11 → T12

T13 → T14 → T15 → T16 → T17

T18 → T19 → T20
          └→ T21 → T22

T23 → T24 → T25
T6  → T26 → T27
T1 + T19 + T24 + T26 → T28 → T29 → T30 → T31

T5 + T18 → T32
T18 + T30 → T33
T19 + T21 + T32 + T33 → T34 → T35
T31 + T35 → T36 → T37
T17 + T33 + T37 → T38 → T39
T39 → T40 → T41
T32 + T40 → T42

T19 → T43
T39 + T43 → T44 → T45
T42 + T45 → T46 → T47

T18 → T48
T43 + T48 → T49
T46 + T49 → T50 → T51

T4 + T39 + T47 + T51 → T52 → T53
T12 + T17 + T53 → T54 → T55 → T56 → T57
```

## 需求覆盖

| Spec | 任务 |
|---|---|
| F1–F5 | T34–T40、T44–T47 |
| F6–F7 | T13、T19–T22 |
| F8–F10 | T33、T37–T41 |
| F11–F13 | T19–T22、T43、T48–T51 |
| F14–F18 | T23–T31 |
| F19–F24 | T5–T12、T32、T42、T46、T50–T51 |
| F25–F26 | T1–T4 |
| F27 | T35–T40、T44–T47 |
| F28–F30 | T18、T39–T47、T49–T51 |
| F31 | T8–T17、T34–T40、T54 |
| F32 | T51–T57 |
| N1–N20 | T1–T57 的接口校验、并发测试、回归、打包和验收 |

## 任务自检

- `plan.md` 中的配置、Agent、事件、收集器、分区器、执行器、会话、终端和 Provider 均有对应任务。
- T1–T57 均包含具体文件、依赖、步骤和验证方式。
- 主执行链不存在循环依赖。
- 配置、工具过滤和 Provider 取消在 Agent 核心之前完成。
- 并发分区、执行和取消分别实现并测试。
- Agent 循环、停止条件、Plan Mode、会话事务和终端分别拆分。
- 每条 F1–F32 至少对应一个实现任务和一个验证任务。
- 开发前仍需 `checklist.md` 获得用户批准。
