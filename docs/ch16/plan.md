# CH16 生产代码中文注释 Plan

> 状态：已完成（2026-08-12）

## 架构概览

本次采用“分级审阅、定点注释”的处理方式，不改变代码结构。生产代码按理解成本分为三层：

1. **核心流程层**：应用启动、Agent Loop、流式工具调度、Provider、MCP、配置加载、权限系统、会话持久化、Worktree、Agent Team 和 Coordinator Mode。补充类级 Javadoc、关键公共接口说明，并在复杂状态转换、协议适配、并发和清理逻辑附近添加行内注释。
2. **功能组件层**：命令、工具、Hook、Memory、Instruction、Context、Terminal、Skill、普通 SubAgent 运行时等模块。说明组件职责、输入输出和非显然约束；简单委托方法不重复解释。
3. **简单数据层**：record、枚举、异常类型、简单配置对象、纯 getter/setter 和显然的适配器。逐文件扫描；只有类型用途或字段语义不直观时才补充注释，否则明确归为“无需补充”。

整体执行顺序：

```text
扫描并分类 561 个文件
    → 核心流程层
    → 安全与持久化层
    → 外部协议与运行时层
    → 其余功能组件
    → 简单数据层复核
    → 注释纯净性检查
    → 编译、全量测试、打包、tmux 端到端
```

每个模块完成后立即编译或运行对应测试；最终通过 Git diff 检查，确保生产代码中只增加注释和必要空白，不发生行为变化。

## 核心审阅数据与注释规范

本次不新增运行时数据结构或 Java 接口，只新增一份审阅清单。

### `comment-audit.md`

每个生产文件记录文件路径、所属模块、等级、处理结果、注释重点和验证状态。文档按模块汇总展示各模块文件数和处理结果数；机器扫描产生的完整路径清单作为折叠代码块保留，证明没有漏检。

### Java 注释规范

类和接口使用标准 Javadoc：

```java
/**
 * 负责协调单次 Agent Loop，并在模型响应和工具执行之间推进状态。
 *
 * <p>该类型只管理编排，不直接实现具体工具。</p>
 */
```

复杂公共方法按需说明参数、返回值和异常：

```java
/**
 * 原子地提交消息，并在外部后端可用时唤醒对应成员。
 *
 * @param recipientId 接收方 Agent ID
 * @param content 已完成脱敏检查的消息正文
 * @return 包含消息 ID 和唤醒结果的发送回执
 * @throws TeamException 接收方不存在或持久化失败时抛出
 */
```

行内注释只解释非显然原因：

```java
// 必须先落盘再唤醒，避免成员被唤醒后读取不到对应消息。
mailbox.append(message);
backend.wake(member);
```

明确禁止逐字翻译代码、机械注释字段与访问器、写入未经代码保证的承诺、使用占位词，或为了放置注释而重排代码。

## 模块设计

### A. 应用入口与 Agent 核心

**范围：** `ImioCodeApplication`、`agent/`、`runtime/`、`conversation/`、`prompt/`

**注释重点：** 应用依赖装配和关闭顺序；Agent Loop；流式调度；动态工具策略；System Prompt 与运行时上下文边界。

**验证：** 编译并运行 Agent、Conversation 相关测试。

### B. Provider 与外部协议

**范围：** `llm/`、`mcp/`

**注释重点：** Provider 请求和流式事件差异；Tool Call 分片；MCP 能力协商、传输、超时和资源生命周期；敏感值隔离。

**验证：** 运行 Provider、流式解析和 MCP 相关测试。

### C. 配置、安全与持久化

**范围：** `config/`、`permission/`、`persistence/`、`session/`

**注释重点：** 配置优先级与安全降级；权限和风险判定；路径越界防护；JSONL 事务、原子替换、损坏恢复和文件锁。

**验证：** 运行配置、权限、沙箱、持久化和 Session 测试。

### D. Worktree、SubAgent 与 Agent Team

**范围：** `worktree/`、`subagent/`、`team/`、`TeamMemberProcess`

**注释重点：** Worktree 所有权与清理约束；SubAgent 工具裁剪；Team roster、Task、Mailbox、transcript；后端选择；ACK 幂等；Coordinator 双锁和阶段推进。

**验证：** 运行 Worktree、SubAgent 和 Team 全部相关测试。

### E. 用户功能与扩展机制

**范围：** `command/`、`tool/`、`terminal/`、`hook/`、`instruction/`、`memory/`、`context/`、`skill/`

**注释重点：** Slash Command 分流；工具校验与权限；终端渲染；Hook 并发和错误策略；指令加载；记忆脱敏；上下文压缩；Skill 安装边界。

**验证：** 按模块运行对应测试，并进行一次完整编译。

### F. 简单数据类型复核

**范围：** 所有模块中的 record、枚举、异常、配置载体和简单适配器。

类型用途不直观时添加一句中文类说明；字段含义直观时不逐项注释；标准枚举值、简单异常和纯数据搬运类型允许判定为无需补充。

**验证：** 自动扫描文件覆盖率、占位词和注释语言，再人工抽查各层代表文件。

## 模块交互

本次不改变现有调用关系：

```text
ImioCodeApplication
    ├── Config / Permission / Session / Memory
    ├── Terminal / Command
    ├── Provider / MCP
    ├── Tool Registry
    └── ConversationCoordinator
            └── Agent Loop
                    ├── StreamingToolScheduler
                    ├── 本地 Tool / MCP Tool
                    ├── SubAgent / Worktree
                    └── Agent Team / Coordinator
```

每个模块执行：列出文件 → 阅读声明、入口和调用方 → 分类 → 补充注释 → 检查行为不变 → 更新审阅记录 → 运行定向测试。

## 行为不变检查

- **文本层：** 拒绝 `TODO`、`TBD`、空 Javadoc和纯复述式注释。
- **差异层：** 过滤空白和注释后比较修改前后的 Java Token，确保程序 Token 序列一致。
- **运行层：** 模块定向测试、全量测试、打包和 tmux 端到端测试全部通过。

发现非注释改动时立即回退对应代码片段，不借本次任务修复代码。

## 文件组织

```text
docs/ch16/
├── spec.md
├── plan.md
├── task.md
├── checklist.md
├── comment-audit.md
└── acceptance-report.md

src/main/java/io/imiocode/
├── ImioCodeApplication.java
├── TeamMemberProcess.java
└── 各生产模块目录
```

不会修改 `src/test/`、构建产物或运行数据。

## 技术决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 注释语言 | 简体中文，保留标准英文术语 | 提升中文团队可读性并避免技术名词误译 |
| 覆盖方式 | 全文件审阅、按理解成本选择性补充 | 兼顾完整覆盖与低噪声 |
| 注释粒度 | 类职责、复杂公共接口、非显然原因 | 优先记录设计意图 |
| 测试文件 | 默认不修改 | 测试名称和断言已表达行为 |
| 行为保护 | Java Token 等价检查与编译测试 | 证明没有可执行代码变更 |
| 执行方式 | 按模块分批修改和验证 | 缩小错误定位范围 |
| 现有注释 | 正确则保留，关键说明按需补中文 | 降低无意义文本扰动 |
| 简单类型 | 允许标记“无需补充” | 不以注释数量作为质量目标 |
| 文档证据 | 模块汇总与完整文件清单 | 兼顾可审阅性和覆盖证明 |
| Git 范围 | 只纳入 CH16 文档和生产代码注释 | 保护用户已有未提交文件 |
