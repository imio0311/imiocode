# CH11 Skill 技能包系统 Tasks

## 文件清单

| 操作 | 文件/目录 | 职责 |
|---|---|---|
| 新建 | `src/main/java/io/imiocode/skill/**` | Skill 定义、解析、加载、执行、工具和命令 |
| 新建 | `src/main/resources/skills/**` | commit、review、test 内置 Skill |
| 修改 | `src/main/java/io/imiocode/agent/**` | 激活状态、每轮上下文和工具选择、fork 支持 |
| 修改 | `src/main/java/io/imiocode/conversation/ConversationSession.java` | inline/fork Skill 会话入口和父历史回流 |
| 修改 | `src/main/java/io/imiocode/runtime/**` | 摘要注入、Skill 命令服务与执行编排 |
| 修改 | `src/main/java/io/imiocode/command/**` | 动态命令注册与 `/skill` 服务接口 |
| 修改 | `src/main/java/io/imiocode/permission/**` | 专属命令工具的真实权限目标 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 完整装配 |
| 新建 | `src/test/java/io/imiocode/skill/**` | CH11 单元、集成和回归测试 |

## T1：定义 Skill 领域模型

**依赖：** 无

**步骤：**
1. 定义执行模式、历史策略、来源级别。
2. 定义 metadata、descriptor、loaded skill、专属工具声明和目录快照。
3. 在构造阶段完成空值、名称、集合和路径规范化。

**验证：** 模型构造测试覆盖合法值、默认值和非法值。

## T2：实现 Markdown 与目录包解析

**依赖：** T1

**步骤：**
1. 分离 YAML frontmatter 与 Markdown body。
2. 实现只读取元信息和完整读取两条路径。
3. 解析 tool.json、references，并校验路径不逃逸。
4. 实现 `$ARGUMENTS` 字面替换。

**验证：** 临时目录测试覆盖单文件、目录、损坏 YAML、空正文、路径逃逸和参数替换。

## T3：实现三级加载与快照热刷新

**依赖：** T2

**步骤：**
1. 扫描内置、用户、项目来源。
2. 以名称应用覆盖优先级并稳定排序。
3. 计算目录指纹，变化时构建新快照。
4. 刷新失败时保留旧快照并返回诊断。

**验证：** 测试同名覆盖、删除回退、正文热更新、错误回退和稳定排序。

## T4：扩展动态命令注册中心

**依赖：** T1

**步骤：**
1. 增加受控注销与动态替换能力。
2. 保证静态命令不会被用户 Skill 静默覆盖。
3. 实现 Skill 命令同步器。

**验证：** 注册、刷新、删除、别名冲突和补全测试通过。

## T5：实现专属声明式命令工具

**依赖：** T2

**步骤：**
1. 渲染 tool.json 命令参数模板。
2. 包装现有 Bash 执行限制与脱敏。
3. 扩展权限入口，使真实渲染命令进入五层检查。

**验证：** 参数渲染、权限拒绝、危险命令硬拦截和正常执行测试通过。

## T6：实现激活器与工具白名单

**依赖：** T3、T5

**步骤：**
1. 建立任务级激活作用域和幂等 activeSkills 列表。
2. 激活时注册专属工具，结束时清理。
3. 每轮生成完整 SOP 提醒。
4. 实现白名单并集、模式交集和系统工具豁免。

**验证：** 多 Skill、重复激活、Plan Mode、嵌套 LoadSkill 和清理测试通过。

## T7：实现 SkillExecutor 与 LoadSkill

**依赖：** T3、T6

**步骤：**
1. prepare 阶段加载完整内容并 fail-fast 校验工具依赖。
2. 实现 LoadSkill JSON Schema 和安全结果。
3. inline 激活当前任务；fork 委托隔离 runner。

**验证：** 未知 Skill、缺失工具、inline 激活和 fork 回传测试通过。

## T8：集成 Agent 每轮上下文

**依赖：** T6、T7

**步骤：**
1. AgentRequest 支持可选初始 Skill 调用。
2. Agent 启动和结束时管理激活作用域。
3. 每轮重新采集环境并加入 activeSkills SOP。
4. 每轮重新计算 ToolSelection。

**验证：** 两阶段工具清单、跨迭代 SOP、系统工具豁免和任务间隔离测试通过。

## T9：实现 inline/fork 会话执行

**依赖：** T7、T8

**步骤：**
1. inline 使用主会话历史运行并正常落盘。
2. fork 实现 full/recent/none 历史选择。
3. fork 只向父历史回流调用请求和最终回复。

**验证：** 三档历史和父历史完整性测试通过。

## T10：实现摘要注入与 `/skill` 管理命令

**依赖：** T3、T4、T9

**步骤：**
1. 生成仅含名称和描述的会话级提醒。
2. 实现动态 `/name` Skill 命令。
3. 实现 `/skill list/info/reload` 本地管理命令。
4. reload 后同步动态命令目录。

**验证：** 摘要无正文、命令不调用 LLM、热刷新同步和帮助补全测试通过。

## T11：添加三个内置 Skill

**依赖：** T2

**步骤：**
1. 编写 commit inline SOP。
2. 编写 review fork SOP。
3. 编写 test inline SOP。
4. 添加内置资源索引。

**验证：** 三个 Skill 均由同一解析器发现，模式和工具依赖正确。

## T12：应用装配与回归

**依赖：** T4—T11

**步骤：**
1. 在应用入口装配加载器、运行时、系统工具和命令。
2. 保持已有静态命令、MCP、权限、上下文和持久化兼容。
3. 增加集成测试并执行全部测试。

**验证：** Java 21 下 `mvn test` 和 `mvn package` 通过。

## T13：端到端验收

**依赖：** T12

**步骤：**
1. 在受控终端启动打包后的 ImioCode。
2. 执行 `/skill list`、`/skill info commit` 和 `/commit`。
3. 用自然语言请求提交/审查，观察 LoadSkill 与工具限制。
4. 对照 checklist 记录实际证据。

**验证：** 端到端输入、工具事件、回复和退出行为符合 checklist。

## 执行顺序

```text
T1 -> T2 -> T3 -> T4 -> T5 -> T6 -> T7 -> T8 -> T9 -> T10 -> T11 -> T12 -> T13
```
