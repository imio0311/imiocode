# CH14 Git Worktree 隔离系统 Checklist

> 2026-08-10 已按代码、真实 Git 仓库测试和 tmux 端到端场景逐项验收。详细命令与结果见 `validation.md`。

## 安全与命名

- [x] 合法 slug 映射到受管目录和 `worktree-` 分支。（验证：slug 与真实 Git 集成测试通过）
- [x] 路径分隔符、`..`、盘符、绝对路径、空白、Unicode、尾点和超长 slug 全部拒绝。（验证：14 个参数化用例通过）
- [x] Remove 前复核规范路径、真实路径、受管根、分支前缀和 Git 注册。（验证：伪造 session 指向外部目录时 fail-closed，外部文件保持不变）
- [x] Git 命令使用参数数组且不经过 Shell，并具备超时、取消和有界输出。（验证：GitCommandRunner/GitWorktreeClient 测试通过）

## 生命周期

- [x] Create 从当前 HEAD 创建独立 Worktree，重复和目录冲突不覆盖。（验证：临时仓库集成测试与 tmux create）
- [x] List 展示 slug、路径、分支、HEAD、active 和 dirty 状态，不输出敏感配置。（验证：命令测试与 tmux list）
- [x] Enter 持久化 session，并通过完整运行时重建切换所有工作区组件。（验证：tmux 中欢迎面板目录、会话 ID 与 MCP 启动流程均重建）
- [x] Exit keep 返回原目录并保留 Worktree/分支。（验证：tmux exit keep 后 Git 元数据仍存在）
- [x] Exit remove/Remove 对 dirty、untracked、独有提交和检查失败默认 fail-closed。（验证：生命周期测试）
- [x] 明确确认 discard 后仅删除目标受管 Worktree 与本地分支。（验证：tmux 先输入 `n` 保留，再输入 `y` 删除；主分支仍存在）

## 创建后设置

- [x] `config.yaml` 和本地配置按白名单复制，不复制 session、tool-results。（验证：post-creation 集成测试）
- [x] Worktree 使用仓库公共 hooks 路径，且配置为 worktree 级别。（验证：`git config --worktree --get core.hooksPath`）
- [x] 依赖目录尽力创建软链接；平台不支持时返回可见警告且 Worktree 可用。（验证：平台适配分支测试）
- [x] copy-includes 只复制工作区内、被 Git 忽略的普通文件，不跟随符号链接。（验证：`.env.local` 集成场景）

## 恢复与运行时切换

- [x] session 以原子 JSON 写入并包含恢复所需全部字段。（验证：存储往返与损坏 JSON 测试）
- [x] 普通启动仅提示记录，显式 `--resume` 才恢复。（验证：同一 tmux 会话两次真实启动对比目录）
- [x] 损坏、仓库不匹配、越界或 Git 状态不一致的 session 被拒绝。（验证：bootstrap/session/safety 测试）
- [x] 切换时关闭旧 Task、Agent/LLM、MCP、Hook、终端资源，再按新路径重建。（验证：外层 runtime loop 与 tmux 重建时新会话 ID/MCP 启动）

## SubAgent 隔离

- [x] `isolation: none` 保持 CH13 行为。（验证：全量旧 Subagent 测试通过）
- [x] `isolation: worktree` 创建唯一目录/分支并注入包含真实路径的隔离通知。（验证：SubagentWorktreeIsolationTest）
- [x] 文件、Bash、权限沙箱、环境、Hook 和上下文均绑定子 Worktree。（验证：scoped registry 与真实 Agent 工具循环）
- [x] 两个并发子 Agent 修改同名相对文件时互不覆盖，父工作区不变。（验证：两个线程、两个真实 Agent、两个不同 Worktree）
- [x] clean 子 Worktree 自动删除；dirty 或独有提交保留并返回路径、分支和原因。（验证：lease 三种终态测试）

## 自动清理

- [x] 过期清理跳过活动 session、进程内 lease、跨进程 live lock、未过期、dirty、独有提交和检查失败对象。（验证：固定 Clock 与真实 FileLock 测试）
- [x] 安全孤立的 agent Worktree、临时分支和零占用 lock 被清理，并执行 Git prune。（验证：清理前后 porcelain 对比）
- [x] 应用关闭后调度器终止且文件锁释放。（验证：单元测试与 tmux 退出后的进程/lock 探测）

## 编译、回归与端到端

- [x] Java 21 `mvn test` 全部通过。（结果：582 tests，0 failures，0 errors，3 个既有平台条件跳过）
- [x] `mvn -DskipTests package` 生成可运行 fat JAR。（结果：`target/imiocode-0.2.0-SNAPSHOT-all.jar`，5,305,670 bytes）
- [x] `git diff --check` 通过，用户已有的 `claude.md`、`.imiocode/`、`hello.txt` 未纳入 CH14 提交。
- [x] tmux 场景：create → list → enter → exit keep，观察完整运行时目录切换与保留。
- [x] tmux 场景：普通启动只提示；`--resume` 恢复到记录目录。
- [x] tmux 场景：真实 DeepSeek 对话调用 `explore` 子 Agent；运行中捕获唯一 agent Worktree/分支/live lock，结束后全部自动清理。
- [x] tmux 场景：dirty Worktree 删除先拒绝再确认，退出后无 Java 进程和残留 lock。
