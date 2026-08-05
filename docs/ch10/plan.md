# CH10 Slash Command 内置命令框架 Plan

## 架构概览

CH10 采用渐进式重构，在 CH9 基础上形成六层结构。

### 1. 命令领域层

将现有注册中心扩展为统一命令注册中心，管理命令描述、主名、别名、查找、列举、补全和执行分派。解析器继续负责引号、转义和大小写规范化。

### 2. 命令上下文层

`CommandContext` 只持有命令服务、`UIController` 和只读命令目录，不再引用 `TerminalUi`。命令因此可以用假 UI 独立测试。

### 3. 内置命令层

迁移现有 help、compact、plan、do、session、memory，并新增 clear、permission、status、review。exit、quit、verbose、compact-ui 作为兼容命令继续注册。

### 4. 运行时状态层

新增进程级权限模式控制器，保留配置文件规则，只原子替换当前模式；权限检查器每次工具执行读取最新快照。统一状态快照汇总模型、工作目录、Agent 模式、权限模式、会话、Token 和 MCP 计数。

### 5. 终端适配层

JLine 实现 UI 控制器，负责清屏、确认、详细度切换和状态刷新；独立补全适配器把注册中心候选转换为 JLine 候选，不让命令模块依赖 JLine。

### 6. 交互循环层

输入顺序固定为“解析命令 → 执行命令 → 根据结果本地结束、退出或转交 Prompt → 普通消息进入 Agent”。状态变化后统一刷新状态栏，异常被转成安全本地错误。

## 核心数据结构

### CommandType

```java
public enum CommandType {
    LOCAL,
    UI,
    PROMPT
}
```

### CommandDescriptor

```java
public record CommandDescriptor(
        String name,
        Set<String> aliases,
        String usage,
        String description,
        CommandType type,
        boolean compatibility) {
}
```

构造时统一校验小写命令名、别名格式和非空说明。核心命令与兼容命令通过 `compatibility` 区分，供 `/help` 分组展示。

### Command

```java
public interface Command {
    CommandDescriptor descriptor();

    CommandResult execute(
            CommandContext context,
            List<String> arguments);
}
```

### CommandOutcome

```java
public enum CommandOutcome {
    HANDLED,
    FORWARD_TO_AGENT,
    EXIT_REQUESTED
}
```

### CommandResult

```java
public record CommandResult(
        CommandOutcome outcome,
        List<CommandMessage> messages,
        Optional<String> prompt) {

    public static CommandResult handled(CommandMessage... messages);

    public static CommandResult forwardToAgent(String prompt);

    public static CommandResult exit();
}
```

约束：

- `HANDLED` 和 `EXIT_REQUESTED` 不允许携带 Prompt。
- `FORWARD_TO_AGENT` 必须且只能携带一个非空 Prompt。
- `/review` 是唯一返回 `FORWARD_TO_AGENT` 的内置命令。

### CommandRegistry

```java
public final class CommandRegistry {
    public void register(Command command);

    public Optional<Command> find(String nameOrAlias);

    public List<CommandDescriptor> listCommands();

    public List<String> complete(String prefix);

    public Optional<CommandResult> dispatch(
            String input,
            CommandContext context);
}
```

主名表和别名表分开保存，查找为 O(1)。列举按主名稳定排序，补全返回带 `/` 的主名和别名。注册过程拒绝主名、别名和跨命令冲突。

### CommandContext

```java
public record CommandContext(
        CommandServices services,
        UIController ui,
        CommandRegistry commands) {
}
```

### UIController

```java
public interface UIController {
    void clearScreen();

    boolean confirm(ConfirmationPrompt prompt);

    UiVerbosity verbosity();

    void setVerbosity(UiVerbosity verbosity);

    void refreshStatus(CommandStatus status);
}
```

`UIController` 是命令可见的最小 UI 能力；JLine 只存在于它的终端实现中。

### CommandStatus

```java
public record CommandStatus(
        String provider,
        String model,
        Path workspace,
        AgentMode agentMode,
        PermissionMode permissionMode,
        SessionSummary session,
        long estimatedTokens,
        long contextWindowTokens,
        int connectedMcpServers,
        int registeredMcpTools) {
}
```

该对象只能持有已分类为可展示的运行信息，不得保存 API Key、认证 Header、环境变量值或完整配置对象。

### CommandServices 扩展

```java
public interface CommandServices {
    // 保留 CH9 的模式、压缩、会话和记忆方法
    PermissionMode permissionMode();

    void switchPermissionMode(PermissionMode mode);

    CommandStatus status();
}
```

### PermissionSettingsProvider

```java
public interface PermissionSettingsProvider {
    PermissionSettings snapshot();
}
```

### RuntimePermissionSettings

```java
public final class RuntimePermissionSettings
        implements PermissionSettingsProvider {
    public PermissionMode mode();

    public void switchMode(PermissionMode mode);

    @Override
    public PermissionSettings snapshot();
}
```

它保存启动时加载的三层规则，只原子替换当前模式；权限检查器每次检查时读取最新快照，因此不需要重建 Agent 或工具注册中心。

### 内置别名

- `/help`：`/h`、`/?`
- `/clear`：`/cls`
- `/session`：`/sessions`
- `/memory`：`/mem`
- `/permission`：`/perm`
- `/status`：`/st`
- `/review`：`/rv`
- `/quit` 继续作为 `/exit` 的兼容别名

## 模块设计

### 命令核心模块

**职责：**

- 替换 CH9 的 `LocalCommand`、`LocalCommandRegistry` 和旧结果分派模型。
- 校验描述信息、注册主名与别名。
- 提供解析、查找、列举、补全和安全分派。
- 捕获参数异常与运行时异常，转换为不含堆栈的本地错误。

**依赖：** Java 集合与命令领域类型，不依赖终端、Agent 或 JLine。

### UI 抽象模块

**职责：**

- 定义 `UIController` 和通用确认模型。
- 提供清屏、确认、详细度切换和状态刷新能力。
- `TerminalUi` 扩展该接口，现有 JLine UI 实现具体行为。
- 命令测试使用内存假 UI，无需构造终端。

### 内置命令模块

| 命令 | 类型 | 行为 |
|---|---|---|
| help | LOCAL | 从注册中心读取描述，分核心/兼容两组输出 |
| compact | UI | 调用现有强制压缩，展示 Token 变化 |
| clear | UI | 清屏；交互循环随后重绘动态状态 |
| plan | UI | 切换 Plan Mode |
| do | UI | 切换 Do Mode |
| session | LOCAL | 复用 CH9 会话管理，删除走抽象确认 |
| memory | LOCAL | 复用 CH9 记忆 CRUD |
| permission | LOCAL | 查看或切换进程级权限模式 |
| status | LOCAL | 格式化非敏感运行状态快照 |
| review | PROMPT | 返回固定审查 Prompt 和可选关注点 |
| exit/quit | UI 兼容 | 请求退出 |
| verbose/compact-ui | UI 兼容 | 修改 UI 详细度 |

`/review` 固定模板要求：

- 检查当前工作区改动及必要的相关代码。
- 优先发现正确性、安全、回归和测试遗漏。
- 先列 findings，包含严重度、文件位置、原因和建议。
- 不写文件、不执行修改命令。
- 无问题时明确说明。
- 参数非空时追加 `Additional focus` 区块。
- 不自动改变当前 Plan/Do 模式。

### 运行时权限模块

**职责：**

- 启动时保存配置文件加载出的模式和三层规则。
- 当前模式使用原子引用，切换时只改变内存值。
- 权限检查器不再持有固定设置，而是每次读取设置快照。
- 危险命令硬拦截、沙箱、规则引擎及 HITL 顺序保持不变。
- 关闭进程后不写配置，下次启动重新读取原始模式。

### 状态聚合模块

**职责：**

- 协调器聚合静态运行信息和动态会话状态。
- 近似 Token 使用现有估算规则统计当前历史，并与配置窗口比较。
- MCP 使用启动结果中的 Server 和工具计数，不重新扫描连接。
- 输出对象不持有 API Key、Header、环境变量或完整配置对象。

### JLine 补全模块

**职责：**

- 将注册中心的 `complete(prefix)` 适配为 JLine 候选。
- 仅当光标位于首个 `/命令前缀` 内时提供候选。
- 出现空格后不再补全参数。
- 单候选由 JLine 直接补齐；多候选使用稳定排序菜单。
- 保留 Alt+Enter 多行输入和现有权限确认键位。

### 动态状态栏模块

**职责：**

- 缓存最后一次安全状态快照。
- Plan/Do、权限、会话或 Agent 状态改变后重新渲染。
- 详细模式显示完整状态行；精简模式只在状态变化或清屏后显示一条紧凑状态，避免刷屏。

推荐紧凑格式：

```text
do · ask · session a1b2c3d4 · 8.2k/64k
```

## 模块交互

### 启动装配

```text
config.yaml
   ↓
运行时权限控制器 ──→ 权限检查器
   ↓
运行状态静态信息（Provider / Model / Workspace / Context Window）
   ↓
注册十个核心命令 + 兼容命令
   ↓
命令补全源 ──→ JLine
   ↓
ConversationCoordinator + UIController + CommandRegistry
   ↓
ConversationLoop
```

MCP 启动完成后，将连接 Server 数和注册工具数保存到安全状态信息中，不在 `/status` 时重新连接或扫描。

### 输入分派

```text
用户输入
   ↓
CommandRegistry.dispatch
   ├─ 非命令 ───────────────→ 原始输入进入 Agent Loop
   ├─ HANDLED ─────────────→ 输出本地消息 → 刷新状态栏
   ├─ FORWARD_TO_AGENT ────→ 使用生成的 Prompt 进入 Agent Loop
   └─ EXIT_REQUESTED ──────→ 取消活动任务 → 安全关闭
```

规则：

- 未知 `/命令` 和解析错误都返回 `HANDLED` 错误，不得落入普通消息分支。
- `/review` 写入会话历史的是生成后的审查 Prompt，不是原始 `/review` 文本。
- `/compact` 直接调用专用压缩服务；需要摘要时允许由现有压缩恢复策略决定的专用摘要请求，但不会创建 Agent 工具循环。
- 命令执行完成或失败后统一恢复 `READY` 状态。

### 权限模式切换

```text
/permission full-access
   ↓
RuntimePermissionSettings.switchMode
   ↓
刷新状态栏
   ↓
下一次工具执行
   ↓
PermissionChecker 获取最新 PermissionSettings 快照
   ↓
危险命令 → 沙箱 → 模式上限 → 三层规则 → 默认策略 → HITL
```

配置文件和规则列表保持不变。进程退出后内存模式消失。

### 状态聚合

```text
静态环境信息
+ ConversationSession 历史与模式
+ 当前 SessionSummary
+ RuntimePermissionSettings
+ MCP 启动统计
+ Token 估算器
          ↓
CommandStatus
          ├─ /status 格式化输出
          └─ UIController.refreshStatus → 状态栏
```

状态快照在命令执行后和 Agent 状态变化时刷新；Token 估算只读取内存历史，不触发文件扫描或模型请求。

### Tab 补全

```text
JLine 当前缓冲区与光标
   ↓
是否为首段 /前缀？
   ├─ 否 → 无候选
   └─ 是 → CommandRegistry.complete(prefix)
                    ↓
          主名 + 别名稳定排序候选
```

补全只使用启动时已注册的内存索引，复杂度与命令数量线性相关，当前固定规模下无外部 I/O。

## 文件组织

```text
src/main/java/io/imiocode/
├── command/
│   ├── Command.java
│   ├── CommandType.java
│   ├── CommandDescriptor.java
│   ├── CommandOutcome.java
│   ├── CommandResult.java
│   ├── CommandRegistry.java
│   ├── CommandParser.java
│   ├── CommandContext.java
│   ├── CommandServices.java
│   ├── CommandStatus.java
│   ├── UIController.java
│   ├── ConfirmationPrompt.java
│   └── builtin/
│       ├── HelpCommand.java
│       ├── CompactCommand.java
│       ├── ClearCommand.java
│       ├── PlanCommand.java
│       ├── DoCommand.java
│       ├── SessionCommand.java
│       ├── MemoryCommand.java
│       ├── PermissionCommand.java
│       ├── StatusCommand.java
│       ├── ReviewCommand.java
│       ├── ReviewPromptBuilder.java
│       ├── ExitCommand.java
│       └── VerbosityCommand.java
├── permission/
│   ├── PermissionSettingsProvider.java
│   ├── RuntimePermissionSettings.java
│   └── PermissionChecker.java
├── context/
│   └── ApproximateTokenEstimator.java
├── runtime/
│   ├── ConversationCoordinator.java
│   └── ConversationLoop.java
├── terminal/
│   ├── TerminalUi.java
│   ├── JLineTerminalUi.java
│   ├── SlashCompletionSource.java
│   ├── SlashCommandCompleter.java
│   └── TerminalLayout.java
└── ImioCodeApplication.java
```

迁移：

- `LocalCommand.java` → `Command.java`
- `LocalCommandRegistry.java` → `CommandRegistry.java`
- `CommandDisposition.java` → `CommandOutcome.java`
- `terminal/ConfirmationPrompt.java` → `command/ConfirmationPrompt.java`

测试文件：

```text
src/test/java/io/imiocode/
├── command/
│   ├── CommandRegistryTest.java
│   ├── CommandParserTest.java
│   ├── BuiltinCommandTest.java
│   ├── ReviewPromptBuilderTest.java
│   └── CommandArchitectureTest.java
├── permission/
│   └── RuntimePermissionSettingsTest.java
├── runtime/
│   └── SlashCommandConversationLoopTest.java
├── terminal/
│   ├── SlashCommandCompleterTest.java
│   ├── JLineSlashCompletionTest.java
│   └── TerminalStatusBarTest.java
└── Ch10ApplicationIT.java
```

其他修改：

- `README.md` 补充十个核心命令、兼容命令、Tab 补全及权限临时切换说明。
- `docs/ch10/plan.md`、`task.md`、`checklist.md` 保存设计、开发顺序和验收记录。
- 不修改 `config.yaml`、`claude.md` 或 `hello.txt`。

## 技术决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 现有命令框架 | 原地迁移为统一框架，不保留两套注册中心 | 避免重复分派和行为漂移 |
| 命令元数据 | 不可变描述对象 | 帮助、补全和测试共用同一事实来源 |
| 注册索引 | 主名表、别名表分离 | O(1) 查找，同时能准确检测冲突 |
| 命令执行结果 | 互斥 outcome + 构造期校验 | 防止本地命令意外携带 Agent Prompt |
| UI 解耦 | 命令包中的最小 `UIController` 接口 | 命令不依赖 JLine，假 UI 可完整测试 |
| `/clear` | ANSI 使用 clear-screen capability，纯文本使用安全分隔并重绘状态 | 跨平台且不写入会话历史 |
| `/compact` | 保留 CH8 专用 LLM 摘要 | 保持摘要质量，不进入 Agent Loop、不执行工具 |
| `/review` | 固定模板 + 可选关注点，返回 Prompt 结果 | 行为可测，不引入动态 Prompt 文件 |
| `/review` 模式 | 不静默切换 Plan/Do，模板明确禁止修改 | 避免命令永久改变用户当前模式 |
| 权限切换 | 原子模式引用 + 不可变规则快照 | 下一次工具检查立即生效，线程安全且不写配置 |
| 状态 Token | 使用现有 3.5 chars/token 规则估算当前历史 | 不引入 tokenizer，也不触发模型请求 |
| MCP 状态 | 复用启动结果计数 | `/status` 无网络或连接副作用 |
| 状态栏 | 事件后推送不可变安全快照 | UI 不反向读取协调器，依赖方向清晰 |
| Tab 补全 | JLine Completer + 内存候选 | 保留原生单候选补齐和多候选菜单行为 |
| 补全边界 | 仅首段 `/前缀` | 防止误补全自然语言和命令参数 |
| 异常策略 | 参数错误附 usage，其他异常只显示安全消息 | 循环不中断且不暴露堆栈或秘密 |
| 兼容命令 | 帮助单独分组，继续参与补全 | 保留可发现性，不混淆十个核心命令 |
| 空闲约束 | 所有状态修改继续由服务层检查 Agent active | UI 和命令层无法绕过并发保护 |

## Spec 覆盖检查

- F1–F2：命令描述、注册中心和解析器负责。
- F3–F4：`UIController`、三类命令和互斥结果负责。
- F5：内置命令模块完整覆盖十个命令。
- F6：交互循环的结果分派和 `/compact` 专用路径负责。
- F7：补全源与 JLine 适配器负责。
- F8：状态聚合和动态状态栏负责。
- F9：兼容命令描述与帮助分组负责。
- F10：注册中心异常边界和安全输出负责。
- 当前未发现未归属的功能需求、接口缺口、技术决策冲突或循环依赖。
