# ImioCode 第三章：工具系统 Tasks

> 每个任务是一个聚焦工作单元。实现阶段必须按依赖顺序执行，并在验证通过后再继续。

## 文件清单

### 新建主代码

| 文件 | 职责 |
|---|---|
| `src/main/java/io/imiocode/conversation/MessagePart.java` | 统一消息部分接口 |
| `src/main/java/io/imiocode/conversation/TextPart.java` | 文本消息部分 |
| `src/main/java/io/imiocode/conversation/ToolCallPart.java` | 工具调用消息部分 |
| `src/main/java/io/imiocode/conversation/ToolResultPart.java` | 工具结果消息部分 |
| `src/main/java/io/imiocode/conversation/ConversationListener.java` | 对话文本与工具事件监听 |
| `src/main/java/io/imiocode/conversation/ConversationException.java` | 会话层安全错误 |
| `src/main/java/io/imiocode/tool/Tool.java` | 工具接口 |
| `src/main/java/io/imiocode/tool/BaseTool.java` | 工具通用执行模板 |
| `src/main/java/io/imiocode/tool/ToolRisk.java` | 工具风险等级 |
| `src/main/java/io/imiocode/tool/ToolDefinition.java` | 工具定义 |
| `src/main/java/io/imiocode/tool/ToolCall.java` | 完整工具调用 |
| `src/main/java/io/imiocode/tool/ToolResult.java` | 统一工具结果 |
| `src/main/java/io/imiocode/tool/ToolLimits.java` | 固定资源限制 |
| `src/main/java/io/imiocode/tool/SecretRedactor.java` | 已知秘密脱敏与环境清理 |
| `src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java` | 厂商工具定义编码入口 |
| `src/main/java/io/imiocode/tool/ToolRegistry.java` | 工具注册、开关和导出 |
| `src/main/java/io/imiocode/tool/ToolExecution.java` | 调用与结果关联 |
| `src/main/java/io/imiocode/tool/ToolExecutionState.java` | 工具执行状态 |
| `src/main/java/io/imiocode/tool/ToolExecutionEvent.java` | 工具生命周期事件 |
| `src/main/java/io/imiocode/tool/ToolExecutionListener.java` | 工具事件监听 |
| `src/main/java/io/imiocode/tool/ToolExecutor.java` | 串行工具执行与取消 |
| `src/main/java/io/imiocode/tool/workspace/WorkspacePolicy.java` | 工作区路径安全 |
| `src/main/java/io/imiocode/tool/workspace/WorkspaceWalker.java` | 安全稳定目录遍历 |
| `src/main/java/io/imiocode/tool/workspace/Utf8TextFile.java` | 有界 UTF-8 文本读取 |
| `src/main/java/io/imiocode/tool/workspace/AtomicFileWriter.java` | 临时写入与替换 |
| `src/main/java/io/imiocode/tool/core/ReadFileTool.java` | 读取文件工具 |
| `src/main/java/io/imiocode/tool/core/WriteFileTool.java` | 写入文件工具 |
| `src/main/java/io/imiocode/tool/core/EditFileTool.java` | 精确编辑工具 |
| `src/main/java/io/imiocode/tool/core/BashTool.java` | 跨平台命令工具 |
| `src/main/java/io/imiocode/tool/core/GlobPattern.java` | 跨平台 Glob 匹配 |
| `src/main/java/io/imiocode/tool/core/GlobTool.java` | 文件名搜索工具 |
| `src/main/java/io/imiocode/tool/core/GrepTool.java` | 正则文本搜索工具 |
| `src/main/java/io/imiocode/llm/ToolCallAssembler.java` | 流式参数碎片组装 |
| `src/main/java/io/imiocode/llm/ToolResultJson.java` | 工具结果 JSON 编码 |
| `src/main/java/io/imiocode/terminal/ToolSummaryFormatter.java` | 工具输入和结果摘要 |

### 修改主代码

| 文件 | 职责变化 |
|---|---|
| `src/main/java/io/imiocode/ImioCodeApplication.java` | 装配工具系统和关闭链路 |
| `src/main/java/io/imiocode/conversation/MessageRole.java` | 增加内部工具角色 |
| `src/main/java/io/imiocode/conversation/ChatMessage.java` | 支持消息部分列表 |
| `src/main/java/io/imiocode/conversation/ChatRequest.java` | 使用扩展消息 |
| `src/main/java/io/imiocode/conversation/ChatResponse.java` | 同时承载文本和工具调用 |
| `src/main/java/io/imiocode/conversation/ConversationSession.java` | 编排一次工具轮次 |
| `src/main/java/io/imiocode/conversation/ConversationLoop.java` | 工具 UI、错误和取消 |
| `src/main/java/io/imiocode/llm/LlmClientFactory.java` | 注入工具注册中心 |
| `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java` | OpenAI 工具协议 |
| `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicClient.java` | Anthropic 工具协议 |
| `src/main/java/io/imiocode/llm/provider/deepseek/DeepSeekClient.java` | DeepSeek 工具协议 |
| `src/main/java/io/imiocode/terminal/TerminalUi.java` | 增加工具事件入口 |
| `src/main/java/io/imiocode/terminal/JLineTerminalUi.java` | 渲染工具过程 |
| `src/main/java/io/imiocode/terminal/TerminalLayout.java` | 工具状态布局 |
| `src/main/java/io/imiocode/terminal/UiState.java` | 增加工具状态 |

### 新建测试

| 文件 | 覆盖范围 |
|---|---|
| `src/test/java/io/imiocode/tool/BaseToolTest.java` | 通用执行、异常和截断 |
| `src/test/java/io/imiocode/tool/SecretRedactorTest.java` | 密钥及认证头脱敏 |
| `src/test/java/io/imiocode/tool/ToolRegistryTest.java` | 注册、开关和导出 |
| `src/test/java/io/imiocode/tool/ToolExecutorTest.java` | 串行、失败继续和取消 |
| `src/test/java/io/imiocode/tool/workspace/WorkspacePolicyTest.java` | 路径与敏感文件保护 |
| `src/test/java/io/imiocode/tool/workspace/WorkspaceWalkerTest.java` | 稳定遍历和扫描限制 |
| `src/test/java/io/imiocode/tool/workspace/AtomicFileWriterTest.java` | 创建、覆盖和失败清理 |
| `src/test/java/io/imiocode/tool/core/ReadFileToolTest.java` | 读取范围、编码和截断 |
| `src/test/java/io/imiocode/tool/core/WriteFileToolTest.java` | 创建、覆盖和父目录错误 |
| `src/test/java/io/imiocode/tool/core/EditFileToolTest.java` | 精确唯一替换 |
| `src/test/java/io/imiocode/tool/core/BashToolTest.java` | 命令、输出、超时和取消 |
| `src/test/java/io/imiocode/tool/core/GlobToolTest.java` | Glob 语义、顺序和限制 |
| `src/test/java/io/imiocode/tool/core/GrepToolTest.java` | 正则搜索、行号和限制 |
| `src/test/java/io/imiocode/llm/ToolCallAssemblerTest.java` | 碎片、交错和无效 JSON |
| `src/test/java/io/imiocode/llm/ToolResultJsonTest.java` | 统一结果格式 |
| `src/test/java/io/imiocode/terminal/ToolSummaryFormatterTest.java` | 安全有界摘要 |

### 修改测试

| 文件 | 覆盖变化 |
|---|---|
| `src/test/java/io/imiocode/conversation/ConversationSessionTest.java` | 工具轮次、历史事务和单轮限制 |
| `src/test/java/io/imiocode/conversation/ConversationLoopTest.java` | 工具 UI、失败和中断 |
| `src/test/java/io/imiocode/llm/LlmClientContractTest.java` | 三家统一工具契约 |
| `src/test/java/io/imiocode/llm/provider/openai/OpenAiClientTest.java` | OpenAI 工具请求与流解析 |
| `src/test/java/io/imiocode/llm/provider/anthropic/AnthropicClientTest.java` | Anthropic 工具请求与流解析 |
| `src/test/java/io/imiocode/llm/provider/deepseek/DeepSeekClientTest.java` | DeepSeek 工具请求与流解析 |
| `src/test/java/io/imiocode/terminal/TerminalLayoutTest.java` | 工具状态布局 |
| `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java` | 工具事件渲染和 ANSI 隔离 |

`claude.md` 不在文件清单内，实施时不得覆盖其现有修改。

## T1：定义工具基础值对象

**文件：**

- `src/main/java/io/imiocode/tool/ToolRisk.java`
- `src/main/java/io/imiocode/tool/ToolDefinition.java`
- `src/main/java/io/imiocode/tool/ToolCall.java`
- `src/main/java/io/imiocode/tool/ToolResult.java`
- `src/main/java/io/imiocode/tool/ToolLimits.java`

**依赖：** 无

**步骤：**

1. 按 plan 定义风险、描述、调用、结果和限制类型。
2. 对名称、说明、参数对象、时长和正数限制执行构造校验。
3. 深拷贝 JSON Schema 和调用参数，复制可变输入。
4. 为结果提供成功、失败、超时、中断及替换耗时的构造入口。
5. 提供包含全部固定数值的默认 `ToolLimits`。

**验证：** 运行 `mvn -q -DskipTests compile`，期望工具值对象全部编译通过。

## T2：实现敏感信息脱敏

**文件：**

- `src/main/java/io/imiocode/tool/SecretRedactor.java`
- `src/test/java/io/imiocode/tool/SecretRedactorTest.java`

**依赖：** T1

**步骤：**

1. 保存当前配置 API Key 的不可变副本，不在 `toString()` 中输出。
2. 脱敏精确 Key、Bearer 值和 `x-api-key` 等认证字段。
3. 实现大小写不敏感的敏感环境变量名判断。
4. 测试中文文本、空文本、短 Key、认证头及普通内容不被误改。

**验证：** 运行 `mvn -q -Dtest=SecretRedactorTest test`，期望全部测试通过且断言输出不含测试密钥。

## T3：实现 Tool 与 BaseTool

**文件：**

- `src/main/java/io/imiocode/tool/Tool.java`
- `src/main/java/io/imiocode/tool/BaseTool.java`
- `src/test/java/io/imiocode/tool/BaseToolTest.java`

**依赖：** T1、T2

**步骤：**

1. 定义工具描述、执行和默认取消接口。
2. 在最终执行模板中处理空参数、耗时、异常和统一结果。
3. 在返回前执行结果字节限制和 `SecretRedactor`。
4. 为参数错误、运行时异常、截断及正常成功编写测试替身。

**验证：** 运行 `mvn -q -Dtest=BaseToolTest test`，期望异常均转成失败结果，超长输出被标记截断。

## T4：实现工具注册中心

**文件：**

- `src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java`
- `src/main/java/io/imiocode/tool/ToolRegistry.java`
- `src/test/java/io/imiocode/tool/ToolRegistryTest.java`

**依赖：** T1、T3

**步骤：**

1. 实现按名称注册并默认启用。
2. 重名注册和未知名称开关返回明确错误。
3. `findEnabled` 仅返回已启用实例。
4. 启用定义按名称稳定排序，并通过编码回调导出。
5. 覆盖注册、禁用、重新启用、未知工具和导出顺序。

**验证：** 运行 `mvn -q -Dtest=ToolRegistryTest test`，期望注册及开关行为全部通过。

## T5：实现串行工具执行器

**文件：**

- `src/main/java/io/imiocode/tool/ToolExecution.java`
- `src/main/java/io/imiocode/tool/ToolExecutionState.java`
- `src/main/java/io/imiocode/tool/ToolExecutionEvent.java`
- `src/main/java/io/imiocode/tool/ToolExecutionListener.java`
- `src/main/java/io/imiocode/tool/ToolExecutor.java`
- `src/test/java/io/imiocode/tool/ToolExecutorTest.java`

**依赖：** T1、T3、T4

**步骤：**

1. 定义调用、结果和生命周期事件。
2. 按输入列表同步查询并执行工具。
3. 为未知及禁用工具生成失败结果并继续后续调用。
4. 用原子引用保存活动工具，取消时调用其 `cancel()` 并停止待执行调用。
5. 测试事件顺序、串行顺序、失败继续、禁用工具和取消。

**验证：** 运行 `mvn -q -Dtest=ToolExecutorTest test`，期望事件和结果顺序与调用顺序完全一致。

## T6：扩展统一消息模型

**文件：**

- `src/main/java/io/imiocode/conversation/MessagePart.java`
- `src/main/java/io/imiocode/conversation/TextPart.java`
- `src/main/java/io/imiocode/conversation/ToolCallPart.java`
- `src/main/java/io/imiocode/conversation/ToolResultPart.java`
- `src/main/java/io/imiocode/conversation/MessageRole.java`
- `src/main/java/io/imiocode/conversation/ChatMessage.java`
- `src/main/java/io/imiocode/conversation/ChatRequest.java`
- `src/main/java/io/imiocode/conversation/ChatResponse.java`

**依赖：** T1

**步骤：**

1. 定义密封消息部分及三个不可变实现。
2. 增加内部 `TOOL` 角色和角色—消息部分组合校验。
3. 将请求和响应扩展为消息部分列表。
4. 提供纯文本构造、`content()` 兼容读取、`text()`、`toolCalls()` 和 `hasToolCalls()`。
5. 确保纯工具响应合法，空响应非法。

**验证：** 运行 `mvn -q -DskipTests compile`，期望现有纯文本调用方仍可编译。

## T7：定义会话事件与安全异常

**文件：**

- `src/main/java/io/imiocode/conversation/ConversationListener.java`
- `src/main/java/io/imiocode/conversation/ConversationException.java`

**依赖：** T5、T6

**步骤：**

1. 定义响应开始、文本增量、响应结束和工具事件回调。
2. 提供无操作默认实现或文本监听适配入口。
3. 在会话异常中保存安全消息、可恢复、中断和工具已执行状态。
4. 确保异常字符串不包含底层响应正文或工具参数。

**验证：** 运行 `mvn -q -DskipTests compile`，期望会话事件类型可被主代码引用。

## T8：实现工作区路径策略

**文件：**

- `src/main/java/io/imiocode/tool/workspace/WorkspacePolicy.java`
- `src/test/java/io/imiocode/tool/workspace/WorkspacePolicyTest.java`

**依赖：** T1

**步骤：**

1. 保存绝对规范工作区并验证其存在且为目录。
2. 拒绝空路径、绝对路径、`..` 和工作区前缀逃逸。
3. 集中拒绝 `config.yaml`、`.env`、`.env.*` 和 `.git/**`。
4. 逐级检查符号链接及 Windows 重解析点。
5. 分别实现已有文件和可写目标解析，并提供遍历过滤。
6. 使用临时目录测试普通路径、中文路径、敏感路径和链接逃逸。

**验证：** 运行 `mvn -q -Dtest=WorkspacePolicyTest test`，期望全部逃逸和敏感路径被拒绝。

## T9：实现有界 UTF-8 文本读取

**文件：**

- `src/main/java/io/imiocode/tool/workspace/Utf8TextFile.java`
- `src/test/java/io/imiocode/tool/core/ReadFileToolTest.java`

**依赖：** T1、T8

**步骤：**

1. 使用严格 UTF-8 解码器逐块或逐行读取。
2. 检测 NUL 字节和非法 UTF-8，并返回安全错误。
3. 在读取过程中执行字节、行数和取消限制。
4. 暂在 ReadFile 测试中加入底层中文、二进制和非法编码用例。

**验证：** 运行 `mvn -q -Dtest=ReadFileToolTest test`，期望底层文本读取相关用例通过。

## T10：实现原子文件写入

**文件：**

- `src/main/java/io/imiocode/tool/workspace/AtomicFileWriter.java`
- `src/test/java/io/imiocode/tool/workspace/AtomicFileWriterTest.java`

**依赖：** T1、T8

**步骤：**

1. 在目标父目录创建唯一临时文件并写入 UTF-8。
2. 写入后重新执行路径安全校验。
3. 优先原子移动，失败时执行经过校验的普通替换。
4. 任意失败都删除临时文件且保留原目标。
5. 测试创建、覆盖、移动失败和清理。

**验证：** 运行 `mvn -q -Dtest=AtomicFileWriterTest test`，期望无失败场景遗留临时文件或半写目标。

## T11：实现安全稳定的工作区遍历

**文件：**

- `src/main/java/io/imiocode/tool/workspace/WorkspaceWalker.java`
- `src/test/java/io/imiocode/tool/workspace/WorkspaceWalkerTest.java`

**依赖：** T1、T8

**步骤：**

1. 实现不跟随链接的确定性深度优先遍历。
2. 每级使用有界字典序选择，避免无界保存目录项。
3. 跳过敏感路径并统计已扫描路径数。
4. 达到扫描上限或取消标志时立即结束并返回截断状态。
5. 测试乱序创建文件、敏感目录、链接和扫描上限。

**验证：** 运行 `mvn -q -Dtest=WorkspaceWalkerTest test`，期望不同创建顺序得到相同遍历结果。

## T12：实现 ReadFile 工具

**文件：**

- `src/main/java/io/imiocode/tool/core/ReadFileTool.java`
- `src/test/java/io/imiocode/tool/core/ReadFileToolTest.java`

**依赖：** T3、T8、T9

**步骤：**

1. 定义 `read_file` Schema 和 LOW 风险元信息。
2. 校验 `path`、`start_line` 和 `end_line`。
3. 使用安全路径和有界文本读取，并生成带行号输出。
4. 覆盖完整读取、范围读取、EOF、无效范围、中文、敏感路径和截断测试。

**验证：** 运行 `mvn -q -Dtest=ReadFileToolTest test`，期望读取行为和截断标记符合 plan。

## T13：实现 WriteFile 工具

**文件：**

- `src/main/java/io/imiocode/tool/core/WriteFileTool.java`
- `src/test/java/io/imiocode/tool/core/WriteFileToolTest.java`

**依赖：** T3、T8、T10

**步骤：**

1. 定义 `write_file` Schema 和 MEDIUM 风险元信息。
2. 在写入前检查 UTF-8 字节数和父目录存在性。
3. 通过 `AtomicFileWriter` 创建或完整覆盖。
4. 测试创建、覆盖、中文、父目录缺失、超限、敏感路径和链接目标。

**验证：** 运行 `mvn -q -Dtest=WriteFileToolTest test`，期望失败场景不改变原文件。

## T14：实现 EditFile 工具

**文件：**

- `src/main/java/io/imiocode/tool/core/EditFileTool.java`
- `src/test/java/io/imiocode/tool/core/EditFileToolTest.java`

**依赖：** T3、T8、T9、T10

**步骤：**

1. 定义 `edit_file` Schema 和 MEDIUM 风险元信息。
2. 有界读取完整文件并统计旧文本非重叠出现次数。
3. 仅在恰好出现一次时构造新内容并原子替换。
4. 测试零次、一次、多次、空新文本、中文、超限和敏感路径。

**验证：** 运行 `mvn -q -Dtest=EditFileToolTest test`，期望零次及多次匹配时文件字节完全不变。

## T15：实现跨平台 GlobPattern

**文件：**

- `src/main/java/io/imiocode/tool/core/GlobPattern.java`
- `src/test/java/io/imiocode/tool/core/GlobToolTest.java`

**依赖：** T1

**步骤：**

1. 将使用 `/` 的模式安全转换为内部正则。
2. 实现 `*`、`**` 和 `?` 的路径段语义。
3. 转义普通正则字符并拒绝无效模式。
4. 在 Glob 测试中加入根文件、嵌套文件、中文和分隔符用例。

**验证：** 运行 `mvn -q -Dtest=GlobToolTest test`，期望模式匹配相关用例通过。

## T16：实现 Glob 工具

**文件：**

- `src/main/java/io/imiocode/tool/core/GlobTool.java`
- `src/test/java/io/imiocode/tool/core/GlobToolTest.java`

**依赖：** T3、T11、T15

**步骤：**

1. 定义 `glob` Schema 和 LOW 风险元信息。
2. 使用 `WorkspaceWalker` 过滤普通文件并匹配相对路径。
3. 返回统一 `/` 路径、稳定排序和截断状态。
4. 测试结果上限、扫描上限、敏感路径省略及链接省略。

**验证：** 运行 `mvn -q -Dtest=GlobToolTest test`，期望结果稳定且受保护文件不出现。

## T17：实现 Grep 工具

**文件：**

- `src/main/java/io/imiocode/tool/core/GrepTool.java`
- `src/test/java/io/imiocode/tool/core/GrepToolTest.java`

**依赖：** T3、T8、T9、T11

**步骤：**

1. 定义 `grep` Schema 和 LOW 风险元信息。
2. 校验正则及可选文件或目录范围。
3. 按稳定遍历顺序逐文件、逐行执行 `find()`。
4. 输出相对路径、行号和有界匹配行。
5. 测试无效正则、中文、二进制跳过、敏感路径、超长行及结果限制。

**验证：** 运行 `mvn -q -Dtest=GrepToolTest test`，期望匹配顺序和行号稳定，输出限制生效。

## T18：实现 Bash 基础执行

**文件：**

- `src/main/java/io/imiocode/tool/core/BashTool.java`
- `src/test/java/io/imiocode/tool/core/BashToolTest.java`

**依赖：** T3、T8

**步骤：**

1. 定义 `bash` Schema 和 HIGH 风险元信息。
2. 按操作系统构造 PowerShell 或 `/bin/bash` 参数列表。
3. 固定工作目录并执行模型提供的原始命令参数。
4. 使用两个虚拟线程排空 stdout 和 stderr。
5. 测试成功命令、非零退出码、工作目录和中文输出。

**验证：** 运行 `mvn -q -Dtest=BashToolTest test`，期望当前操作系统的基础命令结果包含正确退出码和输出。

## T19：补齐 Bash 限制、脱敏和取消

**文件：**

- `src/main/java/io/imiocode/tool/core/BashTool.java`
- `src/test/java/io/imiocode/tool/core/BashToolTest.java`

**依赖：** T2、T5、T18

**步骤：**

1. 启动前移除名称含敏感片段的环境变量。
2. 对 stdout、stderr 分别执行容量限制并持续排空超出内容。
3. 实现 30 秒超时、活动进程原子引用和幂等取消。
4. 按子孙后主进程顺序终止，并在宽限后强制终止。
5. 测试输出截断、环境清理、短超时、显式取消和重复关闭。

**验证：** 运行 `mvn -q -Dtest=BashToolTest test`，期望超时及取消进程退出，结果不含测试密钥。

## T20：实现工具结果 JSON

**文件：**

- `src/main/java/io/imiocode/llm/ToolResultJson.java`
- `src/test/java/io/imiocode/llm/ToolResultJsonTest.java`

**依赖：** T1、T2

**步骤：**

1. 使用 Jackson 节点构造统一结果 JSON，不拼接字符串。
2. 输出成功、输出、错误、截断、毫秒耗时和可选退出码。
3. 编码前再次执行已知秘密脱敏。
4. 测试成功、失败、Bash 结果、Unicode 和转义字符。

**验证：** 运行 `mvn -q -Dtest=ToolResultJsonTest test`，期望结果可重新解析且字段完整。

## T21：实现 ToolCallAssembler

**文件：**

- `src/main/java/io/imiocode/llm/ToolCallAssembler.java`
- `src/test/java/io/imiocode/llm/ToolCallAssemblerTest.java`

**依赖：** T1

**步骤：**

1. 按整数位置维护独立 ID、名称和参数缓冲区。
2. 严格按调用顺序追加传入碎片。
3. 完成时按位置排序并解析 JSON 对象。
4. 缺少 ID、名称、参数或收到非对象 JSON 时抛出安全协议异常。
5. 测试单调用、交错调用、Unicode 碎片和全部错误场景。

**验证：** 运行 `mvn -q -Dtest=ToolCallAssemblerTest test`，期望交错碎片还原为互不混淆的调用。

## T22：为 OpenAI 请求加入工具定义

**文件：**

- `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java`
- `src/test/java/io/imiocode/llm/provider/openai/OpenAiClientTest.java`

**依赖：** T4、T6、T20

**步骤：**

1. 向客户端注入 `ToolRegistry`，保留测试可用的兼容构造入口。
2. 将已启用定义映射为 Responses API 函数工具。
3. 显式使用非严格模式并保留现有模型、流式和 token 字段。
4. 测试默认工具、禁用工具和纯文本请求不回退。

**验证：** 运行 `mvn -q -Dtest=OpenAiClientTest test`，期望捕获请求只包含已启用工具。

## T23：实现 OpenAI 工具消息映射

**文件：**

- `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java`
- `src/test/java/io/imiocode/llm/provider/openai/OpenAiClientTest.java`

**依赖：** T6、T20、T22

**步骤：**

1. 映射普通用户及助手文本输入项。
2. 将助手工具调用映射为 `function_call`。
3. 将工具结果映射为与 `call_id` 关联的 `function_call_output`。
4. 测试混合文本、多个调用、成功及失败结果的请求 JSON。

**验证：** 运行 `mvn -q -Dtest=OpenAiClientTest test`，期望请求中的调用标识与结果标识一一对应。

## T24：实现 OpenAI 流式工具解析

**文件：**

- `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java`
- `src/test/java/io/imiocode/llm/provider/openai/OpenAiClientTest.java`

**依赖：** T21、T23

**步骤：**

1. 从输出项事件保存 `output_index`、`call_id` 和名称。
2. 将函数参数 delta 按位置送入组装器。
3. 保留文本 delta，并允许纯工具或混合响应。
4. 正常完成后生成统一响应；失败、不完整或无效 JSON 转成协议错误。
5. 测试碎片、交错调用、混合文本、纯工具及缺失完成事件。

**验证：** 运行 `mvn -q -Dtest=OpenAiClientTest test`，期望 OpenAI 工具流全部解析用例通过。

## T25：实现 Anthropic 工具定义与消息映射

**文件：**

- `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicClient.java`
- `src/test/java/io/imiocode/llm/provider/anthropic/AnthropicClientTest.java`

**依赖：** T4、T6、T20

**步骤：**

1. 注入注册中心并导出 `name`、`description`、`input_schema`。
2. 将助手工具调用映射为 `tool_use` 内容块。
3. 将多个工具结果作为紧邻的用户 `tool_result` 内容块。
4. 失败结果设置 `is_error: true`。
5. 测试工具开关、混合内容、多结果及纯文本请求。

**验证：** 运行 `mvn -q -Dtest=AnthropicClientTest test`，期望工具结果紧邻调用且顺序一致。

## T26：实现 Anthropic 流式工具解析

**文件：**

- `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicClient.java`
- `src/test/java/io/imiocode/llm/provider/anthropic/AnthropicClientTest.java`

**依赖：** T21、T25

**步骤：**

1. 在 `content_block_start` 中识别 `tool_use` 的索引、ID 和名称。
2. 追加 `input_json_delta.partial_json`。
3. 处理内容块结束、`stop_reason` 和 `message_stop`。
4. 保留文本块并允许纯工具响应。
5. 测试交错内容块、无效 JSON、错误事件及不完整流。

**验证：** 运行 `mvn -q -Dtest=AnthropicClientTest test`，期望工具内容块按索引正确还原。

## T27：实现 DeepSeek 工具定义与消息映射

**文件：**

- `src/main/java/io/imiocode/llm/provider/deepseek/DeepSeekClient.java`
- `src/test/java/io/imiocode/llm/provider/deepseek/DeepSeekClientTest.java`

**依赖：** T4、T6、T20

**步骤：**

1. 注入注册中心并映射 Chat Completions 函数工具。
2. 将助手调用放入 `tool_calls` 数组。
3. 将每个结果拆成独立 `tool` 角色消息。
4. 保持 `tool_call_id`、内容和消息顺序。
5. 测试启用定义、多个结果、失败结果和纯文本请求。

**验证：** 运行 `mvn -q -Dtest=DeepSeekClientTest test`，期望所有工具消息均关联正确调用标识。

## T28：实现 DeepSeek 流式工具解析

**文件：**

- `src/main/java/io/imiocode/llm/provider/deepseek/DeepSeekClient.java`
- `src/test/java/io/imiocode/llm/provider/deepseek/DeepSeekClientTest.java`

**依赖：** T21、T27

**步骤：**

1. 读取 `delta.content` 和 `delta.tool_calls`。
2. 按工具索引追加 ID、名称和参数碎片。
3. 接受 `stop` 或 `tool_calls` 正常结束原因，并要求 `[DONE]`。
4. 对长度、内容过滤、资源不足和无效结构返回协议错误。
5. 测试交错工具、混合文本、纯工具、无效 JSON 及不完整流。

**验证：** 运行 `mvn -q -Dtest=DeepSeekClientTest test`，期望 DeepSeek 工具流解析与另外两家统一。

## T29：更新客户端工厂与统一契约

**文件：**

- `src/main/java/io/imiocode/llm/LlmClientFactory.java`
- `src/test/java/io/imiocode/llm/LlmClientContractTest.java`
- 三个厂商客户端测试

**依赖：** T22–T28

**步骤：**

1. 工厂创建三个客户端时注入同一注册中心。
2. 保持 `LlmClient.streamChat` 和 `StreamListener` 方法不变。
3. 将纯文本契约扩展为纯工具、混合响应和协议失败契约。
4. 验证三个客户端均只公开已启用工具。

**验证：** 运行 `mvn -q -Dtest=LlmClientContractTest,OpenAiClientTest,AnthropicClientTest,DeepSeekClientTest test`，期望三家契约全部通过。

## T30：适配 ConversationSession 的纯文本路径

**文件：**

- `src/main/java/io/imiocode/conversation/ConversationSession.java`
- `src/test/java/io/imiocode/conversation/ConversationSessionTest.java`

**依赖：** T6、T7、T29

**步骤：**

1. 接收 `ConversationListener` 并为客户端创建每次请求独立的文本桥接。
2. 保留仅传文本监听器的兼容入口。
3. 第一次响应没有工具时原子提交用户和助手消息。
4. 模型异常时不改变正式历史。
5. 调整原有两轮纯文本测试。

**验证：** 运行 `mvn -q -Dtest=ConversationSessionTest test`，期望第二章纯文本和历史回滚测试继续通过。

## T31：实现 ConversationSession 单轮工具编排

**文件：**

- `src/main/java/io/imiocode/conversation/ConversationSession.java`
- `src/test/java/io/imiocode/conversation/ConversationSessionTest.java`

**依赖：** T5、T20、T30

**步骤：**

1. 首次响应含工具时暂存助手消息。
2. 用串行执行器执行完整调用列表并转发工具事件。
3. 将关联结果组成内部工具消息。
4. 使用临时消息序列发起唯一一次后续请求。
5. 最终文本成功时一次提交用户、首次助手、工具结果和最终助手消息。
6. 测试多个工具、一个失败但继续、结果关联和请求次数。

**验证：** 运行 `mvn -q -Dtest=ConversationSessionTest test`，期望工具场景恰好调用模型两次且历史顺序正确。

## T32：补齐会话失败与单轮边界

**文件：**

- `src/main/java/io/imiocode/conversation/ConversationSession.java`
- `src/test/java/io/imiocode/conversation/ConversationSessionTest.java`

**依赖：** T31

**步骤：**

1. 最终响应再次含工具时拒绝执行并返回单轮限制错误。
2. 最终请求失败时设置 `toolsExecuted=true` 且不提交历史。
3. 用户中断时取消执行器和客户端，不发起后续请求。
4. `close()` 保持幂等并按执行器、客户端顺序关闭。
5. 测试第二批工具拒绝、后续失败、副作用提示状态和中断。

**验证：** 运行 `mvn -q -Dtest=ConversationSessionTest test`，期望所有未完成工具轮次保持历史不变。

## T33：扩展终端工具状态

**文件：**

- `src/main/java/io/imiocode/terminal/UiState.java`
- `src/main/java/io/imiocode/terminal/TerminalLayout.java`
- `src/main/java/io/imiocode/terminal/JLineTerminalUi.java`
- `src/test/java/io/imiocode/terminal/TerminalLayoutTest.java`

**依赖：** T5

**步骤：**

1. 增加 `TOOL_WAITING` 和 `TOOL_RUNNING` 状态及标签。
2. 为完整、紧凑和纯文本模式提供工具状态布局。
3. 补齐 JLine 状态颜色分支，保证枚举 switch 完整。
4. 测试不同宽度和无 ANSI 模式下的工具状态。

**验证：** 运行 `mvn -q -Dtest=TerminalLayoutTest test`，期望工具状态在三种终端模式均可观察。

## T34：实现工具摘要格式化

**文件：**

- `src/main/java/io/imiocode/terminal/ToolSummaryFormatter.java`
- `src/test/java/io/imiocode/terminal/ToolSummaryFormatterTest.java`

**依赖：** T2、T5

**步骤：**

1. 为六种工具生成输入摘要，不输出 Write/Edit 正文。
2. 为成功、失败、退出码、匹配数量和截断生成结果摘要。
3. 所有摘要限制为 240 字符并执行已知秘密脱敏。
4. 测试长命令、中文、写入正文隐藏和认证值脱敏。

**验证：** 运行 `mvn -q -Dtest=ToolSummaryFormatterTest test`，期望摘要不含完整文件正文或测试密钥。

## T35：渲染工具生命周期事件

**文件：**

- `src/main/java/io/imiocode/terminal/TerminalUi.java`
- `src/main/java/io/imiocode/terminal/JLineTerminalUi.java`
- `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java`

**依赖：** T33、T34

**步骤：**

1. 在终端接口增加工具事件展示入口。
2. 事件到来前结束已打开的助手文本行。
3. 按等待、运行、成功和失败显示名称、风险、输入及结果摘要。
4. 使用 JLine 样式生成颜色，不把 ANSI 拼入领域数据。
5. 测试事件顺序、纯文本降级、输出刷新和 ANSI 隔离。

**验证：** 运行 `mvn -q -Dtest=JLineTerminalUiTest test`，期望捕获输出含工具过程且原始事件数据无 ANSI。

## T36：接入 ConversationLoop

**文件：**

- `src/main/java/io/imiocode/conversation/ConversationLoop.java`
- `src/test/java/io/imiocode/conversation/ConversationLoopTest.java`

**依赖：** T7、T32、T35

**步骤：**

1. 使用 `ConversationListener` 驱动文本行和工具事件。
2. 按 plan 更新 `THINKING`、`STREAMING`、工具状态和 `READY`。
3. 根据 `ConversationException.toolsExecuted()` 输出准确失败提示。
4. 单轮限制显示明确消息，不执行第二批工具。
5. Ctrl+C 设置停止标志、关闭会话且不重新读取输入。
6. 扩展 fake 终端测试状态、事件、错误和中断顺序。

**验证：** 运行 `mvn -q -Dtest=ConversationLoopTest test`，期望工具流程、错误流程和中断流程全部通过。

## T37：装配六个核心工具

**文件：**

- `src/main/java/io/imiocode/ImioCodeApplication.java`
- `src/main/java/io/imiocode/llm/LlmClientFactory.java`

**依赖：** T12–T19、T29、T32、T36

**步骤：**

1. 获取当前工作区绝对规范路径并同时用于配置和工具。
2. 创建默认限制及当前 API Key 的脱敏器。
3. 创建路径策略、六个工具和注册中心。
4. 创建执行器、持有注册中心的客户端和会话。
5. 在启动 UI 中显示同一工作区。
6. 保持 finally 关闭顺序幂等，不重复产生错误。

**验证：** 运行 `mvn -q -DskipTests compile`，期望应用入口及全部生产代码编译通过。

## T38：完成三厂商统一工具契约测试

**文件：**

- `src/test/java/io/imiocode/llm/LlmClientContractTest.java`
- 三个厂商客户端测试
- `src/test/java/io/imiocode/llm/transport/MockLlmServer.java`

**依赖：** T29、T37

**步骤：**

1. 为三家准备等价的纯工具、混合文本和多个交错工具 SSE。
2. 验证工具定义请求、调用顺序和统一 `ChatResponse`。
3. 为每家准备带工具结果的第二请求并验证关联字段。
4. 覆盖无效 JSON、缺少 ID、缺少名称及不完整流。
5. 确认测试服务器记录中没有真实配置或环境密钥。

**验证：** 运行 `mvn -q -Dtest=LlmClientContractTest,OpenAiClientTest,AnthropicClientTest,DeepSeekClientTest test`，期望三家行为断言一致。

## T39：完成安全与资源边界回归

**文件：**

- 全部 `src/test/java/io/imiocode/tool/**` 测试
- `src/test/java/io/imiocode/conversation/ConversationSessionTest.java`
- `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java`

**依赖：** T8–T19、T31–T36

**步骤：**

1. 运行工具测试并补齐绝对路径、`..`、链接和敏感路径矩阵。
2. 覆盖所有固定边界的“恰好等于”和“超过一单位”场景。
3. 用固定假 Key 验证结果、异常、终端和 Bash 环境不泄露。
4. 验证一个工具异常后注册中心和下一次会话仍可使用。

**验证：** 运行 `mvn -q -Dtest=BaseToolTest,SecretRedactorTest,ToolRegistryTest,ToolExecutorTest,WorkspacePolicyTest,WorkspaceWalkerTest,AtomicFileWriterTest,ReadFileToolTest,WriteFileToolTest,EditFileToolTest,BashToolTest,GlobToolTest,GrepToolTest,ConversationSessionTest,JLineTerminalUiTest test`，期望全部通过。

## T40：执行完整构建与回归

**文件：** 全项目

**依赖：** T1–T39

**步骤：**

1. 执行干净测试，确认第二章及第三章测试全部通过。
2. 执行可执行 JAR 打包。
3. 检查测试报告无失败、错误或意外跳过。
4. 检查 Git 变更仅包含第三章文档和计划内代码，保留 `claude.md`。

**验证：**

- 运行 `mvn clean test`，期望退出码为 0。
- 运行 `mvn package`，期望生成 `target/imiocode-0.2.0-SNAPSHOT-all.jar`。
- 运行 `git status --short`，期望无计划外文件。

## 执行顺序

```text
T1
├── T2 → T3 → T4 → T5
├── T6 → T7
└── T8
    ├── T9  → T12
    ├── T10 → T13 → T14
    └── T11 → T16 → T17
          ↑
         T15

T3 + T8 → T18 → T19
T1 + T2 → T20
T1      → T21

T4 + T6 + T20 + T21
├── T22 → T23 → T24
├── T25 → T26
└── T27 → T28
              ↓
             T29

T5 + T6 + T7 + T29
  → T30 → T31 → T32

T5 → T33
T2 + T5 → T34
T33 + T34 → T35
T7 + T32 + T35 → T36

核心工具 + T29 + T32 + T36
  → T37 → T38

全部功能 → T39 → T40
```

## 提交点

实现阶段按以下逻辑组提交：

1. T1–T7：工具框架与统一消息模型。
2. T8–T19：工作区安全与六个核心工具。
3. T20–T29：三厂商工具协议。
4. T30–T37：会话编排、终端 UI 和应用装配。
5. T38–T40：契约测试、安全回归和完整构建修正。

每个提交前必须先运行该组最后一个任务的验证命令；验证失败时不得创建提交。
