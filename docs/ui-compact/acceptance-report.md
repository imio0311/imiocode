# 精简终端 UI 验收报告

验收日期：2026-08-02

## 结论

精简终端 UI 已完成并按使用反馈恢复原启动面板。启动时始终使用原 FULL/COMPACT/PLAIN 响应式布局；进入对话后默认模式为 `compact`，运行中可用 `/verbose` 与 `/compact-ui` 切换。最终回答、权限确认、失败与安全警告始终可见；Thinking、Usage、状态跳转和工具中间态只在 `verbose` 显示。

## 自动化验收

- 配置：缺失 `ui` 时默认为 compact；compact/verbose 大小写不敏感；非法值明确指向 `ui.verbosity`。
- 布局：启动面板只依赖 FULL、COMPACT、PLAIN 终端能力，两种详细度生成相同启动面板；对话区域继续覆盖 compact/verbose，20、40、60、80、100、200 列均未越界。
- 工具：compact 只显示成功或失败完成行；正文、完整输出、风险等级、调用 ID 和 MCP 参数不显示；密钥经过脱敏。
- 安全：权限确认、权限结果、MCP 拒绝/失败、上下文结果、重试和 Agent 停止保持可见。
- 命令：`/verbose`、`/compact-ui` 不调用 LLM、不进入会话历史、不改变 Plan/Do 模式。
- 回归：Provider、Agent、工具、权限、MCP、Prompt 和上下文测试全部通过。

最终命令：

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
mvn clean package
```

实际结果：`BUILD SUCCESS`；343 tests，0 failures，0 errors，3 skipped。三个跳过项均为现有平台条件测试。生成的 shaded JAR 为 `target/imiocode-0.2.0-SNAPSHOT-all.jar`。

## 端到端场景

- E1 默认 compact：真实 Java 子进程先显示原完整启动信息，再连接本地可控 LLM，完成 Thinking → ReadFile → Usage → 最终回答。对话区只出现一条 `[ok] Read pom.xml` 和完整回答，不出现 Thinking、Usage、LOW 或工具中间态。
- E2 切换 verbose：真实 Java 子进程先输入 `/verbose`，启动面板不重绘，下一轮恢复 Thinking、Usage、queued、running、风险等级和详细完成行。
- E3 切回 compact：同一进程输入 `/compact-ui` 后显示精简模式确认且不重绘启动面板；两个 UI 命令没有产生额外模型轮次。
- E4 失败可见：失败工具完整生命周期只显示一条 `[fail]`，仅包含脱敏后的首行错误与耗时。
- E5 权限可见：compact 和 verbose 均完整显示工具、风险、目标、原因和三个选择；拒绝结果可见且权限端到端测试通过。
- E6 窄终端：20 列起的多组宽度测试通过，启动、状态和工具摘要均截断在终端宽度内。
- E7 tmux：Windows 环境未安装原生 tmux，WSL 启动返回 `Wsl/Service/CreateInstance/E_ACCESSDENIED`。按 checklist 回退方案，使用真实 shaded JAR、真实 Java 子进程和本地可控 LLM 完成等价验收。另以本地 `config.yaml` 启动真实 JAR，PLAIN 回退实际显示产品版本、Provider/模型、目录和 `Ready`，拒绝 MCP 后退出码为 0；FULL 的 Logo 与边框由内存终端布局测试覆盖。

## 非阻塞警告

- Maven 提示用户目录的 `settings.xml` 根元素不是 `settings`；本次依赖已可用，未影响构建。
- Shade 插件报告依赖间存在重复的许可证、服务声明和模块描述资源；这是现有打包警告，shaded JAR 可正常启动。

## Git 隔离

本次提交只暂存 UI 功能、测试、示例配置和文档。用户已有的 `claude.md`、`hello.txt` 以及被忽略的本地 `config.yaml` 明确排除；变更差异的常见真实凭据模式扫描为 0 命中。
