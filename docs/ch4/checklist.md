# ImioCode 第四章：Agent Loop Checklist

> 每一项都必须通过运行测试、捕获请求或观察真实行为验证。没有证据的项目不得标记为通过。

## 配置与兼容性

- [ ] 旧 `config.yaml` 不增加 `agent` 节点时，最大轮数为 20、任务超时为 600 秒、安全工具并发数为 4。（验证：运行 `ConfigLoaderTest` 默认配置用例）
- [ ] YAML 可以分别配置 `max-iterations`、`timeout-seconds` 和 `max-parallel-tools`。（验证：运行 `YamlConfigLoaderTest` 并检查最终 `AgentConfig`）
- [ ] 三个 `IMIO_AGENT_*` 环境变量只覆盖各自字段，其他值继续来自 YAML。（验证：运行混合配置测试）
- [ ] 空白环境变量不会抹掉 YAML Agent 配置。（验证：运行空白环境变量测试）
- [ ] 零值、负值、非数字和整数溢出会阻止启动。（验证：运行配置失败参数化测试）
- [ ] 配置错误指出具体 YAML 或环境变量名称，且不包含 API Key。（验证：检查异常文本）
- [ ] `config.example.yaml` 包含默认 Agent 配置且不含真实 Key。（验证：读取示例并运行密钥模式扫描）
- [ ] `AppConfig` 旧构造入口继续产生有效默认 Agent 配置。（验证：运行现有 Provider 和配置测试）

## 请求级工具选择

- [ ] `ToolSelection.allEnabled()` 允许注册中心内全部已启用工具。（验证：运行 `ToolRegistryTest`）
- [ ] `ToolSelection.only()` 只允许指定且当前已启用的工具。（验证：运行过滤查询测试）
- [ ] 已禁用工具即使出现在允许集合中也不可查询、不可导出。（验证：禁用工具过滤测试）
- [ ] 未知工具名称不会因 ToolSelection 变为可执行。（验证：未知名称测试）
- [ ] 过滤后的工具定义顺序稳定。（验证：比较两次导出名称列表）
- [ ] 旧 `ChatRequest` 构造方式默认使用全部已启用工具。（验证：运行 Ch2、Ch3 Provider 回归测试）
- [ ] 空允许集合不会被解释成 unrestricted。（验证：三家请求 JSON 中 tools 为空）

## 三家 Provider 工具过滤

- [ ] Anthropic 请求只包含 ToolSelection 允许的已启用工具。（验证：捕获 `/v1/messages` 请求 JSON）
- [ ] OpenAI 请求只包含 ToolSelection 允许的已启用工具。（验证：捕获 `/v1/responses` 请求 JSON）
- [ ] DeepSeek 请求只包含 ToolSelection 允许的已启用工具。（验证：捕获 `/chat/completions` 请求 JSON）
- [ ] 三家空选择都不暴露 WriteFile、EditFile 或 Bash。（验证：检查三组捕获请求）
- [ ] 三家默认请求仍暴露原有六工具，Schema 格式没有改变。（验证：运行原 Provider 请求映射测试）
- [ ] 工具过滤不改变消息历史、Thinking、Usage 或结果回传格式。（验证：运行三个 Provider 全部测试）

## 当前模型请求取消

- [ ] `cancelActiveRequest()` 能中断 Anthropic 活动请求而不永久关闭客户端。（验证：慢速流取消后再次正常请求）
- [ ] `cancelActiveRequest()` 能中断 OpenAI 活动请求而不永久关闭客户端。（验证：慢速流取消后再次正常请求）
- [ ] `cancelActiveRequest()` 能中断 DeepSeek 活动请求而不永久关闭客户端。（验证：慢速流取消后再次正常请求）
- [ ] 取消的响应没有 `StreamCompleted`。（验证：记录富事件列表）
- [ ] 取消后释放 future、输入流和响应流。（验证：关闭 Mock Server 后测试进程无挂起）
- [ ] `close()` 仍永久关闭客户端并拒绝新请求。（验证：三家关闭契约测试）
- [ ] 重复取消和重复关闭不抛异常。（验证：公共契约测试）

## AgentEvent 与流式收集器

- [ ] Agent 事件覆盖任务开始、轮次开始、模型流、工具批次、模式切换和最终状态。（验证：运行 `AgentEventTest`）
- [ ] 负轮次、负调用索引、空工具名和空错误被拒绝。（验证：事件构造失败测试）
- [ ] Thinking 文本增量在模型生成期间实时发布。（验证：阻塞流中等待首个 `ThinkingDelta`）
- [ ] 最终回答文本在 Collector 返回前逐片发布。（验证：阻塞流中等待首个 `TextDelta`）
- [ ] 完整 `ChatResponse` 保留 Thinking、文本、工具调用和 Usage。（验证：检查 Collector 返回消息）
- [ ] Thinking 签名和 OpenAI encrypted content 不进入任何 AgentEvent。（验证：使用唯一标记扫描事件）
- [ ] 工具参数 JSON 碎片不进入 AgentEvent。（验证：使用唯一参数标记扫描事件）
- [ ] 模型工具开始事件只暴露轮次、索引、调用 ID 和工具名。（验证：检查事件字段）
- [ ] 缺少 `StreamCompleted` 时 Collector 返回协议错误。（验证：截断流测试）
- [ ] 重复 `StreamCompleted` 时 Collector 返回协议错误且 Agent 不继续。（验证：重复完成测试）
- [ ] LLM 异常保留安全错误类型、recoverable 和 Retry-After。（验证：Collector 错误测试）
- [ ] 旧文本 LlmClient 经兼容桥只补发一次 `StreamCompleted`。（验证：`LlmClientContractTest`）

## 工具分区

- [ ] 连续 LOW 风险调用合并成一个 `PARALLEL_SAFE` 批次。（验证：`ToolCallPartitionerTest`）
- [ ] MEDIUM 风险调用各自形成单元素 `SERIAL_BARRIER`。（验证：混合风险序列测试）
- [ ] HIGH 风险调用各自形成单元素 `SERIAL_BARRIER`。（验证：混合风险序列测试）
- [ ] 屏障后的 LOW 风险调用形成新的安全批次。（验证：比较完整批次列表）
- [ ] 未知工具形成串行屏障且不会执行。（验证：未知工具分区及执行测试）
- [ ] 已禁用工具形成串行屏障且不会执行。（验证：禁用工具分区及执行测试）
- [ ] Plan Mode 禁止工具形成串行屏障且不会执行。（验证：只读 ToolSelection 测试）
- [ ] 空工具列表得到空批次列表。（验证：空输入测试）
- [ ] 每个原始调用恰好出现在一个批次中，原始索引不变。（验证：索引集合断言）

## 工具分批执行

- [ ] 两个连续安全工具真实同时处于 RUNNING。（验证：使用 latch 运行 `ToolBatchExecutorTest`）
- [ ] 同时运行的安全工具数不超过配置上限。（验证：记录最大活动计数）
- [ ] 单个安全工具失败不取消同批其他工具。（验证：一个失败、一个成功的并发批次）
- [ ] 并发工具乱序结束后，结果按原始调用顺序返回。（验证：使用不同延迟并比较 call ID）
- [ ] 前一安全批次全部结束后才启动写工具。（验证：记录纳秒级开始/结束时间）
- [ ] 写工具结束后才启动后一安全批次。（验证：比较时间区间）
- [ ] WriteFile、EditFile 和 Bash 彼此没有执行重叠。（验证：三个屏障工具活动计数始终为 1）
- [ ] 未知、禁用和 Plan Mode 禁止工具只产生失败结果，不调用工具实现。（验证：工具调用计数为 0）
- [ ] 每个工具依次发布 queued、running、succeeded/failed。（验证：按 call index 检查事件序列）
- [ ] 批次开始和结束事件数量一一对应。（验证：统计 `ToolBatchStarted/Completed`）
- [ ] 取消时活动安全工具均收到 cancel，待执行屏障不会启动。（验证：阻塞工具取消测试）
- [ ] 重复 cancel/close 幂等且无活动线程残留。（验证：执行器关闭测试）

## Agent Loop 基本行为

- [ ] 单轮纯文本响应产生一次模型请求并以 FINAL_RESPONSE 完成。（验证：`AgentTest` 纯文本用例）
- [ ] 模型连续请求两批工具后，Agent 自动发起第三次模型请求并得到最终回答。（验证：多轮序列客户端）
- [ ] 每轮请求包含正式历史、用户消息和本任务此前完整临时轨迹。（验证：检查每次捕获的 ChatRequest）
- [ ] 每批助手响应在对应工具结果之前进入临时轨迹。（验证：比较 trajectory 角色和部分顺序）
- [ ] 每批工具结果只生成一条 TOOL 消息，结果按原始调用顺序排列。（验证：检查 ToolResultPart 列表）
- [ ] 工具失败结果进入下一轮模型上下文。（验证：下一请求包含失败 ToolResult）
- [ ] 工具失败后模型可以请求另一工具并最终完成。（验证：失败修正场景）
- [ ] Agent Loop 不包含 OpenAI、Anthropic 或 DeepSeek 类型分支。（验证：代码搜索和公共契约测试）
- [ ] 同一 Agent 同时只能运行一个任务。（验证：第二个并发 `run()` 得到安全拒绝）
- [ ] 一个任务结束后可正常启动下一任务。（验证：连续运行两个任务）

## 五种停止条件

- [ ] 模型不再请求工具时发布一次 `TaskCompleted`，stop reason 为 FINAL_RESPONSE。（验证：事件列表及 AgentResult）
- [ ] 达到最大轮数时发布一次 `TaskStopped(MAX_ITERATIONS)`。（验证：固定每轮请求工具的模型替身）
- [ ] 最后一轮仍返回工具时该批工具不会执行。（验证：工具调用计数）
- [ ] 达到任务截止时间时发布一次 `TaskStopped(TIMEOUT)`。（验证：阻塞模型和固定短超时）
- [ ] 超时后不再启动工具或模型请求。（验证：超时前后调用计数不增长）
- [ ] LLM 或协议错误时发布一次 `TaskFailed`，stop reason 为 ERROR。（验证：错误客户端）
- [ ] 用户取消时发布一次 `TaskStopped(CANCELLED)`。（验证：活动任务取消）
- [ ] 程序关闭使用 CANCELLED 清理活动任务并永久关闭 Agent。（验证：关闭测试）
- [ ] 完成、轮数、超时、错误和取消事件互斥，每个任务只出现一个最终事件。（验证：参数化统计）
- [ ] 最终回答与超时竞争时只由一个 CAS 获胜。（验证：并发 barrier 构造竞态并重复运行）
- [ ] TIMEOUT 或 CANCELLED 后同一未关闭 Agent 可继续完成下一任务。（验证：恢复性测试）

## Plan Mode

- [ ] Agent 默认模式为 DO。（验证：新 Agent 的 `mode()`）
- [ ] `/plan` 切换为 PLAN 并发布一个 ModeChanged。（验证：`ConversationLoopTest`）
- [ ] `/plan` 本身不请求模型。（验证：客户端调用次数不变）
- [ ] `/plan` 本身不增加会话历史。（验证：切换前后 historySnapshot 相同）
- [ ] PLAN 任务每轮只公开 `read_file`、`glob`、`grep`。（验证：检查所有 ChatRequest 的 ToolSelection）
- [ ] PLAN 任务每轮携带固定规划提醒。（验证：比较多轮 reminder 列表）
- [ ] PLAN 任务可连续执行多批只读工具后输出计划。（验证：多轮 Plan Agent 场景）
- [ ] PLAN 中伪造的 WriteFile、EditFile 或 Bash 不会执行。（验证：三个工具调用计数为 0）
- [ ] 禁止工具的失败结果会回传模型，使其能够改为输出计划。（验证：检查下一轮上下文）
- [ ] `/do` 切换回 DO 并发布 ModeChanged。（验证：命令测试）
- [ ] `/do` 本身不请求模型、不增加历史。（验证：调用次数和历史快照）
- [ ] `/do` 不自动执行上次计划。（验证：切换后工具计数仍为 0）
- [ ] DO 模式下一任务恢复全部六个已启用工具。（验证：检查 ToolSelection 和请求 JSON）
- [ ] 模式保持到再次切换或程序退出。（验证：连续两个任务使用相同模式）

## 提醒与历史事务

- [ ] 一次性提醒在同一 Agent 任务的每次模型请求中完全一致。（验证：捕获三轮 ChatRequest）
- [ ] Agent 任务开始时提醒即被消费。（验证：下一任务 reminders 为空）
- [ ] 成功、最大轮数、超时、错误和取消后旧提醒均不重复。（验证：参数化失败后下一任务）
- [ ] 提醒不作为 ChatMessage 写入正式历史。（验证：扫描 historySnapshot）
- [ ] 成功任务提交用户消息、所有助手消息、工具结果和最终回答。（验证：完整轨迹角色顺序）
- [ ] 完整轨迹在 Agent 成功前不可从 historySnapshot 观察到。（验证：阻塞最终响应时读取历史）
- [ ] 最大轮数停止不提交任何本任务消息。（验证：停止前后历史相同）
- [ ] 超时不提交任何本任务消息。（验证：超时前后历史相同）
- [ ] 错误不提交任何本任务消息。（验证：错误前后历史相同）
- [ ] 用户取消不提交任何本任务消息。（验证：取消前后历史相同）
- [ ] 已开始 MEDIUM/HIGH 工具后异常结束时 `sideEffectsPossible=true`。（验证：写工具后模型失败）
- [ ] 只有 LOW 工具执行后异常结束时不会虚假标记写入副作用。（验证：只读工具后错误）
- [ ] Retry-After 从 LLM 错误透传到 ConversationException。（验证：429 Agent 场景）

## 终端 UI

- [ ] 每轮 Thinking 使用独立 Thinking 行，不与最终回答重叠。（验证：内存终端捕获输出）
- [ ] 每轮文本按增量顺序显示。（验证：比较 fake terminal deltas）
- [ ] 每轮实际 Usage 分别显示，不伪造跨轮总计。（验证：三轮不同 Usage）
- [ ] 工具等待、运行、成功或失败状态来自真实 ToolExecutionChanged。（验证：事件和输出一一对应）
- [ ] AgentEvent 的工具请求不会提前显示执行成功。（验证：模型事件与工具事件间状态）
- [ ] `/plan` 和 `/do` 显示当前模式。（验证：富终端与 dumb terminal 输出）
- [ ] 最大轮数和超时显示不同停止原因。（验证：捕获两种输出）
- [ ] 已有不安全工具执行时，失败提示包含“部分操作可能已经执行”。（验证：副作用失败场景）
- [ ] 429 显示等待建议但不自动重试。（验证：请求次数为 1）
- [ ] API Key、认证头、Thinking 签名、encrypted content 和写文件正文标记均不出现在终端。（验证：唯一标记扫描）
- [ ] dumb terminal 不输出 ANSI，Plan Mode 和 Agent 停止信息仍可理解。（验证：`JLineTerminalUiTest`）
- [ ] `/exit`、`/quit` 和 Ctrl+C 保持正常退出。（验证：循环测试和进程测试）

## 中断与资源释放

- [ ] 模型流中断后无新文本、工具或模型请求。（验证：取消后等待并比较事件数量）
- [ ] 安全并发批次中断后所有活动工具结束。（验证：活动计数归零）
- [ ] Bash 中断后活动进程终止。（验证：`BashToolTest` 和 Agent 取消集成测试）
- [ ] 中断后未开始的写工具不执行。（验证：安全批次后放置写屏障）
- [ ] Agent 超时看门 future 在正常完成后取消。（验证：无延迟超时事件）
- [ ] Agent 关闭后看门执行器终止。（验证：线程/任务状态）
- [ ] Provider、工具执行器和 Agent 重复关闭无异常。（验证：幂等关闭测试）
- [ ] 应用进程退出后无遗留测试命令或 Java 子进程。（验证：进程 E2E finally 与进程列表）

## 编译与自动化测试

- [ ] Java 21 主源码从干净状态编译通过。（验证：`mvn -q clean compile`）
- [ ] 测试源码编译通过。（验证：`mvn -q test-compile`）
- [ ] Agent 配置测试通过。（验证：`mvn -q -Dtest=ConfigLoaderTest,YamlConfigLoaderTest test`）
- [ ] Agent 事件和 Collector 测试通过。（验证：`mvn -q -Dtest=AgentEventTest,StreamingResponseCollectorTest test`）
- [ ] 分区器和批次执行测试通过。（验证：`mvn -q -Dtest=ToolCallPartitionerTest,ToolBatchExecutorTest,ToolExecutorTest test`）
- [ ] Agent 循环和取消测试通过。（验证：`mvn -q -Dtest=AgentTest,AgentCancellationTest test`）
- [ ] 会话和终端测试通过。（验证：`mvn -q -Dtest=ConversationSessionTest,ConversationLoopTest,JLineTerminalUiTest test`）
- [ ] 三家 Provider 测试通过。（验证：运行三个 Provider 测试类）
- [ ] Ch2 富事件和 Thinking 测试继续通过。（验证：运行 `io.imiocode.llm` 全部测试）
- [ ] Ch3 六工具和工作区安全测试继续通过。（验证：运行 `io.imiocode.tool` 全部测试）
- [ ] 全量测试 0 failures、0 errors。（验证：`mvn -q clean test` 并统计 Surefire XML）
- [ ] `git diff --check` 无空白错误。（验证：运行命令）
- [ ] shaded 可执行 JAR 生成。（验证：`mvn -q clean package` 和检查 `target/*-all.jar`）
- [ ] 无配置目录启动安全退出且无堆栈、无密钥。（验证：运行可执行 JAR 并检查退出码）
- [ ] 本地 API Key、测试签名、encrypted content 和写入正文标记不在生产 JAR。（验证：二进制字符串扫描）
- [ ] `claude.md` 本地修改未进入 Ch4 提交。（验证：`git status` 和 staged diff）

## Spec AC1–AC22 映射

- [ ] AC1：至少两批工具后自动得到最终回答。（验证：`AgentTest` 多轮用例）
- [ ] AC2：无工具响应立即完成。（验证：`AgentTest` 单轮用例）
- [ ] AC3：五种停止条件可区分且终态唯一。（验证：Agent 参数化停止测试）
- [ ] AC4：流实时透传并返回完整结构化响应。（验证：Collector 阻塞流测试）
- [ ] AC5：截断和无效响应不执行工具、不继续循环。（验证：Collector 与 Agent 错误测试）
- [ ] AC6：工具失败进入下一轮并可修正。（验证：Agent 失败修正场景）
- [ ] AC7：安全工具真实有界并发。（验证：ToolBatchExecutor latch 测试）
- [ ] AC8：不安全工具保持顺序屏障。（验证：时间区间断言）
- [ ] AC9：乱序完成仍按原调用顺序回传。（验证：延迟工具排序测试）
- [ ] AC10：模型、工具和 Bash 中断都停止后续工作且不提交历史。（验证：AgentCancellationTest）
- [ ] AC11：AgentEvent 可独立还原任务生命周期，Agent 无终端依赖。（验证：事件序列及代码依赖检查）
- [ ] AC12：`/plan` 零请求、零历史且只公开读工具。（验证：ConversationLoopTest + 请求捕获）
- [ ] AC13：Plan Mode 写工具和 Bash 不执行。（验证：伪造调用测试）
- [ ] AC14：`/do` 零请求、零历史、不自动执行并恢复六工具。（验证：模式切换测试）
- [ ] AC15：Agent 默认值、YAML、环境变量和非法值正确。（验证：配置测试）
- [ ] AC16：提醒同任务复用、结束后消费、不进历史。（验证：会话提醒测试）
- [ ] AC17：成功完整轨迹原子提交。（验证：阻塞提交及最终历史测试）
- [ ] AC18：非成功轨迹不提交，副作用提示准确。（验证：四类失败和写工具场景）
- [ ] AC19：三家模拟协议下 Agent 行为一致。（验证：公共契约和 Provider 测试）
- [ ] AC20：Ch2、Ch3、Unicode、安全和退出回归通过。（验证：全量测试）
- [ ] AC21：真实 Java 进程完成读、写、命令验证和最终回答。（验证：进程 E2E）
- [ ] AC22：Java 21 构建、全测、打包和 tmux 记录完成。（验证：最终验收报告）

## 真实 Java 进程端到端

- [ ] 本地 Mock Server 提供“读取 → 写入 → Bash 验证 → 最终回答”四次模型响应。（验证：服务器收到四次请求）
- [ ] 启动真实 `ImioCodeApplication` 进程并显示 Ready。（验证：捕获子进程输出）
- [ ] 用户只输入一次任务，应用自动完成所有 Agent 轮次。（验证：输入流只写入一次任务和 `/exit`）
- [ ] 第二次请求包含 ReadFile 结果。（验证：捕获请求 JSON）
- [ ] 第三次请求包含 WriteFile 成功结果。（验证：捕获请求 JSON）
- [ ] 第四次请求包含 Bash 验证结果。（验证：捕获请求 JSON）
- [ ] 最终文件内容符合模型要求。（验证：读取测试目标文件）
- [ ] 终端显示 Thinking、三类工具状态、每轮 Usage 和最终回答。（验证：检查完整输出）
- [ ] `/plan`、只读任务、`/do` 在同一真实进程中可切换。（验证：捕获模式输出和请求工具列表）
- [ ] `/exit` 后进程退出码为 0，无异常堆栈和挂起线程。（验证：等待进程并检查输出）
- [ ] 测试结束删除临时目标文件。（验证：finally 后文件不存在）

## tmux 人工端到端

### 场景一：多步 Agent 任务

- [ ] 在 tmux 会话 `imiocode-ch4` 中启动 shaded JAR。（验证：`tmux capture-pane -p -t imiocode-ch4` 包含 Ready）
- [ ] 输入“读取 pom.xml，把项目说明写入 target/ch4-agent.txt，再运行命令验证文件内容并总结”。（验证：捕获完整 pane）
- [ ] 观察至少三轮工具调用自动连续执行，期间没有再次等待用户输入。（验证：检查提示符出现次数）
- [ ] ReadFile、WriteFile 和 Bash 按依赖顺序执行。（验证：比较工具状态行）
- [ ] 最终文件存在、验证命令成功且模型输出总结。（验证：读取文件并检查 pane）
- [ ] 输入 `/exit` 后 pane 正常结束。（验证：检查 pane_dead 或会话结束）

### 场景二：Plan Mode

- [ ] 输入 `/plan` 后显示 PLAN，且没有模型活动。（验证：切换后立即捕获 pane）
- [ ] 输入“分析实现 Agent Loop 需要修改哪些文件”，只出现 ReadFile、Glob、Grep。（验证：检查工具名称）
- [ ] 最终输出实施计划，没有文件变化和 Bash 调用。（验证：`git status --short` 与 pane）
- [ ] 输入 `/do` 后显示 DO，不自动执行计划。（验证：等待后无新工具事件）

### 场景三：安全并发与顺序屏障

- [ ] 让模型同时调用两个读工具，观察两个工具都进入运行状态后才完成批次。（验证：捕获状态顺序）
- [ ] 后续写工具只在读批次完成后运行。（验证：比较 pane 行顺序）
- [ ] 写工具运行时没有其他工具处于 running。（验证：人工检查捕获输出）

### 场景四：中断

- [ ] 让 Agent 调用长时间 Bash，在运行时发送 Ctrl+C。（验证：`tmux send-keys -t imiocode-ch4 C-c`）
- [ ] Bash、Agent 和后续模型请求停止。（验证：pane 内容不再增长且无新工具）
- [ ] 未显示完成事件或下一输入提示。（验证：最终 pane）
- [ ] 无遗留等待子进程。（验证：检查进程列表）

## 最终验收记录

| 项目 | 结果 | 证据 |
|---|---|---|
| 自动化测试 | 通过 | `mvn -q clean package`：143 tests，0 failures，0 errors，1 skipped；40 个测试套件 |
| Agent 多轮循环 | 通过 | `AgentTest` 5/5；覆盖单轮、多轮工具回传、末轮停止和监听器异常 |
| 安全并发与屏障 | 通过 | `ToolBatchExecutorTest` 2/2；验证安全工具真实并发、并发上限、原顺序结果和禁止工具不执行 |
| 五种停止条件 | 通过 | `AgentTest` + `AgentCancellationTest` 7/7；覆盖 FINAL、MAX、ERROR、TIMEOUT、CANCELLED |
| Plan Mode | 通过 | Agent 与终端测试验证只导出 ReadFile/Glob/Grep、附加固定提醒、`/plan`/`/do` 不请求模型 |
| 历史事务 | 通过 | `ConversationSessionTest` 8/8；成功提交完整多轮轨迹，停止和失败不提交 |
| 三家 Provider | 通过 | Anthropic 5/5、OpenAI 6/6、DeepSeek 5/5；工具过滤和交错工具顺序回归通过 |
| 真实 Java 进程 | 部分通过 | shaded JAR 真实启动并输入 `/exit`，显示 Ready 且退出码 0；未执行真实联网模型任务 |
| 安全扫描 | 通过 | 扫描生产源码、测试报告和 shaded JAR 等 227 个文件，本地配置 Key 匹配文件数为 0 |
| tmux | 环境阻塞 | Windows 未安装 tmux；WSL 存在但创建实例返回 `E_ACCESSDENIED`，无法取得 capture-pane |

> 2026-07-27 验收说明：tmux 阻塞属于本机环境限制。自动化 Agent、多轮工具、Plan Mode、
> 超时/取消与真实 JAR 启动均已执行；没有把未运行的 tmux 场景标记为通过。
