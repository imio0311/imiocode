# CH14 Git Worktree 隔离系统 Tasks

## 文件清单

| 操作 | 文件/目录 | 职责 |
|---|---|---|
| 新建 | `src/main/java/io/imiocode/worktree/**` | 安全、Git、生命周期、持久化、启动切换 |
| 新建 | `src/main/java/io/imiocode/command/builtin/WorktreeCommand.java` | `/worktree` 五个子命令 |
| 新建 | `src/main/java/io/imiocode/subagent/runtime/SubagentToolRegistryFactory.java` | Worktree 专属工具注册表 |
| 新建 | `src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java` | 子 Agent 隔离提醒 |
| 修改 | `ImioCodeApplication.java` | 配置 Worktree 系统、启动恢复和运行时切换 |
| 修改 | `config/*` | `worktrees:` 配置映射 |
| 修改 | `command/*`、`runtime/ConversationLoop.java` | 命令服务和切换结果 |
| 修改 | `subagent/runtime/*` | 执行 Worktree lease |
| 修改 | `tool/ToolRegistry.java` | 安全复制并替换工作区工具 |
| 修改 | `README.md`、`config.example.yaml` | 用户文档与配置示例 |
| 新建 | `src/test/java/io/imiocode/worktree/**` | 临时 Git 仓库集成测试 |
| 修改/新建 | `src/test/java/io/imiocode/subagent/**` | 并行隔离与清理测试 |

## T1：Slug 与名称映射

**依赖：** 无
**步骤：** 实现严格验证器、分支/目录生成器和唯一 agent slug；覆盖恶意输入。
**验证：** 定向测试确认所有路径遍历、Unicode、边界长度输入均被拒绝且没有文件副作用。

## T2：无 Shell Git 执行层

**依赖：** T1
**步骤：** 实现 ProcessBuilder 参数数组、超时、取消、输出限制与安全异常；封装 worktree/status/log/branch/prune。
**验证：** 临时仓库测试真实执行 Git；含空格路径和失败命令均得到稳定结果。

## T3：Worktree 配置

**依赖：** T1
**步骤：** 新增 WorktreeConfig、YAML 映射、默认值、环境无关路径校验和 example 配置。
**验证：** ConfigLoader 默认和完整配置测试通过，非法受管目录启动失败。

## T4：会话模型与原子存储

**依赖：** T1
**步骤：** 实现 WorktreeSession、JSON schema、原子写入、加载、清除和损坏诊断。
**验证：** 往返、损坏 JSON、越界路径、替换失败保持旧文件等测试通过。

## T5：创建后设置

**依赖：** T2、T3
**步骤：** 复制本地配置、设置 worktree hooks、链接依赖目录、复制 include；输出脱敏警告。
**验证：** 临时仓库中逐项观察目标文件、Git 配置和链接；不支持链接时验证安全降级。

## T6：生命周期 Create/List

**依赖：** T2、T4、T5
**步骤：** 实现仓库验证、分支创建/复用、目录冲突拒绝、porcelain 列表解析和真实路径复核。
**验证：** 真实仓库创建并列出多个 Worktree；重复、冲突和非 Git 目录失败。

## T7：安全检查与 Exit/Remove

**依赖：** T6
**步骤：** 汇总 status、独有提交、远端状态；实现 keep、remove、显式 discard 和 fail-closed 删除。
**验证：** clean、dirty、untracked、独有 commit、Git 失败、越界路径六类测试逐一通过。

## T8：自动与过期清理

**依赖：** T7
**步骤：** 实现 lease lock、单线程清理器、过期扫描、零占用 lock 清理和 `git worktree prune`。
**验证：** 可控 Clock 测试证明只删除满足全部条件的过期 agent Worktree，关闭后线程退出。

## T9：启动参数与恢复

**依赖：** T4、T6
**步骤：** 解析 `--resume`，验证 session 与 Git 状态，普通启动生成提示，恢复失败回到原目录。
**验证：** 参数解析和 bootstrap 测试覆盖无参数、resume、未知参数、合法/损坏/过期记录。

## T10：`/worktree` 命令

**依赖：** T7、T9
**步骤：** 实现 list/create/enter/exit/remove、参数校验、危险确认和本地输出。
**验证：** 命令测试确认不调用 Agent；删除确认拒绝时无副作用。

## T11：受控 WorkspaceRuntime 切换

**依赖：** T9、T10
**步骤：** 引入 transition controller；让 ConversationLoop 返回切换意图；拆出一次性 runtime 装配和完整关闭顺序。
**验证：** 进入/退出集成测试确认新 Agent、工具、环境和 session 的 workspace 均改变，旧资源已关闭。

## T12：Worktree 专属子 Agent 工具

**依赖：** T6
**步骤：** 扩展 ToolRegistry 安全复制；替换六个工作区工具；按 workdir 创建权限、环境和上下文组件。
**验证：** scoped registry 测试确认同名相对路径分别解析到不同 Worktree，绝对父路径仍被拒绝。

## T13：SubAgent executeWithWorktree

**依赖：** T8、T12
**步骤：** 接通 isolation 分支、唯一 lease、隔离提醒、结果附加保留信息和安全自动清理。
**验证：** 两个并行子 Agent 在不同 Worktree 修改同名文件，父目录不变；clean 自动清理、dirty 保留。

## T14：文档与兼容回归

**依赖：** T11、T13
**步骤：** 更新 README、内置 Agent 定义和 CH14 验收说明；修复全部旧测试。
**验证：** `mvn test`、fat JAR、`git diff --check` 通过。

## T15：真实终端验收

**依赖：** T14
**步骤：** 在 tmux 启动 ImioCode，执行手动 create/list/enter/exit/remove、`--resume` 和真实子 Agent 隔离请求，逐项记录证据。
**验证：** checklist 全部勾选；退出后无 agent lock、清理线程或 Git worktree 锁残留。

## 执行顺序

```text
T1 -> T2 -> T5 -> T6 -> T7 -> T8
  \-> T3 -/       \-> T12 -> T13
  \-> T4 ------------> T9 -> T10 -> T11
T11 + T13 -> T14 -> T15
```
