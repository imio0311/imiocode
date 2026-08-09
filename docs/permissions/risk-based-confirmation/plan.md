# 风险分级确认策略 Plan

## 架构概览

权限链继续保留“危险命令硬拦截 → 路径沙箱 → 模式上限 → 三层规则 → 模式默认动作 → HITL”的主顺序。在权限请求创建阶段新增命令风险分类器：它只解析文本，不执行命令，把具体 Bash 调用从固定 HIGH 转换为 LOW、MEDIUM 或 HIGH，并附带可安全展示的判定原因。

自动编辑模式改为依据请求的最终风险等级决策：LOW/MEDIUM 自动允许，HIGH 请求确认。未知 MCP 和第三方工具继续使用其静态 HIGH；精确允许规则位于模式默认动作之前，因此可以表达用户主动建立的信任。危险命令和沙箱拒绝仍在规则之前，不能被放行。

## 核心数据结构

### CommandRiskAssessment

```java
public record CommandRiskAssessment(
        ToolRisk risk,
        String reason
) { }
```

表示一次命令文本的确定性风险结论。`reason` 必须是短小、脱敏、可直接展示的中文原因。

### CommandRiskClassifier

```java
public interface CommandRiskClassifier {
    CommandRiskAssessment classify(String command);
}
```

分类器不得执行命令或访问网络。解析失败、命令未知或出现无法安全理解的 Shell 语法时返回 HIGH，而不是抛出后放行。

### RegexCommandRiskClassifier

```java
public final class RegexCommandRiskClassifier implements CommandRiskClassifier {
    public RegexCommandRiskClassifier(
            SafeCommandDetector safeDetector,
            ShellCommandScanner scanner,
            ShellCommandTokenizer tokenizer);

    @Override
    public CommandRiskAssessment classify(String command);
}
```

分类顺序：

1. 严格只读检测通过 → LOW。
2. Shell 扫描或分词失败、重定向/替换/后台语法 → HIGH。
3. 每个命令段匹配高风险规则或本地开发命令规则。
4. 任一段 HIGH → 整体 HIGH；否则任一段 MEDIUM → 整体 MEDIUM；全部只读 → LOW。
5. 无法识别的程序或参数 → HIGH。

### ShellCommandTokenizer

```java
public final class ShellCommandTokenizer {
    public ShellTokenizeResult tokenize(String segment);
}

public record ShellTokenizeResult(
        boolean valid,
        List<String> tokens,
        String reason
) { }
```

从现有严格安全检测器中抽取引号感知分词逻辑，供只读检测和风险分类共同使用，避免两套解析规则产生分歧。

### PermissionRequest

```java
public record PermissionRequest(
        ToolCall call,
        ToolRisk risk,
        PermissionOperation operation,
        String normalizedTarget,
        String displayTarget,
        String riskReason
) { }
```

保留五参数兼容构造器，旧调用方自动使用静态风险原因。权限提示使用动态 `risk`，模式决策使用 `riskReason` 解释询问原因。

## 风险规则设计

### LOW：严格只读

- 工作区内文件查看和搜索。
- Git status/diff/log/show 等只读查询。
- 工具版本、当前目录和系统信息查询。
- 继续复用现有严格只读检测器，参数越界或动态展开不会进入 LOW。

### MEDIUM：普通本地开发

- Maven、Gradle、npm/pnpm/yarn、Go、Cargo、dotnet 等本地 build/test/lint/format 命令。
- 编译器、测试运行器、格式化器和静态检查器。
- Git add/commit/switch/merge/pull/fetch 等非远程写入、非历史破坏操作。
- 工作区内 mkdir、touch/New-Item、copy/move 等普通文件组织操作。
- 明确允许的本地开发程序只有在参数未命中高风险规则时才进入 MEDIUM。

### HIGH：需要确认

- 删除、递归删除、强制覆盖和输出重定向。
- Git push、force push、reset/rebase/clean、强制 checkout、删除分支或标签。
- npm/cargo/Maven/Docker/GitHub Release 等发布操作。
- 全局包安装和系统包管理器安装/卸载。
- chmod/chown/icacls/Set-Acl、注册表、服务、计划任务、执行策略等权限或系统修改。
- curl/wget/Invoke-WebRequest 后执行、Shell 命令替换、`eval`/`Invoke-Expression` 等远程脚本或动态执行。
- 未知命令、无法解析的复合语法和没有可信风险元信息的远程工具。

危险黑名单仍由现有检测器先行处理；其命令不是 HIGH/ASK，而是直接 DENY。

## 模块设计

### 命令解析模块

**职责：** 共享扫描、分词和动态风险分类，输出不含敏感正文的判定原因。
**对外接口：** `CommandRiskClassifier.classify`。
**依赖：** 现有严格只读检测器、Shell 扫描器、工具风险枚举。

### 权限请求工厂

**职责：** 规范化权限目标；仅对 Bash 调用应用动态分类；保留工具自身提供的动态风险；MCP/未知第三方工具沿用静态 HIGH。
**对外接口：** 现有两个 `create` 重载保持不变。
**依赖：** 命令风险分类器、脱敏器、工具定义。

### 权限模式策略

**职责：** 根据模式、操作和最终风险输出默认决策。
**自动编辑语义：** LOW/MEDIUM 允许，HIGH 询问。
**其他模式：** ASK 仍对非读取操作询问；READ_ONLY、LOCKDOWN 仍作为上限；FULL_ACCESS 仍允许未命中硬拦截的操作。

### 权限检查器

**职责：** 保持硬拦截和沙箱优先；应用规则；最后调用模式策略。普通 HIGH 可由精确允许规则建立信任，但保留强制 Skill 覆盖的专用确认分支，避免永久规则静默覆盖已安装代码。

### 配置模块

**职责：** 没有显式 `permissions.mode` 时使用 AUTO_EDIT；将项目配置和示例同步为 `auto-edit`。显式配置 `ask/read-only/full-access/lockdown` 时不改写。

### UI/HITL

**职责：** 继续消费现有权限提示事件。提示中的 risk 来自动态请求，reason 来自风险分类或规则/模式决策，目标继续经过脱敏和长度限制。无需改动按键协议。

## 模块交互

```text
ToolCall + ToolDefinition
        │
        ▼
PermissionRequestFactory
        ├─ 规范化/脱敏目标
        ├─ Bash → CommandRiskClassifier
        └─ MCP/第三方 → 保留静态风险
        │
        ▼
PermissionRequest(risk, riskReason)
        │
        ▼
PermissionChecker
        ├─ DangerousCommandDetector → DENY
        ├─ PathSandbox → DENY
        ├─ Mode ceiling → DENY
        ├─ RuleEngine → ALLOW / ASK / DENY
        └─ ModePolicy → LOW/MEDIUM ALLOW，HIGH ASK
                                      │
                                      ▼
                         PermissionCoordinator + UI
```

## 文件组织

```text
src/main/java/io/imiocode/
├── permission/
│   ├── PermissionRequest.java                 — 增加动态风险原因
│   ├── PermissionRequestFactory.java          — 接入命令分类器
│   ├── PermissionChecker.java                 — 统一高风险决策路径
│   ├── PermissionModePolicy.java              — AUTO_EDIT 风险驱动
│   ├── PermissionSettings.java                — 默认 AUTO_EDIT
│   ├── command/
│   │   ├── CommandRiskAssessment.java         — 分类结果
│   │   ├── CommandRiskClassifier.java         — 分类接口
│   │   ├── RegexCommandRiskClassifier.java    — 风险规则
│   │   ├── ShellCommandTokenizer.java         — 共享分词器
│   │   ├── ShellTokenizeResult.java           — 分词结果
│   │   └── StrictSafeCommandDetector.java     — 改用共享分词器
│   └── rule/PermissionRuleLoader.java          — 缺省模式调整
├── ImioCodeApplication.java                    — 组装并注入分类器
config.yaml                                     — 当前项目模式
config.example.yaml                             — 示例默认模式
src/test/java/io/imiocode/
├── permission/command/RegexCommandRiskClassifierTest.java
├── permission/command/ShellCommandTokenizerTest.java
├── permission/PermissionRequestFactoryTest.java
├── permission/PermissionModePolicyTest.java
├── permission/PermissionCheckerTest.java
├── permission/PermissionCoordinatorTest.java
├── permission/rule/PermissionRuleLoaderTest.java
└── PermissionRiskApplicationIT.java
```

## 技术决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 风险判断位置 | 权限请求创建阶段 | HITL 提示和模式策略拿到同一个最终风险，避免 UI 与决策不一致 |
| Bash 工具静态风险 | 保持 HIGH | 调度器仍把命令视为不安全并串行；权限请求可单独动态降级，不影响并发安全 |
| 未知命令 | HIGH | 无法证明安全时必须保守确认 |
| MCP/第三方工具 | 静态 HIGH | MCP 标准没有可靠副作用元数据，名称启发式容易误放行 |
| 规则优先级 | 硬拦截/沙箱之后、模式默认动作之前 | 保证不可绕过的安全边界，同时允许用户明确建立信任；强制 Skill 覆盖仍保留专用确认 |
| 复合命令 | 取最高风险 | 一个高风险片段足以改变整个调用的安全属性 |
| 解析实现 | 抽取并复用现有扫描/分词器 | 避免只读检测与风险分类对引号和边界产生不同解释 |
| 新权限模式 | 不新增 | `auto-edit` 的名称和既有用途最适合承载风险驱动行为，减少配置迁移 |
