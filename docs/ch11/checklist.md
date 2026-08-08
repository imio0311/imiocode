# CH11 Skill 技能包系统 Checklist

> 每项通过运行测试或观察真实终端行为验证，结论必须附实际证据。

## 定义与解析

- [x] 有效 YAML frontmatter 与 Markdown body 被正确分离。（验证：解析器测试）
- [x] 缺少 name、description、正文或模式非法时返回可定位错误。（验证：参数化异常测试）
- [x] `$ARGUMENTS` 按原始用户参数字面替换，不发生正则替换错误。（验证：特殊字符测试）
- [x] 单文件 Skill 与目录型 `SKILL.md` 都能加载。（验证：临时目录测试）
- [x] `references/` 和 `tool.json` 只在完整加载阶段读取。（验证：两阶段读取测试）
- [x] 符号链接或相对路径逃逸被拒绝。（验证：路径安全测试）

## 目录与热加载

- [x] 项目级同名 Skill 覆盖用户级和内置级。（验证：三级来源测试）
- [x] 删除项目级覆盖后回退用户级版本。（验证：reload 测试）
- [x] 启动摘要稳定排序且只包含 name、description。（验证：快照与提醒测试）
- [x] 修改正文后下次执行立即生效。（验证：正文热更新测试）
- [x] 修改元信息后自动检测或 `/skill reload` 生效。（验证：指纹测试）
- [x] 损坏的新目录不替换上一份有效快照。（验证：回退测试）

## 执行与工具约束

- [x] inline 在主历史中保存完整交互。（验证：ConversationSession 集成测试）
- [x] fork 的 full/recent/none 三档历史选择正确。（验证：历史选择测试）
- [x] fork 父历史不包含子 Agent 中间工具轨迹。（验证：父历史断言）
- [x] 缺失或禁用 allowedTools 时在模型请求前失败。（验证：fail-fast 测试）
- [x] 多 Skill 白名单取并集，非白名单普通工具不可见。（验证：ToolSelection 测试）
- [x] Plan Mode 不因 Skill 白名单开放写入或命令工具。（验证：模式交集测试）
- [x] LoadSkill 等系统工具始终可见并支持嵌套 Skill。（验证：嵌套激活测试）
- [x] 重复激活同一 Skill 幂等。（验证：激活列表测试）
- [x] 任务结束后 activeSkills 和临时专属工具被清理。（验证：生命周期测试）

## 专属工具与权限

- [x] tool.json 的名称、描述、Schema、命令模板完成校验。（验证：声明解析测试）
- [x] 专属工具未激活时不出现在 API tools 清单。（验证：PromptAssembler 集成测试）
- [x] 激活后专属工具可见且参数模板正确渲染。（验证：工具调用测试）
- [x] 渲染后的真实命令进入权限检查。（验证：权限请求断言）
- [x] 危险专属命令被硬拦截，普通命令按当前模式确认或放行。（验证：权限集成测试）

## 两阶段加载与 Agent

- [x] 未激活时 messages 中只有 Skill 摘要，无正文和 references。（验证：API payload 测试）
- [x] 自然语言意图可通过 LoadSkill 加载完整 Skill。（验证：脚本化 LLM 集成测试）
- [x] 激活 SOP 在后续每轮环境提醒中持续存在。（验证：多迭代请求捕获）
- [x] 环境上下文每轮重新采集。（验证：可变 provider 计数测试）
- [x] 多个 Skill 可在同一任务中并存。（验证：嵌套加载测试）

## 命令和内置 Skill

- [x] `/skill list` 本地列出名称、来源和模式。（验证：命令测试）
- [x] `/skill info <name>` 本地显示摘要、路径和工具约束，不泄露正文。（验证：命令测试）
- [x] `/skill reload` 本地刷新目录与动态命令。（验证：命令注册测试）
- [x] `/commit`、`/review`、`/test` 自动注册并支持 Tab 补全。（验证：Registry 测试）
- [x] commit 为 inline，review 为 fork，test 为 inline。（验证：内置资源测试）
- [x] 三个内置 Skill 的 SOP 可完成各自预期流程。（验证：内容契约测试）

## 兼容与质量

- [x] Java 21 编译和打包成功。（验证：`mvn package`）
- [x] 全部单元和集成测试通过。（验证：`mvn test`）
- [x] 既有 CH2—CH10 测试无回归。（验证：完整 Surefire 报告）
- [x] Skill 错误不导致应用崩溃或当前会话丢失。（验证：故障集成测试）
- [x] 错误信息不包含环境变量值、密钥或异常栈。（验证：脱敏测试）

## 端到端场景

- [x] 场景一：启动应用 -> `/skill list` -> 看到 commit/review/test，且未发生模型请求。
- [x] 场景二：输入 `/commit 文档更新` -> commit Skill 激活 -> Agent 查看状态、生成提交并给出简洁结果。
- [x] 场景三：说“帮我审查当前改动” -> 模型调用 LoadSkill(review) -> 子 Agent 隔离执行 -> 主会话只收到最终审查结果。
- [x] 场景四：新增项目 Skill 后执行 `/skill reload` -> 新 Slash Command 立即可补全和调用。
- [x] 场景五：Skill 声明不存在的工具 -> 调用立即失败且没有模型或文件副作用。
