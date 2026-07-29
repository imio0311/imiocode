# ImioCode 第五章：Prompt 工程体系 Tasks

## 文件清单

### 新建文件

| 文件 | 职责 |
|---|---|
| `src/main/java/io/imiocode/prompt/Section.java` | Prompt 模块不可变数据 |
| `src/main/java/io/imiocode/prompt/SectionPriority.java` | 七模块稳定优先级 |
| `src/main/java/io/imiocode/prompt/PromptSection.java` | Prompt 模块契约 |
| `src/main/java/io/imiocode/prompt/SystemPromptBuilder.java` | 模块排序与 System Prompt 渲染 |
| `src/main/java/io/imiocode/prompt/ApiPayload.java` | 三通道统一组装结果 |
| `src/main/java/io/imiocode/prompt/CacheDirective.java` | 通道缓存指令 |
| `src/main/java/io/imiocode/prompt/CacheIntent.java` | system/tools 缓存意图 |
| `src/main/java/io/imiocode/prompt/PromptAssembler.java` | 七源到三通道组装管线 |
| `src/main/java/io/imiocode/prompt/EnvironmentContext.java` | 单任务环境快照 |
| `src/main/java/io/imiocode/prompt/EnvironmentContextProvider.java` | 环境采集抽象 |
| `src/main/java/io/imiocode/prompt/EnvironmentContextCollector.java` | OS、Shell、时间和 Git 采集 |
| `src/main/java/io/imiocode/prompt/EnvironmentReminderFormatter.java` | 环境快照提醒格式化 |
| `src/main/java/io/imiocode/prompt/GitContext.java` | Git 分支与工作区状态 |
| `src/main/java/io/imiocode/prompt/GitWorkingTreeState.java` | Git 状态枚举 |
| `src/main/java/io/imiocode/prompt/section/IdentitySection.java` | 身份模块 |
| `src/main/java/io/imiocode/prompt/section/BehaviorSection.java` | 行为模块 |
| `src/main/java/io/imiocode/prompt/section/ToolUsageSection.java` | 工具使用模块 |
| `src/main/java/io/imiocode/prompt/section/CodeQualitySection.java` | 代码质量模块 |
| `src/main/java/io/imiocode/prompt/section/SecuritySection.java` | 安全模块 |
| `src/main/java/io/imiocode/prompt/section/TaskPatternSection.java` | 任务模式模块 |
| `src/main/java/io/imiocode/prompt/section/OutputStyleSection.java` | 输出风格模块 |
| `src/main/java/io/imiocode/conversation/ReminderScope.java` | 提醒作用域 |
| `src/test/java/io/imiocode/prompt/SystemPromptBuilderTest.java` | 七模块顺序、稳定性和空模块测试 |
| `src/test/java/io/imiocode/prompt/PromptAssemblerTest.java` | 三通道路由和提醒顺序测试 |
| `src/test/java/io/imiocode/prompt/EnvironmentContextCollectorTest.java` | 环境采集与失败降级测试 |
| `src/test/java/io/imiocode/prompt/EnvironmentReminderFormatterTest.java` | 安全格式化测试 |
| `src/test/java/io/imiocode/conversation/SystemReminderTest.java` | 提醒作用域与 XML 包装测试 |
| `src/test/java/io/imiocode/agent/PlanModePromptTest.java` | Plan Mode 周期与只读工具测试 |
| `src/test/java/io/imiocode/tool/core/CoreToolDescriptionTest.java` | 六个工具描述契约测试 |
| `docs/ch5/eval-scenarios.md` | 五个定性评估场景和记录表 |

### 修改文件

| 文件 | 改动 |
|---|---|
| `src/main/java/io/imiocode/conversation/SystemReminder.java` | 增加作用域、兼容构造器和统一 XML 包装 |
| `src/main/java/io/imiocode/agent/Agent.java` | 采集任务环境并按轮生成提醒 |
| `src/main/java/io/imiocode/agent/PlanModePrompt.java` | 完整/精简 Plan 提醒周期 |
| `src/main/java/io/imiocode/llm/LlmClientFactory.java` | 创建并注入统一 PromptAssembler |
| `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java` | 序列化 ApiPayload 与解析缓存写入量 |
| `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicClient.java` | 序列化提醒消息及缓存断点 |
| `src/main/java/io/imiocode/llm/provider/deepseek/DeepSeekClient.java` | 注入稳定 system 并解析官方缓存字段 |
| `src/main/java/io/imiocode/tool/ToolRegistry.java` | 按 ToolSelection 返回工具定义 |
| `src/main/java/io/imiocode/tool/core/ReadFileTool.java` | 强化使用描述 |
| `src/main/java/io/imiocode/tool/core/WriteFileTool.java` | 强化使用描述 |
| `src/main/java/io/imiocode/tool/core/EditFileTool.java` | 强化使用描述 |
| `src/main/java/io/imiocode/tool/core/BashTool.java` | 强化使用描述 |
| `src/main/java/io/imiocode/tool/core/GlobTool.java` | 强化使用描述 |
| `src/main/java/io/imiocode/tool/core/GrepTool.java` | 强化使用描述 |
| `src/main/java/io/imiocode/ImioCodeApplication.java` | 装配工作区环境采集器 |
| `src/test/java/io/imiocode/agent/AgentTest.java` | 环境复用、轮次提醒与历史隔离测试 |
| `src/test/java/io/imiocode/conversation/ConversationSessionTest.java` | 会话提醒消费与历史隔离回归测试 |
| `src/test/java/io/imiocode/llm/LlmClientContractTest.java` | Provider 无关客户端契约回归测试 |
| `src/test/java/io/imiocode/llm/provider/openai/OpenAiClientTest.java` | OpenAI 三通道与 cache usage 测试 |
| `src/test/java/io/imiocode/llm/provider/anthropic/AnthropicClientTest.java` | Anthropic 消息和 cache_control 测试 |
| `src/test/java/io/imiocode/llm/provider/deepseek/DeepSeekClientTest.java` | DeepSeek system 和缓存字段测试 |

## T0：建立回归基线

**文件：** 无  
**依赖：** 无

**步骤：**

1. 确认当前工作树中 `claude.md` 和 `hello.txt` 属于用户已有改动，不纳入本章提交。
2. 运行全部现有测试并保存通过数量。
3. 若基线失败，停止开发并记录失败测试，不能把既有失败归因于 Ch5。

**验证：** 运行 `mvn -q test`，期望当前 Ch2～Ch4 测试全部通过。

## T1：建立 Section 核心契约

**文件：**

- `src/main/java/io/imiocode/prompt/Section.java`
- `src/main/java/io/imiocode/prompt/SectionPriority.java`
- `src/main/java/io/imiocode/prompt/PromptSection.java`

**依赖：** T0

**步骤：**

1. 定义七个显式优先级及只读数值访问器。
2. 定义 `Section`，校验名称、优先级和内容。
3. 定义 `PromptSection.section()` 契约。
4. 对传入文本做确定性裁剪，不允许可变集合或空名称。

**验证：** 运行 `mvn -q -DskipTests compile`，期望主代码编译通过。

## T2：实现七个固定 Prompt Section

**文件：**

- `src/main/java/io/imiocode/prompt/section/IdentitySection.java`
- `src/main/java/io/imiocode/prompt/section/BehaviorSection.java`
- `src/main/java/io/imiocode/prompt/section/ToolUsageSection.java`
- `src/main/java/io/imiocode/prompt/section/CodeQualitySection.java`
- `src/main/java/io/imiocode/prompt/section/SecuritySection.java`
- `src/main/java/io/imiocode/prompt/section/TaskPatternSection.java`
- `src/main/java/io/imiocode/prompt/section/OutputStyleSection.java`

**依赖：** T1

**步骤：**

1. 按 Plan 中批准的内容分别实现七个中文模块。
2. 每个实现只返回自身的名称、优先级和固定内容。
3. 检查相同规则只出现一次，避免身份、行为和工具模块互相重复。
4. 不引入环境、时间、模式或 Provider 信息。

**验证：** 运行 `mvn -q -DskipTests compile`，期望七个 Section 均可编译。

## T3：实现稳定 System Prompt 组装

**文件：**

- `src/main/java/io/imiocode/prompt/SystemPromptBuilder.java`
- `src/test/java/io/imiocode/prompt/SystemPromptBuilderTest.java`

**依赖：** T1、T2

**步骤：**

1. 构造时复制 Section 列表并拒绝空列表和重复模块名。
2. 按优先级数值和名称稳定排序。
3. 过滤空内容，以固定标题、空行和换行规则生成最终文本。
4. 测试乱序输入、相同优先级、空模块、重复模块和重复构建。
5. 断言默认七模块只出现一次且顺序固定。

**验证：** 运行 `mvn -q -Dtest=SystemPromptBuilderTest test`，期望全部通过。

## T4：实现分层 System Reminder

**文件：**

- `src/main/java/io/imiocode/conversation/ReminderScope.java`
- `src/main/java/io/imiocode/conversation/SystemReminder.java`
- `src/test/java/io/imiocode/conversation/SystemReminderTest.java`

**依赖：** T0

**步骤：**

1. 定义 `ENVIRONMENT`、`SESSION`、`ROUND` 三种作用域。
2. 将 `SystemReminder` 扩展为作用域和正文，并保留旧单参数构造器。
3. 集中实现 `<system-reminder>` 包装。
4. 拒绝空正文及嵌套保留标签。
5. 测试默认作用域、三种作用域、稳定包装和非法输入。

**验证：** 运行 `mvn -q -Dtest=SystemReminderTest test`，期望全部通过。

## T5：建立环境快照与安全格式化

**文件：**

- `src/main/java/io/imiocode/prompt/EnvironmentContext.java`
- `src/main/java/io/imiocode/prompt/EnvironmentContextProvider.java`
- `src/main/java/io/imiocode/prompt/EnvironmentReminderFormatter.java`
- `src/main/java/io/imiocode/prompt/GitContext.java`
- `src/main/java/io/imiocode/prompt/GitWorkingTreeState.java`
- `src/test/java/io/imiocode/prompt/EnvironmentReminderFormatterTest.java`

**依赖：** T4

**步骤：**

1. 定义不可变环境、Git 上下文和状态类型。
2. 校验工作区为规范化绝对路径，时间必须包含时区。
3. 按固定字段顺序格式化工作区、OS、Shell、时间、时区、Git 分支和状态。
4. 输出 `ENVIRONMENT` 作用域提醒。
5. 测试 clean、dirty、非仓库和不可用状态。
6. 断言输出不包含环境变量、凭据、Git 文件路径或原始命令输出。

**验证：** 运行 `mvn -q -Dtest=EnvironmentReminderFormatterTest test`，期望全部通过。

## T6：实现单任务环境采集

**文件：**

- `src/main/java/io/imiocode/prompt/EnvironmentContextCollector.java`
- `src/test/java/io/imiocode/prompt/EnvironmentContextCollectorTest.java`

**依赖：** T5

**步骤：**

1. 使用构造参数固定工作区、Clock 和 Git 超时。
2. 从 Java 系统属性采集 OS，并把父进程命令裁剪成文件名。
3. 使用不经过 Shell 的参数化 Git 命令获取分支和 porcelain 状态。
4. 只将结果归纳为分支和 clean/dirty，不保留文件名。
5. 对 Git 不存在、非仓库、超时和异常进行安全降级。
6. 使用固定 Clock 和临时工作区测试确定性，不访问真实模型。

**验证：** 运行 `mvn -q -Dtest=EnvironmentContextCollectorTest test`，期望全部通过且失败场景不抛出任务级异常。

## 提交点 A：Prompt 与环境基础

完成 T1～T6 后，只暂存对应源码、测试及 `docs/ch5` 已批准文档，创建一次本地 Git 提交。不得暂存 `claude.md` 和 `hello.txt`。

## T7：建立统一 Payload 与缓存意图

**文件：**

- `src/main/java/io/imiocode/prompt/ApiPayload.java`
- `src/main/java/io/imiocode/prompt/CacheDirective.java`
- `src/main/java/io/imiocode/prompt/CacheIntent.java`

**依赖：** T3、T4

**步骤：**

1. 定义 `NONE` 和 `EPHEMERAL` 缓存指令。
2. 定义 system/tools 两通道缓存意图及稳定通道工厂。
3. 定义包含 system、messages、tools、缓存意图和输出限制的不可变 Payload。
4. 对字符串、集合和可选输出限制进行防御性复制与校验。

**验证：** 运行 `mvn -q -DskipTests compile`，期望主代码编译通过。

## T8：扩展工具选择查询

**文件：**

- `src/main/java/io/imiocode/tool/ToolRegistry.java`
- `src/test/java/io/imiocode/tool/ToolRegistryTest.java`

**依赖：** T0

**步骤：**

1. 增加按 `ToolSelection` 返回启用工具定义的方法。
2. 复用现有启用状态和名称排序逻辑。
3. 保留 `exportEnabled(...)` 的行为和公开接口。
4. 测试全部工具、指定工具、禁用工具和空选择结果。

**验证：** 运行 `mvn -q -Dtest=ToolRegistryTest test`，期望新旧注册中心测试全部通过。

## T9：实现七源到三通道组装管线

**文件：**

- `src/main/java/io/imiocode/prompt/PromptAssembler.java`
- `src/test/java/io/imiocode/prompt/PromptAssemblerTest.java`

**依赖：** T3、T4、T7、T8

**步骤：**

1. 构造时生成并保存一次稳定 System Prompt。
2. 按环境、会话、历史/轨迹、轮次的顺序组装消息。
3. 将每条提醒转换为独立的 user 消息并调用统一 XML 包装。
4. 保持用户原始消息对象和正文不变。
5. 根据 `ToolSelection` 获取稳定工具列表。
6. system 始终标记 EPHEMERAL，tools 仅在非空时标记 EPHEMERAL。
7. 测试七类信息的通道归属、顺序、空提醒、Plan 工具子集和重复组装稳定性。

**验证：** 运行 `mvn -q -Dtest=PromptAssemblerTest test`，期望全部通过。

## 提交点 B：统一组装管线

完成 T7～T9 后，创建只包含 Payload、注册中心、组装器及相关测试的本地 Git 提交。

## T10：强化六个核心工具描述

**文件：**

- `src/main/java/io/imiocode/tool/core/ReadFileTool.java`
- `src/main/java/io/imiocode/tool/core/WriteFileTool.java`
- `src/main/java/io/imiocode/tool/core/EditFileTool.java`
- `src/main/java/io/imiocode/tool/core/BashTool.java`
- `src/main/java/io/imiocode/tool/core/GlobTool.java`
- `src/main/java/io/imiocode/tool/core/GrepTool.java`
- `src/test/java/io/imiocode/tool/core/CoreToolDescriptionTest.java`

**依赖：** T0

**步骤：**

1. 按 Plan 补齐六个工具的适用场景、限制、优先级和配合关系。
2. 必要时强化参数说明，但不改变参数名称、类型、必填项或风险级别。
3. 测试每个描述均包含其关键使用约束。
4. 对比修改前后 Schema，断言协议保持不变。

**验证：** 运行 `mvn -q -Dtest=CoreToolDescriptionTest,ReadFileToolTest,WriteFileToolTest,EditFileToolTest,BashToolTest,GlobToolTest,GrepToolTest test`，期望全部通过。

## T11：改造 Plan Mode 轮次提醒

**文件：**

- `src/main/java/io/imiocode/agent/PlanModePrompt.java`
- `src/test/java/io/imiocode/agent/PlanModePromptTest.java`

**依赖：** T4

**步骤：**

1. 保留现有只读工具集合和 `toolSelection` 行为。
2. 定义完整和精简两种固定提醒正文。
3. 实现第 1、6、11……轮返回完整提醒，其余轮返回精简提醒。
4. 所有 Plan 提醒使用 `ROUND` 作用域。
5. 正常模式不返回提醒，并校验 iteration 必须为正数。

**验证：** 运行 `mvn -q -Dtest=PlanModePromptTest test`，期望 1～11 轮节奏和 `/do` 正常模式断言全部通过。

## T12：让 Agent 注入任务环境和逐轮提醒

**文件：**

- `src/main/java/io/imiocode/agent/Agent.java`
- `src/test/java/io/imiocode/agent/AgentTest.java`

**依赖：** T5、T6、T9、T11

**步骤：**

1. 为 Agent 注入 `EnvironmentContextProvider` 和 `EnvironmentReminderFormatter`，保留兼容构造路径。
2. 在 `run()` 开始时仅调用一次环境采集。
3. 每轮组合同一环境提醒、会话提醒和当前 Plan 轮次提醒。
4. 删除原先任务开始时永久拼接 Plan 提醒的逻辑。
5. 保持历史/轨迹、工具选择、停止条件和重试路径不变。
6. 使用计数型假环境提供者测试一次采集、多轮复用和下一任务刷新。
7. 断言提醒不进入成功提交轨迹，失败轨迹规则不变。

**验证：** 运行 `mvn -q -Dtest=AgentTest test`，期望环境、Plan Mode 和既有 Agent Loop 测试全部通过。

## T13：装配统一 Prompt 管线

**文件：**

- `src/main/java/io/imiocode/llm/LlmClientFactory.java`
- `src/main/java/io/imiocode/ImioCodeApplication.java`
- `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java`
- `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicClient.java`
- `src/main/java/io/imiocode/llm/provider/deepseek/DeepSeekClient.java`

**依赖：** T2、T3、T6、T9、T12

**步骤：**

1. 在 LLM 客户端创建路径中实例化七个 Section、Builder 和单个 PromptAssembler。
2. 将同一个 PromptAssembler 注入当前 Provider 客户端。
3. 在应用入口使用启动时的 workspace 创建环境采集器。
4. 将环境采集器传给 Agent。
5. 保留 Provider 测试所需的兼容构造路径，避免公共调用方突然失效。

**验证：** 运行 `mvn -q -DskipTests compile`，期望应用主代码完整编译。

## T14：迁移 OpenAI Payload 序列化

**文件：**

- `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java`
- `src/test/java/io/imiocode/llm/provider/openai/OpenAiClientTest.java`

**依赖：** T13

**步骤：**

1. 在请求构造入口调用 `assembleApiPayload`。
2. 将稳定 System Prompt 写入 `instructions`。
3. 将组装后的消息写入 `input`，删除旧 reminders 到 instructions 的路径。
4. 从 Payload 工具列表生成 function tools。
5. 不发送 `cache_control`、`prompt_cache_options` 或模型受限的显式字段。
6. 从 `input_tokens_details` 解析 `cached_tokens` 和 `cache_write_tokens`。
7. Mock Server 测试 system、提醒角色、工具顺序、用户正文不变和 usage 映射。

**验证：** 运行 `mvn -q -Dtest=OpenAiClientTest,OpenAiRichEventTest test`，期望全部通过。

## T15：迁移 Anthropic Payload 与缓存断点

**文件：**

- `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicClient.java`
- `src/test/java/io/imiocode/llm/provider/anthropic/AnthropicClientTest.java`

**依赖：** T13

**步骤：**

1. 在请求构造入口调用 `assembleApiPayload`。
2. 将 System Prompt 编码为单个 system text block。
3. 当 system 缓存意图为 EPHEMERAL 时添加缓存标记。
4. 序列化组装后的 messages，移除旧 reminders 到 system 的路径。
5. 合并协议要求的相邻同角色外层消息，同时保留提醒为独立 text block。
6. 从 Payload 工具列表生成 tools，并只在最后一个工具上设置 ephemeral 标记。
7. 测试无工具、Plan 工具子集、提醒顺序、两个缓存断点及既有 usage 映射。

**验证：** 运行 `mvn -q -Dtest=AnthropicClientTest,AnthropicRichEventTest test`，期望全部通过。

## T16：迁移 DeepSeek Payload 与缓存 Usage

**文件：**

- `src/main/java/io/imiocode/llm/provider/deepseek/DeepSeekClient.java`
- `src/test/java/io/imiocode/llm/provider/deepseek/DeepSeekClientTest.java`

**依赖：** T13

**步骤：**

1. 在请求构造入口调用 `assembleApiPayload`。
2. 将稳定 System Prompt 写为 messages 中唯一的首条 system 消息。
3. 后续追加组装后的 user/assistant/tool 消息。
4. 删除旧 reminders 作为 system 消息的路径。
5. 从 Payload 工具列表生成 function tools，不添加缓存控制字段。
6. 优先解析 `prompt_cache_hit_tokens`，再兼容旧的 `prompt_tokens_details.cached_tokens`。
7. Mock Server 测试 system 唯一性、提醒 user 角色、工具顺序、官方 usage 和旧 usage 回退。

**验证：** 运行 `mvn -q -Dtest=DeepSeekClientTest,DeepSeekRichEventTest test`，期望全部通过。

## 提交点 C：Agent、工具与 Provider 接入

完成 T10～T16 后，运行相关测试并创建一次本地 Git 提交。提交只包含 Ch5 源码和测试。

## T17：补齐跨模块回归测试

**文件：**

- `src/test/java/io/imiocode/agent/AgentTest.java`
- `src/test/java/io/imiocode/conversation/ConversationSessionTest.java`
- `src/test/java/io/imiocode/llm/LlmClientContractTest.java`

**依赖：** T12、T14、T15、T16

**步骤：**

1. 验证多轮工具调用每轮均得到相同环境快照。
2. 验证会话提醒消费一次且不进入正式历史。
3. 验证 Plan Mode 与普通模式工具选择未回归。
4. 验证 Provider 无关的 LLM 客户端契约仍支持纯文本、工具和 usage。
5. 若现有测试已覆盖某项，仅补充缺失断言，不重复建立平行测试。

**验证：** 运行 `mvn -q -Dtest=AgentTest,ConversationSessionTest,LlmClientContractTest test`，期望全部通过。

## T18：编写五个定性评估场景

**文件：** `docs/ch5/eval-scenarios.md`  
**依赖：** T10、T11、T12

**步骤：**

1. 为项目探索、精确修改、创建文件、失败恢复、Plan Mode 各写一个场景。
2. 每个场景包含准备条件、用户输入、预期工具顺序、禁止行为和观察点。
3. 提供模型回复、工具轨迹、usage、结论和备注记录表。
4. 明确人工评估不等同于自动通过，不能伪造工具或缓存结果。

**验证：** 读取文档并搜索五个场景标题，期望每个场景均包含“输入”“预期工具行为”“观察点”“结果记录”四项。

## T19：执行完整构建与回归

**文件：** 必要时仅修改本章已经列出的实现或测试文件  
**依赖：** T1～T18

**步骤：**

1. 运行 Java 21 全量测试、编译和打包。
2. 检查测试数量与 T0 基线相比没有减少。
3. 检查可执行 JAR 已生成。
4. 检查 Git diff，不得包含 `config.yaml`、API Key、`claude.md` 或 `hello.txt`。
5. 若失败，修复后重新运行完整验证，不跳过失败测试。

**验证：** 运行 `mvn -q clean verify`，期望退出码为 0 且 `target` 中生成可执行 JAR。

## 提交点 D：文档与回归收尾

完成 T17～T19 后，创建本章最后一次本地 Git 提交，包含评估文档、回归测试及必要修复。提交后工作树只允许保留开发前已存在的用户改动。

## 执行顺序

```text
T0
├─ T1 → T2 → T3 ───────────────┐
├─ T4 → T5 → T6 ───────────────┤
└─ T8 ─────────────────────────┤
                                ↓
                    提交点 A → T7 → T9 → 提交点 B
                                      │
                 T10 ─────────────────┤
                 T11 ─────────────────┤
                                      ↓
                          T12 → T13
                                ├─ T14
                                ├─ T15
                                └─ T16
                                      ↓
                            提交点 C → T17
                                      ├─ T18
                                      └─ T19
                                           ↓
                                      提交点 D
```

T10 可以在 T7～T9 期间独立进行，但实际执行仍应避免与同一文件的其他修改交错。

## 兼容补充任务

### 补充文件清单

| 操作 | 文件 | 职责 |
|---|---|---|
| 新建 | `src/main/java/io/imiocode/prompt/BuildOptions.java` | 三个可选稳定内容插槽 |
| 新建 | `src/main/java/io/imiocode/prompt/section/CustomInstructionsSection.java` | 自定义指令模块 |
| 新建 | `src/main/java/io/imiocode/prompt/section/SkillSection.java` | Skill 文本模块 |
| 新建 | `src/main/java/io/imiocode/prompt/section/MemorySection.java` | Memory 文本模块 |
| 修改 | `src/main/java/io/imiocode/prompt/SectionPriority.java` | 增加三个可选模块优先级 |
| 修改 | `src/main/java/io/imiocode/prompt/SystemPromptBuilder.java` | 空构造、链式注册和带选项默认入口 |
| 修改 | `src/main/java/io/imiocode/prompt/EnvironmentContext.java` | 架构、模型和 Git 仓库判断 |
| 修改 | `src/main/java/io/imiocode/prompt/EnvironmentContextCollector.java` | 分字段采集 OS/架构并接收模型 |
| 修改 | `src/main/java/io/imiocode/prompt/EnvironmentReminderFormatter.java` | 输出新增环境字段 |
| 修改 | `src/main/java/io/imiocode/agent/PlanModePrompt.java` | 退出 Plan Mode 提醒模板 |
| 修改 | `src/main/java/io/imiocode/agent/Agent.java` | 原子模式状态与一次性提醒消费 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 将配置模型传给环境采集器 |
| 修改 | `src/test/java/io/imiocode/prompt/SystemPromptBuilderTest.java` | 增量 Builder 和可选模块测试 |
| 修改 | `src/test/java/io/imiocode/prompt/EnvironmentContextCollectorTest.java` | 新环境字段采集测试 |
| 修改 | `src/test/java/io/imiocode/prompt/EnvironmentReminderFormatterTest.java` | 新环境字段与缓存隔离测试 |
| 修改 | `src/test/java/io/imiocode/agent/PlanModePromptTest.java` | 退出提醒模板测试 |
| 修改 | `src/test/java/io/imiocode/agent/AgentTest.java` | 模式切换生命周期测试 |

## T20：扩展可选模块优先级与配置

**文件：**

- `src/main/java/io/imiocode/prompt/SectionPriority.java`
- `src/main/java/io/imiocode/prompt/BuildOptions.java`

**依赖：** T19

**步骤：**

1. 在七个核心优先级之后增加 CustomInstructions、Skill、Memory 三个稳定优先级。
2. 定义不可变 `BuildOptions`，保存三个 Optional 文本字段。
3. 将 null、空字符串和纯空白归一为空 Optional，非空内容去除首尾空白。
4. 提供三字符串兼容构造和 `empty()`。

**验证：** 使用 Java 21 运行 `mvn -q -DskipTests compile`，期望编译通过。

## T21：实现三个可选 Prompt 模块

**文件：**

- `src/main/java/io/imiocode/prompt/section/CustomInstructionsSection.java`
- `src/main/java/io/imiocode/prompt/section/SkillSection.java`
- `src/main/java/io/imiocode/prompt/section/MemorySection.java`

**依赖：** T20

**步骤：**

1. 每个类只接收已提供文本，不读取文件、环境或外部状态。
2. 分别生成名称固定为“自定义指令”“Skill”“Memory”的 Section。
3. 使用 T20 定义的对应优先级。
4. 复用 Section 的内容校验与不可变行为。

**验证：** 运行 `mvn -q -DskipTests compile`，期望三个模块编译通过。

## T22：扩展 Builder 并验证确定性

**文件：**

- `src/main/java/io/imiocode/prompt/SystemPromptBuilder.java`
- `src/test/java/io/imiocode/prompt/SystemPromptBuilderTest.java`

**依赖：** T20、T21

**步骤：**

1. 增加空构造器，保留现有列表构造器。
2. 增加返回当前 Builder 的 `add`，立即拒绝 null。
3. `build` 使用模块快照执行重名检查、空内容过滤和稳定排序。
4. 增加 `defaults(BuildOptions)`，按核心七模块、CustomInstructions、Skill、Memory 注册。
5. 测试乱序链式注册、重复构建、重名、空配置、单配置及全配置顺序。
6. 断言 `defaults()` 与 `defaults(BuildOptions.empty())` 完全一致。

**验证：** 运行 `mvn -q -Dtest=SystemPromptBuilderTest test`，期望全部通过。

## T23：扩展环境上下文模型

**文件：**

- `src/main/java/io/imiocode/prompt/EnvironmentContext.java`
- `src/main/java/io/imiocode/prompt/EnvironmentContextCollector.java`

**依赖：** T19

**步骤：**

1. 为环境上下文增加 architecture 和 model。
2. 保留旧五参数构造器，并为新增字段使用固定 `unknown`。
3. 根据 Git 状态实现 `Optional<Boolean> isGitRepository()`。
4. 为采集器增加接收模型的构造器，旧构造器继续使用固定 `unknown`。
5. 将 OS 名称和架构拆成两个系统属性采集结果。

**验证：** 运行 `mvn -q -Dtest=EnvironmentContextCollectorTest test`，期望兼容和新增字段断言全部通过。

## T24：格式化扩展环境提醒

**文件：**

- `src/main/java/io/imiocode/prompt/EnvironmentReminderFormatter.java`
- `src/test/java/io/imiocode/prompt/EnvironmentReminderFormatterTest.java`

**依赖：** T23

**步骤：**

1. 按固定顺序加入架构、Git 仓库状态和模型。
2. Git 仓库状态分别显示是、否、未知。
3. 保持 dirty 文件名、命令输出和环境变量值不进入提醒。
4. 测试完整字段、三态 Git 仓库值和 XML 包装。
5. 断言新增动态字段不会出现在默认 System Prompt。

**验证：** 运行 `mvn -q -Dtest=EnvironmentReminderFormatterTest,SystemPromptBuilderTest test`，期望全部通过。

## T25：把当前模型接入应用环境采集

**文件：** `src/main/java/io/imiocode/ImioCodeApplication.java`

**依赖：** T23

**步骤：**

1. 使用新增采集器构造器传入 `config.model()`。
2. 保持工作区、Clock 和 Git 超时设置不变。
3. 不把模型添加到 System Prompt Builder 或 Provider system 字段。

**验证：** 运行 `mvn -q -DskipTests compile`，期望应用主代码编译通过。

## T26：实现退出 Plan Mode 一次性提醒

**文件：**

- `src/main/java/io/imiocode/agent/PlanModePrompt.java`
- `src/main/java/io/imiocode/agent/Agent.java`

**依赖：** T19

**步骤：**

1. 在 `PlanModePrompt` 定义固定 `ROUND` 退出提醒。
2. 用单个 `AtomicReference<ModeState>` 替换独立模式原子值。
3. Plan→DO 时设置 pending，DO→DO 不新增，切回 Plan 时清除。
4. 任务启动时用 CAS 原子取得模式并消费 pending。
5. 只在下一普通任务 iteration=1 注入退出提醒。
6. 保持 TaskStarted、ModeChanged、工具选择和 Plan 周期提醒行为不变。

**验证：** 运行 `mvn -q -DskipTests compile`，期望 Agent 主代码编译通过。

## T27：验证模式切换生命周期

**文件：**

- `src/test/java/io/imiocode/agent/PlanModePromptTest.java`
- `src/test/java/io/imiocode/agent/AgentTest.java`

**依赖：** T26

**步骤：**

1. 测试退出提醒的 ROUND 作用域和固定内容。
2. 测试初始 DO 不注入。
3. 测试 Plan→DO 后下一任务第一轮恰好注入一次，第二轮及再下一任务不注入。
4. 测试 Plan→DO→Plan 会取消 pending。
5. 测试 DO→DO 不产生退出提醒。
6. 断言退出提醒不进入最终 trajectory。

**验证：** 运行 `mvn -q -Dtest=PlanModePromptTest,AgentTest,ConversationLoopTest test`，期望全部通过。

## T28：执行补充功能完整回归

**文件：** 本补充文件清单中涉及的实现、测试与 Ch5 文档

**依赖：** T20～T27

**步骤：**

1. 运行所有补充功能定向测试。
2. 运行 Java 21 `mvn -q clean verify`。
3. 比较测试数量，不得低于当前 185 项基线。
4. 检查可执行 JAR 仍能生成和启动。
5. 检查 Git diff 和暂存范围，排除 `config.yaml`、`claude.md` 和 `hello.txt`。

**验证：** `mvn -q clean verify` 退出码为 0，Surefire 失败数和错误数均为 0，并生成 `target/imiocode-0.2.0-SNAPSHOT-all.jar`。

## 提交点 E：兼容补充

完成 T20～T28 后创建新的本地提交。提交只包含补充 spec、plan、task、checklist、实现和测试，不包含用户原有文件改动。

## 补充执行顺序

```text
T20 → T21 → T22 ──────────────┐
T23 → T24 → T25 ──────────────┼→ T28 → 提交点 E
T26 → T27 ────────────────────┘
```
