# CH13 多 Agent 与后台任务 Checklist

> 已按自动化测试与真实 CLI 行为验收；详细证据见 `validation.md`。

## 定义与加载

- [x] frontmatter/body、必填字段、名称、轮数、超时、工具集合和冲突配置均严格校验。
- [x] 项目 > 用户 > 内置 > 插件覆盖正确，无效高优先级定义会诊断并回退。
- [x] 三个内置 Agent 与外部定义走同一解析器，可列出且正文不进入常驻摘要。

## 统一工具与上下文

- [x] 一个 `agent` 工具可通过 subagent_type 分流 Definition/Fork，两条路径均能完成真实任务。
- [x] Fork Provider 请求保留父历史稳定前缀，子轨迹不写入父会话。
- [x] Definition 正文和 initialPrompt 位于首个任务消息之前。
- [x] 模型调用覆盖 > 定义 > 父模型，haiku alias 映射和回退均可观察。
- [x] cwd 越界与 isolation=worktree 明确拒绝。

## 执行与安全

- [x] RunToCompletion 支持多轮工具、最终文本、最大轮数、超时、取消和错误终态。
- [x] 后台权限 ASK 自动拒绝，任务不会挂起。
- [x] 全局禁止、自定义禁止、后台白名单、定义 tools/disallowedTools 和只读上限逐层生效。
- [x] 被过滤工具既不出现在 API Schema，也不能通过执行解析调用。
- [x] Explore/Plan 不能写文件或执行 Bash，general-purpose 仍经过权限链。

## 后台任务与 Trace

- [x] TaskManager 六种状态转换合法，任务表/执行池/通知队列有界。
- [x] 后台启动、自动超时、取消、关闭回收和 ESC 转后台均可观察。
- [x] task-notification 在 UI 和父 Agent 消息中各回传一次，重复 drain 不重复。
- [x] TraceRegistry 可还原父子链路、模型和终态，Token 五字段聚合正确。
- [x] Trace/Task 重启后不恢复，且日志、详情不泄露 Prompt 或密钥。

## 命令、兼容与质量

- [x] `/tasks`、`/task info <id>`、`/task cancel <id>` 本地执行、补全正确、零 LLM 请求。
- [x] Skill Fork 适配统一的 Agent/历史快照约束且原 CH11 场景不回归。
- [x] 无 agents 配置时 CH2—CH12 全部测试保持通过。
- [x] `config.example.yaml` 与 README 示例可由真实 ConfigLoader 加载。
- [x] `git diff --check`、Java 21 全量测试和 fat JAR 构建通过。

## 端到端场景

- [x] 场景 1：主 Agent 调用 Explore -> 只读探索多轮 -> 最终结果回到父会话。
- [x] 场景 2：Fork 继承真实父历史 -> 完成任务 -> 子轨迹不写回父会话。
- [x] 场景 3：启动后台任务 -> `/tasks` 可见 -> 终态通知注入下一轮。
- [x] 场景 4：前台任务中断转后台 -> 父 UI 恢复 -> `/task cancel` 可取消。
- [x] 场景 5：后台任务遇到权限 ASK -> 自动拒绝 -> 应用继续对话并正常退出。
