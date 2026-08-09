# CH13 验收报告

## 自动化验证

- Java 21 `mvn -q clean test`：549 tests，0 failures，0 errors，3 skipped。
- `mvn -q -DskipTests package`：生成 `target/imiocode-0.2.0-SNAPSHOT-all.jar`。
- `git diff --check`：通过，仅报告工作区既有的 CRLF 转换提示。
- `config.example.yaml` 由真实 `ConfigLoader.loadAll` 加载测试覆盖。

## 端到端验证

- 定义式路径：主模型调用唯一 `agent` 工具并指定 `subagent_type=explore`；Explore 在独立上下文中使用只读工具，返回三个内置定义。
- Fork 路径：主模型调用 `agent` 且不传 `subagent_type`；Fork 从完整父历史正确恢复测试口令 `ORCHID`。
- 后台路径：`run_in_background=true` 立即返回 task ID，`/tasks` 显示 running；通知、终态和重复 drain 由 TaskManager 单元测试覆盖。
- 命令路径：`/tasks` 本地返回任务列表，未知 `/task info` ID 返回稳定本地错误，不触发模型请求。
- 安全路径：验收中发现 `agent` 调度入口曾被文件沙箱误判；修复后只放行调度动作，子 Agent 的真实工具调用仍经过工具过滤、权限检查与路径沙箱。

## 环境说明

Windows 宿主没有原生 tmux；WSL 虽有 tmux，但该环境禁用了 Windows 可执行文件的 tmux TTY interop。因此按 Spec N6 使用真实 Windows CLI 子进程完成等价端到端验证。测试结束后已关闭临时 tmux 会话，无遗留任务。
