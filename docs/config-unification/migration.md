# 统一配置迁移指南

ImioCode 现在把基础模型、Provider、Thinking、Agent、上下文、MCP 和权限配置统一放在项目根目录 `config.yaml`。

## 快速开始

复制仓库中的无密钥示例：

```powershell
Copy-Item config.example.yaml config.yaml
```

然后设置当前 Provider 的密钥。配置文件中的 `${NAME}` 会从 ImioCode 启动时的环境变量展开。

### PowerShell

```powershell
$env:DEEPSEEK_API_KEY="你的密钥"
java -jar target/imiocode-0.2.0-SNAPSHOT-all.jar
```

### CMD

```bat
set DEEPSEEK_API_KEY=你的密钥
java -jar target/imiocode-0.2.0-SNAPSHOT-all.jar
```

### Bash

```bash
export DEEPSEEK_API_KEY="你的密钥"
java -jar target/imiocode-0.2.0-SNAPSHOT-all.jar
```

## 完整结构

```yaml
provider: deepseek
model: deepseek-chat
connect-timeout-seconds: 10
request-timeout-seconds: 120
max-output-tokens: 4096

thinking:
  enabled: false
  mode: auto
  budget-tokens: 1024
  effort: high
  summary: auto

agent:
  max-iterations: 20
  timeout-seconds: 600
  max-parallel-tools: 4

context:
  window-tokens: 64000
  auto-compact-threshold: 0.80

ui:
  verbosity: compact

providers:
  openai:
    api-key: ${OPENAI_API_KEY}
    base-url: https://api.openai.com
  anthropic:
    api-key: ${ANTHROPIC_API_KEY}
    base-url: https://api.anthropic.com
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

只需要保留当前项目实际使用的区域。`ui` 缺失时默认使用 `compact`；也可设为 `verbose` 查看状态、Thinking、Usage 和完整工具生命周期。启动面板始终使用原响应式完整布局，不受该值影响。运行中可用 `/verbose` 和 `/compact-ui` 临时切换，这不会重绘启动面板、修改配置或会话历史。精简模式仍完整显示最终回答、权限确认和错误。`mcp: {}` 表示明确不启用 MCP，`permissions: {}` 表示使用安全默认模式 `ask` 且没有自定义规则。

## 从旧 MCP 文件迁移

旧格式：

```yaml
servers:
  context7:
    transport: stdio
    command: npx
    args: ["-y", "@upstash/context7-mcp"]
```

迁移时把原内容整体放到根配置的 `mcp` 下：

```yaml
mcp:
  servers:
    context7:
      transport: stdio
      command: npx
      args: ["-y", "@upstash/context7-mcp"]
```

## 从旧权限文件迁移

旧格式：

```yaml
mode: ask
rules:
  - action: allow
    tool: mcp_context7__query-docs
```

迁移时把原内容整体放到根配置的 `permissions` 下：

```yaml
permissions:
  mode: ask
  rules:
    - action: allow
      tool: mcp_context7__query-docs
```

## 接管与回退规则

- `config.yaml` 出现 `mcp` 时，MCP 完全由该区域接管，不再混入任何旧 MCP 文件。
- `config.yaml` 出现 `permissions` 时，权限完全由该区域接管，不再混入任何旧权限文件。
- 对应区域完全缺失时，ImioCode 才按旧优先级读取旧格式，并在启动时显示一次迁移提示。
- 统一区域存在但内容无效时会明确报错，不会悄悄回退到旧文件。

建议迁移并验证成功后删除项目中的重复旧配置，避免维护两份来源。用户目录中的旧文件不会被程序自动修改。

## 敏感值

- 推荐用 `${NAME}` 引用 API Key、Token 和认证 Header。
- 启动环境变量直接覆盖基础配置的行为保持不变。
- 缺少占位符变量时，错误只显示变量名和安全配置位置。
- 不要把真实凭据写入 `config.example.yaml`、文档或 Git 提交。
