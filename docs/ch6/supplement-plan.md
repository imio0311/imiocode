# ch6 权限系统补充 Plan

## 架构概览

本次补充只修改权限命令判断层，不改 Agent、HITL、路径沙箱和规则文件协议。

### 1. 命令扫描层

新增 `ShellCommandScanner`：

- 逐字符扫描命令；
- 跟踪单引号和双引号状态；
- 在引号外识别 `|`、`&&`、`||`、`;`；
- 检测重定向、后台执行、命令替换、反引号和畸形引号；
- 输出有序命令段或“不适合自动放行”的结果。

它只负责有限的安全扫描，不尝试构建完整 Shell AST。

### 2. 安全命令分类层

新增 `SafeCommandDetector` 和 `StrictSafeCommandDetector`：

- 每个命令段先拆出命令名和参数；
- 普通读取命令按完整命令名白名单匹配；
- Git 命令按允许子命令及参数组合匹配；
- 版本查询必须包含允许的版本参数；
- 全部命令段均安全时才返回安全；
- 任何未知或歧义直接返回“不确定”。

### 3. 危险检测扩展

在现有 `RegexDangerousCommandDetector` 中增加：

- Unix 根目录递归放权；
- Windows 系统盘递归完全授权；
- curl/wget 管道执行；
- PowerShell 下载后 `iex`；
- `eval` 和命令替换执行下载内容。

危险检测继续独立于安全检测，并保持最高优先级。

### 4. 权限检查器接入

`PermissionChecker` 新增安全命令检测器依赖，执行顺序变为：

```text
危险硬拦截
→ 路径沙箱
→ 模式上限
→ 三层显式规则
→ 安全命令自动允许
→ 模式默认值
→ HITL
```

新增 `SAFE_COMMAND` 决策来源。安全检测只处理 `COMMAND` 请求。

### 5. 兼容装配

- 应用入口注入严格安全命令检测器。
- 保留兼容构造器，避免既有测试和调用方失效。
- 工具调度器收到安全命令的 `ALLOW` 后走现有串行 Bash 执行流程。
- Plan Mode 在权限检查前已过滤 Bash，因此不需要额外分支。

## 核心接口

```java
public interface SafeCommandDetector {
    SafeCommandResult inspect(String command);
}

public record SafeCommandResult(
        boolean safe,
        String reason
) {}

public final class StrictSafeCommandDetector
        implements SafeCommandDetector {

    public StrictSafeCommandDetector(Path workspace);

    @Override
    public SafeCommandResult inspect(String command);
}
```

安全检测器在构造时接收工作区。涉及文件参数的只读命令仍需拒绝绝对路径、`..` 和明显的工作区逃逸目标，避免 `cat C:\...` 因属于只读命令而绕过既定沙箱边界。

### Shell 扫描接口

```java
public final class ShellCommandScanner {
    public ShellCommandScanResult scan(String command);
}

public record ShellCommandScanResult(
        boolean eligible,
        List<String> segments,
        String reason
) {}
```

扫描规则：

- 引号外的 `|`、`&&`、`||`、`;` 形成命令边界。
- `>`、`>>`、`<`、单独 `&`、`$(`、反引号直接标记为不可自动允许。
- 单引号和双引号必须闭合。
- 空命令段、连续异常分隔符和无法解释的转义返回不可自动允许。
- 分段后使用同一套引号感知 tokenizer 提取命令名及参数。

## 安全命令分类

分类器采用三类规则：

1. **命令名即可证明只读**
   - `ls/dir/pwd/cat/type/head/tail/wc/stat/file`
   - `grep/Get-Content/Get-ChildItem/Select-String/Test-Path/Resolve-Path`
   - `which/where/Get-Command/whoami/uname`

2. **必须验证子命令**
   - Git 只接受明确列出的查询子命令。
   - `git branch` 只接受 `--show-current`。
   - `git remote` 只接受 `-v`。
   - 拒绝 `--output`、`--ext-diff`、`--textconv` 等可能写文件或执行外部程序的选项。
   - `rg` 拒绝 `--pre` 等可执行外部命令的选项。

3. **必须精确匹配查询参数**
   - Java、Maven、Node、npm、Python、Go、Rust、Cargo、Gradle 只接受版本查询形式。
   - `hostname` 和 `date` 只接受不修改系统状态的查询参数。
   - 不接受额外脚本、目标或子命令。

## 路径参数约束

对于安全白名单中的文件读取命令：

- 拒绝 Windows/Unix 绝对路径、UNC 路径和 `..`。
- 允许当前工作区相对路径、只读选项和搜索 pattern。
- 无法区分某个参数究竟是 pattern、选项值还是路径时，返回“不确定”，交给 HITL。
- 安全检测不改变原命令，只判断是否可以自动允许。

## `PermissionChecker` 接口调整

```java
public PermissionChecker(
        Path workspace,
        DangerousCommandDetector dangerous,
        PathSandbox sandbox,
        PermissionRuleEngine rules,
        PermissionModePolicy modes,
        PermissionSettings settings,
        SafeCommandDetector safeCommands
);
```

保留旧构造器，由旧构造器注入“永不自动允许”的安全检测器，避免既有调用方行为突然变化。

`PermissionDecisionSource` 增加：

```java
SAFE_COMMAND
```

## 文件调整

```text
src/main/java/io/imiocode/permission/command/
├── SafeCommandDetector.java
├── SafeCommandResult.java
├── ShellCommandScanResult.java
├── ShellCommandScanner.java
└── StrictSafeCommandDetector.java

修改：
├── RegexDangerousCommandDetector.java
├── PermissionDecisionSource.java
├── PermissionChecker.java
└── ImioCodeApplication.java
```

测试新增：

```text
src/test/java/io/imiocode/permission/command/
├── ShellCommandScannerTest.java
└── StrictSafeCommandDetectorTest.java
```

并扩展：

- `RegexDangerousCommandDetectorTest`
- `PermissionCheckerTest`
- `StreamingToolSchedulerPermissionTest`
- `PermissionApplicationE2ETest`

## 技术决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 安全判断 | 完整命令段全部匹配 | 防止只看开头造成混合危险命令逃逸 |
| Shell 解析 | 有限状态扫描器 | 能处理引号和分隔符，同时避免引入完整 Shell 解析范围 |
| 不确定结果 | 继续模式/HITL | 安全白名单失败不等于拒绝，也绝不自动允许 |
| 显式规则优先 | 规则在安全白名单之前 | 用户仍可强制询问或拒绝安全命令 |
| 路径安全 | 安全命令同时检查路径形态 | 防止 Bash 只读命令绕过工作区范围 |
| Git 判断 | 子命令与危险选项双重限制 | Git 查询也可能通过参数写文件或启动外部程序 |
| 兼容构造器 | 默认不启用自动允许 | 避免库调用方升级后权限意外扩大 |
| Plan Mode | 保持现有工具选择过滤 | 不重复实现，也不开放 Bash |
