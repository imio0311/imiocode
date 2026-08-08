# CH11 Skill 系统补齐 Tasks

## 文件清单

| 操作 | 文件 | 职责 |
|---|---|---|
| 修改 | `src/main/java/io/imiocode/config/ConfigDocument.java` | 映射 skills.install 配置 |
| 修改 | `src/main/java/io/imiocode/config/ConfigLoader.java` | 校验并组装安装限制 |
| 修改 | `src/main/java/io/imiocode/config/RuntimeConfig.java` | 携带 SkillInstallConfig |
| 新建 | `src/main/java/io/imiocode/skill/install/SkillInstallConfig.java` | 安装限制配置 |
| 新建 | `src/main/java/io/imiocode/skill/install/RemoteSkillKind.java` | 来源类型 |
| 新建 | `src/main/java/io/imiocode/skill/install/RemoteSkillLocation.java` | 规范化远程位置 |
| 新建 | `src/main/java/io/imiocode/skill/install/RemoteSkillLocator.java` | 安全 URL 解析 |
| 新建 | `src/main/java/io/imiocode/skill/install/RemoteResponse.java` | 受限 HTTP 响应 |
| 新建 | `src/main/java/io/imiocode/skill/install/SkillDownloadBudget.java` | 文件数和字节预算 |
| 新建 | `src/main/java/io/imiocode/skill/install/SkillRemoteTransport.java` | 网络抽象与取消 |
| 新建 | `src/main/java/io/imiocode/skill/install/JdkSkillRemoteTransport.java` | Java HttpClient 实现 |
| 新建 | `src/main/java/io/imiocode/skill/install/RemoteSkillFile.java` | 候选文件 |
| 新建 | `src/main/java/io/imiocode/skill/install/RemoteSkillPackage.java` | 候选包 |
| 新建 | `src/main/java/io/imiocode/skill/install/RemoteSkillFetcher.java` | 获取器接口 |
| 新建 | `src/main/java/io/imiocode/skill/install/GitHubSkillFetcher.java` | Raw/Contents API 下载 |
| 新建 | `src/main/java/io/imiocode/skill/install/SkillInstallRequest.java` | 安装请求 |
| 新建 | `src/main/java/io/imiocode/skill/install/SkillInstallResult.java` | 安装结果 |
| 新建 | `src/main/java/io/imiocode/skill/install/SkillInstallStage.java` | 进度阶段 |
| 新建 | `src/main/java/io/imiocode/skill/install/SkillInstallListener.java` | 脱敏进度接口 |
| 新建 | `src/main/java/io/imiocode/skill/install/SkillInstallException.java` | 安装领域错误 |
| 新建 | `src/main/java/io/imiocode/skill/install/SkillInstaller.java` | 安装服务接口 |
| 新建 | `src/main/java/io/imiocode/skill/install/DefaultSkillInstaller.java` | 原子安装和回滚 |
| 新建 | `src/main/java/io/imiocode/skill/InstallSkillTool.java` | Agent 安装工具 |
| 修改 | `src/main/java/io/imiocode/skill/SkillActivator.java` | 系统安装工具豁免白名单 |
| 修改 | `src/main/java/io/imiocode/skill/SkillManagementCommand.java` | install 子命令 |
| 修改 | `src/main/java/io/imiocode/command/CommandServices.java` | 安装和取消服务 |
| 修改 | `src/main/java/io/imiocode/command/builtin/ClearCommand.java` | 取消后清屏 |
| 修改 | `src/main/java/io/imiocode/permission/PermissionRequestFactory.java` | 安装权限目标 |
| 修改 | `src/main/java/io/imiocode/permission/PermissionGate.java` | 安装工具权限链 |
| 修改 | `src/main/java/io/imiocode/runtime/ConversationCoordinator.java` | 安装、刷新和统一取消 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 组件组装和注册 |
| 修改 | `config.example.yaml` | 安装配置示例 |
| 修改 | `config.yaml` | 当前项目安装配置 |
| 修改 | `src/main/resources/skills/index.txt` | 注册 backend-interview |
| 新建 | `src/main/resources/skills/backend-interview/SKILL.md` | 后端面试工作流 |
| 新建/修改 | `src/test/java/io/imiocode/...` | 单元、集成和端到端测试 |

## T1：定义安装配置

**文件：** 配置模型、`SkillInstallConfig.java`、两个 YAML 文件
**依赖：** 无

**步骤：**
1. 定义超时、文件数、单文件、总大小和允许域名。
2. 在 YAML 映射中增加 `skills.install`。
3. 对负数、零值、重复或非法域名返回安全配置错误。
4. 缺省时应用 30 秒、64 文件、256 KiB、2 MiB 和固定域名。

**验证：** 运行配置测试，默认值、合法覆盖和非法值断言通过。

## T2：实现远程 URL 定位

**文件：** `RemoteSkillKind.java`、`RemoteSkillLocation.java`、`RemoteSkillLocator.java`
**依赖：** T1

**步骤：**
1. 解析 skills.sh 三段地址。
2. 解析 GitHub tree 的 revision 和目录。
3. 解析 Raw SKILL.md 地址。
4. 拒绝 HTTP、用户信息、端口、查询污染、编码分隔符和点段。

**验证：** 参数化测试覆盖三类成功地址和恶意变体。

## T3：实现下载预算和传输接口

**文件：** `RemoteResponse.java`、`SkillDownloadBudget.java`、`SkillRemoteTransport.java`
**依赖：** T1

**步骤：**
1. 定义每个响应和整个安装共享的预算扣减。
2. 超限时立即失败，错误不含正文。
3. 定义可取消传输接口。

**验证：** 预算边界测试覆盖刚好上限和超一字节。

## T4：实现 Java HTTP 传输

**文件：** `JdkSkillRemoteTransport.java`
**依赖：** T2、T3

**步骤：**
1. 使用禁用自动重定向的 HttpClient。
2. 每跳校验 HTTPS、域名和重定向次数。
3. 同时检查 Content-Length 和实际响应字节数。
4. 保存活动 Future 并支持取消。
5. 将网络错误转换为脱敏领域错误。

**验证：** 使用本地内存 HTTP Server 验证成功、超时、跨域跳转、超限和取消。

## T5：实现 GitHub/Raw 获取器

**文件：** 远程文件模型、`RemoteSkillFetcher.java`、`GitHubSkillFetcher.java`
**依赖：** T2—T4

**步骤：**
1. Raw 来源生成单个根 `SKILL.md`。
2. tree/skills.sh 调用 Contents API。
3. 稳定递归目录并拒绝 symlink、submodule、越界路径和过深目录。
4. 共享文件数和总字节预算。

**验证：** 内存传输测试完整目录、空目录、链接、深度、文件数和大小限制。

## T6：定义安装领域模型和进度

**文件：** 安装 request/result/stage/listener/exception/interface
**依赖：** T1

**步骤：**
1. 定义不可变请求和结果。
2. 定义六阶段进度和 NOOP 监听器。
3. 保证 listener 只接收安全短消息。
4. 定义服务取消语义。

**验证：** 模型构造约束和阶段顺序测试通过。

## T7：实现原子安装和回滚

**文件：** `DefaultSkillInstaller.java`
**依赖：** T5、T6、现有 SkillParser/SkillLoader

**步骤：**
1. 在 `.imiocode/skills` 内创建唯一临时目录。
2. 安全写入候选包并完整解析。
3. 校验目录名、Skill 名和动态命令冲突。
4. 默认拒绝覆盖；force 时备份旧目录。
5. 原子移动、reload、同步命令，成功后删除备份。
6. 任意失败或取消时恢复旧目录并清理临时文件。

**验证：** 临时目录测试新装、冲突、覆盖、解析失败、reload 失败、取消和恢复。

## T8：接入安装权限链

**文件：** `InstallSkillTool.java`、权限工厂和 Gate
**依赖：** T6、T7

**步骤：**
1. 定义严格工具 Schema、描述和系统工具名称。
2. 提供规范化权限目标，不把 URL 当工作区文件路径。
3. 普通安装走写权限；force 覆盖标记高风险。
4. 工具 cancel 委托安装器。
5. 让 install_skill 不受 active Skill 白名单隐藏。

**验证：** 权限测试覆盖 default/ask/acceptEdits/plan/bypass 和 force。

## T9：实现 `/skill install`

**文件：** `SkillManagementCommand.java`、`CommandServices.java`、`ConversationCoordinator.java`
**依赖：** T7

**步骤：**
1. 解析 URL 与可选 `--force`，拒绝多余参数。
2. 通过 CommandServices 调用同一安装器。
3. 将阶段和结果转成简洁 CommandMessage。
4. 安装成功后同步动态命令和 Tab 补全。

**验证：** 命令测试断言 Provider 零调用、热刷新和错误信息。

## T10：补齐取消与 `/clear`

**文件：** `ClearCommand.java`、`CommandServices.java`、`ConversationCoordinator.java`
**依赖：** T7、T9

**步骤：**
1. CommandServices 增加统一取消入口。
2. Coordinator 同时取消主 Agent、fork 和安装器。
3. ClearCommand 先取消再调用 UI clear。
4. 保持会话历史不变。

**验证：** 并发测试验证取消顺序、临时工具清理和历史保留。

## T11：增加 backend-interview 内置 Skill

**文件：** 内置 Skill 与索引
**依赖：** 无

**步骤：**
1. 定义 fork/recent 元信息和只读工具白名单。
2. SOP 覆盖简历/项目解析、两轮技术面试、追问和复盘模板。
3. 加入内置索引，保留原有三项。

**验证：** 加载器测试看到四个内置 Skill，模式和工具约束正确。

## T12：完成应用组装和进度展示

**文件：** `ImioCodeApplication.java` 及必要 UI 适配
**依赖：** T4—T11

**步骤：**
1. 创建 HttpClient、Locator、Fetcher 和 Installer。
2. 注册 InstallSkillTool。
3. 将安全阶段消息连接到紧凑终端输出。
4. 应用关闭时取消并释放安装器。

**验证：** 应用装配测试编译通过，工具注册表包含 install_skill。

## T13：补充单元和集成测试

**文件：** `src/test/java/io/imiocode/skill/install/` 等
**依赖：** T1—T12

**步骤：**
1. 覆盖 URL、预算、网络、获取器、安装事务。
2. 覆盖权限、工具 Schema、Slash Command、取消和内置 Skill。
3. 覆盖错误脱敏和既有 Skill 回归。

**验证：** 定向 Maven 测试全部通过。

## T14：增加应用入口端到端测试

**文件：** `Ch11SkillInstallApplicationIT.java`
**依赖：** T13

**步骤：**
1. 使用受控 HTTP 服务模拟 skills.sh/GitHub。
2. 验证显式命令安装、工具安装、热刷新和调用。
3. 验证恶意来源、覆盖回滚和取消。
4. 断言安装流程不调用 npx。

**验证：** 子进程 IT 全部通过且无残留进程、目录。

## T15：全量回归、验收与文档

**文件：** `docs/ch11/completion/checklist.md`、验收报告
**依赖：** T14

**步骤：**
1. 运行 Java 21 全量测试和打包。
2. 对照 checklist 逐项记录实际证据。
3. 检查 JAR 包含四个内置 Skill。
4. 检查 Git 变更范围，不纳入用户无关文件。

**验证：** `mvn clean test`、IT、`mvn package`、`git diff --check` 全部通过。

## 执行顺序

```text
T1 → T2 ─┐
  └→ T3 → T4 → T5 ─┐
T1 → T6 ────────────┴→ T7 → T8 → T9 → T10 ─┐
T11 ─────────────────────────────────────────┼→ T12 → T13 → T14 → T15
                                             ┘
```
