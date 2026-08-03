# CH9 跨会话记忆系统 Tasks

## 文件清单

| 操作 | 文件 | 职责 |
|---|---|---|
| 新建 | `src/main/java/io/imiocode/config/InstructionsConfig.java`<br>`src/main/java/io/imiocode/config/SessionsConfig.java`<br>`src/main/java/io/imiocode/config/MemoryConfig.java` | 三组运行时配置、默认值与硬边界 |
| 修改 | `src/main/java/io/imiocode/config/AppConfig.java`<br>`src/main/java/io/imiocode/config/ConfigDocument.java`<br>`src/main/java/io/imiocode/config/ConfigLoader.java` | 接入 YAML 文档、合并默认值并校验范围 |
| 修改 | `config.example.yaml` | 展示 CH9 配置及注释 |
| 新建 | `src/main/java/io/imiocode/instruction/InstructionScope.java`<br>`InstructionSource.java`<br>`InstructionProblem.java`<br>`InstructionSnapshot.java`<br>`InstructionLoadRequest.java`<br>`InstructionLoader.java`<br>`GitProjectLocator.java`<br>`IncludeExpander.java`<br>`FileInstructionLoader.java`<br>`InstructionReminderFormatter.java` | 分层指令发现、安全 include、排序和提醒格式化 |
| 新建 | `src/main/java/io/imiocode/session/SessionId.java`<br>`SessionMetadata.java`<br>`SessionSnapshot.java`<br>`SessionSummary.java`<br>`SessionRecoveryStatus.java`<br>`SessionLoadResult.java`<br>`SessionStore.java`<br>`SessionManager.java`<br>`SessionIntegrityValidator.java`<br>`SessionMetadataReader.java`<br>`SessionMessageCodec.java`<br>`JsonlSessionStore.java` | 会话身份、生命周期、事务持久化和恢复 |
| 新建 | `src/main/java/io/imiocode/session/record/SessionHeaderRecord.java`<br>`TransactionBeginRecord.java`<br>`MessageRecord.java`<br>`TransactionCommitRecord.java`<br>`TransactionMode.java`<br>`StoredMessage.java`<br>`StoredPart.java`<br>`StoredToolResult.java`<br>`SessionRecordCodec.java` | 版本化 JSONL 存储 DTO 和规范编码 |
| 新建 | `src/main/java/io/imiocode/memory/MemoryScope.java`<br>`MemoryCategory.java`<br>`MemoryEntry.java`<br>`MemoryDocument.java`<br>`MemoryCandidate.java`<br>`MemoryExtractionResult.java`<br>`MemoryUpdateReport.java`<br>`MemoryStore.java`<br>`MemoryExtractor.java`<br>`MarkdownMemoryStore.java`<br>`MemorySafetyPolicy.java`<br>`MemoryManager.java`<br>`MemoryResponseParser.java`<br>`MemoryReminderFormatter.java`<br>`LlmMemoryExtractor.java` | 双层 Markdown 记忆、安全更新与 LLM 提取 |
| 新建 | `src/main/java/io/imiocode/persistence/FileFingerprint.java`<br>`PersistenceEvent.java`<br>`PersistenceEventListener.java`<br>`PersistentContextProvider.java`<br>`DefaultPersistentContextProvider.java` | 文件变化检测、稳定提醒和持久化事件 |
| 新建 | `src/main/java/io/imiocode/command/CommandContext.java`<br>`CommandDisposition.java`<br>`CommandMessage.java`<br>`CommandParser.java`<br>`CommandResult.java`<br>`CommandServices.java`<br>`LocalCommand.java`<br>`LocalCommandRegistry.java` | 本地命令契约、解析与注册中心 |
| 新建 | `src/main/java/io/imiocode/command/builtin/PlanCommand.java`<br>`DoCommand.java`<br>`CompactCommand.java`<br>`VerbosityCommand.java`<br>`ExitCommand.java`<br>`SessionCommand.java`<br>`MemoryCommand.java` | 现有命令迁移与 CH9 管理命令 |
| 新建 | `src/main/java/io/imiocode/runtime/ConversationCoordinator.java` | 跨模块事务编排并实现命令服务 |
| 移动并修改 | `src/main/java/io/imiocode/conversation/ConversationLoop.java` → `src/main/java/io/imiocode/runtime/ConversationLoop.java` | 本地命令路由、流式 UI 与持久化事件渲染 |
| 修改 | `src/main/java/io/imiocode/conversation/ConversationSession.java` | 增加空闲检查和受控历史替换，不引入 CH9 反向依赖 |
| 新建/修改 | `src/main/java/io/imiocode/terminal/ConfirmationPrompt.java`<br>`src/main/java/io/imiocode/terminal/TerminalUi.java`<br>`src/main/java/io/imiocode/terminal/JLineTerminalUi.java` | 通用删除确认和 CH9 状态显示 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 装配 CH9 服务和安全关闭顺序 |
| 修改 | `.gitignore` | 忽略本地会话和项目记忆，不忽略 `MEWCODE.md` |
| 修改 | `README.md` | 说明 CH9 配置、文件位置和本地命令 |
| 新建/修改 | `src/test/java/io/imiocode/config/*Test.java`<br>`src/test/java/io/imiocode/instruction/*Test.java`<br>`src/test/java/io/imiocode/session/*Test.java`<br>`src/test/java/io/imiocode/memory/*Test.java`<br>`src/test/java/io/imiocode/persistence/*Test.java`<br>`src/test/java/io/imiocode/command/*Test.java`<br>`src/test/java/io/imiocode/runtime/*Test.java`<br>`src/test/java/io/imiocode/conversation/*Test.java`<br>`src/test/java/io/imiocode/Ch9ApplicationIT.java` | 单元、集成和应用级回归测试 |

## T1：定义 CH9 配置记录

**文件：** `InstructionsConfig.java`、`SessionsConfig.java`、`MemoryConfig.java`
**依赖：** 无

**步骤：**
1. 按 plan 定义三个不可变配置记录及 `defaults()`。
2. 实现默认值、非负/正数约束和不可配置硬上限。
3. 允许 `retentionDays`、`maxSessions` 为 0，其他容量项必须为正数。

**验证：** `mvn -q -DskipTests compile` 通过；构造边界值时错误字段清晰。

## T2：接入统一配置加载

**文件：** `AppConfig.java`、`ConfigDocument.java`、`ConfigLoader.java`
**依赖：** T1

**步骤：**
1. 在 `AppConfig` 末尾增加三组配置并保留全部旧构造器。
2. 在 YAML 文档模型中增加 kebab-case 字段映射。
3. 将空配置段映射为默认值，将非法值转换为不泄密的 `ConfigException`。
4. 更新 `toString()`，只展示安全配置摘要。

**验证：** `mvn -q -Dtest=ConfigLoaderTest,YamlConfigLoaderTest test` 通过。

## T3：补充配置样例和配置测试

**文件：** `config.example.yaml`、`ConfigLoaderTest.java`、`UnifiedConfigLoaderTest.java`、`YamlConfigLoaderTest.java`
**依赖：** T2

**步骤：**
1. 添加三组配置的默认示例和 0 值语义注释。
2. 测试旧配置缺少新段时仍加载默认值。
3. 测试功能开关、边界值、硬上限和错误字段路径。
4. 确认样例不包含密钥或真实用户路径。

**验证：** `mvn -q -Dtest=ConfigLoaderTest,UnifiedConfigLoaderTest,YamlConfigLoaderTest test` 全部通过。

## T4：建立指令领域模型

**文件：** `InstructionScope.java`、`InstructionSource.java`、`InstructionProblem.java`、`InstructionSnapshot.java`、`InstructionLoadRequest.java`、`InstructionLoader.java`
**依赖：** T1

**步骤：**
1. 定义作用域、来源、问题、快照和加载请求。
2. 对路径、优先级、内容、列表不可变性和累计字节数做构造校验。
3. 为关闭功能提供空快照工厂。

**验证：** `mvn -q -DskipTests compile` 通过，空快照不含可变集合。

## T5：实现项目根定位和分层候选发现

**文件：** `GitProjectLocator.java`、`FileInstructionLoader.java`
**依赖：** T4

**步骤：**
1. 用带超时的非交互 Git 命令定位仓库根目录。
2. 非 Git、Git 不可用或超时时回退到工作目录。
3. 生成用户文件以及项目根到工作目录的候选路径，按低到高优先级排序。
4. 对工作目录不在返回 Git 根下的异常结果安全回退。

**验证：** 新增 `FileInstructionLoaderTest` 的 Git、非 Git、嵌套目录用例并运行通过。

## T6：解析 include 语法和路径沙箱

**文件：** `IncludeExpander.java`
**依赖：** T4

**步骤：**
1. 只识别独占一行的 `@include`，支持无引号和成对单/双引号路径。
2. 拒绝空路径、未闭合引号、绝对路径、点段越界和非普通文件。
3. 同时校验规范化路径与 `toRealPath()` 结果位于允许根目录。
4. 保留普通正文原有顺序和 UTF-8 内容。

**验证：** `mvn -q -Dtest=IncludeExpanderTest test`，合法语法和四类越界用例通过。

## T7：实现 include 递归、循环和容量限制

**文件：** `IncludeExpander.java`、`IncludeExpanderTest.java`
**依赖：** T6

**步骤：**
1. 使用真实路径递归栈检测直接和间接循环。
2. 按配置限制深度，并用 UTF-8 字节数累计展开大小。
3. 错误携带顶层来源、当前 include 文件和安全原因。
4. 确认失败时不返回半展开正文。

**验证：** 运行深度 8/9、循环、128 KiB 边界和多字节中文测试，全部通过。

## T8：完成指令加载、失败隔离和优先级

**文件：** `FileInstructionLoader.java`、`FileInstructionLoaderTest.java`
**依赖：** T5、T7

**步骤：**
1. 读取存在的普通 `MEWCODE.md` 并为每个来源调用 include 展开器。
2. 单来源失败时记录一个 `InstructionProblem` 并排除整个来源。
3. 成功来源按用户、项目根、项目近端稳定排序。
4. 总容量按全部成功来源统一计算，超限来源不生效。

**验证：** `mvn -q -Dtest=FileInstructionLoaderTest test`，部分失败不影响其他来源且冲突顺序正确。

## T9：格式化指令提醒

**文件：** `InstructionReminderFormatter.java`、`InstructionReminderFormatterTest.java`
**依赖：** T8

**步骤：**
1. 为来源生成含作用域、相对路径、优先级和覆盖规则的稳定文本。
2. 拒绝正文内嵌保留的 system-reminder 标签。
3. 空快照不生成提醒，同一快照重复格式化结果完全相同。

**验证：** `mvn -q -Dtest=InstructionReminderFormatterTest test` 通过。

## T10：定义 JSONL 记录和规范编码

**文件：** `session/record/*.java`、`SessionRecordCodec.java`
**依赖：** 无

**步骤：**
1. 定义 header、begin、message、commit、存储消息和工具结果 DTO。
2. 使用固定属性顺序和 UTF-8 单行 JSON 编解码。
3. 拒绝未知记录类型、未知 schema、空事务 ID 和非法序号。
4. 提供事务 SHA-256 所需的规范字节输出。

**验证：** 新增 `SessionRecordCodecTest`，逐类往返后 JSON 字节稳定。

## T11：实现消息文本与思考部件编解码

**文件：** `SessionMessageCodec.java`、`SessionMessageCodecTest.java`
**依赖：** T10

**步骤：**
1. 映射消息角色、文本部件和思考部件。
2. 显式保存 Anthropic、DeepSeek、OpenAI 三类思考元数据及字段。
3. 拒绝未知 Provider 元数据、角色与部件不匹配和空消息。

**验证：** `mvn -q -Dtest=SessionMessageCodecTest test`，三类思考消息逐字段往返一致。

## T12：实现工具调用与结果编解码

**文件：** `SessionMessageCodec.java`、`SessionMessageCodecTest.java`
**依赖：** T11

**步骤：**
1. 保存工具调用 ID、名称和参数 JSON 树。
2. 保存工具结果的成功状态、输出、错误、截断、耗时和退出码。
3. 解码后验证工具调用 ID 唯一，工具结果必须对应此前调用。
4. 测试错误结果、空输出、无退出码和截断结果。

**验证：** `mvn -q -Dtest=SessionMessageCodecTest test` 全部通过。

## T13：建立会话身份和生命周期模型

**文件：** `SessionId.java`、`SessionMetadata.java`、`SessionSnapshot.java`、`SessionSummary.java`、`SessionRecoveryStatus.java`、`SessionLoadResult.java`、`SessionStore.java`
**依赖：** T12

**步骤：**
1. 生成文件名安全且不可预测的会话 ID，并严格解析命令输入。
2. 定义元数据、历史快照、列表摘要和恢复结果。
3. 对时间、提交数、消息数和不可变历史做校验。

**验证：** 新增 `SessionIdTest`，合法 ID 往返，路径分隔符、点段和扩展名全部被拒绝。

## T14：实现会话创建和事务追加

**文件：** `JsonlSessionStore.java`、`SessionManager.java`
**依赖：** T10、T13

**步骤：**
1. 在项目会话目录创建 header 并强制刷新。
2. 比较前后历史，选择 APPEND 或 REPLACE。
3. 追加 begin、连续 message、commit，计算摘要并在提交后 `force(true)`。
4. 只在落盘成功后增加提交数和更新时间。

**验证：** `mvn -q -Dtest=JsonlSessionStoreTest#createsAndAppendsTransactions test` 通过，文件每行均可独立解析。

## T15：实现顺序恢复和完整性校验

**文件：** `SessionIntegrityValidator.java`、`JsonlSessionStore.java`
**依赖：** T12、T14

**步骤：**
1. 按状态机验证 header 和每个事务的顺序、基数、序号、数量与摘要。
2. 重放 APPEND/REPLACE 得到候选历史。
3. 对恢复后的角色和工具调用链做完整校验。
4. 只有全部验证成功才构造 `SessionSnapshot`。

**验证：** `mvn -q -Dtest=JsonlSessionStoreTest#loadsCommittedHistory test`，含压缩替换的历史恢复一致。

## T16：实现尾部隔离与中部损坏拒绝

**文件：** `JsonlSessionStore.java`、`SessionIntegrityValidator.java`、`SessionRecoveryTest.java`
**依赖：** T15

**步骤：**
1. 记录最后有效提交的字节边界并分类 EOF 未完成事务和最后行损坏。
2. 尾部恢复先写入、刷新隔离副本，再原子重写有效前缀。
3. 文件头、首事务、中部或损坏后仍有完整记录时拒绝恢复。
4. 隔离或重写任一步失败时保持主文件原样并拒绝加载。

**验证：** `mvn -q -Dtest=SessionRecoveryTest test`，尾部恢复、中部拒绝、修复失败三组测试通过。

## T17：实现会话列表和保留策略

**文件：** `SessionMetadataReader.java`、`SessionManager.java`、`SessionMetadataReaderTest.java`
**依赖：** T14、T16

**步骤：**
1. 仅读取头部和尾部提交元数据生成 `SessionSummary`。
2. 单个损坏文件不阻断列表，并返回安全状态提示。
3. 按更新时间排序，应用天数和数量策略时跳过当前会话。
4. 默认 0/0 不删除任何文件。

**验证：** `mvn -q -Dtest=SessionMetadataReaderTest test`，大量消息文件列表测试不解码消息正文。

## T18：建立记忆模型和 Markdown 解析

**文件：** `MemoryScope.java`、`MemoryCategory.java`、`MemoryEntry.java`、`MemoryDocument.java`、`MemoryCandidate.java`、`MemoryExtractionResult.java`、`MemoryUpdateReport.java`、`MemoryStore.java`、`MarkdownMemoryStore.java`
**依赖：** T1

**步骤：**
1. 定义双作用域、三类别、稳定 ID 和候选/报告模型。
2. 解析标题、空行和规范 `- [id] [category] content` 条目。
3. 拒绝重复 ID、未知类别、多行注入和格式错误条目。
4. 路由用户与项目文件，关闭作用域时不访问对应文件。

**验证：** `mvn -q -Dtest=MarkdownMemoryStoreTest#loadsDocuments test` 通过。

## T19：实现记忆原子替换

**文件：** `MarkdownMemoryStore.java`、`MarkdownMemoryStoreTest.java`
**依赖：** T18

**步骤：**
1. 按稳定 ID 排序输出 UTF-8 Markdown。
2. 使用同目录临时文件、`force(true)` 和优先原子移动替换。
3. 写入失败清理临时文件并保留原文件。
4. 写入前后都重新校验目标真实路径位于允许作用域。

**验证：** `mvn -q -Dtest=MarkdownMemoryStoreTest test`，成功替换和模拟失败保留原文件用例通过。

## T20：实现记忆安全策略

**文件：** `MemorySafetyPolicy.java`、`MemorySafetyPolicyTest.java`
**依赖：** T18

**步骤：**
1. 校验类别、作用域、条目长度和文件容量。
2. 复用 `SecretRedactor` 检测令牌、密钥、密码和凭据赋值。
3. 为自动候选拒绝个人敏感信息、临时任务、大段原始文本和非白名单类别。
4. 错误只返回类别化原因，不回显秘密正文。

**验证：** `mvn -q -Dtest=MemorySafetyPolicyTest test`，白名单与全部拒绝样例通过且日志无秘密。

## T21：实现记忆 CRUD、去重和合并

**文件：** `MemoryManager.java`、`MemoryManagerTest.java`
**依赖：** T19、T20

**步骤：**
1. 实现 list/add/edit/forget，并在每次变更前读取磁盘最新版本。
2. 手动 add 默认按作用域选择合法类别，edit 保持 ID 稳定。
3. 实现规范化精确去重和同作用域 `replacesId` 更新。
4. 按作用域独立提交候选并汇总 added/updated/skipped/warnings。

**验证：** `mvn -q -Dtest=MemoryManagerTest test`，CRUD、重复、更新目标不存在和跨作用域替换测试通过。

## T22：实现自动提取响应协议

**文件：** `MemoryResponseParser.java`、`MemoryResponseParserTest.java`
**依赖：** T18

**步骤：**
1. 只提取唯一 `<memories>` XML 边界内的 JSON。
2. 严格映射 scope/category/content/replacesId 并拒绝未知字段。
3. 对普通文本、多个标签、非法枚举、过量候选和截断 JSON 返回安全失败。

**验证：** `mvn -q -Dtest=MemoryResponseParserTest test` 全部通过。

## T23：实现无工具 LLM 记忆提取器

**文件：** `MemoryExtractor.java`、`LlmMemoryExtractor.java`、`LlmMemoryExtractorTest.java`
**依赖：** T20、T22

**步骤：**
1. 构建稳定提取 System Prompt，包含白名单、禁止项和输出协议。
2. 只序列化本轮用户消息、成功轨迹和当前记忆，先经过秘密清理。
3. 使用共享 `LlmClient`、空工具选择和独立输出 Token 上限发起请求。
4. 将模型、协议和解析错误转为非致命提取警告。

**验证：** `mvn -q -Dtest=LlmMemoryExtractorTest test`，断言请求无工具、输出上限正确且失败不抛到用户轮次。

## T24：格式化记忆提醒

**文件：** `MemoryReminderFormatter.java`、`MemoryReminderFormatterTest.java`
**依赖：** T21

**步骤：**
1. 分用户和项目区块输出稳定 ID、类别与正文。
2. 先用户后项目，并明确项目冲突优先。
3. 空文档不生成提醒，拒绝保留标签嵌套。

**验证：** `mvn -q -Dtest=MemoryReminderFormatterTest test`，重复格式化结果一致。

## T25：建立持久上下文快照和事件

**文件：** `PersistenceEvent.java`、`PersistenceEventListener.java`、`PersistentContextProvider.java`、`DefaultPersistentContextProvider.java`
**依赖：** T9、T24

**步骤：**
1. 定义保存、恢复、尾部恢复、记忆更新和安全警告事件。
2. 按用户指令、项目指令、用户记忆、项目记忆顺序生成 SESSION reminders。
3. 快照不可变，刷新失败时保留上一份有效快照并产生警告。

**验证：** `mvn -q -Dtest=DefaultPersistentContextProviderTest#ordersStableReminders test` 通过。

## T26：实现每轮文件指纹刷新

**文件：** `FileFingerprint.java`、`DefaultPersistentContextProvider.java`、`DefaultPersistentContextProviderTest.java`
**依赖：** T25

**步骤：**
1. 指纹包含候选路径、真实路径、存在性、大小和修改时间。
2. 跟踪所有顶层候选和已展开 include 依赖，检测新建、修改、删除和符号链接变化。
3. 指纹不变时复用快照；变化时整体重载对应模块。
4. 确认每个用户轮次最多检查一次，不在 Agent 迭代中重复。

**验证：** `mvn -q -Dtest=DefaultPersistentContextProviderTest test`，外部修改下一轮生效且无变化时不重复读正文。

## T27：定义本地命令契约和解析器

**文件：** `CommandContext.java`、`CommandDisposition.java`、`CommandMessage.java`、`CommandResult.java`、`CommandServices.java`、`LocalCommand.java`、`CommandParser.java`
**依赖：** T13、T18

**步骤：**
1. 定义命令服务、结果、UI 消息和命令接口。
2. 解析 `/name`、多余空白、单/双引号和反斜杠转义。
3. 非 `/` 输入返回非命令；未知或引号错误生成已消费的安全错误。
4. 命令名统一小写，参数保留正文大小写。

**验证：** `mvn -q -Dtest=CommandParserTest test` 全部通过。

## T28：实现命令注册中心

**文件：** `LocalCommandRegistry.java`、`LocalCommandRegistryTest.java`
**依赖：** T27

**步骤：**
1. 注册规范名和别名，拒绝重复或冲突。
2. 精确分派命令并把参数错误转换为对应 usage。
3. 未知 `/` 命令返回提示且绝不标记为普通对话。

**验证：** `mvn -q -Dtest=LocalCommandRegistryTest test` 通过。

## T29：迁移现有本地命令

**文件：** `PlanCommand.java`、`DoCommand.java`、`CompactCommand.java`、`VerbosityCommand.java`、`ExitCommand.java`
**依赖：** T28

**步骤：**
1. 将现有 `/plan`、`/do`、`/compact`、`/verbose`、`/compact-ui`、`/exit`、`/quit` 行为迁移为处理器。
2. 保持现有模式切换、压缩报告、UI 提示和退出语义。
3. 为无参数命令拒绝额外参数并显示 usage。

**验证：** 新增 `BuiltinCommandTest` 并运行，迁移前后的可见输出一致。

## T30：实现 `/session` 命令

**文件：** `SessionCommand.java`、`ConfirmationPrompt.java`、`TerminalUi.java`
**依赖：** T17、T28

**步骤：**
1. 实现 list/current/new/resume/delete 参数分派和用法提示。
2. resume 使用严格 `SessionId`，失败时保持当前会话。
3. delete 拒绝当前会话，并通过通用终端确认后才调用删除。
4. 会话功能关闭时所有管理子命令显示明确提示。

**验证：** `mvn -q -Dtest=SessionCommandTest test`，确认接受/拒绝、恢复失败和禁用状态通过。

## T31：实现 `/memory` 命令

**文件：** `MemoryCommand.java`
**依赖：** T21、T28

**步骤：**
1. 实现 list 可选作用域以及 add/edit/forget 必需参数。
2. 使用解析后的引号内容保留内部空格，拒绝未知作用域和空正文。
3. 将安全或容量错误转换为不泄密的 UI 错误。
4. 记忆功能关闭时显示明确提示。

**验证：** `mvn -q -Dtest=MemoryCommandTest test`，全部子命令和错误 usage 测试通过。

## T32：完成本地命令回归测试

**文件：** `command/*Test.java`
**依赖：** T29、T30、T31

**步骤：**
1. 验证全部 `/` 命令返回 handled 或 exit，不形成模型请求。
2. 验证命令正文不写入会话历史。
3. 验证未知命令、参数错误和未闭合引号均被本地消费。

**验证：** `mvn -q -Dtest='io.imiocode.command.*Test' test` 全部通过。

## T33：为核心会话增加受控历史替换

**文件：** `ConversationSession.java`、`ConversationSessionTest.java`
**依赖：** 无

**步骤：**
1. 暴露只读空闲状态，Agent 执行期间返回 busy。
2. 增加受控 `replaceHistory`，仅空闲且未关闭时接受已验证的不可变历史。
3. 替换时清除一次性提醒，关闭状态和运行中替换均拒绝。
4. 不引入 session、memory、persistence 包依赖。

**验证：** `mvn -q -Dtest=ConversationSessionTest test`，替换、busy、closed 和旧发送事务测试通过。

## T34：实现运行时协调器的成功提交路径

**文件：** `runtime/ConversationCoordinator.java`、`runtime/ConversationCoordinatorTest.java`
**依赖：** T14、T23、T26、T33

**步骤：**
1. 包装核心会话，持有当前会话快照和 CH9 服务。
2. 每轮开始刷新持久上下文并添加 SESSION reminders。
3. Agent 成功后比较前后历史并先写入会话事务。
4. 写入成功后执行自动记忆、应用候选、刷新记忆并发事件。
5. 写入失败保留内存回复、跳过自动记忆并发高可见警告。

**验证：** `mvn -q -Dtest=ConversationCoordinatorTest#commitsThenExtractsMemory test`，用调用顺序断言验证严格顺序。

## T35：实现协调器失败、压缩与功能开关路径

**文件：** `ConversationCoordinator.java`、`ConversationCoordinatorTest.java`
**依赖：** T34

**步骤：**
1. Agent 失败或取消时不提交、不提取。
2. 手动压缩成功后写 REPLACE 事务但不提取记忆。
3. sessions 禁用时保留内存历史并允许独立自动记忆。
4. memory 或 instructions 禁用时分别返回空快照，不影响其他能力。

**验证：** 运行协调器失败、压缩和三种开关组合测试，调用次数符合预期。

## T36：实现协调器会话与记忆命令服务

**文件：** `ConversationCoordinator.java`、`ConversationCoordinatorTest.java`
**依赖：** T17、T21、T35

**步骤：**
1. 实现 `CommandServices` 的会话 list/current/new/resume/delete。
2. resume 先解码候选历史，成功后调用核心历史替换并刷新上下文。
3. 实现记忆 list/add/edit/forget，成功后刷新记忆快照。
4. 所有切换操作二次检查核心会话空闲，失败不改变当前状态。

**验证：** 运行 coordinator 的恢复原子性、busy 拒绝、记忆刷新和当前会话删除拒绝测试。

## T37：迁移运行时交互循环

**文件：** 移动 `conversation/ConversationLoop.java` 到 `runtime/ConversationLoop.java`，修改相关测试和 import
**依赖：** T32、T36

**步骤：**
1. 将所有手写斜杠分支替换为注册中心 dispatch。
2. 普通输入委托协调器并保持现有流式文本、思考、工具和权限渲染。
3. 渲染持久化保存、恢复、尾部恢复、记忆更新和警告事件。
4. 保持 Ctrl+C、退出、错误状态和 UI 精简/详细模式行为。

**验证：** `mvn -q -Dtest=ConversationLoopTest test`，旧场景和新增本地命令拦截场景全部通过。

## T38：实现通用终端确认 UI

**文件：** `ConfirmationPrompt.java`、`TerminalUi.java`、`JLineTerminalUi.java`、相关终端测试
**依赖：** T30

**步骤：**
1. 定义无权限持久授权语义的确认提示模型。
2. 在 JLine 中渲染删除对象、风险说明和 y/N 默认拒绝。
3. EOF、Ctrl+C、未知输入均按拒绝处理。
4. 保持现有权限确认 UI 不变。

**验证：** 新增 `JLineConfirmationPromptTest`，接受、拒绝、默认、EOF、Ctrl+C 全部通过；原权限测试仍通过。

## T39：完成应用装配与安全关闭

**文件：** `ImioCodeApplication.java`、`.gitignore`
**依赖：** T26、T34、T37、T38

**步骤：**
1. 由 workspace、userHome 和配置装配指令、会话、记忆、上下文、协调器和命令注册中心。
2. 自动提取器借用共享 LLM 客户端，不单独关闭。
3. 启动时创建新空会话、应用保留策略并显示简洁问题提示。
4. 调整 finally 顺序，确保已提交记录刷新且客户端只关闭一次。
5. 忽略 `.imiocode/sessions/` 和 `.imiocode/memories.md`，保留 `MEWCODE.md` 可跟踪。

**验证：** `mvn -q -DskipTests package` 通过，`git check-ignore` 结果符合三条路径预期。

## T40：补充运行时和应用集成测试

**文件：** `runtime/*Test.java`、`Ch9ApplicationIT.java`、已有应用测试
**依赖：** T39

**步骤：**
1. 用临时 userHome/workspace 验证启动创建新会话并注入指令和记忆。
2. 验证成功对话落盘、重启后显式恢复、下一轮携带恢复历史。
3. 验证本地命令不调用模型，自动提取失败不影响响应。
4. 验证旧配置、功能关闭和 UTF-8 中文路径。

**验证：** `mvn -q -Dtest=Ch9ApplicationIT,UnifiedConfigApplicationIT test` 通过。

## T41：完成 CH9 自动化测试矩阵

**文件：** 本章全部测试及受影响的旧测试
**依赖：** T40

**步骤：**
1. 运行 instruction、session、memory、persistence、command、runtime 包测试。
2. 运行 conversation、prompt、agent、context、permission、terminal 回归测试。
3. 修复失败，不通过降低断言或跳过测试规避问题。
4. 记录测试数量、失败数和跳过数作为 checklist 证据。

**验证：** `mvn test` 退出码为 0，所有新增测试执行且无意外跳过。

## T42：执行真实端到端验收

**文件：** 无实现文件；结果用于 `checklist.md`
**依赖：** T41

**步骤：**
1. 打包并在 tmux 中启动 ImioCode；若当前 Windows 环境确认无法使用 tmux，则使用真实 Java 进程和可控终端输入作为已批准的替代方案并记录原因。
2. 创建临时 `MEWCODE.md` 与两层记忆，发送真实对话，观察模型获得上下文。
3. 完成包含工具调用的对话，退出、重启并用 `/session resume <id>` 恢复。
4. 实测 `/memory` CRUD、`/session` 管理、删除确认和 `/compact` 后恢复。
5. 制造尾部损坏并观察隔离恢复；制造中部损坏并观察安全拒绝。

**验证：** 对照 `checklist.md` 逐项记录真实命令、输出和通过/不通过结论。

## T43：文档和工作树最终检查

**文件：** `README.md`、`docs/ch9/*.md`
**依赖：** T42

**步骤：**
1. 补充用户可见的配置、文件位置和命令说明。
2. 更新 checklist 实际结果，不修改已批准的需求含义。
3. 运行格式检查并确认没有生成文件、会话、记忆或秘密被暂存。
4. 保留用户已有的 `claude.md` 和 `hello.txt` 变更，不编辑、不暂存。

**验证：** `git diff --check` 通过；`git status --short` 只包含 CH9 预期文件和用户原有变更。

## 执行顺序

```text
配置：T1 → T2 → T3

指令：T4 → T5 → T6 → T7 → T8 → T9

会话：T10 → T11 → T12 → T13 → T14 → T15 → T16 → T17

记忆：T18 → T19 → T20 → T21
                 └→ T22 → T23
                      T21 → T24

上下文：T9 + T24 → T25 → T26

命令：T13 + T18 → T27 → T28 → T29
                         T17 → T30
                         T21 → T31
             T29 + T30 + T31 → T32

运行时：T33
        T14 + T23 + T26 + T33 → T34 → T35
        T17 + T21 + T35 → T36
        T32 + T36 → T37
        T30 → T38
        T26 + T34 + T37 + T38 → T39 → T40 → T41 → T42 → T43
```

T4—T9、T10—T17、T18—T24 三条模块链在依赖满足后可以独立推进；正式执行时仍按每个任务的验证结果逐项标记，任何验证失败都先修复再进入下游任务。
