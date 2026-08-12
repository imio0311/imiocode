# CH16 生产代码中文注释审阅记录

> 基线提交：`bb1bb71`。生成日期：2026-08-12。状态由 Git 差异、中文注释语法扫描和简单类型结构复核共同判定。

## 覆盖结论

- 生产 Java 文件：561
- 唯一路径：561
- 新增注释：77
- 保留现有中文注释：339
- 无需补充：145
- 待审阅：0

“无需补充”仅用于名称和字段已自解释的 record、enum、异常或极小协议载体；核心流程、并发、安全、协议和生命周期类型不能仅凭文件短小归入该类。

## 模块汇总

| 模块 | 文件数 | 新增注释 | 保留现有 | 无需补充 |
|---|---:|---:|---:|---:|
| (顶层入口) | 2 | 1 | 1 | 0 |
| agent | 28 | 3 | 22 | 3 |
| command | 29 | 13 | 14 | 2 |
| config | 24 | 3 | 14 | 7 |
| context | 21 | 2 | 10 | 9 |
| conversation | 20 | 5 | 4 | 11 |
| hook | 51 | 7 | 24 | 20 |
| instruction | 10 | 3 | 2 | 5 |
| llm | 21 | 11 | 7 | 3 |
| mcp | 39 | 0 | 39 | 0 |
| memory | 16 | 4 | 4 | 8 |
| permission | 40 | 0 | 40 | 0 |
| persistence | 5 | 2 | 2 | 1 |
| prompt | 25 | 0 | 25 | 0 |
| runtime | 2 | 0 | 2 | 0 |
| session | 22 | 4 | 3 | 15 |
| skill | 48 | 5 | 38 | 5 |
| subagent | 32 | 3 | 15 | 14 |
| team | 61 | 8 | 23 | 30 |
| terminal | 12 | 1 | 11 | 0 |
| tool | 30 | 2 | 25 | 3 |
| worktree | 23 | 0 | 14 | 9 |

## 完整文件清单

| 文件路径 | 处理结果 |
|---|---|

| `src/main/java/io/imiocode/ImioCodeApplication.java` | 新增注释 |
| `src/main/java/io/imiocode/TeamMemberProcess.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/agent/Agent.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/AgentError.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/AgentEvent.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/AgentEventListener.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/AgentHistoryContext.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/AgentMode.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/AgentRequest.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/AgentResult.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/AgentStopReason.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/AgentTaskContext.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/CircuitObservation.java` | 无需补充 |
| `src/main/java/io/imiocode/agent/DefaultRetryWaiter.java` | 新增注释 |
| `src/main/java/io/imiocode/agent/IndexedToolCall.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/LlmRetryPolicy.java` | 新增注释 |
| `src/main/java/io/imiocode/agent/ManagedConversationState.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/PlanModePrompt.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/RetryDecision.java` | 无需补充 |
| `src/main/java/io/imiocode/agent/RetryWaiter.java` | 新增注释 |
| `src/main/java/io/imiocode/agent/StreamingResponseCollector.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/StreamingToolScheduler.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/StreamingTurnExecutor.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/StreamingTurnResult.java` | 无需补充 |
| `src/main/java/io/imiocode/agent/ToolBatch.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/ToolBatchExecutor.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/ToolBatchKind.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/ToolCallPartitioner.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/UnknownToolCircuitBreaker.java` | 保留现有注释 |
| `src/main/java/io/imiocode/agent/UnknownToolCircuitOpenException.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/command/builtin/ClearCommand.java` | 新增注释 |
| `src/main/java/io/imiocode/command/builtin/CompactCommand.java` | 新增注释 |
| `src/main/java/io/imiocode/command/builtin/DoCommand.java` | 新增注释 |
| `src/main/java/io/imiocode/command/builtin/ExitCommand.java` | 新增注释 |
| `src/main/java/io/imiocode/command/builtin/HelpCommand.java` | 新增注释 |
| `src/main/java/io/imiocode/command/builtin/MemoryCommand.java` | 新增注释 |
| `src/main/java/io/imiocode/command/builtin/PermissionCommand.java` | 新增注释 |
| `src/main/java/io/imiocode/command/builtin/PlanCommand.java` | 新增注释 |
| `src/main/java/io/imiocode/command/builtin/ReviewCommand.java` | 新增注释 |
| `src/main/java/io/imiocode/command/builtin/ReviewPromptBuilder.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/builtin/SessionCommand.java` | 新增注释 |
| `src/main/java/io/imiocode/command/builtin/StatusCommand.java` | 新增注释 |
| `src/main/java/io/imiocode/command/builtin/TaskCommand.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/builtin/VerbosityCommand.java` | 新增注释 |
| `src/main/java/io/imiocode/command/builtin/WorktreeCommand.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/Command.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/CommandContext.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/CommandDescriptor.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/CommandMessage.java` | 无需补充 |
| `src/main/java/io/imiocode/command/CommandOutcome.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/CommandParser.java` | 新增注释 |
| `src/main/java/io/imiocode/command/CommandRegistry.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/CommandResult.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/CommandServices.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/CommandStatus.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/CommandType.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/ConfirmationPrompt.java` | 保留现有注释 |
| `src/main/java/io/imiocode/command/ParsedCommand.java` | 无需补充 |
| `src/main/java/io/imiocode/command/UIController.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/config/AgentConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/AppConfig.java` | 新增注释 |
| `src/main/java/io/imiocode/config/ConfigDocument.java` | 新增注释 |
| `src/main/java/io/imiocode/config/ConfigException.java` | 无需补充 |
| `src/main/java/io/imiocode/config/ConfigLoader.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/ConfigNotice.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/ConfigSource.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/ConfigSourceSummary.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/ContextConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/EnvironmentPlaceholderResolver.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/InstructionsConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/MemoryConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/MissingEnvironmentVariableException.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/Provider.java` | 无需补充 |
| `src/main/java/io/imiocode/config/ProviderConfig.java` | 无需补充 |
| `src/main/java/io/imiocode/config/ReasoningEffort.java` | 无需补充 |
| `src/main/java/io/imiocode/config/ReasoningSummary.java` | 无需补充 |
| `src/main/java/io/imiocode/config/RuntimeConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/SessionsConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/ThinkingConfig.java` | 无需补充 |
| `src/main/java/io/imiocode/config/ThinkingMode.java` | 无需补充 |
| `src/main/java/io/imiocode/config/UiConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/UiVerbosity.java` | 保留现有注释 |
| `src/main/java/io/imiocode/config/YamlConfigLoader.java` | 新增注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/context/ApproximateTokenEstimator.java` | 保留现有注释 |
| `src/main/java/io/imiocode/context/AutoCompactTrackingState.java` | 保留现有注释 |
| `src/main/java/io/imiocode/context/CompactReport.java` | 无需补充 |
| `src/main/java/io/imiocode/context/ContextEvent.java` | 无需补充 |
| `src/main/java/io/imiocode/context/ContextEventListener.java` | 新增注释 |
| `src/main/java/io/imiocode/context/ContextException.java` | 无需补充 |
| `src/main/java/io/imiocode/context/ContextManageMode.java` | 无需补充 |
| `src/main/java/io/imiocode/context/ContextManager.java` | 保留现有注释 |
| `src/main/java/io/imiocode/context/ContextOutcome.java` | 无需补充 |
| `src/main/java/io/imiocode/context/ContextPolicy.java` | 保留现有注释 |
| `src/main/java/io/imiocode/context/ContextRequest.java` | 保留现有注释 |
| `src/main/java/io/imiocode/context/ContextResult.java` | 无需补充 |
| `src/main/java/io/imiocode/context/ConversationSerializer.java` | 保留现有注释 |
| `src/main/java/io/imiocode/context/ConversationSummarizer.java` | 保留现有注释 |
| `src/main/java/io/imiocode/context/OffloadResult.java` | 无需补充 |
| `src/main/java/io/imiocode/context/ParsedSummary.java` | 无需补充 |
| `src/main/java/io/imiocode/context/SpilledResult.java` | 无需补充 |
| `src/main/java/io/imiocode/context/SummaryParser.java` | 保留现有注释 |
| `src/main/java/io/imiocode/context/SummaryPrompt.java` | 新增注释 |
| `src/main/java/io/imiocode/context/ToolResultOffloader.java` | 保留现有注释 |
| `src/main/java/io/imiocode/context/ToolResultSpillStore.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/conversation/AnthropicThinkingMetadata.java` | 无需补充 |
| `src/main/java/io/imiocode/conversation/ChatMessage.java` | 新增注释 |
| `src/main/java/io/imiocode/conversation/ChatRequest.java` | 新增注释 |
| `src/main/java/io/imiocode/conversation/ChatResponse.java` | 新增注释 |
| `src/main/java/io/imiocode/conversation/ConversationException.java` | 保留现有注释 |
| `src/main/java/io/imiocode/conversation/ConversationListener.java` | 新增注释 |
| `src/main/java/io/imiocode/conversation/ConversationLoop.java` | 新增注释 |
| `src/main/java/io/imiocode/conversation/ConversationRuntimePolicy.java` | 保留现有注释 |
| `src/main/java/io/imiocode/conversation/ConversationSession.java` | 保留现有注释 |
| `src/main/java/io/imiocode/conversation/DeepSeekReasoningMetadata.java` | 无需补充 |
| `src/main/java/io/imiocode/conversation/MessagePart.java` | 无需补充 |
| `src/main/java/io/imiocode/conversation/MessageRole.java` | 无需补充 |
| `src/main/java/io/imiocode/conversation/OpenAiReasoningMetadata.java` | 无需补充 |
| `src/main/java/io/imiocode/conversation/ReminderScope.java` | 保留现有注释 |
| `src/main/java/io/imiocode/conversation/SystemReminder.java` | 无需补充 |
| `src/main/java/io/imiocode/conversation/TextPart.java` | 无需补充 |
| `src/main/java/io/imiocode/conversation/ThinkingMetadata.java` | 无需补充 |
| `src/main/java/io/imiocode/conversation/ThinkingPart.java` | 无需补充 |
| `src/main/java/io/imiocode/conversation/ToolCallPart.java` | 无需补充 |
| `src/main/java/io/imiocode/conversation/ToolResultPart.java` | 无需补充 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/hook/action/Action.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/action/ActionDispatcher.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/action/AgentAction.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/action/AgentPlaceholderHookExecutor.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/action/CommandAction.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/action/CommandHookExecutor.java` | 新增注释 |
| `src/main/java/io/imiocode/hook/action/HookActionExecutor.java` | 新增注释 |
| `src/main/java/io/imiocode/hook/action/HookActionType.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/action/HookHttpTransport.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/action/HookProcessRunner.java` | 新增注释 |
| `src/main/java/io/imiocode/hook/action/HttpAction.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/action/HttpHookExecutor.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/action/JdkHookHttpTransport.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/action/JdkHookProcessRunner.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/action/PromptAction.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/action/PromptHookExecutor.java` | 新增注释 |
| `src/main/java/io/imiocode/hook/condition/Condition.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/condition/ConditionConnector.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java` | 新增注释 |
| `src/main/java/io/imiocode/hook/condition/ConditionGroup.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/condition/ConditionOperator.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/condition/ConditionParseException.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/condition/ConditionParser.java` | 新增注释 |
| `src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java` | 新增注释 |
| `src/main/java/io/imiocode/hook/condition/DefaultConditionParser.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/condition/HookGlobPattern.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/config/ActionDocument.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/config/HookConfigError.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/config/HookConfigLoadResult.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/config/HookConfigMapper.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/config/HookDocument.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/config/HookValidator.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/DefaultHookEngine.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/Hook.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/HookActionResult.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/HookContext.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/HookEvent.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/HookExecutionException.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/HookExecutionRecord.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/HookExecutionStatus.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/HookFailurePolicy.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/HookNotification.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/HookNotificationQueue.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/HookPromptInbox.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/HookRunResult.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/HookRuntime.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/integration/HookContextFactory.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/integration/HookToolLifecycleListener.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/PreToolHookResult.java` | 无需补充 |
| `src/main/java/io/imiocode/hook/template/HookTemplateResolver.java` | 保留现有注释 |
| `src/main/java/io/imiocode/hook/ToolRejectedError.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/instruction/FileInstructionLoader.java` | 新增注释 |
| `src/main/java/io/imiocode/instruction/GitProjectLocator.java` | 保留现有注释 |
| `src/main/java/io/imiocode/instruction/IncludeExpander.java` | 保留现有注释 |
| `src/main/java/io/imiocode/instruction/InstructionLoader.java` | 新增注释 |
| `src/main/java/io/imiocode/instruction/InstructionLoadRequest.java` | 无需补充 |
| `src/main/java/io/imiocode/instruction/InstructionProblem.java` | 无需补充 |
| `src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java` | 新增注释 |
| `src/main/java/io/imiocode/instruction/InstructionScope.java` | 无需补充 |
| `src/main/java/io/imiocode/instruction/InstructionSnapshot.java` | 无需补充 |
| `src/main/java/io/imiocode/instruction/InstructionSource.java` | 无需补充 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/llm/LlmClient.java` | 新增注释 |
| `src/main/java/io/imiocode/llm/LlmClientFactory.java` | 新增注释 |
| `src/main/java/io/imiocode/llm/LlmErrorType.java` | 无需补充 |
| `src/main/java/io/imiocode/llm/LlmEvent.java` | 新增注释 |
| `src/main/java/io/imiocode/llm/LlmEventListener.java` | 新增注释 |
| `src/main/java/io/imiocode/llm/LlmException.java` | 新增注释 |
| `src/main/java/io/imiocode/llm/LlmStreamAssembler.java` | 保留现有注释 |
| `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicClient.java` | 保留现有注释 |
| `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java` | 新增注释 |
| `src/main/java/io/imiocode/llm/provider/deepseek/DeepSeekClient.java` | 保留现有注释 |
| `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java` | 保留现有注释 |
| `src/main/java/io/imiocode/llm/StreamListener.java` | 新增注释 |
| `src/main/java/io/imiocode/llm/TokenUsage.java` | 无需补充 |
| `src/main/java/io/imiocode/llm/TokenUsageBuilder.java` | 新增注释 |
| `src/main/java/io/imiocode/llm/ToolCallAssembler.java` | 保留现有注释 |
| `src/main/java/io/imiocode/llm/ToolResultJson.java` | 保留现有注释 |
| `src/main/java/io/imiocode/llm/transport/HttpClientFactory.java` | 新增注释 |
| `src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java` | 新增注释 |
| `src/main/java/io/imiocode/llm/transport/RetryAfterParser.java` | 保留现有注释 |
| `src/main/java/io/imiocode/llm/transport/SseEvent.java` | 无需补充 |
| `src/main/java/io/imiocode/llm/transport/SseEventReader.java` | 新增注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/mcp/client/DefaultMcpClient.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/client/McpCallHandle.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/client/McpCallResult.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/client/McpClient.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/client/McpClientState.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/client/McpInitializeResult.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/client/McpRemoteTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/client/McpServerInfo.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/config/McpConfigDocument.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/config/McpConfigError.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/config/McpConfigLoader.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/config/McpConfigLoadResult.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/config/McpEnvironmentResolver.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/config/McpServerConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/config/McpTransportType.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/config/ResolvedMcpServerConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcCodec.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcError.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcId.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcMessage.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcNotification.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcRequest.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/jsonrpc/JsonRpcResponse.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/manager/McpEvent.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/manager/McpEventListener.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/manager/McpEventType.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/manager/McpLaunchApprover.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/manager/McpLaunchRequest.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/manager/McpManager.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/manager/McpStartupResult.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/tool/McpToolName.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/tool/McpToolResultMapper.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/tool/McpToolWrapper.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/transport/McpTransport.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/transport/McpTransportException.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/transport/McpTransportFactory.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/transport/SseMessageDecoder.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/transport/StdioMcpTransport.java` | 保留现有注释 |
| `src/main/java/io/imiocode/mcp/transport/StreamableHttpMcpTransport.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/memory/LlmMemoryExtractor.java` | 保留现有注释 |
| `src/main/java/io/imiocode/memory/MarkdownMemoryStore.java` | 保留现有注释 |
| `src/main/java/io/imiocode/memory/MemoryCandidate.java` | 无需补充 |
| `src/main/java/io/imiocode/memory/MemoryCategory.java` | 无需补充 |
| `src/main/java/io/imiocode/memory/MemoryDocument.java` | 无需补充 |
| `src/main/java/io/imiocode/memory/MemoryEntry.java` | 无需补充 |
| `src/main/java/io/imiocode/memory/MemoryException.java` | 无需补充 |
| `src/main/java/io/imiocode/memory/MemoryExtractionResult.java` | 无需补充 |
| `src/main/java/io/imiocode/memory/MemoryExtractor.java` | 新增注释 |
| `src/main/java/io/imiocode/memory/MemoryManager.java` | 新增注释 |
| `src/main/java/io/imiocode/memory/MemoryReminderFormatter.java` | 新增注释 |
| `src/main/java/io/imiocode/memory/MemoryResponseParser.java` | 保留现有注释 |
| `src/main/java/io/imiocode/memory/MemorySafetyPolicy.java` | 保留现有注释 |
| `src/main/java/io/imiocode/memory/MemoryScope.java` | 无需补充 |
| `src/main/java/io/imiocode/memory/MemoryStore.java` | 新增注释 |
| `src/main/java/io/imiocode/memory/MemoryUpdateReport.java` | 无需补充 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/permission/command/CommandRiskAssessment.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/command/CommandRiskClassifier.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/command/DangerousCommandDetector.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/command/DangerousCommandMatch.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/command/RegexCommandRiskClassifier.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/command/RegexDangerousCommandDetector.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/command/SafeCommandDetector.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/command/SafeCommandResult.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/command/ShellCommandScanner.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/command/ShellCommandScanResult.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/command/ShellCommandTokenizer.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/command/ShellTokenizeResult.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/command/StrictSafeCommandDetector.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionAction.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionChecker.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionCoordinator.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionDecision.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionDecisionSource.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionEvaluation.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionGate.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionMode.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionModePolicy.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionOperation.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionPrompt.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionReply.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionRequest.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionRequestFactory.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionRule.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionRuleLayer.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionSettings.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionSettingsProvider.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/PermissionTargetProvider.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/rule/PermissionConfigDocument.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/rule/PermissionGlobMatcher.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/rule/PermissionRuleEngine.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/rule/PermissionRuleLoader.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/RuntimePermissionSettings.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/sandbox/PathSandbox.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/sandbox/SandboxResult.java` | 保留现有注释 |
| `src/main/java/io/imiocode/permission/sandbox/WorkspacePathSandbox.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/persistence/DefaultPersistentContextProvider.java` | 保留现有注释 |
| `src/main/java/io/imiocode/persistence/FileFingerprint.java` | 保留现有注释 |
| `src/main/java/io/imiocode/persistence/PersistenceEvent.java` | 无需补充 |
| `src/main/java/io/imiocode/persistence/PersistenceEventListener.java` | 新增注释 |
| `src/main/java/io/imiocode/persistence/PersistentContextProvider.java` | 新增注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/prompt/ApiPayload.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/BuildOptions.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/CacheDirective.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/CacheIntent.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/EnvironmentContext.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/EnvironmentContextCollector.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/EnvironmentContextProvider.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/EnvironmentReminderFormatter.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/GitContext.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/GitWorkingTreeState.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/PromptAssembler.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/PromptSection.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/Section.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/section/BehaviorSection.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/section/CodeQualitySection.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/section/CustomInstructionsSection.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/section/IdentitySection.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/section/MemorySection.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/section/OutputStyleSection.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/section/SecuritySection.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/section/SkillSection.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/section/TaskPatternSection.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/section/ToolUsageSection.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/SectionPriority.java` | 保留现有注释 |
| `src/main/java/io/imiocode/prompt/SystemPromptBuilder.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/runtime/ConversationCoordinator.java` | 保留现有注释 |
| `src/main/java/io/imiocode/runtime/ConversationLoop.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/session/JsonlSessionStore.java` | 保留现有注释 |
| `src/main/java/io/imiocode/session/record/MessageRecord.java` | 无需补充 |
| `src/main/java/io/imiocode/session/record/SessionHeaderRecord.java` | 无需补充 |
| `src/main/java/io/imiocode/session/record/SessionRecordCodec.java` | 保留现有注释 |
| `src/main/java/io/imiocode/session/record/StoredMessage.java` | 无需补充 |
| `src/main/java/io/imiocode/session/record/StoredPart.java` | 无需补充 |
| `src/main/java/io/imiocode/session/record/StoredToolResult.java` | 无需补充 |
| `src/main/java/io/imiocode/session/record/TransactionBeginRecord.java` | 无需补充 |
| `src/main/java/io/imiocode/session/record/TransactionCommitRecord.java` | 无需补充 |
| `src/main/java/io/imiocode/session/record/TransactionMode.java` | 无需补充 |
| `src/main/java/io/imiocode/session/SessionException.java` | 无需补充 |
| `src/main/java/io/imiocode/session/SessionId.java` | 无需补充 |
| `src/main/java/io/imiocode/session/SessionIntegrityValidator.java` | 新增注释 |
| `src/main/java/io/imiocode/session/SessionLoadResult.java` | 无需补充 |
| `src/main/java/io/imiocode/session/SessionManager.java` | 新增注释 |
| `src/main/java/io/imiocode/session/SessionMessageCodec.java` | 保留现有注释 |
| `src/main/java/io/imiocode/session/SessionMetadata.java` | 无需补充 |
| `src/main/java/io/imiocode/session/SessionMetadataReader.java` | 新增注释 |
| `src/main/java/io/imiocode/session/SessionRecoveryStatus.java` | 无需补充 |
| `src/main/java/io/imiocode/session/SessionSnapshot.java` | 无需补充 |
| `src/main/java/io/imiocode/session/SessionStore.java` | 新增注释 |
| `src/main/java/io/imiocode/session/SessionSummary.java` | 无需补充 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/skill/ClasspathSkillSource.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/DefaultSkillForkRunner.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/FileSkillSource.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/install/DefaultSkillInstaller.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/install/GitHubSkillFetcher.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/install/JdkSkillRemoteTransport.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/install/RemoteResponse.java` | 无需补充 |
| `src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java` | 新增注释 |
| `src/main/java/io/imiocode/skill/install/RemoteSkillFile.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/install/RemoteSkillKind.java` | 无需补充 |
| `src/main/java/io/imiocode/skill/install/RemoteSkillLocation.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/install/RemoteSkillLocator.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/install/RemoteSkillPackage.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/install/SkillDownloadBudget.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/install/SkillInstallConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/install/SkillInstaller.java` | 新增注释 |
| `src/main/java/io/imiocode/skill/install/SkillInstallException.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/install/SkillInstallListener.java` | 新增注释 |
| `src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java` | 新增注释 |
| `src/main/java/io/imiocode/skill/install/SkillInstallRequest.java` | 无需补充 |
| `src/main/java/io/imiocode/skill/install/SkillInstallResult.java` | 无需补充 |
| `src/main/java/io/imiocode/skill/install/SkillInstallStage.java` | 无需补充 |
| `src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java` | 新增注释 |
| `src/main/java/io/imiocode/skill/InstallSkillTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/LoadedSkill.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/LoadSkillTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillActivator.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillCatalogSnapshot.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillCommandRegistrar.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillCommandTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillDescriptor.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillException.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillExecutor.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillForkRunner.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillHistoryMode.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillInvocation.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillLoader.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillManagementCommand.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillMetadata.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillMode.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillOrigin.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillParser.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillReference.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillRunScope.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillSlashCommand.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillSource.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillSummaryFormatter.java` | 保留现有注释 |
| `src/main/java/io/imiocode/skill/SkillToolSpec.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/subagent/config/SubagentConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/context/SubagentContextBuilder.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java` | 新增注释 |
| `src/main/java/io/imiocode/subagent/definition/AgentCatalogSnapshot.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/definition/AgentDefinition.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/definition/AgentDefinitionException.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/definition/AgentDefinitionLoader.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/definition/AgentDefinitionParser.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/definition/AgentDefinitionSource.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/definition/AgentIsolation.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/filter/SubagentToolFilter.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/model/ModelAliasResolver.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/model/ModelResolution.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/runtime/AgentTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/runtime/DefaultSubagentRunner.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/runtime/RunToCompletion.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java` | 新增注释 |
| `src/main/java/io/imiocode/subagent/runtime/SubagentAgentHandle.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/runtime/SubagentDispatcher.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/runtime/SubagentDispatchResult.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/runtime/SubagentRunMode.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java` | 新增注释 |
| `src/main/java/io/imiocode/subagent/runtime/SubagentRunResult.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/runtime/SubagentToolRegistryFactory.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/task/TaskManager.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/task/TaskNotification.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/task/TaskSnapshot.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/task/TaskStatus.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/trace/TraceRegistry.java` | 保留现有注释 |
| `src/main/java/io/imiocode/subagent/trace/TraceSnapshot.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/trace/TraceStatus.java` | 无需补充 |
| `src/main/java/io/imiocode/subagent/trace/TraceTokenUsage.java` | 无需补充 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/team/backend/BackendAvailability.java` | 无需补充 |
| `src/main/java/io/imiocode/team/backend/BackendHandle.java` | 无需补充 |
| `src/main/java/io/imiocode/team/backend/BackendSelection.java` | 无需补充 |
| `src/main/java/io/imiocode/team/backend/BackendSelector.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/backend/InProcessBackend.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/backend/ITerm2Backend.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/backend/JdkProcessExecutor.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/backend/ProcessExecutor.java` | 新增注释 |
| `src/main/java/io/imiocode/team/backend/ProcessResult.java` | 无需补充 |
| `src/main/java/io/imiocode/team/backend/TeammateBackend.java` | 新增注释 |
| `src/main/java/io/imiocode/team/backend/TeammateLaunchRequest.java` | 无需补充 |
| `src/main/java/io/imiocode/team/backend/TmuxBackend.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/config/TeamRuntimeConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/coordinator/ConversationPolicy.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/coordinator/CoordinatorModeController.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/coordinator/CoordinatorSnapshot.java` | 无需补充 |
| `src/main/java/io/imiocode/team/coordinator/CoordinatorStage.java` | 无需补充 |
| `src/main/java/io/imiocode/team/mailbox/MailboxMessage.java` | 无需补充 |
| `src/main/java/io/imiocode/team/mailbox/MailboxMessageType.java` | 无需补充 |
| `src/main/java/io/imiocode/team/mailbox/MailboxStore.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/model/TeamBackend.java` | 无需补充 |
| `src/main/java/io/imiocode/team/model/TeamConfig.java` | 无需补充 |
| `src/main/java/io/imiocode/team/model/TeammateInfo.java` | 无需补充 |
| `src/main/java/io/imiocode/team/model/TeammateStatus.java` | 无需补充 |
| `src/main/java/io/imiocode/team/model/TeamPrincipal.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/model/TeamRole.java` | 无需补充 |
| `src/main/java/io/imiocode/team/persistence/AtomicJsonFile.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/persistence/CrossProcessFileLock.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/persistence/JsonlLog.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/persistence/TeamPaths.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/persistence/TeamStore.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/persistence/TranscriptEntry.java` | 无需补充 |
| `src/main/java/io/imiocode/team/persistence/TranscriptRole.java` | 无需补充 |
| `src/main/java/io/imiocode/team/persistence/TranscriptStore.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/runtime/AgentTeamManager.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/runtime/ConvergenceReport.java` | 无需补充 |
| `src/main/java/io/imiocode/team/runtime/SendReceipt.java` | 无需补充 |
| `src/main/java/io/imiocode/team/runtime/TeamCreateRequest.java` | 无需补充 |
| `src/main/java/io/imiocode/team/runtime/TeamDeletionReport.java` | 无需补充 |
| `src/main/java/io/imiocode/team/runtime/TeammateSpawnRequest.java` | 无需补充 |
| `src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java` | 新增注释 |
| `src/main/java/io/imiocode/team/runtime/TeamMemberWorker.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/runtime/TeamMessenger.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/task/TeamTask.java` | 无需补充 |
| `src/main/java/io/imiocode/team/task/TeamTaskService.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/task/TeamTaskStatus.java` | 无需补充 |
| `src/main/java/io/imiocode/team/task/TeamTaskStore.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/TeamException.java` | 无需补充 |
| `src/main/java/io/imiocode/team/tool/AbstractTaskTool.java` | 无需补充 |
| `src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java` | 新增注释 |
| `src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java` | 新增注释 |
| `src/main/java/io/imiocode/team/tool/SendMessageTool.java` | 新增注释 |
| `src/main/java/io/imiocode/team/tool/TaskCreateTool.java` | 无需补充 |
| `src/main/java/io/imiocode/team/tool/TaskGetTool.java` | 无需补充 |
| `src/main/java/io/imiocode/team/tool/TaskListTool.java` | 无需补充 |
| `src/main/java/io/imiocode/team/tool/TaskStopTool.java` | 无需补充 |
| `src/main/java/io/imiocode/team/tool/TaskUpdateTool.java` | 无需补充 |
| `src/main/java/io/imiocode/team/tool/TeamConvergeTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/team/tool/TeamCreateTool.java` | 新增注释 |
| `src/main/java/io/imiocode/team/tool/TeamDeleteTool.java` | 新增注释 |
| `src/main/java/io/imiocode/team/tool/TeamToolContext.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/terminal/JLineTerminalUi.java` | 保留现有注释 |
| `src/main/java/io/imiocode/terminal/SlashCommandCompleter.java` | 保留现有注释 |
| `src/main/java/io/imiocode/terminal/SlashCompletionSource.java` | 保留现有注释 |
| `src/main/java/io/imiocode/terminal/TerminalLayout.java` | 保留现有注释 |
| `src/main/java/io/imiocode/terminal/TerminalMode.java` | 保留现有注释 |
| `src/main/java/io/imiocode/terminal/TerminalUi.java` | 保留现有注释 |
| `src/main/java/io/imiocode/terminal/ToolSummaryFormatter.java` | 保留现有注释 |
| `src/main/java/io/imiocode/terminal/UiContext.java` | 保留现有注释 |
| `src/main/java/io/imiocode/terminal/UiDisplayPolicy.java` | 保留现有注释 |
| `src/main/java/io/imiocode/terminal/UiState.java` | 保留现有注释 |
| `src/main/java/io/imiocode/terminal/UsageFormatter.java` | 新增注释 |
| `src/main/java/io/imiocode/terminal/VersionResolver.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/tool/BaseTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/core/BashTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/core/EditFileTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/core/GlobPattern.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/core/GlobTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/core/GrepTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/core/ReadFileTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/core/WriteFileTool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/SecretRedactor.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/Tool.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/ToolAvailability.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/ToolCall.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/ToolDefinition.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java` | 新增注释 |
| `src/main/java/io/imiocode/tool/ToolExecution.java` | 无需补充 |
| `src/main/java/io/imiocode/tool/ToolExecutionEvent.java` | 无需补充 |
| `src/main/java/io/imiocode/tool/ToolExecutionListener.java` | 新增注释 |
| `src/main/java/io/imiocode/tool/ToolExecutionState.java` | 无需补充 |
| `src/main/java/io/imiocode/tool/ToolExecutor.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/ToolLifecycleListener.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/ToolLimits.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/ToolRegistry.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/ToolResolution.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/ToolResult.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/ToolRisk.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/ToolSelection.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/workspace/AtomicFileWriter.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/workspace/Utf8TextFile.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/workspace/WorkspacePolicy.java` | 保留现有注释 |
| `src/main/java/io/imiocode/tool/workspace/WorkspaceWalker.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it

| `src/main/java/io/imiocode/worktree/config/WorktreeConfig.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/git/GitCommandResult.java` | 无需补充 |
| `src/main/java/io/imiocode/worktree/git/GitCommandRunner.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/git/GitWorktreeClient.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/git/GitWorktreeEntry.java` | 无需补充 |
| `src/main/java/io/imiocode/worktree/lifecycle/WorktreeManager.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/lifecycle/WorktreePostCreationSetup.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/lifecycle/WorktreeSafetyInspector.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/model/ManagedWorktree.java` | 无需补充 |
| `src/main/java/io/imiocode/worktree/model/WorktreeChangeSummary.java` | 无需补充 |
| `src/main/java/io/imiocode/worktree/model/WorktreeCleanupReport.java` | 无需补充 |
| `src/main/java/io/imiocode/worktree/model/WorktreeCreation.java` | 无需补充 |
| `src/main/java/io/imiocode/worktree/model/WorktreeLease.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/model/WorktreeSession.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/persistence/WorktreeSessionStore.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/runtime/LaunchOptions.java` | 无需补充 |
| `src/main/java/io/imiocode/worktree/runtime/WorkspaceTransition.java` | 无需补充 |
| `src/main/java/io/imiocode/worktree/runtime/WorkspaceTransitionController.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/runtime/WorktreeBootstrap.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/runtime/WorktreeStartup.java` | 无需补充 |
| `src/main/java/io/imiocode/worktree/security/WorktreeNames.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/security/WorktreeSlugValidator.java` | 保留现有注释 |
| `src/main/java/io/imiocode/worktree/WorktreeException.java` | 保留现有注释 |
warning: in the working copy of 'src/main/java/io/imiocode/ImioCodeApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/DefaultRetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/LlmRetryPolicy.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/agent/RetryWaiter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/CommandParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ClearCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/CompactCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/DoCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ExitCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/HelpCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/MemoryCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PermissionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/PlanCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/ReviewCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/SessionCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/StatusCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/command/builtin/VerbosityCommand.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/AppConfig.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/ConfigDocument.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/config/YamlConfigLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/ContextEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/context/SummaryPrompt.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatMessage.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatRequest.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ChatResponse.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/conversation/ConversationLoop.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/CommandHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookActionExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/HookProcessRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/action/PromptHookExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/ConditionParser.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/hook/condition/DefaultConditionEvaluator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/FileInstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionLoader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/instruction/InstructionReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClient.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEvent.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/LlmException.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/StreamListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/TokenUsageBuilder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/provider/anthropic/AnthropicThinkingModeResolver.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpClientFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/llm/transport/SseEventReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryExtractor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryReminderFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/memory/MemoryStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistenceEventListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/persistence/PersistentContextProvider.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionIntegrityValidator.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionManager.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionMetadataReader.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/session/SessionStore.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallListener.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstallRefresher.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillInstaller.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentAgentFactory.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/subagent/runtime/SubagentRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/ProcessExecutor.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/backend/TeammateBackend.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/runtime/TeamMemberRunner.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorAdvanceTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/CoordinatorModeTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/SendMessageTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamCreateTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/team/tool/TeamDeleteTool.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/terminal/UsageFormatter.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolDefinitionEncoder.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/io/imiocode/tool/ToolExecutionListener.java', LF will be replaced by CRLF the next time Git touches it
<!-- AUDIT-END -->

## 验证方式

- 从上表提取 561 个反引号内路径，与 `src/main/java` 的实际 Java 文件集合双向求差；两个差集必须为空。
- 相对基线，生产 Java 文件只允许新增独立 Javadoc、独立行注释和必要空白；禁止行尾注释，以便机械验证所有原程序行仍按原顺序存在。
