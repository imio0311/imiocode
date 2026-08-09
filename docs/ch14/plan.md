# CH14 Git Worktree 隔离系统 Plan

## 架构概览

CH14 新增 `io.imiocode.worktree` 模块，并把职责分成五层：安全命名、Git 进程适配、生命周期管理、会话/启动切换、子 Agent 隔离。`WorktreeManager` 是唯一允许创建和删除受管 Worktree 的入口；所有命令和子 Agent 都通过它操作。

手动 Enter/Exit 不通过修改 JVM `user.dir` 实现。应用新增受控重启循环：当前 `WorkspaceRuntime` 关闭后，使用目标路径重新装配配置、工具、权限、MCP、Hook、Session 和 Agent。这样不存在一半组件仍指向旧目录的混合状态。

子 Agent 不切换全局运行时。`DefaultSubagentRunner` 为 `isolation: worktree` 获取 `WorktreeLease`，由 `SubagentAgentFactory` 使用 lease 路径创建工作区专属工具注册表、权限沙箱、环境收集器和上下文存储，完成后交给 `WorktreeManager` 安全清理。

## 推荐决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 恢复 | 仅显式 `--resume` | 防止普通启动意外切换目录 |
| 手动分支 | `worktree-<slug>` | 可预测、便于用户后续 merge/删除 |
| 子 Agent 分支 | `worktree-agent-<type>-<随机短 ID>` | 并发无冲突且仍可识别来源 |
| 运行时切换 | 关闭并重建 WorkspaceRuntime | Java 无可靠进程级 chdir，避免固定路径对象失效 |
| 自动清理 | 只自动删除可证明干净且无独有提交的对象 | 满足自动清理同时防止数据丢失 |
| Git 调用 | `ProcessBuilder(List<String>)` | 不经过 Shell，避免注入和平台转义差异 |
| hooks | `git config --worktree core.hooksPath` | 不污染其他 Worktree 的共享配置 |
| 本地状态目录 | `.imiocode/` | 与现有项目约定一致，不沿用参考图中的 `.mewcode` |

## 核心数据结构

### WorktreeConfig

```java
public record WorktreeConfig(
        Path directory,
        Duration gitTimeout,
        Duration staleAfter,
        Duration cleanupInterval,
        List<String> linkDirectories,
        List<String> copyIncludes,
        boolean copyLocalConfig) {}
```

由 `config.yaml` 的 `worktrees:` 映射，路径必须是项目相对路径并位于 `.imiocode` 受管空间。

### WorktreeSession

```java
public record WorktreeSession(
        String sessionId,
        String slug,
        Path originalCwd,
        Path worktreePath,
        String worktreeBranch,
        String originalBranch,
        String originalHead,
        Instant createdAt) {}
```

会话只描述手动 Enter；子 Agent lease 不写全局会话。

### ManagedWorktree

```java
public record ManagedWorktree(
        String slug,
        Path path,
        String branch,
        String head,
        boolean active,
        WorktreeChangeSummary changes) {}
```

### WorktreeLease

```java
public final class WorktreeLease implements AutoCloseable {
    WorktreeSession session();
    Path workdir();
    CleanupReport closeSafely();
}
```

lease 持有唯一锁；`closeSafely` 只清理可证明安全的临时 Worktree。

### WorkspaceTransition

```java
public sealed interface WorkspaceTransition {
    record Stay() implements WorkspaceTransition {}
    record Enter(Path path) implements WorkspaceTransition {}
    record Exit(Path originalPath) implements WorkspaceTransition {}
    record Stop() implements WorkspaceTransition {}
}
```

`ConversationLoop` 返回转换意图，`ImioCodeApplication` 在完整关闭旧 runtime 后重建。

## 模块设计

### slug

`WorktreeSlugValidator` 负责校验、手动 slug 规范化和子 Agent 唯一 slug 生成。它不接受路径对象，不执行容错替换；非法 LLM 输出直接失败。`WorktreeNames` 只对已校验 slug 生成路径和分支。

### git

`GitCommandRunner` 封装有界、可取消、无 Shell 的进程执行。`GitWorktreeClient` 提供仓库根、HEAD、分支存在性、worktree add/list/remove/prune、status porcelain、独有提交和远端包含检查。所有破坏操作前重新读取 Git 状态。

### lifecycle

`WorktreeManager` 串行化 Create/Enter/Exit/Remove。`WorktreeSafetyInspector` 汇总脏文件、未跟踪文件、独有提交、远端包含和真实路径归属。`WorktreePostCreationSetup` 完成本地配置复制、hooks、依赖链接和 include 复制。

`WorktreeAutoCleaner` 使用单线程调度器。它只处理 `worktree-agent-*`，跳过活动 session、有效 lock、未过期对象和任何不能证明安全的对象。

### persistence 与启动

`WorktreeSessionStore` 使用 Jackson 和临时文件 + 原子替换写入 `.imiocode/worktree-session.json`。`LaunchOptions` 只接受 `--resume`；未知参数安全失败。`WorktreeBootstrap` 在装配应用前解析原始 Git 根和恢复记录。

### command 与运行时

`WorktreeCommand` 实现五个子命令。Create/List/Remove 原地完成；Enter/Exit 写入生命周期状态后请求 `WorkspaceTransition`。`WorkspaceRuntime` 封装当前 `ImioCodeApplication.run` 中的一次性资源，保证切换前按 TaskManager、Coordinator、MCP、LLM、Hook、终端的安全顺序关闭。

### SubAgent 集成

`SubagentAgentFactory#create` 增加 `Path workdir`。`SubagentToolRegistryFactory` 从父注册表复制与目录无关的工具，并替换 `read_file/write_file/edit_file/bash/glob/grep` 为绑定 workdir 的实例。子权限组件和环境组件同样以 workdir 构建。

`DefaultSubagentRunner` 新增 `executeWithWorktree` 分支：

1. 从 Agent 类型生成唯一安全 slug。
2. 创建 `WorktreeLease`。
3. 构建 Worktree 专属 Agent。
4. 注入 `WorktreeIsolationNotice`，明确父目录、子目录、分支和禁止访问父目录。
5. 执行到终态。
6. 安全自动清理；若保留，将路径、分支和原因附加到最终结果。

## 模块交互

```text
启动参数
  -> WorktreeBootstrap
  -> WorkspaceRuntime(original 或 resumed path)
  -> ConversationLoop
       -> /worktree -> WorktreeManager -> GitWorktreeClient
                         -> WorkspaceTransition -> 关闭并重建 Runtime
       -> agent tool -> SubagentDispatcher -> TaskManager
                         -> DefaultSubagentRunner
                         -> WorktreeManager.createAgentWorktree
                         -> scoped Agent/Tools/Permission
                         -> safe auto cleanup
```

## 文件组织

```text
src/main/java/io/imiocode/worktree/
├── config/WorktreeConfig.java
├── git/GitCommandRunner.java
├── git/GitCommandResult.java
├── git/GitWorktreeClient.java
├── lifecycle/WorktreeManager.java
├── lifecycle/WorktreePostCreationSetup.java
├── lifecycle/WorktreeSafetyInspector.java
├── lifecycle/WorktreeAutoCleaner.java
├── model/ManagedWorktree.java
├── model/WorktreeChangeSummary.java
├── model/WorktreeCleanupReport.java
├── model/WorktreeLease.java
├── model/WorktreeSession.java
├── persistence/WorktreeSessionStore.java
├── security/WorktreeSlugValidator.java
├── security/WorktreeNames.java
└── runtime/LaunchOptions.java
    runtime/WorktreeBootstrap.java
    runtime/WorkspaceTransition.java
    runtime/WorkspaceTransitionController.java

src/main/java/io/imiocode/command/builtin/WorktreeCommand.java
src/main/java/io/imiocode/subagent/runtime/SubagentToolRegistryFactory.java
src/main/java/io/imiocode/subagent/context/WorktreeIsolationNotice.java
```

现有文件主要修改：`ImioCodeApplication`、配置 records/loader、`CommandServices`、`ConversationLoop`、`SubagentAgentFactory`、`DefaultSubagentRunner`、`RunToCompletion`、`SubagentDispatcher`、`ToolRegistry`、README 和内置 Agent 定义。

## Spec 覆盖

- F1—F2：security。
- F3—F8、F16—F17：git + lifecycle。
- F9—F10：persistence + bootstrap。
- F11—F12：command + runtime transition。
- F13—F15：subagent isolation。
- F18：回归测试和兼容装配。
