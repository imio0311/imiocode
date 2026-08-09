# CH14 Git Worktree 隔离系统 Spec

## 背景

ImioCode 的 CH13 已提供定义式子 Agent、Fork、后台任务和 `isolation` 声明，但当前只允许 `isolation: none`。所有 Agent 的文件与命令工具仍绑定同一个工作区，并行写入时会相互覆盖。CH14 引入 Git Worktree 生命周期管理，使手动工作区切换和子 Agent 隔离共用同一套安全、恢复与清理协议。

## 目标

- 为 Git 仓库提供安全、完整、可恢复的 Worktree 创建、进入、退出、列出、删除和清理能力。
- 让声明 `isolation: worktree` 的每个子 Agent 使用独立工作目录、独立文件工具和独立命令工作目录。
- 对 LLM 提供的名称、路径和清理请求执行 fail-closed 校验，避免路径遍历和误删分支。
- 保留有价值的未合并改动，由用户决定后续 merge、cherry-pick 或丢弃。

## 功能需求

- F1：系统应验证所有 Worktree slug。仅允许 ASCII 字母、数字、点、下划线和连字符；总长 1—64；不得以点开头、以点结尾、包含连续 `..`、路径分隔符、盘符、绝对路径或空白。系统生成的子 Agent slug 也必须通过同一验证器。
- F2：系统应把合法 slug 映射为 `.imiocode/worktrees/<slug>` 和 `worktree-<slug>` 分支；任何规范化后的目录和分支必须仍属于当前仓库与受管命名空间。
- F3：WorktreeManager 应覆盖 Create、Enter、Exit、List、Remove、AutoCleanup 和 StaleCleanup。Git 操作必须使用参数数组直接启动进程，不经过 Shell，并具有超时、输出上限和安全错误。
- F4：Create 应从原始工作区当前 HEAD 创建独立分支和 Worktree。slug 已有受管分支时可重新挂载该分支；目录、分支或 Git 元数据冲突时明确拒绝，不覆盖现有内容。
- F5：创建成功后应执行四类设置：复制本地配置；设置该 Worktree 的 Git hooks 路径；为配置的依赖目录创建链接；按配置的 include 规则复制被 Git 忽略但运行必需的文件。非关键设置失败应给出警告并保留已创建 Worktree，关键 Git 设置失败应回滚本次创建。
- F6：手动 Enter 应激活一个已存在的受管 Worktree，保存 WorktreeSession，并通过受控运行时重建让配置、工具、权限沙箱、环境上下文、会话与指令全部绑定到新目录；不得依赖修改 `user.dir` 假装切换。
- F7：手动 Exit 默认执行 keep：返回原始工作区、清除活动会话记录但保留 Worktree 和分支。选择 remove 时必须先检查未提交改动和相对原始 HEAD 的提交；存在任一内容时，只有用户明确确认丢弃后才能删除。
- F8：Remove 应执行与 Exit remove 相同的 fail-closed 检查。任何 `git status`、提交差异、真实路径或分支归属检查失败都必须拒绝删除；不得删除非 `worktree-` 分支或受管目录之外的路径。
- F9：WorktreeSession 应原子持久化到原始工作区 `.imiocode/worktree-session.json`，包含原始目录、Worktree 目录、分支、原始分支、原始 HEAD、slug、创建时间和会话 ID。普通启动只提示存在可恢复记录；只有显式 `--resume` 才验证记录并恢复到 Worktree。
- F10：损坏、越界、仓库不匹配、Worktree 已消失或 Git 状态无法验证的恢复记录必须被拒绝，应用保持在原始工作区并显示安全诊断，不自动删除记录或目录。
- F11：`/worktree` 提供 `list`、`create <slug>`、`enter <slug>`、`exit [keep|remove]`、`remove <slug>` 五个子命令；危险删除必须通过现有终端确认接口，所有命令均为本地命令且不进入 Agent Loop。
- F12：进入或退出 Worktree 后，当前 JVM 应完成受控运行时重建；旧 Agent、后台任务、MCP、Hook 和文件句柄先关闭，新运行时再以目标工作区启动。切换不得遗留仍指向旧目录的可执行组件。
- F13：现有 AgentDefinition 的 `isolation: worktree` 应成为可执行能力。每次子 Agent 调用创建唯一 Worktree，不修改全局 WorktreeSession，不切换父进程目录，并把 Worktree 路径、分支和原始目录以系统提醒注入子 Agent。
- F14：Worktree 子 Agent 的核心文件工具、Bash 工作目录、权限沙箱、环境提醒、上下文溢出目录和项目配置必须绑定子 Worktree；MCP 等与工作区无关的工具可以复用，但不得借此访问父工作区文件。
- F15：子 Agent 结束后执行安全自动清理：无改动且无独有提交时删除 Worktree 和临时分支；存在改动、独有提交或检查失败时保留 Worktree，并在结果中返回路径、分支和保留原因，交给用户决定。
- F16：后台清理器应周期处理超过保留期的孤立 `agent-*` Worktree、零占用锁和 Git prune。只有非活动会话、无有效锁、无改动、无独有提交且不含远端独有状态的 Worktree才可自动删除。
- F17：List 应结合 `git worktree list --porcelain` 和受管元数据展示 slug、路径、分支、HEAD、活动状态与是否脏；不得展示配置内容、Prompt 或密钥。
- F18：CH2—CH13 的普通工作区、Session、Memory、Skill、Hook、MCP、权限、命令与 `isolation: none` 子 Agent 行为必须保持兼容。

## 非功能需求

- N1：所有路径删除前必须进行绝对路径规范化、真实路径、父目录和 Git 归属复核；检查失败默认拒绝。
- N2：Worktree 创建、删除和会话写入应串行化；并发子 Agent 使用唯一 slug 和文件锁，不得复用同一目录或分支。
- N3：Git 子进程、后台清理线程和 Worktree 锁均有界；应用关闭后不得遗留本进程持有的锁或清理线程。
- N4：Windows 与 Unix 都应支持。依赖链接在平台或权限不支持时降级为警告，不得导致主 Worktree 创建失败。
- N5：日志、UI、异常和持久化诊断不得包含 API Key、配置文件内容或完整 Git 命令环境。
- N6：配置缺省时使用安全值：Worktree 根目录 `.imiocode/worktrees`，过期时间 7 天，清理周期 1 小时，依赖链接目录为 `node_modules`、`.venv`、`venv`、`vendor`，复制本地配置但不复制会话和工具结果。
- N7：完整自动化测试之外，必须在 tmux 中启动真实 ImioCode，执行 `/worktree` 流程和真实子 Agent 隔离对话；若宿主环境无法让 tmux 驱动 Windows JVM，按既有项目约定记录等价真实子进程证据。

## 不做的事

- 不决定或自动执行 Worktree 之间的 merge、rebase、cherry-pick 或冲突解决。
- 不提供跨 Worktree 文件同步、补丁搬运或代码广播工具。
- 不实现 Agent Teams、同级 Agent 通信或多 Agent 编排。
- 不自动丢弃包含未提交改动或独有提交的 Worktree。
- 不管理用户在受管目录之外手工创建的 Worktree。

## 验收标准

- AC1：合法和恶意 slug 测试覆盖路径分隔符、`..`、盘符、绝对路径、边界长度和 Unicode；恶意输入均不能创建文件或分支。（F1、F2）
- AC2：真实临时 Git 仓库中可创建、列出、进入、保留退出、恢复和删除 Worktree；路径和分支映射稳定。（F3、F4、F6、F9、F11）
- AC3：本地配置、hooks、依赖链接和 include 文件按策略设置；可降级步骤失败时有警告且不泄密。（F5、N4、N5）
- AC4：普通启动只提示恢复记录；`--resume` 对合法记录进入 Worktree，对损坏或越界记录安全拒绝。（F9、F10）
- AC5：有未提交改动或独有提交时，Exit remove、Remove 和自动清理均拒绝无确认删除；Git 检查失败同样拒绝。（F7、F8、F15、N1）
- AC6：`/worktree` 五个子命令均本地执行，参数错误稳定，进入/退出后新运行时的 cwd、工具根和状态均为目标目录。（F11、F12）
- AC7：两个并行 `isolation: worktree` 子 Agent 获得不同目录和分支；各自在同名文件写入不同内容而互不覆盖，父工作区不变。（F13、F14、N2）
- AC8：干净子 Agent Worktree 自动删除；有改动或提交的 Worktree 被保留并在结果中明确返回位置和原因。（F15）
- AC9：过期清理只删除满足全部安全条件的孤立 Worktree，活动会话、有效锁、脏目录、有独有提交或检查失败对象均保留。（F16）
- AC10：全量测试和 fat JAR 构建通过，现有 `isolation: none` 与 CH2—CH13 行为无回归。（F18）
- AC11：真实终端端到端完成手动生命周期、`--resume` 和子 Agent 隔离场景，退出后无遗留本进程锁或后台线程。（N3、N7）
