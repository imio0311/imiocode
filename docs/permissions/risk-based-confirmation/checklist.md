# 风险分级确认策略 Checklist

> 每项必须通过运行测试或观察真实行为验证；先记录证据，再标记通过。

## 默认模式与兼容

- [ ] 未配置 `permissions.mode` 时进入 `auto-edit`。（验证：加载最小配置，观察运行时权限模式）
- [ ] `config.yaml` 和 `config.example.yaml` 使用 `auto-edit`。（验证：读取配置并启动应用，状态栏显示 auto-edit）
- [ ] 显式 `ask` 仍允许读取、询问写入和命令。（验证：模式矩阵测试）
- [ ] 显式 `read-only` 仍拒绝写入和命令。（验证：模式矩阵测试）
- [ ] 显式 `full-access` 仍允许未命中硬拦截的操作。（验证：模式矩阵测试）
- [ ] 显式 `lockdown` 仍拒绝全部工具。（验证：模式矩阵测试）
- [ ] `/permission` 查询和切换五种模式的用法不变。（验证：Slash Command 测试）

## 动态风险分类

- [ ] 工作区文件读取、搜索和 Git 查询返回 LOW。（验证：分类器参数化测试）
- [ ] Maven、Gradle、npm、Go、Cargo、dotnet 构建/测试/检查/格式化返回 MEDIUM。（验证：分类器参数化测试）
- [ ] Git add、commit、switch、merge、pull、fetch 返回 MEDIUM。（验证：分类器参数化测试）
- [ ] 普通工作区目录创建、文件创建、复制和移动返回 MEDIUM。（验证：Windows/跨平台命令测试）
- [ ] 删除、递归删除、覆盖和输出重定向返回 HIGH。（验证：高风险参数化测试）
- [ ] Git push、force push、reset、rebase、clean 和强制 checkout 返回 HIGH。（验证：高风险参数化测试）
- [ ] 发布、全局安装和系统包安装/卸载返回 HIGH。（验证：高风险参数化测试）
- [ ] 权限、注册表、服务、计划任务和执行策略修改返回 HIGH。（验证：Windows 高风险测试）
- [ ] 远程脚本、命令替换、eval/Invoke-Expression 返回 HIGH 或危险拒绝。（验证：动态执行测试）
- [ ] 未知程序、空命令、未闭合引号和无法解析语法采用 HIGH。（验证：保守回退测试）
- [ ] 分类原因不回显完整命令、密钥或令牌。（验证：敏感参数断言）

## 复合命令

- [ ] LOW + LOW 聚合为 LOW。（验证：管道和条件连接测试）
- [ ] LOW + MEDIUM 聚合为 MEDIUM。（验证：复合构建命令测试）
- [ ] MEDIUM + HIGH 聚合为 HIGH。（验证：构建后发布测试）
- [ ] 任一片段命中危险黑名单时整体 DENY。（验证：只读命令拼接危险命令）
- [ ] 引号中的分隔符不被错误切成新命令。（验证：Shell 扫描/分词测试）
- [ ] 重定向、后台执行、子进程和命令替换不会被误判为普通开发命令。（验证：副作用语法测试）

## 权限链与规则

- [ ] `auto-edit` 自动允许 LOW 和 MEDIUM 请求。（验证：模式策略测试）
- [ ] `auto-edit` 对 HIGH 请求返回 ASK。（验证：模式策略测试）
- [ ] 危险命令在 `full-access` 和 allow 规则下仍 DENY。（验证：Checker 集成测试）
- [ ] 路径沙箱拒绝不能被 `full-access` 或 allow 规则绕过。（验证：路径逃逸集成测试）
- [ ] 精确 deny 和 ask 规则优先于风险自动允许。（验证：三层规则测试）
- [ ] 精确 allow 规则可以放行对应 HIGH 请求。（验证：高风险规则放行测试）
- [ ] 三层规则原有优先级保持不变。（验证：用户级、项目级、本地级冲突测试）
- [ ] 强制覆盖 Skill 默认 HIGH，在 auto-edit 下 ASK。（验证：安装工具权限测试）
- [ ] 强制覆盖 Skill 的精确 allow 规则仍不能绕过专用确认。（验证：安装规则测试）

## MCP、第三方工具与调度

- [ ] 未知 MCP 工具保持 HIGH，并在 auto-edit 下 ASK。（验证：MCP 包装器和 Gate 测试）
- [ ] MCP 精确 allow 规则可免确认，deny 规则仍拒绝。（验证：MCP 规则测试）
- [ ] Skill 专属工具声明 LOW/MEDIUM 时按声明处理，声明 HIGH 时确认。（验证：动态 Skill 工具测试）
- [ ] Bash 工具定义仍为 HIGH，确保调度器继续串行执行。（验证：ToolDefinition 和 partition 测试）
- [ ] Bash 权限请求使用动态风险，不受静态 HIGH 影响。（验证：PermissionRequestFactory 测试）

## HITL 与 UI

- [ ] 普通文件写入和 MEDIUM 命令不产生 PermissionRequestEvent。（验证：脚本化 Agent 测试）
- [ ] HIGH 请求产生一次确认事件，工具在回复前不执行。（验证：阻塞状态和执行计数）
- [ ] 提示显示工具名、HIGH、脱敏目标和具体风险原因。（验证：PermissionPrompt 断言）
- [ ] `ALLOW_ONCE` 仅允许当前调用，`ALLOW_SESSION` 复用相同工具与目标。（验证：Coordinator 测试）
- [ ] `DENY` 后工具不执行，Agent 能收到拒绝结果并继续或停止。（验证：Agent 集成测试）
- [ ] `/clear` 仍能取消待确认请求并唤醒 Agent。（验证：取消集成测试）

## 安全与实现约束

- [ ] 风险分类过程不启动进程、不访问网络、不写文件。（验证：零调用测试替身）
- [ ] 分类异常安全降级为 HIGH/ASK 或 ERROR/DENY，不会自动允许。（验证：故障注入）
- [ ] API Key、Bearer Token 和常见 secret 参数在提示与错误中被脱敏。（验证：SecretRedactor 集成断言）
- [ ] 风险分类不改变工具实际参数和执行结果。（验证：执行前后 ToolCall 参数一致）

## 编译与回归

- [ ] 新增分类、分词、工厂、策略和 Checker 定向测试全部通过。（验证：定向 Maven 测试）
- [ ] 既有危险命令、沙箱、规则、HITL、Agent Loop 和 MCP 测试全部通过。（验证：相关测试包）
- [ ] Java 21 `mvn clean test` 无 failure/error。（验证：汇总 Surefire 报告）
- [ ] `mvn -DskipTests package` 成功生成可执行 fat JAR。（验证：JAR 存在且入口可运行）
- [ ] `git diff --check` 无空白错误。（验证：Git 命令退出码为 0）
- [ ] 用户无关的 `claude.md`、`hello.txt` 和运行时 `.imiocode` 文件未被暂存。（验证：`git status --short`）

## 端到端场景

- [ ] 场景一：启动默认配置 → 请求读取和修改工作区文件 → 无确认并成功完成。
- [ ] 场景二：请求运行 Maven 测试 → 动态风险为 MEDIUM → 无确认并返回测试结果。
- [ ] 场景三：请求执行 Git push → UI 显示 HIGH 和原因 → 允许后才执行。
- [ ] 场景四：请求删除文件 → UI 显示 HIGH → 拒绝后文件仍存在。
- [ ] 场景五：请求危险系统破坏命令 → 无确认机会，直接硬拒绝。
- [ ] 场景六：调用未知 MCP 工具 → 默认确认；配置精确 allow 后不再确认。
- [ ] 场景七：切换 `/permission ask` → 普通写入重新要求确认；切回 auto-edit 后恢复风险驱动行为。
