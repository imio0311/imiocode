# CH15 Agent Team 与 Coordinator Mode Checklist

> 每一项都通过命令输出、持久文件或终端行为验证；完成开发后填写实际证据。

> 状态：41/41 已通过。逐项证据见 [`acceptance-report.md`](acceptance-report.md)。

## 实现完整性

- [x] 团队模型、Lead 和花名册可创建并在应用重启后恢复。（验证：运行 `TeamStoreTest`，比较重启前后快照）
- [x] 非法 slug、重复成员、未知 schema、损坏配置和受管目录越界全部 fail-closed。（验证：运行安全/仓储负例测试）
- [x] TeamCreate 创建 Lead；同名团队不会被覆盖。（验证：连续执行两次同名创建，第二次返回明确失败）
- [x] 团队内 agent spawn 生成稳定身份、独立 Worktree 和单独任务消息。（验证：集成测试检查 roster、worktree 与 mailbox 文件）
- [x] spawn 任一步失败会逆序补偿，无法清理项会报告而非隐藏。（验证：故障注入测试逐个触发失败点）
- [x] TeamDelete 仅允许 Lead，默认拒绝删除有活动成员、未读重要消息或有成果的 Worktree。（验证：运行生命周期负例测试）
- [x] 明确 discard 后只清理可证明属于目标团队的资源。（验证：目标团队删除后，旁路文件/团队/pane 仍存在）

## 后端

- [x] auto 在 tmux 内优先 tmux，macOS+iTerm2 次之，其他环境使用 in-process。（验证：fake probe 组合测试）
- [x] auto 外部启动失败会告警并回退 in-process；显式后端失败不回退。（验证：故障后端测试）
- [x] tmux/iTerm2 命令使用参数数组，启动参数中不出现 API Key、完整 Prompt 或消息正文。（验证：ProcessExecutor 参数捕获测试）
- [x] tmux wake 仅发送固定信号；wake 失败不丢 Mailbox 消息。（验证：模拟 send-keys 失败后仍能轮询消息）
- [x] in-process 成员各自拥有独立线程、上下文、Agent handle 和取消句柄。（验证：并发 fake runner 测试）
- [x] 所有后端 stop/close 都有界，无法清理时返回可诊断保留项。（验证：超时 fake process 测试）

## Task 与工具隔离

- [x] TaskCreate/Get/List/Update/Stop 可由 LLM 工具调用，旧 `/tasks`、`/task info`、`/task cancel` 不回归。（验证：工具测试 + `BuiltinCommandTest`）
- [x] `addBlocksOn`/`addBlockedBy` 同时更新依赖两端。（验证：读取两个任务快照）
- [x] 自依赖、循环依赖、未知任务、跨团队依赖和 stale version 被拒绝。（验证：任务图负例测试）
- [x] 成员只能看到本团队任务，不能伪造 team、sender 或 Lead 身份。（验证：两个团队 principal 的隔离测试）
- [x] 成员工具池仅含 Agent 定义许可的工作工具、Task 工具和 SendMessage，不含 TeamCreate/TeamDelete/普通 spawn/全局管理能力。（验证：导出工具定义并比较白名单）

## Mailbox、transcript 与续写

- [x] 单播写入目标 agent 文件，广播为每个目标创建独立可消费消息。（验证：MailboxStoreTest）
- [x] 消息确认幂等，并发写不丢记录；损坏尾部隔离后完整消息仍可读取。（验证：并发与恢复测试）
- [x] 停止/响应/计划审批使用结构化消息，只有 Lead 能审批计划。（验证：控制消息权限测试）
- [x] Mailbox 与 transcript 中的测试密钥均已脱敏，单条和单文件上限生效。（验证：搜索原始测试密钥为零命中）
- [x] 成员任务完成后进入 idle 并通知 Lead，同时保留身份、Mailbox、transcript 和 Worktree。（验证：成员状态与持久文件）
- [x] SendMessage 给 idle/stopped 成员后，以同一 agent ID 恢复并携带完整旧 transcript 续写。（验证：fake runner 第二轮收到第一轮上下文）
- [x] transcript 损坏尾部可恢复；无法安全恢复时成员保持 stopped 并向 Lead 报错。（验证：两类损坏测试）

## 收敛与 Coordinator Mode

- [x] 收敛报告包含全部任务/成员状态、未读 Mailbox、Worktree 分支、dirty 和独有提交。（验证：构造混合状态后比较报告）
- [x] feature flag 或 `IMIO_COORDINATOR_MODE=true` 任一缺失时无法进入 Coordinator Mode。（验证：四种双锁组合测试）
- [x] 非 Lead 无法进入 Coordinator Mode。（验证：member principal 调用失败）
- [x] Coordinator 工具集只保留 agent、TaskStop/查询、SendMessage、阶段/综合输出能力，Read/Write/Edit/Bash 均不可用。（验证：每阶段导出工具定义）
- [x] Research → Synthesis → Implementation → Verification 顺序可观察，跳级被拒绝。（验证：状态机测试与终端 reminder）
- [x] 退出后恢复进入前 ToolSelection/提醒，不重建或删除团队；重启不会因单锁自动进入。（验证：前后快照和重启测试）

## 编译与回归

- [x] Java 21 项目编译无错误。（验证：`mvn -q -DskipTests compile`）
- [x] CH15 自动化测试全部通过。（验证：`mvn -q -Dtest='io.imiocode.team.**' test` 或平台等价选择）
- [x] 全量单元/集成测试通过，CH13/CH14 回归为零。（验证：`mvn test`）
- [x] fat JAR 成功生成且可启动。（验证：`mvn package -DskipTests` 后运行 `java -jar target/*-jar-with-dependencies.jar`）
- [x] 代码无空白错误，配置样例可加载。（验证：`git diff --check` 和配置加载测试）
- [x] 应用关闭后无本章创建的虚拟线程、外部进程、pane、lock 残留。（验证：测试探针与 tmux/list-process 检查）

## 端到端场景

- [x] 场景 1：在 tmux 中启动 ImioCode，请求“创建两个队员分别分析和实现一个小改动并互相汇报” → Lead 调用 TeamCreate/agent，两个成员拥有独立 Worktree，通过 Mailbox 协作并收敛。（验证：capture-pane、roster、mailbox、worktree list）
- [x] 场景 2：一个队员完成后 idle，Lead 发送追问 → 同一 agent ID 续写，回答能引用此前任务上下文。（验证：capture-pane 与 transcript 前后记录）
- [x] 场景 3：打开配置锁和环境锁后进入 Coordinator Mode → 按四阶段完成一次真实协作，Lead 不能直接调用文件/Bash，Verification 后恢复普通工具。（验证：capture-pane 中的工具列表、阶段提示和结果）
- [x] 场景 4：团队含 dirty Worktree 时普通删除被拒绝 → 保留成果并报告路径；清理安全资源后退出无残留。（验证：TeamDelete 输出、文件存在性、tmux pane 和进程检查）

## Spec 对齐自检

- AC1 对应“实现完整性”前两项。
- AC2 对应 TeamCreate/spawn/TeamDelete 条目。
- AC3 对应“后端”全部条目。
- AC4 对应“Task 与工具隔离”。
- AC5 对应 Mailbox 条目。
- AC6 对应 idle/transcript/续写条目。
- AC7 对应收敛与关闭条目。
- AC8 对应 Coordinator Mode 条目。
- AC9 对应“编译与回归”。
- AC10 对应四个 tmux 端到端场景。
