# CH15 Agent Team 与 Coordinator Mode Spec

## 状态

已批准并完成。用户已统一确认 spec → plan → task → checklist 的推荐方案；本章实现与验收于 2026-08-12 完成。

## 背景

ImioCode 已具备单个主 Agent、可后台运行的子 Agent、任务记录和 Git Worktree 隔离，但这些能力仍以一次性委派为中心：子 Agent 之间没有稳定身份、共享团队花名册、持久消息邮箱和可恢复的工作记录；主 Agent 也缺少一个只负责拆解、调度、收敛和验收的受限模式。复杂任务因此难以形成持续协作的队伍。

CH15 引入本机 Agent Team。一个 Lead 可创建团队、生成具名队员、分派有依赖关系的任务、通过持久 Mailbox 通信，并在队员停止后基于 transcript 继续工作。队员可以运行在 tmux pane、iTerm2 pane 或当前 JVM 内；所有后端共享相同的团队协议和安全边界。Lead 还可在双锁保护下进入 Coordinator Mode，放弃直接修改代码的工具，只执行研究、综合、实施调度和验证收敛。

## 目标

- 为团队、Lead 和队员提供稳定、可验证、可持久化的身份与生命周期。
- 让外部 pane 后端与 in-process 后端遵守同一任务、Mailbox、transcript 和清理语义。
- 复用并扩展现有任务系统，使 Lead 和队员能够建立任务依赖、查询状态、停止工作和交换消息。
- 让已停止但仍在花名册中的队员可以保留上下文并被续写，而不是每次都创建新 Agent。
- 提供默认关闭、双锁启用、工具最小化的 Coordinator Mode。

## 功能需求

### 团队模型与持久化

- F1: 系统必须持久化团队名称、描述、Lead 身份、成员花名册、团队配置和创建/更新时间；应用重启后可恢复同一团队。
- F2: 每个成员必须具有团队内唯一的 agent ID、显示名称、Agent 类型、模型、执行后端、Worktree 路径、活动状态、计划审批要求和最近活动时间。未知状态必须与 `false` 区分，避免旧配置被误判为明确关闭。
- F3: 团队名称和成员名称必须使用严格的安全 slug 规则；所有团队文件、Mailbox、transcript 和 Worktree 路径必须在当前仓库的 `.imiocode` 受管目录内，真实路径越界时 fail-closed。
- F4: Lead 在创建团队后自动加入花名册且不可被普通成员操作冒充；成员看到的花名册不得包含密钥、完整 Prompt 或其他敏感配置。
- F5: 团队配置写入必须使用版本化格式和原子替换；损坏、未知版本、重复成员或路径不一致的配置必须给出安全诊断，不得部分加载。

### 团队创建、成员生成与删除

- F6: 主 Agent 必须获得顶层 TeamCreate 工具，至少接收团队名称，可选描述、默认 Agent 类型和后端偏好；同名团队不得被静默覆盖。
- F7: 团队创建成功后，主 Agent 的 Agent 委派入口必须能够在指定团队内生成具名成员；成员名称缺省时按 Agent 类型生成稳定可读且不冲突的序号名。
- F8: 生成成员必须依次完成定义解析、名称验证、独立 Worktree 获取、成员专属工具构建、后端启动、花名册注册和团队上下文注入；任一步失败都必须回滚可安全回滚的资源并报告保留项。
- F9: 团队成员创建后，系统必须通过 Mailbox 发送一条独立的附加任务消息；不得把动态任务永久拼接进成员基础 System Prompt。
- F10: 只有 Lead 可调用 TeamDelete。删除前必须请求所有活动成员停止并等待有界响应；仍有活动成员、dirty/独有提交 Worktree、未消费的重要消息或安全检查失败时默认拒绝删除。
- F11: 用户明确确认丢弃后，TeamDelete 可终止目标团队进程、移除安全的 pane/Worktree/临时分支以及团队元数据；不得删除团队之外的会话、pane、分支或目录。

### 三种执行后端

- F12: 系统必须提供 tmux pane、iTerm2 pane 和 in-process 三种成员执行后端，并记录每个成员实际使用的后端。
- F13: `auto` 后端采用推荐策略：当前已位于 tmux 会话时优先 tmux；macOS 且 iTerm2 CLI 可用时选择 iTerm2；其他情况选择 in-process。自动选择的外部后端启动失败时回退 in-process 并显示警告；用户显式指定的后端不可用时直接失败，不静默降级。
- F14: tmux 后端必须为团队创建独立 window 或受管 pane，成员各占一个 pane；不得把 pane 错绑到非 ImioCode 会话。iTerm2 后端必须使用独立 pane/process，并在非 macOS 或 CLI 不可用时判定为不可用。
- F15: 外部后端必须通过受限的成员进程入口启动，显式传入仓库、团队和 agent ID；成员进程从持久配置恢复身份，不通过命令行传递 API Key、完整 Prompt 或 Mailbox 内容。
- F16: in-process 后端必须让每个成员拥有独立 Agent 实例、虚拟线程、取消句柄、上下文、权限边界和 Worktree；不得共享可变会话历史。
- F17: 后端检测必须有超时且不经过 Shell 拼接；探测错误不得阻塞应用启动。团队内允许不同成员使用不同后端。

### 协调任务工具

- F18: 系统必须把现有任务记录能力提升为 Agent 可调用的 TaskCreate、TaskGet、TaskList、TaskUpdate 和 TaskStop 工具，同时保留现有 `/tasks` 与 `/task` 本地命令兼容性。
- F19: TaskUpdate 必须支持状态/负责人更新，并通过 `addBlocksOn` 与 `addBlockedBy` 建立双向一致的任务依赖；自依赖、循环依赖、未知任务或跨团队依赖必须被拒绝。
- F20: 团队任务必须记录所属 team、负责人 agent ID、阻塞关系和版本；并发更新必须避免丢失，不得把其他团队任务暴露给成员。
- F21: 队员工具池只包含允许的任务工具、SendMessage 和其 Agent 定义允许的工作工具；TeamCreate、TeamDelete、普通 Agent spawn、全局管理工具和外部未授权能力不得泄露给成员。
- F22: 主 Agent 在未创建团队时不得获得成员专属工具；普通一次性 SubAgent 也不得获得团队工具。

### Mailbox 与 SendMessage

- F23: Mailbox 必须按 team 和 agent ID 分文件持久化，使用只追加或原子更新语义；每条消息包含唯一 ID、发送者、接收者、类型、摘要、正文、创建时间、投递/消费状态和可选关联任务。
- F24: SendMessage 必须支持向单个成员发送和向 `*` 广播；摘要为简短预览，正文必须有大小上限。不存在的接收者、伪造发送者、跨团队目标和越界路径必须被拒绝。
- F25: 普通消息通过 mailbox 轮询消费；tmux 后端投递后还必须用参数化 `send-keys` 仅发送无敏感信息的唤醒信号。唤醒失败不丢消息，成员可在下一次轮询读取。
- F26: 系统必须支持结构化的停止请求/响应和计划审批响应。只有 Lead 可发送计划审批结果；拒绝必须携带反馈。消息协议必须向后兼容未知的可忽略字段。
- F27: Mailbox 消费必须幂等；崩溃恢复后不得重复执行已确认完成的控制消息，也不得因部分写入丢失已成功持久化的消息。

### 空闲、transcript 与续写

- F28: 每个成员必须将输入、模型回复、工具结果、Mailbox 控制事件和终态摘要持久化到独立 transcript；敏感值在写盘前脱敏，单文件和单条记录均有大小上限。
- F29: 成员完成当前任务后进入 idle，释放本轮 Agent/LLM 等运行资源但保留花名册、Mailbox、transcript、Worktree 和后端身份；Lead 必须收到 idle 通知。
- F30: Lead 向 idle 或已停止成员 SendMessage 时，系统必须用原 agent ID 恢复该成员，加载 transcript 重建完整上下文，并把新消息作为新的用户输入继续执行；不得创建同名新成员或丢失此前讨论。
- F31: transcript 损坏时必须隔离损坏尾部并恢复最后一个完整记录；无法安全恢复时成员保持停止并向 Lead 报错。

### 收敛与生命周期清理

- F32: Lead 必须能观察所有成员和任务的 pending/running/blocked/idle/stopped/failed 状态，并对超时或无响应成员执行 TaskStop/停止请求。
- F33: 团队收敛时，系统必须等待所需任务终态，汇总每个成员的结果、Worktree 分支、dirty/独有提交和未处理消息，生成可供 Lead 决策的收敛报告。
- F34: CH15 不自动 merge 或丢弃成员 Worktree。clean 且无独有提交的临时资源可安全清理；有改动或提交的 Worktree 必须保留并报告，由用户决定合并或丢弃。
- F35: 应用退出或团队删除后，不得遗留本进程虚拟线程、受管外部成员进程、活跃 lock 或错误指向其他会话的 pane；无法清理的资源必须记录为可诊断保留项。

### Coordinator Mode

- F36: Coordinator Mode 默认关闭，只有配置 feature flag 与环境变量 `IMIO_COORDINATOR_MODE=true` 同时开启才可激活；缺任一锁时请求进入必须失败并说明缺失条件。
- F37: Coordinator Mode 仅允许 Lead 使用。激活后 Lead 的工具集必须收窄到团队委派、任务停止/查询、SendMessage 和最终综合输出；文件读写、编辑、Bash、安装、TeamDelete 之外的破坏性工具以及普通直接执行能力不得可用。
- F38: Coordinator Mode 必须注入四阶段工作流提醒：Research（并行调研）、Synthesis（Lead 综合并决定返工）、Implementation（按综合规格由队员实施）、Verification（独立验证与收敛）。阶段转换必须可观察，Lead 不得跳过综合直接让队员按未经审视的初始方案实施。
- F39: Coordinator Mode 下 Lead 不直接修改代码；最终输出必须基于团队结果、任务状态和验证证据。成员的普通 Prompt 不得被污染为 Coordinator Prompt。
- F40: 退出 Coordinator Mode 必须恢复进入前的工具选择和提醒状态，不重建或删除现有团队；应用重启后不得因残留单锁而自动进入。

## 非功能需求

- N1: 所有团队名称、agent ID、文件路径、pane 目标和外部进程参数都视为不可信输入；不得使用 Shell 字符串拼接。
- N2: 团队配置、Mailbox、transcript 和任务图必须有 schema version、写入上限、原子性或可恢复追加协议，并在多线程/多进程访问下保持一致。
- N3: 所有删除、停止、广播和后端回退都必须有可观察日志；日志、错误、进程参数和 pane 唤醒不得泄露密钥或完整敏感正文。
- N4: 现有单 Agent、SubAgent、Worktree、Task 命令、Hook、权限、MCP、Session 和非 Git 目录启动行为保持兼容。
- N5: 外部后端不可用时，除用户显式要求该后端外，ImioCode 仍可通过 in-process 完成团队任务。
- N6: 团队数量、成员数、消息数、transcript 大小、并发任务和关闭等待时间必须可配置且有安全默认上限。
- N7: 所有后台轮询器、虚拟线程、进程和 pane 句柄必须支持取消与有界关闭；检查失败一律 fail-closed。

## 不做的事

- 不实现跨机器、跨仓库主机的分布式 Agent Team。
- 不实现 token 级或字符级的队员实时流式通信；Mailbox 只传递持久消息和控制事件。
- 不自动决定或执行成员 Worktree 的 merge、rebase、冲突解决或丢弃。
- 不接入 Jira、Slack 等组织级权限或第三方团队服务。
- 不实现通用终端复用器管理器；只管理本章创建并可证明归属 ImioCode 的 pane/process。

## 验收标准

- AC1: 创建团队、重启应用并恢复后，Lead、成员花名册、配置和实际后端保持一致；损坏配置被安全拒绝。（覆盖 F1—F5）
- AC2: TeamCreate、团队内成员生成、附加任务投递和 TeamDelete 的成功/失败回滚均可在真实临时仓库观察，且不会影响团队外资源。（覆盖 F6—F11）
- AC3: tmux、iTerm2 和 in-process 后端均有契约测试；可用平台完成真实 pane/process 场景，auto/显式失败语义与 F13 一致。（覆盖 F12—F17）
- AC4: TaskCreate/Get/List/Update/Stop 与旧命令共用同一状态；依赖图双向一致并拒绝环，成员看不到跨团队或管理工具。（覆盖 F18—F22）
- AC5: 单播、广播、幂等消费、控制消息、tmux 唤醒失败降级和并发写入测试全部通过，且输出不含消息正文中的测试密钥。（覆盖 F23—F27）
- AC6: 成员 idle 后资源释放；Lead 再次 SendMessage 使用同一 agent ID 和完整旧 transcript 续写；损坏尾部可恢复。（覆盖 F28—F31）
- AC7: 团队状态与收敛报告准确反映任务、成员、Mailbox 和 Worktree；退出后无可清理的进程、线程、pane、lock 残留。（覆盖 F32—F35）
- AC8: Coordinator Mode 只有双锁可进入，工具白名单和四阶段提醒生效；Lead 不能直接读写或执行 Bash，退出后恢复原工具状态。（覆盖 F36—F40）
- AC9: Java 21 全量测试、fat JAR 和 `git diff --check` 通过，CH13/CH14 回归为零。（覆盖 N1—N7）
- AC10: tmux 端到端场景中，Lead 创建至少两个队员，队员通过 Mailbox 协作并在独立 Worktree 工作；Lead 续写一个 idle 队员、进入 Coordinator Mode 完成四阶段流程、收敛后安全清理。（覆盖整体用户流程）

## 自检

- 未包含实现类名、方法签名或 Java 数据结构定义。
- F1—F40 均由 AC1—AC10 覆盖。
- 明确排除自动合并、跨机器分布式和实时流式通信。
- 未包含未决占位内容；后端选择已随本次审批确定。
