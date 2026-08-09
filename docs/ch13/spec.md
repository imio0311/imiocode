# CH13 多 Agent 与后台任务 Spec

## 背景

IMIOCode 已具备单 Agent 循环、工具与权限系统、Skill 的隔离 Fork、Hook 和上下文管理，但所有复杂任务仍由一个 Agent 承担。Skill Fork 只能执行 Skill，不能让模型按任务选择独立角色，也没有后台任务、父子 Trace 或统一管理入口。本章把现有 Fork 能力提升为通用的子 Agent 运行平台。

## 目标

- 主 Agent 可通过一个统一工具把工作委派给定义式子 Agent或基于当前会话的 Fork 子 Agent。
- 每个子 Agent 拥有独立上下文、模型选择、工具视图、权限上限、超时和循环预算。
- 同步子任务直接返回最终结果；后台子任务可继续运行、查询、取消并异步回传结果。
- 所有父子调用均可在当前进程内追踪状态、Token 用量和调用链。
- 项目、用户、内置和插件来源可以无编译地扩展 Agent 定义。

## 功能需求

- F1：系统应解析 YAML frontmatter + Markdown 正文的 Agent 定义；名称、描述、模型、权限模式、轮数、超时、工具允许/禁止集合、后台能力、初始提示等字段在加载时完成校验。
- F2：系统应按“项目级 > 用户级 > 内置级 > 插件级”加载同名定义；高优先级无效定义给出诊断并回退下一份有效定义，刷新采用完整不可变快照。
- F3：系统提供 `agent` 统一工具，至少接收任务描述与任务 Prompt；存在 `subagent_type` 时走定义式路径，缺省时走 Fork 路径。两条路径共享结果、Trace、超时和后台协议。
- F4：Fork 路径应复用父 Agent 完整、合法的对话历史作为消息前缀，保持内容和顺序稳定，并只在尾部追加 Fork 边界提醒与子任务，便于 Provider Prompt Cache 命中；不得把子 Agent 中间轨迹写回父会话。
- F5：定义式路径使用独立上下文，在第一条任务消息之前注入定义正文和可选 `initialPrompt`，完成后只返回最终结果。
- F6：模型选择优先级为“工具调用覆盖 > Agent 定义 > 父 Agent 模型”。`haiku` 是可配置逻辑别名；没有映射时安全回退父模型并产生可观测提示，不阻止 Explore 使用。
- F7：RunToCompletion 应非交互式运行 ReAct 循环，遵守最大轮数、任务超时、取消、未知工具熔断和 Agent 既有终止条件；任何未解决的交互式权限请求在后台任务中自动拒绝。
- F8：子 Agent 工具可见性依次应用全局禁止集合、自定义额外禁止集合、后台白名单、定义 `tools` 允许集合、定义 `disallowedTools` 禁止集合及权限模式上限；任一层禁止后不得在工具 Schema 或执行路径中重新出现。
- F9：后台任务由 TaskManager 管理，状态包含 pending、running、completed、failed、cancelled、timed_out；支持后台启动、自动超时、取消、关闭时回收和前台运行中通过 ESC 转入后台。
- F10：后台任务完成后应产生一次有界 `task-notification`，在 UI 安全点显示，并在父 Agent 下一次模型请求前以消息上下文注入；重复 drain 不得重复回传。
- F11：TraceRegistry 应为每次父任务和子任务生成稳定 ID，记录父子关系、Agent 类型、模型、前后台、开始/结束时间、状态、终止原因及输入/输出/推理/缓存 Token 汇总；Trace 只保存在当前进程。
- F12：系统提供 `/tasks`、`/task info <id>`、`/task cancel <id>` 本地命令，不进入 Agent Loop；列表和详情不得泄露 Prompt、工具参数或密钥。
- F13：内置 `explore`、`plan`、`general-purpose` 三个定义使用与用户定义相同的解析和加载流程。Explore 使用 `haiku` 逻辑模型且只读；Plan 只读并输出计划；general-purpose 继承正常工具与权限能力。
- F14：`cwd` 只能位于当前工作区内；`isolation` 在本章只接受 `none`。请求 `worktree` 必须明确返回“下一章支持”，不得静默假装隔离。
- F15：现有 Skill Fork 应迁移或适配到统一 RunToCompletion 基础设施，且 CH2—CH12 的会话、权限、Hook、Skill、MCP 和 Slash Command 行为保持兼容。

## 非功能需求

- N1：定义目录摘要只包含名称和描述；正文按需加载，避免常驻 Prompt 膨胀。
- N2：后台执行池、任务表、通知队列和输出均有界；应用关闭后不得遗留子 Agent、权限等待或命令进程。
- N3：Agent 定义和 `cwd` 加载拒绝绝对逃逸、`..` 与符号链接越界；诊断和 Trace 统一脱敏。
- N4：父历史必须深复制为不可变快照，父子并发修改互不影响；父 Agent 与后台子 Agent 不共享可取消的活动 LLM 请求状态。
- N5：所有过滤层默认拒绝优先，未知模型别名、未知 Agent、未知工具与非法定义均产生稳定安全错误。
- N6：Windows 与 Unix 行为一致；Windows 无 tmux 时允许用真实交互子进程做等价端到端验收。

## 不做的事

- 不实现 Worktree 文件系统隔离；`isolation: worktree` 仅返回明确的不支持错误。
- 不实现 Agent Team、同级 Agent 通信、共享团队邮箱或协作编排。
- 不持久化 Trace、Task 或父子链路，重启后清空。
- 不实现跨机器任务队列、分布式 Worker 或远程 Agent。

## 验收标准

- AC1：四来源同名定义只暴露最高优先级有效版本；损坏高优先级文件会诊断并回退。（F1、F2）
- AC2：模型通过同一个 `agent` 工具分别完成定义式和 Fork 调用；参数冲突和未知类型明确失败。（F3）
- AC3：Fork 的实际 Provider 请求保留父历史稳定前缀，子轨迹不进入父历史，并能观察到缓存用量字段。（F4）
- AC4：定义式 Agent 的正文和 initialPrompt 位于任务之前，模型三级覆盖和 haiku 回退均可观察。（F5、F6）
- AC5：同步子 Agent 能运行多轮工具并返回最终文本；超时、取消、最大轮数和权限 ASK 均确定终止而不挂起。（F7）
- AC6：对每层工具过滤分别构造样例，Schema 和执行解析都无法越过禁止层；后台默认只暴露白名单工具。（F8）
- AC7：后台任务可启动、列出、查看、取消；前台任务可由 ESC 转后台；完成通知只显示和注入一次。（F9、F10、F12）
- AC8：Trace 可还原父子链路并汇总 Token；取消、失败、超时状态准确，且重启不恢复旧 Trace。（F11）
- AC9：三个内置 Agent 可列出并调用，Explore/Plan 无写命令能力，general-purpose 遵守父权限链。（F13）
- AC10：越界 cwd 和 worktree isolation 被拒绝，工作区文件不受影响。（F14）
- AC11：Skill Fork、权限、Hook、MCP、Session、Memory 与既有命令测试全部通过。（F15、N5）
- AC12：全量测试、fat JAR 构建和真实终端端到端场景通过，退出后无遗留子进程或后台线程。（N2、N4、N6）
