# CH16 生产代码中文注释 Checklist

> 状态：已完成（2026-08-12），50/50 通过。每一项均通过运行命令、检查差异或观察程序行为验证。

## 审阅覆盖

- [x] C01 生产 Java 文件基线为 561 个（验证：递归统计 `src/main/java/**/*.java`，期望结果为 561）。
- [x] C02 完整路径清单没有重复或遗漏（验证：源文件路径与 `comment-audit.md` 路径集合双向求差，期望两个差集均为空）。
- [x] C03 21 个目录模块和两个顶层入口均有审阅统计（验证：对照源目录清单，期望缺失模块数为 0）。
- [x] C04 每个生产文件都有“新增注释”“保留现有注释”或“无需补充”之一（验证：审阅记录 561 条，待审阅为 0）。
- [x] C05 每个模块至少人工抽查一个核心类型和一个简单类型（验证：审阅文档包含模块抽查证据；没有简单类型的模块注明不适用）。

## 注释完整性

- [x] C06 应用入口具有中文职责、装配与关闭顺序说明（验证：阅读 `ImioCodeApplication` 和 `TeamMemberProcess` 的新增 Javadoc/行内注释）。
- [x] C07 Agent Loop 具有迭代、终止、工具回灌和动态策略说明（验证：阅读 Agent 核心类型，并运行 Agent 定向测试）。
- [x] C08 流式工具调度具有分片、并发、顺序、取消和收尾说明（验证：阅读调度器及运行流式调度测试）。
- [x] C09 Conversation、Runtime 和 Prompt 具有编排、分流、运行时重建与上下文注入说明（验证：抽查核心类型并运行对应测试）。
- [x] C10 Context 具有预算、压缩、结果外置和容量边界说明（验证：抽查 Context 核心类型并运行相关测试）。
- [x] C11 配置加载具有来源优先级、环境变量、安全降级和兼容回退说明（验证：阅读 ConfigLoader 等核心类型并运行 Config 测试）。
- [x] C12 权限系统具有规则优先级、模式、高风险确认和 fail-closed 说明（验证：抽查权限决策、命令风险与沙箱实现并运行相关测试）。
- [x] C13 持久化与 Session 具有原子提交、JSONL 边界、损坏恢复和锁说明（验证：抽查存储实现并运行 Persistence/Session 测试）。
- [x] C14 三种 Provider 具有请求、流式事件、Tool Call 和停止原因差异说明（验证：抽查 OpenAI、Anthropic、DeepSeek 客户端并运行 Provider 测试）。
- [x] C15 MCP 具有握手、能力、传输、超时、响应上限和敏感值隔离说明（验证：抽查 Client、stdio、HTTP 实现并运行 MCP 测试）。
- [x] C16 Worktree 具有资源所有权、dirty/独有提交、回滚和保守清理说明（验证：抽查生命周期实现并运行 Worktree 测试）。
- [x] C17 SubAgent 具有工具裁剪、前后台调度、容量和隔离说明（验证：抽查 Dispatcher、Filter、Registry 并运行 SubAgent 测试）。
- [x] C18 Agent Team 具有任务图、Mailbox、ACK、transcript、后端、生命周期和清理竞态说明（验证：抽查 Team 各子模块并运行 Team 测试）。
- [x] C19 Coordinator Mode 具有双锁、Lead 身份、工具收窄和四阶段说明（验证：抽查控制器和动态策略并运行 Coordinator 测试）。
- [x] C20 Command、Tool、Terminal 与 Hook 具有分流、权限、渲染、同步阻断和异步错误策略说明（验证：抽查核心类型并运行相应测试）。
- [x] C21 Instruction、Memory 与 Skill 具有 include 安全、脱敏、来源、容量和原子安装说明（验证：抽查核心类型并运行相应测试）。

## 注释质量

- [x] C22 新增注释以简体中文为主，必要技术术语保留英文（验证：扫描新增注释并人工抽查所有模块）。
- [x] C23 不存在 `TODO`、`TBD`、空 Javadoc 或占位文本（验证：扫描本次新增注释，期望命中数为 0）。
- [x] C24 不存在乱码或替换字符（验证：扫描 `�`、异常编码片段并人工检查，期望命中数为 0）。
- [x] C25 Javadoc 语法可被编译器接受（验证：Java 21 编译通过；新增标签只使用标准 Javadoc 标签）。
- [x] C26 复杂公共接口按需包含参数、返回值或异常说明（验证：抽查每个核心模块的公开入口）。
- [x] C27 行内注释解释原因或约束，没有逐句复述代码（验证：人工抽查每个模块新增行内注释）。
- [x] C28 简单 record、枚举、异常和访问器未被机械注释淹没（验证：抽查“无需补充”类型及变更统计）。
- [x] C29 注释没有声称实现未保证的线程安全、性能或兼容性（验证：人工审阅涉及并发、协议和安全的全部新增注释）。

## 行为与范围不变

- [x] C30 生产代码只增加注释和必要空白（验证：相对基线过滤注释与空白后，Java 程序 Token 序列完全一致）。
- [x] C31 没有修改公开签名、注解、常量或序列化字段（验证：差异审计无非注释程序行，编译与序列化测试通过）。
- [x] C32 没有修改 `src/test/`（验证：`git diff --name-only` 中不存在 `src/test/` 路径）。
- [x] C33 没有修改构建配置、第三方依赖或运行数据（验证：提交候选路径只含 `docs/ch16/` 和 `src/main/java/`）。
- [x] C34 用户原有 `claude.md`、`.imiocode/`、`hello.txt` 未被修改或暂存（验证：最终状态与基线一致，暂存区禁止项为 0）。
- [x] C35 Git 空白检查通过（验证：`git diff --check` 和 `git diff --cached --check` 退出码均为 0）。

## 分模块集成

- [x] C36 Agent、Runtime、Conversation、Prompt 与 Context 定向测试通过（验证：运行对应 Maven 测试，退出码 0）。
- [x] C37 Config、Permission、Persistence 与 Session 定向测试通过（验证：运行对应 Maven 测试，退出码 0）。
- [x] C38 LLM Provider 与 MCP 定向测试通过（验证：运行对应 Maven 测试，退出码 0）。
- [x] C39 Worktree、SubAgent 与 Agent Team 定向测试通过（验证：运行对应 Maven 测试，退出码 0）。
- [x] C40 Command、Tool、Terminal、Hook、Instruction、Memory 与 Skill 定向测试通过（验证：运行对应 Maven 测试，退出码 0）。

## 编译与全量测试

- [x] C41 Java 21 编译通过（验证：`mvn -q -DskipTests compile` 退出码 0）。
- [x] C42 基线回归不少于 619 项且无失败（验证：显式运行全部 173 个 `*Test` 类及 `AgentTeamManagerIT`，独立报告统计 tests ≥ 619、failures 0、errors 0）。
- [x] C43 fat JAR 打包成功（验证：`mvn -q -DskipTests package` 退出码 0，`target/imiocode-0.2.0-SNAPSHOT-all.jar` 存在且非空）。

## 端到端场景

- [x] C44 tmux 中能够启动 ImioCode（验证：pane 中显示正常启动面板和输入提示符，没有启动异常）。
- [x] C45 输入真实项目阅读请求后发生工具调用（验证：pane 中可见至少一次真实文件读取或搜索工具执行成功）。
- [x] C46 工具结果被 Agent 正确回灌并生成最终中文回复（验证：最终回复包含从项目文件得到的可核对信息）。
- [x] C47 输入 `/exit` 后应用正常退出且辅助资源清理完毕（验证：ImioCode 进程、测试 tmux session 和 mock Provider 均无残留）。

## 验收记录

- [x] C48 `acceptance-report.md` 记录实际命令、退出码、测试统计、JAR 信息和 tmux 观察结果（验证：报告不存在“预计”“应该”等未执行表述）。
- [x] C49 失败项在修复后重新执行并记录最终证据（验证：所有 checklist 条目最终均有实际通过证据或明确未通过说明）。
- [x] C50 最终提交仅包含已验收范围（验证：提交文件路径与候选清单一致，禁止项和意外路径均为 0）。
