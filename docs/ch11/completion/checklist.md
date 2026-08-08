# CH11 Skill 系统补齐 Checklist

> 每项必须通过运行测试或观察真实应用行为验证，并在验收报告中记录实际证据。

## URL 与安全边界

- [ ] skills.sh 标准地址解析为正确仓库和 Skill 目录。（验证：参数化定位测试）
- [ ] GitHub tree 地址保留指定 revision 和目标目录。（验证：分支、tag、commit 地址测试）
- [ ] GitHub Raw 地址只接受以 `SKILL.md` 结尾的文件。（验证：Raw 地址测试）
- [ ] HTTP、用户信息、自定义端口、点段、编码分隔符和反斜杠被拒绝。（验证：恶意 URL 测试）
- [ ] 重定向每一跳重新校验，跨域或超过三跳时失败。（验证：受控 HTTP Server）
- [ ] 不访问 localhost、私网 IP 或不受信任域名。（验证：SSRF 拒绝测试）

## 下载预算与目录获取

- [ ] 单文件刚好 256 KiB 时允许，超一字节时拒绝。（验证：边界测试）
- [ ] 总大小刚好 2 MiB 时允许，超一字节时拒绝。（验证：共享预算测试）
- [ ] 最多 64 个文件，第 65 个文件触发失败。（验证：目录枚举测试）
- [ ] 目录递归超过 8 层时失败。（验证：深目录测试）
- [ ] symlink、submodule 和未知 GitHub 内容类型被拒绝。（验证：Contents API 响应测试）
- [ ] 取消下载会终止活动请求且不继续发起后续请求。（验证：可控 Future 测试）
- [ ] 下载错误不包含响应正文、请求头或异常栈。（验证：脱敏断言）

## 原子安装和回滚

- [ ] 新 Skill 完整验证后安装到 `.imiocode/skills/<name>`。（验证：临时工作区测试）
- [ ] 缺少 SKILL.md、非法 frontmatter、非法 tool.json 或路径逃逸时不安装。（验证：候选包测试）
- [ ] 安装阶段不执行脚本或 tool.json 命令。（验证：零执行计数断言）
- [ ] 同名 Skill 默认拒绝覆盖且旧版本保持可用。（验证：冲突测试）
- [ ] `force=true` 可替换同名项目 Skill。（验证：覆盖测试）
- [ ] 强制覆盖过程中解析、移动或 reload 失败时恢复旧版本。（验证：故障注入测试）
- [ ] 成功、失败和取消后均不存在临时目录或备份残留。（验证：目录扫描）

## 工具、权限与命令

- [ ] `install_skill` Schema 只接受 url 和 force，url 必填。（验证：Schema 测试）
- [ ] `install_skill` 在活动 Skill 白名单下仍可见。（验证：ToolSelection 测试）
- [ ] 普通安装经过写权限判定，不会被系统工具豁免绕过。（验证：PermissionGate 测试）
- [ ] force 覆盖在非 bypass 模式下要求高风险确认。（验证：五种权限模式测试）
- [ ] `/skill install URL` 在本地执行且 Provider 调用次数为零。（验证：命令集成测试）
- [ ] `/skill install URL --force` 与工具共用同一安装和回滚服务。（验证：服务调用断言）
- [ ] 安装过程产生 queued/downloading/validating/installing/reloading/completed 阶段。（验证：监听器序列）
- [ ] UI 只显示简短阶段和结果，不显示下载正文。（验证：终端输出断言）

## 热刷新与调用

- [ ] 安装后 `/skill list` 立即显示新 Skill，无需重启。（验证：应用入口测试）
- [ ] 安装后 `/skill info <name>` 显示项目来源和约束。（验证：命令测试）
- [ ] 安装后 Tab 补全立即包含 `/<name>`。（验证：Registry 测试）
- [ ] 安装后 `/<name>` 可显式触发完整 SOP。（验证：脚本化 Provider 测试）
- [ ] 下一轮摘要包含新 Skill 名称和描述，不包含完整正文。（验证：请求载荷断言）
- [ ] Agent 可根据自然语言调用 load_skill 激活新 Skill。（验证：多轮工具调用测试）
- [ ] 安装 frontend-design 不调用 Bash、npx、npm 或 git。（验证：工具轨迹断言）

## 内置 Skill 和取消

- [ ] 内置索引同时包含 commit、review、test、backend-interview。（验证：加载器测试）
- [ ] backend-interview 为 fork/recent，只允许读和搜索类工具。（验证：元信息断言）
- [ ] backend-interview SOP 覆盖材料解析、两轮面试、追问和复盘输出。（验证：内容契约测试）
- [ ] `/clear` 先取消主 Agent、fork 和安装器，再清空终端。（验证：调用顺序测试）
- [ ] `/clear` 不删除已有会话历史。（验证：会话快照测试）
- [ ] 取消后活动 Skill、临时工具和白名单全部清理。（验证：生命周期断言）

## 配置与兼容

- [ ] 不配置 skills.install 时使用安全默认值。（验证：配置测试）
- [ ] 合法 YAML 可调整超时和预算。（验证：配置加载测试）
- [ ] 非法配置在启动时给出可定位错误。（验证：应用启动测试）
- [ ] 原有项目级、用户级和内置 Skill 加载行为不变。（验证：既有 SkillLoaderTest）
- [ ] 原有 `/commit`、`/review`、`/test` 和 `/skill list/info/reload` 不回归。（验证：CH11 IT）
- [ ] Java 21 全量测试通过，无 failure/error。（验证：`mvn clean test`）
- [ ] fat JAR 打包成功并包含四个内置 Skill。（验证：`mvn package` + `jar tf`）
- [ ] `git diff --check` 无空白错误，用户无关文件未被暂存。（验证：Git 审计）

## 端到端场景

- [ ] 场景一：输入“安装 frontend-design” → Agent 调用 install_skill → Skill 安装并立即可调用。
- [ ] 场景二：执行 `/skill install <GitHub tree URL>` → 完整目录包安装 → `/skill info` 可见且无模型请求。
- [ ] 场景三：安装恶意域名或路径逃逸包 → 明确拒绝 → 工作区没有残留文件。
- [ ] 场景四：强制覆盖时模拟中途失败 → 原版本自动恢复并仍可调用。
- [ ] 场景五：安装过程中触发 `/clear` → 下载取消、临时目录清理、终端清空、会话历史保留。
- [ ] 场景六：调用 `/backend-interview` → 子 Agent 隔离执行 → 父会话只收到最终面试结果。
