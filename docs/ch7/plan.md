# ch7 MCP 开放工具生态 Plan

## 架构概览

ch7 采用“协议层与业务层分离”的结构，共分为六层：

1. **配置层**
   - 加载用户级、项目级、本地级三份 MCP 配置。
   - 按“用户级 > 项目级 > 本地级”合并同名 Server。
   - 展开环境变量，并将错误隔离到单个 Server。
2. **JSON-RPC 协议层**
   - 负责请求、响应、通知的编解码。
   - 生成请求 ID，并用线程安全的等待表匹配异步响应。
   - 统一处理协议错误、超时、取消和未知服务端请求。
3. **Transport 传输层**
   - stdio：启动子进程，通过换行分隔的 stdin/stdout 交换 JSON-RPC；stderr 单独限量采集。
   - Streamable HTTP：每条消息使用 POST，支持普通 JSON 响应及本次请求内结束的 SSE 响应。
   - 不实现长期 GET、断线续传和旧版 HTTP+SSE。
   - HTTP Transport 负责保存会话 ID、协议版本请求头并限制重定向。
4. **MCP Client 会话层**
   - 执行 `initialize → notifications/initialized → tools/list`。
   - 支持分页发现工具及 `tools/call`。
   - 初始化、发现和调用分别受超时控制。
   - Agent 取消工具时发送 `notifications/cancelled`。
   -服务端返回的 `instructions` 只记录元数据，不进入 Prompt。
5. **工具适配层**
   - 将远程工具包装成现有 `Tool` 接口。
   - 工具名统一为 `mcp_<server>__<tool>`。
   - 输入 Schema 原样适配；结果转换为文本或结构化 JSON。
   - 所有 MCP 工具标记为 `COMMAND + HIGH`，进入现有权限链；Plan Mode 不加载。
   - 最终结果经过脱敏和长度限制。
6. **Manager 与应用集成层**
   - 启动时加载配置，逐个确认 stdio Server，然后并行连接获批的 Server。
   - 缓存连接和工具清单，将成功发现的工具注册进现有 `ToolRegistry`。
   - 单个 Server 失败不会阻止程序和其他 Server 启动。
   - 终端显示连接、发现、调用、失败和关闭状态。
   - 程序退出时统一关闭 HTTP 会话和 stdio 子进程。

核心依赖链：

```text
配置文件
   ↓
McpConfigLoader
   ↓
McpManager
   ├─ StdioMcpTransport
   └─ StreamableHttpMcpTransport
          ↓
       McpClient
          ↓
     McpToolWrapper
          ↓
现有 ToolRegistry → PermissionChecker → Agent Loop
```

## 核心数据结构

### MCP 配置模型

```java
enum McpTransportType {
    STDIO,
    STREAMABLE_HTTP
}

record McpServerConfig(
        String name,
        boolean enabled,
        McpTransportType transport,
        String command,
        List<String> args,
        URI url,
        Map<String, String> env,
        Map<String, String> headers,
        Duration initializationTimeout,
        Duration callTimeout
) {}
```

约束：

- `STDIO` 必须提供 `command`，不得配置 `url`。
- `STREAMABLE_HTTP` 必须提供 `url`，不得配置 `command`。
- 默认初始化及工具发现超时 10 秒，调用超时 120 秒。
- 配置集合全部防御性复制。
- 环境变量展开后使用独立的 `ResolvedMcpServerConfig`，禁止默认 `toString()` 输出密钥。

```java
record McpConfigError(
        Path source,
        String serverName,
        String code,
        String safeMessage
) {}

record McpConfigLoadResult(
        Map<String, McpServerConfig> servers,
        List<McpConfigError> errors
) {}
```

### JSON-RPC 2.0 消息模型

```java
sealed interface JsonRpcMessage
        permits JsonRpcRequest, JsonRpcResponse, JsonRpcNotification {}

record JsonRpcId(JsonNode value) {}

record JsonRpcRequest(
        JsonRpcId id,
        String method,
        JsonNode params
) implements JsonRpcMessage {}

record JsonRpcResponse(
        JsonRpcId id,
        JsonNode result,
        JsonRpcError error
) implements JsonRpcMessage {}

record JsonRpcNotification(
        String method,
        JsonNode params
) implements JsonRpcMessage {}

record JsonRpcError(
        int code,
        String message,
        JsonNode data
) {}
```

```java
interface JsonRpcCodec {
    String encode(JsonRpcMessage message);

    JsonRpcMessage decode(String json);
}
```

约束：

- `JsonRpcId` 只接受整数或字符串。
- Response 必须在 `result` 和 `error` 中二选一。
- 所有消息固定写出 `"jsonrpc": "2.0"`。
- 非法消息返回协议错误，不允许让读线程直接崩溃。

### Transport 接口

```java
interface McpTransport extends AutoCloseable {
    CompletableFuture<Void> start(
            Consumer<JsonRpcMessage> inboundHandler,
            Consumer<Throwable> failureHandler);

    CompletableFuture<Void> send(JsonRpcMessage message);

    boolean isOpen();

    @Override
    void close();
}
```

两个实现：

```java
final class StdioMcpTransport implements McpTransport {}

final class StreamableHttpMcpTransport implements McpTransport {}
```

Transport 只负责连接和收发消息，不负责 MCP 握手、工具语义或权限判断。

### MCP Client 接口

```java
interface McpClient extends AutoCloseable {
    CompletableFuture<McpInitializeResult> connect();

    CompletableFuture<List<McpRemoteTool>> listTools();

    McpCallHandle callTool(
            String toolName,
            ObjectNode arguments,
            Duration timeout);

    CompletableFuture<Void> cancelRequest(
            JsonRpcId requestId,
            String reason);

    McpClientState state();

    @Override
    void close();
}
```

```java
record McpCallHandle(
        JsonRpcId requestId,
        CompletableFuture<McpCallResult> result
) {}

record McpInitializeResult(
        String protocolVersion,
        McpServerInfo serverInfo,
        JsonNode capabilities,
        String instructions
) {}

record McpRemoteTool(
        String name,
        String description,
        ObjectNode inputSchema
) {}

record McpCallResult(
        List<JsonNode> content,
        JsonNode structuredContent,
        boolean error
) {}
```

`McpClient` 内部维护：

```java
AtomicLong nextRequestId;
ConcurrentMap<JsonRpcId, CompletableFuture<JsonNode>> pendingRequests;
```

响应到达后按 ID 完成对应 Future；连接关闭或传输失败时，所有未完成请求统一失败。

### 本地工具适配器

```java
final class McpToolWrapper implements Tool {
    @Override
    ToolDefinition definition();

    @Override
    ToolResult execute(ObjectNode arguments);

    @Override
    void cancel();
}
```

配套组件：

```java
final class McpToolName {
    static String create(String serverName, String remoteToolName);
}

final class McpToolResultMapper {
    ToolResult map(McpCallResult result, Duration duration);
}
```

规则：

- 名称构造为 `mcp_<server>__<tool>`。
- 非法字符替换为 `_`。
- 空名称、超过 64 字符或注册后碰撞时，拒绝该工具并记录安全错误，不静默截断。
- Wrapper 保存当前调用的 `McpCallHandle`，Agent 取消时发送取消通知。
- `ToolDefinition.risk` 固定为 `HIGH`。
- 权限模块把 `mcp_` 工具统一归类为 `COMMAND`。

### Manager 与交互接口

```java
interface McpLaunchApprover {
    boolean approve(McpLaunchRequest request);
}

record McpLaunchRequest(
        String serverName,
        String command,
        List<String> args
) {}
```

确认请求不包含环境变量和请求头。

```java
final class McpManager implements AutoCloseable {
    McpStartupResult start(
            Collection<McpServerConfig> configs,
            ToolRegistry registry);

    Map<String, McpClient> activeClients();

    @Override
    void close();
}

record McpStartupResult(
        int connectedServers,
        int registeredTools,
        List<McpConfigError> errors
) {}
```

Manager 按 Server 名缓存 Client；同一启动周期不重复连接，也不自动重试失败连接。

### 可观测事件

```java
record McpEvent(
        McpEventType type,
        String serverName,
        String toolName,
        String safeMessage
) {}

enum McpEventType {
    WAITING_FOR_APPROVAL,
    APPROVED,
    DENIED,
    CONNECTING,
    CONNECTED,
    TOOL_DISCOVERED,
    TOOL_CALL_STARTED,
    TOOL_CALL_SUCCEEDED,
    TOOL_CALL_FAILED,
    SERVER_FAILED,
    CLOSED
}

interface McpEventListener {
    void onMcpEvent(McpEvent event);
}
```

所有事件仅携带经过脱敏的安全文本，终端 UI 订阅事件，不直接依赖 Transport 或 Client。

## 模块设计

### 配置模块 `mcp.config`

**职责：**

- 定位并加载三层 `mcp.yaml`。
- 拒绝符号链接配置文件。
- 按“本地级 → 项目级 → 用户级”覆盖合并。
- 合并完成后展开 `${NAME}`。
- 校验 Transport 字段组合、URL、超时和 Server 名称。
- 返回有效配置及隔离后的安全错误。

**主要组件：**

- `McpConfigLoader`
- `McpConfigDocument`
- `McpEnvironmentResolver`
- `McpServerConfig`

**依赖：**

- Jackson YAML
- `SecretRedactor`
- Java NIO

敏感值展开后立即注册到脱敏器；错误信息只包含变量名，不能包含变量值。

### JSON-RPC 模块 `mcp.jsonrpc`

**职责：**

- JSON-RPC 2.0 消息编解码。
- 严格区分 Request、Response、Notification。
- 校验版本、ID、method、result/error。
- 为 Client 和两个 Transport 提供统一消息模型。

**主要组件：**

- `JsonRpcCodec`
- `JsonRpcMessage`
- `JsonRpcRequest`
- `JsonRpcResponse`
- `JsonRpcNotification`
- `JsonRpcError`

该模块不认识 MCP 的 `initialize`、`tools/list` 等业务方法。

### stdio Transport 模块 `mcp.transport`

**职责：**

- 使用 `ProcessBuilder` 直接启动命令，不经过 Shell。
- 清空继承环境，只保留父进程 `PATH` 和配置中显式声明的变量。
- stdin/stdout 使用 UTF-8 单行 JSON-RPC。
- 独立消费 stderr，防止缓冲区堵塞。
- 管理子进程关闭和强制终止。

**资源限制：**

- 单条协议消息最大 4 MiB。
- stderr 仅保留最近 64 KiB。
- stdout 非法行触发该 Server 连接失败。
- 关闭顺序为：关闭 stdin → 等待宽限期 → 普通终止 → 强制终止。

**依赖：**

- JSON-RPC Codec
- `ResolvedMcpServerConfig`
- Java Process API

stdio 启动审批不属于 Transport，由 Manager 在创建子进程前完成。

### Streamable HTTP Transport 模块 `mcp.transport`

**职责：**

- 使用 Java 21 `HttpClient` 发送 POST。
- 设置 `Accept: application/json, text/event-stream`。
- 解析普通 JSON 或本次 POST 内结束的 SSE 响应。
- 保存并回传 `MCP-Session-Id`。
- 初始化完成后添加 `MCP-Protocol-Version: 2025-11-25`。
- 关闭时对有 Session ID 的连接发送 DELETE。

**安全边界：**

- 远程地址必须使用 HTTPS。
- 仅环回地址允许 HTTP。
- URL 禁止包含用户名或密码。
- 禁止自动跨域重定向。
- 最多手动跟随 3 次同源重定向。
- 响应正文最大 4 MiB。
- 不发起长期 GET，不实现断线续传或旧版 SSE endpoint。

有限 SSE 解析器只读取 `data:` 字段；允许同一 POST 返回多条消息，收到对应 Response 后结束本次请求。

### MCP Client 模块 `mcp.client`

**职责：**

- 管理 MCP 生命周期及状态机。
- 生成请求 ID 并异步匹配响应。
- 完成握手、工具分页发现、工具调用、超时和取消。

状态机：

```text
NEW → CONNECTING → READY → CLOSED
          │          │
          └──────────┴→ FAILED
```

握手过程：

```text
initialize
    ↓
校验 protocolVersion 与 tools capability
    ↓
notifications/initialized
    ↓
tools/list（跟随 nextCursor）
    ↓
READY
```

防御行为：

- Client capabilities 只声明本章实际支持的能力。
- 服务端返回其他协议版本时拒绝连接。
- 未声明 `tools` 能力时不调用工具发现。
- 分页设置最大页数，并检测重复 cursor。
- 收到不支持的服务端 Request 时返回 `-32601 Method not found`。
- 无关 Notification 安全忽略。
- 请求超时后发送 `notifications/cancelled`，并移除等待项。
- Transport 失败时完成所有异常 Future，避免线程永久等待。

### 工具适配模块 `mcp.tool`

**职责：**

- 把 `McpRemoteTool` 转换为本地 `ToolDefinition`。
- 调用远程工具并转换结果。
- 接入现有取消、权限、脱敏和输出限制机制。

结果映射：

- 所有 `text` 内容按顺序拼接。
- 有 `structuredContent` 时序列化为格式化 JSON。
- image、audio、resource 只输出类型、MIME 和安全摘要。
- `isError=true` 转为失败的 `ToolResult`。
- 输出经过 `SecretRedactor`。
- 使用现有 `ToolLimits.maxResultBytes` 截断。
- 不保存或输出二进制/base64 正文。

工具命名器负责字符替换、长度校验及碰撞报告。

### MCP Manager 模块 `mcp.manager`

**职责：**

- 编排配置、用户审批、Transport、Client 和工具注册。
- 缓存活动连接。
- 保证单 Server 失败隔离。
- 统一关闭所有资源。

启动过程：

1. 接收已校验配置。
2. 对每个 stdio Server 串行请求一次启动确认。
3. 跳过被拒绝或配置无效的 Server。
4. 使用虚拟线程并行连接剩余 Server。
5. 收集工具清单。
6. 按确定性顺序注册工具。
7. 对命名碰撞单独报错，保留其他工具和 Server。
8. 汇总启动结果并发送 UI 事件。

运行期不自动重连、不热加载配置，也不响应 `tools/list_changed`。

### 现有系统集成

**`ImioCodeApplication`：**

- 在构造 LLM Client 和 Agent 前初始化 MCP Manager。
- 将远程工具注册到现有 `ToolRegistry`。
- 程序退出时关闭 Agent/LLM 后再关闭 MCP Manager。

**`TerminalUi`：**

- 实现 `McpLaunchApprover`。
- stdio 确认框只展示 Server 名、命令及参数。
- 实现 `McpEventListener`，展示连接和调用状态。
- 不显示 env、headers、完整响应正文。

**权限系统：**

- `PermissionRequestFactory` 识别 `mcp_` 前缀。
- 分类固定为 `COMMAND`，风险固定为 `HIGH`。
- 权限规则可用工具名匹配，例如 `mcp_github__*`。
- 权限目标只使用本地工具名，不把远程参数复制进权限日志或提示框。

**Plan Mode：**

- 保持现有静态只读工具白名单。
- MCP 工具不会进入 Plan Mode Schema，也无法在 Plan Mode 执行。

## 模块交互

### 启动与工具注册

```text
ImioCodeApplication
    │
    ├─ 创建 SecretRedactor、ToolRegistry、TerminalUi
    │
    ├─ McpConfigLoader.load()
    │      ├─ 读取 local 配置
    │      ├─ 读取 project 配置
    │      ├─ 读取 user 配置
    │      └─ 合并、展开变量、校验
    │
    └─ McpManager.start()
           ├─ stdio Server → TerminalUi 请求启动确认
           ├─ 创建对应 Transport
           ├─ McpClient.connect()
           │      ├─ initialize
           │      ├─ notifications/initialized
           │      └─ tools/list 分页
           ├─ 为远程工具创建 McpToolWrapper
           └─ 注册到 ToolRegistry
                  ↓
           创建 LLM Client 与 Agent
```

配置或连接错误以 `McpConfigError` 或 `McpEvent` 返回，不通过异常终止整个应用。

### 普通 MCP 工具调用

```text
LLM 产生 tool_use
    ↓
Agent Loop
    ↓
ToolRegistry 找到 McpToolWrapper
    ↓
PermissionChecker
    ├─ DENY → 返回拒绝结果
    ├─ ASK  → TerminalUi HITL 确认
    └─ ALLOW
          ↓
McpToolWrapper.execute(arguments)
    ↓
McpClient.callTool()
    ├─ 分配 requestId
    ├─ 放入 pendingRequests
    └─ Transport.send(tools/call)
          ↓
       MCP Server
          ↓
Transport 收到 Response
    ↓
McpClient 按 ID 完成 Future
    ↓
McpToolResultMapper
    ├─ 内容转换
    ├─ 密钥脱敏
    └─ 长度限制
          ↓
ToolResult → AgentEvent → TerminalUi
          ↓
tool_result 回传 LLM，继续 Agent Loop
```

MCP 工具继续复用 ch4 的 Agent Loop，因此可以参与多轮自主执行。

### JSON-RPC 请求匹配

```text
发送请求
  requestId=17
      │
      ├─ pendingRequests.put(17, future)
      └─ Transport.send(...)
                         │
响应可能乱序到达          │
      ↓                  │
JsonRpcResponse(id=17) ←─┘
      ↓
pendingRequests.remove(17)
      ↓
future.complete(result/error)
```

规则：

- 未知 ID 的 Response 记录安全警告后丢弃。
- 重复 Response 不会第二次完成 Future。
- Transport 断开时，等待表中的所有 Future 都异常完成。
- 完成、失败、超时和取消都会移除等待项。

### 超时与主动取消

```text
Agent / 请求计时器
        ↓
McpToolWrapper.cancel()
        ↓
McpClient.cancelRequest(requestId)
        ├─ 移除并取消本地 Future
        └─ Transport.send(
             notifications/cancelled
           )
```

- 超时先停止本地等待，再尽力发送取消通知。
- 发送取消通知失败不会覆盖原始的“超时/用户取消”结果。
- `cancel()` 必须幂等；没有活动请求时直接返回。
- 本章不等待服务端确认取消。

### Streamable HTTP 单次请求

```text
JsonRpcMessage
    ↓
HTTP POST /mcp
    ├─ application/json
    │      └─ 解析一条 JSON-RPC 消息
    │
    ├─ text/event-stream
    │      └─ 逐事件解析 data 字段
    │             ├─ Notification
    │             ├─ Request → 回应 -32601
    │             └─ 目标 Response
    │
    └─ 202 + 空正文
           └─ 仅用于 Notification/Response
```

初始化响应若包含 `MCP-Session-Id`，Transport 保存它，并在之后的 POST/DELETE 中携带。初始化后的请求同时携带协商后的协议版本。

### stdio 子进程交互

```text
McpClient
    ↓ JSON-RPC 单行
stdin writer（串行写锁）
    ↓
MCP 子进程
    ├─ stdout → 专用读取线程 → JsonRpcCodec → inboundHandler
    └─ stderr → 专用排空线程 → 限量诊断缓冲
```

写入使用锁保证多条消息不会交叉；stdout 和 stderr 分别消费，避免任一管道堵塞。

### 部分失败与关闭

单 Server 失败：

```text
Server A 连接成功 → 注册工具
Server B 握手失败 → 发送 SERVER_FAILED
Server C 被拒绝   → 禁用本次启动
                     ↓
             ImioCode 正常运行
```

关闭顺序：

```text
停止接受新工具调用
    ↓
取消未完成请求
    ↓
关闭 MCP Client
    ├─ HTTP：尝试 DELETE Session
    └─ stdio：关闭 stdin 并终止子进程
    ↓
关闭 Manager 执行器
    ↓
关闭 TerminalUi
```

关闭操作全部幂等；单个连接关闭失败不妨碍其他连接释放。

## 文件组织

```text
src/main/java/io/imiocode/
├── mcp/
│   ├── config/
│   │   ├── McpTransportType.java
│   │   ├── McpServerConfig.java
│   │   ├── ResolvedMcpServerConfig.java
│   │   ├── McpConfigDocument.java
│   │   ├── McpConfigError.java
│   │   ├── McpConfigLoadResult.java
│   │   ├── McpConfigLoader.java
│   │   └── McpEnvironmentResolver.java
│   ├── jsonrpc/
│   │   ├── JsonRpcMessage.java
│   │   ├── JsonRpcId.java
│   │   ├── JsonRpcRequest.java
│   │   ├── JsonRpcResponse.java
│   │   ├── JsonRpcNotification.java
│   │   ├── JsonRpcError.java
│   │   └── JsonRpcCodec.java
│   ├── transport/
│   │   ├── McpTransport.java
│   │   ├── McpTransportFactory.java
│   │   ├── McpTransportException.java
│   │   ├── StdioMcpTransport.java
│   │   ├── StreamableHttpMcpTransport.java
│   │   └── SseMessageDecoder.java
│   ├── client/
│   │   ├── McpClient.java
│   │   ├── DefaultMcpClient.java
│   │   ├── McpClientState.java
│   │   ├── McpInitializeResult.java
│   │   ├── McpServerInfo.java
│   │   ├── McpRemoteTool.java
│   │   ├── McpCallHandle.java
│   │   └── McpCallResult.java
│   ├── tool/
│   │   ├── McpToolName.java
│   │   ├── McpToolResultMapper.java
│   │   └── McpToolWrapper.java
│   └── manager/
│       ├── McpManager.java
│       ├── McpStartupResult.java
│       ├── McpLaunchApprover.java
│       ├── McpLaunchRequest.java
│       ├── McpEvent.java
│       ├── McpEventType.java
│       └── McpEventListener.java
├── ImioCodeApplication.java
├── permission/
│   └── PermissionRequestFactory.java
├── terminal/
│   ├── TerminalUi.java
│   └── JLineTerminalUi.java
└── tool/
    └── SecretRedactor.java
```

测试文件：

```text
src/test/java/io/imiocode/
├── mcp/
│   ├── config/
│   │   ├── McpConfigLoaderTest.java
│   │   └── McpEnvironmentResolverTest.java
│   ├── jsonrpc/
│   │   └── JsonRpcCodecTest.java
│   ├── transport/
│   │   ├── StdioMcpTransportTest.java
│   │   ├── StreamableHttpMcpTransportTest.java
│   │   └── SseMessageDecoderTest.java
│   ├── client/
│   │   └── DefaultMcpClientTest.java
│   ├── tool/
│   │   ├── McpToolNameTest.java
│   │   ├── McpToolResultMapperTest.java
│   │   └── McpToolWrapperTest.java
│   ├── manager/
│   │   └── McpManagerTest.java
│   ├── fixture/
│   │   └── FakeStdioMcpServer.java
│   └── McpIntegrationTest.java
├── permission/
│   └── PermissionRequestFactoryTest.java
└── terminal/
    └── JLineTerminalUiTest.java
```

文档文件：

```text
docs/ch7/
├── spec.md
├── plan.md
├── task.md
├── checklist.md
└── mcp.example.yaml
```

另修改根目录 `README.md`，补充：

- 三层 MCP 配置文件位置。
- stdio 与 Streamable HTTP 示例。
- 环境变量占位符写法。
- stdio Server 启动确认说明。
- 权限规则与 MCP 工具命名示例。
- 配置修改后需要重启。

文件组织约束：

- 不向 `pom.xml` 增加 MCP SDK 或响应式框架依赖。
- HTTP 测试使用 JDK 自带本地 HTTP Server。
- stdio 测试启动测试源码中的 Java 假 Server，不要求安装 Node/npm。
- 测试不得连接真实公网 Server。
- 示例配置只放在文档目录，不创建会自动生效的项目 MCP 配置。

## 技术决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 协议版本 | 严格使用 MCP `2025-11-25` | 与已批准的 spec 一致，避免同时兼容多个版本扩大范围 |
| 实现方式 | Java 21 + Jackson + JDK HttpClient 自研轻量客户端 | 当前项目没有响应式技术栈，本章只消费 Tools，不需要引入完整 SDK |
| JSON-RPC 调度 | `CompletableFuture` + 请求 ID 等待表 | 同时适配 stdio 乱序响应和 HTTP 响应，且便于实现超时、取消 |
| 并发模型 | 虚拟线程处理读取、连接和阻塞等待 | 与现有同步 `Tool.execute()` 接口兼容，同时避免长期占用平台线程 |
| Transport 边界 | Transport 只收发 JSON-RPC，MCP 语义全部留在 Client | 两种传输可复用相同握手、分页、调用和错误逻辑 |
| 配置合并 | 同名 Server 整体覆盖，不做字段级深度合并 | 避免 command、URL、headers 等字段跨层拼出不可预测配置 |
| 配置优先级 | 用户级 > 项目级 > 本地级 | 与已经批准的需求一致；按低到高依次覆盖实现 |
| 配置安全 | 配置文件为符号链接时拒绝加载 | 防止工作区配置被链接到非预期位置 |
| 环境变量替换 | 仅支持严格 `${NAME}`，缺失则禁用该 Server | 行为清晰，不把未展开占位符发送给子进程或远程服务 |
| 子进程环境 | 清空继承环境，只加入 `PATH` 和显式 `env` | 防止 API Key、云凭证等宿主环境信息泄露 |
| stdio 启动 | 每次应用启动逐 Server 确认 | 子进程本身具有执行本地程序的能力，需要明确 HITL 边界 |
| HTTP 地址 | 远程 HTTPS；环回地址可 HTTP；禁止 URL 凭据 | 在开发便利性和远程传输安全之间取得平衡 |
| HTTP 重定向 | 关闭自动跳转，仅手动跟随最多 3 次同源跳转 | 避免认证头或 Session ID 被转发到其他源 |
| Streamable HTTP 范围 | POST 支持 JSON 和有限 SSE；不打开长期 GET | 满足工具调用响应，排除服务端推送、断线恢复等本章不做能力 |
| Session 过期 | 当前请求失败并标记连接不可用，不重放工具调用 | 避免可能具有副作用的远程工具被自动执行第二次；重新启动应用后建立新会话 |
| 请求重试 | 不自动重试 | MCP 工具可能产生外部副作用，无法普遍保证幂等 |
| 请求超时 | 初始化/发现 10 秒，工具调用 120 秒，可按 Server 覆盖 | 防止启动和调用永久挂起，同时兼顾较慢的远程操作 |
| 分页保护 | 检测重复 cursor，并设置最大 100 页 | 防止错误 Server 造成无限发现循环或内存膨胀 |
| 服务端请求 | 返回 JSON-RPC `-32601` | 明确拒绝本章不支持的 Sampling、Elicitation 等 Client 能力 |
| 工具命名 | 固定前缀并替换非法字符；过长或碰撞直接拒绝 | 保证名称稳定可配置，不通过截断制造隐式碰撞 |
| MCP 权限 | 所有 MCP 工具均为 `COMMAND + HIGH` | 远端 Schema 和 annotations 不能作为本地可信安全依据 |
| Plan Mode | 继续使用现有静态只读白名单 | 无需增加第二套 MCP 过滤逻辑，保证远程工具绝不进入 Plan Mode |
| 远程结果 | 文本和结构化 JSON 可见；二进制只显示摘要 | 控制上下文成本，避免大体积 base64 和敏感数据直接进入模型 |
| Server instructions | 保存为诊断元数据但不注入 Prompt | 远程 Server 不应获得修改 System Prompt 的隐式能力 |
| 启动失败策略 | Server 级和工具级部分成功 | 单个社区 Server 的故障不应让 ImioCode 整体不可用 |
| 动态刷新 | 启动时发现一次，修改配置或工具清单后重启 | 热加载与 `list_changed` 明确不属于本章 |
| 生命周期 | Manager 统一持有 Client 并幂等关闭 | 防止子进程、HTTP 会话和读线程泄漏 |
| 测试策略 | 假 stdio 子进程 + 本地 HTTP Server + 集成测试 | 可重复、无公网依赖，也能覆盖真实进程管道和 HTTP 行为 |
| 端到端验收 | 优先 tmux；当前 Windows 无可用 tmux 时运行真实 Java 子进程场景并记录限制 | 遵循项目验收要求，同时让当前环境仍有可复现证据 |

补充安全决策：

- 配置、日志、UI 事件和异常不得输出 env/header 的值。
- HTTP 响应、stdio 消息和 stderr 缓冲都设置硬上限。
- JSON-RPC 错误中的服务端 `data` 只用于内部诊断，输出前必须脱敏和限长。
- MCP 参数不复制到权限提示的目标字段，防止参数中的凭据进入 UI。
- 不根据远程工具的 `readOnlyHint` 降低风险等级。

## Spec 覆盖映射

| Spec | 架构归属 |
|---|---|
| F1 三层配置 | `mcp.config`、`McpManager` |
| F2 环境变量与隔离 | `McpEnvironmentResolver`、`StdioMcpTransport`、`SecretRedactor` |
| F3 JSON-RPC | `mcp.jsonrpc`、`DefaultMcpClient.pendingRequests` |
| F4 stdio Transport | `StdioMcpTransport`、`McpLaunchApprover` |
| F5 Streamable HTTP | `StreamableHttpMcpTransport`、`SseMessageDecoder` |
| F6 MCP 会话 | `McpClient`、`DefaultMcpClient` |
| F7 远程工具适配 | `McpToolName`、`McpToolWrapper`、权限集成 |
| F8 工具结果映射 | `McpToolResultMapper`、`SecretRedactor`、`ToolLimits` |
| F9 MCP Manager | `McpManager`、`McpStartupResult` |
| F10 终端可观测性 | `McpEventListener`、`TerminalUi`、`JLineTerminalUi` |

## 依赖方向

```text
mcp.config ───────────────┐
mcp.jsonrpc ──────────────┼→ mcp.transport → mcp.client → mcp.tool
                          │                         │          │
现有 tool / permission ───┴─────────────────────────┴──────────┤
                                                              ↓
                           terminal abstractions ← mcp.manager
                                      ↑                │
                                      └─ ImioCodeApplication
```

- `mcp.jsonrpc` 不依赖任何 MCP 业务模块。
- Transport 不依赖 Client、工具或 UI。
- Client 不依赖 Manager、工具注册中心或 UI。
- 工具适配层不依赖 Manager。
- Manager 只通过 `McpLaunchApprover` 和 `McpEventListener` 抽象通知终端。
- `ImioCodeApplication` 是唯一的顶层组装入口，因此模块依赖无环。
