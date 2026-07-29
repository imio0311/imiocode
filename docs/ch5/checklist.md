# ImioCode 第五章：Prompt 工程体系 Checklist

> 每一项必须通过运行命令或观察真实行为验证。未执行、无证据或受外部条件阻塞的项目不得标记为通过。

## 实现完整性

- [ ] **七个固定模块均已存在并可参与组装（AC1）**  
  （验证：运行 `mvn -q -Dtest=SystemPromptBuilderTest test`，观察七个模块完整性断言通过。）

- [ ] **乱序注册仍按 Identity → Behavior → ToolUsage → CodeQuality → Security → TaskPattern → OutputStyle 输出（AC1）**  
  （验证：运行 Builder 乱序输入测试，比较实际标题顺序。）

- [ ] **相同输入重复组装的 System Prompt 完全一致（AC1）**  
  （验证：运行稳定性测试，比较两次输出字符串及其长度。）

- [ ] **System Prompt 不包含工作目录、时间、Git 状态或 Plan Mode 文本（AC1、AC5）**  
  （验证：在组装测试中搜索动态字段和 Plan 提醒关键字，期望均不存在。）

- [ ] **统一管线只输出 system、messages、tools 三个内容通道（AC2）**  
  （验证：运行 `mvn -q -Dtest=PromptAssemblerTest test`，检查 ApiPayload 三通道断言。）

- [ ] **七类信息进入正确通道（AC2）**  
  （验证：构造同时包含环境、会话提醒、历史、当前轨迹、轮次提醒和工具的请求，观察各通道内容。）

- [ ] **工具选择只影响 tools，不改写 System Prompt（AC2、AC5）**  
  （验证：分别组装普通模式和 Plan Mode 请求，比较 system 完全相同、tools 分别为六个和三个只读工具。）

## 环境与提醒

- [ ] **每个 Agent 任务只采集一次环境（AC3）**  
  （验证：使用计数型环境提供者运行多轮 Agent 测试，期望单任务计数为 1。）

- [ ] **同一任务的每轮环境提醒完全一致（AC3）**  
  （验证：捕获至少两轮 ChatRequest，比较环境提醒文本。）

- [ ] **下一个用户任务重新采集环境（AC3）**  
  （验证：连续运行两个任务，期望计数为 2 且第二任务使用新的快照。）

- [ ] **环境提醒包含工作区、OS、Shell、时间/时区、Git 分支和状态（AC3）**  
  （验证：运行 `mvn -q -Dtest=EnvironmentContextCollectorTest,EnvironmentReminderFormatterTest test`，观察字段完整性断言。）

- [ ] **环境采集不注入环境变量值、凭据、Git 文件名或原始命令输出（AC3）**  
  （验证：向测试环境放入哨兵秘密值和带名称的 dirty 文件，期望提醒中搜索不到哨兵及文件名。）

- [ ] **Git 不存在、非仓库或超时时安全降级（AC3）**  
  （验证：运行环境采集失败场景，期望返回 unavailable/not-repository 且 Agent 仍能继续。）

- [ ] **每条提醒均为独立逻辑 user 消息并带完整 XML 标签（AC4）**  
  （验证：运行 `mvn -q -Dtest=SystemReminderTest,PromptAssemblerTest test`，检查角色、起止标签和消息数量。）

- [ ] **提醒顺序为环境 → 会话 → 历史/当前轨迹 → 轮次（AC4）**  
  （验证：在每类内容中放入唯一标记，比较 ApiPayload.messages 的实际顺序。）

- [ ] **用户原始输入未被拼接或改写（AC4）**  
  （验证：组装前后比较原始 `ChatMessage` 内容和对象字段。）

- [ ] **提醒不进入正式会话历史（AC3、AC4、AC5）**  
  （验证：任务完成后读取 history snapshot，搜索环境、会话及 Plan 提醒标记，期望均不存在。）

## Plan Mode

- [ ] **Plan Mode 第 1、6、11 轮使用完整提醒（AC5）**  
  （验证：运行 `mvn -q -Dtest=PlanModePromptTest test`，观察完整提醒周期断言。）

- [ ] **Plan Mode 第 2～5、7～10 轮使用精简提醒（AC5）**  
  （验证：运行相同测试，观察精简提醒周期断言。）

- [ ] **普通模式不产生 Plan Mode 提醒（AC5）**  
  （验证：对普通模式调用轮次提醒接口，期望返回空。）

- [ ] **Plan Mode 只向模型公开 ReadFile、Glob、Grep（AC5）**  
  （验证：捕获 Plan Mode Provider 请求，检查工具名称集合严格等于 `read_file`、`glob`、`grep`。）

- [ ] **切换 `/do` 后恢复六个工具且无 Plan 提醒（AC5）**  
  （验证：终端输入 `/do` 后发起下一任务，捕获请求并检查工具集合与消息。）

## 工具描述

- [ ] **ReadFile 描述说明局部读取及修改前优先读取（AC6）**  
  （验证：运行 `mvn -q -Dtest=CoreToolDescriptionTest test`，检查关键约束。）

- [ ] **Glob 描述说明按路径发现候选文件并优先用于探索（AC6）**  
  （验证：运行相同描述契约测试。）

- [ ] **Grep 描述说明搜索内容并与 ReadFile 配合（AC6）**  
  （验证：运行相同描述契约测试。）

- [ ] **EditFile 描述说明唯一匹配、精确旧文本和先读取（AC6）**  
  （验证：运行相同描述契约测试。）

- [ ] **WriteFile 描述说明新建/完整覆盖及局部修改优先 EditFile（AC6）**  
  （验证：运行相同描述契约测试。）

- [ ] **Bash 描述说明用于构建测试且不能替代专用工具（AC6）**  
  （验证：运行相同描述契约测试。）

- [ ] **六个工具的名称、参数 Schema 和风险级别未变化（AC6、AC12）**  
  （验证：运行六个原有工具测试和描述 Schema 回归断言。）

## Provider 请求结构

- [ ] **OpenAI 将七模块 Prompt 放入 instructions，提醒只出现在 input user 消息（AC7）**  
  （验证：运行 `mvn -q -Dtest=OpenAiClientTest test`，检查 Mock Server 捕获的 JSON。）

- [ ] **OpenAI 不发送 cache_control 或不兼容的显式缓存字段（AC7）**  
  （验证：在捕获的请求中搜索 `cache_control`、`prompt_cache_options`、`prompt_cache_breakpoint`，期望不存在。）

- [ ] **Anthropic System Prompt text block 带 ephemeral 缓存标记（AC7）**  
  （验证：运行 `mvn -q -Dtest=AnthropicClientTest test`，检查 system block。）

- [ ] **Anthropic 只有最后一个工具带 ephemeral 标记并覆盖完整工具前缀（AC7）**  
  （验证：检查 tools 数组，前 N-1 个无标记，最后一个标记正确。）

- [ ] **Anthropic 提醒位于 messages，不再进入 system（AC4、AC7）**  
  （验证：检查 system 只包含七模块文本，提醒作为 user content block 出现且顺序不变。）

- [ ] **DeepSeek 只有首条消息是稳定 System Prompt（AC7）**  
  （验证：运行 `mvn -q -Dtest=DeepSeekClientTest test`，检查首条角色及 system 消息数量。）

- [ ] **DeepSeek 不发送任何缓存控制字段（AC7）**  
  （验证：搜索 Mock Server 捕获请求，期望不存在 `cache_control`。）

- [ ] **三个 Provider 的动态提醒变化不改写稳定 System Prompt（AC7）**  
  （验证：每家连续构造两个不同环境/轮次请求，比较其 system 通道内容完全一致。）

## Usage 与缓存

- [ ] **OpenAI cached_tokens 和 cache_write_tokens 均映射到统一 usage（AC8）**  
  （验证：运行 OpenAI usage 样例测试，检查 cache-read/cache-write 数值。）

- [ ] **Anthropic cache_read_input_tokens 和 cache_creation_input_tokens 均映射到统一 usage（AC8）**  
  （验证：运行 Anthropic usage 样例测试，检查两个数值。）

- [ ] **DeepSeek prompt_cache_hit_tokens 映射到统一 cache-read（AC8）**  
  （验证：运行 DeepSeek 官方字段样例测试。）

- [ ] **DeepSeek 旧 cached_tokens 格式仍可解析（AC8）**  
  （验证：运行 DeepSeek 兼容样例测试。）

- [ ] **终端能显示 input、output、reasoning、cache-read 和 cache-write（AC8）**  
  （验证：运行 `mvn -q -Dtest=UsageFormatterTest test` 并观察真实请求的 usage 输出。）

- [ ] **当前配置 Provider 的后续 Agent 轮次出现真实 cache-read > 0（AC8）**  
  （验证：在 tmux 中发送一个必然触发“读取工具 → 回传结果 → 再次请求模型”的真实任务，捕获至少两次 usage；后续轮次必须显示非零 `cache-read`。）

- [ ] **真实缓存未命中时如实记录外部限制，不伪造通过（AC9）**  
  （验证：若 cache-read 始终为 0，记录模型、两轮 input、稳定前缀证据、API usage、等待间隔及 Provider 限制；该真实命中项保持未通过或阻塞。）

## 自动化测试与集成

- [ ] **Prompt、环境、提醒、Plan、工具描述和 Provider 定向测试全部通过（AC11）**  
  （验证：依次运行 task.md 中 T3～T17 的测试命令，所有退出码为 0。）

- [ ] **多轮工具调用和工具结果回传无回归（AC12）**  
  （验证：运行 `mvn -q -Dtest=AgentTest,StreamingTurnExecutorTest,ToolBatchExecutorTest test`。）

- [ ] **流式文本、Thinking、ToolUse 和 Usage 事件无回归（AC12）**  
  （验证：运行三个 RichEvent 测试及 `StreamingResponseCollectorTest`。）

- [ ] **会话成功提交和失败不提交规则无回归（AC12）**  
  （验证：运行 `mvn -q -Dtest=ConversationSessionTest,AgentCancellationTest test`。）

- [ ] **OpenAI、Anthropic、DeepSeek 客户端契约全部通过（AC11、AC12）**  
  （验证：运行三个 Provider 测试及 `LlmClientContractTest`。）

## 编译与安全检查

- [ ] **项目使用 Java 21 编译（AC11）**  
  （验证：运行 `java -version` 和 `mvn -version`，确认 Maven 使用 Java 21。）

- [ ] **完整测试、编译和打包成功（AC11、AC12）**  
  （验证：运行 `mvn -q clean verify`，期望退出码 0。）

- [ ] **生成可执行 JAR（AC13）**  
  （验证：检查 `target/imiocode-0.2.0-SNAPSHOT-all.jar` 存在，并运行 `java -jar target/imiocode-0.2.0-SNAPSHOT-all.jar` 能进入终端界面。）

- [ ] **测试数量没有低于开发前基线（AC11、AC12）**  
  （验证：比较 T0 与最终 Surefire 报告的 tests run 总数。）

- [ ] **代码和请求日志不泄露 API Key 或配置秘密（AC3、AC11）**  
  （验证：检查 Git diff、测试输出和 tmux 捕获内容，搜索已知秘密哨兵，期望不存在。）

- [ ] **Git 提交不包含用户原有 `claude.md` 和 `hello.txt` 改动（AC12）**  
  （验证：运行 `git status --short` 和 `git diff --cached --name-only` 检查提交范围。）

## 五个定性评估场景

- [ ] **评估文档包含五个完整场景（AC10）**  
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

- [ ] **tmux 不可用时明确记录阻塞且不标记端到端通过（AC13）**  
  （验证：保存 `tmux -V` 或 WSL/tmux 启动失败的实际输出；仅记录阻塞，不用普通终端结果替代。）
