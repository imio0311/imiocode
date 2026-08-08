# CH11 Skill 技能包系统 Plan

## 架构概览

Skill 子系统拆分为定义解析、来源加载、运行时激活、执行编排、工具适配和命令集成六层。加载层维护不可变目录快照；启动摘要只读取 frontmatter。执行时重新读取正文和目录附件，形成一次不可变的激活对象。Agent 每轮从运行时取得激活快照，重建提醒和工具选择。

Slash Command 和模型工具共享同一个 `SkillExecutor`。inline 调用把激活对象带入主 Agent；fork 调用由独立 Agent 实例在选定历史切片中运行，父会话只接收最终响应。`LoadSkill` 是系统工具，不受 Skill 白名单屏蔽；加载 inline Skill 时激活当前任务，加载 fork Skill 时运行隔离 Agent 并把结果作为工具结果返回。

## 核心数据结构

### SkillMetadata

字段包括规范化名称、描述、执行模式、别名、允许工具集合、历史携带策略。名称只允许小写字母、数字和连字符。

### SkillDescriptor

包含元信息、来源级别、源路径、是否为目录包和内容指纹，仅用于目录摘要，不持有正文。

### LoadedSkill

包含描述符、已替换参数的完整 SOP、参考资料、专属工具定义以及本次激活实际可用的工具集合。对象创建前完成全部校验。

### SkillCatalogSnapshot

按名称保存最终覆盖后的不可变目录及诊断信息。只有新目录完整构建成功才原子替换旧目录。

### SkillInvocation

描述一次显式或嵌套调用，包含 Skill 名称、原始参数、加载后的内容和执行模式。

### SkillRunState

保存本次 Agent 任务的激活 Skill 有序表、注册的临时专属工具和嵌套执行状态。任务结束时释放临时注册。

### SkillParser

- `parseDescriptor(source)`：只解析 frontmatter，返回摘要描述符。
- `parseLoaded(descriptor, arguments)`：读取并校验正文、references 和 tool.json，替换 `$ARGUMENTS`。

### SkillLoader

- `snapshot()`：在文件指纹变化时尝试热刷新并返回当前有效快照。
- `reload()`：强制重建三个来源的目录。
- `load(name, arguments)`：从当前描述符按需加载完整 Skill。

### SkillExecutor

- `prepare(name, arguments)`：加载并执行 fail-fast 依赖校验。
- `executeFork(invocation, history, events)`：选择历史并运行隔离 Agent。

### SkillActivator

- `beginRun(initialInvocation)`：创建任务级激活作用域。
- `activate(invocation)`：幂等激活、注册专属工具。
- `activeReminder()`：生成当前全部 SOP 的环境提醒。
- `selectTools(baseSelection)`：应用白名单、Plan Mode 交集和系统工具豁免。

## 模块设计

### 定义与解析

**职责：** 解析 YAML frontmatter 与 Markdown body，校验名称、模式、历史策略、工具白名单；安全读取目录附件。

**依赖：** Jackson YAML/JSON、Java NIO。

### 三级加载与热刷新

**职责：** 扫描项目 `.imiocode/skills/`、用户 `~/.imiocode/skills/` 和 classpath 内置目录，应用覆盖优先级，维护稳定快照与文件指纹。

**依赖：** 定义与解析模块。

### 专属工具适配

**职责：** 把 `tool.json` 的声明式命令包装成内部 Tool；参数模板渲染后的真实命令作为权限目标，执行复用 Bash 工具的超时、输出限制、脱敏和工作区。

**依赖：** Tool、PermissionGate、WorkspacePolicy。

### 激活与两阶段上下文

**职责：** 生成启动摘要；在任务作用域维护激活列表；每轮生成完整 SOP 提醒；计算工具可见性并管理临时专属工具生命周期。

**依赖：** Loader、ToolRegistry、Agent。

### inline/fork 执行

**职责：** inline 把初始激活传给主 Agent；fork 根据 full/recent/none 选择历史，在独立 Agent 中运行并只返回最终响应。嵌套 fork 复用同一客户端和权限链但拥有独立激活作用域。

**依赖：** Agent factory、会话历史、激活模块。

### Slash Command 与应用装配

**职责：** 动态注册 Skill 命令，提供 `/skill list/info/reload`，在应用启动时装配加载器、系统工具、运行时和 fork runner。

**依赖：** CommandRegistry、ConversationCoordinator。

## 模块交互

```text
启动
  -> SkillLoader 建立摘要快照
  -> 注册 LoadSkill 系统工具
  -> 为最终目录注册动态 Slash Command
  -> 每个用户任务把 name + description 摘要注入 messages

自然语言调用
  -> 模型根据摘要调用 LoadSkill
  -> SkillExecutor 按需读取完整包并校验依赖
  -> inline: SkillActivator 激活
     -> 下一轮重新注入完整 SOP + 过滤工具
  -> fork: 独立 Agent 执行
     -> LoadSkill 工具结果只携带最终输出

Slash 调用
  -> 动态 SkillCommand
  -> SkillExecutor.prepare
  -> inline: 主会话携带初始激活运行
  -> fork: 隔离运行并把最终响应追加到主会话
```

## 文件组织

```text
src/main/java/io/imiocode/skill/
├── model/                 — metadata、descriptor、loaded skill、枚举
├── parse/                 — frontmatter、正文、tool.json、references 解析
├── load/                  — 三级来源、快照、指纹和热刷新
├── runtime/               — activator、executor、fork runner、摘要提醒
├── tool/                  — LoadSkill 和声明式专属工具
└── command/               — 动态 SkillCommand 与 /skill

src/main/resources/skills/
├── index.txt
├── commit/SKILL.md
├── review/SKILL.md
└── test/SKILL.md

src/test/java/io/imiocode/skill/ — 单元与集成测试
docs/ch11/                  — spec、plan、task、checklist
```

现有文件将修改 Agent、AgentRequest、ConversationSession、ConversationCoordinator、CommandRegistry、CommandServices、PermissionRequestFactory、PermissionGate、StreamingToolScheduler 和应用装配入口。

## 技术决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 内置 Skill | classpath Markdown 资源 | 与用户 Skill 使用同一格式，发布包可直接携带 |
| 热加载 | 指纹检测 + 强制 reload + 执行时重读正文 | 常态开销低，正文编辑立即生效，失败可回退 |
| 覆盖 | 先内置、再用户、最后项目原子覆盖 | 明确实现项目级最高优先级 |
| 激活生命周期 | 单次顶层 Agent 任务 | 防止旧 SOP 污染后续无关任务，同时支持循环内多 Skill |
| fork 历史 | full/recent/none，默认 recent | 同时兼顾上下文质量、隔离性和 Token 成本 |
| fork 回流 | 父历史只追加调用请求和最终助手响应 | 保持子任务轨迹隔离 |
| 白名单组合 | 多 Skill 并集，再与模式限制求交，最后加入系统工具 | 支持组合与嵌套且不突破 Plan Mode |
| 专属工具 | 声明式固定命令 + 参数模板 | 无需编译即可新增，且真实命令可进入权限检查 |
| 远程安装 | 不实现 | 属于明确排除的市场和分发范围 |
