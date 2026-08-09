# 风险分级确认策略 Tasks

## 文件清单

| 操作 | 文件 | 职责 |
|---|---|---|
| 新建 | `src/main/java/io/imiocode/permission/command/CommandRiskAssessment.java` | 动态风险结果 |
| 新建 | `src/main/java/io/imiocode/permission/command/CommandRiskClassifier.java` | 分类接口 |
| 新建 | `src/main/java/io/imiocode/permission/command/RegexCommandRiskClassifier.java` | 命令风险规则与聚合 |
| 新建 | `src/main/java/io/imiocode/permission/command/ShellCommandTokenizer.java` | 引号感知的共享分词器 |
| 新建 | `src/main/java/io/imiocode/permission/command/ShellTokenizeResult.java` | 分词结果 |
| 修改 | `src/main/java/io/imiocode/permission/command/StrictSafeCommandDetector.java` | 使用共享分词器 |
| 修改 | `src/main/java/io/imiocode/permission/PermissionRequest.java` | 携带风险原因并兼容旧构造器 |
| 修改 | `src/main/java/io/imiocode/permission/PermissionRequestFactory.java` | 为 Bash 解析动态风险 |
| 修改 | `src/main/java/io/imiocode/permission/PermissionModePolicy.java` | AUTO_EDIT 按风险决策 |
| 修改 | `src/main/java/io/imiocode/permission/PermissionChecker.java` | 统一规则与高风险路径 |
| 修改 | `src/main/java/io/imiocode/permission/PermissionSettings.java` | 默认模式改为 AUTO_EDIT |
| 修改 | `src/main/java/io/imiocode/permission/rule/PermissionRuleLoader.java` | 配置缺省模式调整 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 组装分类器与权限工厂 |
| 修改 | `config.yaml` | 当前项目使用 auto-edit |
| 修改 | `config.example.yaml` | 示例默认使用 auto-edit |
| 新建/修改 | `src/test/java/io/imiocode/permission/...` | 分类、模式、规则、提示测试 |
| 新建 | `src/test/java/io/imiocode/PermissionRiskApplicationIT.java` | 真实应用入口测试 |
| 新建 | `docs/permissions/risk-based-confirmation/acceptance.md` | 验收证据 |

## T1：定义动态风险领域模型

**文件：** `CommandRiskAssessment.java`、`CommandRiskClassifier.java`

**依赖：** 无

**步骤：**
1. 定义包含风险等级和安全原因的不可变结果。
2. 校验风险和原因均不能为空。
3. 定义纯文本分类接口，不暴露执行能力。

**验证：** 运行新增模型测试，合法构造成功，空风险或空原因被拒绝。

## T2：抽取共享 Shell 分词器

**文件：** `ShellCommandTokenizer.java`、`ShellTokenizeResult.java`、`StrictSafeCommandDetector.java`

**依赖：** T1

**步骤：**
1. 从严格只读检测器抽取现有引号感知分词逻辑。
2. 保留单引号、双引号、反斜杠空格和未闭合引号语义。
3. 让严格只读检测器改用共享分词器，不改变已有判断结果。

**验证：** 运行分词器测试和既有 `StrictSafeCommandDetectorTest`，全部通过。

## T3：建立分类器骨架和 LOW 判断

**文件：** `RegexCommandRiskClassifier.java`

**依赖：** T1、T2

**步骤：**
1. 注入严格只读检测器、Shell 扫描器和共享分词器。
2. 严格只读检测通过时返回 LOW。
3. 空命令、扫描失败、分词失败和无法识别的命令保守返回 HIGH。
4. 保证原因只描述类别，不回显完整命令。

**验证：** 定向测试覆盖只读、空输入、未闭合引号、重定向和未知程序。

## T4：实现 HIGH 风险规则

**文件：** `RegexCommandRiskClassifier.java`

**依赖：** T3

**步骤：**
1. 增加删除、递归删除和强制覆盖规则。
2. 增加 Git 远程写入、历史改写和强制操作规则。
3. 增加发布、全局安装和系统包管理规则。
4. 增加权限、注册表、服务、计划任务和执行策略规则。
5. 增加远程脚本、动态执行和 Shell 副作用语法规则。

**验证：** 参数化测试中每类至少包含一个 Windows 命令和一个跨平台命令，全部返回 HIGH。

## T5：实现 MEDIUM 规则和复合聚合

**文件：** `RegexCommandRiskClassifier.java`

**依赖：** T4

**步骤：**
1. 增加构建、测试、检查、格式化和编译工具白名单。
2. 增加普通 Git 本地工作流规则。
3. 增加工作区文件组织命令规则。
4. 对每个 Shell 片段独立分类，并取最高风险。
5. 高风险规则始终先于 MEDIUM 白名单，避免参数绕过。

**验证：** 参数化测试覆盖 Maven/Gradle/npm/Go/Cargo/dotnet、Git 本地操作和 LOW+MEDIUM/HIGH 复合命令。

## T6：扩展权限请求

**文件：** `PermissionRequest.java`

**依赖：** T1

**步骤：**
1. 增加 `riskReason` 字段并校验非空。
2. 提供原五参数兼容构造器。
3. 兼容构造器生成不含目标内容的静态风险原因。

**验证：** 现有权限请求测试编译通过，新增断言确认动态原因可用。

## T7：权限请求工厂接入动态分类

**文件：** `PermissionRequestFactory.java`

**依赖：** T3、T6

**步骤：**
1. 增加接收分类器的构造器，保留原兼容构造器。
2. 仅对 Bash 调用使用命令分类结果替换请求风险。
3. 保留 `PermissionTargetProvider` 提供的动态风险。
4. MCP 和未知第三方工具继续使用定义中的静态风险。
5. 保持目标规范化、脱敏和长度截断。

**验证：** 工厂测试断言普通 Bash 为 MEDIUM、高风险 Bash 为 HIGH、MCP 为 HIGH、目标仍被脱敏。

## T8：调整自动编辑模式策略

**文件：** `PermissionModePolicy.java`

**依赖：** T6

**步骤：**
1. AUTO_EDIT 对 LOW/MEDIUM 返回 ALLOW。
2. AUTO_EDIT 对 HIGH 返回 ASK，并使用 `riskReason`。
3. 保留 ASK、READ_ONLY、FULL_ACCESS、LOCKDOWN 语义。

**验证：** 模式矩阵测试覆盖五种模式 × 三种风险 × 三种操作。

## T9：统一权限检查顺序

**文件：** `PermissionChecker.java`

**依赖：** T8

**步骤：**
1. 保持危险命令、沙箱和模式上限顺序不变。
2. 保持三层显式规则在模式默认动作之前。
3. 保留强制 Skill 覆盖的专用确认分支，精确 allow 也不能绕过。
4. 确认精确 allow 可放行其他 HIGH，但危险命令、沙箱和强制覆盖兜底仍不能绕过。

**验证：** 检查器测试覆盖硬拒绝、沙箱、allow/ask/deny 规则、高风险默认确认和 full-access。

## T10：修改默认配置

**文件：** `PermissionSettings.java`、`PermissionRuleLoader.java`、`config.yaml`、`config.example.yaml`

**依赖：** T8

**步骤：**
1. 权限设置空模式默认 AUTO_EDIT。
2. 统一配置和旧三层配置都在没有 mode 时默认 AUTO_EDIT。
3. 将当前项目和示例 YAML 改为 `auto-edit`。
4. 显式模式继续按配置解析。

**验证：** 配置测试覆盖未配置、显式五种模式和非法模式。

## T11：完成应用组装

**文件：** `ImioCodeApplication.java`

**依赖：** T5、T7、T9、T10

**步骤：**
1. 创建并共享严格只读检测器、扫描器、分词器和风险分类器。
2. 将分类器注入权限请求工厂。
3. 保证 Checker 和分类器使用同一工作区语义。
4. 保持权限提示事件和 UI 控制器接口不变。

**验证：** 应用装配编译通过，启动状态显示 `auto-edit`。

## T12：补齐分类与权限单元测试

**文件：** `src/test/java/io/imiocode/permission/command/`、权限现有测试

**依赖：** T1—T11

**步骤：**
1. 增加风险类别参数化测试。
2. 增加复合命令、引号、重定向、替换和未知命令测试。
3. 增加五模式矩阵、规则优先级和动态提示原因测试。
4. 回归危险命令与路径沙箱测试。

**验证：** 权限相关定向 Maven 测试全部通过，无 failure/error。

## T13：增加应用入口集成测试

**文件：** `PermissionRiskApplicationIT.java`

**依赖：** T12

**步骤：**
1. 用脚本化 Provider 让 Agent 执行读文件、改文件和构建命令，断言不产生确认事件。
2. 让 Agent 执行高风险命令，断言出现动态 HIGH 提示且工具未提前执行。
3. 回复允许后断言 Agent 被唤醒；回复拒绝后断言工具不执行。
4. 模拟未知 MCP 工具并验证默认确认和精确规则放行。

**验证：** 子进程 IT 正常退出，无残留进程或临时文件。

## T14：全量验收与记录

**文件：** `checklist.md`、`acceptance.md`

**依赖：** T13

**步骤：**
1. 使用 Java 21 运行全量测试和打包。
2. 在可用终端会话中启动 ImioCode，输入真实读写、构建和高风险请求。
3. 对照 checklist 记录实际输出和证据。
4. 运行 `git diff --check` 并确认不纳入用户无关文件。

**验证：** `mvn clean test`、`mvn package`、端到端场景和 Git 审计全部通过。

## 执行顺序

```text
T1 → T2 → T3 → T4 → T5 ─┐
T1 ───────────→ T6 → T7 ├→ T11 → T12 → T13 → T14
T6 ───────────→ T8 → T9 ┤
T8 ───────────────→ T10 ┘
```
