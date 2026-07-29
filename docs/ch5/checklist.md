# ImioCode 第五章：Prompt 工程体系 Checklist

> 每一项必须通过运行命令或观察真实行为验证。未执行、无证据或受外部条件阻塞的项目不得标记为通过。

## 实现完整性

- [x] **七个固定模块均已存在并可参与组装（AC1）**  
  （验证：运行 `mvn -q -Dtest=SystemPromptBuilderTest test`，观察七个模块完整性断言通过。）

- [x] **乱序注册仍按 Identity → Behavior → ToolUsage → CodeQuality → Security → TaskPattern → OutputStyle 输出（AC1）**  
  （验证：运行 Builder 乱序输入测试，比较实际标题顺序。）

- [x] **相同输入重复组装的 System Prompt 完全一致（AC1）**  
  （验证：运行稳定性测试，比较两次输出字符串及其长度。）

- [x] **System Prompt 不包含工作目录、时间、Git 状态或 Plan Mode 文本（AC1、AC5）**  
  （验证：在组装测试中搜索动态字段和 Plan 提醒关键字，期望均不存在。）

- [x] **统一管线只输出 system、messages、tools 三个内容通道（AC2）**  
  （验证：运行 `mvn -q -Dtest=PromptAssemblerTest test`，检查 ApiPayload 三通道断言。）

- [x] **七类信息进入正确通道（AC2）**  
  （验证：构造同时包含环境、会话提醒、历史、当前轨迹、轮次提醒和工具的请求，观察各通道内容。）

- [x] **工具选择只影响 tools，不改写 System Prompt（AC2、AC5）**  
  （验证：分别组装普通模式和 Plan Mode 请求，比较 system 完全相同、tools 分别为六个和三个只读工具。）

## 环境与提醒

- [x] **每个 Agent 任务只采集一次环境（AC3）**  
  （验证：使用计数型环境提供者运行多轮 Agent 测试，期望单任务计数为 1。）

- [x] **同一任务的每轮环境提醒完全一致（AC3）**  
  （验证：捕获至少两轮 ChatRequest，比较环境提醒文本。）

- [x] **下一个用户任务重新采集环境（AC3）**  
  （验证：连续运行两个任务，期望计数为 2 且第二任务使用新的快照。）

- [x] **环境提醒包含工作区、OS、Shell、时间/时区、Git 分支和状态（AC3）**  
  （验证：运行 `mvn -q -Dtest=EnvironmentContextCollectorTest,EnvironmentReminderFormatterTest test`，观察字段完整性断言。）

- [x] **环境采集不注入环境变量值、凭据、Git 文件名或原始命令输出（AC3）**  
  （验证：向测试环境放入哨兵秘密值和带名称的 dirty 文件，期望提醒中搜索不到哨兵及文件名。）

- [x] **Git 不存在、非仓库或超时时安全降级（AC3）**  
  （验证：运行环境采集失败场景，期望返回 unavailable/not-repository 且 Agent 仍能继续。）

- [x] **每条提醒均为独立逻辑 user 消息并带完整 XML 标签（AC4）**  
  （验证：运行 `mvn -q -Dtest=SystemReminderTest,PromptAssemblerTest test`，检查角色、起止标签和消息数量。）

- [x] **提醒顺序为环境 → 会话 → 历史/当前轨迹 → 轮次（AC4）**  
  （验证：在每类内容中放入唯一标记，比较 ApiPayload.messages 的实际顺序。）

- [x] **用户原始输入未被拼接或改写（AC4）**  
  （验证：组装前后比较原始 `ChatMessage` 内容和对象字段。）

- [x] **提醒不进入正式会话历史（AC3、AC4、AC5）**  
  （验证：任务完成后读取 history snapshot，搜索环境、会话及 Plan 提醒标记，期望均不存在。）

## Plan Mode

- [x] **Plan Mode 第 1、6、11 轮使用完整提醒（AC5）**  
  （验证：运行 `mvn -q -Dtest=PlanModePromptTest test`，观察完整提醒周期断言。）

- [x] **Plan Mode 第 2～5、7～10 轮使用精简提醒（AC5）**  
  （验证：运行相同测试，观察精简提醒周期断言。）

- [x] **普通模式不产生 Plan Mode 提醒（AC5）**  
  （验证：对普通模式调用轮次提醒接口，期望返回空。）

- [x] **Plan Mode 只向模型公开 ReadFile、Glob、Grep（AC5）**  
  （验证：捕获 Plan Mode Provider 请求，检查工具名称集合严格等于 `read_file`、`glob`、`grep`。）

- [x] **切换 `/do` 后恢复六个工具且无 Plan 提醒（AC5）**  
  （验证：终端输入 `/do` 后发起下一任务，捕获请求并检查工具集合与消息。）

## 工具描述

- [x] **ReadFile 描述说明局部读取及修改前优先读取（AC6）**  
  （验证：运行 `mvn -q -Dtest=CoreToolDescriptionTest test`，检查关键约束。）

- [x] **Glob 描述说明按路径发现候选文件并优先用于探索（AC6）**  
  （验证：运行相同描述契约测试。）

- [x] **Grep 描述说明搜索内容并与 ReadFile 配合（AC6）**  
  （验证：运行相同描述契约测试。）

- [x] **EditFile 描述说明唯一匹配、精确旧文本和先读取（AC6）**  
  （验证：运行相同描述契约测试。）

- [x] **WriteFile 描述说明新建/完整覆盖及局部修改优先 EditFile（AC6）**  
  （验证：运行相同描述契约测试。）

- [x] **Bash 描述说明用于构建测试且不能替代专用工具（AC6）**  
  （验证：运行相同描述契约测试。）

- [x] **六个工具的名称、参数 Schema 和风险级别未变化（AC6、AC12）**  
  （验证：运行六个原有工具测试和描述 Schema 回归断言。）

## Provider 请求结构

- [x] **OpenAI 将七模块 Prompt 放入 instructions，提醒只出现在 input user 消息（AC7）**  
  （验证：运行 `mvn -q -Dtest=OpenAiClientTest test`，检查 Mock Server 捕获的 JSON。）

- [x] **OpenAI 不发送 cache_control 或不兼容的显式缓存字段（AC7）**  
  （验证：在捕获的请求中搜索 `cache_control`、`prompt_cache_options`、`prompt_cache_breakpoint`，期望不存在。）

- [x] **Anthropic System Prompt text block 带 ephemeral 缓存标记（AC7）**  
  （验证：运行 `mvn -q -Dtest=AnthropicClientTest test`，检查 system block。）

- [x] **Anthropic 只有最后一个工具带 ephemeral 标记并覆盖完整工具前缀（AC7）**  
  （验证：检查 tools 数组，前 N-1 个无标记，最后一个标记正确。）

- [x] **Anthropic 提醒位于 messages，不再进入 system（AC4、AC7）**  
  （验证：检查 system 只包含七模块文本，提醒作为 user content block 出现且顺序不变。）

- [x] **DeepSeek 只有首条消息是稳定 System Prompt（AC7）**  
  （验证：运行 `mvn -q -Dtest=DeepSeekClientTest test`，检查首条角色及 system 消息数量。）

- [x] **DeepSeek 不发送任何缓存控制字段（AC7）**  
  （验证：搜索 Mock Server 捕获请求，期望不存在 `cache_control`。）

- [x] **三个 Provider 的动态提醒变化不改写稳定 System Prompt（AC7）**  
  （验证：每家连续构造两个不同环境/轮次请求，比较其 system 通道内容完全一致。）

## Usage 与缓存

- [x] **OpenAI cached_tokens 和 cache_write_tokens 均映射到统一 usage（AC8）**  
  （验证：运行 OpenAI usage 样例测试，检查 cache-read/cache-write 数值。）

- [x] **Anthropic cache_read_input_tokens 和 cache_creation_input_tokens 均映射到统一 usage（AC8）**  
  （验证：运行 Anthropic usage 样例测试，检查两个数值。）

- [x] **DeepSeek prompt_cache_hit_tokens 映射到统一 cache-read（AC8）**  
  （验证：运行 DeepSeek 官方字段样例测试。）

- [x] **DeepSeek 旧 cached_tokens 格式仍可解析（AC8）**  
  （验证：运行 DeepSeek 兼容样例测试。）

- [x] **终端能显示 input、output、reasoning、cache-read 和 cache-write（AC8）**  
  （验证：运行 `mvn -q -Dtest=UsageFormatterTest test` 并观察真实请求的 usage 输出。）

- [ ] **当前配置 Provider 的后续 Agent 轮次出现真实 cache-read > 0（AC8）**  
  （验证：在 tmux 中发送一个必然触发“读取工具 → 回传结果 → 再次请求模型”的真实任务，捕获至少两次 usage；后续轮次必须显示非零 `cache-read`。）

- [ ] **真实缓存未命中时如实记录外部限制，不伪造通过（AC9）**  
  （验证：若 cache-read 始终为 0，记录模型、两轮 input、稳定前缀证据、API usage、等待间隔及 Provider 限制；该真实命中项保持未通过或阻塞。）

## 自动化测试与集成

- [x] **Prompt、环境、提醒、Plan、工具描述和 Provider 定向测试全部通过（AC11）**  
  （验证：依次运行 task.md 中 T3～T17 的测试命令，所有退出码为 0。）

- [x] **多轮工具调用和工具结果回传无回归（AC12）**  
  （验证：运行 `mvn -q -Dtest=AgentTest,StreamingTurnExecutorTest,ToolBatchExecutorTest test`。）

- [x] **流式文本、Thinking、ToolUse 和 Usage 事件无回归（AC12）**  
  （验证：运行三个 RichEvent 测试及 `StreamingResponseCollectorTest`。）

- [x] **会话成功提交和失败不提交规则无回归（AC12）**  
  （验证：运行 `mvn -q -Dtest=ConversationSessionTest,AgentCancellationTest test`。）

- [x] **OpenAI、Anthropic、DeepSeek 客户端契约全部通过（AC11、AC12）**  
  （验证：运行三个 Provider 测试及 `LlmClientContractTest`。）

## 编译与安全检查

- [x] **项目使用 Java 21 编译（AC11）**  
  （验证：运行 `java -version` 和 `mvn -version`，确认 Maven 使用 Java 21。）

- [x] **完整测试、编译和打包成功（AC11、AC12）**  
  （验证：运行 `mvn -q clean verify`，期望退出码 0。）

- [x] **生成可执行 JAR（AC13）**  
  （验证：检查 `target/imiocode-0.2.0-SNAPSHOT-all.jar` 存在，并运行 `java -jar target/imiocode-0.2.0-SNAPSHOT-all.jar` 能进入终端界面。）

- [x] **测试数量没有低于开发前基线（AC11、AC12）**  
  （验证：比较 T0 与最终 Surefire 报告的 tests run 总数。）

- [x] **代码和请求日志不泄露 API Key 或配置秘密（AC3、AC11）**  
  （验证：检查 Git diff、测试输出和 tmux 捕获内容，搜索已知秘密哨兵，期望不存在。）

- [x] **Git 提交不包含用户原有 `claude.md` 和 `hello.txt` 改动（AC12）**  
  （验证：运行 `git status --short` 和 `git diff --cached --name-only` 检查提交范围。）

## 五个定性评估场景

- [x] **评估文档包含五个完整场景（AC10）**  
  （验证：读取 `docs/ch5/eval-scenarios.md`，每个场景都有输入、预期工具行为、观察点和结果记录。）

- [ ] **场景 1：项目探索优先使用 Glob/Grep/ReadFile（AC6、AC10）**  
  （验证：执行文档输入并捕获工具轨迹；没有用 Bash 替代文件发现和读取。）

- [ ] **场景 2：精确修改先读后改且使用 EditFile（AC6、AC10）**  
  （验证：在测试夹具文件上执行，观察 ReadFile 位于 EditFile 之前，未使用 WriteFile 整体覆盖。）

- [ ] **场景 3：创建文件使用 WriteFile 并验证结果（AC6、AC10）**  
  （验证：在 `target/ch5-eval-fixture` 中执行，观察路径确认、WriteFile 和后续读取/验证。）

- [ ] **场景 4：命令失败后调整方案而非盲目重复（AC6、AC10）**  
  （验证：执行故意失败的 Maven goal，观察模型读取错误并改用正确命令，失败命令不重复。）

- [ ] **场景 5：Plan Mode 只调查并输出计划（AC5、AC10）**  
  （验证：执行 `/plan` 场景，工具轨迹仅包含 ReadFile/Glob/Grep，没有写入或 Bash。）

## tmux 端到端验收

- [ ] **在 tmux 中启动真实 ImioCode 进程（AC13）**  
  （验证：创建独立 tmux session，运行可执行 JAR，使用 `tmux capture-pane` 观察欢迎界面和输入提示。）

- [ ] **普通任务完成真实工具循环（AC13）**  
  （验证：输入“先用 Glob 找到 Agent.java，再用 ReadFile 读取并说明职责”，观察工具调用、工具结果、第二轮模型请求和最终回复。）

- [ ] **普通任务的后续轮次展示真实缓存 usage（AC8、AC13）**  
  （验证：捕获 pane，记录每轮 usage 并检查后续轮次的 cache-read。）

- [ ] **Plan Mode 端到端只读（AC5、AC13）**  
  （验证：输入 `/plan` 后要求调查 Provider Prompt 组装并给计划，观察只读工具及计划回复；再输入 `/do` 恢复普通模式。）

- [ ] **终端流式文本和工具状态显示正常（AC12、AC13）**  
  （验证：捕获 pane，能看到 Thinking/Streaming、工具 queued/running/succeeded 或失败状态及最终回答，没有一步一停错误。）

- [ ] **程序能够正常退出且 tmux 会话中无遗留进程（AC13）**  
  （验证：输入 `/exit`，确认 Java 进程结束，再关闭 tmux session。）

- [x] **tmux 不可用时明确记录阻塞且不标记端到端通过（AC13）**  
  （验证：保存 `tmux -V` 或 WSL/tmux 启动失败的实际输出；仅记录阻塞，不用普通终端结果替代。）

## 兼容补充验收

### 增量 Builder

- [x] **空 Builder 可以链式注册模块（AC14）**  
  （验证：运行 `mvn -q -Dtest=SystemPromptBuilderTest test`，观察空构造与连续 `add` 返回当前 Builder 的断言。）

- [x] **乱序增量注册与默认七模块输出完全一致（AC14）**  
  （验证：以乱序逐个注册七个模块，比较实际完整字符串与 `defaults().build()`。）

- [x] **增量 Builder 继续过滤空模块并拒绝重名（AC14）**  
  （验证：运行空内容和重名场景，期望空标题不存在且重名构建抛出明确异常。）

- [x] **现有列表构造和无参数 `defaults()` 行为保持兼容（AC14、AC18）**  
  （验证：运行原有 Builder 测试，比较 `defaults()` 与 `defaults(BuildOptions.empty())`。）

### 可选稳定模块

- [x] **三个可选内容均为空时不产生额外标题（AC15）**  
  （验证：用 null、空字符串和纯空白构造选项，期望输出只包含七个核心模块。）

- [x] **单个可选内容只产生对应模块（AC15）**  
  （验证：分别只提供 CustomInstructions、Skill、Memory，期望每次只新增一个对应标题。）

- [x] **三个可选模块按 CustomInstructions → Skill → Memory 稳定排列（AC15）**  
  （验证：同时提供三段唯一文本，比较标题位置和重复构建结果。）

- [x] **可选模块不读取文件、环境或外部状态（AC15）**  
  （验证：在空临时目录中仅传入内存文本完成构建，期望输出只包含提供的文本且不产生外部访问。）

### 环境摘要扩展

- [x] **环境提醒包含架构、Git 仓库状态和当前模型（AC16）**  
  （验证：运行 `mvn -q -Dtest=EnvironmentContextCollectorTest,EnvironmentReminderFormatterTest test`，检查三个新增字段。）

- [x] **Git 仓库状态能区分是、否和未知（AC16）**  
  （验证：分别使用 clean/dirty、not-repository、unavailable 上下文，期望格式化为对应三态。）

- [x] **旧环境构造路径继续可用并使用 unknown 新字段（AC16、AC18）**  
  （验证：使用旧五参数上下文构造器和旧采集器构造器，期望编译通过且新字段为固定 unknown。）

- [x] **新增环境字段不进入 System Prompt 或正式历史（AC16）**  
  （验证：在环境值中放入唯一标记，检查 Provider system 和任务完成后的 history 均不存在该标记。）

### 退出 Plan Mode

- [x] **退出提醒具有 ROUND 作用域且文本明确恢复执行能力（AC17）**  
  （验证：运行 `mvn -q -Dtest=PlanModePromptTest test`，检查作用域和正文。）

- [x] **初始 DO 和 DO→DO 不产生退出提醒（AC17）**  
  （验证：捕获对应任务每轮 ChatRequest，搜索退出提醒标记，期望不存在。）

- [x] **Plan→DO 后仅下一任务第一轮出现一次退出提醒（AC17）**  
  （验证：运行至少两轮的下一普通任务，再运行一个新任务；计数分别为 1、0、0。）

- [x] **Plan→DO→Plan 会取消待注入退出提醒（AC17）**  
  （验证：按顺序切换模式后运行 Plan 任务，期望只有 Plan 提醒，没有退出提醒。）

- [x] **退出提醒不进入 trajectory 或正式会话历史（AC17）**  
  （验证：任务完成后搜索 AgentResult trajectory 和 ConversationSession history，期望均不存在退出提醒。）

### 补充回归

- [x] **补充功能定向测试全部通过（AC18）**  
  （验证：运行 `SystemPromptBuilderTest`、环境测试、`PlanModePromptTest`、`AgentTest` 和 `ConversationLoopTest`，期望退出码均为 0。）

- [x] **Java 21 完整构建继续通过且测试数不少于 185（AC18）**  
  （验证：运行 `mvn -q clean verify`，汇总 Surefire 报告并与 185 项基线比较。）

- [x] **可执行 JAR 仍可生成和启动（AC18）**  
  （验证：检查 all.jar 存在，输入 `/exit` 启动后正常退出且进程退出码为 0。）

- [x] **补充提交不包含配置和用户原有文件（AC18）**  
  （验证：检查 staged names，期望不存在 `config.yaml`、`claude.md` 和 `hello.txt`。）

## 本次验收记录（2026-07-29）

- 兼容补充：增量 Builder、三个可选稳定模块、扩展环境字段和 Plan→DO 一次性提醒均已通过定向测试。
- 补充后完整构建：51 个套件、195 项测试、0 失败、0 错误、1 项按环境跳过；不少于补充前 185 项基线。
- 补充后产物：`target/imiocode-0.2.0-SNAPSHOT-all.jar` 已重新生成；主代码变更后实际启动并输入 `/exit`，退出码为 0。
- Java：Oracle JDK `21.0.3`；Maven `3.9.11` 使用同一 JDK。
- 完整构建：`mvn -q clean verify` 退出码为 0。
- 测试报告：51 个套件、185 项测试、0 失败、0 错误、1 项按环境跳过；开发前基线为 161 项。
- 产物：`target/imiocode-0.2.0-SNAPSHOT-all.jar` 已生成，并通过管道输入 `/exit` 实际启动和正常退出，进程退出码为 0。
- 安全检查：Ch5 源码与文档 diff 中凭据样式匹配数为 0；`config.yaml` 无变更。
- tmux：Windows 环境找不到 `tmux`；`wsl.exe --status` 返回 `E_ACCESSDENIED`，因此 tmux 普通任务、Plan Mode 和缓存 usage 端到端条目保持未通过。
- 真实 DeepSeek 验收：外部执行权限因请求会把项目派生 Prompt/文件内容发送到 DeepSeek 而被拒绝；未发送请求、未消耗 Token，也未伪造缓存命中。
- 五个定性场景：文档结构已验证完整，但需要在具备 tmux 且明确允许向当前 Provider 发送场景内容的环境中人工执行，所以五个场景的行为条目保持未勾选。
