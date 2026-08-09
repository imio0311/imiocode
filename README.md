# ImioCode

## 多 Agent 与后台任务（CH13）

Agent 定义使用 YAML frontmatter + Markdown 正文，按“项目 `.imiocode/agents/` > 用户
`~/.imiocode/agents/` > 内置 > 插件”加载。内置 `explore`、`plan`、`general-purpose`
三个类型通过同一个 `agent` 工具调用。`explore` 和 `plan` 始终只读；后台任务默认只允许
`read_file`、`glob`、`grep`，需要确认的操作会自动拒绝，不会挂起。

后台任务命令：`/tasks`、`/task info <id>`、`/task cancel <id>`。模型别名、全局禁用工具、
后台白名单和容量配置统一位于 `config.yaml` 的 `subagents:` 区域。`haiku` 未映射时会回退
父模型并显示警告。Agent 定义可使用 `isolation: none` 或 `isolation: worktree`；内置 Agent
默认使用独立 Worktree。

## Git Worktree 隔离（CH14）

`/worktree` 是完全本地的管理命令，不会调用模型：

```text
/worktree list
/worktree create <slug>
/worktree enter <slug>
/worktree exit [keep|remove]
/worktree remove <slug>
```

slug 只接受 1–64 个 ASCII 字母、数字、点、下划线或连字符，并拒绝路径、`..`、盘符、
首尾空白和尾点。Worktree 默认创建在 `.imiocode/worktrees/`，分支名为
`worktree-<slug>`。进入或退出时，ImioCode 会关闭当前工作区运行时，再按目标目录重建
Agent、文件与 Bash 工具、权限沙箱、Hook、MCP、会话和子 Agent 运行时。

`enter` 会把恢复信息原子写入 `.imiocode/worktree-session.json`。普通启动只提示可恢复状态；
只有从原仓库根目录显式执行 `java -jar ... --resume` 才恢复。`exit keep` 保留目录和分支；
`exit remove` 与 `remove` 必须确认，且安全检查失败时拒绝删除。dirty、未跟踪文件或独有提交
默认都会保留。ImioCode 不负责 Worktree 之间的 merge 或同步。

创建 Worktree 后会按 `worktrees:` 配置复制本地配置和被忽略的必要文件、设置仓库 hooks，
并尽力软链接依赖目录；平台不支持软链接时会显示警告但不会破坏 Worktree。子 Agent 使用
`isolation: worktree` 时会获得唯一目录、分支、工作区工具和上下文通知；clean 结果自动清理，
dirty 或有独有提交的结果会保留路径与分支供用户决定合并或丢弃。

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

## 终端显示模式

启动时始终显示原有响应式完整面板；进入对话后默认使用精简模式，只展示最终工具结果、完整模型回答以及必须处理的权限和错误信息：

```yaml
ui:
  verbosity: compact
```

如需查看状态变化、Thinking、Token Usage 和完整工具生命周期，可将值改为 `verbose`。运行中输入 `/verbose` 可临时切到详细模式，输入 `/compact-ui` 切回精简模式；这两个命令只改变当前进程的后续对话显示，不会重绘启动面板、调用模型、写入会话历史或修改 `config.yaml`。精简模式不会截断最终回答，也不会隐藏权限确认和错误。

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

## 跨会话指令、会话与记忆（CH9）

### 项目指令 `MEWCODE.md`

ImioCode 会在每个用户轮次开始时加载以下指令，越靠后的项目近端文件优先级越高：

1. 用户级：`%USERPROFILE%/.imiocode/MEWCODE.md`
2. Git 项目根目录到当前工作目录沿途的 `MEWCODE.md`

项目指令可以用独占一行的相对路径 include 拆分模块：

```markdown
@include docs/java-style.md
@include "docs/team conventions.md"
```

include 不能使用绝对路径或 `..`，也不能通过符号链接逃离当前指令根目录。指令通过
`system-reminder` 消息注入，不进入 System Prompt，也不会写入会话历史。

### 会话存档

成功完成的对话轮次会以崩溃安全的 JSONL 事务保存到 `.imiocode/sessions/`。每次启动默认创建新会话，
不会静默恢复旧历史；需要时使用：

```text
/session current
/session list
/session new
/session resume <会话ID>
/session delete <会话ID>
```

删除非当前会话必须再次确认。损坏的未提交尾部会先隔离为 `*.corrupt-*` 再恢复到最后完整提交；
已提交数据的中部损坏会被拒绝，避免加载错误历史。

### 双层记忆

用户级记忆保存在 `%USERPROFILE%/.imiocode/memories.md`，项目级记忆保存在
`.imiocode/memories.md`。项目记忆发生冲突时优先于用户记忆。可用以下本地命令管理，命令不会发送给模型，
也不会写入会话历史：

```text
/memory list [user|project]
/memory add <user|project> "长期信息"
/memory edit <user|project> <记忆ID> "更新后的信息"
/memory forget <user|project> <记忆ID>
```

自动记忆默认关闭。只有显式设置 `memory.auto-extract: true` 后，ImioCode 才会在成功轮次落盘后，
把经过脱敏的本轮用户文本和最终助手文本再次发送给当前 LLM Provider，提取稳定偏好、项目事实或长期决策。
Thinking、工具参数、工具输出、旧会话历史和已有记忆不会发送给提取请求。自动候选仍会经过秘密、个人敏感信息、
临时任务、重复内容和容量限制检查。

CH9 配置示例：

```yaml
instructions:
  enabled: true
  max-include-depth: 8
  max-expanded-bytes: 131072

sessions:
  enabled: true
  retention-days: 0
  max-sessions: 0

memory:
  enabled: true
  auto-extract: false
  user-scope-enabled: true
  project-scope-enabled: true
  max-entries-per-scope: 200
  max-entry-chars: 1000
  max-file-bytes: 262144
  extraction-output-tokens: 1024
```

`retention-days: 0` 和 `max-sessions: 0` 表示不自动清理。项目会话和项目记忆默认被 Git 忽略；
`MEWCODE.md` 不会被忽略，可以作为项目协作规范提交到仓库。

## Slash Command（CH10）

输入以 `/` 开头的内容时，ImioCode 会先在本地命令注册中心解析，不会把未知命令或参数错误写入会话。
输入 `/help` 可查看当前注册目录；在命令首段按 Tab 可补全主名和别名，参数部分不做补全。

十个核心命令：

```text
/help
/compact
/clear
/plan
/do
/session list|current|new|resume <id>|delete <id>
/memory list|add|edit|forget ...
/permission [ask|auto-edit|read-only|full-access|lockdown]
/status
/review [关注点]
```

常用别名包括 `/h`、`/?`、`/cls`、`/sessions`、`/mem`、`/perm`、`/st` 和 `/rv`。
兼容命令 `/exit`、`/quit`、`/verbose`、`/compact-ui` 继续可用。

- `/clear` 只清空终端显示，不删除当前会话历史。
- `/permission` 的切换仅对当前进程有效；重启后仍以 `config.yaml` 为准，已有权限规则不会改变。
- `/status` 只显示 Provider、模型、工作目录、Agent/权限模式、会话、Token 估算和 MCP 计数，不显示密钥。
- `/review` 会生成只读代码审查 Prompt 并进入一次 Agent Loop；其他普通本地/UI 命令不会调用 Agent。
- `/compact` 绕过 Agent Loop 和工具，但会在确有可压缩历史时调用一次专用 LLM 摘要请求。

## Hook 自动化（CH12）

在根 `config.yaml` 的 `hooks` 列表中声明自动化动作，修改配置后需要重启。未配置时 Hook Runtime 为
NOOP，不创建命令、HTTP 或后台任务。支持以下 15 个事件：

```text
startup / shutdown / session_start / session_end / turn_start / turn_end
pre_send / post_receive / pre_tool_use / post_tool_use / permission_request
file_change / command_execute / compact / error
```

只有 `pre_tool_use` 可以阻塞工具。下面的规则会在权限判断之前阻止写入 `.env`，失败结果以
`blocked by hook protect-dotenv: ...` 回传给模型，Agent 可以在下一轮调整方案：

```yaml
hooks:
  - id: protect-dotenv
    event: pre_tool_use
    if: 'tool_name == "write_file" && args.path ~= "**/.env"'
    reject: true
    reject-message: "禁止覆盖 .env，请改用 .env.example。"
    action:
      type: prompt
      message: "检测到敏感配置写入"
```

条件支持 `==`、`!=`、`=~`（正则）、`~=`（glob）以及纯 `&&` 或纯 `||`；同一表达式不能混用两种连接符。
模板可用 `$EVENT`、`$TOOL_NAME`、`$FILE_PATH`、`$MESSAGE`、`$ERROR` 和
`$TOOL_ARGS.<字段>`。变量只展开一次，参数中出现的 `$...` 不会二次执行替换。

动作类型：

- `command`：在工作区的平台 Shell 中执行；默认超时 600 秒，可设 `timeout-seconds`。只继承运行所需的
  PATH/系统环境，并通过 `MEWCODE_EVENT`、`MEWCODE_TOOL_NAME`、`MEWCODE_FILE_PATH`、
  `MEWCODE_MESSAGE`、`MEWCODE_ERROR`、`MEWCODE_TOOL_ARGS` 提供上下文。
- `prompt`：作为 `<system-reminder>` 消息进入下一次模型请求，不修改 System Prompt 和缓存键。
- `http`：默认 POST JSON，不跟随重定向，响应限制 1 MiB；请求头不会出现在 Hook 通知中。
- `agent`：CH12 仅保留配置与明确的 `NOT_IMPLEMENTED` 结果，不会调用 LLM。

`once: true` 只在当前进程执行一次，重启后重置；`async: true` 使用有界后台队列，但不允许用于
`pre_tool_use`。`on-error` 可取 `ignore`、`fail`，`pre_tool_use` 额外支持 `reject`。命令输出、HTTP
结果、通知和错误统一脱敏。任何 Hook 配置错误都会让本次整个 Hook 列表安全降级为空，并在 UI 显示诊断，
不会部分加载或阻止 ImioCode 启动。
