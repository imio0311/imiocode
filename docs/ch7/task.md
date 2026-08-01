# ch7 MCP 开放工具生态 Tasks

## 文件清单

### 新建生产代码

| 模块 | 文件 | 职责 |
|---|---|---|
| 配置 | `src/main/java/io/imiocode/mcp/config/McpTransportType.java` | Transport 类型 |
| 配置 | `src/main/java/io/imiocode/mcp/config/McpServerConfig.java` | 合并前的 Server 配置 |
| 配置 | `src/main/java/io/imiocode/mcp/config/ResolvedMcpServerConfig.java` | 环境变量展开后的安全配置 |
| 配置 | `src/main/java/io/imiocode/mcp/config/McpConfigDocument.java` | YAML 映射模型 |
| 配置 | `src/main/java/io/imiocode/mcp/config/McpConfigError.java` | 单文件或单 Server 安全错误 |
| 配置 | `src/main/java/io/imiocode/mcp/config/McpConfigLoadResult.java` | 有效配置与错误汇总 |
| 配置 | `src/main/java/io/imiocode/mcp/config/McpConfigLoader.java` | 三层定位、解析、合并和校验 |
| 配置 | `src/main/java/io/imiocode/mcp/config/McpEnvironmentResolver.java` | `${NAME}` 展开及缺失变量检测 |
| JSON-RPC | `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcMessage.java` | 消息密封接口 |
| JSON-RPC | `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcId.java` | 字符串/整数请求 ID |
| JSON-RPC | `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcRequest.java` | 请求消息 |
| JSON-RPC | `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcResponse.java` | 成功或错误响应 |
| JSON-RPC | `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcNotification.java` | 通知消息 |
| JSON-RPC | `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcError.java` | 标准错误对象 |
| JSON-RPC | `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcCodec.java` | 严格编解码 |
| Transport | `src/main/java/io/imiocode/mcp/transport/McpTransport.java` | 统一异步传输接口 |
| Transport | `src/main/java/io/imiocode/mcp/transport/McpTransportFactory.java` | 根据配置创建 Transport |
| Transport | `src/main/java/io/imiocode/mcp/transport/McpTransportException.java` | 安全传输异常 |
| Transport | `src/main/java/io/imiocode/mcp/transport/StdioMcpTransport.java` | 子进程管道传输 |
| Transport | `src/main/java/io/imiocode/mcp/transport/StreamableHttpMcpTransport.java` | POST 型 Streamable HTTP |
| Transport | `src/main/java/io/imiocode/mcp/transport/SseMessageDecoder.java` | 有限 SSE `data:` 解码 |
| Client | `src/main/java/io/imiocode/mcp/client/McpClient.java` | MCP Client 对外接口 |
| Client | `src/main/java/io/imiocode/mcp/client/DefaultMcpClient.java` | 请求匹配、握手、发现、调用 |
| Client | `src/main/java/io/imiocode/mcp/client/McpClientState.java` | Client 状态机 |
| Client | `src/main/java/io/imiocode/mcp/client/McpInitializeResult.java` | 初始化结果 |
| Client | `src/main/java/io/imiocode/mcp/client/McpServerInfo.java` | Server 元信息 |
| Client | `src/main/java/io/imiocode/mcp/client/McpRemoteTool.java` | 远程工具定义 |
| Client | `src/main/java/io/imiocode/mcp/client/McpCallHandle.java` | 可取消调用句柄 |
| Client | `src/main/java/io/imiocode/mcp/client/McpCallResult.java` | 远程调用结果 |
| 工具适配 | `src/main/java/io/imiocode/mcp/tool/McpToolName.java` | 本地名称清洗和校验 |
| 工具适配 | `src/main/java/io/imiocode/mcp/tool/McpToolResultMapper.java` | 内容映射、脱敏和截断 |
| 工具适配 | `src/main/java/io/imiocode/mcp/tool/McpToolWrapper.java` | MCP 工具到现有 `Tool` 的适配器 |
| Manager | `src/main/java/io/imiocode/mcp/manager/McpManager.java` | 审批、连接、注册、缓存和关闭 |
| Manager | `src/main/java/io/imiocode/mcp/manager/McpStartupResult.java` | 启动汇总 |
| Manager | `src/main/java/io/imiocode/mcp/manager/McpLaunchApprover.java` | stdio 启动审批抽象 |
| Manager | `src/main/java/io/imiocode/mcp/manager/McpLaunchRequest.java` | 不含敏感值的审批信息 |
| Manager | `src/main/java/io/imiocode/mcp/manager/McpEvent.java` | MCP 可观测事件 |
| Manager | `src/main/java/io/imiocode/mcp/manager/McpEventType.java` | 事件类型 |
| Manager | `src/main/java/io/imiocode/mcp/manager/McpEventListener.java` | UI 事件监听抽象 |

### 修改生产代码

| 文件 | 改动 |
|---|---|
| `src/main/java/io/imiocode/ImioCodeApplication.java` | 组装 MCP 配置、Manager、UI 与关闭生命周期 |
| `src/main/java/io/imiocode/permission/PermissionRequestFactory.java` | 将 `mcp_` 工具归类为高风险命令 |
| `src/main/java/io/imiocode/terminal/TerminalUi.java` | 增加 MCP 审批和事件入口 |
| `src/main/java/io/imiocode/terminal/JLineTerminalUi.java` | 渲染 stdio 确认和 MCP 状态 |
| `src/main/java/io/imiocode/tool/SecretRedactor.java` | 注册并脱敏运行期展开的密钥 |
| `README.md` | 增加 MCP 配置与使用说明 |

### 新建及修改测试

| 文件 | 职责 |
|---|---|
| `src/test/java/io/imiocode/mcp/config/McpConfigLoaderTest.java` | 三层合并、校验、符号链接与错误隔离 |
| `src/test/java/io/imiocode/mcp/config/McpEnvironmentResolverTest.java` | 占位符展开和秘密脱敏 |
| `src/test/java/io/imiocode/mcp/jsonrpc/JsonRpcCodecTest.java` | 四类消息和畸形消息 |
| `src/test/java/io/imiocode/mcp/transport/StdioMcpTransportTest.java` | 环境隔离、管道、失败和关闭 |
| `src/test/java/io/imiocode/mcp/transport/StreamableHttpMcpTransportTest.java` | JSON、SSE、Session、Header 与 URL 安全 |
| `src/test/java/io/imiocode/mcp/transport/SseMessageDecoderTest.java` | SSE 分帧、注释、多行与上限 |
| `src/test/java/io/imiocode/mcp/client/DefaultMcpClientTest.java` | 握手、乱序响应、分页、超时和取消 |
| `src/test/java/io/imiocode/mcp/tool/McpToolNameTest.java` | 名称清洗、长度与碰撞输入 |
| `src/test/java/io/imiocode/mcp/tool/McpToolResultMapperTest.java` | 文本、JSON、二进制摘要、错误和截断 |
| `src/test/java/io/imiocode/mcp/tool/McpToolWrapperTest.java` | Schema、执行和取消透传 |
| `src/test/java/io/imiocode/mcp/manager/McpManagerTest.java` | 审批、部分成功、缓存、注册和关闭 |
| `src/test/java/io/imiocode/mcp/fixture/FakeStdioMcpServer.java` | 真实 Java 子进程测试 Server |
| `src/test/java/io/imiocode/mcp/McpIntegrationTest.java` | Registry、权限、Agent 与 MCP 集成 |
| `src/test/java/io/imiocode/permission/PermissionRequestFactoryTest.java` | 补充 MCP 权限归类 |
| `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java` | 补充 MCP 确认和安全输出 |

### 文档

| 文件 | 职责 |
|---|---|
| `docs/ch7/mcp.example.yaml` | stdio 与 Streamable HTTP 安全示例 |
| `docs/ch7/task.md` | 本任务拆解 |
| `docs/ch7/checklist.md` | 可观测验收项 |

## T1：建立配置值对象

**文件：**

- `src/main/java/io/imiocode/mcp/config/McpTransportType.java`
- `src/main/java/io/imiocode/mcp/config/McpServerConfig.java`
- `src/main/java/io/imiocode/mcp/config/ResolvedMcpServerConfig.java`
- `src/main/java/io/imiocode/mcp/config/McpConfigDocument.java`
- `src/main/java/io/imiocode/mcp/config/McpConfigError.java`
- `src/main/java/io/imiocode/mcp/config/McpConfigLoadResult.java`

**依赖：** 无

**步骤：**

1. 定义两种 Transport 类型，并支持 YAML 的 `stdio`、`streamable-http` 文本。
2. 为所有集合字段做防御性复制，为默认超时提供常量。
3. 区分未展开配置和已展开配置。
4. 为已展开配置覆盖 `toString()`，不输出 env、headers 的值。
5. 定义包含来源、Server 名、错误代码和安全消息的错误模型。

**验证：** 运行 `mvn -q -DskipTests compile`，期望配置类型编译通过。

## T2：实现三层配置发现与覆盖

**文件：** `src/main/java/io/imiocode/mcp/config/McpConfigLoader.java`

**依赖：** T1

**步骤：**

1. 接收 workspace、userHome 和启动环境，不在内部读取不可替换的全局状态。
2. 按本地级、项目级、用户级顺序定位三个文件。
3. 使用 `NOFOLLOW_LINKS` 验证普通文件，拒绝符号链接。
4. 使用 Jackson YAML 将每层解析为 `McpConfigDocument`。
5. 同名 Server 使用高优先级完整对象覆盖，保留来源信息。
6. 单层 YAML 错误加入错误列表，并继续处理其他层。

**验证：** 运行 `mvn -q -DskipTests compile`，期望 Loader 与配置模型编译通过。

## T3：实现逐 Server 配置校验

**文件：** `src/main/java/io/imiocode/mcp/config/McpConfigLoader.java`

**依赖：** T2

**步骤：**

1. 校验 Server 名、enabled、Transport、超时正数。
2. 校验 stdio 只使用 command/args，HTTP 只使用 URL/headers。
3. 校验远程 HTTPS、环回 HTTP、URL 无 user-info。
4. 将配置错误隔离为对应 Server 的 `McpConfigError`。
5. 保证禁用配置不创建连接，但不会被当成全局错误。

**验证：** 运行 `mvn -q -DskipTests compile`，期望非法 Server 可表示为错误而不抛出全局异常。

## T4：实现环境变量展开与动态脱敏

**文件：**

- `src/main/java/io/imiocode/mcp/config/McpEnvironmentResolver.java`
- `src/main/java/io/imiocode/tool/SecretRedactor.java`

**依赖：** T1、T3

**步骤：**

1. 只识别严格 `${NAME}` 占位符，支持一个值中存在多个占位符。
2. 仅从注入的启动环境读取变量。
3. 缺失变量时返回仅含变量名的安全错误并禁用对应 Server。
4. 将成功展开的非空敏感值注册到 `SecretRedactor`。
5. 使用线程安全集合保存动态秘密，脱敏时同时处理 API Key、Bearer/Header 和动态秘密。
6. 确保任何对象的 `toString()` 不包含展开值。

**验证：** 运行 `mvn -q -DskipTests compile`，期望 SecretRedactor 兼容原构造方式且新接口编译通过。

## T5：补齐配置与环境测试

**文件：**

- `src/test/java/io/imiocode/mcp/config/McpConfigLoaderTest.java`
- `src/test/java/io/imiocode/mcp/config/McpEnvironmentResolverTest.java`

**依赖：** T2、T3、T4

**步骤：**

1. 测试三层不同 Server 合并及同名整体覆盖。
2. 测试缺少 command/url、冲突字段、无效超时和禁用 Server。
3. 测试 YAML 错误与符号链接不影响其他层；平台不允许创建符号链接时使用条件跳过。
4. 测试多占位符展开、缺失变量和动态秘密脱敏。
5. 断言错误、配置对象和字符串表示均不包含秘密值。

**验证：** 运行 `mvn -q -Dtest=McpConfigLoaderTest,McpEnvironmentResolverTest test`，期望 0 failures、0 errors。

## T6：建立 JSON-RPC 消息模型

**文件：**

- `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcMessage.java`
- `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcId.java`
- `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcRequest.java`
- `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcResponse.java`
- `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcNotification.java`
- `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcError.java`

**依赖：** 无

**步骤：**

1. 使用密封接口表达三种消息，Response 同时覆盖成功和错误。
2. 限制 ID 为整数或字符串，并对 `JsonNode` 防御性复制。
3. 校验 method 非空、Response 的 result/error 二选一。
4. 对 params、result、data 的缺省语义作统一约定。

**验证：** 运行 `mvn -q -DskipTests compile`，期望消息模型编译通过。

## T7：实现 JSON-RPC 严格编解码

**文件：** `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcCodec.java`

**依赖：** T6

**步骤：**

1. 编码时固定写出 `jsonrpc: "2.0"`。
2. 解码时按 id、method、result、error 区分消息类型。
3. 校验必填字段、ID 类型及 result/error 互斥。
4. 忽略未知字段，保留标准错误 code/message/data。
5. 将畸形 JSON 和协议错误转换为不包含原始秘密正文的安全异常。

**验证：** 运行 `mvn -q -DskipTests compile`，期望 Codec 编译通过。

## T8：验证 JSON-RPC 行为

**文件：** `src/test/java/io/imiocode/mcp/jsonrpc/JsonRpcCodecTest.java`

**依赖：** T7

**步骤：**

1. 测试请求、成功响应、错误响应和通知的往返。
2. 测试数字 ID 与字符串 ID。
3. 测试未知字段向前兼容。
4. 测试错误版本、缺失字段、双结果、非法 ID 和畸形 JSON。

**验证：** 运行 `mvn -q -Dtest=JsonRpcCodecTest test`，期望 0 failures、0 errors。

## T9：定义 Transport 公共契约

**文件：**

- `src/main/java/io/imiocode/mcp/transport/McpTransport.java`
- `src/main/java/io/imiocode/mcp/transport/McpTransportException.java`

**依赖：** T6、T7

**步骤：**

1. 定义 start、send、isOpen 和幂等 close。
2. 定义入站消息与致命失败回调。
3. 定义不携带原始请求正文的安全异常类型。
4. 明确 send Future 只代表消息已交给 Transport，不代表业务响应成功。

**验证：** 运行 `mvn -q -DskipTests compile`，期望 Transport 接口编译通过。

## T10：实现有限 SSE 解码器

**文件：** `src/main/java/io/imiocode/mcp/transport/SseMessageDecoder.java`

**依赖：** T7

**步骤：**

1. 按空行切分 SSE event，拼接多行 `data:`。
2. 忽略注释以及不消费的 event/id/retry 字段。
3. 跳过空 data event，解码非空 JSON-RPC data。
4. 对总响应、单事件和消息数量设置硬上限。
5. 流结束时处理最后一个未带空行的完整事件。

**验证：** 运行 `mvn -q -DskipTests compile`，期望 SSE 解码器编译通过。

## T11：验证有限 SSE 解码

**文件：** `src/test/java/io/imiocode/mcp/transport/SseMessageDecoderTest.java`

**依赖：** T10

**步骤：**

1. 测试单事件、多事件、注释和空 data。
2. 测试多行 data 拼接及结尾无空行。
3. 测试畸形 JSON、事件过多和响应超限。
4. 验证不建立 GET 或保留重连状态。

**验证：** 运行 `mvn -q -Dtest=SseMessageDecoderTest test`，期望 0 failures、0 errors。

## T12：实现 stdio 子进程启动与环境隔离

**文件：** `src/main/java/io/imiocode/mcp/transport/StdioMcpTransport.java`

**依赖：** T4、T9

**步骤：**

1. 用 command 和 args 分项构造 `ProcessBuilder`，不经过 Shell。
2. 清空环境，只放回父进程 PATH 和显式 env。
3. 设置工作目录并启动子进程。
4. 初始化 UTF-8 stdin/stdout/stderr 管道。
5. 使用原子状态保证 start 只能成功执行一次。

**验证：** 运行 `mvn -q -DskipTests compile`，期望 stdio Transport 可构造并编译通过。

## T13：实现 stdio 收发与失败传播

**文件：** `src/main/java/io/imiocode/mcp/transport/StdioMcpTransport.java`

**依赖：** T12

**步骤：**

1. 使用写锁逐行发送 UTF-8 JSON-RPC。
2. 使用虚拟线程持续读取 stdout，并限制单行 4 MiB。
3. 使用独立虚拟线程持续排空 stderr，仅保留最近 64 KiB。
4. 将合法消息送入 inbound handler。
5. 对畸形 stdout、EOF、写入失败和子进程异常退出只触发一次 failure handler。
6. 失败后拒绝新发送并完成相关 Future。

**验证：** 运行 `mvn -q -DskipTests compile`，期望读写和失败路径编译通过。

## T14：实现 stdio 关闭与假 Server

**文件：**

- `src/main/java/io/imiocode/mcp/transport/StdioMcpTransport.java`
- `src/test/java/io/imiocode/mcp/fixture/FakeStdioMcpServer.java`

**依赖：** T13

**步骤：**

1. 实现关闭 stdin、等待、destroy、destroyForcibly 的顺序。
2. 中断并回收读写虚拟线程，close 保持幂等。
3. 编写 Java 假 Server，支持 initialize、initialized、分页 tools/list、tools/call 和 cancelled。
4. 为测试模式增加乱序响应、畸形输出、异常退出、延迟及环境回显。
5. 假 Server 的 stdout 只输出协议消息，诊断仅写 stderr。

**验证：** 运行 `mvn -q -DskipTests test-compile`，期望生产代码和假 Server 编译通过。

## T15：验证 stdio Transport

**文件：** `src/test/java/io/imiocode/mcp/transport/StdioMcpTransportTest.java`

**依赖：** T14

**步骤：**

1. 使用当前 Java 可执行文件启动 `FakeStdioMcpServer`。
2. 验证 UTF-8 单行消息往返及中文工作路径。
3. 验证子进程只能看到 PATH 和显式 env，不能看到测试注入秘密。
4. 验证 stderr 大量输出不会阻塞，且保留量有界。
5. 验证畸形 stdout、异常退出和管道关闭触发安全失败。
6. 验证 close 后子进程在宽限期内终止。

**验证：** 运行 `mvn -q -Dtest=StdioMcpTransportTest test`，期望 0 failures、0 errors 且无遗留 Java 子进程。

## T16：实现 HTTP URL 与请求安全

**文件：** `src/main/java/io/imiocode/mcp/transport/StreamableHttpMcpTransport.java`

**依赖：** T4、T9

**步骤：**

1. 使用 `HttpClient.Redirect.NEVER` 创建 Client。
2. 校验远程 HTTPS、环回 HTTP、无 URL user-info。
3. 为每个消息建立 POST，请求体为单个 JSON-RPC 对象。
4. 设置 Content-Type 和 Accept，不复制未声明的宿主 Header。
5. 限制配置 Header 名和值，拒绝覆盖 Host、Content-Length 等受限头。

**验证：** 运行 `mvn -q -DskipTests compile`，期望安全校验和请求构造编译通过。

## T17：实现 HTTP JSON、Session 与协议头

**文件：** `src/main/java/io/imiocode/mcp/transport/StreamableHttpMcpTransport.java`

**依赖：** T16

**步骤：**

1. 异步发送 POST 并限制响应最大 4 MiB。
2. 接受请求对应的 JSON 响应，以及通知对应的 202 空响应。
3. 从初始化响应保存可见 ASCII `MCP-Session-Id`。
4. 初始化成功后在后续请求添加 Session ID 和 `MCP-Protocol-Version`。
5. 将非成功状态转换为限长、脱敏的 Transport 错误。
6. HTTP 404 且已有 Session 时使当前连接失败，不重放原工具调用。

**验证：** 运行 `mvn -q -DskipTests compile`，期望 JSON 与 Session 路径编译通过。

## T18：实现 HTTP SSE、重定向与关闭

**文件：**

- `src/main/java/io/imiocode/mcp/transport/StreamableHttpMcpTransport.java`
- `src/main/java/io/imiocode/mcp/transport/McpTransportFactory.java`

**依赖：** T10、T17

**步骤：**

1. 根据 Content-Type 将有限 SSE 交给 `SseMessageDecoder`。
2. 将 SSE 中每条消息依次交给 inbound handler。
3. 手动跟随最多 3 次同源重定向，跨源或无 Location 时失败。
4. close 时取消进行中的 HTTP Future；有 Session 时尽力发送 DELETE。
5. 实现 Transport Factory，按配置创建 stdio 或 HTTP 实现。
6. 保证 close 和并发 send/close 的竞争只完成一次。

**验证：** 运行 `mvn -q -DskipTests compile`，期望两个 Transport 可经 Factory 创建。

## T19：验证 Streamable HTTP

**文件：** `src/test/java/io/imiocode/mcp/transport/StreamableHttpMcpTransportTest.java`

**依赖：** T18

**步骤：**

1. 使用 JDK 本地 HTTP Server 返回 JSON、202 和有限 SSE。
2. 验证 Accept、Content-Type、Session ID 和协议版本 Header。
3. 验证同源重定向可用，跨源重定向、远程 HTTP 和 URL 凭据被拒绝。
4. 验证响应超限、错误 Content-Type、畸形 JSON/SSE 和 HTTP 错误。
5. 验证取消 Future 与 close 后 DELETE Session。

**验证：** 运行 `mvn -q -Dtest=StreamableHttpMcpTransportTest test`，期望 0 failures、0 errors。

## T20：建立 Client 模型与请求路由

**文件：**

- `src/main/java/io/imiocode/mcp/client/McpClient.java`
- `src/main/java/io/imiocode/mcp/client/McpClientState.java`
- `src/main/java/io/imiocode/mcp/client/McpInitializeResult.java`
- `src/main/java/io/imiocode/mcp/client/McpServerInfo.java`
- `src/main/java/io/imiocode/mcp/client/McpRemoteTool.java`
- `src/main/java/io/imiocode/mcp/client/McpCallHandle.java`
- `src/main/java/io/imiocode/mcp/client/McpCallResult.java`
- `src/main/java/io/imiocode/mcp/client/DefaultMcpClient.java`

**依赖：** T9

**步骤：**

1. 定义 Client 接口、状态及不可变结果对象。
2. 使用单调 `AtomicLong` 分配数字 ID。
3. 使用 `ConcurrentMap` 保存等待响应。
4. 实现 send request、接收响应、未知/重复 ID 忽略。
5. 收到 Transport 致命失败时使所有等待项失败，并进入 FAILED。
6. close 时清空等待表、关闭 Transport，且保持幂等。

**验证：** 运行 `mvn -q -DskipTests compile`，期望 Client 基础请求路由编译通过。

## T21：实现初始化握手

**文件：** `src/main/java/io/imiocode/mcp/client/DefaultMcpClient.java`

**依赖：** T20

**步骤：**

1. start Transport 后发送协议版本 `2025-11-25` 的 initialize。
2. 使用 ImioCode 名称、版本和空的已支持 Client capabilities。
3. 校验响应协议版本、serverInfo 和 tools capability。
4. 保存 instructions 但不暴露给 Prompt 组装器。
5. 成功后发送 `notifications/initialized` 并进入 READY。
6. 收到 Server Request 时回复 `-32601`，安全忽略无关 Notification。

**验证：** 运行 `mvn -q -DskipTests compile`，期望握手状态机编译通过。

## T22：实现分页工具发现

**文件：** `src/main/java/io/imiocode/mcp/client/DefaultMcpClient.java`

**依赖：** T21

**步骤：**

1. READY 后调用 `tools/list`。
2. 按 `nextCursor` 获取后续页面并聚合工具。
3. 检测重复 cursor，限制最多 100 页。
4. 校验远程工具名称、description 和 inputSchema。
5. 缺少 inputSchema 时生成 `{type:"object", properties:{}}`。
6. 同一 Client 缓存发现结果，不为每次调用重复发现。

**验证：** 运行 `mvn -q -DskipTests compile`，期望分页发现编译通过。

## T23：实现工具调用、超时与取消

**文件：** `src/main/java/io/imiocode/mcp/client/DefaultMcpClient.java`

**依赖：** T22

**步骤：**

1. 构造 `tools/call` 的 name 和 arguments。
2. 返回包含 request ID 与结果 Future 的 `McpCallHandle`。
3. 解析 content、structuredContent 与 isError。
4. 为初始化、发现和工具调用应用各自超时。
5. 超时或主动取消时移除等待项，并尽力发送 `notifications/cancelled`。
6. 处理响应、取消、超时和 close 的竞争，确保 Future 只完成一次。

**验证：** 运行 `mvn -q -DskipTests compile`，期望调用和取消编译通过。

## T24：验证 MCP Client

**文件：** `src/test/java/io/imiocode/mcp/client/DefaultMcpClientTest.java`

**依赖：** T20、T21、T22、T23

**步骤：**

1. 使用可控内存 Transport 测试初始化请求和 initialized 通知。
2. 测试协议版本不匹配、缺少 tools capability 和不支持 Server Request。
3. 测试乱序、未知和重复响应 ID。
4. 测试多页工具发现、重复 cursor、缺省 Schema 和缓存。
5. 测试工具调用成功、业务错误、超时、取消和断线。
6. 断言每种结束路径的等待表均为空。

**验证：** 运行 `mvn -q -Dtest=DefaultMcpClientTest test`，期望 0 failures、0 errors。

## T25：实现 MCP 工具命名

**文件：** `src/main/java/io/imiocode/mcp/tool/McpToolName.java`

**依赖：** T20

**步骤：**

1. 构造固定 `mcp_<server>__<tool>` 前缀。
2. 将本地工具名不允许的字符替换为 `_`。
3. 校验清洗后的组件非空、最终长度不超过 64。
4. 不截断名称；无法表示时返回明确失败。

**验证：** 运行 `mvn -q -DskipTests compile`，期望名称工具编译通过。

## T26：实现远程结果映射

**文件：** `src/main/java/io/imiocode/mcp/tool/McpToolResultMapper.java`

**依赖：** T4、T20

**步骤：**

1. 按顺序拼接 text，并序列化 structuredContent。
2. 为 image、audio、resource、resource_link 生成安全摘要。
3. 禁止复制 data/base64 正文。
4. 空内容返回“无输出”，isError 返回失败结果。
5. 使用 `SecretRedactor` 脱敏，并按 UTF-8 字节应用 `maxResultBytes`。
6. 保留截断标记和实际执行时长。

**验证：** 运行 `mvn -q -DskipTests compile`，期望结果映射器编译通过。

## T27：实现 MCP Tool Wrapper

**文件：** `src/main/java/io/imiocode/mcp/tool/McpToolWrapper.java`

**依赖：** T23、T25、T26

**步骤：**

1. 将远程名称、描述和 Schema 转为本地 `ToolDefinition`。
2. 固定风险为 `HIGH`。
3. 同步等待 `McpCallHandle.result()`，使用 Server call timeout。
4. 将协议、超时、中断和业务错误映射为安全 `ToolResult`。
5. 用原子引用保存当前句柄，执行完成后清理。
6. cancel 幂等地透传到 `McpClient.cancelRequest`。

**验证：** 运行 `mvn -q -DskipTests compile`，期望 Wrapper 实现现有 `Tool` 接口。

## T28：验证工具命名、结果与 Wrapper

**文件：**

- `src/test/java/io/imiocode/mcp/tool/McpToolNameTest.java`
- `src/test/java/io/imiocode/mcp/tool/McpToolResultMapperTest.java`
- `src/test/java/io/imiocode/mcp/tool/McpToolWrapperTest.java`

**依赖：** T25、T26、T27

**步骤：**

1. 测试斜杠、空格、中文、超长及清洗后相同的名称。
2. 测试多文本、结构化 JSON、空结果和 isError。
3. 测试 image/audio/resource 不包含 base64。
4. 测试 UTF-8 字节截断与动态秘密脱敏。
5. 测试 Wrapper Schema、HIGH 风险、成功、失败、超时和 cancel。

**验证：** 运行 `mvn -q -Dtest=McpToolNameTest,McpToolResultMapperTest,McpToolWrapperTest test`，期望 0 failures、0 errors。

## T29：建立 Manager 与事件模型

**文件：**

- `src/main/java/io/imiocode/mcp/manager/McpStartupResult.java`
- `src/main/java/io/imiocode/mcp/manager/McpLaunchApprover.java`
- `src/main/java/io/imiocode/mcp/manager/McpLaunchRequest.java`
- `src/main/java/io/imiocode/mcp/manager/McpEvent.java`
- `src/main/java/io/imiocode/mcp/manager/McpEventType.java`
- `src/main/java/io/imiocode/mcp/manager/McpEventListener.java`

**依赖：** T1

**步骤：**

1. 定义不携带 env、headers 和参数值的启动确认对象。
2. 定义连接、发现、调用、失败、拒绝和关闭事件。
3. 对事件字段做非空、长度与安全文本约束。
4. 定义无操作 Listener，便于非终端调用方使用。

**验证：** 运行 `mvn -q -DskipTests compile`，期望 Manager 边界类型编译通过。

## T30：实现 Manager 审批与并行连接

**文件：** `src/main/java/io/imiocode/mcp/manager/McpManager.java`

**依赖：** T18、T24、T27、T29

**步骤：**

1. 按 Server 名排序并过滤 disabled/invalid 配置。
2. 对 stdio Server 串行发送审批事件并调用 Approver。
3. 被拒绝时不创建 Transport，不启动子进程。
4. 审批完成后使用虚拟线程并行连接全部剩余 Server。
5. 捕获 Server 级异常，发送安全失败事件并继续其他连接。
6. 仅缓存完成握手和发现的 Client。

**验证：** 运行 `mvn -q -DskipTests compile`，期望 Manager 启动流程编译通过。

## T31：实现工具注册、缓存与关闭

**文件：** `src/main/java/io/imiocode/mcp/manager/McpManager.java`

**依赖：** T30

**步骤：**

1. 将成功发现的远程工具按确定性顺序包装。
2. 注册前检测同一 Server 清洗碰撞和 `ToolRegistry` 已有名称。
3. 单工具失败只跳过该工具，不关闭同 Server 的其他工具。
4. 汇总连接数、注册数和安全错误。
5. start 保证同一 Manager 只执行一次。
6. close 停止接受新工作、关闭所有 Client 和执行器，且保持幂等。

**验证：** 运行 `mvn -q -DskipTests compile`，期望注册与生命周期编译通过。

## T32：验证 Manager

**文件：** `src/test/java/io/imiocode/mcp/manager/McpManagerTest.java`

**依赖：** T30、T31

**步骤：**

1. 测试 stdio 批准和拒绝，断言拒绝时 Transport Factory 未调用。
2. 测试三个 Server 中一个失败时另外两个注册成功。
3. 测试清洗碰撞、内置名称碰撞、过长名称的隔离。
4. 测试同 Server 缓存一次、工具发现一次。
5. 测试事件不含 env/header/参数秘密。
6. 测试 close 关闭全部 Client，且重复 close 不报错。

**验证：** 运行 `mvn -q -Dtest=McpManagerTest test`，期望 0 failures、0 errors。

## T33：接入现有权限系统

**文件：**

- `src/main/java/io/imiocode/permission/PermissionRequestFactory.java`
- `src/test/java/io/imiocode/permission/PermissionRequestFactoryTest.java`

**依赖：** T25

**步骤：**

1. 识别 `mcp_` 前缀并分类为 `COMMAND`。
2. 归一化目标只使用本地工具名，不序列化参数。
3. 保持危险命令检测、路径沙箱和内置工具分类不变。
4. 测试 ask/auto-edit 默认 ASK、显式规则 ALLOW/DENY 可覆盖。
5. 验证权限请求和提示中不存在 MCP 参数秘密。

**验证：** 运行 `mvn -q -Dtest=PermissionRequestFactoryTest,PermissionCheckerTest test`，期望 0 failures、0 errors。

## T34：扩展终端 UI

**文件：**

- `src/main/java/io/imiocode/terminal/TerminalUi.java`
- `src/main/java/io/imiocode/terminal/JLineTerminalUi.java`
- `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java`

**依赖：** T29

**步骤：**

1. 让 TerminalUi 实现或适配 `McpLaunchApprover` 与 `McpEventListener`。
2. 渲染 Server 名、command、args 和允许/拒绝选择。
3. 不展示 env、headers 和工具参数。
4. 为连接、发现数量、配置、协议、超时、业务失败使用可区分文本。
5. 保持现有权限对话框、Agent 状态和输入循环不变。
6. 用捕获输出测试确认和所有 MCP 事件。

**验证：** 运行 `mvn -q -Dtest=JLineTerminalUiTest,JLinePermissionPromptTest test`，期望 0 failures、0 errors。

## T35：接入应用启动与关闭

**文件：** `src/main/java/io/imiocode/ImioCodeApplication.java`

**依赖：** T5、T31、T33、T34

**步骤：**

1. 先创建 Terminal、Redactor、内置 ToolRegistry 和权限组件。
2. 加载 MCP 配置并将安全配置错误输出到 Terminal。
3. 启动 Manager，在创建 LLM Client 前注册远程工具。
4. 输出连接 Server 数和工具注册数。
5. 无 MCP 配置时不显示确认，不增加连接工作。
6. finally 中按 Conversation/LLM、MCP Manager、Terminal 的顺序幂等关闭。
7. 保持现有 ConfigException、IOException 和运行错误退出码。

**验证：** 运行 `mvn -q -DskipTests compile`，期望应用入口完整编译。

## T36：增加端到端集成测试

**文件：** `src/test/java/io/imiocode/mcp/McpIntegrationTest.java`

**依赖：** T15、T19、T24、T28、T32、T33、T35

**步骤：**

1. 用真实 stdio Transport 连接 Java 假 Server。
2. 通过 Manager 将远程工具注册到真实 ToolRegistry。
3. 使用测试 LLM Client 驱动 Agent 产生一次 MCP tool_use 和 tool_result。
4. 验证权限 ASK/允许后才调用，拒绝时 Server 未收到调用。
5. 验证 Plan Mode Schema 不含 MCP 工具。
6. 增加一个协议错误 Server 与正常 Server 共存的部分成功场景。
7. 关闭所有组件后断言子进程和等待任务已结束。

**验证：** 运行 `mvn -q -Dtest=McpIntegrationTest test`，期望 0 failures、0 errors。

## T37：补充示例与用户文档

**文件：**

- `docs/ch7/mcp.example.yaml`
- `README.md`

**依赖：** T3、T18、T25、T33、T34

**步骤：**

1. 提供 stdio 和 Streamable HTTP 示例，不写入任何真实凭据。
2. 说明三层路径、覆盖优先级和同名整体替换。
3. 说明 `${NAME}`、环境隔离、HTTPS/loopback 规则。
4. 说明每次启动的 stdio 确认和五种权限模式的影响。
5. 给出 `mcp_<server>__<tool>` 权限规则示例。
6. 明确配置修改、工具变更和失败 Server 需要重启。

**验证：** 搜索示例和 README，期望不存在真实 Token、绝对用户路径、旧版 SSE 或未实现能力描述。

## T38：运行完整回归与打包

**文件：** 全部生产代码和测试

**依赖：** T36、T37

**步骤：**

1. 运行完整测试。
2. 检查原有测试和新增测试均无失败。
3. 运行打包并确认 shaded JAR 生成。
4. 运行 `git diff --check` 检查空白和冲突标记。
5. 检查未意外修改用户文件 `claude.md`、`hello.txt`。

**验证：**

- `mvn test`：期望 0 failures、0 errors。
- `mvn package -DskipTests`：期望生成 `target/imiocode-0.2.0-SNAPSHOT-all.jar`。
- `git diff --check`：期望无错误。

## T39：执行真实进程端到端验收

**文件：** 不新增生产文件；依据 `docs/ch7/checklist.md` 记录证据

**依赖：** T38

**步骤：**

1. 优先检查 tmux 是否可用；可用时在隔离工作区创建 tmux 会话。
2. 配置 Java 假 stdio Server，启动打包后的 ImioCode。
3. 批准 Server 启动，输入一段要求模型调用该远程工具的真实请求。
4. 观察连接、工具发现、权限确认、调用结果和 Agent 后续回复。
5. 输入 `/plan`，验证远程工具不在模型工具列表；输入 `/do` 恢复。
6. 退出应用并验证假 Server 子进程终止。
7. 再以一个协议错误 Server 和一个正常 Server 验证部分成功。
8. 当前 Windows 环境若 tmux/WSL 不可用，则运行同等的真实 Java 子进程场景，并在验收报告明确记录环境限制。

**验证：** 对照 `docs/ch7/checklist.md`，端到端成功与失败场景均有实际终端输出或测试报告证据。

## 执行顺序

```text
T1 → T2 → T3 → T4 → T5

T6 → T7 → T8
            ├→ T9 → T12 → T13 → T14 → T15
            └→ T10 → T11
                      └────────→ T16 → T17 → T18 → T19

T9 → T20 → T21 → T22 → T23 → T24
                              ├→ T25 → T26 → T27 → T28
                              └→ T29 → T30 → T31 → T32

T25 → T33
T29 → T34

T5 + T31 + T33 + T34 → T35
T15 + T19 + T24 + T28 + T32 + T33 + T35 → T36
T3 + T18 + T25 + T33 + T34 → T37
T36 + T37 → T38 → T39
```

## 实施提交点

开发阶段按以下逻辑组提交，只暂存本章文件，不包含用户已有的 `claude.md` 和 `hello.txt`：

1. 配置与 JSON-RPC：T1–T8。
2. Transport：T9–T19。
3. Client 与工具适配：T20–T28。
4. Manager、权限、UI 与应用集成：T29–T36。
5. 文档、回归和验收修复：T37–T39。
