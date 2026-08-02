# 统一配置文件 Plan

## 架构概览

本次重构保留 `config.yaml` 作为唯一主解析入口，但把当前“基础配置加载后，再由应用分别加载 MCP 和权限文件”的启动链改为一次性装配：

1. `YamlConfigLoader` 只读取并解析一次项目根目录 `config.yaml`，得到包含基础、MCP、权限三个区域的原始文档。
2. `ConfigLoader` 作为统一编排器，先解析基础 `AppConfig`，创建本次进程共用的 `SecretRedactor`，再分别解析统一 MCP 和权限配置。
3. 如果原始文档没有 `mcp` 或 `permissions` 字段，编排器调用现有旧格式加载逻辑，并产生迁移提示；字段存在但为空时视为用户明确配置了空集合，不再回退。
4. 所有规范化结果装入一个不可变的 `RuntimeConfig`，启动入口只消费这一份对象，不再自行创建其他配置加载器。
5. MCP Server 保持逐个解析和隔离错误；权限配置保持整体严格校验。统一配置解析失败时不回退旧文件。

推荐的统一 YAML 结构如下：

```yaml
provider: deepseek
model: deepseek-chat
connect-timeout-seconds: 10
request-timeout-seconds: 120
max-output-tokens: 4096

thinking:
  enabled: false
  mode: adaptive

agent:
  max-iterations: 20
  timeout-seconds: 600
  max-parallel-tools: 4

context:
  window-tokens: 64000
  auto-compact-threshold: 0.8

providers:
  deepseek:
    api-key: ${DEEPSEEK_API_KEY}
    base-url: https://api.deepseek.com

mcp:
  servers:
    context7:
      enabled: true
      transport: stdio
      command: npx
      args: ["-y", "@upstash/context7-mcp"]
      initialization-timeout-seconds: 120
      call-timeout-seconds: 120

permissions:
  mode: ask
  rules:
    - action: allow
      tool: mcp_context7__query-docs
    - action: allow
      tool: mcp_context7__resolve-library-id
```

## 核心数据结构

### `RuntimeConfig`

```java
public record RuntimeConfig(
        AppConfig app,
        McpConfigLoadResult mcp,
        PermissionSettings permissions,
        SecretRedactor redactor,
        ConfigSourceSummary sources,
        List<ConfigNotice> notices) {}
```

- `app`：Provider、模型、Thinking、Agent 和上下文配置。
- `mcp`：已完成环境展开的 MCP Server 与逐 Server 安全错误。
- `permissions`：最终权限模式和有序规则快照。
- `redactor`：注册了 Provider 密钥及 MCP 展开秘密的进程级脱敏器。
- `sources`：MCP、权限分别来自统一文件、旧格式或默认值。
- `notices`：启动后由终端输出的安全迁移提示。

### `ConfigDocument`

扩展现有根 YAML 映射，新增两个可空字段：

```java
record ConfigDocument(
        /* 现有基础字段 */,
        McpDocument mcp,
        PermissionDocument permissions,
        Map<String, ProviderConfig> providers) {}
```

`null` 表示对应功能域未声明，需要进入旧格式回退；非 `null`（包括空对象）表示统一配置已经接管该功能域。

`McpDocument` 的 `servers` 保留每个 Server 的原始 JSON 节点，使一个 Server 的类型错误不会导致整个 `config.yaml` 解析失败。每个节点之后由 MCP 解析器独立转换和校验。

`PermissionDocument` 包含 `mode` 与有序 `rules`；统一规则全部标记为项目级来源，使现有规则引擎和决策来源保持兼容。

### `ConfigSourceSummary`

```java
public record ConfigSourceSummary(
        ConfigSource app,
        ConfigSource mcp,
        ConfigSource permissions) {}

public enum ConfigSource {
    UNIFIED,
    LEGACY,
    DEFAULT
}
```

该结构只描述安全来源类型，不保存或输出包含用户目录、密钥或请求头的完整配置正文。

### `ConfigNotice`

```java
public record ConfigNotice(String code, String safeMessage) {}
```

当前使用 `legacy_mcp_config`、`legacy_permission_config` 两类迁移提示。代码用于测试和后续 UI 国际化，终端只显示 `safeMessage`。

### `EnvironmentPlaceholderResolver`

```java
public final class EnvironmentPlaceholderResolver {
    public String expand(
            String value,
            Map<String, String> environment,
            Consumer<String> secretRegistrar);
}
```

- 统一识别 `${NAME}` 占位符，支持一个值中包含多个占位符。
- 缺少变量时抛出只携带变量名的 `MissingEnvironmentVariableException`。
- Provider 密钥和 MCP `env` / `headers` 使用同一规则。
- 展开出的敏感值通过 `secretRegistrar` 注册到同一个 `SecretRedactor`。

## 核心接口

### `ConfigLoader`

```java
public RuntimeConfig loadAll(
        Path workspace,
        Path userHome,
        Map<String, String> environment);

public AppConfig load(
        Path workspace,
        Map<String, String> environment);
```

- `loadAll` 是生产启动的唯一入口，保证根配置只读取一次。
- 现有 `load` 保留给兼容调用方和聚焦基础配置的测试；它复用同一基础解析逻辑，但不加载 MCP 和权限。

### `McpConfigLoader`

```java
public McpConfigLoadResult loadUnified(
        ConfigDocument.McpDocument document,
        Path configPath,
        Map<String, String> environment,
        SecretRedactor redactor);

public McpConfigLoadResult loadLegacy(
        Path workspace,
        Path userHome,
        Map<String, String> environment,
        SecretRedactor redactor);
```

两条入口共用 Server 节点解析、Transport 校验、环境展开和错误隔离逻辑。原有 `load(...)` 可委托给 `loadLegacy(...)`，保持二进制源级兼容。

### `PermissionRuleLoader`

```java
public PermissionSettings loadUnified(
        ConfigDocument.PermissionDocument document);

public PermissionSettings loadLegacy(
        Path workspace,
        Path userHome);
```

统一配置中的规则按书写顺序进入 `projectRules`，`userRules` 和 `localRules` 为空；旧格式入口保留现有三层优先级。原有 `load(...)` 委托给旧格式入口。

## 模块设计

### YAML 文档解析模块

**职责：** 安全读取根 `config.yaml`，严格解析根字段和整体权限结构，并为 MCP 保留逐 Server 节点。

**对外接口：** `YamlConfigLoader.load(Path)`。

**依赖：** Jackson YAML。

**关键行为：**

- 使用 UTF-8，只允许普通文件。
- 未知根字段和未知权限字段直接报错。
- 错误包含字段路径及行列，不包含对应值。
- 只负责语法和结构解析，不读取旧配置，不解析业务默认值。

### 统一配置编排模块

**职责：** 将原始文档、环境变量与旧格式回退装配成 `RuntimeConfig`。

**对外接口：** `ConfigLoader.loadAll(...)`。

**依赖：** `YamlConfigLoader`、`McpConfigLoader`、`PermissionRuleLoader`、环境占位符解析器。

**关键行为：**

- 先构造 `AppConfig` 并校验基础参数。
- 环境变量直接覆盖保持现有优先级；从 YAML 取到的 Provider 敏感值再执行占位符展开。
- 基于最终 API Key 创建唯一 `SecretRedactor`，MCP 展开继续向其中注册秘密。
- 通过字段是否为 `null` 决定统一解析或旧格式回退，不因统一解析错误而回退。
- 统一收集来源摘要与迁移提示。

### MCP 配置模块

**职责：** 把统一或旧格式的 Server 配置转换为 `ResolvedMcpServerConfig`。

**对外接口：** `loadUnified(...)`、`loadLegacy(...)`。

**依赖：** MCP 环境解析、URI 和 Transport 校验。

**关键行为：**

- 两个来源复用同一 `parseServer` 路径，杜绝默认值和校验漂移。
- 统一模式不读取任何旧 MCP 文件。
- 单 Server 的未知字段、类型错误、传输冲突或缺失变量记为 `McpConfigError`，不影响其他 Server。
- 旧格式仍按本地、项目、用户既有顺序合并，不改变兼容语义。

### 权限配置模块

**职责：** 把统一或旧格式规则转换为 `PermissionSettings`。

**对外接口：** `loadUnified(...)`、`loadLegacy(...)`。

**依赖：** 权限模式解析、Glob 编译验证。

**关键行为：**

- 统一配置存在时只使用统一模式和规则。
- 统一规则按顺序放入项目规则层，不改变 `PermissionRuleEngine`。
- 任一规则无效时拒绝权限配置，避免部分规则静默失效。
- 未声明模式时使用 `ASK`。

### 启动集成与终端展示

**职责：** 使用一次统一加载结果初始化所有模块并展示安全摘要。

**对外接口：** `ImioCodeApplication.run()` 现有入口。

**依赖：** `RuntimeConfig`、`TerminalUi`、MCP Manager、权限检查器。

**关键行为：**

- 删除启动入口中对 `McpConfigLoader` 和 `PermissionRuleLoader` 的直接调用。
- 终端复用 `RuntimeConfig.redactor()`，保证配置展开秘密也能脱敏。
- Welcome 后输出旧格式迁移提示、MCP 连接统计与当前权限模式。
- 不打印配置正文、密钥、Header 或子进程环境变量。

### 文档与示例

**职责：** 将所有用户配置说明收口到根 `config.yaml`。

**内容：**

- 更新实际根 `config.yaml` 的 MCP 和权限段，并把当前本地 Context7 配置迁入其中。
- Provider API Key 改用环境变量占位符，避免继续在项目文件中保存明文。
- 新增统一配置说明与旧格式迁移对照。
- 更新启动、MCP、权限、上下文相关文档中的路径。
- 旧 `.imiocode/mcp.local.yaml` 和 `.imiocode/permissions.local.yaml` 保留用于兼容验证，但从当前工作配置移除，避免重复来源。

## 模块交互

```text
ImioCodeApplication
        |
        v
ConfigLoader.loadAll(workspace, userHome, env)
        |
        +--> YamlConfigLoader.load(config.yaml)  [唯一一次读取]
        |
        +--> build AppConfig --> SecretRedactor
        |
        +--> mcp 字段存在？
        |       +-- 是 --> McpConfigLoader.loadUnified
        |       `-- 否 --> McpConfigLoader.loadLegacy + ConfigNotice
        |
        +--> permissions 字段存在？
        |       +-- 是 --> PermissionRuleLoader.loadUnified
        |       `-- 否 --> PermissionRuleLoader.loadLegacy + ConfigNotice
        |
        `--> RuntimeConfig
                 |
                 +--> Terminal / LlmClient / Agent / ContextManager
                 +--> PermissionChecker
                 `--> McpManager
```

环境敏感值处理链：

```text
环境变量覆盖或 ${NAME} 展开
        --> 注册到共享 SecretRedactor
        --> 规范化配置
        --> 日志、终端、工具和 MCP 共用脱敏器
```

## 文件组织

```text
src/main/java/io/imiocode/
├── ImioCodeApplication.java                    — 改为消费 RuntimeConfig
├── config/
│   ├── ConfigDocument.java                     — 增加 mcp、permissions 文档结构
│   ├── ConfigLoader.java                       — 统一加载与回退编排
│   ├── RuntimeConfig.java                      — 全部运行期配置快照
│   ├── ConfigSource.java                       — 来源枚举
│   ├── ConfigSourceSummary.java                — 各域来源摘要
│   ├── ConfigNotice.java                       — 安全迁移提示
│   ├── EnvironmentPlaceholderResolver.java     — 通用 ${NAME} 展开
│   └── MissingEnvironmentVariableException.java
├── mcp/config/
│   ├── McpConfigDocument.java                  — 复用于统一 MCP 节点
│   ├── McpConfigLoader.java                    — 统一/旧格式双入口
│   └── McpEnvironmentResolver.java             — 复用通用占位符解析
└── permission/rule/
    ├── PermissionConfigDocument.java           — 复用于统一权限节点
    └── PermissionRuleLoader.java               — 统一/旧格式双入口

src/test/java/io/imiocode/
├── config/
│   ├── ConfigLoaderTest.java
│   ├── UnifiedConfigLoaderTest.java
│   └── EnvironmentPlaceholderResolverTest.java
├── mcp/config/McpConfigLoaderTest.java
├── permission/rule/PermissionRuleLoaderTest.java
└── UnifiedConfigApplicationIT.java

docs/config-unification/
├── spec.md
├── plan.md
├── task.md
├── checklist.md
└── migration.md

config.yaml                                    — 当前项目的统一配置示例
```

## 技术决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 统一入口对象 | `RuntimeConfig` 聚合现有领域对象 | 不推翻稳定的 `AppConfig`、`PermissionSettings`、`McpConfigLoadResult`，降低回归风险 |
| YAML 命名 | 根级基础字段保持不变，新增 `mcp.servers` 与 `permissions` | 对现有文件零破坏，领域边界清晰 |
| 接管判定 | 字段存在即完整接管；字段缺失才回退 | 避免统一配置与隐藏旧文件混合，空对象也能明确禁用模块 |
| 旧格式兼容 | 保留专用 legacy 入口，不参与统一域合并 | 保持旧行为，同时让新行为可预测 |
| MCP 错误隔离 | 根文档保存逐 Server 原始节点 | 单 Server 类型错误不会拖垮其他 Server，满足现有部分成功语义 |
| 权限错误处理 | 整体严格失败 | 部分加载权限规则可能造成错误放行，不符合安全失败原则 |
| 统一权限规则层 | 映射到 `PROJECT` 层 | 根 `config.yaml` 是项目配置，且无需修改现有规则引擎与决策来源 |
| 敏感值语法 | 复用 `${NAME}` | 与现有 MCP 配置一致，学习成本低，可覆盖 Provider 和 Header |
| 脱敏器生命周期 | 配置加载时创建并随 `RuntimeConfig` 返回 | 确保 Provider 与 MCP 展开的秘密使用同一实例，不产生脱敏盲区 |
| 迁移提示 | 结构化 notice，由 UI 输出 | 配置层不依赖终端，测试可稳定断言提示代码 |
| 当前本地旧文件 | 迁移内容后移除本地工作副本 | 用户看到的项目配置真正只有一份，同时通过测试夹具继续验证兼容性 |
