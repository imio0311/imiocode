# CH11 Skill 系统补齐验收报告

## 结果

CH11 补齐范围已通过单元、集成、子进程和真实远程安装验收。

## 自动化证据

- Java 21 执行 `mvn -q clean test`：441 tests，0 failures，0 errors，3 skipped。
- `JdkSkillRemoteTransportTest`：覆盖成功响应、Content-Length 上限、HTTP 错误脱敏、超时、逐跳重定向校验、重定向上限和取消未完成请求。
- `RemoteSkillLocatorTest`：覆盖 skills.sh、GitHub tree、GitHub Raw，以及 HTTP、非信任域名、用户信息、自定义端口、查询参数、点段和编码分隔符拒绝。
- `SkillDownloadBudgetTest` 与 `GitHubSkillFetcherTest`：覆盖文件/总字节预算、文件数限制、目录下载和链接拒绝；文件内容统一通过 GitHub Contents API 的 base64 响应获取。
- `DefaultSkillInstallerTest`：覆盖新安装、同名拒绝、强制覆盖失败回滚和事务残留清理。
- `InstallSkillToolTest`、`PermissionCheckerTest` 与 `SkillManagementCommandTest`：覆盖工具 Schema、取消、规范化写目标、force 高风险决策、五种权限模式以及本地 Slash Command 服务调用。
- `BuiltinCommandTest`：验证 `/clear` 先统一取消活动工作，再清屏。
- `SkillLoaderTest`：确认内置索引同时加载 commit、review、test、backend-interview；backend-interview 使用 fork/recent 和只读搜索工具。
- `Ch11ApplicationIT`：既有 Skill 管理、显式调用、自然语言激活和 fork 隔离全部通过。
- `mvn -q -DskipTests package`：fat JAR 成功生成；`jar tf` 确认包含四个内置 Skill 的 `SKILL.md`。
- `git diff --check`：无空白错误。

## 真实端到端

Windows 环境没有 tmux，因此使用真实 ImioCode 子进程完成等价的终端端到端测试：

1. 在隔离工作区启动 fat JAR，Provider 指向不可达本地地址，确保安装命令不能依赖模型。
2. 输入 `/skill install https://skills.sh/anthropics/skills/frontend-design`。
3. 观察到 downloading、completed，并得到“可立即使用 /frontend-design”。
4. 不重启继续输入 `/skill info frontend-design`，看到来源为 project、模式为 inline、历史为 recent。
5. 加载结果的工具白名单为 read_file、write_file、edit_file、glob、grep，没有 Bash、npx、npm 或 git。
6. 子进程正常退出，模型端点未被访问。

另用安装服务对当前项目执行真实强制覆盖，完整经历 queued、downloading、validating、installing、reloading、completed；安装结果已保留在 `.imiocode/skills/frontend-design`。

## Git 范围

本次提交只纳入 CH11 补齐代码、测试、配置示例和文档。用户原有 `claude.md`、`hello.txt` 以及运行时安装的 `.imiocode/skills/frontend-design` 不纳入版本控制。
