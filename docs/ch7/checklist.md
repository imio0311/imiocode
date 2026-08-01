# ch7 MCP 开放工具生态 Checklist

> 每一项都通过运行代码、测试或真实进程观察行为来验证。开发完成前不预先勾选。

## 配置与环境隔离

- [x] **AC1 三层配置与整体覆盖**：分别在用户级、项目级、本地级声明不同 Server，再声明一个同名 Server；启动后不同 Server 全部存在，同名 Server 只采用用户级完整配置，没有混合低优先级字段。（验证：运行 `McpConfigLoaderTest` 的三层合并场景）
- [x] **AC2 单 Server 配置错误隔离**：同时配置一个有效 Server 及缺少 command、缺少 URL、Transport 冲突或无效超时的 Server；只有无效 Server 被禁用，ImioCode 和有效 Server 正常启动。（验证：运行 `McpConfigLoaderTest` 的逐 Server 校验场景）
- [x] **AC3 配置文件安全检查**：使用符号链接配置和畸形 YAML；应用显示安全错误并继续处理其他配置，输出不包含配置中的秘密哨兵值。（验证：运行符号链接/YAML 错误测试；平台禁止符号链接时记录条件跳过）
- [x] **AC4 stdio 环境隔离**：让假 Server 回报收到的环境变量名；结果只有 PATH 和配置显式 env，不包含模型 API Key、测试秘密或其他宿主变量。（验证：运行 `StdioMcpTransportTest` 环境场景）
- [x] **AC5 缺失占位符安全失败**：env 或 Header 使用不存在的 `${MISSING_TOKEN}`；只禁用对应 Server，错误包含 `MISSING_TOKEN` 变量名但不包含任何相邻配置值。（验证：运行 `McpEnvironmentResolverTest`）
- [x] **动态秘密脱敏**：配置中的 `${MCP_TEST_TOKEN}` 成功展开后，把相同秘密放入服务端错误、工具文本和 Header 风格文本；所有本地结果均显示 `***`。（验证：运行环境解析、结果映射和 UI 脱敏测试）
- [x] **配置不存在时兼容启动**：三个 MCP 配置文件均不存在时，加载结果为空且没有错误，不创建 Transport、不显示 stdio 确认。（验证：运行空配置测试及无 MCP 启动场景）

## JSON-RPC 2.0

- [x] **AC6 四类消息往返**：请求、成功响应、错误响应、通知经编码再解码后字段一致，固定使用 JSON-RPC 2.0；数字和字符串 ID 均可处理。（验证：运行 `JsonRpcCodecTest` 往返测试）
- [x] **AC7 异步响应匹配与容错**：同时发送多个请求并乱序返回；每个 Future 得到自己的结果，重复 ID、未知 ID、未知通知和畸形消息不会使应用崩溃。（验证：运行 `DefaultMcpClientTest` 乱序和异常消息场景）
- [x] **严格字段校验**：错误协议版本、缺少 method/id、非法 ID、同时出现 result/error 等消息被安全拒绝，错误中不回显完整原始正文。（验证：运行 `JsonRpcCodecTest` 非法输入测试）
- [x] **等待项无泄漏**：成功、协议错误、Transport 断开、超时、取消和 close 后，请求等待表均为空且每个 Future 只完成一次。（验证：运行 `DefaultMcpClientTest` 竞争场景）

## stdio Transport

- [x] **AC8 stdio 完整协议路径**：真实 Java 假 Server 通过 stdin/stdout 完成 initialize、initialized、tools/list 和 tools/call。（验证：运行 `StdioMcpTransportTest` 与 `McpIntegrationTest`）
- [x] **AC9 子进程异常退出**：假 Server 在存在等待请求时退出；全部等待请求在超时前失败，内置工具仍可继续执行。（验证：运行 stdio 异常退出和集成隔离测试）
- [x] **AC10 启动拒绝不创建进程**：在 stdio 确认中选择拒绝；Transport Factory 未创建子进程，该 Server 无工具注册，其他 Server 继续连接。（验证：运行 `McpManagerTest` 拒绝场景）
- [x] **stdout/stderr 分离**：假 Server 向 stderr 写入超过保留上限的日志，协议调用仍成功；stderr 不被当成 JSON-RPC，保留量不超过限制。（验证：运行 `StdioMcpTransportTest` 大量 stderr 场景）
- [x] **消息上限**：stdio 单条消息超过 4 MiB 时连接安全失败，不继续无限读取，等待请求全部结束。（验证：运行 stdio 超限测试）
- [x] **进程关闭**：关闭 Transport 或 ImioCode 后，stdin 关闭，假 Server 在宽限期内退出；不响应时被终止，测试结束后没有遗留假 Server 进程。（验证：运行 stdio close 测试并检查 `Process.isAlive()`）

## Streamable HTTP Transport

- [x] **AC11 JSON 与有限 SSE**：本地 HTTP Server 分别返回 `application/json` 和有限 `text/event-stream`；两种响应均能完成握手和工具调用。（验证：运行 `StreamableHttpMcpTransportTest` 两种 Content-Type 场景）
- [x] **AC12 Session 传递**：初始化响应返回 `MCP-Session-Id` 后，initialized、tools/list、tools/call 和关闭 DELETE 都携带相同 Session ID。（验证：由本地 HTTP Server 捕获并断言请求头）
- [x] **AC13 HTTP 地址安全**：远程明文 HTTP、URL user-info 和跨源重定向被拒绝；localhost、127.0.0.1、`[::1]` 的 HTTP 可以通过校验。（验证：运行 HTTP URL 安全参数化测试）
- [x] **协议与内容协商头**：POST 携带 JSON Content-Type 和 JSON/SSE Accept；初始化后的请求携带 `MCP-Protocol-Version: 2025-11-25`。（验证：由本地 HTTP Server 捕获并断言请求头）
- [x] **同源重定向边界**：最多 3 次同源跳转可成功；第 4 次、缺少 Location 或跨源跳转失败，认证 Header 和 Session 不发送到其他源。（验证：运行 HTTP 重定向测试）
- [x] **HTTP 响应上限和类型错误**：超大正文、未知 Content-Type、畸形 JSON/SSE、非成功状态都在限制内失败，不把完整响应写入错误。（验证：运行 HTTP 异常响应测试）
- [x] **HTTP 取消与关闭**：取消调用会取消本地 HTTP Future；close 取消未完成请求并尽力 DELETE Session，重复 close 不报错。（验证：运行 HTTP cancel/close 测试）
- [x] **无长期 GET**：全部 Transport 测试中本地 Server 未收到长期 GET/SSE 监听请求，只出现 POST 和可选 DELETE。（验证：检查本地 Server 请求记录）

## MCP Client 生命周期

- [x] **AC14 2025-11-25 握手**：Server 收到的 initialize 包含协议版本、ImioCode 名称和版本；响应成功后收到 `notifications/initialized`，之后才发生工具发现。（验证：运行 `DefaultMcpClientTest` 和假 Server 握手记录）
- [x] **AC15 分页工具发现**：Server 返回多页工具和 `nextCursor`；Client 收集全部工具一次且不重复，重复 cursor 或超过 100 页时安全失败。（验证：运行分页、重复 cursor 和页数上限测试）
- [x] **AC16 独立超时**：初始化、发现和工具调用分别按配置超时；超时后调用失败、取消通知尽力发出、等待表为空。（验证：运行三类超时测试）
- [x] **AC17 Agent 主动取消**：Agent 取消正在执行的 MCP 工具；Server 收到包含原 request ID 的取消通知，本地返回中断结果且 tools/call 只出现一次。（验证：运行 Wrapper 与集成取消测试）
- [x] **协议版本和能力拒绝**：Server 返回不支持版本或没有 tools capability 时不进入 READY、不调用 tools/list、不注册工具。（验证：运行 Client 握手失败测试）
- [x] **不支持的 Server Request**：Server 主动发起 Sampling/Elicitation 等 Request 时收到 `-32601 Method not found`，Client 保持可用。（验证：运行 Client Server Request 测试）
- [x] **Server instructions 隔离**：初始化返回恶意 instructions 后，捕获发给 LLM 的 system/messages，内容中不存在该 instructions。（验证：运行 `McpIntegrationTest` Prompt 隔离场景）
- [x] **无自动重试**：工具调用超时、HTTP Session 失效或业务错误时，Server 记录到的 tools/call 始终只有一次。（验证：运行超时、404 和 isError 场景）

## 工具适配、权限与 Plan Mode

- [x] **AC18 稳定命名**：Server `github-main` 的远程工具 `issues/list` 生成稳定的 `mcp_github-main__issues_list`，只包含本地允许字符。（验证：运行 `McpToolNameTest`）
- [x] **AC19 名称冲突隔离**：清洗碰撞、超过 64 字符或与 Registry 已有名称冲突时不覆盖原工具；冲突工具被跳过，其他工具正常注册。（验证：运行命名与 Manager 冲突测试）
- [x] **AC20 Schema 导出**：远程 inputSchema 经 Anthropic、OpenAI Chat Completions 和 OpenAI Responses 三种编码器导出后保持结构；缺失 Schema 输出合法空对象 Schema。（验证：运行 Wrapper/Provider Schema 集成测试）
- [x] **AC21 权限与 Plan Mode**：MCP 工具在 ask/auto-edit 默认进入 HITL，显式规则可 ALLOW/DENY；Plan Mode 的工具清单和执行入口均不含 MCP 工具。（验证：运行权限、规则和 `McpIntegrationTest` Plan 场景）
- [x] **AC22 结果语义映射**：多段 text 保持顺序，structuredContent 输出合法 JSON，空结果显示“无输出”，`isError=true` 产生失败 ToolResult。（验证：运行 `McpToolResultMapperTest`）
- [x] **AC23 二进制、截断和脱敏**：image/audio/resource 只出现安全摘要，不含 data/base64；超长 UTF-8 输出按字节截断并标记，秘密显示为 `***`。（验证：运行结果映射边界测试）
- [x] **固定风险分类**：所有 MCP `ToolDefinition` 均为 HIGH，权限请求均为 COMMAND，远程 annotations/readOnlyHint 不能降低等级。（验证：运行 Wrapper 与 `PermissionRequestFactoryTest`）
- [x] **权限提示不复制参数**：在 MCP 参数中放入秘密哨兵；权限确认只展示本地工具名，不出现参数、env 或 Header 值。（验证：运行权限请求和终端捕获输出测试）
- [x] **取消透传幂等**：执行中的 Wrapper 连续调用两次 cancel，不抛异常，远程取消最多一次；无活动调用时 cancel 无副作用。（验证：运行 `McpToolWrapperTest`）

## Manager、UI 与系统集成

- [x] **AC24 Server 部分成功**：配置三个 Server，其中一个连接失败；另外两个完成工具注册，六个内置工具仍存在且可调用。（验证：运行 `McpManagerTest` 和 `McpIntegrationTest`）
- [x] **AC25 会话缓存**：同一 Server 多次调用不同工具，只发生一次 initialize 和一次完整 tools/list。（验证：检查假 Server 计数）
- [x] **AC26 完整资源释放**：退出后所有 Client 状态为 CLOSED，HTTP Future、JSON-RPC 等待项、虚拟线程任务和 stdio 子进程全部结束。（验证：运行 Manager close 与集成关闭测试）
- [x] **AC27 终端可观测且安全**：终端能区分配置、连接、协议、超时、业务错误，并显示确认、连接结果和工具数量；输出不含秘密哨兵。（验证：运行 `JLineTerminalUiTest` 与捕获输出的集成场景）
- [x] **Manager 单次启动**：每个启用 Server 只创建一个 Client；重复 start 被安全拒绝或返回已有结果，不重复注册工具。（验证：运行 `McpManagerTest` 单次启动场景）
- [x] **确定性注册**：相同配置多次测试时，Server 和工具事件顺序、最终工具清单顺序保持一致，不受并行连接完成顺序影响。（验证：重复运行 Manager 顺序测试）
- [x] **现有 UI 和权限无回归**：原有工具状态、Agent 状态、权限确认和模式提示测试全部通过。（验证：运行 terminal、permission 全包测试）
- [x] **所有公开入口有真实调用方**：生产代码完整编译，不存在仅测试可达的顶层组装接口；应用入口实际创建 Loader、Manager、Transport Factory 和 UI 适配器。（验证：`mvn -q -DskipTests compile` 并运行集成测试）

## 跨平台、构建与回归

- [x] **AC28 Windows 中文路径与参数边界**：在中文路径下启动 Java 假 Server，中文 JSON 往返正确；包含空格的 command/args 分项传入，不经过 Shell 拼接。（验证：运行 stdio 中文路径测试）
- [x] **AC29 无 MCP 配置兼容性**：不创建任何 MCP 配置时启动 ImioCode，六个内置工具、权限模式、Plan/Do 和现有对话行为与 ch6 一致，不出现 MCP 确认。（验证：运行空配置集成场景和原有测试）
- [x] **AC30 全部测试通过**：新增测试和原有至少 241 项测试均完成，Maven 汇总为 0 failures、0 errors；平台受限的符号链接测试可明确显示 skipped。（验证：运行 `mvn test` 并保存汇总）
- [x] **生产代码编译**：Java 21 编译无错误、无警告导致的失败。（验证：运行 `mvn -q -DskipTests compile`）
- [x] **可执行 JAR 打包**：构建生成 `target/imiocode-0.2.0-SNAPSHOT-all.jar`，主类仍为 `io.imiocode.ImioCodeApplication`。（验证：运行 `mvn package -DskipTests` 并检查 JAR）
- [x] **无意外依赖**：依赖树未新增 MCP SDK、Reactor、Node、Python 或网络测试依赖。（验证：运行 `mvn dependency:tree` 并对照 `pom.xml`）
- [x] **源码与文档整洁**：无冲突标记、尾随空格或未完成占位内容，README 与示例不存在真实密钥。（验证：运行 `git diff --check`，搜索常见待补标记、冲突标记和真实 Token）
- [x] **用户文件未被纳入改动**：`claude.md` 和 `hello.txt` 保持用户原状态，不被本章提交暂存。（验证：提交前运行 `git status --short` 和 `git diff --cached --name-only`）

## 端到端场景

- [ ] **AC31 真实成功场景**：在隔离工作区配置 Java 假 stdio Server，启动打包 JAR，批准启动，输入一段要求 Agent 调用远程工具的真实请求；观察到连接成功、工具发现、权限确认、远程结果、LLM 后续回复，退出后假 Server 终止。（验证：优先在 tmux 中运行并保存 pane 输出；当前 Windows 无可用 tmux 时运行同等真实 Java 子进程流程并记录限制）
- [x] **AC32 真实部分失败场景**：同时配置一个返回协议错误的假 Server 和一个正常 Server；终端显示坏 Server 失败，应用继续启动，Agent 能调用正常 Server 工具。（验证：保存实际终端输出或真实进程集成报告）
- [x] **Plan/Do 真实切换**：成功连接 MCP 后输入 `/plan`，模型工具列表中没有 MCP 工具；输入 `/do` 后 MCP 工具重新可用且仍经过权限确认。（验证：真实进程观察或捕获 LLM 请求 Schema）
- [x] **退出可靠性**：在 MCP 调用进行中退出或触发中断，ImioCode 不挂死，全部子进程在宽限期内结束。（验证：真实进程退出后检查进程状态）

## 验收报告要求

完成开发后，为每个条目记录：

- 实际运行的命令或操作。
- 实际观察到的结果。
- Maven 测试数量、failures、errors、skipped。
- 端到端成功与失败场景的输出摘要。
- tmux 不可用时的具体环境错误及替代验证方式。
- 若有未通过项，记录预期、实际、原因和修复后复测证据。
