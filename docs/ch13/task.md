# CH13 多 Agent 与后台任务 Tasks

## 文件清单

| 操作 | 文件/目录 | 职责 |
|---|---|---|
| 新建 | `src/main/java/io/imiocode/subagent/definition/` | 定义、解析、四来源加载 |
| 新建 | `src/main/java/io/imiocode/subagent/{model,context,filter,runtime,task,trace,command}/` | 运行平台各层 |
| 新建 | `src/main/resources/agents/` | 三个内置定义 |
| 修改 | `agent/Agent.java`、`AgentRequest.java` | 固定工具选择、初始上下文、Token 事件 |
| 修改 | `config/**`、`config.example.yaml` | agents 配置与模型别名 |
| 修改 | `ImioCodeApplication.java` | 装配 AgentTool、TaskManager、Trace、命令 |
| 修改 | `runtime/**`、`terminal/**`、`command/**` | 后台通知、ESC、Slash 命令 |
| 修改 | `skill/DefaultSkillForkRunner.java` | 复用统一运行基础设施 |
| 新建 | `src/test/java/io/imiocode/subagent/**` | 单元、集成与 E2E |

## 有序任务

1. T1：实现 AgentDefinition、来源枚举、诊断与不可变目录快照。验证：领域测试。
2. T2：实现 frontmatter/body 严格解析和全部字段校验。验证：有效/无效样例测试。
3. T3：实现项目、用户、内置、插件四来源加载及优先级回退。验证：临时目录覆盖测试。
4. T4：加入 agents 配置、模型别名与 SubagentModelResolver。验证：三级覆盖和回退测试。
5. T5：实现 ToolFilterPipeline 及 ToolSelection 交集能力。验证：每层 Schema/resolve 双检查。
6. T6：扩展 AgentRequest/Agent 支持固定 ToolSelection、初始提醒和 usage 转发。验证：兼容测试。
7. T7：实现 ForkContextBuilder 与 DefinitionContextBuilder。验证：byte-exact 历史前缀和轨迹隔离。
8. T8：实现 TraceRegistry 和 TokenAccumulator。验证：父子树、并发终态和 Token 汇总。
9. T9：实现 RunToCompletion 与子 Agent client/factory。验证：多轮、超时、取消、权限不挂起。
10. T10：实现 TaskManager、前后台转换和有界通知队列。验证：状态机、超时、关闭、一次 drain。
11. T11：实现统一 AgentTool 参数解析、两路径分发和稳定 ToolResult。验证：同步/后台/非法组合。
12. T12：增加三个内置 Agent 并走同一 Loader。验证：目录、模型、权限和工具限制。
13. T13：实现 `/tasks`、`/task info`、`/task cancel` 与补全。验证：纯本地零 LLM 请求。
14. T14：接入 UI 通知、父消息注入和 ESC foreground→background。验证：Fake UI 与交互测试。
15. T15：应用装配、关闭顺序、Skill Fork 适配及文档示例。验证：旧测试与配置兼容。
16. T16：运行定向、全量、fat JAR 和真实子进程 E2E，逐项更新 checklist。

## 执行顺序

```text
T1 -> T2 -> T3 -> T4
T5 -> T6 -> T7 -> T9
T8 --------^    -> T10 -> T11 -> T12 -> T13 -> T14 -> T15 -> T16
```
