# CH15 Agent Team 与 Coordinator Mode Tasks

## 文件清单

| 操作 | 文件/目录 | 职责 |
|---|---|---|
| 新建 | `src/main/java/io/imiocode/team/config/` | 团队配置与默认上限 |
| 新建 | `src/main/java/io/imiocode/team/model/` | 团队、成员、状态与 principal |
| 新建 | `src/main/java/io/imiocode/team/persistence/` | 安全路径、原子快照与恢复 |
| 新建 | `src/main/java/io/imiocode/team/mailbox/` | Mailbox 协议与 JSONL 存储 |
| 新建 | `src/main/java/io/imiocode/team/task/` | 持久任务图与依赖服务 |
| 新建 | `src/main/java/io/imiocode/team/backend/` | 后端接口、探测、选择与实现 |
| 新建 | `src/main/java/io/imiocode/team/runtime/` | 团队编排、成员运行与收敛 |
| 新建 | `src/main/java/io/imiocode/team/coordinator/` | Coordinator 双锁与阶段状态机 |
| 新建 | `src/main/java/io/imiocode/team/tool/` | Lead/成员 LLM 工具与 TeamConverge |
| 修改 | `src/main/java/io/imiocode/config/{ConfigDocument,ConfigLoader,RuntimeConfig}.java` | 接入 `teams:` 配置 |
| 修改 | `src/main/java/io/imiocode/subagent/runtime/{AgentTool,SubagentDispatcher,SubagentToolRegistryFactory}.java` | 团队 spawn 与专属工具 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 应用装配、恢复和关闭 |
| 修改 | `config.example.yaml`、`README.md` | 配置与使用说明 |
| 新建 | `src/test/java/io/imiocode/team/` | 单元、契约与集成测试 |

## T1：团队配置与安全路径

**文件：** `team/config/*`、`team/persistence/TeamPaths.java`、配置加载文件
**依赖：** 无

**步骤：**
1. 定义后端偏好、Coordinator flag、容量/超时/大小上限及安全默认值。
2. 把 `teams:` 映射进统一配置并验证正数、枚举和值域。
3. 实现 team/member slug 和 `.imiocode/teams` 真实路径边界校验。

**验证：** 运行配置与 `TeamPathsTest`，合法值加载、未知后端/越界路径被拒绝。

## T2：团队领域模型

**文件：** `team/model/*`
**依赖：** T1

**步骤：**
1. 定义 TeamConfig、TeammateInfo、TeamPrincipal、角色、成员状态和后端枚举。
2. 在构造器中复制集合、规范化文本并验证 Lead/成员不变量。
3. 为旧配置的 nullable 审批字段保留未知语义。

**验证：** 运行模型测试，重复成员、无 Lead、非法状态组合均失败。

## T3：原子团队仓储

**文件：** `team/persistence/TeamStore.java`
**依赖：** T1、T2

**步骤：**
1. 使用 Jackson 写版本化 `team.json`，临时文件 fsync 后原子替换。
2. 同进程锁与 FileLock 串行化修改；同名创建 fail-closed。
3. 加载时验证 schema、路径、成员唯一性并返回安全错误。

**验证：** `TeamStoreTest` 覆盖重启恢复、并发更新、损坏 JSON、未知版本和原子替换。

## T4：Mailbox 协议与存储

**文件：** `team/mailbox/*`
**依赖：** T1、T2

**步骤：**
1. 定义普通、停止、停止响应、计划审批、idle 消息类型与结构。
2. 按成员追加 JSONL，生成唯一 ID，限制摘要/正文并在写盘前脱敏。
3. 实现单播、广播展开、未消费读取和幂等确认。
4. 检测不完整尾行，隔离损坏尾部并保留完整记录。

**验证：** `MailboxStoreTest` 覆盖并发写、广播、幂等、崩溃恢复、越界与密钥脱敏。

## T5：Transcript 存储

**文件：** `team/persistence/TranscriptStore.java`
**依赖：** T1、T2

**步骤：**
1. 定义输入、回复、工具、Mailbox、终态记录。
2. 实现有界 JSONL 追加和完整历史加载。
3. 复用尾部隔离逻辑并拒绝无法安全恢复的文件。

**验证：** `TranscriptStoreTest` 覆盖顺序恢复、脱敏、大小上限和损坏尾部。

## T6：持久任务图仓储

**文件：** `team/task/{TeamTask,TeamTaskStatus,TeamTaskStore}.java`
**依赖：** T1、T2、T3

**步骤：**
1. 定义带 team、assignee、version、双向依赖的任务。
2. 原子保存 `tasks.json`，按 team 隔离查询。
3. 在同一事务中维护依赖两端，DFS 检测环并支持 expectedVersion。

**验证：** `TeamTaskStoreTest` 覆盖 CRUD、双向依赖、自环/多节点环、跨团队和版本冲突。

## T7：Task 服务与五个工具

**文件：** `team/task/TeamTaskService.java`、`team/tool/Task*Tool.java`
**依赖：** T6

**步骤：**
1. 为 principal 提供 Create/Get/List/Update/Stop 作用域 API。
2. assignee 活跃时把 stop 桥接到成员停止；让旧 Task 本地命令聚合团队任务查询/取消。
3. 定义严格 JSON schema、未知字段拒绝和脱敏结果。

**验证：** `TeamTaskToolsTest` 执行真实工具参数，确认权限、状态和错误消息。

## T8：统一后端接口与选择器

**文件：** `team/backend/{TeammateBackend,BackendSelector,...}.java`
**依赖：** T1、T2

**步骤：**
1. 定义 probe/start/wake/stop 契约及句柄、请求、诊断。
2. 实现有界并行探测和推荐 auto 顺序。
3. 显式不可用失败；auto 启动失败记录告警并尝试下一安全后端。

**验证：** `BackendSelectorTest` 用 fake backend 覆盖优先级、超时、回退和显式失败。

## T9：tmux 与 iTerm2 后端

**文件：** `team/backend/{ProcessExecutor,TmuxBackend,ITerm2Backend}.java`
**依赖：** T8

**步骤：**
1. 通过 `ProcessBuilder` 参数列表执行探测和启动，禁止 Shell 字符串。
2. tmux 只管理带团队前缀的 window/pane，wake 只发送固定无敏感 token。
3. iTerm2 仅在 macOS 且 CLI 可用时创建受管 pane/process。
4. stop 前验证句柄归属，超时后返回可诊断保留项。

**验证：** 参数捕获契约测试通过；安装 tmux 的环境运行真实 pane smoke test。

## T10：in-process 成员运行时

**文件：** `team/backend/InProcessBackend.java`、`team/runtime/TeammateRuntime.java`
**依赖：** T4、T5、T8

**步骤：**
1. 每成员使用独立虚拟线程、Agent handle、取消句柄和消息循环。
2. 把每轮消息/输出写 transcript，完成后转 idle 并释放 Agent handle。
3. 新消息到 idle/stopped 成员时加载 transcript，以同 agent ID 重建后续轮次。

**验证：** `InProcessBackendTest` 用 fake runner 证明隔离、idle、续写上下文和有界关闭。

## T11：成员工具工厂与上下文

**文件：** `subagent/runtime/SubagentToolRegistryFactory.java`、`team/runtime/TeamContextBuilder.java`
**依赖：** T4、T7、T10

**步骤：**
1. 为成员工作区替换核心工具并注册绑定 principal 的 Task/SendMessage 工具。
2. 与 Agent 定义 allow/deny 求交，硬拒绝 Lead/全局管理工具。
3. 注入精简花名册、身份、Mailbox 协议和 Worktree 提醒；动态任务走 Mailbox。

**验证：** `TeamMemberToolIsolationTest` 证明成员可工作/通信但无法创建删除团队或跨团队读取。

## T12：团队编排创建与 spawn

**文件：** `team/runtime/AgentTeamManager.java`、`team/tool/TeamCreateTool.java`、`AgentTool.java`、`SubagentDispatcher.java`
**依赖：** T3、T4、T8、T10、T11

**步骤：**
1. TeamCreate 持久化 Lead 和默认配置。
2. 扩展 agent 工具参数 `team_name`、`name`、`backend`、`plan_approval_required`。
3. 按定义→名称→Worktree→工具→后端→花名册→上下文→任务消息顺序 spawn。
4. 为每一步登记补偿动作，失败时逆序回滚并报告保留资源。

**验证：** `AgentTeamManagerSpawnTest` 覆盖成功、命名冲突及各失败点的回滚。

## T13：SendMessage、wake 与 resume

**文件：** `team/runtime/AgentTeamManager.java`、`team/tool/SendMessageTool.java`
**依赖：** T4、T5、T9、T10、T12

**步骤：**
1. 从 principal 固定 sender，校验接收者/控制消息权限。
2. 持久化后调用后端 wake；wake 失败仅告警。
3. 对 idle/stopped 成员触发同身份 resume，并把新消息作为用户输入。

**验证：** `SendMessageIntegrationTest` 覆盖单播、广播、伪造、wake 降级和 transcript 续写。

## T14：收敛与安全删除

**文件：** `team/runtime/{ConvergenceReport,TeamDeletionReport,AgentTeamManager}.java`、`TeamConvergeTool.java`、`TeamDeleteTool.java`
**依赖：** T7、T12、T13

**步骤：**
1. 汇总任务、成员、未读 Mailbox 与 Worktree 安全状态。
2. 删除前发送停止请求并有界等待；只有 Lead 可删除。
3. 默认保留 dirty/独有提交资源；明确 discard 才清理且再次校验归属。
4. close 取消轮询、线程、进程和文件锁并报告残留。

**验证：** `TeamLifecycleTest` 覆盖正常收敛、拒绝删除、确认丢弃、跨团队隔离和无线程泄漏。

## T15：Coordinator 双锁和阶段状态机

**文件：** `team/coordinator/*`
**依赖：** T1、T2

**步骤：**
1. 仅 feature flag + `IMIO_COORDINATOR_MODE=true` 且 Lead 身份允许进入。
2. 保存原 ToolSelection，切到 Agent/Task/SendMessage/综合输出白名单。
3. 实现四阶段合法转换、阶段提醒和退出恢复。
4. 禁止残留单锁在重启后自动激活。

**验证：** `CoordinatorModeControllerTest` 覆盖 4 种锁组合、非 Lead、非法跳级、白名单和恢复。

## T16：Coordinator 工具与会话接入

**文件：** `team/tool/{CoordinatorModeTool,CoordinatorAdvanceTool}.java`、Agent/Conversation 接入点
**依赖：** T15

**步骤：**
1. 暴露显式 enter/advance/exit 工具。
2. 让每个模型请求读取当前选择与阶段 reminder 快照。
3. Verification 完成或 exit 后恢复原工具，不影响团队本身。

**验证：** 集成测试观察工具定义和 reminder 随阶段变化，Lead 不能调用文件/Bash 工具。

## T17：应用装配、成员入口与恢复

**文件：** `ImioCodeApplication.java`、`LaunchOptions.java` 及 team runtime
**依赖：** T12—T16

**步骤：**
1. 装配 TeamStore、Mailbox、Task、后端、TeamManager 与 Lead 工具。
2. 加入受限 `--team-member <team> <agent-id>` 启动模式，从存储恢复身份。
3. 启动时恢复现有团队运行状态；关闭时有界停止所有受管资源。

**验证：** `TeamApplicationIT` 在临时仓库完成重启恢复，命令行不含密钥/Prompt。

## T18：文档与配置样例

**文件：** `config.example.yaml`、`README.md`
**依赖：** T17

**步骤：**
1. 补充 teams 配置、auto 规则、双锁和环境变量说明。
2. 给出 Lead 创建团队、spawn、Task、SendMessage、收敛和删除示例。
3. 说明 Worktree 默认保留策略与平台限制。

**验证：** 示例 YAML 可由 ConfigLoader 读取，README 工具名与实现一致。

## T19：全量自动化验证

**文件：** `src/test/java/io/imiocode/team/*`
**依赖：** T1—T18

**步骤：**
1. 运行 CH15 单元、契约、集成测试。
2. 运行全量 Maven 测试、fat JAR 打包和 `git diff --check`。
3. 修复所有回归后记录证据。

**验证：** `mvn test`、`mvn package -DskipTests`、`git diff --check` 均为成功退出码。

## T20：tmux 端到端验收

**文件：** `docs/ch15/checklist.md`
**依赖：** T19

**步骤：**
1. 在 tmux 中启动 fat JAR，并输入真实团队协作请求。
2. 创建两个成员、独立 Worktree、Mailbox 协作、idle 续写和收敛。
3. 双锁启用 Coordinator Mode，依次走四阶段并验证工具受限。
4. 安全清理，逐项填写 checklist 的实际证据。

**验证：** tmux capture-pane 显示工具调用、回复、续写、阶段转换和清理结果符合 checklist。

## 执行顺序

```text
T1 -> T2 -> T3 -> T4 -> T5 -> T6 -> T7
                  \-> T8 -> T9 -> T10 -> T11
T3 + T7 + T8 + T11 -> T12 -> T13 -> T14
T1 + T2 -> T15 -> T16
T14 + T16 -> T17 -> T18 -> T19 -> T20
```

## 自检

- plan.md 的全部模块均有对应任务。
- 每项任务均声明依赖、具体步骤和可执行验证。
- 任务依赖图无环；T8/T15 可在核心持久化完成后独立推进。
- 类型名和接口名与 plan.md 一致，无占位符。
