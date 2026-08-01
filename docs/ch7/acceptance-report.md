# ch7 MCP 开放工具生态验收报告

## 结论

- 通过：61/62 项。
- 待用户单独授权：1/62 项（AC31 中向外部 DeepSeek 发送真实项目对话的部分）。
- 自动化测试：277 tests，0 failures，0 errors，3 skipped。
- 可执行包：`target/imiocode-0.2.0-SNAPSHOT-all.jar`。
- `git diff --check`：通过。

## 通过项证据

### 配置与环境隔离

- 三层配置、整体覆盖、逐 Server 错误隔离、占位符展开和动态秘密脱敏由 `McpConfigLoaderTest`、`McpEnvironmentResolverTest` 验证。
- stdio 假 Server 实际回报子进程环境，只包含 PATH 和显式 env；模型 API Key 未继承。
- 配置不存在时返回空成功，不创建 MCP 连接。
- 符号链接拒绝逻辑已实现；当前 Windows 账户无创建符号链接权限，对应测试明确 skipped。

### JSON-RPC、Transport 与 Client

- 请求、成功响应、错误响应、通知和非法输入由 `JsonRpcCodecTest` 验证。
- 真实 stdio Java 子进程完成 UTF-8 收发、stderr 排空、畸形输出失败传播和关闭回收。
- Streamable HTTP 本地 Server 验证 JSON、有限 SSE、Session ID、协议版本头、同源重定向、跨源拒绝、URL 安全和 4 MiB 响应上限。
- Client 测试覆盖 `2025-11-25` 握手、tools capability、分页、工具调用、未知 Server Request、超时、取消和等待表清理。
- 真实管道测试发现并修复数值 ID 在 `LongNode`/`IntNode` 之间无法匹配的问题；现在按整数数学值匹配。
- 超时与 Agent 同时取消时，只有成功移除等待请求的一方发送取消通知。

### 工具适配、Manager、权限和 UI

- 名称固定为 `mcp_<server>__<tool>`，非法字符清洗，超长与碰撞安全拒绝。
- text、structuredContent、空结果、isError、image/audio/resource 摘要均有测试；二进制正文不进入上下文。
- MCP 工具固定 `HIGH + COMMAND`，权限目标只包含本地工具名，不复制参数。
- Plan Mode 集成测试确认 MCP 工具不可见；Do 模式 Agent 能通过真实 stdio Server 调用远程工具。
- Manager 测试覆盖 stdio 批准/拒绝、并行连接、部分成功、确定性注册、缓存、冲突和幂等关闭。
- 真实部分失败场景使用一个畸形 stdio Server 和一个正常 stdio Server；正常工具仍完成注册。
- 终端测试覆盖 stdio 启动确认、连接和工具注册事件。

### 构建与真实进程

- 最终全量命令：`mvn test`。
- 实际结果：277 tests，0 failures，0 errors，3 skipped。
- 三个 skipped 均为当前 Windows 账户不能创建符号链接：
  - `McpConfigLoaderTest`
  - `WorkspacePathSandboxTest`
  - `WorkspacePolicyTest`
- 打包命令：`mvn package -DskipTests`。
- 实际产物：`target/imiocode-0.2.0-SNAPSHOT-all.jar`，约 4.66 MiB。
- 依赖树未增加 MCP SDK、Reactor、Node 或 Python 依赖。
- 原生 Windows 无 tmux；WSL 创建实例返回 `E_ACCESSDENIED`。
- 替代真实进程验收成功：
  - 启动打包后的 ImioCode。
  - 读取临时 MCP YAML。
  - 用户输入 `1` 批准 stdio Server。
  - 终端显示连接成功、发现 1 个工具并注册 `mcp_e2e__echo_text`。
  - 输入 `/exit` 后终端显示 MCP 连接关闭。
  - 临时工作区和配置已清理。
- `McpIntegrationTest` 通过真实 Java stdio 子进程和真实 Agent Loop 输入对话请求，验证 tool_use、tool_result 和后续回复；LLM 部分使用确定性测试 Client，未连接外部服务。

## 待授权项

### AC31 外部真实模型对话

尝试使用现有 DeepSeek 配置发送一次最小对话时，安全审查拒绝执行，原因是请求会把项目 System Prompt 和环境信息发送给外部 DeepSeek，而用户尚未明确授权这次具体数据发送。

未绕过该限制，临时 `.imiocode/mcp.local.yaml` 和临时目录均已删除。

若用户明确批准“允许使用现有 DeepSeek 配置发送一次 ch7 MCP 端到端测试对话”，即可补跑该项并更新为 62/62。

## 工作区保护

- 用户已有的 `claude.md` 修改未编辑。
- 用户已有的 `hello.txt` 未编辑、未暂存。
- 本轮尚未创建 Git 提交，也未推送远端。
