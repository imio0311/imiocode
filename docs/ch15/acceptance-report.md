# CH15 Agent Team 与 Coordinator Mode 验收报告

## 结论

2026-08-12 完成验收，`checklist.md` 41/41 通过。实现覆盖 Agent Team 模型与持久化、tmux/iTerm2/in-process 三后端、Task/SendMessage、Mailbox、transcript 续写、收敛/清理及 Coordinator Mode；未加入跨机器分布式或队员实时流式通信。

## 自动化证据

- Java 21 编译：`mvn -q -DskipTests compile`，退出码 0。
- CH15 与关联定向套件：团队、工具隔离、权限、动态 Coordinator、Session 恢复测试全部通过。
- 全量回归：`mvn -q test`，174 个报告、619 项测试、0 failure、0 error、3 skipped；测试累计 129.95 秒，命令墙钟约 121.5 秒。
- 打包：`mvn -q -DskipTests package`，生成 `target/imiocode-0.2.0-SNAPSHOT-all.jar`，5,467,261 字节。
- 空白检查：`git diff --check` 退出码 0；仅显示 Windows LF/CRLF 提示。

自动化重点覆盖：

- 团队 schema、原子替换、跨进程锁、slug/真实路径、重复 JSON key、损坏配置与任务图 fail-closed。
- Task 五工具真实调用、ISO-8601 时间编码、双向依赖原子更新、环/跨团队/stale version 拒绝；旧 `/tasks`、`/task info`、`/task cancel` 聚合团队任务。
- 成员工具注册表物理裁剪：无限制定义默认仅获得六个仓库工作工具；显式定义才可授权扩展工具；Lead/全局/安装/普通 spawn 能力不可恢复。
- Mailbox 并发写、广播、ACK 幂等、回复落盘但 ACK 前崩溃的重放幂等、尾部隔离、脱敏和大小上限。
- tmux 当前会话归属与固定 wake token；iTerm2 受控 AppleScript split pane、私有 launcher、session ID/PID 关闭；in-process 虚拟线程和短任务竞态。
- TeamDelete 的未读/dirty 默认拒绝、精确 Worktree 归属、停止失败保留，以及 pane 已自行退出时 wake 警告不阻断安全清理。
- Coordinator 双锁、Lead 身份、动态工具求交、同一 Agent run 立即收窄、四阶段顺序和退出恢复。

## tmux 端到端证据

环境：WSL Ubuntu、tmux 3.4、OpenJDK 21.0.11；隔离 Git 仓库位于被主项目忽略的 `target/ch15-tmux-e2e`，会话名 `imio-ch15-final4`。

### 创建与真实成员 pane

- Lead pane 调用依次可见：`TeamCreate`、`agent`、`agent`。
- pane 列表可见 `%3|imiocode-e2e-tmux-final4-worker-a|java` 与 `%4|imiocode-e2e-tmux-final4-worker-b|java`。
- `team.json` 记录 3 名成员；worker-a/worker-b 后端均为 `tmux`、句柄分别 `%3`/`%4`、状态 `IDLE`。
- `git worktree list` 可见两个独立受管 Worktree 和临时分支；每位成员有独立 mailbox/transcript 文件。

### idle 续写与收敛

- Lead 调用 `SendMessage` 后仍为同一 `%3` pane 和同一 `worker-a` agent ID。
- worker-a transcript 包含两组按 correlation ID 对应的 `MAILBOX → USER → ASSISTANT → IDLE`，第二轮历史在第一轮之后追加。
- Lead 调用 `TeamConverge (1.0s)`，Provider 观察到报告并返回 `TEAM_CONVERGENCE_REPORT_OBSERVED`。

### Coordinator Mode

- 配置 `teams.coordinator-enabled: true`，环境 `IMIO_COORDINATOR_MODE=true`，双锁成立。
- `CoordinatorMode` 执行后的同一 Agent run 中，Provider 检查下一请求工具列表：不存在 `bash`，返回 `COORDINATOR_RESTRICTED_SAME_TURN`。
- 四次 `CoordinatorAdvance` 按 Research → Synthesis → Implementation → Verification → Off 执行，均成功；非法跳级由自动化负例覆盖。

### 删除竞态与最终清理

- 首次真实 TeamDelete 暴露“成员已停止但 wake 告警阻断删除”的竞态；修复后用原保留现场重启应用，终端显示恢复 `e2e-tmux-final4` 共 3 名成员。
- 重新确认高风险操作后 `TeamDelete (6.9s)` 成功。
- 最终证据：团队目录不存在；`git worktree list` 只剩主仓库；final4 临时分支为零；final4 lock 为零；成员 pane 为零。
- 输入 `/exit` 后精确执行 `tmux kill-session -t imio-ch15-final4`；最终无 tmux session、ImioCode 成员进程或 mock Provider 残留。

## 平台说明

- tmux 与 in-process 完成了真实平台运行。
- 当前机器不是 macOS，iTerm2 无法做实机运行；已依据 iTerm2 官方 scripting 契约实现 split pane，并以参数捕获契约测试验证启动、句柄、失败保留与清理。非 macOS 探测会 fail-closed。

## 需求覆盖

- AC1—AC2：团队持久化、创建/spawn/删除及回滚测试 + tmux 重启恢复。
- AC3：三后端契约；tmux/in-process 实跑；iTerm2 macOS 契约测试。
- AC4—AC6：Task/工具隔离、Mailbox、idle/transcript/续写自动化 + E2E。
- AC7：`TeamConverge` 报告与删除后进程/pane/Worktree/分支/锁零残留。
- AC8：Coordinator 双锁、工具收窄、四阶段与恢复。
- AC9：619 项全量测试、fat JAR、diff check。
- AC10：两个队员、Mailbox 续写、Coordinator、收敛与清理完整 tmux 流程。
