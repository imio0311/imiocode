# CH14 验收报告

日期：2026-08-10

## 自动化验证

- `mvn test`（Java 21）：582 tests，0 failures，0 errors，3 skipped；共 164 个测试套件。
- 新增覆盖：slug 攻击面、真实 Git Worktree 生命周期、创建后复制/hooks/依赖链接、原子 session、显式恢复、路径篡改 fail-closed、跨进程 lease lock、五个 `/worktree` 子命令、scoped 工具注册表、两个真实 Agent 并发写同名文件。
- `mvn -DskipTests package`：生成 `target/imiocode-0.2.0-SNAPSHOT-all.jar`，大小 5,305,670 bytes。
- `git diff --check`：通过。

## tmux 端到端

环境：WSL Ubuntu、tmux 3.4、OpenJDK 21.0.11；临时仓库位于 `target/ch14-e2e-repo`。

### 生命周期与运行时重建

1. 启动 fat JAR，拒绝可选 MCP Server 后进入 Ready。
2. `/worktree create demo`：观察到路径 `.imiocode/worktrees/demo`、分支 `worktree-demo`。
3. `/worktree list`：观察到 `demo`、分支、idle/dirty 和绝对路径。
4. `/worktree enter demo`：当前运行时退出，欢迎面板以 demo 路径重新出现；MCP 启动确认和会话 ID 重新生成。
5. `/worktree exit keep`：欢迎面板返回原仓库，Worktree 与分支保留。

### 显式恢复

1. 再次 enter 后直接 `/exit`，确认 `.imiocode/worktree-session.json` 保留 `demo/worktree-demo`。
2. 普通启动：欢迎面板仍为原仓库，并显示“使用 --resume 显式恢复”。
3. `java -jar ... --resume`：欢迎面板目录恢复到 demo Worktree。

### 确认删除与资源释放

1. 对 dirty demo 执行 `/worktree exit remove`，输入 `n`：目录和 session 均保留。
2. 再次执行并输入 `y`：目录、session 和 `worktree-demo` 分支消失；Git porcelain 只剩主工作区。
3. `/exit` 后 `pgrep -x java` 和 `*.lock` 扫描均为空。

### 真实子 Agent 对话

输入：`You must call the explore subagent to inspect README.md carefully and summarize the project name, Java version, and all CH14 worktree commands. Do not inspect files directly.`

观察结果：

- 主 Agent 显示 `agent` 工具成功完成（20.3s），回复正确给出 ImioCode、Java 21 和五个 `/worktree` 子命令。
- 子 Agent 运行期间轮询捕获 `.imiocode/worktrees/agent-explore-840720d1`、分支 `worktree-agent-explore-840720d1` 和 `agent-explore-840720d1.lock`，证明省略 isolation 参数时继承内置 `explore` 的 worktree 定义。
- 对话完成后 `git worktree list --porcelain` 只剩主工作区。
- `worktree-agent-*` 分支和 `.imiocode/worktree-locks/*.lock` 均为空，证明 clean 子 Worktree 自动清理。

## 结论

Checklist 33/33 通过。CH14 明确不实现 Worktree 合并策略、跨 Worktree 同步和 Agent Teams 并行编排。
