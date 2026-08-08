# CH11 Skill 系统补齐 Spec

## 背景

ImioCode 已具备三级 Skill 目录、两阶段加载、inline/fork 执行、工具白名单、目录型 Skill 和本地管理命令，但不能从远程 URL 安装 Skill。Agent 遇到 skills.sh 或 GitHub 地址时只能尝试 Bash、npx 等通用手段，容易因目标目录不兼容或外部命令失败而反复循环。本周期扩展原 CH11 范围，增加安全的远程安装，并补齐内置 Skill 与取消清理行为。

## 目标

- Agent 可通过内置工具安全安装受支持的远程 Skill。
- 用户可通过本地命令显式安装，安装后无需重启即可调用。
- 远程下载、校验、覆盖和回滚具有明确安全边界。
- 补齐 backend-interview 内置 Skill 和活动 Skill 取消清理。
- 保持现有本地 Skill、内置 Skill、权限系统和 Slash Command 兼容。

## 功能需求

- F1：新增 `install_skill` 内置工具，接收远程 URL，安装到项目级 `.imiocode/skills/`。
- F2：支持 `https://skills.sh/<owner>/<repo>/<skill>`、GitHub tree 目录 URL 和 GitHub Raw `SKILL.md` URL。
- F3：只允许 HTTPS，并限制受信域名、重定向目标、文件数量、单文件大小、总大小和下载超时。
- F4：GitHub 目录型 Skill 递归下载 `SKILL.md`、`tool.json`、`references/` 以及同目录合法资源。
- F5：拒绝符号链接、绝对路径、相对路径逃逸、缺少 `SKILL.md`、非法元信息和保留命令名。
- F6：先下载到临时目录，完整解析通过后再原子移动；失败时不留下半成品。
- F7：同名 Skill 默认拒绝覆盖；仅显式 `force=true` 时替换，并保证失败可回滚。
- F8：安装成功后自动刷新 Skill 目录和 Slash Command，不需要重启。
- F9：安装行为接入现有权限系统，危险来源或覆盖操作不能绕过当前权限模式。
- F10：安装过程发出排队、下载、验证、成功或失败状态，终端仅展示简洁信息。
- F11：新增 `/skill install <URL>` 本地命令，与模型工具复用同一安装服务。
- F12：新增内置 `backend-interview` fork Skill，同时保留 commit、review、test。
- F13：执行 `/clear` 时，先取消活动 Agent、fork Skill 和工具，再清空终端。
- F14：安装错误必须简短、可操作且不泄露下载正文、密钥或异常栈。
- F15：安装后的 Skill 同时支持 Slash Command 显式调用和 Agent 摘要匹配后按需加载。

## 非功能需求

- N1：使用 Java 标准 HTTP 客户端，不依赖 npx、npm、git 或外部安装程序。
- N2：下载默认超时 30 秒，最多 64 个文件；单文件不超过 256 KiB，总大小不超过 2 MiB。
- N3：只接受 skills.sh、github.com、raw.githubusercontent.com 和 GitHub API 的受控 HTTPS 跳转。
- N4：所有目标路径写入前完成标准化和目录边界校验，Windows 与 Linux 行为一致。
- N5：安装阶段只保存和解析远程内容，不执行脚本或 `tool.json` 命令。
- N6：HTTP 传输层可替换，自动化测试不依赖真实网络。
- N7：现有 CH2—CH11 接口和行为保持兼容。
- N8：安装、覆盖、回滚和取消具有确定性；异常退出不能破坏已有 Skill。
- N9：日志和 UI 不输出认证头、环境变量、下载正文或完整异常栈。
- N10：新增行为必须具备单元、集成和真实应用入口端到端测试证据。

## 不做的事

- 不建设 Skill 市场浏览、搜索、评分和推荐页面。
- 不实现版本锁定、自动升级、依赖解析和更新检查。
- 不支持任意网站、HTTP、FTP、SSH 或本地网络地址。
- 不执行远程仓库中的安装脚本、构建脚本或二进制文件。
- 不支持需要登录的私有 GitHub 仓库和 GitHub Token。
- 不安装整个仓库，只获取目标 Skill 目录范围内的文件。
- 不修改 MCP、Plugin 或 Codex Skill 安装机制。
- 不自动提交或推送安装结果。
- 不新增图形化管理页面。

## 验收标准

- AC1：Agent 安装 skills.sh 的 frontend-design 成功，不调用 npx，不耗尽循环。
- AC2：GitHub tree URL 可安装包含 `SKILL.md`、`tool.json`、`references/` 的完整目录型 Skill。
- AC3：Raw `SKILL.md` URL 可安装单文件 Skill。
- AC4：安装后无需重启，`/skill list`、Tab 补全和 `/skill info` 立即可见。
- AC5：安装后的 Skill 可通过 `/name` 调用，也可由 Agent根据摘要自主选择。
- AC6：非法域名、HTTP、跨域重定向、路径逃逸、超限文件和非法 Skill 被拒绝且无残余文件。
- AC7：同名安装默认失败；显式强制覆盖成功；覆盖失败时旧版本仍可使用。
- AC8：远程 Skill 中的脚本和专属命令在安装阶段不会执行。
- AC9：`/skill install <URL>` 与 `install_skill` 使用相同校验、安装和热刷新逻辑。
- AC10：backend-interview 自动注册为 fork Skill，commit/review/test 保持可用。
- AC11：活动 Agent 或 fork Skill 期间执行 `/clear`，网络请求、工具和临时白名单均被取消清理。
- AC12：完整 Maven 测试和打包通过，CH2—CH11 无回归。
- AC13：受控子进程端到端测试覆盖安装成功、恶意来源拒绝、热刷新和取消。
- AC14：错误输出不包含下载正文、密钥或异常栈。
