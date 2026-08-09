# CH12 Hook 系统 Checklist

> 每项都通过测试命令或真实运行行为验证；验收时记录实际证据后勾选。

## 验收证据（2026-08-09）

- Java 21 全量测试：`mvn -q clean test`，实际结果 `537 tests / 0 failures / 0 errors / 3 skipped`。
- Hook 定向测试：事件、条件、模板、配置、Engine、command/HTTP、队列、Agent/工具集成全部通过。
- 真实终端等价 E2E：Windows 无 tmux，使用真实交互子进程启动 `ImioCodeApplication`；验证 Prompt 注入、
  pre-tool 拒绝及模型回传、普通文件写入、file_change 命令、非法配置降级、session_end → shutdown 顺序。
- Fat JAR：`target/imiocode-0.2.0-SNAPSHOT-all.jar`，大小 5,157,699 字节。
- 工作区检查：`git diff --check` 通过；测试进程正常退出，未发现遗留 Hook 子进程。

## 实现完整性

- [x] 15 个事件名全部可解析并严格拒绝未知名称（验证：运行 `HookEventTest`）。
- [x] Hook、四类 Action、HookContext、条件结构、执行结果、通知和 ToolRejectedError 均可被真实 Engine 使用（验证：Hook 包编译及领域测试）。
- [x] 条件支持 `==`、`!=`、`=~`、`~=`，支持纯 `&&`/纯 `||` 短路且拒绝混用（验证：parser/evaluator 测试）。
- [x] `$EVENT`、`$TOOL_NAME`、`$FILE_PATH`、`$MESSAGE`、`$ERROR`、`$TOOL_ARGS.xxx` 正确单次替换（验证：模板测试）。
- [x] command 动作可执行、获得最小上下文环境、限制输出、超时终止进程树（验证：Command executor 测试且检查子进程消失）。
- [x] prompt 动作进入 messages 的 system-reminder，不进入 System Prompt（验证：捕获实际 ChatRequest）。
- [x] HTTP 动作发送模板化 POST JSON，处理非 2xx/超时/过大响应且不泄露 header（验证：本地 HTTP Server 测试）。
- [x] agent 动作返回明确 NOT_IMPLEMENTED，未调用 LLM 或启动线程（验证：占位执行器测试）。
- [x] once 在并发触发下只调度一次，重建 Engine 后可重新执行（验证：并发测试）。
- [x] async 不阻塞主流程，pre-tool/reject 异步配置在启动期被拒绝（验证：Engine 与配置测试）。
- [x] 普通 runHooks 与专用 runPreToolHooks 互相拒绝错误事件类型（验证：Engine API 测试）。

## 配置与安全

- [x] 无 hooks 的现有 config 正常加载，不创建外部动作（验证：ConfigLoader 兼容测试）。
- [x] 合法 hooks 严格保持 YAML 声明顺序（验证：配置映射测试）。
- [x] 重复 id、未知 event/action、非法条件/URI/timeout、缺字段及 reject/async 约束一次汇总（验证：多错误 YAML 测试）。
- [x] 任一配置错误使整个 Hook 集为空，应用仍进入 UI 并显示脱敏诊断（验证：启动集成测试）。
- [x] command 子进程不继承 Provider API Key，HTTP 日志不显示 Authorization value（验证：安全测试）。
- [x] 通知与 prompt 队列容量有界，溢出可观察且 drain 后不重复（验证：队列测试）。
- [x] Hook 自身失败不递归触发 error Hook（验证：递归保护计数测试）。

## Agent、工具和会话集成

- [x] startup/shutdown 各触发一次且关闭顺序正确（验证：应用生命周期测试）。
- [x] 初始/new/resume/close 会话产生匹配的 session_start/session_end，失败切换不产生伪事件（验证：会话集成测试）。
- [x] 每个用户 Agent 任务产生一次 turn_start 和一次 turn_end，包括 completed/stopped/failed（验证：Agent 集成测试）。
- [x] 每次真实 Provider attempt 产生 pre_send，只有完整成功响应产生 post_receive（验证：重试测试）。
- [x] pre-tool 在权限 evaluate 前执行，流式 eager/串行/并行路径每个 call 仅执行一次（验证：scheduler 顺序探针测试）。
- [x] pre-tool reject 不执行工具、不询问权限、不触发 post-tool，且模型收到 `blocked by hook <id>` tool result（验证：多轮 Agent 测试）。
- [x] 工具真实执行后触发 post-tool，业务失败也携带脱敏结果；后置 Hook 失败不覆盖原工具结果（验证：scheduler 测试）。
- [x] 只有 PermissionAction.ASK 触发 permission_request；ALLOW/DENY 不触发（验证：权限集成测试）。
- [x] WriteFile/EditFile 成功各触发一次 file_change，失败不触发（验证：核心工具测试）。
- [x] Bash 成功 start 触发 command_execute，启动失败不触发，Hook command 不递归（验证：核心工具与 Engine 测试）。
- [x] 自动、恢复、手动压缩仅在实际 compact 时触发事件并携带前后 token（验证：上下文集成测试）。
- [x] 主流程最终错误触发 error；error Hook 失败不覆盖原错误（验证：Agent 错误测试）。
- [x] 主 Agent、inline/fork Skill 共享 once 状态，子 Agent 不提前关闭 Engine（验证：Skill 集成测试）。

## UI 与兼容性

- [x] compact 模式只显示 Hook 失败/超时/拒绝，verbose 显示全部（验证：Fake Terminal 测试）。
- [x] 异步通知只在 UI 主线程安全点绘制，不破坏输入框且只显示一次（验证：ConversationLoop 测试与真实终端观察）。
- [x] 未配置 Hook 时既有权限、MCP、Session、Memory、Skill 和 Slash Command 测试保持通过（验证：Maven 全量测试）。
- [x] `config.example.yaml` 可加载，README 示例与真实字段一致（验证：ConfigLoader 测试）。

## 编译与测试

- [x] `mvn -DskipTests package` 编译并生成 fat JAR。
- [x] Hook 定向测试全部通过。
- [x] `mvn test` 全量测试零失败、零错误。
- [x] 测试结束后无遗留 Hook 命令子进程或非守护后台线程。

## 端到端场景

- [x] 场景 1：配置 turn_start/pre_send prompt → 启动真实 ImioCode → 用户提问 → 回答遵守注入指令，日志显示对应 Hook。
- [x] 场景 2：配置 `file_change` command → 让 Agent 修改测试文件 → 文件成功修改且格式化/标记命令自动运行一次。
- [x] 场景 3：配置 `pre_tool_use` reject 写入 `.env` → 让 Agent 尝试写入 → 文件不存在/不变，模型看到拒绝并说明替代方案。
- [x] 场景 4：配置失败的 async 非拦截 Hook → 完成一轮对话 → UI 显示安全失败通知，下一轮对话仍正常完成。
- [x] 场景 5：退出 ImioCode → session_end、shutdown 均执行，进程在有界时间内正常退出。
