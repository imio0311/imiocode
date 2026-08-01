# ch7 MCP 开放工具生态 Spec

## 背景

ImioCode 当前只有六个内置工具。新增工具需要修改 Java 代码、注册工具并重新打包，无法直接复用社区已有的 GitHub、数据库、Slack 等 MCP Server。

ch7 将增加 MCP Client 能力，让用户通过配置文件声明外部 Server。ImioCode 启动时加载配置、建立连接、发现远程工具，并将它们适配到现有工具注册中心和 Agent Loop。

## 目标

- 支持 MCP `2025-11-25` 协议的初始化、工具发现和工具调用。
- 支持 stdio 与 Streamable HTTP 两种 Transport。
- 用户只需修改 YAML 配置即可接入新的 MCP Server。
- 多个 Server 相互隔离，单个连接失败不影响其他 Server 和内置工具。
- MCP 工具复用现有 LLM、Agent、权限确认和终端展示链路。
- stdio 子进程不得继承 API Key 等未显式授权的环境变量。
- 项目配置不能在未经确认时自动启动本地进程。
- 所有 MCP 会话在 ImioCode 退出时可靠关闭。

## 功能需求

### F1：三层 MCP 配置

- 支持以下配置文件：
  - 用户级：`%USERPROFILE%\.imiocode\mcp.yaml`
  - 项目级：`<项目>\.imiocode\mcp.yaml`
  - 本地级：`<项目>\.imiocode\mcp.local.yaml`
- 按 Server 名称合并，优先级为用户级 > 项目级 > 本地级。
- 同名 Server 由高优先级配置整体覆盖。
- 每个 Server 可配置：
  - `transport`
  - `command`、`args`
  - `url`
  - `headers`
  - `env`
  - 初始化超时
  - 工具调用超时
  - 是否启用
- stdio 配置必须提供 `command`，HTTP 配置必须提供 `url`；冲突或缺失时只禁用对应 Server。
- 配置文件必须是普通文件，拒绝符号链接。
- 单个 Server 配置错误不阻止其他 Server 加载。

### F2：环境变量展开与隔离

- `env` 和 `headers` 支持 `${NAME}` 占位符。
- 占位符只能从 ImioCode 启动环境中读取。
- 缺少变量时禁用对应 Server，并只显示变量名，不显示任何变量值。
- stdio 子进程只获得：
  - `PATH`
  - 当前 Server 显式声明的 `env`
- 不继承模型 API Key、代理凭据、云平台令牌等其他环境变量。
- 终端、日志和异常中不得输出展开后的敏感值。

### F3：JSON-RPC 2.0 消息

- 支持请求、成功响应、错误响应和通知的编码与解码。
- 每个请求具有唯一 ID。
- 支持多个请求同时等待，通过 ID 将响应交还给正确调用方。
- 错误响应保留标准错误码和安全错误信息。
- 未知响应 ID、重复响应、畸形消息和未知通知不能导致 ImioCode 崩溃。
- 收到 Server 通知时能够完成解码；本章不根据通知动态重建工具列表。

### F4：stdio Transport

- 用户确认后启动配置声明的子进程。
- 通过子进程标准输入和标准输出传输逐行 JSON-RPC 消息。
- 标准错误不能混入协议流，并需要持续消费以避免子进程阻塞。
- 子进程异常退出、输出畸形消息或管道关闭时，使所有等待请求安全失败。
- ImioCode 退出时关闭管道并终止仍在运行的子进程。
- 每次 ImioCode 启动时，每个 stdio Server 都需要单独确认。
- 确认界面只展示 Server 名、command 和 args，不展示环境变量值。
- 拒绝启动只禁用对应 Server。

### F5：Streamable HTTP Transport

- 远程地址必须使用 HTTPS。
- 仅 `localhost`、`127.0.0.1` 和 `[::1]` 可以使用 HTTP。
- 禁止 URL 内嵌用户名或密码。
- 禁止自动跟随跨源重定向。
- 通过 HTTP POST 发送 JSON-RPC 请求。
- 支持：
  - `application/json` 响应
  - 单次 POST 返回的有限 `text/event-stream` 响应
- 初始化响应包含 Session ID 时，后续请求必须携带该 Session ID。
- 支持协议版本和内容协商请求头。
- 不建立长期 GET 连接，不实现旧版 SSE Transport。
- HTTP 请求取消时同步取消本地等待任务。

### F6：MCP 会话

- 按 MCP `2025-11-25` 执行初始化握手。
- 初始化内容包含 ImioCode 客户端名称、版本和客户端能力。
- 初始化成功后发送完成通知。
- 支持分页发现全部远程工具。
- 支持调用远程工具并传递 JSON 参数。
- 初始化和工具调用使用独立超时：
  - 默认初始化/发现 10 秒
  - 默认工具调用 120 秒
  - 可由 Server 配置覆盖
- 超时或断线时本次调用失败，不自动重试。
- Agent 取消任务时发送 MCP 取消通知，并取消本地等待。
- Server 返回的 instructions 不注入模型上下文。

### F7：远程工具适配

- 每个远程工具包装为 ImioCode 的统一工具接口。
- 本地工具名固定为：

```text
mcp_<serverName>__<toolName>
```

- 名称统一清洗为安全字符，并限制在 64 字符以内。
- 清洗后发生冲突时拒绝冲突工具，不覆盖已有工具。
- 远程输入 Schema 原样传给模型；缺失时使用空对象 Schema。
- 所有 MCP 工具统一标记为高风险命令操作：
  - `ask` 和 `auto-edit` 默认进入 HITL
  - 显式权限规则可以允许或拒绝
  - Plan Mode 不暴露 MCP 工具
- 工具取消必须传递到 MCP Client。

### F8：工具结果映射

- 多个文本内容按原始顺序拼接。
- `structuredContent` 序列化为 JSON 文本。
- image、audio、resource 等内容只保留类型、MIME、URI 和大小等安全摘要。
- 不把二进制或超大 Base64 内容加入模型上下文。
- `isError: true` 映射为本地失败结果，并保留安全的服务端错误文本。
- 空结果统一显示为“无输出”。
- 结果继续经过现有敏感信息脱敏和输出长度限制。

### F9：MCP Manager

- 启动时加载、合并并验证全部 Server 配置。
- 每个 Server 只建立一个缓存会话。
- 独立连接所有已启用 Server。
- 单个 Server 失败时：
  - 显示安全错误
  - 不注册该 Server 的工具
  - 继续连接其他 Server
  - 不阻止 ImioCode 启动
- 将成功发现的工具一次性注册到现有工具中心。
- 配置修改后需要重启 ImioCode，不做运行时热加载。
- ImioCode 退出时关闭全部 Client、Transport 和子进程。

### F10：终端可观测性

- 启动时显示 MCP Server 的连接结果和已注册工具数量。
- 显示 stdio 启动确认对话框。
- MCP 工具调用继续使用现有工具状态展示和权限确认界面。
- 用户可区分配置错误、连接错误、协议错误、超时和工具业务错误。
- 所有错误信息必须经过安全处理，不泄露 Header、环境变量或工具参数中的秘密。

## 非功能需求

### N1：安全优先

- 所有配置、协议和远程响应均视为不可信输入。
- 解析失败、超时、进程退出和网络异常必须安全失败，不能绕过权限检查。
- stdio 子进程启动必须经过用户确认。
- MCP 工具必须进入现有权限链，危险行为仍受 ch6 防线约束。
- API Key、Header、环境变量值和工具参数秘密不得出现在终端、异常或测试快照中。
- HTTP 不允许明文远程连接、URL 内嵌凭据或跨源重定向。
- Server instructions 不得影响 System Prompt。

### N2：协议兼容性

- 对齐 MCP `2025-11-25`。
- JSON-RPC 必填字段严格验证，未知字段向前兼容地忽略。
- 支持字符串或数字响应 ID，但本地生成的请求 ID 使用单调唯一值。
- 支持工具列表分页。
- Streamable HTTP 同时兼容 JSON 和有限 SSE 响应。
- 不依赖特定语言编写的 MCP Server。

### N3：并发与线程安全

- 请求 ID 分配、等待请求表、连接状态和关闭流程必须线程安全。
- 多个 Server 可以独立连接，单个慢 Server 不应阻塞其他 Server。
- 同一个 Client 可以正确匹配多个同时进行的请求。
- 关闭、取消、超时和响应到达之间发生竞争时，每个请求只能完成一次。
- 读取循环不能因终端输出、工具执行或其他 Server 而永久阻塞。

### N4：资源边界

- JSON-RPC 单条消息必须有大小上限，防止恶意 Server 消耗无限内存。
- stdio stderr 使用有界保留策略，不能无限积累。
- 工具输出继续受现有输出长度限制。
- HTTP 响应和有限 SSE 响应必须设置最大读取量。
- 所有线程、虚拟线程、HTTP 请求和子进程在关闭后能够释放。
- 不允许遗留僵尸进程或永远未完成的请求 Future。

### N5：性能

- Server 连接相互独立，可在用户完成 stdio 确认后并行初始化。
- 查找缓存会话和匹配响应 ID 应保持常数级开销。
- 不为每次工具调用重新发现工具或重新创建连接。
- 本章功能不能明显增加未配置 MCP 时的启动时间和内存占用。

### N6：兼容现有系统

- 未配置 MCP 时，ImioCode 行为与 ch6 保持一致。
- 六个内置工具的名称、Schema、权限和执行行为不变。
- 现有 LLM Provider、Agent Loop、Plan Mode 和 HITL 行为保持兼容。
- MCP Server 失败不能影响内置工具。
- 现有配置文件无需修改即可继续启动。

### N7：跨平台

- stdio Transport 同时支持 Windows 与常见 Unix 环境。
- command 和 args 作为独立参数传递，不通过 Shell 字符串拼接。
- Windows 路径、中文工作区和 UTF-8 JSON 内容能够正常处理。
- 不依赖 Docker 才能运行基础测试。

### N8：可测试性

- JSON-RPC 编解码、异步匹配、名称清洗和配置合并均有独立单元测试。
- 使用本地假 stdio Server 验证握手、发现、调用、取消和异常退出。
- 使用本地 HTTP Server 验证 JSON、有限 SSE、Session ID、Header 和安全策略。
- 集成测试验证 MCP 工具进入注册中心、Agent Loop 和权限确认。
- 至少有一个真实 ImioCode 进程端到端场景。
- 原有 241 项测试必须继续保持 0 failures、0 errors。

## 不做的事

- 不支持 MCP `2026-07-28` 的新会话与发现模型。
- 不实现旧版 SSE Transport。
- 不建立 Streamable HTTP 长期 GET/SSE 推送连接。
- 不消费 Server 主动推送的工具列表变更；配置或工具变化后需要重启。
- 不消费 MCP Resources。
- 不消费 MCP Prompts。
- 不实现 Sampling。
- 不实现 Elicitation。
- 不实现 Roots、Logging 等其他高级 Client 能力。
- 不处理 Server 主动发起的 Client 请求；返回标准“不支持”错误。
- 不实现 OAuth 登录、令牌刷新、浏览器授权或凭据存储；认证仅使用显式 Header。
- 不自动安装 `npx`、Node、Python 或任何 MCP Server 依赖。
- 不提供 MCP Server 市场、搜索或安装界面。
- 不让 Server instructions 进入 System Prompt 或 system-reminder。
- 不根据远程 `readOnlyHint` 自动降低工具风险。
- 不让 MCP 工具进入 Plan Mode。
- 不完整传递 image/audio 等二进制内容。
- 不自动重试可能产生副作用的工具调用。
- 不运行时热加载配置、动态注册或卸载工具。
- 不实现 ImioCode 自身作为 MCP Server。
- 不新增网络域名白名单、资源配额或审计日志。

## 验收标准

### 配置与隔离

- **AC1（F1）**：三层配置包含不同 Server 时，启动后全部出现；存在同名 Server 时只使用最高优先级的完整配置。
- **AC2（F1）**：某个 Server 缺少 command、URL 或 Transport 配置错误时，只禁用该 Server，其他 Server 和 ImioCode 正常启动。
- **AC3（F1）**：配置文件是符号链接或 YAML 格式错误时，终端显示安全错误且不泄露原始敏感值。
- **AC4（F2）**：stdio 假 Server 观察到的环境仅包含 `PATH` 和配置显式声明的变量，不包含模型 API Key。
- **AC5（F2）**：Header 或 env 引用了不存在的 `${NAME}` 时，对应 Server 连接失败，错误只包含变量名。

### JSON-RPC 与 Transport

- **AC6（F3）**：请求、成功响应、错误响应和通知可以完成编码、解码与往返一致性测试。
- **AC7（F3）**：多个乱序返回的响应仍能按 ID 交给正确请求；重复、未知或畸形响应不会导致应用崩溃。
- **AC8（F4）**：stdio 假 Server 能完成初始化、工具发现和工具调用。
- **AC9（F4）**：stdio Server 异常退出后，全部等待请求在时限内失败，ImioCode 可以继续使用内置工具。
- **AC10（F4）**：用户拒绝 stdio 启动确认后，子进程没有启动，该 Server 的工具没有注册。
- **AC11（F5）**：本地 HTTP Server 分别返回 JSON 和有限 SSE 时，两种情况均能完成初始化和工具调用。
- **AC12（F5）**：Server 返回 Session ID 后，后续发现和调用请求均携带相同 Session ID。
- **AC13（F5）**：远程 HTTP、URL 内嵌凭据和跨源重定向被拒绝；localhost HTTP 可以连接。

### MCP Client

- **AC14（F6）**：Server 观察到初始化协议版本为 `2025-11-25`，并在初始化响应后收到完成通知。
- **AC15（F6）**：工具发现存在多页结果时，Client 能收集全部页面且不会重复工具。
- **AC16（F6）**：初始化、发现和工具调用分别受到配置超时约束，超时后请求表中不残留等待项。
- **AC17（F6）**：Agent 取消工具时，Server 收到取消通知，本地调用返回中断结果且不会自动重试。

### 工具适配与权限

- **AC18（F7）**：远程工具 `issues/list` 来自 Server `github-main` 时，本地生成符合命名约束的稳定前缀名称。
- **AC19（F7）**：工具名清洗后冲突、超过本地名称限制或与已有工具冲突时，不覆盖原工具，并显示安全错误。
- **AC20（F7）**：远程 Schema 能被三家 LLM Provider 正确导出；缺失 Schema 时输出合法空对象 Schema。
- **AC21（F7）**：MCP 工具在 `ask/auto-edit` 模式下进入 HITL，显式规则可以允许或拒绝，Plan Mode 中不可见。
- **AC22（F8）**：文本、结构化 JSON、空结果和 `isError` 均能正确转换成本地工具结果。
- **AC23（F8）**：image/audio/resource 不传递二进制正文，超长结果被截断，敏感信息被脱敏。

### Manager 与生命周期

- **AC24（F9）**：三个 Server 中一个连接失败时，另外两个仍完成注册，内置六工具仍可调用。
- **AC25（F9）**：同一个 Server 的多个工具调用复用同一会话，不重复初始化或发现。
- **AC26（F9）**：退出 ImioCode 后，全部 HTTP 请求、等待任务和 stdio 子进程均已关闭。
- **AC27（F10）**：终端能观察到 stdio 确认、连接成功/失败、注册数量、权限确认和工具执行状态，但看不到环境变量值或 Header 内容。

### 兼容与端到端

- **AC28（N2/N7）**：Windows 中文路径下 stdio 传输 UTF-8 消息正常；command 与 args 不经过 Shell 拼接。
- **AC29（N6）**：没有任何 MCP 配置时，ImioCode 启动行为和 ch6 一致，不出现多余确认。
- **AC30（N8）**：全部新增测试和原有测试通过，结果为 0 failures、0 errors。
- **AC31（端到端）**：真实 ImioCode 进程读取 MCP 配置，用户批准启动本地假 Server，Agent 发现并调用其工具，终端展示权限确认和成功结果，退出后 Server 子进程终止。
- **AC32（端到端失败场景）**：一个假 Server 返回协议错误时，终端显示该 Server 失败，另一个正常 Server 的工具仍能被 Agent 调用。
