# 统一配置文件验收报告

## 结论

当前已通过 51/53 项验收。核心功能、兼容回退、安全校验、完整构建、真实进程启动和 Git 隔离均通过；剩余 2 项受当前环境阻塞。

## 通过（51/53）

### 统一配置与兼容

- [x] 根 `config.yaml` 可同时装配 App、MCP、权限和共享脱敏器。
  - 证据：`UnifiedConfigLoaderTest` 6 个测试全部通过。
- [x] `mcp: {}`、`permissions: {}` 明确接管，缺失字段才回退旧格式。
  - 证据：统一接管、空对象、旧格式和默认来源测试全部通过。
- [x] 当前本地 `config.yaml` 已迁入 Context7 和两条只读权限规则，两份重复 `.local.yaml` 已移除。
  - 证据：真实 shaded JAR 启动时未显示旧格式迁移提示，并显示 Context7 启动确认与 `ask` 模式。
- [x] 无密钥 `config.example.yaml` 可直接加载。
  - 证据：仓库示例加载测试通过，来源均为 `UNIFIED`。

### MCP 与权限

- [x] 统一 stdio MCP 配置完成真实子进程握手、工具发现和注册。
  - 证据：`UnifiedConfigApplicationIT` 启动 `FakeStdioMcpServer`，连接 1 个 Server、注册 1 个工具。
- [x] Streamable HTTP、MCP Client、Manager、工具包装和生命周期回归通过。
  - 证据：全量 Maven 测试中的 MCP 测试全部通过。
- [x] 统一权限规则命中远程工具 allow，五种模式、危险命令和沙箱保持原行为。
  - 证据：`UnifiedConfigApplicationIT`、`PermissionModePolicyTest`、`PermissionCheckerTest` 与权限全套测试通过。
- [x] 单个 MCP Server 配置错误被隔离，权限配置错误整体安全失败。
  - 证据：`McpConfigLoaderTest` 与 `PermissionRuleLoaderTest` 聚焦测试通过。

### 敏感信息与错误处理

- [x] Provider、MCP `env` 和 Header 共用 `${NAME}` 展开规则。
  - 证据：占位符、Provider、MCP 环境解析测试通过。
- [x] 共享 `SecretRedactor` 可同时脱敏模型密钥与 MCP 展开秘密。
  - 证据：统一加载及真实装配测试中的秘密否定断言通过。
- [x] 仓库敏感值模式扫描无命中。
  - 证据：排除构建产物和本地运行数据后，`sk-` 长密钥模式命中数为 0。
- [x] `git diff --check` 无空白错误。

### 全量构建

- [x] JDK 21 全量构建成功。
  - 命令：`mvn clean package`
  - 结果：324 tests，0 failures，0 errors，3 skipped，`BUILD SUCCESS`。
  - 说明：3 个跳过项均为当前 Windows 环境无法创建测试符号链接，并非功能失败。
- [x] 真实统一配置集成测试成功。
  - 命令：`mvn -Dtest=UnifiedConfigApplicationIT test`
  - 结果：1 test，0 failures，0 errors。
- [x] shaded JAR 已生成。
  - 文件：`target/imiocode-0.2.0-SNAPSHOT-all.jar`

### 真实进程替代验收

- [x] 使用本地根 `config.yaml` 和测试环境变量启动真实 shaded JAR。
- [x] 观察到 Context7 启动确认；输入拒绝后显示连接 0 个 Server、注册 0 个工具。
- [x] 应用继续进入权限模式 `ask`，随后 `/exit` 正常退出，进程退出码为 0。
- [x] 输出没有旧格式迁移提示，也没有出现测试密钥。
- [x] 三次功能提交均未包含用户文件；最终工作树只剩用户原有的 `claude.md` 与 `hello.txt`，暂存区为空。

## 环境阻塞（2/53）

- [ ] E2：未执行真实 Provider + Context7 的完整聊天回合。
  - 原因：验收进程没有提供真实 `DEEPSEEK_API_KEY`；为避免使用或输出私人凭据，没有绕过环境变量配置，也没有发起真实模型请求。
  - 已有替代证据：真实本地 MCP 子进程完成握手、工具注册和统一权限 allow；原 MCP Agent Loop 集成测试继续通过。
- [ ] E7：当前系统无法执行 tmux 验收。
  - `tmux`：未安装。
  - WSL：启动失败，系统返回 `Wsl/Service/CreateInstance/E_ACCESSDENIED`。
  - 替代证据：真实 Windows shaded JAR 进程启动与退出成功，真实本地 stdio MCP 集成测试成功。

## 已知非阻塞警告

- Maven 检测到用户目录 `.m2/settings.xml` 根节点不是 `settings`；构建仍然成功。本次未修改工作区外的用户 Maven 配置。
- JLine 在重定向输入的真实 JAR 验收中退化为 dumb terminal；不影响启动、确认、权限模式和退出行为。
