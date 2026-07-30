# ch6 权限系统 Tasks

共 22 个任务，每个任务完成后立即运行对应验证。

## 文件清单

| 操作 | 文件 | 职责 |
|---|---|---|
| 新建 | `src/main/java/io/imiocode/permission/PermissionMode.java` 等权限领域类型 | 模式、动作、请求、决策、规则和回复模型 |
| 新建 | `src/main/java/io/imiocode/permission/PermissionRequestFactory.java` | 六工具参数归一化 |
| 新建 | `src/main/java/io/imiocode/permission/command/*` | 跨平台危险命令检测 |
| 新建 | `src/main/java/io/imiocode/permission/sandbox/*` | 调度前路径沙箱 |
| 新建 | `src/main/java/io/imiocode/permission/rule/*` | 三层 YAML、Glob 和规则引擎 |
| 新建 | `src/main/java/io/imiocode/permission/PermissionModePolicy.java` | 五种模式策略 |
| 新建 | `src/main/java/io/imiocode/permission/PermissionChecker.java` | 五层权限决策 |
| 新建 | `src/main/java/io/imiocode/permission/PermissionCoordinator.java` | HITL 等待及会话授权 |
| 新建 | `src/main/java/io/imiocode/permission/PermissionGate.java` | 调度器统一入口 |
| 修改 | `src/main/java/io/imiocode/tool/workspace/WorkspacePolicy.java` | 强化真实路径与重解析点检查 |
| 修改 | `src/main/java/io/imiocode/agent/AgentEvent.java` | 权限请求和结果事件 |
| 修改 | `src/main/java/io/imiocode/agent/StreamingToolScheduler.java` | 工具启动前权限闸门 |
| 修改 | `src/main/java/io/imiocode/agent/StreamingTurnExecutor.java` | 向调度器传递权限组件 |
| 修改 | `src/main/java/io/imiocode/agent/Agent.java` | 权限生命周期和回复入口 |
| 修改 | `src/main/java/io/imiocode/conversation/ConversationSession.java` | 转发 HITL 回复 |
| 修改 | `src/main/java/io/imiocode/conversation/ConversationLoop.java` | 消费权限事件 |
| 修改 | `src/main/java/io/imiocode/terminal/TerminalUi.java` | 确认接口 |
| 修改 | `src/main/java/io/imiocode/terminal/JLineTerminalUi.java` | 三选一确认框 |
| 修改 | `src/main/java/io/imiocode/terminal/UiState.java` | 权限等待状态 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 权限系统装配 |
| 新建/修改 | 对应 `src/test/java/io/imiocode/**` 测试 | 单元、集成和回归验证 |
| 新建 | `docs/ch6/permissions.example.yaml` | 三层规则示例 |

## T1：建立权限领域模型

**依赖：** 无

**步骤：**

1. 创建五种模式、三种动作、三种操作类型、规则层、决策来源和 HITL 回复枚举。
2. 创建权限请求、决策、规则、设置和提示记录类型。
3. 在构造器中完成非空、非空白和不可变集合校验。

**验证：** `mvn -DskipTests compile`，期望权限领域类型编译成功。

## T2：实现六工具权限请求提取

**依赖：** T1

**步骤：**

1. 建立工具名到操作类型及参数名的固定映射。
2. 规范化路径分隔符、Glob/Grep 默认目标和 Bash 命令文本。
3. 生成脱敏、截断后的展示目标。
4. 为缺失参数、未知工具和非法目标补充拒绝测试。

**验证：** `mvn -Dtest=PermissionRequestFactoryTest test`。

## T3：实现危险命令检测器

**依赖：** T1

**步骤：**

1. 实现 PowerShell/Bash 命令规范化和命令段扫描。
2. 添加系统、磁盘、Fork Bomb、Git 清理和工作区整体删除规则。
3. 返回稳定规则 ID，不回显完整危险命令。
4. 添加危险正例、大小写/空白绕过和正常命令反例测试。

**验证：** `mvn -Dtest=RegexDangerousCommandDetectorTest test`。

## T4：强化路径沙箱

**依赖：** T1

**步骤：**

1. 实现调度前路径预检。
2. 强化 `WorkspacePolicy` 对真实路径、符号链接、Junction 和重解析点的检查。
3. 保留原子写入前复检。
4. 使用临时目录测试合法路径、绝对路径、`..` 和链接逃逸。

**验证：** `mvn -Dtest=WorkspacePathSandboxTest,WorkspacePolicyTest,AtomicFileWriterTest test`。

## T5：实现跨平台权限 Glob

**依赖：** T1

**步骤：**

1. 支持 `*`、`**`、`?` 和普通文本。
2. 将目标统一为 `/` 分隔。
3. 正确转义正则特殊字符。
4. 拒绝非法或空 glob。

**验证：** `mvn -Dtest=PermissionRuleEngineTest test` 中 Glob 用例通过。

## T6：实现三层规则加载

**依赖：** T1、T5

**步骤：**

1. 定义严格 YAML 文档模型。
2. 按用户、项目、本地固定路径加载。
3. 按优先级解析模式，缺省为 `ASK`。
4. 对未知字段、非法枚举、非普通文件和格式错误快速失败。
5. 确保规则顺序保持不变。

**验证：** `mvn -Dtest=PermissionRuleLoaderTest test`。

## T7：实现规则引擎

**依赖：** T5、T6

**步骤：**

1. 按用户、项目、本地顺序匹配。
2. 同层采用第一条匹配规则。
3. 支持工具 glob 与可选目标 glob。
4. 将匹配层和原因写入决策。
5. 覆盖层级冲突、同层冲突和无匹配测试。

**验证：** `mvn -Dtest=PermissionRuleEngineTest test`。

## T8：实现模式策略和统一检查器

**依赖：** T2、T3、T4、T7

**步骤：**

1. 实现五种模式默认行为。
2. 在规则匹配前应用 `LOCKDOWN` 和 `READ_ONLY` 上限。
3. 按危险命令、沙箱、模式上限、规则、模式默认值串联。
4. 所有异常转为安全拒绝。
5. 覆盖五种模式及不可绕过规则。

**验证：** `mvn -Dtest=PermissionModePolicyTest,PermissionCheckerTest test`。

## T9：实现 HITL 协调器

**依赖：** T1

**步骤：**

1. 使用唯一请求 ID 和可完成等待对象管理请求。
2. 实现一次允许、会话允许和拒绝。
3. 会话授权严格按工具名与规范化目标匹配。
4. 实现取消全部等待请求和关闭清理。
5. 覆盖并发回复、重复回复、未知 ID 和取消测试。

**验证：** `mvn -Dtest=PermissionCoordinatorTest test`。

## T10：实现权限闸门

**依赖：** T8、T9

**步骤：**

1. 组合请求提取、统一检查器和协调器。
2. `ALLOW`、`DENY` 直接返回，`ASK` 发布提示并等待。
3. 保证危险命令和沙箱拒绝不会进入 HITL。
4. 暴露回复、取消和关闭入口。

**验证：** `mvn -Dtest=PermissionGateTest test`。

## T11：扩展 Agent 权限事件

**依赖：** T1

**步骤：**

1. 新增权限请求与权限结果事件。
2. 校验请求 ID、提示和回复不能为空。
3. 扩展事件密封类型测试。

**验证：** `mvn -Dtest=AgentEventTest test`。

## T12：接入调度前权限预检

**依赖：** T10、T11

**步骤：**

1. 调度器收到完整工具调用后执行权限检查。
2. `DENY` 直接生成失败结果。
3. 只有明确允许的 LOW 工具能够流式提前执行。
4. `ASK` 建立 eager barrier，后续工具不得抢跑。
5. 保留未知工具熔断行为。

**验证：** `mvn -Dtest=StreamingToolSchedulerPermissionTest,StreamingToolSchedulerTest test`。

## T13：完成 ASK 顺序、回传和取消

**依赖：** T12

**步骤：**

1. 模型流结束后按原始索引逐个处理 ASK。
2. 允许后再执行工具并标记潜在副作用。
3. 拒绝结果作为失败 `tool_result` 返回。
4. 取消调度器时唤醒待确认请求。
5. 验证混合 ALLOW/ASK/DENY 批次保持结果顺序。

**验证：** `mvn -Dtest=StreamingToolSchedulerPermissionTest test`。

## T14：接入 TurnExecutor 和 Agent 生命周期

**依赖：** T10、T11、T13

**步骤：**

1. 将权限闸门传入每个流式工具调度器。
2. Agent 增加兼容构造入口和权限回复入口。
3. 任务取消、超时和关闭时取消待确认请求。
4. 保证会话授权跨轮次、关闭后清空。
5. 更新 Agent 与流式执行测试。

**验证：** `mvn -Dtest=AgentTest,AgentCancellationTest,StreamingTurnExecutorTest test`。

## T15：扩展会话权限回复入口

**依赖：** T14

**步骤：**

1. 在会话层转发请求 ID 和用户回复。
2. 保证回复入口不会与正在运行的同步会话死锁。
3. 会话关闭时拒绝新回复并取消等待。
4. 添加确认完成、未知 ID 和关闭测试。

**验证：** `mvn -Dtest=ConversationSessionTest,ConversationPermissionTest test`。

## T16：扩展终端接口和 UI 状态

**依赖：** T1

**步骤：**

1. 添加 `confirmPermission` 接口。
2. 添加 `PERMISSION_WAITING` 状态及颜色。
3. 更新测试终端替身，避免既有测试编译失败。
4. 保持非交互实现可以直接返回拒绝。

**验证：** `mvn -Dtest=ConversationLoopTest,JLineTerminalUiTest test` 编译并运行。

## T17：实现 JLine 确认框

**依赖：** T16

**步骤：**

1. 展示工具名、风险、目标摘要和原因。
2. 支持数字及英文别名。
3. 非法输入循环提示。
4. EOF、Ctrl+C 和关闭返回拒绝。
5. 确保命令和秘密经过既有脱敏器。

**验证：** `mvn -Dtest=JLinePermissionPromptTest,JLineTerminalUiTest test`。

## T18：接入 ConversationLoop

**依赖：** T11、T15、T17

**步骤：**

1. 消费权限请求事件并结束正在输出的行。
2. 切换权限等待状态。
3. 调用终端确认并回复会话。
4. 显示权限结果并恢复工具状态。
5. 验证普通输入与确认输入不竞争。

**验证：** `mvn -Dtest=ConversationLoopTest,ConversationPermissionTest test`。

## T19：完成应用装配

**依赖：** T6、T10、T14、T18

**步骤：**

1. 从工作区和用户目录加载权限设置。
2. 创建检测器、沙箱、规则引擎、协调器和闸门。
3. 把闸门注入 Agent。
4. 权限配置错误按配置错误退出。
5. 按正确顺序关闭终端、会话和权限资源。

**验证：** `mvn -DskipTests package`，使用最小有效配置启动后能进入终端。

## T20：补充权限配置示例

**依赖：** T6

**步骤：**

1. 创建 `docs/ch6/permissions.example.yaml`。
2. 包含五种模式说明和 allow/ask/deny 示例。
3. 说明三层路径、优先级、第一条匹配和会话授权不落盘。
4. 不在实际加载路径写入会影响项目的默认规则。

**验证：** 将示例复制到临时工作区后，`PermissionRuleLoaderTest` 能成功解析。

## T21：全量回归与安全验收

**依赖：** T1–T20

**步骤：**

1. 运行全部测试。
2. 对照既有 Agent、工具、Provider 和 Prompt 测试排查回归。
3. 验证危险测试只调用检测器，不执行真实危险命令。
4. 修复所有失败后重新运行全量测试。

**验证：** `mvn test`，0 failures、0 errors。

## T22：打包和真实终端测试

**依赖：** T21

**步骤：**

1. 使用 Java 21 打包可执行 JAR。
2. 启动 ImioCode。
3. 发送读取、写入、安全 Bash、危险 Bash 和路径逃逸请求。
4. 观察自动允许、HITL、拒绝、工具结果和最终回复。
5. 将实际结果记录到 checklist 验收报告；若当前系统没有 tmux，明确记录环境限制并使用可用的真实终端替代。

**验证：** JAR 正常启动退出，工作区外文件未改变，危险命令未启动。

## 执行顺序

```text
T1
├── T2 ───────────────┐
├── T3 ───────────────┤
├── T4 ───────────────┼→ T8 → T10 → T12 → T13 → T14 → T15 ─┐
├── T5 → T6 → T7 ─────┘                                     │
├── T9 ─────────────────────→ T10                            ├→ T18 → T19
├── T11 ─────────────────────────────→ T12/T14 ──────────────┤
└── T16 → T17 ───────────────────────────────────────────────┘
T6 → T20
T19 + T20 → T21 → T22
```
