# ch6 权限系统补充 Tasks

## 文件清单

| 操作 | 文件 | 职责 |
|---|---|---|
| 新建 | `src/main/java/io/imiocode/permission/command/ShellCommandScanResult.java` | 命令扫描结果 |
| 新建 | `src/main/java/io/imiocode/permission/command/ShellCommandScanner.java` | 引号感知的有限 Shell 扫描 |
| 新建 | `src/main/java/io/imiocode/permission/command/SafeCommandDetector.java` | 安全命令检测接口 |
| 新建 | `src/main/java/io/imiocode/permission/command/SafeCommandResult.java` | 安全命令判断结果 |
| 新建 | `src/main/java/io/imiocode/permission/command/StrictSafeCommandDetector.java` | 严格只读命令白名单 |
| 修改 | `src/main/java/io/imiocode/permission/command/RegexDangerousCommandDetector.java` | 扩展危险命令硬拦截 |
| 修改 | `src/main/java/io/imiocode/permission/PermissionDecisionSource.java` | 增加 `SAFE_COMMAND` 来源 |
| 修改 | `src/main/java/io/imiocode/permission/PermissionChecker.java` | 接入安全命令检测层 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 注入严格检测器 |
| 新增/修改 | `src/test/java/io/imiocode/**` 对应测试文件 | 单元、集成及端到端验证 |

## ST1：实现 Shell 命令扫描器

**文件：**

- `src/main/java/io/imiocode/permission/command/ShellCommandScanResult.java`
- `src/main/java/io/imiocode/permission/command/ShellCommandScanner.java`
- `src/test/java/io/imiocode/permission/command/ShellCommandScannerTest.java`

**依赖：** 无

**步骤：**

1. 定义扫描结果，包含是否可自动判断、命令段列表和原因。
2. 实现单引号、双引号状态跟踪。
3. 在引号外识别 `|`、`&&`、`||`、`;`。
4. 拒绝重定向、后台执行、命令替换、反引号和畸形引号。
5. 测试引号内分隔符不会被错误拆分。

**验证：**

```powershell
mvn -o "-Dmaven.repo.local=C:\Users\10355\.m2\repository" -Dtest=ShellCommandScannerTest test
```

预期：扫描器专项测试全部通过。

## ST2：定义安全命令检测契约

**文件：**

- `src/main/java/io/imiocode/permission/command/SafeCommandDetector.java`
- `src/main/java/io/imiocode/permission/command/SafeCommandResult.java`

**依赖：** ST1

**步骤：**

1. 定义统一检测接口。
2. 定义安全、无法确认两种结果。
3. 统一原因文本，便于权限决策和测试使用。

**验证：**

```powershell
mvn -o "-Dmaven.repo.local=C:\Users\10355\.m2\repository" -DskipTests package
```

预期：主代码编译成功。

## ST3：实现普通只读命令与路径限制

**文件：**

- `src/main/java/io/imiocode/permission/command/StrictSafeCommandDetector.java`
- `src/test/java/io/imiocode/permission/command/StrictSafeCommandDetectorTest.java`

**依赖：** ST1、ST2

**步骤：**

1. 接入 `ShellCommandScanner`。
2. 支持目录、文件读取、搜索和 PowerShell 只读命令。
3. 命令名使用完整词边界、不区分大小写匹配。
4. 涉及文件参数时拒绝绝对路径、UNC 路径和 `..`。
5. 拒绝 `rg --pre`、未知选项及无法可靠解释的参数。
6. 只有所有命令段均安全时才返回安全。

**验证：**

```powershell
mvn -o "-Dmaven.repo.local=C:\Users\10355\.m2\repository" -Dtest=StrictSafeCommandDetectorTest test
```

预期：普通读取通过，路径逃逸、混合命令和未知语法不能自动放行。

## ST4：实现 Git、版本及系统查询规则

**文件：**

- `src/main/java/io/imiocode/permission/command/StrictSafeCommandDetector.java`
- `src/test/java/io/imiocode/permission/command/StrictSafeCommandDetectorTest.java`

**依赖：** ST3

**步骤：**

1. 严格限制 Git 只读子命令。
2. `git remote` 只允许 `-v`，`git branch` 只允许 `--show-current`。
3. 拒绝 Git 写操作及 `--output`、`--ext-diff`、`--textconv`。
4. 严格匹配 Java、Maven、Node、Python、Go、Rust、Gradle 版本查询。
5. 支持无副作用的系统信息查询。
6. 为每个命令族添加允许和相近拒绝用例。

**验证：**

```powershell
mvn -o "-Dmaven.repo.local=C:\Users\10355\.m2\repository" -Dtest=StrictSafeCommandDetectorTest test
```

预期：白名单命令被识别为安全，近似危险或带副作用的命令不能自动放行。

## ST5：扩展危险命令硬拦截

**文件：**

- `src/main/java/io/imiocode/permission/command/RegexDangerousCommandDetector.java`
- `src/test/java/io/imiocode/permission/command/RegexDangerousCommandDetectorTest.java`

**依赖：** 无

**步骤：**

1. 拦截 Unix 根目录递归完全放权。
2. 拦截 Windows 系统盘递归完全授权。
3. 拦截 `curl/wget` 管道执行 Shell。
4. 拦截 PowerShell 下载后交给 `iex`。
5. 拦截 `eval`、命令替换等直接执行下载内容的形式。
6. 增加大小写、空白变化测试。
7. 增加单独下载和帮助命令等反例，防止误拦截。

**验证：**

```powershell
mvn -o "-Dmaven.repo.local=C:\Users\10355\.m2\repository" -Dtest=RegexDangerousCommandDetectorTest test
```

预期：危险变体全部命中，正常反例不命中。

## ST6：接入权限检查器

**文件：**

- `src/main/java/io/imiocode/permission/PermissionDecisionSource.java`
- `src/main/java/io/imiocode/permission/PermissionChecker.java`
- `src/test/java/io/imiocode/permission/PermissionCheckerTest.java`

**依赖：** ST4、ST5

**步骤：**

1. 增加 `SAFE_COMMAND` 决策来源。
2. 注入 `SafeCommandDetector`。
3. 保留旧构造器，并默认使用“永不自动放行”实现。
4. 按以下顺序检查：

```text
危险硬拦截 → 路径沙箱 → 模式上限 → 显式规则
→ 安全命令 → 模式默认行为 → HITL
```

5. 验证明示 `deny/ask` 规则优先于安全命令。
6. 验证 `READ_ONLY` 等模式上限不能被白名单绕过。

**验证：**

```powershell
mvn -o "-Dmaven.repo.local=C:\Users\10355\.m2\repository" -Dtest=PermissionCheckerTest test
```

预期：权限决策结果及来源符合既定优先级。

## ST7：完成应用和调度器集成

**文件：**

- `src/main/java/io/imiocode/ImioCodeApplication.java`
- `src/test/java/io/imiocode/agent/StreamingToolSchedulerPermissionTest.java`

**依赖：** ST6

**步骤：**

1. 应用启动时用工作区创建严格安全命令检测器。
2. 注入权限检查器。
3. 验证安全 Bash 命令无需 HITL 即可执行。
4. 验证普通非白名单命令仍进入 HITL。
5. 验证危险命令直接拒绝，不发送确认事件。
6. 确认 Plan Mode 仍不提供 Bash 工具。

**验证：**

```powershell
mvn -o "-Dmaven.repo.local=C:\Users\10355\.m2\repository" -Dtest=StreamingToolSchedulerPermissionTest test
```

预期：调度器权限集成测试全部通过。

## ST8：增加真实进程端到端测试

**文件：**

- `src/test/java/io/imiocode/PermissionApplicationE2ETest.java`

**依赖：** ST7

**步骤：**

1. 启动真实 ImioCode 进程。
2. 请求执行 `git status`，验证没有确认框。
3. 请求执行非白名单命令，验证出现 HITL。
4. 请求下载并执行脚本，验证直接拒绝且没有允许选项。
5. 保留原有 allow-once 和错误配置场景。

**验证：**

```powershell
mvn -o "-Dmaven.repo.local=C:\Users\10355\.m2\repository" -Dtest=PermissionApplicationE2ETest test
```

预期：进程退出状态和终端输出符合三个端到端场景。

## ST9：全量回归与验收准备

**文件：** 全项目；不修改或提交用户的 `claude.md`、`hello.txt`

**依赖：** ST8

**步骤：**

1. 运行全部测试。
2. 确认原有 218 项测试继续通过。
3. 打包可执行 JAR。
4. 启动真实程序完成一次人工终端测试。
5. 根据 `supplement-checklist.md` 记录实际证据。

**验证：**

```powershell
mvn -o "-Dmaven.repo.local=C:\Users\10355\.m2\repository" test
mvn -o "-Dmaven.repo.local=C:\Users\10355\.m2\repository" -DskipTests package
```

预期：0 failures、0 errors，且可执行 JAR 构建成功。

## 执行顺序

```text
ST1 → ST2 → ST3 → ST4
                    ├→ ST6 → ST7 → ST8 → ST9
ST5 ────────────────┘
```
