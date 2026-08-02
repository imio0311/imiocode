# 精简终端 UI Checklist

> 每项必须通过测试输出、真实进程或可观察终端行为验证。先记录证据，再勾选。

## 配置与模式

- [ ] C1：旧配置没有 `ui` 字段时默认得到 `COMPACT`。（验证：运行 ConfigLoader 默认值测试。）
- [ ] C2：`ui.verbosity: compact` 与 `verbose` 大小写不敏感且分别生效。（验证：参数化 YAML 加载测试。）
- [ ] C3：未知 verbosity 在终端、MCP 和 LLM 初始化前产生指向 `ui.verbosity` 的配置错误。（验证：无效配置启动测试。）
- [ ] C4：`AppConfig` 旧构造器继续编译并自动使用 compact。（验证：全量测试编译及兼容构造器断言。）
- [ ] C5：应用把配置模式传给唯一的 JLine 终端实例。（验证：启动集成测试与调用点检查。）

## 本地切换命令

- [ ] C6：输入 `/verbose` 后只显示一条详细模式确认，后续事件按 verbose 展示。（验证：ConversationLoop + JLine 集成测试。）
- [ ] C7：输入 `/compact-ui` 后只显示一条精简模式确认，后续事件按 compact 展示。（验证：ConversationLoop + JLine 集成测试。）
- [ ] C8：两个 UI 命令不调用 LLM、不进入历史、不改变 Plan/Do 模式。（验证：伪客户端调用计数与历史快照。）
- [ ] C9：重复切换到当前模式安全且输出确定，不产生异常或历史副作用。（验证：连续相同命令测试。）

## 完整启动与精简输入区

- [ ] C10：compact 与 verbose 在相同终端能力下生成同一启动面板；FULL 包含 Logo、边框、产品版本、Provider、模型、目录和 Ready。（验证：两种详细度的内存终端快照相等。）
- [ ] C11：窄富终端按原规则保留边框和环境字段并省略 Logo；PLAIN 使用无 ANSI 的多行环境摘要。（验证：COMPACT/PLAIN 布局断言。）
- [ ] C12：compact 富终端输入提示为短 `› `，纯文本回退为 `> `。（验证：布局与内存终端测试。）
- [ ] C13：compact 不打印输入顶部边框和 readLine 尾部状态栏，verbose 保持原行为。（验证：对同一输入比较捕获输出。）
- [ ] C14：启动帮助只出现一次，并能发现 `/verbose`；不会每轮重复打印。（验证：真实进程输出计数。）

## 状态、Thinking 与 Usage

- [ ] C15：compact 完整经历 `Ready → Thinking → Streaming → Tool waiting → Tool running → Ready` 后，滚动输出不含任何状态标签。（验证：内存终端事件序列。）
- [ ] C16：compact 的 `state()` 仍准确返回最后状态，证明只隐藏显示而未丢事件。（验证：逐状态调用后检查原子状态。）
- [ ] C17：compact 收到多个 Thinking 片段和完成事件时，输出不含正文、前缀、签名或加密元数据。（验证：带哨兵文本的终端测试。）
- [ ] C18：compact 收到已知 Usage 时输出不含 input/output/reasoning 数值。（验证：TokenUsage 捕获测试。）
- [ ] C19：verbose 对同一 Thinking、Usage 和状态序列继续输出当前详细信息。（验证：verbose 回归快照。）
- [ ] C20：从 compact 切换到 verbose 后只显示新 Thinking，不补打切换前内容。（验证：前后使用不同哨兵文本。）

## 工具单行摘要

- [ ] C21：compact 的 queued、running 不输出，succeeded 只输出一行成功摘要。（验证：单调用三事件序列，计数完成行为 1。）
- [ ] C22：compact 的 queued、running、failed 只输出一行失败摘要。（验证：单调用三事件序列，计数失败行为 1。）
- [ ] C23：Read/Write/Edit 显示易读名称和路径，不显示写入正文、替换正文、风险或调用 ID。（验证：三个工具快照与否定断言。）
- [ ] C24：Bash 显示截断命令和耗时，不显示完整 stdout/stderr、风险或调用 ID。（验证：长命令与大输出测试。）
- [ ] C25：Glob/Grep 显示 pattern、结果数和耗时，不重复完整匹配内容。（验证：多行结果测试。）
- [ ] C26：MCP/未知工具显示安全化工具名并隐藏全部参数。（验证：带秘密参数的 MCP 调用测试。）
- [ ] C27：失败摘要只包含脱敏后的第一行错误，后续错误正文不可见。（验证：多行含秘密错误测试。）
- [ ] C28：零耗时、毫秒耗时和秒级耗时格式稳定。（验证：Duration 参数化测试。）
- [ ] C29：verbose 继续显示 queued、running、风险标签、输入摘要和详细结果。（验证：现有工具 UI 测试显式 verbose。）

## MCP、权限与上下文

- [ ] C30：compact 隐藏 MCP WAITING、APPROVED、CONNECTING、CONNECTED、TOOL_DISCOVERED、CLOSED 日志。（验证：逐枚举事件捕获输出。）
- [ ] C31：compact 仍显示 MCP 启动确认、DENIED、SERVER_FAILED 和最终聚合统计。（验证：确认输入与事件测试。）
- [ ] C32：compact 与 verbose 都完整显示权限工具、风险、目标、原因和三个选择。（验证：JLinePermissionPromptTest 两种模式。）
- [ ] C33：权限允许一次、会话允许和拒绝结果始终显示。（验证：三种 reply 测试。）
- [ ] C34：compact 隐藏自动压缩 Started，保留 Completed、Failed、CircuitOpened、ResultsOffloaded。（验证：逐 ContextEvent 测试。）
- [ ] C35：手动 `/compact` 报告在两种模式都显示，且命令行为不变。（验证：ConversationCompactTest 与终端输出。）

## 回答、错误与安全

- [ ] C36：compact 最终回答使用短前缀，所有流式片段、中文、Markdown、代码块和换行完整保留。（验证：多片段内存终端快照。）
- [ ] C37：回答结束、工具完成、错误和下一输入提示之间换行正确，不粘连也不产生多余空白块。（验证：混合事件输出顺序测试。）
- [ ] C38：Provider 错误、工具失败、重试、超时、取消、熔断和副作用警告在 compact 下可见。（验证：各安全事件回归测试。）
- [ ] C39：Thinking、工具正文、完整输出和 Usage 不通过摘要、模式确认或错误旁路泄露。（验证：统一哨兵扫描输出。）
- [ ] C40：所有可见内容继续经过 SecretRedactor，API Key、Token 和认证字段无命中。（验证：两种模式秘密扫描测试。）

## 响应式与兼容性

- [ ] C41：启动面板只受 FULL、COMPACT、PLAIN 终端能力影响；输入区和事件输出继续受 UiVerbosity 控制。（验证：布局接口与六种对话组合测试。）
- [ ] C42：20、40、60、80、100、200 列下启动行、工具行、模式提示和错误行均不越界。（验证：JLine 列宽断言。）
- [ ] C43：窄终端的中文路径、长命令和 MCP 名称不会切断 Unicode 代理字符。（验证：边界字符串测试。）
- [ ] C44：dumb terminal 不输出 ANSI，使用 `> `、`[ok]`、`[fail]` 且语义完整。（验证：内存 dumb terminal 输出扫描。）
- [ ] C45：Alt+Enter、Ctrl+C、`/exit`、`/quit`、`/plan`、`/do`、`/compact` 均保持通过。（验证：现有终端与会话回归测试。）

## 文档与配置示例

- [ ] C46：`config.example.yaml` 包含 `ui.verbosity: compact` 且能被加载器解析。（验证：仓库示例加载测试。）
- [ ] C47：README 和统一配置迁移文档说明 compact、verbose、`/verbose`、`/compact-ui`。（验证：文档搜索和人工核对。）
- [ ] C48：文档明确最终回答不截断、权限与错误不隐藏。（验证：人工核对说明段落。）
- [ ] C49：配置、示例、文档和终端快照不含真实凭据。（验证：敏感值模式扫描。）

## 编译与回归

- [ ] C50：配置、策略、布局、摘要、JLine 和 ConversationLoop 聚焦测试全部通过。（验证：运行指定 Maven 测试，0 failures、0 errors。）
- [ ] C51：三个 Provider、Agent、工具、权限、MCP 和上下文测试行为不变。（验证：JDK 21 全量测试。）
- [ ] C52：`mvn clean package` BUILD SUCCESS，记录 tests、failures、errors、skipped。（验证：Maven 输出与 Surefire 报告。）
- [ ] C53：shaded JAR 存在，默认 compact 可观察到恢复后的完整启动面板并正常退出。（验证：真实 Java 进程退出码为 0。）
- [ ] C54：Git 提交不包含 `claude.md`、`hello.txt` 或忽略的本地 `config.yaml`。（验证：每次暂存名单及最终状态。）
- [ ] C55：除启动面板外，compact 的短提示符、Thinking/Usage/状态过滤、工具单行摘要、MCP 降噪、权限和错误可见性全部保持不变。（验证：原 UI 聚焦测试与真实 compact 工具回合回归通过。）

## 端到端场景

- [ ] E1：默认 compact——启动时看到原完整响应式面板；可控 LLM 返回 Thinking → ReadFile → Usage → 最终回答后，终端只新增一条 Read 完成行和完整回答。
- [ ] E2：切换 verbose——输入 `/verbose` 后不重绘启动面板；再次执行同类请求时恢复 Thinking、Usage、queued/running/完成详细行。
- [ ] E3：切回 compact——输入 `/compact-ui` 后不重绘启动面板，第三轮再次精简，且切换命令均未进入模型请求。
- [ ] E4：失败可见——compact 中执行一个失败工具，看到唯一失败摘要与安全错误，Agent 后续仍可回复。
- [ ] E5：权限可见——compact 中触发需要 HITL 的操作，确认界面完整，拒绝后结果可见且未执行工具副作用。
- [ ] E6：窄终端——40 列下运行同一请求，启动、工具、回答和输入提示均不越界。
- [ ] E7：tmux 优先验收——优先在 tmux 完成 E1-E6；若环境无 tmux，报告记录阻塞并用真实 shaded JAR 子进程和可控服务提供等价证据。
