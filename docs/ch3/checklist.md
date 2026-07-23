# ImioCode 第三章：工具系统 Checklist

> 每一项都必须通过运行测试、命令或真实终端观察来验证。实施完成后记录实际证据，再将对应条目标记为通过。

## 工具框架

- [ ] AC1：六个工具均能返回名称、说明、JSON Schema 和风险等级；执行结果统一包含成功、输出、错误、截断、耗时和可选退出码。（验证：运行 `mvn -q -Dtest=BaseToolTest test`，检查正常、参数错误、运行异常和截断断言）
- [ ] AC1：工具实现抛出未预期异常时，异常被转换成安全失败结果，测试进程不退出且不出现堆栈内容。（验证：运行 `BaseToolTest` 的异常转换用例，检查 `success=false` 和安全错误）
- [ ] AC2：启动注册六个核心工具，名称分别为 `read_file`、`write_file`、`edit_file`、`bash`、`glob`、`grep`。（验证：运行 `mvn -q -Dtest=ToolRegistryTest test`，检查默认启用集合）
- [ ] AC2：重复注册失败；禁用后无法查询和执行；重新启用后恢复；未知工具返回失败结果。（验证：运行 `mvn -q -Dtest=ToolRegistryTest,ToolExecutorTest test`）
- [ ] AC3：注册中心按稳定顺序分别导出 OpenAI、Anthropic 和 DeepSeek 工具定义，且不包含已禁用工具。（验证：运行三个厂商客户端测试中的工具定义请求断言）
- [ ] 工具参数根 Schema 均为对象，列出必填字段并禁止未知字段。（验证：运行 `ToolRegistryTest` 并检查六个 Schema 的 `type`、`required`、`additionalProperties`）
- [ ] 工具执行器严格串行，生命周期事件依次为等待、运行、成功或失败。（验证：运行 `mvn -q -Dtest=ToolExecutorTest test`，检查事件和线程执行顺序）
- [ ] 一个工具失败后，注册中心和执行器仍可继续处理下一次调用。（验证：运行 `ToolExecutorTest` 的失败后复用用例）

## 工作区与敏感路径

- [ ] AC7：ReadFile、WriteFile、EditFile、Glob、Grep 拒绝绝对路径和 `..` 跳转。（验证：运行 `mvn -q -Dtest=WorkspacePolicyTest test`）
- [ ] AC7：五个文件类工具无法通过符号链接或 Windows 重解析点访问工作区外部。（验证：在支持创建链接的临时目录运行 `WorkspacePolicyTest` 链接逃逸用例）
- [ ] AC7：`config.yaml`、`.env`、`.env.*` 和 `.git/**` 无法读取、写入或编辑。（验证：运行 `WorkspacePolicyTest` 及三个文件工具的敏感路径用例）
- [ ] AC7：Glob 结果不包含受保护路径，Grep 不读取其内容。（验证：在临时工作区放入唯一敏感标记，运行 `GlobToolTest,GrepToolTest`，检查路径和标记均未返回）
- [ ] 路径在实际读取、写入和移动前被重新校验。（验证：运行 `WorkspacePolicyTest,AtomicFileWriterTest` 的校验后目标变化用例）
- [ ] 目录遍历不跟随链接，创建顺序不同的相同目录树得到完全相同的路径顺序。（验证：运行 `mvn -q -Dtest=WorkspaceWalkerTest test`）
- [ ] 遍历扫描量达到 20,000 个路径项后停止并报告截断，不无界保存目录树。（验证：运行 `WorkspaceWalkerTest` 的小限制注入用例）

## ReadFile

- [ ] AC4：`read_file` 能读取工作区内完整 UTF-8 文本并返回从 1 开始的行号。（验证：运行 `mvn -q -Dtest=ReadFileToolTest test`）
- [ ] AC4：指定 `start_line` 和 `end_line` 时返回包含两端的行范围；结束行超过 EOF 时读取到实际末尾。（验证：运行 `ReadFileToolTest` 的范围用例）
- [ ] AC4：文件不存在、起始行超过 EOF、非正行号及结束行小于起始行时返回可理解的失败结果。（验证：运行 `ReadFileToolTest` 的参数和文件错误用例）
- [ ] ReadFile 对无效 UTF-8 和包含 NUL 的二进制文件返回失败，不输出原始字节。（验证：运行 `ReadFileToolTest` 的编码和二进制用例）

## WriteFile

- [ ] AC5：`write_file` 能创建新文件并完整覆盖已有文件，写入内容保持 UTF-8。（验证：运行 `mvn -q -Dtest=WriteFileToolTest test`）
- [ ] AC5：父目录不存在时失败且不自动创建目录。（验证：运行 `WriteFileToolTest` 的父目录缺失用例，检查目录仍不存在）
- [ ] 内容超过 1 MiB 时在写入前失败，原文件字节不变。（验证：运行 `WriteFileToolTest` 的写入上限用例）
- [ ] 写入失败不遗留临时文件或部分目标文件。（验证：运行 `mvn -q -Dtest=AtomicFileWriterTest test`）

## EditFile

- [ ] AC6：`edit_file` 在旧文本恰好出现一次时完成精确替换。（验证：运行 `mvn -q -Dtest=EditFileToolTest test`）
- [ ] AC6：旧文本出现零次或多次时返回失败且文件字节完全不变。（验证：运行 `EditFileToolTest` 的零次和多次用例）
- [ ] EditFile 支持中文文本和替换为空文本，不执行模糊匹配。（验证：运行 `EditFileToolTest` 的 Unicode 和删除用例）
- [ ] 编辑后的文件超过 1 MiB 时失败且原文件不变。（验证：运行 `EditFileToolTest` 的结果上限用例）

## Bash

- [ ] AC8：`bash` 从工作区启动；Windows 使用 PowerShell，Linux/macOS 使用 `/bin/bash`。（验证：运行 `mvn -q -Dtest=BashToolTest test`，检查工作目录和平台命令）
- [ ] AC8：结果分别保留退出码、stdout 和 stderr；非零退出码形成可回传结果而不终止程序。（验证：运行 `BashToolTest` 的成功、标准错误和非零退出用例）
- [ ] AC8：Bash 定义和终端展示均标记为 HIGH 风险。（验证：运行 `ToolRegistryTest,ToolSummaryFormatterTest`）
- [ ] AC9：命令超过配置测试超时后，主进程及子孙进程均终止。（验证：运行 `BashToolTest` 的短超时及子进程用例，检查进程不再存活）
- [ ] AC9：调用取消后活动命令结束，待执行工具不启动，重复取消不报错。（验证：运行 `BashToolTest,ToolExecutorTest` 的取消用例）
- [ ] AC12：stdout 和 stderr 分别超过限制时被截断并持续排空，命令不会因管道写满而挂起。（验证：运行 `BashToolTest` 的双流大输出用例）
- [ ] AC24：Bash 子进程不继承名称包含 KEY、TOKEN、SECRET、PASSWORD、CREDENTIAL 的环境变量。（验证：运行 `BashToolTest` 的环境枚举用例）
- [ ] AC24：命令输出当前配置测试 Key 或认证头时，工具结果和终端摘要只显示脱敏值。（验证：运行 `SecretRedactorTest,BashToolTest,ToolSummaryFormatterTest`）

## Glob 与 Grep

- [ ] AC10：`glob` 正确支持 `*`、`**`、`?`，输出统一使用 `/`。（验证：运行 `mvn -q -Dtest=GlobToolTest test`）
- [ ] AC10：Glob 结果按字典序稳定排列，最多返回 1,000 条，超过时标记截断。（验证：运行 `GlobToolTest` 的乱序创建和小限制注入用例）
- [ ] AC11：`grep` 使用正则递归搜索，返回相对路径、从 1 开始的行号和匹配行。（验证：运行 `mvn -q -Dtest=GrepToolTest test`）
- [ ] AC11：Grep 可限定文件或子目录，无效正则返回参数错误。（验证：运行 `GrepToolTest` 的路径范围和正则错误用例）
- [ ] AC11：Grep 跳过二进制和无效 UTF-8 文件，最多返回 200 条匹配。（验证：运行 `GrepToolTest` 的二进制、编码和结果限制用例）
- [ ] AC12：Grep 超长行、总输出或扫描量超过固定限制时停止并标记截断。（验证：运行 `GrepToolTest` 的三种限制用例）
- [ ] Glob 和 Grep 在没有 `rg`、`grep`、`find` 外部命令时仍能运行。（验证：测试中使用受限 PATH 运行 `GlobToolTest,GrepToolTest`，期望全部通过）

## 模型协议

- [ ] AC13：OpenAI 请求携带已启用工具，同时原有纯文本流式响应仍正常完成。（验证：运行 `mvn -q -Dtest=OpenAiClientTest test`）
- [ ] AC13：Anthropic 请求携带已启用工具，同时原有纯文本流式响应仍正常完成。（验证：运行 `mvn -q -Dtest=AnthropicClientTest test`）
- [ ] AC13：DeepSeek 请求携带已启用工具，同时原有纯文本流式响应仍正常完成。（验证：运行 `mvn -q -Dtest=DeepSeekClientTest test`）
- [ ] AC14：每家客户端都能将单个工具参数的多个 JSON 碎片还原为完整对象。（验证：运行三个厂商客户端测试的碎片参数用例）
- [ ] AC14：两个工具调用的参数交错到达时，调用 ID、名称和参数不会互相混合，结果顺序按调用位置稳定。（验证：运行 `mvn -q -Dtest=ToolCallAssemblerTest,LlmClientContractTest test`）
- [ ] AC15：缺少调用 ID、缺少工具名称、无效 JSON 和非对象 JSON 均产生安全协议错误，且没有工具执行。（验证：运行 `ToolCallAssemblerTest` 及三个客户端错误流用例）
- [ ] AC15：SSE 缺少正常完成事件时不返回半成品响应。（验证：运行三个客户端的不完整流测试）
- [ ] AC16：同一响应含文本和工具调用时，文本被流式监听器接收，完整调用保留在统一响应中。（验证：运行 `LlmClientContractTest` 的混合响应契约）
- [ ] OpenAI 使用 `call_id` 关联 `function_call` 和 `function_call_output`。（验证：检查 `OpenAiClientTest` 捕获的第二请求 JSON）
- [ ] Anthropic 将 `tool_result` 作为紧邻助手调用的用户内容块，并为失败结果设置 `is_error=true`。（验证：检查 `AnthropicClientTest` 捕获的第二请求 JSON）
- [ ] DeepSeek 将每个工具结果编码为带 `tool_call_id` 的独立 `tool` 消息。（验证：检查 `DeepSeekClientTest` 捕获的第二请求 JSON）
- [ ] AC22：同一模拟场景在三家客户端中得到相同的文本、调用顺序、参数和结果关联。（验证：运行 `mvn -q -Dtest=LlmClientContractTest test`）

## 会话编排

- [ ] AC17：首次模型响应包含多个工具时按顺序串行执行。（验证：运行 `mvn -q -Dtest=ConversationSessionTest test` 的多工具用例）
- [ ] AC17：中间工具失败会生成失败结果，后续工具仍执行，全部结果一次性进入第二请求。（验证：检查 `ConversationSessionTest` 的执行及请求记录）
- [ ] AC18：工具结果回传后只发起一次最终模型请求。（验证：运行 `ConversationSessionTest` 并断言客户端调用次数恰好为 2）
- [ ] AC18：最终响应再次请求工具时不执行第二批调用，并返回明确单轮限制提示。（验证：运行 `ConversationSessionTest,ConversationLoopTest` 的第二工具批次用例）
- [ ] AC19：纯文本成功轮次只提交用户和助手消息。（验证：运行 `ConversationSessionTest` 的纯文本历史用例）
- [ ] AC19：完整工具轮次按用户、首次助手、工具结果、最终助手顺序原子提交。（验证：运行 `ConversationSessionTest` 的工具历史快照用例）
- [ ] AC19：首次请求失败、工具中断、最终请求失败和第二次工具请求均不污染正式历史。（验证：运行 `ConversationSessionTest` 的失败事务用例）
- [ ] AC19：工具已经运行但最终请求失败时，界面说明工具已执行、回复未完成，不声称回滚。（验证：运行 `ConversationLoopTest` 的后续请求失败用例）
- [ ] 工具失败结果与原调用 ID 一一对应，多个结果顺序不发生变化。（验证：检查 `ConversationSessionTest` 捕获的工具消息）
- [ ] 会话关闭会同时取消活动工具和模型请求，重复关闭无异常。（验证：运行 `ConversationSessionTest` 的关闭幂等用例）

## 终端 UI 与中断

- [ ] AC20：终端依次展示工具名称、风险、输入摘要、等待、运行、成功或失败及结果摘要。（验证：运行 `mvn -q -Dtest=JLineTerminalUiTest,ConversationLoopTest test`）
- [ ] AC20：WriteFile 和 EditFile 摘要只显示路径和字符数，不显示完整正文。（验证：运行 `ToolSummaryFormatterTest`）
- [ ] AC20：Bash 命令及所有摘要最多显示 240 字符，截断状态可见。（验证：运行 `ToolSummaryFormatterTest` 的长输入用例）
- [ ] AC20：ANSI、边框和状态文字不进入工具参数、工具结果或会话历史。（验证：运行 `JLineTerminalUiTest,ConversationLoopTest` 的领域数据断言）
- [ ] 工具状态在 FULL、COMPACT 和 PLAIN 三种模式中均可理解。（验证：运行 `mvn -q -Dtest=TerminalLayoutTest test`）
- [ ] 首次助手文本、工具事件和最终助手文本各自正确换行，不覆盖历史输出。（验证：运行 `JLineTerminalUiTest` 的混合流程用例）
- [ ] AC21：用户中断时，待执行工具不启动、活动命令停止且不发起工具结果回传请求。（验证：运行 `ConversationLoopTest,ConversationSessionTest` 的中断用例）
- [ ] AC21：中断后不再次读取输入，也不显示下一次提示符。（验证：检查 `ConversationLoopTest` fake 终端的读取次数和输出）

## Unicode、安全与兼容性

- [ ] AC23：中文文件名和中文内容可被读取、写入、编辑、Glob 和 Grep 正确处理。（验证：运行全部核心工具的 Unicode 用例）
- [ ] AC23：工具 JSON 参数被拆在多字节字符边界附近时仍能正确拼接和解析。（验证：运行 `ToolCallAssemblerTest` 的 Unicode 碎片用例）
- [ ] AC23：终端可以显示中文工具摘要和最终回复，不出现替换字符。（验证：运行 `JLineTerminalUiTest` 的 UTF-8 输出用例）
- [ ] AC24：应用产生的错误、工具结果、终端输出和会话历史不含当前配置测试 API Key 或认证头。（验证：运行 `SecretRedactorTest,BaseToolTest,ConversationSessionTest,JLineTerminalUiTest`）
- [ ] AC25：原有配置、纯文本、多轮、HTTP/SSE 和终端测试全部继续通过。（验证：运行 `mvn clean test`）
- [ ] 测试不读取真实 `config.yaml`、用户文件或真实 API Key，不调用外部模型服务。（验证：检查测试全部使用临时目录、固定假 Key 和 `MockLlmServer`）
- [ ] Maven 依赖未增加，配置文件格式未改变。（验证：检查 `pom.xml` 和配置类型的 Git diff）
- [ ] `claude.md` 的用户原有修改未被覆盖或加入本章提交。（验证：执行 `git diff -- claude.md` 并与开发前状态核对）

## 编译与测试

- [ ] AC27：Java 21 主代码干净编译。（验证：运行 `mvn clean compile`，期望退出码 0）
- [ ] AC27：全部单元和集成测试通过。（验证：运行 `mvn clean test`，期望 Surefire 报告 0 failures、0 errors）
- [ ] AC27：可执行 JAR 打包成功。（验证：运行 `mvn package`，检查 `target/imiocode-0.2.0-SNAPSHOT-all.jar` 存在）
- [ ] 测试报告没有非预期跳过；仅允许当前操作系统无法创建符号链接时记录平台限制。（验证：检查 `target/surefire-reports`）
- [ ] Git 变更只包含第三章文档及 task.md 文件清单中的实现和测试。（验证：运行 `git status --short` 和 `git diff --stat`）

## tmux 端到端

### 场景一：真实读取并生成最终回答

- [ ] AC26：在 tmux 中启动打包后的 ImioCode，输入“请读取 pom.xml，告诉我项目使用的 Java 版本”，观察到 `read_file` 等待、运行、成功、结果回传和最终回答。（验证：使用 `tmux capture-pane -p -t imiocode-ch3` 保存实际输出）
- [ ] AC26：场景一只执行第一批工具，工具结果后生成一次最终文本，不出现第二批工具执行。（验证：检查 tmux 输出中的工具运行次数和最终回复顺序）
- [ ] 场景一结束后输入 `/exit`，进程和 tmux pane 正常退出。（验证：运行 `tmux list-panes -t imiocode-ch3 -F '#{pane_dead}'` 或检查会话结束）

建议命令：

```bash
tmux new-session -d -s imiocode-ch3 \
  "java -jar target/imiocode-0.2.0-SNAPSHOT-all.jar"
tmux send-keys -t imiocode-ch3 \
  "请读取 pom.xml，告诉我项目使用的 Java 版本。" Enter
tmux capture-pane -p -t imiocode-ch3
```

### 场景二：多个工具串行

- [ ] 输入“请调用 glob 查找 Java 文件，同时调用 grep 搜索 LlmClient，然后总结结果”，观察多个工具按展示顺序串行运行。（验证：捕获 pane，比较等待、运行和完成顺序）
- [ ] 全部工具结果在同一次后续模型请求中使用，最终回复同时包含文件列表和搜索结论。（验证：检查终端输出及测试日志中记录的请求次数）

### 场景三：写入与精确编辑

- [ ] 让模型使用 `write_file` 创建 `target/ch3-e2e.txt`，内容为 `old-value`，再用 `edit_file` 改为 `new-value`；观察两个工具串行成功。（验证：运行 `Get-Content target/ch3-e2e.txt` 或 `cat target/ch3-e2e.txt`，期望仅为 `new-value`）
- [ ] E2E 临时文件验证后被删除，不影响项目源码。（验证：删除 `target/ch3-e2e.txt` 后运行 `git status --short`，期望无该文件）

### 场景四：中断活动命令

- [ ] 让模型调用 Bash 执行一个 30 秒等待命令，在运行状态按 Ctrl+C，观察命令停止且没有最终模型请求或下一输入提示。（验证：`tmux send-keys -t imiocode-ch3 C-c` 后捕获 pane）
- [ ] 中断后不存在该场景遗留的等待子进程。（验证：按平台检查进程列表，并确认 ImioCode 会话已退出）

### 场景五：单轮边界

- [ ] 输入需要“先读取文件，再根据内容请求另一个工具”的请求；若最终响应再次产生工具调用，观察系统显示单轮限制且不执行第二批工具。（验证：捕获 pane，确认限制提示后没有新的 RUNNING 事件）
- [ ] 场景五已执行工具产生的可见结果不被声称回滚，未完成轮次不影响下一次启动后的正常对话。（验证：检查提示文字并重新启动一次场景一）

## 最终验收记录

- [ ] AC1–AC27 均至少有一条已通过的对应检查项。（验证：搜索本文件中的 AC 编号并核对勾选状态）
- [x] 所有未通过项均记录预期、实际结果、证据和修复方案。（验证：见下方 2026-07-23 验收执行记录）
- [x] 修复后的条目已重新运行原验证方式，而不是只根据代码阅读判断通过。（验证：最终重新执行 `mvn clean test`）

## 2026-07-23 验收执行记录

### 已通过

- [x] Java 21 干净编译、全部自动化测试及集成测试通过。（证据：`mvn clean test`；主源码 68 个、测试源码 28 个）
- [x] 六个核心工具、工作区边界、固定资源限制、注册中心、三家协议、单轮编排、终端展示和取消链路均有自动化覆盖。（证据：Surefire 中对应测试类全部 0 failures、0 errors）
- [x] 应用进程本地端到端通过：真实启动 `ImioCodeApplication`，输入“请读取 pom.xml，告诉我项目使用的 Java 版本”，模型替身请求 `read_file`，结果回传后最终输出“项目使用 Java 21。”。（证据：`ConversationLoopTest.applicationProcessCompletesLocalToolRoundTrip`）
- [x] 可执行 JAR 打包成功。（证据：`target/imiocode-0.2.0-SNAPSHOT-all.jar`）
- [x] Maven 依赖和配置格式未改变，`claude.md` 未加入第三章提交。（证据：`git diff` 与 `git status --short`）

### 未通过或受环境限制

- [ ] tmux 五个端到端场景未执行。（预期：按本文件场景在 tmux 中捕获真实交互；实际：当前 Windows 主机没有 `tmux`，WSL 没有可用发行版；修复方案：在安装 tmux 的 Linux/WSL 环境重新执行本节命令）
- [ ] 外部 DeepSeek 真实对话未执行。（预期：真实模型读取 `pom.xml`；实际：该操作会把项目内容发送给外部服务，当前未获得用户对该数据外传的明确授权；替代证据：已通过仅监听 `127.0.0.1` 的应用进程端到端测试；修复方案：用户明确授权后再运行）
- [ ] Windows 符号链接逃逸用例在当前主机跳过。（预期：创建链接并验证拒绝；实际：当前账户不允许创建符号链接；代码仍逐级拒绝符号链接和 `BasicFileAttributes.isOther()` 重解析点；修复方案：在启用开发者模式或具备创建链接权限的 Windows 环境重跑）
- [ ] Bash 超时已验证主进程退出，子孙进程树终止逻辑由代码实现但当前未单独记录子进程存活断言。（预期：子孙进程均不存活；实际：`BashToolTest` 已覆盖超时、取消和重复关闭，未保存独立子进程 PID；修复方案：后续在 tmux 中执行“场景四”并检查进程列表）
