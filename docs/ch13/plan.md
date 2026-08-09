# CH13 多 Agent 与后台任务 Plan

## 架构概览

CH13 新增 `subagent` 子系统，由定义目录、请求归一化、模型解析、工具过滤、非交互运行器、任务管理和 Trace 七层组成。应用启动时建立定义快照并注册唯一 `agent` 工具；工具把定义式或 Fork 请求交给 Dispatcher，同步请求直接 RunToCompletion，后台请求提交 TaskManager。父 Agent 仅看到最终 ToolResult 或任务 ID，子 Agent 的中间事件由 Trace 收集而不写入父会话。

## 核心数据结构

### AgentDefinition

不可变定义：name、description、promptBody、model、permissionMode、maxTurns、timeout、tools、disallowedTools、backgroundAllowed、initialPrompt、skills、mcpServers、hooks、memory、isolation、source/sourcePath。名称采用小写字母、数字和连字符。

### SubagentRequest / SubagentResult

请求包含 description、prompt、可选 subagentType/model/name/mode/cwd/isolation、runInBackground、自定义禁止工具和父历史快照；结果包含 taskId、traceId、状态、最终文本、安全错误、stopReason 与 Token 汇总。

### TaskRecord / TraceRecord

TaskRecord 保存后台生命周期、deadline、Future、取消入口和最终结果。TraceRecord 保存调用树、模型、Agent 类型、状态、时间、TokenUsage 汇总与安全摘要。两者均由并发 Registry 管理并对外返回不可变快照。

### ToolFilterPipeline

输入 Registry 可用工具和执行上下文，按固定顺序做 deny 优先过滤，输出唯一 ToolSelection；同一 Selection 同时用于 Schema 导出和执行解析。

## 模块设计

### 定义解析与四来源加载

`AgentDefinitionParser` 解析 frontmatter/body，`AgentDefinitionLoader` 扫描项目 `.imiocode/agents/`、用户 `~/.imiocode/agents/`、classpath `agents/` 以及注入的插件源。每个名称按优先级选择第一份有效定义；刷新成功后原子替换快照。

### 模型解析与客户端工厂

`SubagentModelResolver` 实现调用值 > 定义值 > 父模型，逻辑别名来自 `config.yaml subagents.model-aliases`。未映射 `haiku` 回退父模型并记录 warning。后台任务总是获得独立 LlmClient；同步 Fork 也使用独立客户端以隔离取消状态，同时保持 API payload 的 system/messages 前缀稳定。

### Fork 与定义式上下文

`ForkContextBuilder` 深复制父历史，不重写已有消息；仅修复协议不完整的尾部工具调用，然后追加稳定 Fork reminder 和用户任务。定义式构造器从空历史开始，以 system-reminder 形式先放 definition body/initialPrompt，再放任务。两者都不把子轨迹提交父 ConversationSession。

### RunToCompletion

`RunToCompletion` 创建受限 Agent、注入 ToolSelection 与初始提醒，收集 Token 事件，运行到最终响应或确定终态。`NonInteractivePermissionBridge` 对后台 ASK 自动 DENY；同步前台可复用 UI PermissionGate，转后台时取消当前等待并按后台规则继续/失败。

### 工具过滤

`ToolFilterPipeline` 顺序：全局禁止 → 运行时额外禁止 → 后台白名单 → definition tools → definition disallowedTools → 权限模式只读上限。最终 Selection 被 AgentRequest 固定，子 Agent 无法在后续轮次放宽。

### TaskManager 与通知

TaskManager 使用有界虚拟线程执行池和有界任务表。同步运行注册为 foreground；ESC 调用 `adoptRunning(taskId)` 后立刻把 ToolResult 转为“已转后台”，原 Future 继续运行。完成时写入一次 TaskNotificationQueue；关闭时取消全部任务并等待有界时间。

### Trace 与命令/UI

TraceRegistry 在 Dispatcher 创建子 span，AgentEvent 中的 usage 累加到 span，终态原子落盘。`/tasks`、`/task info`、`/task cancel` 经 CommandServices 访问 TaskManager；ConversationLoop 在安全点 drain 通知并注入父 Agent reminder。

## 模块交互

```text
父 Agent 调用 agent 工具
  -> SubagentRequestParser
  -> subagent_type? DefinitionContext : ForkContext
  -> ModelResolver + ToolFilterPipeline
  -> TraceRegistry.startChild
  -> run_in_background?
       yes -> TaskManager.submit -> 返回 taskId
       no  -> RunToCompletion -> 返回最终文本
  -> 完成/失败 -> Trace finish
  -> 后台完成 -> task-notification -> UI + 父 Agent 下一请求
```

## 文件组织

```text
src/main/java/io/imiocode/subagent/
├── definition/    # definition、parser、loader、snapshot、source
├── model/         # model override/alias resolver、client factory
├── context/       # fork/definition context builders
├── filter/        # ToolFilterPipeline 与固定策略
├── runtime/       # dispatcher、RunToCompletion、AgentTool
├── task/          # TaskManager、record、notification queue
├── trace/         # TraceRegistry、records、token accumulator
└── command/       # tasks/task commands

src/main/resources/agents/
├── index.txt
├── explore.md
├── plan.md
└── general-purpose.md
```

现有 Agent/AgentRequest 增加固定 ToolSelection 与初始上下文入口；应用装配、ConversationCoordinator、ConversationLoop、TerminalUi、CommandServices 和 Skill Fork 适配统一运行平台。

## 技术决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 公开内置 Agent | 3 个，不含 verification | 以用户文字需求为权威范围 |
| haiku | 逻辑别名，缺省回退父模型 | 兼容 DeepSeek/OpenAI/Anthropic 且开箱可用 |
| 客户端隔离 | 每个子任务独立 LlmClient | 背景并发时取消和活动请求互不影响 |
| Prompt Cache | 父 history byte-exact 前缀 + 稳定尾部 | 不依赖共享 Java client，依赖 Provider 缓存语义 |
| 后台权限 ASK | 自动拒绝 | 后台不得阻塞等待不可见 UI |
| 工具策略 | deny 优先、固定 Selection | Schema 与执行双层一致，禁止运行期放宽 |
| worktree | 显式拒绝 | 避免在没有真实隔离时制造安全错觉 |
| Trace/Task 存储 | 当前进程内存、有界 | 符合本章不持久化边界 |
