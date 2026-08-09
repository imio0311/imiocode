# CH12 Hook 系统 Tasks

## 文件清单

| 操作 | 文件/目录 | 职责 |
|---|---|---|
| 新建 | `src/main/java/io/imiocode/hook/` | Hook 领域模型、Runtime、Engine、结果与通知 |
| 新建 | `src/main/java/io/imiocode/hook/condition/` | 条件解析、求值、glob |
| 新建 | `src/main/java/io/imiocode/hook/template/` | 上下文变量单次替换 |
| 新建 | `src/main/java/io/imiocode/hook/action/` | 四类动作及进程/HTTP 抽象 |
| 新建 | `src/main/java/io/imiocode/hook/config/` | YAML 映射、校验、错误汇总 |
| 新建 | `src/main/java/io/imiocode/hook/integration/` | 上下文工厂与工具监听适配器 |
| 修改 | `src/main/java/io/imiocode/config/ConfigDocument.java` | 接收根 hooks 列表 |
| 修改 | `src/main/java/io/imiocode/config/ConfigLoader.java` | 解析 Hook 并加入 RuntimeConfig |
| 修改 | `src/main/java/io/imiocode/config/RuntimeConfig.java` | 暴露 Hook 加载结果 |
| 修改 | `src/main/java/io/imiocode/agent/Agent.java` | turn/error/compact 生命周期 |
| 修改 | `src/main/java/io/imiocode/agent/StreamingTurnExecutor.java` | pre_send/post_receive 与 prompt drain |
| 修改 | `src/main/java/io/imiocode/agent/StreamingToolScheduler.java` | pre/post tool、permission_request |
| 新建 | `src/main/java/io/imiocode/tool/ToolLifecycleListener.java` | 文件/命令中性监听接口 |
| 修改 | `src/main/java/io/imiocode/tool/core/{WriteFileTool,EditFileTool,BashTool}.java` | file_change / command_execute 真实触发点 |
| 修改 | `src/main/java/io/imiocode/runtime/ConversationCoordinator.java` | session 生命周期与通知 drain |
| 修改 | `src/main/java/io/imiocode/runtime/ConversationLoop.java` | UI 安全点回收通知 |
| 修改 | `src/main/java/io/imiocode/tui/{TerminalUi,JLineTerminalUi}.java` | Hook 通知渲染 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 装配与 startup/shutdown |
| 修改 | `config.example.yaml`、`README.md` | Hook 配置示例与使用文档 |
| 新建/修改 | `src/test/java/io/imiocode/hook/**` 及集成测试 | 核心、动作、配置、生命周期验证 |

## T1：事件、动作与 Hook 领域模型

**文件：** `hook/HookEvent.java`、`Hook.java`、`HookFailurePolicy.java`、`hook/action/*.java`
**依赖：** 无
**步骤：**
1. 定义 15 个事件及严格 configName 解析。
2. 定义四类不可变 action 和 action type。
3. 定义 Hook 字段、默认值和基础构造不变量。

**验证：** 运行领域模型测试；15 个合法事件往返一致，未知值失败。

## T2：上下文、结果、拒绝与通知模型

**文件：** `hook/HookContext.java`、结果/记录/通知/Error 文件
**依赖：** T1
**步骤：**
1. 实现 Builder、不可变 Map、嵌套 args 字段解析。
2. 定义执行状态、动作结果、执行记录、普通/前置结果。
3. 实现无可写堆栈的 ToolRejectedError 和安全消息。

**验证：** 运行 HookContext/ToolRejectedError 测试，嵌套参数与缺失值符合设计。

## T3：条件解析

**文件：** `hook/condition/Condition*.java`、`DefaultConditionParser.java`
**依赖：** T2
**步骤：**
1. 实现 quote-aware 连接符与操作符扫描。
2. 支持单/双引号和转义，禁止 &&/|| 混用。
3. 在解析期校验字段、正则和 glob。

**验证：** 条件解析测试覆盖四操作符、引号、转义和所有非法输入。

## T4：条件求值与跨平台 glob

**文件：** `HookGlobPattern.java`、`ConditionEvaluator.java`、`DefaultConditionEvaluator.java`
**依赖：** T3
**步骤：**
1. 把 glob 编译为分隔符无关 matcher。
2. 实现四操作符和 AND/OR 短路。
3. 统一缺失字段为空字符串。

**验证：** Windows/Unix 路径样例及短路计数测试通过。

## T5：模板变量替换

**文件：** `hook/template/HookTemplateResolver.java`
**依赖：** T2
**步骤：**
1. 支持六类变量和嵌套 TOOL_ARGS。
2. 保证单次扫描、不递归展开。
3. 提供模板变量静态校验。

**验证：** 全变量、缺失值、恶意二次变量和未知变量测试通过。

## T6：YAML 文档与配置校验

**文件：** `hook/config/HookDocument.java`、`ActionDocument.java`、`HookValidator.java`
**依赖：** T1、T3、T5
**步骤：**
1. 定义 Jackson 原始记录和字段名称。
2. 汇总 id/event/action/timeout/URI/条件错误。
3. 校验 reject/on-error/async 交叉约束。

**验证：** 一个 YAML 同时包含多类错误时一次返回全部带索引诊断。

## T7：配置映射与整体降级

**文件：** `HookConfigMapper.java`、`HookConfigLoadResult.java`、`HookConfigError.java`
**依赖：** T6
**步骤：**
1. 解析 `${NAME}` 后映射领域对象和默认值。
2. 条件启动期预编译。
3. 任一错误时返回空 Hook 集，合法时保持声明顺序。

**验证：** 合法四动作 YAML 映射通过；非法配置无部分加载。

## T8：动作分派、Prompt 与 Agent 占位

**文件：** `HookActionExecutor.java`、`ActionDispatcher.java`、`PromptHookExecutor.java`、`AgentPlaceholderHookExecutor.java`
**依赖：** T2、T5
**步骤：**
1. 建立严格 action type 到 executor 映射。
2. Prompt 完成模板展开并输出 reminder 候选。
3. Agent 固定返回 NOT_IMPLEMENTED，不调用 LLM。

**验证：** 分派、Prompt 和占位执行器单测通过。

## T9：Command 执行器

**文件：** `HookProcessRunner.java`、`JdkHookProcessRunner.java`、`CommandHookExecutor.java`
**依赖：** T5
**步骤：**
1. 选择平台 shell，固定 workspace 和最小环境。
2. 并行有界读取 stdout/stderr。
3. 实现超时、取消和进程树终止。

**验证：** 成功、非零、环境、截断、超时子进程测试通过且无遗留进程。

## T10：HTTP 执行器

**文件：** `HookHttpTransport.java`、`JdkHookHttpTransport.java`、`HttpHookExecutor.java`
**依赖：** T5
**步骤：**
1. 模板化 URL/header/body，缺省 body 序列化安全上下文。
2. 禁用重定向并限制响应 1 MiB。
3. 区分 2xx、非 2xx、超时、网络和超限失败。

**验证：** 使用本地 HTTP Server 覆盖成功、头/body、超时、非 2xx 和过大响应。

## T11：Prompt/通知有界队列

**文件：** `HookPromptInbox.java`、`HookNotificationQueue.java`
**依赖：** T2
**步骤：**
1. 实现线程安全 offer/drain/clear。
2. 满容量时丢最旧项并产生一次汇总告警。
3. 保证 drain 后不重复。

**验证：** 容量、顺序、并发 drain 测试通过。

## T12：DefaultHookEngine 普通调度

**文件：** `HookRuntime.java`、`DefaultHookEngine.java`、`HookExecutionException.java`
**依赖：** T4、T8–T11
**步骤：**
1. 按事件索引并实现 runHooks 匹配、once 和同步执行。
2. 建立有界 async 执行器和完整通知。
3. 实现 ignore/fail、prompt inbox、关闭和 NOOP。

**验证：** 顺序、once 并发、async 非阻塞、失败隔离和 close 测试通过。

## T13：DefaultHookEngine 工具拦截

**文件：** 同 T12
**依赖：** T12
**步骤：**
1. 实现只接受 PRE_TOOL_USE 的同步入口。
2. 支持显式 reject、on-error reject、ignore/fail short-circuit。
3. 生成稳定脱敏的 ToolRejectedError。

**验证：** 各拒绝路径、顺序停止和非 pre-tool 调用防误用测试通过。

## T14：主配置系统集成

**文件：** `ConfigDocument.java`、`ConfigLoader.java`、`RuntimeConfig.java`
**依赖：** T7
**步骤：**
1. 增加根 hooks 列表字段。
2. 解析并把 load result 放入 RuntimeConfig。
3. 保持无 hooks 与旧 config 行为兼容。

**验证：** 配置集成测试覆盖缺省、合法、非法整体降级。

## T15：文件与命令生命周期监听

**文件：** `ToolLifecycleListener.java`、三个核心工具、Hook adapter
**依赖：** T12
**步骤：**
1. 为旧构造器提供 NOOP 委托。
2. 写/编辑成功后通知规范化路径。
3. Bash 进程启动后通知脱敏命令，Hook command 不递归。

**验证：** 成功恰好一次、失败零次、已有工具测试全部通过。

## T16：流式工具拦截与权限事件

**文件：** `StreamingToolScheduler.java`、`StreamingTurnExecutor.java`
**依赖：** T13
**步骤：**
1. 缓存每 index 的 pre-hook 结果并置于权限前。
2. 拒绝转 ToolResult，保持 Agent 后续迭代。
3. 工具真实执行后发 post-tool；仅 ASK 发 permission_request。

**验证：** eager、串行、并行、拒绝、ASK/ALLOW/DENY 集成测试通过。

## T17：模型请求与 Agent 生命周期

**文件：** `Agent.java`、`StreamingTurnExecutor.java`
**依赖：** T12、T16
**步骤：**
1. 接入 turn_start/turn_end/error。
2. 每个 Provider attempt 接入 pre_send/post_receive 并 drain prompt。
3. 自动/恢复/手动压缩成功时接入 compact。

**验证：** 正常、重试、失败、压缩和 prompt 当前/下一请求测试通过。

## T18：会话、应用和 Skill 生命周期

**文件：** `ConversationCoordinator.java`、`ImioCodeApplication.java`
**依赖：** T14、T17
**步骤：**
1. 接入 startup/shutdown 与安全关闭顺序。
2. 接入初始/new/resume/close 的 session start/end 并清 inbox。
3. 主 Agent、inline/fork Skill 共享 runtime，只有应用拥有关闭权。

**验证：** 会话切换、重复 close、fork 共享 once 和应用关闭测试通过。

## T19：UI 通知集成

**文件：** `TerminalUi.java`、`JLineTerminalUi.java`、`ConversationLoop.java`、`ConversationCoordinator.java`
**依赖：** T18
**步骤：**
1. 增加 Hook 通知渲染 API。
2. 在主线程安全点 drain。
3. compact 只显示关键状态，verbose 显示全部且统一脱敏。

**验证：** UI fake 测试确认通知顺序、过滤和不重复。

## T20：配置示例与文档

**文件：** `config.example.yaml`、`README.md`
**依赖：** T14–T19
**步骤：**
1. 添加 command/prompt/http/reject 示例，默认保持 hooks 为空或安全示例注释。
2. 说明事件、条件、变量、失败策略和重启要求。
3. 说明 agent 占位与安全边界。

**验证：** 示例 YAML 可由真实 ConfigLoader 加载。

## T21：完整自动化与端到端验收

**文件：** 所有测试、`docs/ch12/checklist.md`
**依赖：** T1–T20
**步骤：**
1. 运行定向 Hook 测试和 Maven 全量测试。
2. 构建 fat JAR。
3. 在 tmux；若 Windows 无 tmux，则用可交互子进程等价方案启动 ImioCode，执行真实 prompt、格式化、拒绝和失败隔离场景。
4. 按 checklist 逐项记录实际证据并修复所有失败。

**验证：** checklist 全部勾选、进程正常退出、无遗留子进程。

## 执行顺序

```text
T1 → T2 → T3 → T4 → T5
              ↘ T6 → T7 → T14
T5 → T8 ─┐
T5 → T9  ├→ T12 → T13 → T16 → T17 → T18 → T19 → T20 → T21
T5 → T10 │
T2 → T11 ┘
T12 → T15 ────────────────────────┘
```

