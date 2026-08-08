# CH11 Skill 系统补齐 Plan

## 架构概览

### RemoteSkillLocator

解析和规范化 skills.sh、GitHub tree、GitHub Raw 三类 URL，输出统一远程位置。在网络请求前拒绝 HTTP、非法域名和路径逃逸。

### SkillRemoteTransport

抽象受限 GET、超时、响应大小、重定向和取消。生产环境使用 Java HttpClient，测试使用内存实现。

### RemoteSkillFetcher

Raw URL 下载单个 SKILL.md；GitHub tree 和 skills.sh 通过 GitHub Contents API 枚举并下载目标目录，执行文件数量、递归深度和字节预算。

### SkillInstaller

统一编排定位、下载、暂存、完整解析、权限检查、原子安装、热刷新和失败回滚。安装阶段不执行脚本或专属工具。

### InstallSkillTool

Agent 可见的系统工具，复用 SkillInstaller，支持取消并返回结构化结果。系统可见性不意味着绕过权限。

### Slash Command 与 UI

`/skill install <URL> [--force]` 调用同一安装器。安装后刷新动态命令和补全。`/clear` 先统一取消活动工作，再清空终端。

### 内置 Skill

新增 backend-interview fork Skill；保留 commit、review、test。

## 核心数据结构

### SkillInstallConfig

```java
record SkillInstallConfig(
    Duration timeout,
    int maxFiles,
    long maxFileBytes,
    long maxTotalBytes,
    Set<String> allowedHosts
)
```

### RemoteSkillLocation

```java
enum RemoteSkillKind { SKILLS_SH, GITHUB_TREE, GITHUB_RAW }

record RemoteSkillLocation(
    RemoteSkillKind kind,
    URI sourceUri,
    String owner,
    String repository,
    String revision,
    String path
)
```

### SkillRemoteTransport

```java
interface SkillRemoteTransport {
    RemoteResponse get(URI uri, SkillDownloadBudget budget)
            throws SkillInstallException;
    void cancel();
}
```

### 远程包模型

```java
record RemoteSkillFile(Path relativePath, byte[] content) {}
record RemoteSkillPackage(URI source, List<RemoteSkillFile> files) {}
```

### RemoteSkillFetcher

```java
interface RemoteSkillFetcher {
    RemoteSkillPackage fetch(RemoteSkillLocation location);
}
```

### 安装模型

```java
record SkillInstallRequest(URI source, boolean force) {}

record SkillInstallResult(
    String skillName,
    Path installedPath,
    SkillOrigin origin,
    boolean replaced,
    List<SkillInstallStage> stages
) {}

enum SkillInstallStage {
    QUEUED, DOWNLOADING, VALIDATING, INSTALLING, RELOADING, COMPLETED
}
```

### 安装监听器和服务

```java
interface SkillInstallListener {
    void onStage(SkillInstallStage stage, String safeMessage);
}

interface SkillInstaller {
    SkillInstallResult install(
        SkillInstallRequest request,
        SkillInstallListener listener
    );
    void cancel();
}
```

### InstallSkillTool Schema

输入包含必填字符串 `url` 和可选布尔值 `force`，拒绝额外字段。

## 模块设计

### URL 解析

- skills.sh 解析 owner、repo、skill 并映射到默认分支的 `skills/<skill>`。
- GitHub tree 解析 owner、repo、revision、目标目录。
- Raw URL 必须以 SKILL.md 结尾。
- 分别校验每个路径段，拒绝编码后的分隔符、点段和反斜杠。

### HTTP 传输

- 禁止自动重定向，最多手动跟随三次。
- 每跳重新验证协议、域名和目标路径。
- 检查声明长度和实际读取字节数。
- 取消时中止当前 Future 和后续请求。

### GitHub 下载

- 使用 Contents API 枚举 tree/skills.sh 目录。
- 只接受 file 和 dir；拒绝 symlink、submodule 和其他类型。
- 最多递归 8 层、64 个文件。
- 所有路径必须保留在目标 Skill 根目录。

### 安装事务

按“解析 URL → 下载 → 暂存 → 完整解析 → 冲突检查 → 旧版本备份 → 原子移动 → reload → 动态命令刷新 → 删除备份”执行。任何失败都清理临时目录，必要时恢复旧目录和旧快照。

### 权限

- install_skill 对 Agent 始终可见，但执行仍经过权限系统。
- 普通安装按 write 判定。
- `force=true` 按高风险写操作判定，除 bypassPermissions 外必须确认。
- 安装后专属工具继续经过 CH6 权限链。

### 取消

- 同时只允许一个安装任务。
- Agent 取消、`/clear` 和应用关闭均调用安装器 cancel。
- 取消后清理请求、临时目录、备份和临时白名单。

## 模块交互

```text
自然语言 → Agent → install_skill → PermissionGate → SkillInstaller
→ URL 校验 → 安全下载 → SkillParser → 原子安装
→ SkillLoader.reload → CommandRegistry.replaceDynamic → 工具结果
```

```text
/skill install → CommandRegistry → CommandServices → SkillInstaller
→ 热刷新 → CommandResult（不调用 LLM）
```

## 文件组织

```text
src/main/java/io/imiocode/
├── config/                         配置映射与 SkillInstallConfig
├── skill/install/                 URL、传输、下载、安装事务模型
├── skill/InstallSkillTool.java    Agent 系统工具
├── skill/SkillManagementCommand.java
├── permission/                    安装目标权限适配
├── command/                       安装服务与 /clear 取消
├── runtime/ConversationCoordinator.java
└── ImioCodeApplication.java

src/main/resources/skills/
├── index.txt
└── backend-interview/SKILL.md

src/test/java/io/imiocode/
├── skill/install/
├── skill/InstallSkillToolTest.java
├── command/ClearCommandTest.java
└── Ch11SkillInstallApplicationIT.java
```

## 技术决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 网络实现 | Java HttpClient | 跨平台、无外部依赖、支持取消 |
| URL 范围 | skills.sh、GitHub tree、GitHub Raw | 覆盖目标同时控制 SSRF |
| 目录发现 | GitHub Contents API | 可辨别文件、目录、链接和子模块 |
| 重定向 | 手动逐跳 | 每跳重新执行安全校验 |
| 下载存储 | 小包内存预算后写临时目录 | 总量受限且便于统一校验 |
| 安装提交 | 同文件系统临时目录和原子移动 | 避免半安装 |
| 覆盖 | 备份后替换 | 支持失败恢复 |
| 配置 | config.yaml 的 skills.install | 延续统一配置约定 |
| 权限 | 新装 write，覆盖 high-risk write | 区分破坏性风险 |
| 热刷新 | 复用 SkillLoader 与动态命令同步 | 保持单一目录真相来源 |
| 面试 Skill | fork + recent | 隔离轨迹并保留必要背景 |
| /clear | 先取消再清屏 | 保留 CH10 语义并补齐清理 |
| 兼容 | 保留 review，新增 backend-interview | 避免已有命令回归 |
| 测试网络 | 注入内存 Transport | 稳定且不依赖公网限流 |
