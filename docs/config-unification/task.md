# 统一配置文件 Tasks

## 文件清单

| 操作 | 文件 | 职责 |
|------|------|------|
| 修改 | `src/main/java/io/imiocode/config/ConfigDocument.java` | 增加 MCP、权限统一 YAML 文档结构 |
| 修改 | `src/main/java/io/imiocode/config/YamlConfigLoader.java` | 严格且单次读取统一 YAML |
| 修改 | `src/main/java/io/imiocode/config/ConfigLoader.java` | 统一加载编排与基础配置解析复用 |
| 新建 | `src/main/java/io/imiocode/config/RuntimeConfig.java` | 全部运行期配置快照 |
| 新建 | `src/main/java/io/imiocode/config/ConfigSource.java` | 配置来源枚举 |
| 新建 | `src/main/java/io/imiocode/config/ConfigSourceSummary.java` | 各配置域来源摘要 |
| 新建 | `src/main/java/io/imiocode/config/ConfigNotice.java` | 安全迁移提示 |
| 新建 | `src/main/java/io/imiocode/config/EnvironmentPlaceholderResolver.java` | 通用 `${NAME}` 展开 |
| 新建 | `src/main/java/io/imiocode/config/MissingEnvironmentVariableException.java` | 缺失环境变量安全异常 |
| 修改 | `src/main/java/io/imiocode/mcp/config/McpConfigDocument.java` | 支持逐 Server 节点解析 |
| 修改 | `src/main/java/io/imiocode/mcp/config/McpConfigLoader.java` | 统一/旧格式双入口与共用校验 |
| 修改 | `src/main/java/io/imiocode/mcp/config/McpEnvironmentResolver.java` | 复用通用占位符解析器 |
| 修改 | `src/main/java/io/imiocode/permission/rule/PermissionConfigDocument.java` | 允许统一配置复用文档模型 |
| 修改 | `src/main/java/io/imiocode/permission/rule/PermissionRuleLoader.java` | 统一/旧格式双入口 |
| 修改 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 只消费 `RuntimeConfig` |
| 修改 | `config.yaml` | 迁入 MCP、权限并改用环境变量密钥引用 |
| 删除 | `.imiocode/mcp.local.yaml` | 当前项目不再保留重复 MCP 工作配置 |
| 删除 | `.imiocode/permissions.local.yaml` | 当前项目不再保留重复权限工作配置 |
| 新建 | `docs/config-unification/migration.md` | 完整示例与旧格式迁移说明 |
| 修改 | `README.md`（若存在）及相关章节文档 | 统一配置入口说明 |
| 修改 | `src/test/java/io/imiocode/config/ConfigLoaderTest.java` | 基础配置及 Provider 占位符回归 |
| 修改 | `src/test/java/io/imiocode/config/YamlConfigLoaderTest.java` | 根文档严格解析与存在性语义 |
| 新建 | `src/test/java/io/imiocode/config/EnvironmentPlaceholderResolverTest.java` | 占位符与安全错误测试 |
| 新建 | `src/test/java/io/imiocode/config/UnifiedConfigLoaderTest.java` | 统一装配、来源和回退测试 |
| 修改 | `src/test/java/io/imiocode/mcp/config/McpConfigLoaderTest.java` | MCP 双入口与隔离测试 |
| 修改 | `src/test/java/io/imiocode/permission/rule/PermissionRuleLoaderTest.java` | 权限双入口测试 |
| 新建 | `src/test/java/io/imiocode/UnifiedConfigApplicationIT.java` | 真实启动集成测试 |

## T1：扩展统一 YAML 文档模型

**文件：**

- `src/main/java/io/imiocode/config/ConfigDocument.java`
- `src/main/java/io/imiocode/config/YamlConfigLoader.java`
- `src/test/java/io/imiocode/config/YamlConfigLoaderTest.java`

**依赖：** 无

**步骤：**

1. 为根文档增加可空 `mcp` 和 `permissions` 字段，保留字段缺失与空对象的区别。
2. MCP 文档保存有序 Server 名称和原始节点；权限文档保存模式和有序规则。
3. 确保根、权限和 MCP 容器级未知字段被严格拒绝，MCP Server 内部错误留给逐 Server 解析。
4. 增加缺失、空对象、完整配置、未知字段和错误类型测试。

**验证：** 设置 JDK 21 后运行 `mvn -Dtest=YamlConfigLoaderTest test`，期望 0 failures、0 errors。

## T2：实现配置来源与运行期聚合对象

**文件：**

- `src/main/java/io/imiocode/config/RuntimeConfig.java`
- `src/main/java/io/imiocode/config/ConfigSource.java`
- `src/main/java/io/imiocode/config/ConfigSourceSummary.java`
- `src/main/java/io/imiocode/config/ConfigNotice.java`

**依赖：** T1

**步骤：**

1. 定义统一、旧格式、默认值三种来源。
2. 定义基础、MCP、权限三个域的来源摘要。
3. 定义带稳定代码和安全文本的迁移提示。
4. 定义不可变 `RuntimeConfig`，构造时复制列表并校验必填对象。
5. 为文本表示实现脱敏，禁止输出配置正文和凭据。

**验证：** 运行 `mvn -DskipTests compile`，期望编译成功。

## T3：实现通用环境变量占位符解析

**文件：**

- `src/main/java/io/imiocode/config/EnvironmentPlaceholderResolver.java`
- `src/main/java/io/imiocode/config/MissingEnvironmentVariableException.java`
- `src/test/java/io/imiocode/config/EnvironmentPlaceholderResolverTest.java`

**依赖：** 无

**步骤：**

1. 识别 `${NAME}`，支持单值多个占位符和普通文本混合。
2. 展开时把非空值交给秘密注册回调。
3. 缺失变量时仅在异常中保存变量名，不包含原始配置值。
4. 覆盖空字符串、多个占位符、特殊替换字符、缺失变量和确定性测试。

**验证：** 运行 `mvn -Dtest=EnvironmentPlaceholderResolverTest test`，期望全部通过。

## T4：让基础 Provider 配置支持安全占位符

**文件：**

- `src/main/java/io/imiocode/config/ConfigLoader.java`
- `src/test/java/io/imiocode/config/ConfigLoaderTest.java`

**依赖：** T1、T3

**步骤：**

1. 把现有基础配置转换逻辑提取为接收已解析 `ConfigDocument` 的内部方法。
2. 保持现有环境变量直接覆盖优先级。
3. 对 YAML 中的 Provider API Key 和 Base URL 执行 `${NAME}` 展开。
4. 保持直接字符串兼容，并确保缺失变量、非法 URI 和对象文本不会泄密。
5. 保持现有 `load(...)` 对外行为与默认值。

**验证：** 运行 `mvn -Dtest=ConfigLoaderTest test`，期望全部通过且脱敏断言通过。

## T5：统一 MCP Server 解析路径

**文件：**

- `src/main/java/io/imiocode/mcp/config/McpConfigDocument.java`
- `src/main/java/io/imiocode/mcp/config/McpConfigLoader.java`
- `src/main/java/io/imiocode/mcp/config/McpEnvironmentResolver.java`
- `src/test/java/io/imiocode/mcp/config/McpConfigLoaderTest.java`

**依赖：** T1、T3

**步骤：**

1. 提取单个 Server 节点的反序列化、默认值、Transport、URI 和超时校验。
2. 让 MCP 环境解析复用通用占位符解析器，并继续注册动态秘密。
3. 保持一个 Server 失败、其他 Server 成功的部分成功结果。
4. 为未知字段、错误类型、冲突传输、缺失变量和错误脱敏增加测试。

**验证：** 运行 `mvn -Dtest=McpConfigLoaderTest test`，期望全部通过。

## T6：增加 MCP 统一与旧格式双入口

**文件：**

- `src/main/java/io/imiocode/mcp/config/McpConfigLoader.java`
- `src/test/java/io/imiocode/mcp/config/McpConfigLoaderTest.java`

**依赖：** T5

**步骤：**

1. 实现接收根配置 MCP 文档和根文件路径的统一入口。
2. 把现有三层文件加载保留为旧格式入口，维持当前合并顺序。
3. 让旧 `load(...)` 委托到旧格式入口，避免破坏调用方。
4. 验证统一入口不访问旧文件，空 `servers` 表示明确禁用全部 MCP。

**验证：** 运行 `mvn -Dtest=McpConfigLoaderTest test`，期望统一与旧格式测试全部通过。

## T7：增加权限统一与旧格式双入口

**文件：**

- `src/main/java/io/imiocode/permission/rule/PermissionConfigDocument.java`
- `src/main/java/io/imiocode/permission/rule/PermissionRuleLoader.java`
- `src/test/java/io/imiocode/permission/rule/PermissionRuleLoaderTest.java`

**依赖：** T1

**步骤：**

1. 实现统一权限文档入口，规则按书写顺序映射为项目级规则。
2. 未声明模式时使用 `ASK`，空规则列表合法。
3. 保留原有三层文件加载为旧格式入口及旧 `load(...)` 兼容方法。
4. 为五种模式、allow/deny/ask、目标匹配、非法 Glob、空工具名和完整接管增加测试。

**验证：** 运行 `mvn -Dtest=PermissionRuleLoaderTest,PermissionRuleEngineTest test`，期望全部通过。

## T8：实现统一加载编排

**文件：**

- `src/main/java/io/imiocode/config/ConfigLoader.java`
- `src/test/java/io/imiocode/config/UnifiedConfigLoaderTest.java`

**依赖：** T2、T4、T6、T7

**步骤：**

1. 实现 `loadAll(workspace, userHome, environment)`，一次读取根文档。
2. 先构造 `AppConfig` 和共享 `SecretRedactor`，再解析 MCP 与权限。
3. 字段存在时调用统一入口；字段缺失时调用旧格式入口。
4. 为回退域生成来源摘要和一次迁移提示；默认域不生成虚假警告。
5. 统一配置失败时直接报告，不回退旧格式。
6. 测试统一接管、空对象接管、混合域回退、全部旧格式回退、来源摘要和共享脱敏器。

**验证：** 运行 `mvn -Dtest=UnifiedConfigLoaderTest test`，期望全部通过。

## T9：接入应用启动链与安全展示

**文件：**

- `src/main/java/io/imiocode/ImioCodeApplication.java`
- 相关终端测试（仅在现有接口不足时修改）

**依赖：** T8

**步骤：**

1. 应用启动改为只调用 `ConfigLoader.loadAll(...)`。
2. 从 `RuntimeConfig` 获取共享脱敏器、权限设置和 MCP 结果。
3. 移除启动入口对 MCP、权限加载器的直接实例化。
4. Welcome 后显示迁移提示、MCP 连接统计和权限模式。
5. 确认任何启动错误与摘要都不输出配置正文或敏感值。

**验证：** 运行 `mvn -DskipTests package`，期望应用编译和打包成功。

## T10：迁移当前项目配置

**文件：**

- `config.yaml`
- `.imiocode/mcp.local.yaml`
- `.imiocode/permissions.local.yaml`

**依赖：** T9

**步骤：**

1. 保留现有基础、Thinking、Agent 和上下文配置。
2. 把 Context7 Server 配置迁入 `mcp.servers`。
3. 把当前权限模式与 Context7 只读工具规则迁入 `permissions`。
4. 把 Provider 明文密钥替换为 `${DEEPSEEK_API_KEY}` 引用，不在补丁、日志或报告中复述原密钥。
5. 验证统一配置可解析后，删除当前项目两份重复的本地工作配置。
6. 检查 Git 状态，确保不触碰用户的 `claude.md` 和 `hello.txt`。

**验证：** 设置 `DEEPSEEK_API_KEY` 后运行配置加载测试或应用启动探针，期望来源均为 `UNIFIED`，且没有旧格式迁移提示。

## T11：编写统一配置与迁移文档

**文件：**

- `docs/config-unification/migration.md`
- `README.md`（若存在）
- 现有 MCP、权限、上下文配置说明

**依赖：** T10

**步骤：**

1. 提供覆盖全部配置域的无真实凭据示例。
2. 给出 MCP、权限旧格式到新格式的等价对照。
3. 解释字段存在即接管、字段缺失才回退的规则。
4. 说明 PowerShell、CMD、Bash 设置环境变量的方法。
5. 更新相关文档路径，并明确旧格式只是兼容入口。

**验证：** 搜索文档中的旧文件名，期望只出现在兼容迁移章节；扫描密钥样式，期望没有真实凭据。

## T12：增加真实启动集成测试

**文件：**

- `src/test/java/io/imiocode/UnifiedConfigApplicationIT.java`
- 必要的测试夹具

**依赖：** T9、T10

**步骤：**

1. 用临时工作区和统一 `config.yaml` 启动真实应用装配路径。
2. 使用可控本地 MCP 测试 Server 验证工具发现，禁止依赖公网服务。
3. 验证统一权限规则对只读工具返回 allow，对需要确认的操作返回 ask。
4. 验证启动来源摘要、迁移提示和错误输出不包含测试秘密。

**验证：** 运行 `mvn -Dtest=UnifiedConfigApplicationIT test`，期望全部通过。

## T13：执行聚焦回归测试

**文件：** 本次所有实现与测试文件

**依赖：** T1-T12

**步骤：**

1. 运行配置、MCP、权限、应用启动相关测试。
2. 修复所有回归，不通过时不得进入全量验收。
3. 检查测试报告中 failures、errors 和 skipped 数量。

**验证：** 运行 `mvn -Dtest='*Config*Test,*Permission*Test,UnifiedConfigApplicationIT' test`；Windows 下按 Surefire 支持的模式调整命令，期望 0 failures、0 errors。

## T14：执行全量测试与打包

**文件：** 整个项目

**依赖：** T13

**步骤：**

1. 使用 JDK 21 运行全部 Maven 测试。
2. 生成可运行 shaded JAR。
3. 汇总测试总数、失败数、错误数和平台跳过项。

**验证：** 运行 `mvn clean package`，期望 BUILD SUCCESS、0 failures、0 errors。

## T15：执行端到端验收并记录结果

**文件：**

- `docs/config-unification/checklist.md`
- `docs/config-unification/acceptance-report.md`

**依赖：** T14、已批准的 checklist

**步骤：**

1. 优先在 tmux 中启动 shaded JAR，输入真实只读 MCP 请求，观察工具发现和调用。
2. 输入一个命中 allow 的只读工具请求和一个需要 ask 的操作，观察权限决策。
3. 如果 Windows 环境无 tmux，记录客观阻塞，并用真实 shaded JAR 子进程测试作为替代证据，不伪造 tmux 结果。
4. 按 checklist 逐项记录实际证据、通过项和平台阻塞项。

**验证：** 验收报告包含实际命令、测试计数、启动输出摘要和每个未通过项的原因。

## T16：分组提交与最终检查

**文件：** 本次变更文件

**依赖：** T15

**步骤：**

1. 按“配置模型与解析”“MCP/权限与启动集成”“迁移文档与验收”分组提交。
2. 每次只暂存本任务文件，不暂存 `claude.md`、`hello.txt` 或其他用户改动。
3. 检查提交历史、工作树和敏感信息扫描结果。
4. 除非用户另行要求，只提交到本地，不自动推送远端。

**验证：** `git status --short` 只显示用户原有改动；`git log -3 --oneline` 显示本次分组提交，敏感信息扫描无命中。

## 执行顺序

```text
T1 ──┬──> T4 ───────────────┐
     ├──> T5 --> T6 ────────┤
     └──> T7 ───────────────┤
T3 ─────> T4 / T5           ├──> T8 --> T9 --> T10 --> T11
T2 ─────────────────────────┘                 └──────> T12
                                                      |
                                                      v
                                              T13 --> T14 --> T15 --> T16
```
