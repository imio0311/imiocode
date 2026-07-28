# ImioCode Ch4 Agent Loop 增强验收报告

## 结论

- 自动化与静态验收通过：37/43 项。
- tmux 真实交互阻塞：6/43 项。
- 阻塞项为 AC22、E1、E2、E3、E4、E5；E6“记录真实阻塞证据”已通过。
- 未把未执行的 tmux 场景标记为通过。

## 通过（37/43）

### 流式工具调度与 Agent

- [x] AC1–AC3、S1–S2：LOW 连续前缀、并发上限、写工具屏障、结果顺序、最大轮数末轮不执行。
  - 证据：`StreamingToolSchedulerTest`、`AgentTest` 通过。
- [x] AC4–AC13：可恢复错误重试、工具启动后禁重试、Retry-After/1-2-4 秒策略、取消、重试事件、失败文本分隔和 64000 输出上限。
  - 证据：`LlmRetryPolicyTest`、`StreamingTurnExecutorTest`、`AgentCancellationTest`、`ConversationLoopTest` 通过。
- [x] AC14–AC17、S3：未知工具计数、有效工具清零、禁用/模式禁止保持、第三次熔断、失败尝试快照丢弃。
  - 证据：`UnknownToolCircuitBreakerTest`、`AgentTest`、`JLineTerminalUiTest` 通过。
- [x] AC18、AC20：成功轨迹原子提交，失败/熔断/取消不提交，竞态下工具至多一次且终态唯一。
  - 证据：`ConversationSessionTest`、`StreamingToolSchedulerTest`、`StreamingTurnExecutorTest`、`AgentCancellationTest` 连续运行三次均通过。

### Provider、兼容性与安全

- [x] AC19：OpenAI、Anthropic、DeepSeek 的请求级 token 覆盖和输出上限归一通过。
  - 证据：三家 `ClientTest` 新增 OUTPUT_LIMIT 场景通过；原 RichEvent 测试全量通过。
- [x] I1–I6、I8：任务截止时间、模型专属重试、六个工具兼容、旧配置、敏感字段隔离、正常富事件和 Plan Mode 无回归。
  - 证据：全量测试通过；新增差异敏感模式扫描结果为 0。

### 编译、测试与打包

- [x] B1、AC21、B3–B6：聚焦测试、全量测试、Java 21 编译、打包、差异检查及无真实 API 依赖均通过。
  - 命令：`mvn -q clean package`
  - 实际：Tests=161，Failures=0，Errors=0，Skipped=1，共 44 个测试套件。
  - 产物：`target/imiocode-0.2.0-SNAPSHOT-all.jar`，大小 4,450,428 字节。
  - 差异：`git diff --check` 通过；新增敏感值匹配 0；新增调试语句/TODO/TBD 匹配 0。
- [x] E6：环境阻塞有实际探测命令、退出码和错误文本。

## 阻塞（6/43）

- [ ] AC22、E1–E5：无法在 tmux 内启动 ImioCode，因此没有伪造真实交互输出。

### 阻塞证据

1. Windows 本机：

   ```text
   WINDOWS_TMUX=False
   ```

2. WSL：

   ```text
   TMUX_PATH=/usr/bin/tmux
   tmux 3.4
   JAVA=sh: 1: java: not found
   ```

3. 尝试让 WSL tmux 调用 Windows PowerShell/JDK：

   ```text
   /mnt/c/Windows/System32/WindowsPowerShell/v1.0//powershell.exe
   powershell.exe: Exec format error
   ```

WSL 中 tmux 可用，但没有 Linux Java；当前 WSL 的 Windows 可执行文件互操作不可用，所以无法从 tmux 调用已安装的 Windows JDK 21。Windows 侧可以构建和运行 JAR，但没有 tmux。两边能力无法在当前环境组合，故真实 tmux 场景阻塞。

## 后续复验条件

满足以下任一条件后可继续 AC22、E1–E5：

- 在 WSL 安装 Java 21；或
- 修复 WSL 的 Windows 可执行文件互操作；或
- 在 Windows 环境提供可用 tmux。

复验时使用已生成的 shaded JAR，并按 `checklist.md` 的 E1–E5 保存 pane 输出、请求计数和退出状态。
