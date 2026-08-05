# CH10 Slash Command 内置命令框架 Tasks

## 文件清单

| 操作 | 文件 | 职责 |
|---|---|---|
| 新建 | `src/main/java/io/imiocode/command/Command.java`<br>`CommandType.java`<br>`CommandDescriptor.java`<br>`CommandOutcome.java`<br>`CommandStatus.java`<br>`UIController.java`<br>`ConfirmationPrompt.java` | 统一命令契约、互斥分类、状态与 UI 抽象 |
| 修改 | `src/main/java/io/imiocode/command/CommandResult.java`<br>`CommandContext.java`<br>`CommandServices.java`<br>`CommandParser.java` | 三类结果、依赖背包、状态服务和解析边界 |
| 新建 | `src/main/java/io/imiocode/command/CommandRegistry.java` | 注册、查找、列举、补全与分派 |
| 删除 | `src/main/java/io/imiocode/command/LocalCommand.java`<br>`LocalCommandRegistry.java`<br>`CommandDisposition.java` | 移除 CH9 旧契约，避免双框架 |
| 修改 | `src/main/java/io/imiocode/command/builtin/HelpCommand.java`<br>`CompactCommand.java`<br>`PlanCommand.java`<br>`DoCommand.java`<br>`SessionCommand.java`<br>`MemoryCommand.java`<br>`ExitCommand.java`<br>`VerbosityCommand.java` | 迁移既有命令并补齐描述 |
| 新建 | `src/main/java/io/imiocode/command/builtin/ClearCommand.java`<br>`PermissionCommand.java`<br>`StatusCommand.java`<br>`ReviewCommand.java`<br>`ReviewPromptBuilder.java` | 新增 CH10 核心命令 |
| 新建 | `src/main/java/io/imiocode/permission/PermissionSettingsProvider.java`<br>`RuntimePermissionSettings.java` | 动态权限模式和不可变规则快照 |
| 修改 | `src/main/java/io/imiocode/permission/PermissionChecker.java` | 每次检查获取最新权限设置 |
| 修改 | `src/main/java/io/imiocode/context/ApproximateTokenEstimator.java` | 提供历史消息近似 Token 入口 |
| 修改 | `src/main/java/io/imiocode/runtime/ConversationCoordinator.java` | 权限服务、状态聚合与静态运行信息 |
| 修改 | `src/main/java/io/imiocode/runtime/ConversationLoop.java` | 命令优先、三类结果和状态刷新 |
| 修改 | `src/main/java/io/imiocode/terminal/TerminalUi.java`<br>`JLineTerminalUi.java`<br>`TerminalLayout.java` | UIController、清屏、动态状态栏和补全接入 |
| 新建 | `src/main/java/io/imiocode/terminal/SlashCompletionSource.java`<br>`SlashCommandCompleter.java` | JLine 与命令注册中心解耦的补全适配 |
| 删除 | `src/main/java/io/imiocode/terminal/ConfirmationPrompt.java` | 确认模型迁移到命令抽象层 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 动态权限、命令注册、MCP 状态和补全装配 |
| 新建/修改 | `src/test/java/io/imiocode/command/*Test.java` | 核心契约、注册、解析和十个命令测试 |
| 新建/修改 | `src/test/java/io/imiocode/permission/*Test.java` | 动态权限快照和权限链回归 |
| 新建 | `src/test/java/io/imiocode/runtime/SlashCommandConversationLoopTest.java` | 命令分派、零模型调用和 review 路径 |
| 新建/修改 | `src/test/java/io/imiocode/terminal/*Test.java` | 补全、清屏、状态栏和确认兼容测试 |
| 新建 | `src/test/java/io/imiocode/Ch10ApplicationIT.java` | 真实 Java 子进程端到端测试 |
| 修改 | `README.md`<br>`docs/ch10/checklist.md` | 用户说明和最终验收证据 |

## T1：定义命令描述与类型

**文件：** `Command.java`、`CommandType.java`、`CommandDescriptor.java`
**依赖：** 无

**步骤：**
1. 定义 LOCAL、UI、PROMPT 三类命令。
2. 定义命令描述，校验主名、别名、usage、description 和分类。
3. 定义统一命令接口，只暴露描述和执行方法。
4. 保证描述集合不可变，主名与别名规范为小写。

**验证：** 运行描述构造测试，合法描述往返一致，空值、非法名称和重复别名被拒绝。

## T2：实现互斥命令结果

**文件：** `CommandOutcome.java`、`CommandResult.java`
**依赖：** T1

**步骤：**
1. 定义 HANDLED、FORWARD_TO_AGENT、EXIT_REQUESTED。
2. 为本地消息、Prompt 转发和退出提供工厂方法。
3. 校验 Prompt 与 outcome 互斥关系，复制消息集合。
4. 拒绝空 Prompt、Prompt 混入本地结果和退出结果携带消息。

**验证：** `CommandResultTest` 覆盖三种合法结果与全部非法组合。

## T3：完成注册中心的注册与查找

**文件：** `CommandRegistry.java`
**依赖：** T1、T2

**步骤：**
1. 建立主名表、别名表和唯一命令集合。
2. 实现注册和大小写不敏感查找。
3. 拒绝主名重复、别名重复、主名占用别名及别名占用主名。
4. 保证失败注册不留下半注册状态。

**验证：** 运行 `CommandRegistryTest` 的查找、冲突和失败原子性用例。

## T4：实现稳定列举与补全

**文件：** `CommandRegistry.java`、`CommandRegistryTest.java`
**依赖：** T3

**步骤：**
1. `listCommands()` 每个命令只返回一次并按主名排序。
2. `complete(prefix)` 同时匹配主名和别名，返回带 `/` 候选。
3. 规范有无 `/`、大小写和空前缀输入。
4. 返回不可变、去重且稳定排序的候选列表。

**验证：** 运行列表唯一性、主名/别名补全、大小写和顺序测试。

## T5：强化解析器并接入安全分派

**文件：** `CommandParser.java`、`CommandRegistry.java`、`CommandParserTest.java`
**依赖：** T3

**步骤：**
1. 保持空白、引号、转义、中文和 Windows 反斜杠行为。
2. 非 `/` 输入返回空，任何 `/` 输入都被本地消费。
3. 未知命令返回安全错误和 `/help` 提示。
4. 参数异常附当前命令 usage，其他异常不输出堆栈。

**验证：** `mvn -q -Dtest=CommandParserTest,CommandRegistryTest test` 通过。

## T6：建立 UIController 与新 CommandContext

**文件：** `UIController.java`、`ConfirmationPrompt.java`、`CommandContext.java`
**依赖：** T1

**步骤：**
1. 定义清屏、确认、详细度和状态刷新接口。
2. 把通用确认模型迁移到命令包并保持构造校验。
3. `CommandContext` 改为 services、ui、commands 三项不可空依赖。
4. 删除上下文对 `TerminalUi` 的引用。

**验证：** 编译命令包，并用假 UI 构造上下文成功。

## T7：扩展 CommandServices 和状态模型

**文件：** `CommandStatus.java`、`CommandServices.java`
**依赖：** T6

**步骤：**
1. 定义只含安全字段的不可变状态快照。
2. 增加读取/切换权限模式和获取状态的方法。
3. 保留全部 CH9 会话、记忆、模式和压缩接口。
4. 校验 Token、窗口及 MCP 计数非负且窗口为正数。

**验证：** 状态模型边界测试通过，接口编译无终端具体类型依赖。

## T8：迁移既有核心命令

**文件：** `CompactCommand.java`、`PlanCommand.java`、`DoCommand.java`、`SessionCommand.java`、`MemoryCommand.java`
**依赖：** T1、T2、T6、T7

**步骤：**
1. 迁移到 `Command` 和 `CommandDescriptor`。
2. 为每个命令补齐 usage、description、类型和别名。
3. Session 删除改用 `UIController.confirm`。
4. 保持 CH9 可见输出、参数规则和持久化行为。

**验证：** 运行迁移命令回归测试，原有正常和错误输出一致。

## T9：迁移兼容命令

**文件：** `ExitCommand.java`、`VerbosityCommand.java`
**依赖：** T1、T2、T6

**步骤：**
1. 将 exit/quit、verbose、compact-ui 标记为兼容 UI 命令。
2. quit 继续作为 exit 别名。
3. Verbosity 只经 `UIController` 修改，不引用具体终端。
4. 保持退出和提示文案兼容。

**验证：** 运行兼容命令测试，四个命令字符串均可查找和执行。

## T10：实现动态权限设置容器

**文件：** `PermissionSettingsProvider.java`、`RuntimePermissionSettings.java`
**依赖：** 无

**步骤：**
1. 接收启动时不可变 `PermissionSettings`。
2. 用原子引用保存当前模式。
3. 每次 `snapshot()` 复用原规则并组合最新模式。
4. 拒绝空模式，不提供写配置接口。

**验证：** `RuntimePermissionSettingsTest` 验证五种模式切换、规则引用不变和并发读取一致。

## T11：让权限检查器读取动态快照

**文件：** `PermissionChecker.java`、`PermissionCheckerTest.java`
**依赖：** T10

**步骤：**
1. 把固定设置字段替换为设置 Provider。
2. 每次 `check()` 开始只取一次快照并贯穿整条权限链。
3. 保留接受固定设置的兼容构造器供已有调用方和测试使用。
4. 确认危险命令、沙箱、规则、模式和安全命令顺序不变。

**验证：** 原权限测试全部通过；新增同一 Checker 切换模式后决策变化测试通过。

## T12：实现 PermissionCommand

**文件：** `PermissionCommand.java`
**依赖：** T7、T10

**步骤：**
1. 无参数显示当前模式和五个可用值。
2. 单参数解析并切换当前进程模式。
3. 拒绝额外参数和未知模式并附 usage。
4. 输出只包含模式名，不打印规则或配置正文。

**验证：** 参数化测试覆盖五种模式、查询、非法模式和参数过多。

## T13：实现 HelpCommand 动态目录

**文件：** `HelpCommand.java`
**依赖：** T4、T6

**步骤：**
1. 从上下文注册中心读取命令描述。
2. 分“核心命令”和“兼容命令”稳定输出。
3. 每行展示 usage 和 description，不手写命令清单。
4. 拒绝参数。

**验证：** 帮助测试断言十个核心命令、四个兼容命令字符串各出现一次。

## T14：实现 ClearCommand

**文件：** `ClearCommand.java`
**依赖：** T6

**步骤：**
1. 定义 `/clear` 与 `/cls` 描述。
2. 拒绝参数。
3. 只调用 `UIController.clearScreen()`，不调用任何服务。
4. 返回已处理结果，由循环统一刷新状态。

**验证：** 假 UI 计数为 1，服务调用为 0，命令结果不含 Prompt。

## T15：实现 Review Prompt 构造器

**文件：** `ReviewPromptBuilder.java`、`ReviewPromptBuilderTest.java`
**依赖：** 无

**步骤：**
1. 编写固定只审查、不修改的模板。
2. 固定 findings 优先、严重度、文件位置、原因和测试要求。
3. 无关注点时输出字节稳定的基础模板。
4. 有关注点时仅追加脱离命令语法的 `Additional focus` 正文。

**验证：** 无参数、中文、多词关注点和保留 `/review` 文本样例测试通过。

## T16：实现 ReviewCommand

**文件：** `ReviewCommand.java`
**依赖：** T2、T15

**步骤：**
1. 定义 `/review` 与 `/rv` PROMPT 描述。
2. 将全部参数按空格重组为关注点。
3. 返回 `forwardToAgent`，不调用服务或 UI。
4. 保持当前 Plan/Do 模式不变。

**验证：** 测试结果只含一个非空 Prompt，服务/UI 调用均为 0。

## T17：实现历史 Token 估算入口

**文件：** `ApproximateTokenEstimator.java`、相关上下文测试
**依赖：** 无

**步骤：**
1. 增加只估算消息历史的公开方法。
2. 复用现有消息、部件和 3.5 chars/token 规则。
3. 空历史返回 0，溢出安全保持不变。
4. 不读取文件、不组装工具 schema、不调用模型。

**验证：** 文本、思考、工具调用/结果、空历史和超大输入测试通过。

## T18：扩展协调器的权限与状态服务

**文件：** `ConversationCoordinator.java`
**依赖：** T7、T10、T17

**步骤：**
1. 注入 Provider、模型、工作目录、窗口和 MCP 安全统计。
2. 注入运行时权限容器和 Token 估算器。
3. 实现权限读取/切换，并复用现有空闲检查。
4. 聚合当前历史、会话和模式生成 `CommandStatus`。

**验证：** 协调器测试覆盖状态字段、权限切换、busy 拒绝和无秘密字段。

## T19：实现 StatusCommand

**文件：** `StatusCommand.java`
**依赖：** T18

**步骤：**
1. 无参数读取一次状态快照。
2. 以稳定多行格式显示 Provider、模型、目录、模式、会话、Token 和 MCP。
3. 使用紧凑数值格式并保留完整会话 ID。
4. 拒绝参数，禁止输出对象 `toString()` 或完整配置。

**验证：** 状态格式快照测试通过，并搜索输出确认无测试密钥。

## T20：完成十个核心命令注册

**文件：** `ImioCodeApplication.java` 中的注册工厂、`BuiltinCommandTest.java`
**依赖：** T8、T9、T12–T16、T19

**步骤：**
1. 一次性注册十个核心命令及三个兼容处理器。
2. 断言 `/exit` 与 `/quit` 由一个处理器提供。
3. 启动时注册冲突立即失败。
4. 删除硬编码帮助清单。

**验证：** 注册目录测试断言十个核心描述、兼容分组和所有别名。

## T21：重构 ConversationLoop 三路分派

**文件：** `ConversationLoop.java`
**依赖：** T2、T5、T18、T20

**步骤：**
1. 使用新 `CommandContext` 和 `CommandRegistry`。
2. HANDLED 输出消息后刷新状态。
3. FORWARD_TO_AGENT 把生成 Prompt 传给既有 `runAgent`。
4. EXIT_REQUESTED 保持现有取消和关闭流程。
5. 命令异常或 Agent 结束后恢复 READY 并刷新安全状态。

**验证：** 循环单元测试覆盖普通输入、未知命令、三种 outcome 和异常后继续输入。

## T22：定义终端补全源与 JLine Completer

**文件：** `SlashCompletionSource.java`、`SlashCommandCompleter.java`
**依赖：** T4

**步骤：**
1. 定义只接收前缀、返回字符串候选的终端接口。
2. 解析 JLine buffer 和 cursor，只允许首段 `/前缀`。
3. 将候选转换为 JLine `Candidate`，不读取外部状态。
4. 普通文本、空格后参数、光标位于中间非法位置时返回空。

**验证：** `SlashCommandCompleterTest` 覆盖单候选、多候选、普通文本和参数位置。

## T23：把补全器装入 JLine

**文件：** `JLineTerminalUi.java`、`JLineSlashCompletionTest.java`
**依赖：** T22

**步骤：**
1. 增加接收补全源的构造路径并保留旧构造器。
2. 由 `LineReaderBuilder` 安装补全器和稳定菜单选项。
3. 确认 Tab 单候选补齐、多候选展示，Alt+Enter 仍插入换行。
4. 权限和删除确认使用的 reader 行为保持不变。

**验证：** 运行 JLine 输入序列测试和全部现有终端测试。

## T24：实现终端清屏

**文件：** `TerminalUi.java`、`JLineTerminalUi.java`、确认模型迁移引用
**依赖：** T6

**步骤：**
1. `TerminalUi` 扩展 `UIController`。
2. ANSI 模式调用终端 clear-screen capability 并 flush。
3. dumb/plain 模式输出安全分隔，不写 ANSI 控制序列。
4. 清屏前关闭未结束的回答和 thinking 行。

**验证：** 清屏测试分别断言 ANSI capability、纯文本无 ANSI 和 UI 仍可继续读取。

## T25：实现动态状态栏布局

**文件：** `TerminalLayout.java`、`JLineTerminalUi.java`、`TerminalStatusBarTest.java`
**依赖：** T7、T24

**步骤：**
1. JLine UI 缓存最新不可变状态快照。
2. 新增紧凑与详细状态格式，并处理窄终端截断。
3. 状态变化或清屏后立即渲染；不变时精简模式不重复打印。
4. 保持现有启动完整面板不变。

**验证：** full/responsive/plain、窄宽度、去重刷新和清屏重绘测试通过。

## T26：完成应用装配

**文件：** `ImioCodeApplication.java`
**依赖：** T10、T18、T20、T23、T25

**步骤：**
1. 用加载后的权限设置创建运行时权限容器和动态 Checker。
2. 把 MCP 启动计数及安全静态环境传给协调器。
3. 在 JLine 创建时注入 `registry::complete` 补全源。
4. 启动后推送首个状态快照，保持欢迎面板和关闭顺序不变。

**验证：** `mvn -q -DskipTests package` 通过，旧配置启动测试不需要新增配置项。

## T27：删除旧命令契约并做架构检查

**文件：** 删除 `LocalCommand.java`、`LocalCommandRegistry.java`、`CommandDisposition.java`、旧 terminal 确认模型；新增 `CommandArchitectureTest.java`
**依赖：** T21、T24、T26

**步骤：**
1. 删除旧类型及全部 import。
2. 扫描确保不存在两套注册中心或旧分派枚举。
3. 扫描 command 包不得引用 `JLineTerminalUi`、JLine 类或 runtime 循环。
4. 确认全部命令均由应用注册工厂真实使用。

**验证：** 架构测试和 `mvn -q -DskipTests compile` 通过。

## T28：完成命令单元测试矩阵

**文件：** `src/test/java/io/imiocode/command/*Test.java`
**依赖：** T27

**步骤：**
1. 覆盖十个核心命令和全部别名。
2. 覆盖正常、缺失参数、额外参数、功能关闭和服务异常。
3. 验证八个非 compact/review 命令零模型、零工具、零历史副作用。
4. 验证 compact 只调用压缩服务，review 只返回 Prompt。

**验证：** `mvn -q -Dtest='io.imiocode.command.*Test' test` 全部通过。

## T29：完成运行时与权限集成测试

**文件：** `RuntimePermissionSettingsTest.java`、`PermissionCheckerTest.java`、`SlashCommandConversationLoopTest.java`
**依赖：** T21、T27

**步骤：**
1. 验证权限命令切换影响下一次工具决策。
2. 验证 Agent active 时模式和会话状态修改被拒绝。
3. 验证八个本地命令不增加假 LLM 调用次数。
4. 验证 review 恰好进入一次 Agent，原始 `/review` 不入历史。
5. 验证 compact 使用专用摘要路径且无工具调用。

**验证：** 定向运行 permission 与 runtime 测试，Failures=0、Errors=0。

## T30：完成终端集成测试

**文件：** `src/test/java/io/imiocode/terminal/*Test.java`
**依赖：** T23–T25、T27

**步骤：**
1. 验证补全、状态刷新、清屏与确认 UI。
2. 验证精简模式不刷屏，详细模式保留 Agent 状态变化。
3. 验证启动完整面板像素无关的文本结构不变。
4. 运行原权限、MCP 和输入布局回归测试。

**验证：** `mvn -q -Dtest='io.imiocode.terminal.*Test' test` 全部通过。

## T31：执行真实 Java 进程端到端测试

**文件：** `Ch10ApplicationIT.java`
**依赖：** T26、T28–T30

**步骤：**
1. 用临时 workspace/userHome 和本地可控 LLM Server 启动真实 Java 子进程。
2. 输入 help、clear、plan、do、session、memory、permission、status 及兼容命令，断言无模型请求。
3. 准备历史后运行 compact，断言只出现摘要请求且无工具。
4. 运行 review，断言恰好一次 Agent 请求且包含固定模板和关注点。
5. 在 JLine 测试终端中验证 Tab 单候选与多候选。

**验证：** `mvn -q -Dtest=Ch10ApplicationIT test` 通过。

## T32：运行完整回归与打包

**文件：** 全部受影响测试
**依赖：** T31

**步骤：**
1. 运行命令、权限、终端、runtime 定向测试。
2. 运行 `mvn test` 并记录总数、失败、错误和跳过数。
3. 运行 shaded JAR 打包。
4. 不通过时修复实现，不降低断言或新增规避性跳过。

**验证：** `mvn test` 与 `mvn -DskipTests package` 均退出码 0。

## T33：文档和工作树最终检查

**文件：** `README.md`、`docs/ch10/checklist.md`
**依赖：** T32

**步骤：**
1. 记录核心命令、兼容命令、别名、补全和权限临时模式。
2. 对照 checklist 逐项写入真实命令和结果证据。
3. 运行空白、敏感文件和生成文件检查。
4. 保留用户已有 `claude.md` 和 `hello.txt`，不编辑、不暂存。

**验证：** `git diff --check` 通过；工作树只含 CH10 预期文件和用户原有改动。

## 执行顺序

```text
T1 → T2 → T3 → T4 → T5
 │                   │
 └────→ T6 → T7 ────┤
                     ├→ T8 → T9
T10 → T11 → T12 ────┤
T15 → T16 ──────────┤
T17 → T18 → T19 ────┤
                     ↓
                    T20 → T21
T4 → T22 → T23 ─────────┐
T6 → T24 → T25 ─────────┤
T10/T18/T20/T23/T25 → T26
                         ↓
                        T27
                  ┌──────┼──────┐
                  T28    T29    T30
                   └──────┼──────┘
                         T31 → T32 → T33
```
