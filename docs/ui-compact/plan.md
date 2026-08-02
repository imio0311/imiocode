# 精简终端 UI Plan

## 架构概览

本次优化只改变终端展示层和本地命令路由，不修改 Agent 事件生产、工具调度、权限决策、Provider 消息或对话历史。

整体流程调整为：

```text
config.yaml ui.verbosity
        ↓
UiConfig / UiVerbosity
        ↓
ImioCodeApplication 创建 JLineTerminalUi
        ├─ 启动 → TerminalLayout.welcome(TerminalMode)
        │          └─ 始终渲染原响应式完整面板
        └─ 对话 → ConversationLoop 继续转发全部事件
                   ↓
             JLineTerminalUi + UiDisplayPolicy
             ├─ compact：过滤过程噪声，输出完成摘要
             └─ verbose：保持当前详细输出
```

`ConversationLoop` 不删除 Thinking、Usage 或工具事件，只负责识别 `/verbose`、`/compact-ui` 并切换终端策略。所有事件仍到达 `TerminalUi`，由展示策略决定是否打印。这保证切换显示模式不会改变 Agent 行为，也使权限、错误和安全提醒始终经过原路径。

终端能力与显示详细度保持两个正交维度：

- `TerminalMode`：根据 ANSI 能力和宽度选择 FULL / COMPACT / PLAIN 布局能力。
- `UiVerbosity`：根据配置或命令选择 COMPACT / VERBOSE 信息密度。

启动面板只使用 `TerminalMode`，不再读取 `UiVerbosity`。输入区、状态栏、Thinking、Usage、工具、MCP 和上下文事件继续由两个维度共同决定。这样可以恢复原启动视觉，又不会扩大 compact 模式的后续输出。

## 核心数据结构

### `UiVerbosity`

```java
public enum UiVerbosity {
    COMPACT,
    VERBOSE;

    public static UiVerbosity parse(String value);
}
```

- `null` 或空值解析为 `COMPACT`。
- 接受大小写不敏感的 `compact`、`verbose`。
- 其他值抛出指向 `ui.verbosity` 的 `ConfigException`。

### `UiConfig`

```java
public record UiConfig(UiVerbosity verbosity) {
    public static UiConfig defaults();
}
```

作为 `AppConfig` 的新增字段参与统一配置装配。现有兼容构造器继续存在，并自动补 `UiConfig.defaults()`。

### `ConfigDocument.UiDocument`

```java
record UiDocument(String verbosity) {}
```

根 YAML 新增可选 `ui` 字段：

```yaml
ui:
  verbosity: compact
```

### `UiDisplayPolicy`

```java
final class UiDisplayPolicy {
    boolean showStateTransitions();
    boolean showThinking();
    boolean showUsage();
    boolean showToolEvent(ToolExecutionState state);
    boolean showMcpEvent(McpEventType type);
    boolean showContextEvent(ContextEvent event);
}
```

该类只根据 `UiVerbosity` 返回展示决策，不执行 I/O，也不保存事件正文。权限确认、错误和用户选择结果不经过可隐藏策略。

### `TerminalUi` 模式接口

```java
default UiVerbosity verbosity();
default void setVerbosity(UiVerbosity verbosity);
default void showVerbosityChanged(UiVerbosity verbosity);
```

默认实现保持兼容：非 JLine 测试终端可以继续编译。`JLineTerminalUi` 用 `AtomicReference<UiVerbosity>` 保存当前会话模式，切换只影响后续事件。

本次修订不新增核心数据结构。`UiVerbosity` 仍用于对话详细度，但不再传入启动面板入口。

## 模块设计

### 配置模块

**职责：** 加载、校验和传递默认 UI 详细度。

**修改：**

- `ConfigDocument` 增加 `ui` 文档。
- `ConfigLoader` 将其转换为 `UiConfig`。
- `AppConfig` 增加 `ui` 字段并保持已有构造器兼容。
- `ImioCodeApplication` 使用 `config.ui().verbosity()` 创建终端。

**错误语义：** 非法值在任何终端和 LLM 初始化前以安全配置错误退出。

### 展示策略模块

**职责：** 集中定义 compact 与 verbose 的事件可见性。

compact 策略：

| 信息 | 行为 |
|------|------|
| 状态变化 | 只更新 `UiState`，不打印 |
| Thinking | 全部隐藏 |
| Usage | 隐藏 |
| 工具 queued/running | 隐藏 |
| 工具 succeeded/failed | 显示一条 |
| MCP routine lifecycle | 隐藏 |
| MCP denied/failed | 显示 |
| 自动压缩 started | 隐藏 |
| 自动压缩 completed/failed/circuit/offloaded | 显示摘要 |
| 权限、错误、重试、停止 | 始终显示 |

verbose 策略复用当前全部展示行为。

### 响应式布局模块

**职责：** 启动区只根据 `TerminalMode` 生成原响应式完整面板；输入提示和状态区域继续根据 `TerminalMode + UiVerbosity` 生成。

启动入口固定为：

```java
List<String> welcome(
        UiContext context,
        UiState state,
        int requestedWidth,
        TerminalMode mode);
```

- FULL：输出 Logo、边框、产品版本、Provider、模型、目录和状态。
- COMPACT：保留原窄富终端边框及环境字段，省略 Logo。
- PLAIN：输出无 ANSI 的产品版本、Provider/模型、目录和状态多行摘要。
- `JLineTerminalUi.showWelcome` 只调用上述入口，不读取当前 `verbosity`。

compact 对话仍使用短 `› `（纯文本回退为 `> `），不打印输入顶部边框和每次 readLine 后的状态栏；verbose 对话保持现有输入面板、主提示符和状态栏。所有布局继续使用 JLine 列宽计算与 `TerminalLayout.truncate`。

### Thinking 与状态过滤

**职责：** 在 `JLineTerminalUi` 的现有方法入口安全跳过隐藏内容。

- `updateState` 始终更新原子状态；compact 立即返回，不写滚动区。
- `beginThinking`、`appendThinkingText`、`endThinking` 在 compact 不创建输出行。
- `showUsage` 在 compact 直接返回。
- 模式从 compact 切到 verbose 后只展示新事件，不保存或回放旧内容。
- 回答、工具、错误到来时继续调用现有“结束打开行”逻辑，避免换行粘连。

### 工具摘要模块

**职责：** 为 compact 生成稳定、安全的单行工具完成摘要。

`ToolSummaryFormatter` 增加：

```java
String compactSummary(ToolExecutionEvent event);
String displayName(ToolCall call);
```

映射规则：

| 工具 | 易读名称 | 主要信息 |
|------|----------|----------|
| `read_file` | Read | path |
| `write_file` | Write | path |
| `edit_file` | Edit | path |
| `bash` | Bash | command |
| `glob` | Glob | pattern + 结果数 |
| `grep` | Grep | pattern + 结果数 |
| MCP/未知 | 安全化工具名 | 参数隐藏 |

成功格式：

```text
✓ Read pom.xml (0.1s)
✓ Glob *.java · 12 results (0.0s)
```

失败格式：

```text
✗ Bash mvn test · 命令执行失败 (1.2s)
```

- 富终端使用 `✓` / `✗` 与颜色；纯文本使用 `[ok]` / `[fail]`。
- 耗时从完成事件的 `ToolResult.duration` 获取，零值仍可稳定显示 `0.0s`。
- 首行错误、名称、目标和整行都经过 `SecretRedactor`。
- 最终按终端列宽截断，不能截断成半个代理字符。

### 回答与输入区

**职责：** 保持最终回答完整，同时缩短视觉标记。

- compact 助手前缀使用 `› `，plain 模式使用 `> `。
- verbose 保持 `ImioCode › ` / `ImioCode> `。
- 流式片段原样写入；不对 Markdown、代码或换行做摘要。
- `endAssistantResponse` 负责补齐一个换行；下一工具行、错误或输入提示先结束已打开行。
- compact `readLine` 不输出顶部边框和尾部状态栏；verbose 保持现状。

### 本地命令路由

**职责：** 在进入 Agent 前处理 UI 模式命令。

`ConversationLoop` 在 `/compact` 之后增加：

```text
/verbose    -> terminal.setVerbosity(VERBOSE)
/compact-ui -> terminal.setVerbosity(COMPACT)
```

- 命令只调用终端，不调用 `ConversationSession.sendWithEvents`。
- 切换后调用 `showVerbosityChanged` 输出一条确认。
- 重复切换到当前模式仍输出同一条确定性确认，不产生错误。

### MCP、权限与上下文展示

**职责：** 保留所有需要用户行动的信息。

- MCP compact 隐藏 WAITING、APPROVED、CONNECTING、CONNECTED、TOOL_DISCOVERED、CLOSED；保留 DENIED、SERVER_FAILED 和实际启动确认面板。
- 应用已有 `[MCP] 已连接 N 个 Server，注册 N 个工具` 聚合行继续显示。
- 权限确认与结果不经过过滤。
- 自动上下文 Started 隐藏；Completed、Failed、CircuitOpened、ResultsOffloaded 显示单行。
- `/compact` 的最终 `CompactReport` 始终显示。

## 模块交互

```text
ConfigLoader
   └─ UiConfig(COMPACT/VERBOSE)
          ↓
ImioCodeApplication
   └─ JLineTerminalUi(redactor, verbosity)
          ├─ showWelcome → TerminalLayout.welcome(TerminalMode)
          │                 └─ 原响应式完整启动面板
          └─ ConversationLoop
             ├─ 普通请求 → Agent/Session → 全量事件 → TerminalUi
             ├─ /verbose → setVerbosity(VERBOSE)
             └─ /compact-ui → setVerbosity(COMPACT)
                                                ↓
                                        UiDisplayPolicy
                                        ├─ 可见 → Layout/Formatter → writer
                                        └─ 隐藏 → 不执行 I/O
```

工具事件路径：

```text
QUEUED ─┐
RUNNING ├─ compact policy 丢弃显示
        │
SUCCEEDED/FAILED
        └─ ToolSummaryFormatter.compactSummary
                └─ redact → truncate → 单行输出
```

## 文件组织

```text
src/main/java/io/imiocode/
├── ImioCodeApplication.java                 — 将配置的 verbosity 传给终端
├── config/
│   ├── AppConfig.java                       — 增加 UiConfig
│   ├── ConfigDocument.java                  — 增加 ui 文档
│   ├── ConfigLoader.java                    — 解析 ui.verbosity
│   ├── UiConfig.java                        — UI 配置值对象
│   └── UiVerbosity.java                     — compact/verbose 枚举
├── conversation/
│   └── ConversationLoop.java                — /verbose、/compact-ui
└── terminal/
    ├── TerminalUi.java                      — 模式读取、切换和确认接口
    ├── JLineTerminalUi.java                 — 完整启动面板与对话策略过滤
    ├── UiDisplayPolicy.java                 — 可见性决策
    ├── TerminalLayout.java                  — 原启动面板与 compact/verbose 对话布局
    └── ToolSummaryFormatter.java            — compact 单行工具摘要

src/test/java/io/imiocode/
├── config/
│   ├── ConfigLoaderTest.java
│   └── YamlConfigLoaderTest.java
├── conversation/ConversationLoopTest.java
└── terminal/
    ├── UiDisplayPolicyTest.java
    ├── JLineTerminalUiTest.java
    ├── TerminalLayoutTest.java
    └── ToolSummaryFormatterTest.java

docs/ui-compact/
├── spec.md
├── plan.md
├── task.md
├── checklist.md
└── acceptance-report.md

config.example.yaml                         — 默认 compact 示例
README.md                                   — 模式与命令说明
```

## 技术决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 过滤位置 | `JLineTerminalUi` + 纯决策 `UiDisplayPolicy` | 全量事件仍被消费，不影响 Agent、历史和协议；策略可独立测试 |
| 模式状态 | `AtomicReference<UiVerbosity>` | 终端事件可能来自异步流，运行时切换需要安全可见 |
| 配置归属 | `AppConfig.ui` | UI 是应用级启动配置，与当前统一 `config.yaml` 结构一致 |
| 终端能力与详细度 | 保持 `TerminalMode` 和 `UiVerbosity` 分离 | 避免把“40 列紧凑布局”与“隐藏过程信息”混为一谈 |
| 启动面板维度 | 只依赖 `TerminalMode` | 恢复品牌面板的同时，确保 compact 对话过滤完全不变 |
| 启动接口 | 移除带 `UiVerbosity` 的 welcome 分支 | 从接口层防止后续再次把对话详细度误用于启动面板 |
| 工具去重 | compact 只接受完成态 | 不需要保存调用 ID 集合，天然一调用一完成行，常数状态 |
| Thinking 隐藏 | UI 方法入口直接跳过 | 不缓存正文、不回放，不改变对话历史 |
| 命令实现 | `ConversationLoop` 本地拦截 | 与 `/plan`、`/do`、`/compact` 现有模式一致，确保不进入模型 |
| Verbose 兼容 | 保留当前渲染分支 | 调试能力不退化，原测试只需显式选择 verbose |
| 回答精简边界 | 只改前缀和留白，不改正文 | 避免终端层篡改模型语义或代码内容 |
| MCP 降噪 | 隐藏 routine 事件，保留确认/失败与聚合统计 | 用户仍知道连接结果，但不被每个工具注册事件刷屏 |
