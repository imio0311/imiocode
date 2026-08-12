# CH15 Agent Team 与 Coordinator Mode Plan

## 架构概览

CH15 在现有 `subagent` 与 `worktree` 之上增加独立的 `team` 领域。`AgentTeamManager` 是唯一聚合入口：负责团队生命周期、成员花名册、任务图、Mailbox、transcript、后端选择和收敛；具体 I/O 通过小接口隔离，保证外部 pane 与 JVM 内执行遵守同一状态机。

主 Agent 注册 `TeamCreate`、`TeamDelete`、团队感知的 `agent`、五个 Task 工具、`SendMessage` 和 Coordinator 工具。成员 Agent 使用按身份生成的工具注册表，只能访问本团队 Task、Mailbox 以及 Agent 定义许可的工作工具。所有查询和修改都在服务端从 `TeamPrincipal` 推导 team/agent，模型参数不能伪造调用者。

团队数据位于仓库 `.imiocode/teams/<team-slug>/`。`team.json` 与 `tasks.json` 采用临时文件 + 原子替换；Mailbox 和 transcript 采用 JSONL 追加、逐行校验、损坏尾部隔离。路径入口统一经过 `TeamPaths` 的 slug 与真实路径检查。

运行时通过 `TeammateBackend` 接口统一。`auto` 按 tmux → iTerm2 → in-process 探测；显式后端失败不降级，auto 外部启动失败告警并回退。第一阶段成员执行沿用 `SubagentAgentFactory` 与 `RunToCompletion`；外部后端启动受限的 `--team-member` 子进程，进程再从持久配置加载身份。

Coordinator Mode 是 Lead 侧动态 `ToolSelection` 与 System Reminder 的组合，而不是另一套 Agent。`CoordinatorModeController` 验证配置和环境双锁，保存进入前选择，按 Research → Synthesis → Implementation → Verification 推进并恢复原状态。

## 核心数据结构

### TeamConfig

```java
record TeamConfig(
    int schemaVersion,
    String name,
    String description,
    String leadAgentId,
    TeamBackend preferredBackend,
    Map<String, TeammateInfo> members,
    Instant createdAt,
    Instant updatedAt) {}
```

### TeammateInfo

```java
record TeammateInfo(
    String agentId,
    String name,
    String agentType,
    String model,
    TeamBackend backend,
    String backendHandle,
    Path worktree,
    TeammateStatus status,
    Boolean planApprovalRequired,
    Instant lastActiveAt) {}
```

`Boolean` 保留旧数据中的未知值；运行时只有明确 `TRUE` 才要求计划审批。

### TeamPrincipal

```java
record TeamPrincipal(String teamName, String agentId, TeamRole role) {}
```

所有团队工具在构造时绑定 principal，不接收可伪造的发送者或 team 参数。

### TeamTask

```java
record TeamTask(
    String id,
    String teamName,
    String title,
    String description,
    TeamTaskStatus status,
    String assigneeAgentId,
    Set<String> blocksOn,
    Set<String> blockedBy,
    long version,
    Instant createdAt,
    Instant updatedAt,
    String result) {}
```

任务仓储在一次锁内校验并同时写两端依赖；更新要求可选 expectedVersion，防止多进程静默覆盖。

### MailboxMessage

```java
record MailboxMessage(
    int schemaVersion,
    String id,
    String teamName,
    String senderAgentId,
    String recipientAgentId,
    MailboxMessageType type,
    String summary,
    String body,
    String taskId,
    Instant createdAt,
    Instant consumedAt) {}
```

广播在写入时展开为每位目标成员的独立消息，确保每个收件人独立确认、崩溃恢复幂等。

### TranscriptEntry

```java
record TranscriptEntry(
    int schemaVersion,
    String id,
    Instant createdAt,
    TranscriptRole role,
    String content,
    String correlationId) {}
```

### TeammateBackend

```java
interface TeammateBackend {
    TeamBackend kind();
    BackendAvailability probe(Duration timeout);
    BackendHandle start(TeammateLaunchRequest request);
    void wake(BackendHandle handle);
    void stop(BackendHandle handle, Duration timeout);
}
```

### AgentTeamManager

```java
TeamConfig createTeam(TeamCreateRequest request);
TeammateInfo spawn(TeamPrincipal lead, TeammateSpawnRequest request);
SendReceipt send(TeamPrincipal sender, SendMessageRequest request);
ConvergenceReport converge(TeamPrincipal lead, Duration timeout);
TeamDeletionReport deleteTeam(TeamPrincipal lead, boolean discard);
```

### CoordinatorModeController

```java
CoordinatorSnapshot enter(TeamPrincipal lead, ToolSelection current);
CoordinatorSnapshot advance(CoordinatorStage expected, CoordinatorStage next);
ToolSelection exit();
SystemReminder reminder();
```

## 模块设计

### 配置与安全

**职责：** 读取 `teams:` 配置，定义资源上限、后端偏好和 Coordinator feature flag；集中校验 slug、受管根目录、真实路径和双锁。

**对外接口：** `TeamRuntimeConfig`、`TeamPaths`、`CoordinatorGate`。

**依赖：** Jackson/YAML、JDK `Path`；不依赖 Agent 或后端。

### 团队模型与仓储

**职责：** 保存/恢复团队花名册和任务图，原子写入、schema 校验、跨进程文件锁、损坏配置 fail-closed。

**对外接口：** `TeamStore`、`TeamTaskStore`。

**依赖：** 配置与安全。

### Mailbox 与 transcript

**职责：** JSONL 追加、读取未消费消息、幂等确认、广播展开、尾部隔离、大小限制和写盘前脱敏。

**对外接口：** `MailboxStore`、`TranscriptStore`。

**依赖：** 配置与安全、`SecretRedactor`。

### 持久任务图

**职责：** 提供 create/get/list/update/stop，校验团队作用域、负责人、双向依赖和环；让旧 `/tasks`、`/task info`、`/task cancel` 命令同时观察和停止当前团队任务。

**对外接口：** `TeamTaskService` 及五个 BaseTool。

**依赖：** 团队仓储；本地命令层同时聚合现有 `TaskManager` 与团队任务服务。

### 后端运行时

**职责：** 探测/启动/唤醒/停止 tmux、iTerm2、in-process；统一句柄与诊断；只使用参数数组启动外部进程。

**对外接口：** `TeammateBackend`、`BackendSelector`、三个实现。

**依赖：** `ProcessBuilder`、`SubagentAgentFactory`、任务执行器。

### 团队编排器

**职责：** TeamCreate、spawn、消息投递、idle/续写、收敛、TeamDelete 回滚和清理；维护成员运行句柄。

**对外接口：** `AgentTeamManager`、`TeamCreateTool`、`TeamDeleteTool`、`SendMessageTool`、团队扩展后的 `AgentTool`。

**依赖：** 其余所有 team 子模块、WorktreeManager、AgentDefinitionLoader。

### Coordinator Mode

**职责：** 双锁、Lead 限制、工具白名单、四阶段状态机、提醒文本和退出恢复。

**对外接口：** `CoordinatorModeController`、`CoordinatorModeTool`、`CoordinatorAdvanceTool`。

**依赖：** `ToolSelection`、SystemReminder、团队编排器。

### 应用装配

**职责：** 构建共享 team runtime，注册 Lead 工具，向成员工厂注入 principal 和专属工具，启动恢复与有界关闭。

**对外接口：** `ImioCodeApplication` 装配代码、`--team-member` 启动参数。

## 模块交互

```text
Lead -> TeamCreateTool -> AgentTeamManager -> TeamStore
Lead -> AgentTool(team) -> WorktreeManager -> BackendSelector -> TeammateBackend
                                           -> TeamStore(roster)
                                           -> MailboxStore(task message)
Member -> Task*/SendMessage -> TeamTaskService/MailboxStore -> Lead/Member
SendMessage -> stopped member -> TranscriptStore(load) -> backend.start(resume)
Lead -> converge -> tasks + roster + mailbox + worktree safety -> report
Lead -> TeamDelete -> bounded stop -> safety inspect -> cleanup/preserve
Lead -> CoordinatorMode -> double gate -> ToolSelection whitelist + reminder
```

成员状态机：

```text
STARTING -> RUNNING -> IDLE -> RUNNING (SendMessage resume)
                    -> FAILED
RUNNING/IDLE -> STOPPING -> STOPPED
```

Coordinator 状态机：

```text
OFF -> RESEARCH -> SYNTHESIS -> IMPLEMENTATION -> VERIFICATION -> OFF
```

## 文件组织

```text
src/main/java/io/imiocode/
├── config/ConfigDocument.java, ConfigLoader.java, RuntimeConfig.java
├── team/
│   ├── config/TeamRuntimeConfig.java
│   ├── model/{TeamConfig,TeammateInfo,TeamPrincipal,...}.java
│   ├── persistence/{TeamPaths,TeamStore,JsonlStore,...}.java
│   ├── mailbox/{MailboxStore,MailboxMessage,...}.java
│   ├── task/{TeamTaskStore,TeamTaskService,TeamTask,...}.java
│   ├── backend/{TeammateBackend,BackendSelector,TmuxBackend,ITerm2Backend,InProcessBackend}.java
│   ├── runtime/{AgentTeamManager,TeamRuntime,...}.java
│   ├── coordinator/{CoordinatorModeController,CoordinatorPrompt,...}.java
│   └── tool/{TeamCreateTool,TeamDeleteTool,SendMessageTool,Task*Tool,...}.java
├── subagent/runtime/{AgentTool,SubagentDispatcher,SubagentToolRegistryFactory}.java
└── ImioCodeApplication.java

src/test/java/io/imiocode/team/
├── persistence/TeamStoreTest.java
├── mailbox/MailboxStoreTest.java
├── task/TeamTaskServiceTest.java
├── backend/BackendContractTest.java
├── runtime/AgentTeamManagerTest.java
├── coordinator/CoordinatorModeControllerTest.java
└── TeamToolsIntegrationTest.java
```

## 技术决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 持久目录 | `.imiocode/teams/<slug>` | 仓库作用域明确，可与现有 ImioCode 数据共存并统一沙箱 |
| 结构化快照 | JSON + 原子替换 | 花名册/任务图需要整体不变量和版本校验 |
| 事件记录 | JSONL + 尾部隔离 | Mailbox/transcript 需要追加、恢复和审计 |
| 并发控制 | JVM 锁 + OS FileLock + version | 同时覆盖同进程线程和外部 pane 进程 |
| 广播语义 | 写时展开 | 每个收件人消费状态独立，恢复简单可靠 |
| 后端 auto | tmux → iTerm2 → in-process | 尊重当前终端，跨平台始终可用 |
| 外部启动 | `ProcessBuilder(List<String>)` | 不经过 Shell 拼接，降低注入风险 |
| 成员上下文 | 基础 Prompt + Mailbox 动态任务 + transcript | 身份稳定，任务可续写，Prompt 不无限膨胀 |
| 工作区 | 每个成员独立 Worktree | 沿用 CH14 安全模型，防止并行写冲突 |
| Coordinator | 动态 ToolSelection，不复制 Agent | 能恢复原能力，避免两套会话和状态分叉 |
| 清理策略 | 默认保留有改动/提交的 Worktree | fail-closed，避免用户成果丢失 |

## Spec 覆盖自检

- F1—F5：配置、安全与团队仓储。
- F6—F11：团队编排器与 Lead 工具。
- F12—F17：统一后端接口和选择器。
- F18—F22：持久任务图、Task 工具与成员工具隔离。
- F23—F27：Mailbox JSONL 协议。
- F28—F31：transcript、idle 和 resume 状态机。
- F32—F35：收敛、停止和 Worktree 安全清理。
- F36—F40：Coordinator 双锁、白名单与四阶段状态机。
- 依赖方向为 config/model → persistence → services/backends → runtime/tools → application，无循环依赖。
