# 精简终端 UI Tasks

## 文件清单

| 操作 | 文件 | 职责 |
|------|------|------|
| 新建 | `src/main/java/io/imiocode/config/UiVerbosity.java` | compact/verbose 枚举与校验 |
| 新建 | `src/main/java/io/imiocode/config/UiConfig.java` | UI 配置值对象 |
| 修改 | `src/main/java/io/imiocode/config/ConfigDocument.java` | 增加 `ui` YAML 文档 |
| 修改 | `src/main/java/io/imiocode/config/ConfigLoader.java` | 加载 `ui.verbosity` |
| 修改 | `src/main/java/io/imiocode/config/AppConfig.java` | 携带 `UiConfig` 并保持构造兼容 |
| 新建 | `src/main/java/io/imiocode/terminal/UiDisplayPolicy.java` | compact/verbose 可见性策略 |
| 修改 | `src/main/java/io/imiocode/terminal/TerminalUi.java` | 模式读取、切换与提示接口 |
| 修改 | `src/main/java/io/imiocode/terminal/TerminalLayout.java` | 原响应式启动面板、精简提示符和状态布局 |
| 修改 | `src/main/java/io/imiocode/terminal/ToolSummaryFormatter.java` | 工具完成态单行摘要 |
| 修改 | `src/main/java/io/imiocode/terminal/JLineTerminalUi.java` | 应用策略并渲染两种模式 |
| 修改 | `src/main/java/io/imiocode/conversation/ConversationLoop.java` | `/verbose`、`/compact-ui` 命令 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 将配置模式传入终端 |
| 修改 | `src/test/java/io/imiocode/config/ConfigLoaderTest.java` | UI 配置加载与默认值测试 |
| 修改 | `src/test/java/io/imiocode/config/YamlConfigLoaderTest.java` | UI YAML 严格解析测试 |
| 新建 | `src/test/java/io/imiocode/terminal/UiDisplayPolicyTest.java` | 可见性决策测试 |
| 修改 | `src/test/java/io/imiocode/terminal/TerminalLayoutTest.java` | 两种模式与宽度测试 |
| 修改 | `src/test/java/io/imiocode/terminal/ToolSummaryFormatterTest.java` | 精简摘要与脱敏测试 |
| 修改 | `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java` | compact/verbose 输出测试 |
| 修改 | `src/test/java/io/imiocode/conversation/ConversationLoopTest.java` | 本地命令及真实进程输出测试 |
| 修改 | `config.example.yaml` | 默认 compact 示例 |
| 修改 | `README.md` | UI 模式和命令说明 |
| 新建 | `docs/ui-compact/checklist.md` | 行为验收清单 |
| 新建 | `docs/ui-compact/acceptance-report.md` | 实际验收证据 |

## 本次增量修订：恢复启动面板

> T1-T17 已在上一轮完成。本次只执行 T18-T21，其他实现保持不变。

## T18：恢复 TerminalLayout 启动入口

**文件：**

- `src/main/java/io/imiocode/terminal/TerminalLayout.java`
- `src/test/java/io/imiocode/terminal/TerminalLayoutTest.java`

**依赖：** 已完成的 T5

**步骤：**

1. 让四参数 `welcome` 直接生成原 FULL/COMPACT/PLAIN 响应式面板。
2. 移除带 `UiVerbosity` 的 `welcome` 重载和仅三行的 `compactWelcome`，防止启动布局再次受详细度影响。
3. 保持 `inputTop`、`primaryPrompt`、`continuationPrompt` 和 `statusLine` 的 compact/verbose 分支不变。
4. 更新布局测试，断言宽富终端包含 Logo、边框、完整环境字段和 Ready，窄终端与纯文本仍安全降级。

**验证：** 运行 `mvn -Dtest=TerminalLayoutTest test`，期望所有启动行在 20、40、60、80、100、200 列均不越界。

## T19：让 JLine 始终使用完整启动面板

**文件：**

- `src/main/java/io/imiocode/terminal/JLineTerminalUi.java`
- `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java`
- `src/test/java/io/imiocode/conversation/ConversationLoopTest.java`

**依赖：** T18

**步骤：**

1. `showWelcome` 调用不带 `UiVerbosity` 的启动入口。
2. compact 与 verbose 终端测试对同一能力断言相同启动面板。
3. 真实 compact 进程断言启动面板完整，同时继续断言不显示 Thinking、Usage、LOW 和工具中间态。
4. 保持 `/verbose`、`/compact-ui`、短输入提示和事件过滤测试不变。

**验证：** 运行 `mvn -Dtest=JLineTerminalUiTest,ConversationLoopTest test`，期望启动恢复且精简对话回归通过。

## T20：同步文档与验收清单

**文件：**

- `README.md`
- `docs/ui-compact/spec.md`
- `docs/ui-compact/plan.md`
- `docs/ui-compact/task.md`
- `docs/ui-compact/checklist.md`
- `docs/ui-compact/acceptance-report.md`

**依赖：** T18、T19

**步骤：**

1. 删除“compact 使用三行精简启动区”的旧说明。
2. 说明两种详细度使用同一原响应式启动面板，模式切换只影响后续对话。
3. 更新 checklist 的启动快照、宽度、真实进程和“不影响其他行为”条目。
4. 验收报告记录实际启动输出、测试数量与 tmux/真实进程结果。

**验证：** 搜索旧启动描述应为 0 命中，文档明确包含 Logo、Ready 和“对话保持精简”。

## T21：全量验收与隔离提交

**文件：** 本次增量变更文件

**依赖：** T18-T20

**步骤：**

1. 使用 JDK 21 运行 `mvn clean package`。
2. 启动真实 shaded JAR，观察恢复后的启动面板并正常退出。
3. 运行 `git diff --check` 和敏感信息模式扫描。
4. 只暂存本次增量文件，排除 `claude.md`、`hello.txt` 和本地 `config.yaml`，提交到当前本地分支但不推送。

**验证：** BUILD SUCCESS、0 failures、0 errors；真实进程退出码 0；最终工作树只保留用户原有文件。

## T1：定义 UI 配置类型

**文件：**

- `src/main/java/io/imiocode/config/UiVerbosity.java`
- `src/main/java/io/imiocode/config/UiConfig.java`

**依赖：** 无

**步骤：**

1. 定义 `COMPACT`、`VERBOSE`。
2. 实现大小写不敏感解析，空值返回 compact。
3. 非法值抛出指向 `ui.verbosity` 的安全配置错误。
4. 定义不可变 `UiConfig` 和 compact 默认值。

**验证：** 设置 JDK 21 后运行 `mvn -DskipTests compile`，期望编译通过。

## T2：接入统一 YAML 与 AppConfig

**文件：**

- `src/main/java/io/imiocode/config/ConfigDocument.java`
- `src/main/java/io/imiocode/config/ConfigLoader.java`
- `src/main/java/io/imiocode/config/AppConfig.java`
- `src/test/java/io/imiocode/config/ConfigLoaderTest.java`
- `src/test/java/io/imiocode/config/YamlConfigLoaderTest.java`

**依赖：** T1

**步骤：**

1. 根文档增加可选 `ui` 和 `verbosity` 字段。
2. `ConfigLoader` 生成 `UiConfig` 并放入 `AppConfig`。
3. 更新规范构造器和 `toString`，保持密钥脱敏。
4. 保留所有旧构造器，并自动追加 compact 默认值。
5. 测试缺失、compact、verbose、大小写和非法配置。

**验证：** 运行 `mvn -Dtest=ConfigLoaderTest,YamlConfigLoaderTest test`，期望 0 failures、0 errors。

## T3：实现纯展示策略

**文件：**

- `src/main/java/io/imiocode/terminal/UiDisplayPolicy.java`
- `src/test/java/io/imiocode/terminal/UiDisplayPolicyTest.java`

**依赖：** T1

**步骤：**

1. 为状态、Thinking、Usage、工具状态、MCP 事件和上下文事件定义可见性方法。
2. compact 只允许完成态工具、MCP 拒绝/失败和重要上下文结果。
3. verbose 对所有现有事件返回可见。
4. 权限与通用错误不进入可隐藏策略。
5. 用参数化或完整枚举测试覆盖所有状态值。

**验证：** 运行 `mvn -Dtest=UiDisplayPolicyTest test`，期望全部通过。

## T4：扩展 TerminalUi 模式接口

**文件：** `src/main/java/io/imiocode/terminal/TerminalUi.java`

**依赖：** T1

**步骤：**

1. 增加读取当前详细度、设置详细度和显示切换确认的默认方法。
2. 默认实现不破坏现有测试终端和其他调用方编译。
3. 默认确认文本保持短小且不包含状态正文。

**验证：** 运行 `mvn -DskipTests compile`，期望全部调用方编译通过。

## T5：实现 compact 响应式布局

**文件：**

- `src/main/java/io/imiocode/terminal/TerminalLayout.java`
- `src/test/java/io/imiocode/terminal/TerminalLayoutTest.java`

**依赖：** T1

**步骤：**

1. 布局方法接收 `UiVerbosity`，保留 verbose 现有布局。
2. 启动区复用原响应式完整面板，compact 只精简启动后的输入与事件输出。
3. compact 富终端提示符使用 `› `，纯文本使用 `> `。
4. compact 不生成输入顶部边框和滚动状态栏。
5. 覆盖 FULL/COMPACT/PLAIN 终端能力与 20、40、60、100 列宽度。

**验证：** 运行 `mvn -Dtest=TerminalLayoutTest test`，期望所有行列宽不越界。

## T6：实现工具易读名称与耗时格式

**文件：**

- `src/main/java/io/imiocode/terminal/ToolSummaryFormatter.java`
- `src/test/java/io/imiocode/terminal/ToolSummaryFormatterTest.java`

**依赖：** 无

**步骤：**

1. 为六个内置工具定义 Read/Write/Edit/Bash/Glob/Grep 易读名称。
2. MCP 和未知工具名经过脱敏与安全化，参数默认隐藏。
3. 统一格式化零值、毫秒和秒级耗时。
4. 结果计数复用当前安全统计，错误只取第一行。

**验证：** 运行 `mvn -Dtest=ToolSummaryFormatterTest test`，期望名称、耗时、计数和脱敏断言通过。

## T7：实现 compact 工具完成摘要

**文件：**

- `src/main/java/io/imiocode/terminal/ToolSummaryFormatter.java`
- `src/test/java/io/imiocode/terminal/ToolSummaryFormatterTest.java`

**依赖：** T6

**步骤：**

1. 为成功事件生成“名称 + 核心目标/结果数 + 耗时”。
2. 为失败事件追加安全首行错误和耗时。
3. 文件工具显示路径，Bash 显示命令，Glob/Grep 显示 pattern 与结果数。
4. 不输出写入正文、完整结果、风险、调用 ID或未知工具参数。
5. 确保摘要无换行、最大字符限制且不切断代理字符。

**验证：** 运行 `mvn -Dtest=ToolSummaryFormatterTest test`，期望六个内置工具、MCP、成功和失败用例全部通过。

## T8：为 JLineTerminalUi 接入显示模式

**文件：**

- `src/main/java/io/imiocode/terminal/JLineTerminalUi.java`
- `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java`

**依赖：** T3、T4、T5、T7

**步骤：**

1. 构造器接收初始 `UiVerbosity`，原子保存当前值。
2. compact/verbose 分别调用对应布局。
3. 实现模式读取、切换和单行确认。
4. 保持关闭、中断、终端能力选择和脱敏逻辑不变。
5. 把旧详细 UI 测试改为显式 verbose，并增加默认 compact 测试。

**验证：** 运行 `mvn -Dtest=JLineTerminalUiTest test`，期望两种模式均通过。

## T9：过滤状态、Thinking 与 Usage

**文件：**

- `src/main/java/io/imiocode/terminal/JLineTerminalUi.java`
- `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java`

**依赖：** T8

**步骤：**

1. `updateState` 在 compact 只更新状态，不打印。
2. Thinking 三个方法在 compact 不写输出、不缓存正文。
3. `showUsage` 在 compact 不写输出。
4. verbose 保持当前输出顺序和样式。
5. 测试从 compact 切到 verbose 只展示后续事件。

**验证：** 运行 `mvn -Dtest=JLineTerminalUiTest test`，期望 compact 输出不含测试 Thinking、Usage 或状态文本。

## T10：过滤工具、MCP 与上下文事件

**文件：**

- `src/main/java/io/imiocode/terminal/JLineTerminalUi.java`
- `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java`

**依赖：** T8、T9

**步骤：**

1. compact 工具只处理 SUCCEEDED/FAILED，使用 `[ok]/[fail]` 或图标输出一行。
2. compact MCP 隐藏例行事件，保留 DENIED、SERVER_FAILED 和确认面板。
3. compact 上下文隐藏 Started，保留 Completed、Failed、CircuitOpened、ResultsOffloaded。
4. 权限确认、权限结果、错误、重试和 Agent 停止始终显示。
5. 测试同一工具完整生命周期只输出一条完成摘要。

**验证：** 运行 `mvn -Dtest=JLineTerminalUiTest,JLinePermissionPromptTest test`，期望事件过滤与强制可见项全部通过。

## T11：实现本地模式切换命令

**文件：**

- `src/main/java/io/imiocode/conversation/ConversationLoop.java`
- `src/test/java/io/imiocode/conversation/ConversationLoopTest.java`

**依赖：** T4

**步骤：**

1. 在普通消息之前识别 `/verbose`、`/compact-ui`。
2. 设置终端详细度并显示一条确认。
3. 保持 `/compact`、`/plan`、`/do` 和退出命令优先级清晰。
4. 测试两个命令不增加 LLM 调用、不进入历史、不改变 AgentMode。

**验证：** 运行 `mvn -Dtest=ConversationLoopTest test`，期望命令与现有会话测试全部通过。

## T12：接入应用启动与配置示例

**文件：**

- `src/main/java/io/imiocode/ImioCodeApplication.java`
- `config.example.yaml`
- 本地忽略的 `config.yaml`

**依赖：** T2、T8

**步骤：**

1. 应用创建终端时传入 `config.ui().verbosity()`。
2. compact 启动帮助合并为一条并包含 `/verbose` 发现提示。
3. 示例与当前本地配置增加 `ui.verbosity: compact`。
4. 不读取或输出本地 API Key；本地配置继续使用环境变量引用。

**验证：** 设置测试环境变量启动 shaded JAR，期望默认进入 compact 且配置来源仍为统一配置。

## T13：更新真实进程与回归测试

**文件：**

- `src/test/java/io/imiocode/conversation/ConversationLoopTest.java`
- 受 AppConfig 规范构造器影响的测试文件

**依赖：** T9-T12

**步骤：**

1. 真实进程默认 compact 用例断言不含 Thinking、Usage、LOW 和中间状态。
2. 增加 verbose 真实进程用例或为原详细用例写入 `ui.verbosity: verbose`。
3. 两种模式均断言最终回答、工具结果、错误和历史协议内容正确。
4. 修复所有 AppConfig 构造兼容回归。

**验证：** 运行 `mvn -Dtest=ConversationLoopTest,JLineTerminalUiTest,ConfigLoaderTest test`，期望 0 failures、0 errors。

## T14：更新文档

**文件：**

- `README.md`
- `config.example.yaml`
- `docs/config-unification/migration.md`

**依赖：** T12

**步骤：**

1. 说明 compact 是默认值、verbose 用于调试。
2. 记录 `ui.verbosity`、`/verbose`、`/compact-ui`。
3. 提供不含真实凭据的完整配置片段。
4. 明确最终回答不会被 UI 截断，权限和错误不会隐藏。

**验证：** 搜索三种配置/命令名称，期望文档与示例均覆盖且无真实密钥。

## T15：执行聚焦测试与全量构建

**文件：** 整个项目

**依赖：** T1-T14

**步骤：**

1. 运行配置、策略、布局、格式化、终端和会话聚焦测试。
2. 使用 JDK 21 运行 `mvn clean package`。
3. 记录测试总数、failures、errors、skipped 和构建警告。
4. 运行 `git diff --check` 与敏感信息模式扫描。

**验证：** BUILD SUCCESS、0 failures、0 errors，shaded JAR 存在。

## T16：执行真实终端验收

**文件：**

- `docs/ui-compact/checklist.md`
- `docs/ui-compact/acceptance-report.md`

**依赖：** T15、已批准的 checklist

**步骤：**

1. 优先在 tmux 启动真实 shaded JAR，输入会触发工具的真实请求。
2. 观察 compact 中无 Thinking、Usage、中间状态和风险标签，工具只有完成行，最终回答完整。
3. 输入 `/verbose` 后执行下一轮，观察详细输出恢复；再用 `/compact-ui` 切回。
4. 如果 tmux 不可用，如实记录阻塞，并使用真实 Java 子进程与可控 LLM/MCP 服务完成等价验收。
5. 按 checklist 逐项写实际证据。

**验证：** 验收报告包含两种模式输出断言、真实进程退出码和所有阻塞项。

## T17：分组提交与最终检查

**文件：** 本次变更文件

**依赖：** T16

**步骤：**

1. 按“配置与策略”“终端与命令”“文档与验收”分组提交。
2. 每次只暂存本次文件，排除 `claude.md`、`hello.txt` 和忽略的本地 `config.yaml`。
3. 检查暂存名单、最终工作树和最近提交。
4. 除非用户另行要求，只提交到本地，不推送远端。

**验证：** 最终 `git status --short` 只显示用户原有改动，提交历史包含本次分组提交。

## 执行顺序

```text
T1 --> T2 ------------------------------┐
  └--> T3 --> T8 --> T9 --> T10 --------┤
  └--> T4 --------------------> T11 ----┤
       T5 --------┐                     ├--> T13 --> T15 --> T16 --> T17
       T6 --> T7 -┴--> T8               │
T2 + T8 ----------------------> T12 --> T14

已完成 T1-T17 --> T18 --> T19 --> T20 --> T21
```
