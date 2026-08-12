# CH16 生产代码中文注释 Tasks

> 状态：已完成（2026-08-12），37/37 完成。

## 文件清单

| 操作 | 文件 | 职责 |
|---|---|---|
| 修改 | `src/main/java/io/imiocode/**/*.java` | 仅补充中文 Javadoc 与非显然逻辑说明 |
| 新建 | `docs/ch16/comment-audit.md` | 记录 561 个生产文件的审阅覆盖与处理统计 |
| 新建 | `docs/ch16/checklist.md` | 定义可观测验收项 |
| 新建 | `docs/ch16/acceptance-report.md` | 记录实际测试和端到端证据 |
| 不修改 | `src/test/**/*.java` | 测试只用于验证 |

## 通用执行约束

T03—T33 是按依赖排序的原子工作单元，单次只处理一个核心类型组或一次可重复的模块清单复核。每个任务都必须完整执行：

1. 完整阅读本任务拟修改的核心类型并追踪主要调用方；其余文件使用类型声明、现有 Javadoc、文件规模和结构扫描完成初步分类，仅对可疑项人工复核。
2. 将范围内每个文件判定为“新增注释”“保留现有注释”或“无需补充”，同步更新审阅清单；清单复核任务只校验分类和抽样，不重复修改核心类型。
3. 只添加独立的 Javadoc/行注释行；不修改签名、注解、常量、表达式、控制流或现有代码行。
4. 注释解释职责、设计原因和约束，不复述名称显然的代码。
5. 每批执行 `git diff --check -- <任务路径>`，期望退出码为 0。

## T01：建立基线与完整文件清单

**文件：** `docs/ch16/comment-audit.md`
**依赖：** 已批准的 `spec.md`、`plan.md`

**步骤：**

1. 扫描 `src/main/java` 下全部 Java 文件，记录路径、模块和行数。
2. 确认基线为 561 个生产文件，并按 21 个目录模块和 2 个顶层文件汇总。
3. 记录改动前 Git 提交号、测试基线 619 项和用户未提交文件清单。
4. 把 561 条完整相对路径写入审阅清单，初始状态设为“待审阅”。

**验证：** 重新扫描路径并与清单去重比较，期望源文件数、清单数和唯一数均为 561，差集为空。

## T02：建立只允许注释变化的差异检查

**文件：** `docs/ch16/comment-audit.md`
**依赖：** T01

**步骤：**

1. 以当前 `HEAD` 的 `src/main/java` 为代码基线。
2. 定义差异审计规则：生产代码差异中的新增或删除内容只能是空白、Javadoc 或独立行注释。
3. 禁止在原代码行尾追加注释，以便机械检查准确判断程序行未变化。
4. 在审阅清单中记录检查命令和判定标准。

**验证：** 在尚未修改生产代码时运行差异检查，期望生产代码差异为空且规则自检通过。

## T03：注释应用入口

**文件：** `ImioCodeApplication.java`、`TeamMemberProcess.java`
**依赖：** T02

**步骤：** 注释主进程与成员进程职责、依赖装配顺序、启动参数边界、恢复和关闭顺序；复核所有其余入口逻辑。

**验证：** 编译通过，两个文件在审阅清单中均不再是“待审阅”。

## T04：注释 Agent Loop 核心

**文件：** `agent/Agent.java`、`agent/AgentLoop*.java` 及其直接策略类型
**依赖：** T03

**步骤：** 注释循环推进、终止条件、工具结果回灌、动态策略刷新、重试与错误边界；审阅同范围简单类型。

**验证：** `git diff --check -- src/main/java/io/imiocode/agent` 通过。

## T05：注释流式工具调度

**文件：** `agent/StreamingToolScheduler.java` 及工具调用组装、批次、结果类型
**依赖：** T04

**步骤：** 注释流式分片、并发上限、串并行判定、稳定结果顺序、取消和收尾语义；审阅相关数据类型。

**验证：** Agent 流式调度定向测试通过。

## T06：复核 Agent 模块剩余文件

**文件：** `agent/` 中未被 T04—T05 覆盖的全部文件
**依赖：** T05

**步骤：** 运行 Agent 文件分类扫描，复核无现有 Javadoc且并非简单 record/enum/exception 的候选，按需注释候选并更新模块统计。

**验证：** 审阅清单中 `agent` 模块 28/28 已审阅，Agent 相关测试通过。

## T07：注释会话协调主流程

**文件：** `runtime/`、`conversation/ConversationCoordinator.java`、`conversation/ConversationSession.java`、运行时策略类型
**依赖：** T06

**步骤：** 注释单轮编排、命令与 Agent 分流、运行时重建、会话提交、工具策略求交和关闭生命周期。

**验证：** Runtime 与 Conversation 定向测试通过。

## T08：复核 Conversation 与 Prompt

**文件：** `conversation/` 剩余文件、`prompt/` 全部文件
**依赖：** T07

**步骤：** 注释消息转换、System Prompt 组装、模式提示注入和缓存稳定性；随后运行两个模块的分类扫描并复核候选。

**验证：** 两模块共 45/45 已审阅，Prompt 相关测试通过。

## T09：注释 Context 管理

**文件：** `context/` 全部文件
**依赖：** T08

**步骤：** 注释预算估算、自动压缩阈值、工具结果外置、恢复提示和容量边界；审阅简单载体。

**验证：** `context` 21/21 已审阅，Context 定向测试通过。

## T10：注释配置加载核心

**文件：** `config/ConfigLoader.java`、`ConfigDocument.java`、`RuntimeConfig.java` 及配置来源合并类型
**依赖：** T09

**步骤：** 注释配置优先级、环境变量展开、敏感值处理、兼容回退和整段安全降级。

**验证：** Config Loader 定向测试通过。

## T11：复核配置模型

**文件：** `config/` 中未被 T10 覆盖的全部文件
**依赖：** T10

**步骤：** 运行配置模型分类扫描；只复核语义不直观的候选配置类型、默认值和约束，简单 record/enum 直接记录为无需补充。

**验证：** `config` 24/24 已审阅，Config 全部测试通过。

## T12：注释权限决策链

**文件：** `permission/` 顶层权限模式、规则、请求、评估和确认类型
**依赖：** T11

**步骤：** 注释规则优先级、默认决策、模式差异、HITL 边界与高风险请求构造。

**验证：** Permission 决策相关测试通过。

## T13：注释命令风险检测

**文件：** `permission/command/` 全部文件
**依赖：** T12

**步骤：** 注释安全命令判定、Shell 分段、拒绝条件和 fail-closed 原因；不把规则描述成完整 Shell 解析器。

**验证：** 命令风险检测定向测试通过。

## T14：注释路径沙箱并复核权限模块

**文件：** `permission/sandbox/` 及 `permission/` 剩余文件
**依赖：** T13

**步骤：** 注释路径规范化、符号链接、受管根、越界拒绝和 TOCTOU 限制；完成权限模块 40 个文件审阅。

**验证：** `permission` 40/40 已审阅，Permission 与 Sandbox 全部测试通过。

## T15：注释通用持久化与 Session 事务

**文件：** `persistence/`、`session/JsonlSessionStore.java`、`SessionMessageCodec.java` 及事务恢复类型
**依赖：** T14

**步骤：** 注释原子写入、JSONL 提交边界、损坏尾部隔离、工具调用 ID 恢复和锁语义。

**验证：** Persistence 与 Session Store 定向测试通过。

## T16：复核 Session 模块

**文件：** `session/` 中未被 T15 覆盖的全部文件
**依赖：** T15

**步骤：** 运行 Session 文件分类扫描并复核候选；按需注释索引、保留策略和恢复选择，简单事务 record 记录为无需补充。

**验证：** `persistence` 5/5、`session` 22/22 已审阅，Session 全部测试通过。

## T17：注释 OpenAI Provider

**文件：** `llm/provider/openai/` 及直接使用的共享 LLM 类型
**依赖：** T16

**步骤：** 注释请求映射、SSE 事件、Tool Call 分片、finish reason、Usage 和错误响应处理。

**验证：** OpenAI Provider 定向测试通过。

## T18：注释 Anthropic Provider

**文件：** `llm/provider/anthropic/` 及其协议模型
**依赖：** T17

**步骤：** 注释 content block 生命周期、tool_use 增量、停止原因和与 OpenAI 格式的边界差异。

**验证：** Anthropic Provider 定向测试通过。

## T19：注释 DeepSeek 并复核 LLM 模块

**文件：** `llm/provider/deepseek/`、`llm/` 剩余文件
**依赖：** T18

**步骤：** 注释 reasoning_content、流式兼容点、模型请求抽象；完成 LLM 模块 21 个文件审阅。

**验证：** `llm` 21/21 已审阅，LLM 全部测试通过。

## T20：注释 MCP Client 与能力协商

**文件：** `mcp/client/`、MCP 初始化和协议类型
**依赖：** T19

**步骤：** 注释 initialize 握手、能力缓存、工具命名、超时、响应 ID 和关闭顺序。

**验证：** MCP Client 定向测试通过。

## T21：注释 MCP 传输

**文件：** `mcp/transport/` 全部文件
**依赖：** T20

**步骤：** 注释 stdio 进程隔离、Streamable HTTP、有限 SSE、响应上限、重定向和 Header 安全边界。

**验证：** MCP Transport 定向测试通过。

## T22：复核 MCP 模块

**文件：** `mcp/` 中未被 T20—T21 覆盖的全部文件
**依赖：** T21

**步骤：** 运行 MCP 文件分类扫描，复核配置与工具适配候选；简单 JSON-RPC 协议数据类型记录为保留现有注释或无需补充。

**验证：** `mcp` 39/39 已审阅，MCP 全部测试通过。

## T23：注释 Worktree 生命周期

**文件：** `worktree/lifecycle/`、`WorktreeManager` 及 Git 操作类型
**依赖：** T22

**步骤：** 注释目录/分支所有权、dirty 与独有提交检查、创建回滚、恢复和保守清理。

**验证：** Worktree 生命周期定向测试通过。

## T24：复核 Worktree 模块

**文件：** `worktree/` 剩余文件
**依赖：** T23

**步骤：** 运行 Worktree 文件分类扫描，复核路径、会话状态、复制/链接策略和锁边界候选，更新模块统计。

**验证：** `worktree` 23/23 已审阅，Worktree 全部测试通过。

## T25：注释 SubAgent 调度与隔离

**文件：** `subagent/` 中 dispatcher、runtime、filter 和 worktree 接入类型
**依赖：** T24

**步骤：** 注释前台/后台分流、工具白名单物理裁剪、模型别名、容量限制、通知和隔离生命周期。

**验证：** SubAgent 调度、过滤和 Registry 定向测试通过。

## T26：复核 SubAgent 模块

**文件：** `subagent/` 剩余文件
**依赖：** T25

**步骤：** 运行 SubAgent 文件分类扫描，复核 Agent 定义加载与任务状态候选；简单数据类型记录为无需补充。

**验证：** `subagent` 32/32 已审阅，SubAgent 全部测试通过。

## T27：注释 Team 持久化、Task 与 Mailbox

**文件：** `team/persistence/`、`team/task/`、`team/mailbox/`
**依赖：** T26

**步骤：** 注释跨进程锁、原子替换、任务图一致性、并发版本、消息先落盘、ACK/重放幂等和尾部隔离。

**验证：** Team Persistence、Task Graph、Mailbox 定向测试通过。

## T28：注释 Team 后端

**文件：** `team/backend/` 全部文件
**依赖：** T27

**步骤：** 注释 auto 选择、tmux 会话归属、iTerm2 split pane 契约、in-process 虚拟线程和失败回退/保留句柄。

**验证：** Team Backend 定向测试通过。

## T29：注释 Team 生命周期和成员 Worker

**文件：** `team/runtime/`、`team/tool/` 中生命周期与消息工具
**依赖：** T28

**步骤：** 注释创建/spawn 回滚、idle 续写、transcript、收敛报告、停止轮询、dirty 保留与精确清理。

**验证：** AgentTeamManager 集成测试和 Team Tool 测试通过。

## T30：注释 Coordinator 并复核 Team

**文件：** `team/coordinator/`、`team/model/`、`team/config/` 及剩余 Team 文件
**依赖：** T29

**步骤：** 注释双锁、Lead 身份、动态工具求交和四阶段状态机；运行 Team 文件分类扫描，复核剩余候选并更新统计。

**验证：** `team` 61/61 已审阅，Team 全部测试通过。

## T31：注释命令与工具框架

**文件：** `command/`、`tool/` 全部文件
**依赖：** T30

**步骤：** 注释本地命令分流、参数验证、权限接入、工具执行与结果呈现；运行 Command 与 Tool 分类扫描并复核候选。

**验证：** 两模块 59/59 已审阅，Command 与 Tool 全部测试通过。

## T32：注释 Terminal 与 Hook

**文件：** `terminal/`、`hook/` 全部文件
**依赖：** T31

**步骤：** 注释终端状态刷新、流式渲染、同步阻断、异步有界队列、条件表达式和错误策略；运行两模块分类扫描并复核候选。

**验证：** 两模块 63/63 已审阅，Terminal 与 Hook 全部测试通过。

## T33：注释 Instruction、Memory 与 Skill

**文件：** `instruction/`、`memory/`、`skill/` 全部文件
**依赖：** T32

**步骤：** 注释 include 边界、记忆脱敏/提取、来源白名单、下载容量和原子安装；运行三模块分类扫描并复核候选。

**验证：** 三模块 74/74 已审阅，Instruction、Memory 与 Skill 全部测试通过。

## T34：完成全量审阅与注释质量扫描

**文件：** `src/main/java/**/*.java`、`docs/ch16/comment-audit.md`
**依赖：** T03—T33

**步骤：**

1. 将全部模块统计汇总，确认 561 个文件均有处理结果。
2. 扫描新增注释中的 `TODO`、`TBD`、空 Javadoc、乱码和模板套话。
3. 抽查每个模块至少一个核心类型和一个简单类型。
4. 执行只允许注释变化的差异检查，确认程序行与 `HEAD` 一致。

**验证：** 561/561 已审阅、待审阅为 0、路径差集为空、非法注释为 0、非注释差异为 0。

## T35：编译与全量回归

**文件：** 全项目
**依赖：** T34

**步骤：** 使用 Java 21 编译，运行全部 Maven 测试，统计报告数、测试数、失败、错误和跳过数。

**验证：** `mvn -q test` 退出码 0；测试数不少于 619，failure 0、error 0。

## T36：打包和 tmux 端到端验收

**文件：** `target/`、`docs/ch16/acceptance-report.md`
**依赖：** T35

**步骤：**

1. 执行 `mvn -q -DskipTests package` 并记录 fat JAR 路径和大小。
2. 在 tmux 中启动 ImioCode，输入一个要求读取项目文件的真实请求。
3. 观察至少一次真实工具调用和最终回复，随后用 `/exit` 正常退出。
4. 将命令、输出和 checklist 证据写入验收报告。

**验证：** fat JAR 可启动；tmux 中可见工具调用与最终回答；会话和测试辅助进程清理完毕。

## T37：最终范围审计与提交

**文件：** CH16 文档和本次已注释生产文件
**依赖：** T36

**步骤：** 执行 diff/checklist 审计；只暂存 CH16 文档和生产代码注释；核对禁止文件；提交本章。

**验证：** 暂存区不包含 `claude.md`、`.imiocode/`、`hello.txt` 或 `src/test/`；提交成功后工作区只保留用户原有未提交项。

## 执行顺序

```text
T01 → T02 → T03 → … → T33 → T34 → T35 → T36 → T37
```

注释批次按现有模块依赖顺序串行推进；每个任务验证通过后才进入下一项。
