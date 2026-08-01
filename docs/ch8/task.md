# ch8 上下文管理 Tasks

## 文件清单

### 新建生产代码

| 文件 | 职责 |
|------|------|
| `src/main/java/io/imiocode/config/ContextConfig.java` | 上下文窗口与自动压缩阈值 |
| `src/main/java/io/imiocode/context/ApproximateTokenEstimator.java` | 统一近似 Token 估算 |
| `src/main/java/io/imiocode/context/AutoCompactTrackingState.java` | 连续失败与熔断状态 |
| `src/main/java/io/imiocode/context/CompactReport.java` | 手动压缩报告 |
| `src/main/java/io/imiocode/context/ContextEvent.java` | 上下文领域事件 |
| `src/main/java/io/imiocode/context/ContextEventListener.java` | 上下文事件监听器 |
| `src/main/java/io/imiocode/context/ContextException.java` | 安全的上下文管理异常 |
| `src/main/java/io/imiocode/context/ContextManageMode.java` | AUTO/FORCE/RECOVERY |
| `src/main/java/io/imiocode/context/ContextManager.java` | 两层压缩主编排器 |
| `src/main/java/io/imiocode/context/ContextOutcome.java` | 管理结果枚举 |
| `src/main/java/io/imiocode/context/ContextPolicy.java` | 固定策略阈值 |
| `src/main/java/io/imiocode/context/ContextRequest.java` | 管理请求 |
| `src/main/java/io/imiocode/context/ContextResult.java` | 管理结果 |
| `src/main/java/io/imiocode/context/ConversationSerializer.java` | 历史安全序列化 |
| `src/main/java/io/imiocode/context/ConversationSummarizer.java` | 无工具摘要调用 |
| `src/main/java/io/imiocode/context/OffloadResult.java` | 第一层落盘结果 |
| `src/main/java/io/imiocode/context/ParsedSummary.java` | 已验证摘要结构 |
| `src/main/java/io/imiocode/context/SpilledResult.java` | 单文件落盘元信息 |
| `src/main/java/io/imiocode/context/SummaryParser.java` | `<summary>` 严格解析 |
| `src/main/java/io/imiocode/context/SummaryPrompt.java` | 摘要专用提示 |
| `src/main/java/io/imiocode/context/ToolResultOffloader.java` | 单结果与累计结果瘦身 |
| `src/main/java/io/imiocode/context/ToolResultSpillStore.java` | 安全原子结果存储 |
| `src/main/java/io/imiocode/agent/ManagedConversationState.java` | Agent 内部历史事务边界 |

### 修改生产代码

| 文件 | 改动 |
|------|------|
| `.gitignore` | 忽略工具结果目录 |
| `README.md` | 增加 context 配置、`/compact` 和结果目录说明 |
| `src/main/java/io/imiocode/config/AppConfig.java` | 挂载 ContextConfig |
| `src/main/java/io/imiocode/config/ConfigDocument.java` | 增加 ContextDocument |
| `src/main/java/io/imiocode/config/ConfigLoader.java` | 加载与校验 context 配置 |
| `src/main/java/io/imiocode/conversation/ChatRequest.java` | 增加内部 System Prompt override |
| `src/main/java/io/imiocode/conversation/ConversationLoop.java` | 识别 `/compact` 和渲染事件 |
| `src/main/java/io/imiocode/conversation/ConversationSession.java` | 历史替换与手动压缩 |
| `src/main/java/io/imiocode/agent/Agent.java` | 每迭代压缩与超限恢复 |
| `src/main/java/io/imiocode/agent/AgentEvent.java` | 包装 ContextEvent |
| `src/main/java/io/imiocode/agent/AgentResult.java` | 携带可回写 committedHistory |
| `src/main/java/io/imiocode/agent/StreamingTurnExecutor.java` | 重试时保留 request override |
| `src/main/java/io/imiocode/llm/LlmClientFactory.java` | 接收共享 PromptAssembler |
| `src/main/java/io/imiocode/llm/LlmErrorType.java` | 增加 CONTEXT_LIMIT |
| `src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java` | 映射上下文超限信号 |
| `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicClient.java` | 提取安全错误判别字段 |
| `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java` | 提取安全错误判别字段 |
| `src/main/java/io/imiocode/llm/provider/deepseek/DeepSeekClient.java` | 提取安全错误判别字段 |
| `src/main/java/io/imiocode/prompt/PromptAssembler.java` | 支持 override 并共享实际载荷 |
| `src/main/java/io/imiocode/terminal/TerminalUi.java` | 上下文展示接口 |
| `src/main/java/io/imiocode/terminal/JLineTerminalUi.java` | 压缩状态与报告输出 |
| `src/main/java/io/imiocode/terminal/UiState.java` | 增加 COMPACTING |
| `src/main/java/io/imiocode/tool/workspace/WorkspaceWalker.java` | 跳过结果目录 |
| `src/main/java/io/imiocode/ImioCodeApplication.java` | 组装共享 PromptAssembler 和 ContextManager |

### 新建或修改测试

| 文件 | 职责 |
|------|------|
| `src/test/java/io/imiocode/context/ApproximateTokenEstimatorTest.java` | 各消息块与溢出估算 |
| `src/test/java/io/imiocode/context/AutoCompactTrackingStateTest.java` | 三次熔断与成功重置 |
| `src/test/java/io/imiocode/context/ContextManagerTest.java` | AUTO/FORCE/RECOVERY 编排 |
| `src/test/java/io/imiocode/context/ConversationSerializerTest.java` | XML 转义和分区 |
| `src/test/java/io/imiocode/context/ConversationSummarizerTest.java` | 无工具摘要请求契约 |
| `src/test/java/io/imiocode/context/SummaryParserTest.java` | 严格标签与非法输出 |
| `src/test/java/io/imiocode/context/ToolResultOffloaderTest.java` | 两种瘦身策略与幂等 |
| `src/test/java/io/imiocode/context/ToolResultSpillStoreTest.java` | 路径安全与原子写入 |
| `src/test/java/io/imiocode/config/ConfigLoaderTest.java` | context 配置合并 |
| `src/test/java/io/imiocode/config/YamlConfigLoaderTest.java` | YAML 字段解析与未知字段 |
| `src/test/java/io/imiocode/prompt/PromptAssemblerTest.java` | override 与无工具载荷 |
| `src/test/java/io/imiocode/llm/transport/HttpErrorMapperTest.java` | CONTEXT_LIMIT 判别 |
| `src/test/java/io/imiocode/llm/provider/anthropic/AnthropicClientTest.java` | Anthropic 错误体回归 |
| `src/test/java/io/imiocode/llm/provider/openai/OpenAiClientTest.java` | OpenAI 错误体回归 |
| `src/test/java/io/imiocode/llm/provider/deepseek/DeepSeekClientTest.java` | DeepSeek 错误体回归 |
| `src/test/java/io/imiocode/agent/AgentContextManagementTest.java` | 迭代压缩、回写和恢复 |
| `src/test/java/io/imiocode/agent/AgentTest.java` | AgentResult 构造回归 |
| `src/test/java/io/imiocode/agent/StreamingTurnExecutorTest.java` | override 重试保留 |
| `src/test/java/io/imiocode/conversation/ConversationCompactTest.java` | `/compact` 历史事务 |
| `src/test/java/io/imiocode/conversation/ConversationLoopTest.java` | 命令拦截和 UI 状态 |
| `src/test/java/io/imiocode/conversation/ConversationSessionTest.java` | 成功/失败历史替换 |
| `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java` | 压缩展示不泄露正文 |
| `src/test/java/io/imiocode/tool/workspace/WorkspaceWalkerTest.java` | 跳过结果目录 |

## T1：定义上下文配置模型

**文件：** `ContextConfig.java`、`AppConfig.java`、`ConfigDocument.java`

**依赖：** 无

**步骤：**

1. 定义 `ContextConfig` 默认窗口和阈值，校验正数和 `0 < threshold < 1`。
2. 在 `ConfigDocument` 增加 `context.window-tokens`、`context.auto-compact-threshold` 映射。
3. 在 `AppConfig` 增加不可空 context 字段，并为旧构造器补默认值。
4. 更新 `toString`，只展示非敏感 context 配置。

**验证：** `mvn -DskipTests compile`，期望所有旧调用方仍可编译。

## T2：加载与验证上下文配置

**文件：** `ConfigLoader.java`、`ConfigLoaderTest.java`、`YamlConfigLoaderTest.java`

**依赖：** T1

**步骤：**

1. 合并环境变量、YAML 和默认窗口 64,000。
2. 合并阈值环境变量、YAML 和默认值 0.80，并实现严格 double 解析。
3. 验证窗口大于 `max-output-tokens`。
4. 增加默认值、YAML、环境优先、非法数值、边界和旧配置兼容测试。

**验证：** `mvn -Dtest=ConfigLoaderTest,YamlConfigLoaderTest test`，期望 0 failures。

## T3：为摘要请求增加 System Prompt override

**文件：** `ChatRequest.java`、`PromptAssembler.java`、`LlmClientFactory.java`、`PromptAssemblerTest.java`

**依赖：** T1

**步骤：**

1. 给 ChatRequest 增加可选 override，并保留全部旧构造器。
2. PromptAssembler 对 override 做非空白校验并替换普通 System Prompt。
3. 确保空 ToolSelection 生成空工具列表和正确 cache intent。
4. LlmClientFactory 增加接收共享 PromptAssembler 的重载，旧入口继续可用。
5. 测试普通请求不变、摘要请求 override 生效且 tools 为空。

**验证：** `mvn -Dtest=PromptAssemblerTest test`，期望普通与 override 用例通过。

## T4：实现近似 Token 估算器

**文件：** `ApproximateTokenEstimator.java`、`ApproximateTokenEstimatorTest.java`

**依赖：** T3

**步骤：**

1. 实现字符公式和饱和加法。
2. 覆盖所有 MessagePart、System Prompt、reminders 组装后消息和输出预留。
3. 使用稳定 JSON 序列化估算工具参数与 Schema。
4. 为中文、Unicode、Thinking、ToolCall、ToolResult、工具定义和超大长度写测试。
5. 验证同输入重复估算一致。

**验证：** `mvn -Dtest=ApproximateTokenEstimatorTest test`，期望全部估算测试通过。

## T5：定义上下文领域契约

**文件：** `ContextPolicy.java`、`ContextManageMode.java`、`ContextOutcome.java`、`ContextRequest.java`、`ContextResult.java`、`ContextException.java`、`CompactReport.java`

**依赖：** T1、T4

**步骤：**

1. 固化七个推荐策略值并校验。
2. 定义三种管理模式和五种结果状态。
3. 定义不可变请求/结果，复制所有集合并验证消息边界。
4. ContextException 只暴露安全文案和可恢复标志。
5. CompactReport 不包含正文或摘要内容。

**验证：** `mvn -DskipTests compile`，期望领域模型编译通过。

## T6：实现摘要失败追踪和事件

**文件：** `AutoCompactTrackingState.java`、`ContextEvent.java`、`ContextEventListener.java`、`AutoCompactTrackingStateTest.java`

**依赖：** T5

**步骤：**

1. 实现线程安全或任务内严格封装的连续失败计数。
2. 第 3 次失败打开 circuit，成功清零。
3. 定义 started/offloaded/completed/failed/circuit 事件并校验字段。
4. 提供 NOOP listener。
5. 测试失败、重置和不同实例互不影响。

**验证：** `mvn -Dtest=AutoCompactTrackingStateTest test`，期望熔断测试通过。

## T7：实现工具结果落盘命名与正文编码

**文件：** `SpilledResult.java`、`ToolResultSpillStore.java`、`ToolResultSpillStoreTest.java`

**依赖：** T5

**步骤：**

1. 设计稳定正文格式，完整保存 output、error 和元信息。
2. 清洗调用 ID、限制长度并计算 SHA-256 内容哈希。
3. 构造工作区相对路径，拒绝绝对路径和路径分隔符逃逸。
4. 为相同内容返回相同文件名，不同内容返回不同文件名。
5. 测试中文、空/恶意 ID、Windows 保留名和重名。

**验证：** `mvn -Dtest=ToolResultSpillStoreTest#namesAndEncodesResults test`，期望命名与正文断言通过。

## T8：实现目录安全与原子写入

**文件：** `ToolResultSpillStore.java`、`ToolResultSpillStoreTest.java`

**依赖：** T7

**步骤：**

1. 安全创建 `.imiocode/tool-results`，逐段检查普通目录。
2. 目标存在时只接受匹配的普通文件，拒绝链接/目录/重解析点。
3. 同目录写临时文件并原子、不覆盖地移动。
4. 确保异常路径删除临时文件且不替换原结果。
5. 添加 symlink/Junction 能力检测；无创建权限时按现有测试约定 skip。

**验证：** `mvn -Dtest=ToolResultSpillStoreTest test`，期望安全用例 0 failures。

## T9：实现单个大结果瘦身

**文件：** `OffloadResult.java`、`ToolResultOffloader.java`、`ToolResultOffloaderTest.java`

**依赖：** T8

**步骤：**

1. 遍历 ChatMessage/ToolResultPart，保持角色和块顺序。
2. 对合计超过 5,000 字符的单结果调用 store。
3. 构造固定占位，保留结果元信息与适当 output/error 通道。
4. 识别已有占位，保证二次处理幂等。
5. 写成功、失败、写盘异常和混合内容块测试。

**验证：** `mvn -Dtest=ToolResultOffloaderTest#offloadsSingleLargeResult test`，期望全文文件与占位都正确。

## T10：实现累计旧结果瘦身

**文件：** `ToolResultOffloader.java`、`ToolResultOffloaderTest.java`

**依赖：** T9

**步骤：**

1. 统计未落盘 ToolResult 内容总字符数。
2. 从消息尾部定位最近 3 条 TOOL 消息保护边界。
3. 超过 20,000 时从旧到新落盘，直到低于阈值或无可处理旧结果。
4. 将批量占位预览限制为 2,000 字符。
5. 测试恰好阈值、超过阈值、最近结果保护、多 ToolResultPart 和组合策略。

**验证：** `mvn -Dtest=ToolResultOffloaderTest test`，期望两种策略和幂等测试通过。

## T11：实现会话安全序列化

**文件：** `ConversationSerializer.java`、`ConversationSerializerTest.java`

**依赖：** T5

**步骤：**

1. 序列化 prior_history 和 active_task 两个分区。
2. 覆盖 Text、Thinking、ToolCall 参数和 ToolResult 元信息。
3. 对用户内容执行 XML 转义，控制标签只由序列化器生成。
4. 保持消息/内容块原始顺序和稳定字段顺序。
5. 测试标签注入、中文、空 prior、混合块和重复序列化稳定性。

**验证：** `mvn -Dtest=ConversationSerializerTest test`，期望边界不能被内容突破。

## T12：实现摘要严格解析

**文件：** `ParsedSummary.java`、`SummaryParser.java`、`SummaryParserTest.java`

**依赖：** T11

**步骤：**

1. 要求响应只有一个 summary 根标签。
2. 要求唯一 prior_history 和 active_task 子标签。
3. 解码 XML 实体并拒绝空白、重复、嵌套伪标签和根外文本。
4. 限制解析复杂度和最大响应字符数。
5. 测试合法、缺标签、重复标签、空摘要、Markdown fence 和恶意嵌套。

**验证：** `mvn -Dtest=SummaryParserTest test`，期望非法响应全部被拒绝。

## T13：实现摘要提示与 LLM 调用

**文件：** `SummaryPrompt.java`、`ConversationSummarizer.java`、`ConversationSummarizerTest.java`

**依赖：** T3、T11、T12

**步骤：**

1. 编写固定摘要 System Prompt，列出必须保留字段和严格输出格式。
2. 用 serializer 生成唯一 USER 输入。
3. 构造空工具、空 reminder、System override 和受限输出 Token 的 ChatRequest。
4. 静默收集响应，拒绝 tool call、空 text 和非法 summary。
5. 用假 LLM 验证请求契约、取消透传和安全异常。

**验证：** `mvn -Dtest=ConversationSummarizerTest test`，期望 LLM 只收到一次无工具摘要请求。

## T14：实现 ContextManager 第一层与预算判断

**文件：** `ContextManager.java`、`ContextManagerTest.java`

**依赖：** T4、T6、T10、T13

**步骤：**

1. 用共享 PromptAssembler 构造 before ApiPayload 并估算。
2. 调用 offloader 后重新组装和估算。
3. 未达阈值时不调用 summarizer，返回 UNCHANGED/OFFLOADED。
4. 返回 committed/trajectory/working 三个不可变视图。
5. 发送 ResultsOffloaded 事件但不包含正文。

**验证：** `mvn -Dtest=ContextManagerTest#doesNotSummarizeBelowThreshold test`，期望摘要调用数为 0。

## T15：实现 AUTO/FORCE 摘要与熔断

**文件：** `ContextManager.java`、`ContextManagerTest.java`

**依赖：** T14

**步骤：**

1. AUTO 达到阈值时调用 summarizer 一次。
2. 使用 ParsedSummary 构造工作消息和仅 prior 的可回写历史。
3. 重新估算并拒绝不降反升的摘要。
4. FORCE 跳过阈值，空历史 no-op。
5. 失败保留第一层结果；连续 3 次打开 circuit，成功重置。
6. 覆盖开始、完成、失败和熔断事件。

**验证：** `mvn -Dtest=ContextManagerTest test`，期望三种结果路径和计数全部通过。

## T16：识别 Provider 上下文超限

**文件：** `LlmErrorType.java`、`HttpErrorMapper.java`、三个 Provider Client、相关 Provider/Mapper 测试

**依赖：** 无

**步骤：**

1. 增加 CONTEXT_LIMIT 类型。
2. 三个 Provider 从错误体提取 code/type/message 作为内部判别字符串。
3. HttpErrorMapper 匹配已知上下文超限信号并返回固定安全文案。
4. 确保 OUTPUT_LIMIT、认证、限流和未知 400 不被误分类。
5. 测试原始 provider message 不出现在 safeMessage。

**验证：** `mvn -Dtest=HttpErrorMapperTest,AnthropicClientTest,OpenAiClientTest,DeepSeekClientTest test`。

## T17：实现 Agent 内部会话状态

**文件：** `ManagedConversationState.java`、`AgentResult.java`、`AgentTest.java`

**依赖：** T15

**步骤：**

1. 用 committedHistory + userMessage 初始化 state。
2. 支持应用 ContextResult、追加响应和工具结果。
3. completed 视图包含工作历史和完整轨迹。
4. failed/stopped 视图只暴露 ContextResult 确认的 committedHistory。
5. 给 AgentResult 增加 committedHistory 并更新所有工厂方法与约束。

**验证：** `mvn -Dtest=AgentTest test`，期望旧行为和新事务断言通过。

## T18：在 Agent 每次迭代前管理上下文

**文件：** `Agent.java`、`AgentEvent.java`、`AgentContextManagementTest.java`

**依赖：** T6、T15、T17

**步骤：**

1. 给 Agent 增加 ContextManager 依赖及兼容旧构造器的 no-op/default 路径。
2. 每个迭代在真实 LLM 调用前构造 AUTO ContextRequest。
3. 应用 ContextResult 后用 workingMessages 发起请求。
4. 将 ContextEvent 包装成 AgentEvent.ContextChanged。
5. 模型响应与工具结果追加到 ManagedConversationState。
6. 所有终止路径返回最新 committedHistory。

**验证：** `mvn -Dtest=AgentContextManagementTest test`，期望多迭代第二次请求使用压缩历史。

## T19：实现 CONTEXT_LIMIT 单次恢复

**文件：** `Agent.java`、`StreamingTurnExecutor.java`、`AgentContextManagementTest.java`、`StreamingTurnExecutorTest.java`

**依赖：** T16、T18

**步骤：**

1. StreamingTurnExecutor 重建重试请求时保留 System override。
2. Agent 捕获 CONTEXT_LIMIT 后构造 RECOVERY 请求。
3. 同一迭代使用布尔状态保证最多恢复一次。
4. 摘要调用自身的 CONTEXT_LIMIT 计为失败，不递归恢复。
5. 恢复后重试模型；再次超限返回安全错误。

**验证：** `mvn -Dtest=AgentContextManagementTest,StreamingTurnExecutorTest test`，期望请求次数有严格上限。

## T20：回写 ConversationSession 历史

**文件：** `ConversationSession.java`、`ConversationSessionTest.java`

**依赖：** T17、T18

**步骤：**

1. AgentResult 返回后先替换 committedHistory。
2. completed 时追加 trajectory；失败/停止时不追加。
3. 原子更新 history，避免观察到半替换状态。
4. 保持 pending reminders 和 mode 不变。
5. 测试完成、失败、停止、压缩后失败和无压缩回归。

**验证：** `mvn -Dtest=ConversationSessionTest test`，期望事务边界全部通过。

## T21：实现手动 forceCompact API

**文件：** `Agent.java`、`ConversationSession.java`、`ConversationCompactTest.java`

**依赖：** T15、T20

**步骤：**

1. Agent 增加 forceCompactHistory，活动任务时拒绝。
2. 使用独立 tracking 和 FORCE 模式。
3. Session 同步快照并在成功后一次性替换 history。
4. 空历史返回 no-op，不调用 LLM。
5. 失败保持历史、reminders 和 Agent mode。

**验证：** `mvn -Dtest=ConversationCompactTest test`，期望成功/失败/空历史路径通过。

## T22：接入 `/compact` 命令和事件 UI

**文件：** `ConversationLoop.java`、`TerminalUi.java`、`JLineTerminalUi.java`、`UiState.java`、`ConversationLoopTest.java`、`JLineTerminalUiTest.java`

**依赖：** T6、T21

**步骤：**

1. 在本地命令分支识别大小写无关 `/compact`。
2. 增加 COMPACTING 状态并确保 finally 恢复 READY。
3. 将 ContextChanged 事件转发给终端。
4. 显示 before/after、节省比例和落盘数量。
5. 确保摘要正文、原历史和落盘全文不出现在输出。
6. 测试 `/compact` 不传给 session.sendWithEvents。

**验证：** `mvn -Dtest=ConversationLoopTest,JLineTerminalUiTest test`。

## T23：组装应用依赖

**文件：** `ImioCodeApplication.java`、`LlmClientFactory.java`、相关应用进程测试

**依赖：** T2、T3、T18、T21、T22

**步骤：**

1. 创建单一 PromptAssembler。
2. 用共享 assembler 创建 Provider Client 和 ContextManager。
3. 创建安全 spill store/offloader/summarizer，并注入 Agent。
4. 保持关闭顺序：Session/Agent 取消摘要后再关闭 Client。
5. 调整真实进程测试夹具，使其使用临时工作区，不受本地 MCP/权限配置影响。

**验证：** `mvn -Dtest=ConversationLoopTest,PermissionApplicationE2ETest test`，期望本机 `.imiocode` 配置存在时也不干扰测试。

## T24：隔离工具结果目录

**文件：** `.gitignore`、`WorkspaceWalker.java`、`WorkspaceWalkerTest.java`

**依赖：** T8

**步骤：**

1. Git 忽略 `/.imiocode/tool-results/`。
2. Walker 发现该目录时不返回且不递归。
3. 只跳过精确内部目录，不误伤名称相近路径。
4. 验证 read_file 仍能读取明确结果文件路径。

**验证：** `mvn -Dtest=WorkspaceWalkerTest,ReadFileToolTest test`，并运行 `git check-ignore .imiocode/tool-results/example.txt`。

## T25：更新文档和配置示例

**文件：** `README.md`、`docs/ch8/plan.md`

**依赖：** T2、T21、T24

**步骤：**

1. 记录 context YAML 与环境变量。
2. 记录 `/compact` 行为和 Token 报告。
3. 说明结果目录、读取方式、保留策略和 Git/Glob 隔离。
4. 说明默认 64K/80% 及近似估算误差。

**验证：** 对照实际配置字段和终端输出人工核对，无过期类名或命令。

## T26：运行模块与全量回归

**文件：** 全部生产/测试文件

**依赖：** T1-T25

**步骤：**

1. 运行 context、config、prompt、agent、conversation 定向测试。
2. 运行 `mvn test` 全量回归。
3. 运行 `mvn package -DskipTests` 生成 shaded JAR。
4. 运行 `git diff --check`。
5. 记录测试数量、跳过项和原因。

**验证：** 0 failures、0 errors；仅允许已知的 Windows symlink 权限 skip；JAR 存在。

## T27：真实进程与 tmux 端到端验收

**文件：** `docs/ch8/checklist.md`、`docs/ch8/acceptance-report.md`

**依赖：** T26

**步骤：**

1. 临时配置极小窗口/阈值和可控假 LLM，触发自动压缩。
2. 在 tmux 中启动 shaded JAR，发送长对话和大工具结果请求。
3. 观察落盘占位、自动摘要事件、下一迭代继续工作。
4. 输入 `/compact`，观察前后 Token 和历史替换。
5. 触发摘要失败三次和 CONTEXT_LIMIT，验证熔断与单次恢复。
6. 若本机无 tmux 或 WSL 不可用，记录实际阻塞，并使用真实 Java 进程交互测试替代；不得伪报 tmux 通过。
7. 对照 checklist 逐项记录证据。

**验证：** checklist 每项有实际证据；未通过项修复后重跑，最终生成 acceptance-report。

## 分组提交

| 提交组 | 包含任务 | 建议提交信息 |
|--------|----------|--------------|
| G1 | T1-T6 | `feat(ch8): add context configuration and token estimation` |
| G2 | T7-T13 | `feat(ch8): add tool result offloading and summarization` |
| G3 | T14-T21 | `feat(ch8): integrate context management with agent loop` |
| G4 | T22-T25 | `feat(ch8): add compact command and context UI` |
| G5 | T26-T27 | `test(ch8): complete context management acceptance` |

每组必须先运行对应验证，再提交；不得包含 `claude.md`、`hello.txt`、`config.yaml` 或 `.imiocode` 本地配置。

## 执行顺序

```text
T1 → T2 ───────────────┐
 └→ T3 → T4 → T5 → T6 │
              ├→ T7 → T8 → T9 → T10 ─┐
              └→ T11 → T12 → T13 ─────┤
                                       └→ T14 → T15
T16 ───────────────────────────────────────┐
T15 → T17 → T18 → T19 ←───────────────────┘
              └→ T20 → T21 → T22 → T23
T8 → T24 ───────────────────────────┐
T2 + T21 + T24 → T25 ───────────────┤
                                     └→ T26 → T27
```

## 覆盖自检

- Plan 的 11 个模块均至少对应一个任务。
- Spec F1-F10 分别由 T1-T25 覆盖。
- 每个任务都有具体文件、依赖、步骤和可运行验证。
- 依赖图无循环。
- 自动压缩、手动压缩、超限恢复、失败熔断和真实进程均进入验收任务。
