# ch8 上下文管理验收报告

验收时间：2026-08-01（Asia/Shanghai）

## 结论

- 通过：134 / 138
- 未通过：0
- 环境阻塞 / 未授权：4
- Maven 全量回归：308 tests，0 failures，0 errors，3 skipped
- 真实 shaded JAR：3 tests，0 failures，0 errors，0 skipped

## 分区结果

| Checklist 分区 | 结果 | 主要证据 |
|---|---:|---|
| 配置与预算 | 8 / 8 | `ConfigLoaderTest`、`YamlConfigLoaderTest` |
| Token 近似估算 | 10 / 10 | `ApproximateTokenEstimatorTest`、`ContextManagerTest` |
| 单个大结果落盘 | 12 / 12 | `ToolResultSpillStoreTest`、`ToolResultOffloaderTest`、真实 JAR 大文件场景 |
| 累计旧结果瘦身 | 8 / 8 | `ToolResultOffloaderTest` |
| 落盘路径安全 | 10 / 12 | 路径、重名、原子写、Git/Glob/read_file 用例通过；链接权限项见阻塞 |
| 摘要请求与解析 | 15 / 15 | `ConversationSerializerTest`、`SummaryParserTest`、`ConversationSummarizerTest` |
| 自动压缩与熔断 | 12 / 12 | `ContextManagerTest`、`AutoCompactTrackingStateTest` |
| Agent Loop 与事务 | 12 / 12 | `AgentContextManagementTest`、`ConversationSessionTest` |
| CONTEXT_LIMIT 恢复 | 10 / 10 | `HttpErrorMapperTest`、`AgentContextManagementTest`、Provider 回归 |
| 手动 `/compact` 与终端 | 12 / 12 | `ConversationCompactTest`、`ConversationLoopTest`、真实 JAR 手动场景 |
| 兼容与回归 | 9 / 9 | 全量测试，权限真实进程测试使用隔离工作区 |
| 编译、测试与质量 | 9 / 9 | package、全量测试、`git diff --check`、`git check-ignore` |
| 端到端场景 | 7 / 9 | 自动压缩、落盘、手动压缩及失败/恢复场景通过；真实外部 Provider 与 tmux 未执行 |

## 关键证据

### 全量回归

```text
mvn test
Tests run: 308, Failures: 0, Errors: 0, Skipped: 3
BUILD SUCCESS
```

3 个跳过项均为 Windows 当前账户无法创建符号链接的既有安全测试：

- `McpConfigLoaderTest` 链接配置路径
- `WorkspacePathSandboxTest` 沙箱链接逃逸
- `WorkspacePolicyTest` 工作区链接逃逸

### 发布构建

```text
mvn package -DskipTests
BUILD SUCCESS
target/imiocode-0.2.0-SNAPSHOT-all.jar
```

### 真实 shaded JAR

```text
mvn -Dtest=ShadedJarContextIT test
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

实际验证了：

1. 临时 3,000 Token 窗口达到 50% 后，终端显示自动压缩开始与完成，压缩后的请求继续得到最终回复。
2. `read_file` 返回 7,000 个中文字符后，终端显示落盘事件，`.imiocode/tool-results/` 中生成一个 UTF-8 完整结果文件。
3. 长历史执行 `/compact` 后显示前后 Token，下一条请求中包含 `Compacted conversation summary` 和 `ORBIT-42`，模型正确回答原事实。

### Git 与目录隔离

```text
git diff --check
# exit 0

git check-ignore .imiocode/tool-results/example.txt
.imiocode/tool-results/example.txt
```

`WorkspaceWalkerTest` 同时确认常规遍历跳过 `.imiocode/tool-results/`，而 `read_file` 明确路径仍可读取。

## 环境阻塞 / 未授权

1. `tmux`：Windows 本机无原生 tmux；尝试启动 WSL 返回 `Wsl/Service/CreateInstance/E_ACCESSDENIED`。已按 checklist 使用真实 shaded JAR 进程替代，但 tmux 项不标记通过。
2. 真实外部 Provider：本轮未取得发送真实长对话/API 消耗的明确授权；所有端到端请求均发送到本地 `MockLlmServer`。
3. 工具结果目录符号链接逃逸：当前 Windows 账户缺少创建符号链接权限，无法执行实际链接用例；生产代码使用 `NOFOLLOW_LINKS`、`isSymbolicLink` 和 `BasicFileAttributes.isOther` 拒绝链接/重解析点。
4. 工具结果目录 Junction 逃逸：验收环境未授权创建 Junction；与符号链接相同，不标记为已执行。

## 已知非功能性提示

Maven 每次运行都会提示 `C:\Users\10355\.m2\settings.xml` 根元素为 `mirror` 而不是 `settings`。该用户级文件不在项目工作区，本轮未修改；警告未影响编译、测试或打包结果。
