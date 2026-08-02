# ImioCode

基于 Java 21 的终端 AI 编程助手，支持多轮 Agent Loop、内置文件/命令工具、权限系统和 MCP 外部工具。

## 构建与启动

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
mvn package
java -jar target/imiocode-0.2.0-SNAPSHOT-all.jar
```

所有项目配置统一放在根目录 `config.yaml`。首次使用可复制 `config.example.yaml`，并通过环境变量提供 API Key：

```powershell
Copy-Item config.example.yaml config.yaml
$env:DEEPSEEK_API_KEY="你的密钥"
```

环境变量直接覆盖仍保持最高优先级。配置中的敏感值也可以使用 `${NAME}` 引用，避免把明文凭据写进项目文件。

## MCP Server 配置

MCP Server 放在根配置的 `mcp.servers` 下。修改配置或 Server 工具后需要重启 ImioCode。

旧的用户级、项目级和本地级 MCP 文件仍可兼容读取，但只有 `config.yaml` 完全缺少 `mcp` 字段时才会回退，并显示迁移提示。完整迁移说明见 `docs/config-unification/migration.md`。

### stdio

```yaml
mcp:
  servers:
    local-tools:
      transport: stdio
      command: npx
      args:
        - -y
        - "@modelcontextprotocol/server-example"
      env:
        EXAMPLE_TOKEN: "${EXAMPLE_TOKEN}"
      initialization-timeout-seconds: 10
      call-timeout-seconds: 120
```

每次启动时，ImioCode 都会显示 Server 名、command 和 args，并等待用户确认后才启动 stdio 子进程。子进程只会收到 PATH 和该 Server 显式配置的 env，不会继承模型 API Key 等其他环境变量。

### Streamable HTTP

```yaml
mcp:
  servers:
    remote-tools:
      transport: streamable-http
      url: https://mcp.example.com/mcp
      headers:
        Authorization: "Bearer ${MCP_ACCESS_TOKEN}"
```

远程地址必须使用 HTTPS；仅 localhost、127.0.0.1 和 `[::1]` 允许 HTTP。当前支持 POST 返回的 JSON 或有限 SSE，不支持旧版 SSE Transport 和长期 GET 推送。

`${NAME}` 只能读取 ImioCode 启动环境中的变量。变量缺失时只禁用对应 Server，错误不会显示变量值。

## MCP 工具与权限

远程工具在本地统一命名为：

```text
mcp_<serverName>__<toolName>
```

例如 Server `github-main` 的 `issues/list` 会成为 `mcp_github-main__issues_list`。

所有 MCP 工具均按高风险命令处理：

- `ask`、`auto-edit`：默认需要 HITL 确认。
- `read-only`、`lockdown`：禁止执行。
- `full-access`：允许执行。
- Plan Mode：不向模型暴露 MCP 工具。

权限模式与规则放在根配置的 `permissions` 区域：

```yaml
permissions:
  mode: ask
  rules:
    - action: allow
      tool: "mcp_github-main__issues_*"

    - action: deny
      tool: "mcp_database__drop_*"
```

MCP Header、环境变量值和工具参数不会显示在 stdio 启动确认中。
# 上下文管理（ch8）

ImioCode 默认按 64,000 Token 上下文窗口工作，在近似预算达到 80% 时自动压缩。估算采用约
3.5 字符/Token 的稳定近似值，因此它用于预算保护，不等同于 Provider 的精确 tokenizer。

可在 `config.yaml` 中覆盖默认值：

```yaml
context:
  window-tokens: 64000
  auto-compact-threshold: 0.80
```

环境变量 `IMIO_CONTEXT_WINDOW_TOKENS` 和 `IMIO_CONTEXT_AUTO_COMPACT_THRESHOLD` 的优先级更高。
在终端输入 `/compact` 可随时强制压缩历史，并显示压缩前后的近似 Token 数。

超过阈值的工具结果会以 UTF-8 写入 `.imiocode/tool-results/`，对话中仅保留预览、相对路径和
`read_file` 提示。该目录默认被 Git 与常规 Glob 扫描忽略，但模型可以用明确路径读取其中的文件。
