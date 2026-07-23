# 第二章：LLM API 与终端多轮对话 Checklist

> 每一项都必须通过运行命令、发送请求或观察终端行为验证。先记录实际证据，再标记通过。

## 实现完整性

- [ ] Maven 工程使用 Java 21，并声明 JLine、Jackson 和 JUnit 5 依赖。（验证：运行 `mvn -q help:effective-pom`，确认编译版本和依赖配置）
- [ ] 项目可以生成以 `io.imiocode.ImioCodeApplication` 为主类的可执行 JAR。（验证：运行 `mvn -q package`，然后检查 JAR 清单并执行 `java -jar target/imiocode-*.jar`）
- [ ] OpenAI、Anthropic 和 DeepSeek 均有独立的 LLM 客户端实现。（验证：运行 `mvn -q -DskipTests compile`，确认三个适配器均参与编译）
- [ ] 会话层只依赖统一的 `LlmClient`，不引用任何厂商专属类型。（验证：运行 `rg -n "OpenAiClient|AnthropicClient|DeepSeekClient" src/main/java/io/imiocode/conversation`，期望无匹配）
- [ ] 终端层不负责保存对话历史，也不直接访问厂商 API。（验证：运行 `rg -n "ChatMessage|HttpClient|OpenAi|Anthropic|DeepSeek" src/main/java/io/imiocode/terminal`，期望无不合理依赖）
- [ ] 代码中未实现工具调用、文件操作、Shell 执行、会话持久化或运行时模型切换。（验证：检查公开命令和主流程，确认只有对话、退出及本章配置能力）

## 启动配置

- [ ] 缺少 `IMIO_PROVIDER` 时，程序拒绝进入对话并指出该配置项。（验证：清除该变量后运行 JAR，期望非零退出且出现 `IMIO_PROVIDER`）
- [ ] 缺少 `IMIO_MODEL` 时，程序拒绝进入对话并指出该配置项。（验证：设置厂商和 Key、清除模型后启动，期望非零退出且出现 `IMIO_MODEL`）
- [ ] 当前厂商缺少对应 API Key 时，程序明确指出缺失项。（验证：分别选择三个厂商并清除对应 Key，期望显示正确变量名）
- [ ] `IMIO_PROVIDER` 只接受 `openai`、`anthropic` 和 `deepseek`，且大小写不敏感。（验证：测试三个合法值、大小写变体和一个非法值）
- [ ] 三个厂商均使用正确的默认 Base URL。（验证：运行 `ConfigLoaderTest`，确认 OpenAI、Anthropic、DeepSeek 默认地址断言通过）
- [ ] 自定义 Base URL 可以覆盖当前厂商默认地址。（验证：指向 `MockLlmServer`，确认请求到达自定义地址）
- [ ] 连接超时、请求超时和最大输出 Token 数采用明确默认值。（验证：不提供可选变量运行 `ConfigLoaderTest`，确认默认值断言通过）
- [ ] 超时或 Token 上限为非数字、零或负数时启动失败。（验证：分别设置非法值启动，期望显示对应配置错误）
- [ ] 配置对象、启动信息和错误输出均不包含完整 API Key。（验证：使用唯一测试 Key 运行配置测试，再用 `rg` 搜索测试输出和构建报告，期望无明文泄漏）

## OpenAI 集成

- [ ] OpenAI 请求发送到 `{baseUrl}/v1/responses`。（验证：运行 `OpenAiClientTest`，检查模拟服务记录的路径）
- [ ] OpenAI 请求使用 Bearer 认证，并包含配置的模型、历史、流式开关和最大输出 Token 数。（验证：检查 `OpenAiClientTest` 的请求头与 JSON 断言）
- [ ] `response.output_text.delta` 事件到达后立即产生对应文本增量。（验证：模拟分段事件，观察监听器按相同顺序收到文本）
- [ ] 只有收到 `response.completed` 后才返回完整 `ChatResponse`。（验证：分别运行带完成事件和缺少完成事件的测试）
- [ ] OpenAI 的 HTTP 错误、失败事件、非法 JSON 和异常断流均转换为统一错误。（验证：运行 `mvn -q -Dtest=OpenAiClientTest test`）
- [ ] 关闭 OpenAI 客户端会终止活动响应流，重复关闭不会报错。（验证：运行适配器关闭契约测试）

## Anthropic 集成

- [ ] Anthropic 请求发送到 `{baseUrl}/v1/messages`。（验证：运行 `AnthropicClientTest`，检查模拟服务记录的路径）
- [ ] Anthropic 请求包含 `x-api-key`、协议版本头、模型、消息、`stream=true` 和 `max_tokens`。（验证：检查请求头和 JSON 断言）
- [ ] 只有文本内容块增量会输出到终端，非文本内容块不产生文本。（验证：模拟文本和非文本内容块，检查监听器结果）
- [ ] 只有收到 `message_stop` 后才返回完整 `ChatResponse`。（验证：分别运行正常完成和缺少停止事件的测试）
- [ ] Anthropic 的 HTTP 错误、流内错误、非法事件顺序和异常断流均转换为统一错误。（验证：运行 `mvn -q -Dtest=AnthropicClientTest test`）
- [ ] 关闭 Anthropic 客户端会终止活动响应流，重复关闭不会报错。（验证：运行适配器关闭契约测试）

## DeepSeek 集成

- [ ] DeepSeek 请求发送到 `{baseUrl}/chat/completions`。（验证：运行 `DeepSeekClientTest`，检查模拟服务记录的路径）
- [ ] DeepSeek 请求使用 Bearer 认证，并包含模型、完整历史、`stream=true` 和最大输出 Token 数。（验证：检查请求头和 JSON 断言）
- [ ] `choices[0].delta.content` 中的非空文本按顺序输出，空增量被忽略。（验证：模拟空增量和多个文本增量，检查监听器结果）
- [ ] 只有收到合法结束原因和 `[DONE]` 后才返回完整 `ChatResponse`。（验证：分别运行正常结束和缺少结束标记的测试）
- [ ] DeepSeek 的错误对象、HTTP 错误、缺失选择项、非法 JSON 和异常断流均转换为统一错误。（验证：运行 `mvn -q -Dtest=DeepSeekClientTest test`）
- [ ] 关闭 DeepSeek 客户端会终止活动响应流，重复关闭不会报错。（验证：运行适配器关闭契约测试）

## 流式传输

- [ ] SSE 读取器正确处理单行和多行 `data:` 字段。（验证：运行 `SseEventReaderTest`）
- [ ] SSE 注释和未知字段不会被错误解释为文本内容。（验证：运行包含注释和未知字段的读取器测试）
- [ ] 中文、英文、代码和多行文本经过 SSE 分片后没有乱码或丢失。（验证：运行 UTF-8 分片测试并比较完整输出）
- [ ] 每个有效文本片段到达后立即调用监听器，不等待完整响应。（验证：让模拟服务延迟发送片段，记录每个片段到达时的输出）
- [ ] 网络连接失败映射为 `NETWORK`，请求超时映射为 `TIMEOUT`。（验证：运行对应传输和适配器测试）
- [ ] 响应中途断开时不得返回成功结果。（验证：模拟服务发送部分文本后断开，期望抛出统一异常）
- [ ] 流式异常后已显示的部分文本保留可见，并追加“本轮响应未完成”提示。（验证：运行对话循环集成测试，观察捕获输出）

## 多轮会话

- [ ] 第一轮成功后，历史按“用户、助手”顺序保存完整消息对。（验证：运行 `ConversationSessionTest` 并检查历史快照）
- [ ] 第二轮请求包含第一轮完整用户消息和助手回复。（验证：使用 fake 客户端捕获第二轮 `ChatRequest`）
- [ ] 每次请求使用不可变历史快照，外部修改不会改变内部历史。（验证：尝试修改 `historySnapshot()`，期望失败或不影响会话）
- [ ] 成功回复完成前，当前用户消息不会提前加入正式历史。（验证：在流式回调期间检查历史大小保持不变）
- [ ] 完整回复成功后，用户消息和助手回复同时提交。（验证：检查调用前后历史只从 N 增加到 N+2）
- [ ] 响应中断、超时或协议错误时，本轮用户消息和部分回复均不加入历史。（验证：模拟部分输出后失败，比较失败前后的历史快照）
- [ ] 失败后的下一轮请求从最后一份有效历史继续。（验证：先制造失败，再发送成功消息，检查捕获的请求历史）
- [ ] 会话历史只存在于当前进程，退出后不会写入文件或数据库。（验证：结束程序后检查工作目录和用户配置目录没有新增会话文件）

## 终端交互

- [ ] 启动成功后显示当前厂商、模型和用户输入提示，但不显示 API Key。（验证：在 tmux 中启动并捕获首屏输出）
- [ ] 空输入和纯空白输入不会调用 LLM，终端继续等待输入。（验证：在 `ConversationLoopTest` 中检查 fake 客户端调用次数为零）
- [ ] 有效输入只触发一次 LLM 请求。（验证：输入单条消息并检查 fake 客户端调用次数为一）
- [ ] 模型文本以助手前缀开始，并随片段实时刷新。（验证：模拟延迟片段，在片段之间捕获终端输出）
- [ ] 完整回复后终端补充换行并重新显示输入提示。（验证：完成一次模拟请求后检查输出顺序）
- [ ] `/exit` 和 `/quit` 均可结束程序，且不触发 LLM 请求。（验证：分别输入两个命令并检查退出状态和调用次数）
- [ ] Ctrl+C 在等待输入时结束程序。（验证：在 tmux 中发送 Ctrl+C，确认进程退出）
- [ ] Ctrl+C 在流式响应期间关闭活动请求、丢弃当前轮历史并结束程序。（验证：模拟慢速响应，在中途发送 Ctrl+C，确认无后续片段和新输入提示）
- [ ] 错误提示结束后，下一次输入提示从新行开始，不与错误文本重叠。（验证：模拟可恢复错误并检查终端输出）
- [ ] 终端尺寸变化或不支持富文本时，纯文本输入和输出仍可使用。（验证：调整 tmux pane 尺寸后完成一轮对话）

## 错误处理与安全

- [ ] 401/403 被识别为认证错误，429 被识别为限流，5xx 被识别为服务端错误。（验证：对模拟服务的对应状态运行三个适配器测试）
- [ ] 模型不存在错误被映射为可理解的安全消息。（验证：模拟厂商模型错误代码，检查 `LlmErrorType.MODEL_NOT_FOUND`）
- [ ] 可恢复错误后程序重新进入输入状态。（验证：先返回 429，再返回成功响应，确认第二条消息可以完成）
- [ ] 客户端已关闭等不可恢复状态会结束循环。（验证：关闭客户端后发起请求，确认循环停止）
- [ ] 未知异常不会以未处理堆栈形式直接显示给终端用户。（验证：fake 客户端抛出未知异常，检查终端只显示安全兜底消息）
- [ ] 普通输出、错误输出、测试报告和可执行 JAR 中均不存在真实 API Key。（验证：使用密钥特征串执行 `rg -a` 搜索，期望无匹配）
- [ ] 一家厂商的协议错误不会影响另外两家客户端的构造和测试。（验证：让一家专项测试失败响应，另外两家契约测试仍独立通过）
- [ ] 程序正常退出、错误退出和中断退出后均释放终端、响应流和 HTTP 相关资源。（验证：重复运行测试，确认无端口占用、挂起线程或无法退出的进程）

## 编译与自动化测试

- [ ] 项目可以从干净状态编译。（验证：运行 `mvn -q clean compile`，期望退出码为 0）
- [ ] 所有测试源码可以编译。（验证：运行 `mvn -q test-compile`，期望退出码为 0）
- [ ] 配置测试全部通过。（验证：运行 `mvn -q -Dtest=ConfigLoaderTest test`）
- [ ] SSE 传输测试全部通过。（验证：运行 `mvn -q -Dtest=SseEventReaderTest test`）
- [ ] 三个厂商适配器专项测试全部通过。（验证：运行 `mvn -q -Dtest=OpenAiClientTest,AnthropicClientTest,DeepSeekClientTest test`）
- [ ] 三个厂商通过相同的公共客户端契约。（验证：运行包含 `LlmClientContractTest` 的适配器测试集）
- [ ] 会话测试全部通过。（验证：运行 `mvn -q -Dtest=ConversationSessionTest test`）
- [ ] 终端和循环测试全部通过。（验证：运行 `mvn -q -Dtest=JLineTerminalUiTest,ConversationLoopTest test`）
- [ ] 全部自动化测试无跳过、无失败。（验证：运行 `mvn -q clean test`，检查 Surefire 报告）
- [ ] 可执行 JAR 从干净构建成功生成。（验证：运行 `mvn -q clean package`，期望 `target` 中存在可运行 JAR）

## Spec 验收标准映射

- [ ] AC1：缺失或无效配置会阻止启动，错误指出配置项且不泄露 Key。（验证：执行启动配置检查组）
- [ ] AC2：三个厂商分别请求正确原生端点，并使用配置的模型、认证和地址。（验证：执行三个适配器请求映射测试）
- [ ] AC3：终端显示提示、增量回复，并在完成后重新接受输入。（验证：执行循环测试和任一 tmux 场景）
- [ ] AC4：第二轮能够使用第一轮对话上下文。（验证：执行会话测试和真实两轮对话）
- [ ] AC5：空白输入不触发 API 请求。（验证：执行空输入循环测试）
- [ ] AC6：退出指令和 Ctrl+C 均能停止程序。（验证：执行退出测试和 tmux 中断场景）
- [ ] AC7：认证、限流、模型、服务端和未知错误均有安全提示。（验证：执行三个适配器错误测试）
- [ ] AC8：网络、超时和断流明确提示失败，不污染历史。（验证：执行断流及会话回滚测试）
- [ ] AC9：三个厂商使用相同的终端操作流程。（验证：执行公共契约测试和三组 tmux 场景）
- [ ] AC10：中文、英文、代码和多行输出无乱码或丢失。（验证：执行 UTF-8 测试和真实终端观察）
- [ ] AC11：单个厂商失败不会导致未处理异常或破坏其他厂商。（验证：独立运行三个适配器错误测试）
- [ ] AC12：只有完整回复进入历史，异常结束不提交。（验证：执行完成与断流会话测试）
- [ ] AC13：三家真实 API 均完成规定的 tmux 端到端验证。（验证：执行下方三个场景并保存输出证据）

## 端到端场景

### 场景 1：OpenAI 两轮对话

- [ ] 使用 OpenAI 配置在 tmux 会话 `imiocode-openai` 中启动可执行 JAR。（验证：捕获输出包含 `openai`、模型名和输入提示）
- [ ] 输入“请记住验证码是 IMIO-2749，只回复已记住。”后，回复以增量方式显示。（验证：观察生成过程，不是等待完整结果后一次性出现）
- [ ] 第二轮输入“我刚才让你记住的验证码是什么？”，回复包含 `IMIO-2749`。（验证：`tmux capture-pane -p -t imiocode-openai`）
- [ ] 输入 `/exit` 后程序正常结束。（验证：tmux pane 中进程已退出，无堆栈）

### 场景 2：Anthropic 两轮对话

- [ ] 使用 Anthropic 配置在 tmux 会话 `imiocode-anthropic` 中启动同一 JAR。（验证：捕获输出包含 `anthropic`、模型名和输入提示）
- [ ] 完成与场景 1 相同的两轮验证码对话，回复增量可见且第二轮包含 `IMIO-2749`。（验证：`tmux capture-pane -p -t imiocode-anthropic`）
- [ ] 操作方式、提示符和错误展示与 OpenAI 一致。（验证：并排比较两个 pane 的交互结构）
- [ ] 输入 `/exit` 后程序正常结束。（验证：无挂起进程和未处理异常）

### 场景 3：DeepSeek 两轮对话

- [ ] 使用 DeepSeek 配置在 tmux 会话 `imiocode-deepseek` 中启动同一 JAR。（验证：捕获输出包含 `deepseek`、模型名和输入提示）
- [ ] 完成与场景 1 相同的两轮验证码对话，回复增量可见且第二轮包含 `IMIO-2749`。（验证：`tmux capture-pane -p -t imiocode-deepseek`）
- [ ] 操作方式、提示符和错误展示与另外两家一致。（验证：比较三个 pane 的交互结构）
- [ ] 输入 `/exit` 后程序正常结束。（验证：无挂起进程和未处理异常）

### 场景 4：流中断与历史回滚

- [ ] 使用模拟服务返回若干文本片段后中断连接。（验证：终端保留部分文本并显示本轮未完成）
- [ ] 中断后再次发送消息，捕获请求中不包含失败轮次的用户消息和部分回复。（验证：检查 `MockLlmServer` 记录的下一轮请求）
- [ ] 程序在可恢复断流后仍可完成下一轮正常请求。（验证：下一轮显示完整回复并重新出现提示）

### 场景 5：Ctrl+C 中断生成

- [ ] 使用模拟慢速流在 tmux 中启动 ImioCode，并在输出中途发送 Ctrl+C。（验证：活动流立即停止）
- [ ] Ctrl+C 后不再输出新的模型片段，也不再显示下一次输入提示。（验证：等待短时间后再次捕获 pane，内容未继续增长）
- [ ] 进程正常结束，未显示 API Key 或未处理异常堆栈。（验证：检查最终 pane 输出和进程状态）

## 验收记录

## 内联终端 UI 增量验收（2026-07-23）

- [x] 启动面板显示 Logo、产品名、Manifest 版本、厂商、模型、工作目录和 `Ready`，且不显示 API Key。（验证：`TerminalLayoutTest`、`JLineTerminalUiTest` 及可执行 JAR 首屏）
- [x] 终端按能力和宽度选择完整、紧凑或纯文本模式。（验证：20、40、60、100 列布局测试及显式 dumb terminal 测试）
- [x] 所有面板、输入区和状态栏行在中文、长模型名、长目录下均不超过终端宽度。（验证：逐行使用 JLine 列宽计算断言）
- [x] 富终端输入区具有边框、主提示符、续行提示符和 `chat`/模型状态栏。（验证：`TerminalLayoutTest`）
- [x] `Alt+Enter` 插入换行，`Enter` 将多行内容作为一条消息提交，消息不含边框、状态或 ANSI。（验证：真实按键序列测试与 `ConversationLoopTest` 请求内容断言）
- [x] 请求状态按 `Ready → Thinking… → Streaming → Ready` 变化，失败时进入 `Error`。（验证：`ConversationLoopTest` 精确状态序列断言）
- [x] 状态更新不改变流式片段顺序，也不污染会话历史。（验证：`ConversationLoopTest`、`ConversationSessionTest`）
- [x] dumb terminal 不输出 ANSI，纯文本输入、流式回复、状态和退出仍可使用。（验证：`JLineTerminalUiTest` 与非交互可执行 JAR 验收）
- [x] 可执行 JAR 使用 Manifest 版本，并完成 DeepSeek 两轮真实流式对话和正常退出。（验证：首屏显示 `0.2.0-SNAPSHOT`；第二轮返回 `IMIO-2749`；退出码 0）
- [ ] 在 tmux 中调整 pane 宽度并人工验证光标编辑、历史键和滚动历史。（未执行：当前 Windows 无 tmux，WSL 无已安装发行版）

### UI 增量验收结论

- 自动化与当前环境可执行项：9/9 通过。
- tmux 人工交互项：0/1，受本机缺少 tmux/WSL 发行版阻塞。
- 未擅自安装 WSL、Linux 发行版或其他系统级组件。

| 项目 | 结果 | 证据 |
|------|------|------|
| 自动化测试 | 待执行 | 记录命令、退出码和测试数量 |
| OpenAI 端到端 | 待执行 | 记录 tmux capture 输出摘要 |
| Anthropic 端到端 | 待执行 | 记录 tmux capture 输出摘要 |
| DeepSeek 端到端 | 待执行 | 记录 tmux capture 输出摘要 |
| 流中断回滚 | 待执行 | 记录模拟服务请求与终端输出 |
| Ctrl+C 中断 | 待执行 | 记录进程状态与终端输出 |

## 本次验收报告（2026-07-21）

### 通过（6/10）

- [x] Java 21 干净构建通过。证据：`mvn -q clean package` 退出码为 0。
- [x] 自动化测试全部通过。证据：10 个测试套件、35 个测试，0 失败、0 错误、0 跳过。
- [x] 三家厂商请求映射、流式事件、完成条件和 HTTP 错误通过本地模拟服务验证。
- [x] 多轮历史、成功原子提交和失败回滚通过测试。
- [x] JLine UTF-8 输入输出、助手增量展示、错误换行和中断回调通过测试。
- [x] 可执行 JAR 缺少配置时安全失败。证据：退出码 2，输出“缺少配置项 IMIO_PROVIDER”，无堆栈和密钥。

### 未通过或未执行（4/10）

- [ ] OpenAI 真实 API：完整 JAR 已发起测试，但当前网络连接在约 10 秒后超时；程序正确显示“模型请求超时，本轮响应未完成”，继续接受输入并通过 `/exit` 退出。
- [ ] Anthropic 真实 API：未配置 `ANTHROPIC_API_KEY`，无法执行。
- [x] DeepSeek 真实 API：已通过 `config.yaml` 执行两轮真实对话；第二轮正确返回 `IMIO-2749`，并通过 `/exit` 正常退出。
- [ ] tmux 场景：Windows 主机无 tmux，WSL 未安装 Linux 发行版，无法执行规定的 tmux 验收。

### 产物

- 可执行 JAR：`target/imiocode-0.2.0-SNAPSHOT-all.jar`
- 普通 JAR：`target/imiocode-0.2.0-SNAPSHOT.jar`

## config.yaml 验收

- [ ] 当前工作目录中的 `config.yaml` 可以独立提供完整启动配置。（验证：清除环境变量后启动，确认程序读取 YAML）
- [ ] 环境变量优先覆盖 YAML 中的同名字段。（验证：仅覆盖模型或 Base URL，检查实际请求）
- [ ] 未被环境变量覆盖的字段继续使用 YAML 配置。（验证：混合配置测试）
- [ ] 三家厂商配置按 `providers.openai`、`providers.anthropic`、`providers.deepseek` 正确读取。（验证：配置加载测试）
- [ ] `config.yaml` 不存在时，纯环境变量配置仍能启动。（验证：临时目录启动测试）
- [ ] YAML 语法错误会阻止启动并显示安全错误。（验证：使用非法 YAML 启动）
- [ ] 未知 YAML 字段会阻止启动并指出字段位置。（验证：加入拼写错误字段）
- [ ] YAML 数字字段、URI 和厂商值类型错误会阻止启动。（验证：分别使用非法值启动）
- [ ] 当前厂商缺少 Key 时启动失败，错误不泄漏其他厂商 Key。（验证：删除当前厂商 Key 后启动）
- [ ] `config.example.yaml` 存在且不含任何真实 API Key。（验证：搜索示例文件）
- [ ] `config.yaml` 被版本控制忽略。（验证：运行 `git check-ignore config.yaml`）
- [ ] API Key 不出现在启动信息、异常、测试报告和 JAR 内容中。（验证：执行密钥字符串搜索）
- [ ] 使用 YAML 配置完成 DeepSeek 两轮真实对话。（验证：运行 JAR，输入验证码记忆场景并观察第二轮回复）
- [ ] YAML 配置启动后 `/exit` 能正常退出。（验证：输入 `/exit` 并检查退出码）

## config.yaml 验收报告（2026-07-21）

- [x] 当前工作目录 `config.yaml` 可独立提供完整启动配置。证据：清除 DeepSeek 相关环境变量后，程序从 YAML 读取 `deepseek-chat` 并启动。
- [x] 环境变量优先覆盖 YAML 中的同名字段。证据：`ConfigLoaderTest` 覆盖 `IMIO_MODEL`、`IMIO_CONNECT_TIMEOUT_SECONDS` 和 `DEEPSEEK_BASE_URL` 局部覆盖。
- [x] 未被环境变量覆盖的字段继续使用 YAML 配置。证据：混合配置测试中 API Key、provider、Token 上限等仍来自 YAML。
- [x] 三家厂商分组配置可读取。证据：`YamlConfigLoaderTest` 覆盖 `providers.openai`、`providers.anthropic`、`providers.deepseek`。
- [x] `config.yaml` 不存在时纯环境变量配置仍可启动。证据：原有配置测试均在无 YAML 的临时目录中运行。
- [x] YAML 语法错误、未知字段、错误类型、未知厂商、非法 URI 和缺少当前厂商 Key 均安全失败。证据：`YamlConfigLoaderTest` 与 `ConfigLoaderTest` 覆盖相关失败路径。
- [x] `config.example.yaml` 不含真实 API Key，`.gitignore` 包含 `/config.yaml`。证据：示例文件安全测试通过；当前目录非有效 Git 工作树，无法执行 `git check-ignore`。
- [x] API Key 未进入源码、文档、示例配置或 JAR。证据：密钥模式搜索显示仅本地 `config.yaml` 包含真实 Key，`target/*.jar` 无匹配。
- [x] DeepSeek YAML 真实对话通过。证据：第一轮回复“已记住”，第二轮回复 `IMIO-2749`，进程退出码为 0。
