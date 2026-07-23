# ImioCode 第三章：工具系统 Plan

## 架构概览

第三章采用“统一领域模型 + 厂商协议适配”的分层结构。

### 工具协议层

定义与模型厂商无关的工具描述、风险等级、工具调用和工具结果。OpenAI、Anthropic、DeepSeek 都使用这套内部模型，厂商原生 JSON 不进入工具实现。

### 工具运行层

提供通用工具接口、基础实现、参数校验、异常转换、耗时记录和输出截断。六个核心工具共享工作区路径策略与敏感路径策略，Bash 单独负责跨平台命令进程管理。

### 注册与执行层

注册中心管理工具注册、查询、启用和禁用，并导出当前已启用工具。串行执行器按调用顺序执行工具、关联结果、发布生命周期事件，并持有当前活动命令的取消入口。

### 对话编排层

将现有纯文本消息扩展为由文本、工具调用和工具结果组成的统一消息内容。会话负责：

- 发起第一次模型请求；
- 收集文本和工具调用；
- 串行执行首批工具；
- 一次性回传全部结果；
- 获取最终模型响应；
- 完整成功后原子提交会话历史。

第二次模型请求仍携带工具定义，以便识别模型再次请求工具的情况，但执行器已经关闭本轮执行入口；新的调用只触发单轮限制提示，不会执行。

### 厂商适配层

三个客户端分别负责请求 JSON 映射、工具定义映射、工具结果映射和流式事件解析。每个流式解析器为不同调用维护独立参数缓冲区，在完成事件到达后统一解析和校验 JSON。

### 终端展示层

终端只消费文本增量和工具生命周期事件，展示等待、运行、成功、失败及摘要。展示字符串与发送给模型的原始参数、结果和历史完全分离。

### 应用装配层

启动时确定规范化工作区，创建安全路径策略、六个工具、注册中心、串行执行器、模型客户端、会话与终端，并把取消链路连接到 HTTP 请求和活动命令。

依赖方向保持单向：

```text
应用装配
   ├── 终端展示
   └── 对话编排
         ├── LLM 抽象 ← 三个厂商适配器
         └── 工具执行器 → 注册中心 → 六个核心工具
                                      └── 工作区安全策略
```

工具层不依赖模型厂商、会话或终端；厂商适配器不直接执行工具；终端不参与业务数据构造，从而避免循环依赖和界面内容污染模型上下文。

## 核心数据结构

### ToolRisk

```java
enum ToolRisk {
    LOW,
    MEDIUM,
    HIGH
}
```

### ToolDefinition

```java
record ToolDefinition(
    String name,
    String description,
    ObjectNode inputSchema,
    ToolRisk risk
)
```

`inputSchema` 使用 JSON Schema 对象，构造时深拷贝，避免注册后被外部修改。

### ToolCall

```java
record ToolCall(
    String id,
    String name,
    ObjectNode arguments
)
```

只有调用标识、工具名称和参数 JSON 都通过校验后，才能创建完整调用。厂商流中的碎片不会直接进入执行器。

### ToolResult

```java
record ToolResult(
    boolean success,
    String output,
    String error,
    boolean truncated,
    Duration duration,
    Integer exitCode
)
```

- `output` 和 `error` 始终是安全、非空引用的字符串。
- `exitCode` 只对 Bash 有意义，其他工具为 `null`。
- 原始异常、堆栈和敏感内容不进入结果。
- 提供成功、失败、超时和中断结果的静态构造入口。

### Tool 与 BaseTool

```java
interface Tool {
    ToolDefinition definition();

    ToolResult execute(ObjectNode arguments);

    default void cancel() {
    }
}

abstract class BaseTool implements Tool {
    @Override
    public final ToolResult execute(ObjectNode arguments);

    protected abstract ToolResult executeValidated(
        ObjectNode arguments
    ) throws Exception;
}
```

`BaseTool` 的最终执行入口统一负责：

- 确认参数是 JSON 对象；
- 调用子类参数校验和实际执行；
- 记录耗时；
- 转换异常；
- 限制输出；
- 使用 `SecretRedactor` 脱敏；
- 保证返回 `ToolResult`；
- 禁止原始异常逃逸到会话层。

Bash 覆盖 `cancel()`，终止当前活动进程；其他工具使用默认空实现。

### ToolRegistry

```java
final class ToolRegistry {
    void register(Tool tool);

    void enable(String name);

    void disable(String name);

    Optional<Tool> findEnabled(String name);

    List<ToolDefinition> enabledDefinitions();

    <T> List<T> exportEnabled(
        ToolDefinitionEncoder<T> encoder
    );
}

@FunctionalInterface
interface ToolDefinitionEncoder<T> {
    T encode(ToolDefinition definition);
}
```

`exportEnabled` 让三个厂商适配器提供各自编码函数，注册中心负责筛选和稳定排序。这样满足集中导出 API 格式的要求，同时避免工具层依赖任何厂商。

### ToolExecution

```java
record ToolExecution(
    ToolCall call,
    ToolResult result
)

enum ToolExecutionState {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED
}

record ToolExecutionEvent(
    ToolExecutionState state,
    ToolCall call,
    ToolResult result
)

@FunctionalInterface
interface ToolExecutionListener {
    void onToolEvent(ToolExecutionEvent event);
}

final class ToolExecutor implements AutoCloseable {
    List<ToolExecution> executeAll(
        List<ToolCall> calls,
        ToolExecutionListener listener
    );

    void cancel();

    @Override
    void close();
}
```

执行器使用调用列表顺序同步执行。它通过原子引用记录当前活动工具，因而终端信号处理线程可以取消 Bash；取消后不会启动后续工具。

未知或已禁用工具由执行器转换为失败结果，并继续处理剩余调用。

### ToolLimits

```java
record ToolLimits(
    long maxReadBytes,
    int maxReadLines,
    long maxWriteBytes,
    int maxScannedPaths,
    int maxGlobResults,
    int maxGrepResults,
    int maxGrepLineChars,
    long maxResultBytes,
    long maxCommandStdoutBytes,
    long maxCommandStderrBytes,
    Duration commandTimeout,
    Duration processTerminationGrace
)
```

### WorkspacePolicy

```java
final class WorkspacePolicy {
    Path resolveExistingFile(String input);

    Path resolveWritableFile(String input);

    boolean isAllowedDiscoveredPath(Path path);

    Path workspace();
}
```

`WorkspacePolicy` 集中完成：

- 拒绝绝对路径和 `..` 跳转；
- 规范化路径并检查工作区前缀；
- 逐级拒绝符号链接和 Windows 重解析点；
- 拒绝敏感路径；
- 在实际访问前重新校验；
- 向 Glob 和 Grep 提供统一过滤判断。

### 统一消息模型

```java
sealed interface MessagePart
    permits TextPart, ToolCallPart, ToolResultPart {
}

record TextPart(String text) implements MessagePart {
}

record ToolCallPart(ToolCall call) implements MessagePart {
}

record ToolResultPart(
    String callId,
    String toolName,
    ToolResult result
) implements MessagePart {
}

enum MessageRole {
    USER,
    ASSISTANT,
    TOOL
}

record ChatMessage(
    MessageRole role,
    List<MessagePart> parts
)
```

角色约束：

- `USER` 只包含用户文本；
- `ASSISTANT` 可包含文本和工具调用；
- `TOOL` 只包含工具结果；
- 厂商适配器负责将内部 `TOOL` 角色映射为各厂商要求的原生结构。

保留纯文本便捷构造入口，使第二章调用和测试不必大量改写。

### ChatRequest 与 ChatResponse

```java
record ChatRequest(
    List<ChatMessage> messages
)

record ChatResponse(
    ChatMessage message
) {
    String text();

    List<ToolCall> toolCalls();

    boolean hasToolCalls();
}
```

纯工具响应允许没有文本，但响应至少包含一个消息部分。纯文本响应继续支持原有便捷构造方式。

工具注册中心注入模型客户端，因此每次请求都从注册中心导出当前已启用工具，不把厂商 JSON 放进 `ChatRequest`。

### ToolCallAssembler

```java
final class ToolCallAssembler {
    void append(
        int position,
        String idFragment,
        String nameFragment,
        String argumentsFragment
    );

    List<ToolCall> finish() throws LlmException;
}
```

- 同一位置的碎片严格按到达顺序追加；
- 不同位置拥有独立缓冲区；
- 输出顺序按首次出现位置确定；
- `finish()` 校验标识、名称和参数 JSON；
- 参数必须解析为 JSON 对象；
- 校验失败产生安全的协议异常，不返回半成品调用。

现有 `StreamListener` 继续只负责文本增量并保持函数式接口，不把 JSON 碎片暴露给终端。

### 会话事件与错误

```java
interface ConversationListener {
    void onResponseStarted();

    void onTextDelta(String text);

    void onResponseCompleted();

    void onToolEvent(ToolExecutionEvent event);
}

final class ConversationException extends Exception {
    String safeMessage();

    boolean recoverable();

    boolean interrupted();

    boolean toolsExecuted();
}
```

`ConversationException.toolsExecuted()` 用于区分普通模型失败和“工具已经产生副作用，但最终回复失败”，确保终端给出准确提示。

会话保留仅接收文本监听器的便捷重载，用于兼容纯文本调用和现有测试。

## 模块设计

### 工具框架与注册中心

`BaseTool` 负责所有工具共有的参数对象校验、异常脱敏、耗时记录和结果构造。各工具只实现自己的参数规则与业务操作。

`ToolRegistry` 使用工具名称作为唯一键：

- 注册时默认启用；
- 重名注册直接拒绝；
- 查询和执行均区分大小写；
- 导出定义时按工具名称稳定排序；
- 禁用工具仍保留注册信息，但不向模型公开，也不能执行。

`ToolExecutor` 只同步串行执行。每个调用依次产生 `QUEUED → RUNNING → SUCCEEDED/FAILED` 事件。工具失败转换为结果，不中断列表；用户取消则停止当前工具并放弃剩余列表。

### 工作区安全模块

`WorkspacePolicy` 在启动时保存工作区的绝对规范路径，并为五个文件类工具提供唯一的路径入口。

处理流程：

1. 拒绝空路径、绝对路径和包含父目录跳转的路径。
2. 将 `/` 和平台分隔符转换为工作区相对路径。
3. 拒绝 `config.yaml`、`.env`、`.env.*` 和 `.git/**`。
4. 检查每一级已存在路径，不允许符号链接或 Windows 重解析点。
5. 确认规范化目标仍位于工作区。
6. 在实际打开、移动或遍历前再次校验。
7. 遍历目录时不跟随链接，并再次过滤每个发现的路径。

另设 `WorkspaceWalker`，为 Glob 和 Grep 提供按相对路径字典序遍历、敏感目录跳过、扫描数量限制和取消检查。它不依赖外部搜索命令。

### ReadFile

API 名称为 `read_file`。

参数：

- `path`：必填，工作区相对路径；
- `start_line`：可选，从 1 开始，默认 1；
- `end_line`：可选，包含该行，默认读到文件末尾。

行为：

- 仅接受普通 UTF-8 文本文件；
- 无效 UTF-8 或检测到二进制内容时失败；
- `start_line <= 0`、`end_line < start_line` 或起始行超过文件末尾时失败；
- 结束行超过文件末尾时读取到实际末尾；
- 输出带行号；
- 达到字节或行数限制时停止，并附加截断标记。

### WriteFile

API 名称为 `write_file`。

参数：

- `path`：必填，工作区相对路径；
- `content`：必填，完整 UTF-8 文件内容。

行为：

- 内容超过最大写入字节数时，在写入前失败，不做截断写入；
- 父目录必须已经存在；
- 目标存在时必须是普通文件且不是链接；
- 在目标目录写入临时文件，校验后再原子替换目标；
- 文件系统不支持原子移动时，重新校验后执行普通替换；
- 成功结果返回创建或覆盖状态及写入字节数；
- 失败时清理临时文件，不保留半写文件。

### EditFile

API 名称为 `edit_file`。

参数：

- `path`：必填；
- `old_text`：必填且不能为空；
- `new_text`：必填，可以为空。

行为：

- 在读取限制内完整读取 UTF-8 文本；
- 精确统计 `old_text` 的非重叠出现次数；
- 出现零次或多次时不写入；
- 恰好一次时构造完整新内容；
- 新内容超过写入限制时失败；
- 使用与 WriteFile 相同的临时文件和替换流程；
- 不执行模糊匹配、补丁应用或自动格式化。

### Bash

API 名称为 `bash`，风险等级为 `HIGH`。

参数：

- `command`：必填且不能为空。

启动方式：

- Windows：`powershell.exe -NoLogo -NoProfile -NonInteractive -Command <command>`；
- Linux、macOS：`/bin/bash -lc <command>`；
- 工作目录固定为工作区；
- 每次调用创建独立进程。

标准输出和标准错误由两个独立读取任务持续排空，避免进程阻塞；只在固定容量内保存内容，超出后继续排空但不继续占用内存。

超时或取消时：

1. 阻止后续工具启动。
2. 终止全部可见子孙进程。
3. 终止主进程。
4. 等待短暂退出窗口。
5. 对仍存活的进程强制终止。
6. 关闭输出任务和相关资源。

结果返回退出码、标准输出、标准错误、超时或中断状态及截断标记。应用不会把其他业务数据拼进模型给出的命令。

### Glob

API 名称为 `glob`。

参数：

- `pattern`：必填，使用 `/` 作为统一路径分隔符。

匹配规则：

- `*` 匹配单个路径段内的任意字符；
- `**` 跨目录匹配；
- `?` 匹配单个非分隔字符；
- 返回普通文件的工作区相对路径；
- 路径统一使用 `/`；
- 结果按字典序排列；
- 达到扫描或结果限制时停止并标记截断。

使用内部匹配器保证三个操作系统行为一致，不使用系统安装的 `glob`、`find` 或 `rg`。

### Grep

API 名称为 `grep`。

参数：

- `pattern`：必填，Java 正则表达式；
- `path`：可选，限定到某个文件或子目录，默认工作区根目录。

行为：

- 编译正则失败时返回参数错误；
- 按稳定顺序递归扫描普通文件；
- 跳过敏感路径、链接、二进制文件和无效 UTF-8 文件；
- 流式逐行读取，不把所有文件载入内存；
- 输出相对路径、从 1 开始的行号和匹配行；
- 超长行只保存限定长度并标记；
- 达到扫描、结果或总输出限制时停止并标记截断。

### 统一消息和模型协议

三个客户端构造时注入同一个 `ToolRegistry`。每次请求动态导出已启用工具。

工具定义映射：

| 内部定义 | OpenAI Responses | Anthropic Messages | DeepSeek Chat Completions |
|---|---|---|---|
| 工具名称 | `name` | `name` | `function.name` |
| 工具说明 | `description` | `description` | `function.description` |
| 参数约束 | `parameters` | `input_schema` | `function.parameters` |
| 外层类型 | `type: function` | 无额外外层 | `type: function` |

消息映射：

- OpenAI：工具调用映射为 `function_call`，结果映射为 `function_call_output`。
- Anthropic：工具调用映射为 `tool_use` 内容块，结果映射为用户消息中的 `tool_result` 内容块。
- DeepSeek：工具调用映射为助手消息的 `tool_calls`，每个结果映射为独立的 `tool` 角色消息。

统一工具结果先编码成安全 JSON，包含成功状态、输出、错误、截断、耗时和 Bash 退出码，再放入厂商结果字段。

### 流式解析

- OpenAI 处理输出项开始、函数参数增量、输出项完成和响应完成事件。
- Anthropic 处理 `tool_use` 内容块开始、`input_json_delta` 参数碎片和消息完成事件。
- DeepSeek 处理 `delta.tool_calls` 中按索引出现的标识、名称和参数碎片，以及结束原因和 `[DONE]`。
- 纯工具响应允许没有文本。
- 混合响应保留文本及工具调用。
- 正常完成事件到达后才调用统一组装器生成 `ToolCall`。
- 缺少完成事件或 JSON 校验失败时，整次响应作为协议错误处理，不执行其中任何工具。

### 会话编排

一次用户输入按以下规则处理：

1. 从正式历史创建临时请求，并追加当前用户消息。
2. 发起第一次流式请求。
3. 若只有文本，提交用户消息和助手消息。
4. 若包含工具调用，保留第一次助手消息，但暂不提交历史。
5. 串行执行全部工具，生成一个内部工具结果消息。
6. 使用临时消息序列发起一次最终请求。
7. 最终响应不再包含工具调用时，原子提交整个轮次。
8. 最终响应再次请求工具时，不执行调用，显示单轮限制，整个未完成轮次不进入正式历史。
9. 最终请求失败时，报告“工具已执行，但回复未完成”，不提交历史，也不声称回滚。

只要任一工具进入 `RUNNING`，后续异常就将 `toolsExecuted` 标记为真。

### 终端 UI

新增工具等待和运行状态，并扩展终端接口接收 `ToolExecutionEvent`。

输入摘要规则：

- 文件工具显示相对路径和行范围；
- WriteFile、EditFile 只显示内容字符数，不打印完整内容；
- Bash 显示经过长度限制的命令；
- Glob、Grep 显示模式和搜索范围。

结果摘要规则：

- 读取显示行数和字节数；
- 写入、编辑显示处理字节数；
- Bash 显示退出码和有限输出预览；
- Glob、Grep 显示匹配数量；
- 失败显示安全错误；
- 截断统一显示明确标记。

工具事件到来前先结束已打开的助手文本行；工具完成后，最终模型文本另起一条助手响应。ANSI 和布局文字只在终端内部产生。

### 应用装配

启动入口按以下顺序构造依赖：

1. 获取并规范化当前工作区。
2. 加载配置。
3. 创建工作区策略、资源限制和密钥脱敏器。
4. 注册并默认启用六个工具。
5. 创建串行执行器。
6. 创建持有注册中心的模型客户端。
7. 创建会话和终端循环。
8. 将 Ctrl+C 连接到会话关闭、HTTP 中断和工具取消。

关闭操作保持幂等，应用入口重复关闭客户端或执行器也不会报错。

## 模块交互

### 启动流程

```text
应用入口
  → 加载配置
  → 创建工作区策略与限制
  → 注册六个工具
  → 创建模型客户端
  → 创建执行器、会话和终端
  → 等待用户输入
```

注册中心必须在模型客户端之前创建，确保首次请求就能携带工具定义。

### 纯文本轮次

1. 终端读取有效用户输入并进入 `THINKING`。
2. 会话复制正式历史，将当前用户消息加入临时消息列表。
3. 客户端导出已启用工具并发起流式请求。
4. 首个文本增量到达后，终端进入 `STREAMING`。
5. 响应完成且不包含工具调用时，会话一次性提交用户消息和助手消息。
6. 终端结束助手文本并返回 `READY`。

该路径保持第二章原有行为。

### 单轮工具调用

```text
用户输入
  → 第一次模型请求
  → 流式文本 + 工具调用
  → 按顺序串行执行全部工具
  → 一次性构造全部工具结果
  → 第二次模型请求
  → 最终流式文本
  → 原子提交完整轮次
```

第一次助手文本、工具调用、工具结果和最终助手文本在提交前都只存在于临时轮次中。

### 多工具与失败处理

```text
调用 1 → 结果 1
调用 2 → 失败结果 2
调用 3 → 结果 3
              ↓
一次性回传 [结果 1、失败结果 2、结果 3]
```

普通工具失败不会抛出到会话层，而是形成与调用标识关联的失败结果。只有用户中断、执行器关闭或无法维持执行流程的内部错误才终止剩余调用。

### 第二次工具请求

第二次模型请求仍携带已启用工具定义，以便准确识别再次请求工具的行为。

若最终响应包含新的工具调用：

1. 已流式返回的文本可以保留在终端。
2. 新调用不进入执行器。
3. 终端显示“本章只支持一轮工具执行”。
4. 本轮不提交正式历史。
5. 已执行工具的副作用不回滚。
6. 终端说明工具已经执行，但完整回复未完成。

### 会话历史事务

| 响应情况 | 正式历史变化 |
|---|---|
| 第一次响应为完整纯文本 | 提交用户消息、助手消息 |
| 工具执行并获得有效最终文本 | 提交用户消息、首次助手消息、工具结果消息、最终助手消息 |
| 第一次模型请求失败 | 不提交 |
| 工具执行前被中断 | 不提交 |
| 工具已经执行，最终请求失败 | 不提交，不回滚副作用 |
| 最终响应再次请求工具 | 不提交，不执行第二批工具 |
| 流式响应缺少正常完成事件 | 不提交 |

历史提交在同步块内一次完成，其他读取方只能看到提交前或提交后的完整快照。

### 厂商协议边界

```text
统一 ChatRequest
       ↓
厂商请求编码
       ↓
HTTP + SSE
       ↓
厂商事件解析
       ↓
文本增量 + ToolCallAssembler
       ↓
统一 ChatResponse
```

厂商适配器只负责协议转换：

- 不查询具体工具实现；
- 不执行工具；
- 不修改会话历史；
- 不生成终端装饰；
- 不决定是否允许第二轮工具执行。

### 终端状态流转

```text
READY
  → THINKING
  → STREAMING（首次响应含文本时）
  → TOOL_WAITING
  → TOOL_RUNNING
  → TOOL_WAITING（还有待执行工具时）
  → THINKING（等待最终模型回复）
  → STREAMING
  → READY
```

工具失败但流程可继续时，显示失败事件后进入下一个工具或最终请求，不把整个终端状态切换为不可恢复错误。

模型请求失败时进入 `ERROR`；用户中断直接进入关闭流程。

### Ctrl+C 中断链路

```text
Ctrl+C
  → ConversationLoop.requestStop()
  → ConversationSession.close()
      ├── ToolExecutor.cancel()
      │     └── 活动 Tool.cancel()
      └── LlmClient.close()
  → TerminalUi.close()
```

中断标志一旦设置：

- 不启动待执行工具；
- 不发起工具结果回传请求；
- 不重新显示输入框；
- 重复中断或重复关闭保持幂等。

## 文件组织

```text
docs/ch3/
├── spec.md
├── plan.md
├── task.md
└── checklist.md

src/main/java/io/imiocode/
├── ImioCodeApplication.java
│
├── conversation/
│   ├── MessageRole.java
│   ├── MessagePart.java
│   ├── TextPart.java
│   ├── ToolCallPart.java
│   ├── ToolResultPart.java
│   ├── ChatMessage.java
│   ├── ChatRequest.java
│   ├── ChatResponse.java
│   ├── ConversationListener.java
│   ├── ConversationException.java
│   ├── ConversationSession.java
│   └── ConversationLoop.java
│
├── tool/
│   ├── Tool.java
│   ├── BaseTool.java
│   ├── ToolRisk.java
│   ├── ToolDefinition.java
│   ├── ToolCall.java
│   ├── ToolResult.java
│   ├── ToolLimits.java
│   ├── SecretRedactor.java
│   ├── ToolDefinitionEncoder.java
│   ├── ToolRegistry.java
│   ├── ToolExecution.java
│   ├── ToolExecutionState.java
│   ├── ToolExecutionEvent.java
│   ├── ToolExecutionListener.java
│   ├── ToolExecutor.java
│   │
│   ├── workspace/
│   │   ├── WorkspacePolicy.java
│   │   ├── WorkspaceWalker.java
│   │   ├── Utf8TextFile.java
│   │   └── AtomicFileWriter.java
│   │
│   └── core/
│       ├── ReadFileTool.java
│       ├── WriteFileTool.java
│       ├── EditFileTool.java
│       ├── BashTool.java
│       ├── GlobPattern.java
│       ├── GlobTool.java
│       └── GrepTool.java
│
├── llm/
│   ├── LlmClient.java
│   ├── LlmClientFactory.java
│   ├── StreamListener.java
│   ├── ToolCallAssembler.java
│   ├── ToolResultJson.java
│   ├── provider/
│   │   ├── openai/OpenAiClient.java
│   │   ├── anthropic/AnthropicClient.java
│   │   └── deepseek/DeepSeekClient.java
│   └── transport/
│       └── 现有 HTTP/SSE 文件保持不变
│
└── terminal/
    ├── TerminalUi.java
    ├── JLineTerminalUi.java
    ├── TerminalLayout.java
    ├── ToolSummaryFormatter.java
    ├── UiState.java
    └── 其他现有 UI 文件保持不变
```

### 新建文件职责

| 区域 | 文件 | 职责 |
|---|---|---|
| 消息模型 | `MessagePart` 及三个实现 | 表达文本、工具调用和工具结果 |
| 会话 | `ConversationListener` | 统一转发文本和工具生命周期事件 |
| 会话 | `ConversationException` | 携带安全错误及工具是否已执行 |
| 工具框架 | `Tool`、`BaseTool` | 工具契约与公共执行模板 |
| 工具模型 | `ToolDefinition`、`ToolCall`、`ToolResult` | 工具描述、调用和结果 |
| 工具限制 | `ToolLimits` | 保存固定资源上限和命令超时 |
| 敏感信息 | `SecretRedactor` | 清理已知密钥、认证头和子进程敏感环境 |
| 注册中心 | `ToolRegistry`、`ToolDefinitionEncoder` | 注册、开关和厂商格式导出 |
| 执行 | `ToolExecution*`、`ToolExecutor` | 串行执行、事件发布和取消 |
| 工作区 | `WorkspacePolicy` | 路径边界、链接和敏感路径检查 |
| 工作区 | `WorkspaceWalker` | 安全、稳定、有界的目录遍历 |
| 文件支持 | `Utf8TextFile` | 有界 UTF-8 读取及二进制检测 |
| 文件支持 | `AtomicFileWriter` | 临时文件写入、校验和替换 |
| 核心工具 | 六个 `*Tool` | 六种模型可调用能力 |
| Glob | `GlobPattern` | 跨平台统一 Glob 语义 |
| LLM | `ToolCallAssembler` | 按位置拼接并校验 JSON 碎片 |
| LLM | `ToolResultJson` | 生成三个厂商共用的安全结果 JSON |
| 终端 | `ToolSummaryFormatter` | 生成有界且不泄露正文的工具摘要 |

### 修改现有文件

| 文件 | 主要修改 |
|---|---|
| `ImioCodeApplication.java` | 创建工作区策略、工具、注册中心和执行器并连接关闭链路 |
| `MessageRole.java` | 增加内部 `TOOL` 角色 |
| `ChatMessage.java` | 从单一文本扩展为消息部分列表，并保留文本便捷构造 |
| `ChatRequest.java` | 接收扩展后的消息历史 |
| `ChatResponse.java` | 返回统一助手消息并提供文本、工具调用查询 |
| `ConversationSession.java` | 实现一次工具执行、一次结果回传和原子历史提交 |
| `ConversationLoop.java` | 驱动工具 UI 状态、错误提示和中断流程 |
| `LlmClientFactory.java` | 向三个客户端注入注册中心 |
| `OpenAiClient.java` | 工具定义、消息、调用碎片和结果协议映射 |
| `AnthropicClient.java` | 工具定义、内容块碎片和结果协议映射 |
| `DeepSeekClient.java` | 工具定义、`tool_calls` 碎片和结果协议映射 |
| `TerminalUi.java` | 增加工具事件展示入口 |
| `JLineTerminalUi.java` | 展示工具调用过程和安全摘要 |
| `TerminalLayout.java` | 增加工具相关状态标签 |
| `UiState.java` | 增加 `TOOL_WAITING`、`TOOL_RUNNING` 状态 |

`LlmClient` 和 `StreamListener` 的公开方法保持不变；客户端仍通过 `streamChat` 返回扩展后的 `ChatResponse`，文本监听器仍保持函数式接口。

现有配置类型、HTTP/SSE 传输模块及 Maven 依赖不需要修改。

### 测试文件

```text
src/test/java/io/imiocode/
├── conversation/
│   ├── ConversationSessionTest.java
│   └── ConversationLoopTest.java
│
├── tool/
│   ├── BaseToolTest.java
│   ├── SecretRedactorTest.java
│   ├── ToolRegistryTest.java
│   ├── ToolExecutorTest.java
│   ├── workspace/
│   │   ├── WorkspacePolicyTest.java
│   │   ├── WorkspaceWalkerTest.java
│   │   └── AtomicFileWriterTest.java
│   └── core/
│       ├── ReadFileToolTest.java
│       ├── WriteFileToolTest.java
│       ├── EditFileToolTest.java
│       ├── BashToolTest.java
│       ├── GlobToolTest.java
│       └── GrepToolTest.java
│
├── llm/
│   ├── LlmClientContractTest.java
│   ├── ToolCallAssemblerTest.java
│   ├── ToolResultJsonTest.java
│   └── provider/
│       ├── openai/OpenAiClientTest.java
│       ├── anthropic/AnthropicClientTest.java
│       └── deepseek/DeepSeekClientTest.java
│
└── terminal/
    ├── ToolSummaryFormatterTest.java
    ├── TerminalLayoutTest.java
    └── JLineTerminalUiTest.java
```

测试继续使用 JUnit 5、临时目录和现有本地模拟 HTTP 服务，不使用真实 API Key，不增加 Mockito，也不访问用户文件。

现有 `claude.md` 的未提交修改不在本章文件范围内。

## 技术决策

### 固定资源限制

本章不增加 YAML 配置，统一使用不可变默认值：

| 限制 | 数值 | 行为 |
|---|---:|---|
| 单次读取字节 | 256 KiB | 达到后停止并标记截断 |
| 单次读取行数 | 2,000 行 | 达到后停止并标记截断 |
| 单次写入或编辑结果 | 1 MiB | 超过时写入前失败 |
| 单次工作区扫描 | 20,000 个路径项 | 达到后停止并标记截断 |
| Glob 结果 | 1,000 条 | 达到后停止 |
| Grep 匹配 | 200 条 | 达到后停止 |
| Grep 单行保留 | 2,000 字符 | 超长行截断 |
| 通用工具结果 | 512 KiB | 超出部分不进入模型上下文 |
| Bash 标准输出 | 128 KiB | 超出后继续排空但不保存 |
| Bash 标准错误 | 128 KiB | 超出后继续排空但不保存 |
| Bash 执行超时 | 30 秒 | 超时后终止进程树 |
| 进程优雅退出等待 | 2 秒 | 之后强制终止 |
| 终端输入或结果摘要 | 240 字符 | 仅影响 UI 展示 |

所有截断结果同时设置结构化 `truncated=true`，并在文本末尾加入统一的“输出已截断”标记。

### 工具名称与风险等级

| API 名称 | 实现名称 | 风险等级 |
|---|---|---|
| `read_file` | `ReadFileTool` | LOW |
| `write_file` | `WriteFileTool` | MEDIUM |
| `edit_file` | `EditFileTool` | MEDIUM |
| `bash` | `BashTool` | HIGH |
| `glob` | `GlobTool` | LOW |
| `grep` | `GrepTool` | LOW |

API 使用小写蛇形命名，兼容三家对函数名的限制；Java 类型保持常规类名。

### JSON Schema 与参数校验

所有工具 Schema：

- 根节点固定为 `object`；
- 明确列出 `properties` 和 `required`；
- 设置 `additionalProperties: false`；
- 行号等正整数在 Schema 中声明最小值；
- 应用执行前仍进行完整本地校验；
- OpenAI 和 DeepSeek 显式使用非严格模式，避免不同厂商的严格 Schema 子集差异；
- Anthropic 使用普通 `input_schema`。

模型输出即使符合 Schema，也不能绕过工作区和敏感路径检查。

### 流式 JSON 拼接

使用整数位置作为主键，调用标识作为完成校验字段：

- OpenAI 使用 `output_index` 路由参数碎片，并保存真正用于结果关联的 `call_id`。
- Anthropic 使用内容块 `index`，在 `content_block_stop` 后完成该调用。
- DeepSeek 使用 `tool_calls[].index`，分别追加 ID、名称和参数碎片。
- JSON 只在响应正常结束后解析一次。
- 完成事件提供的完整参数只用于一致性校验，不覆盖已经收到的增量。
- 未知事件忽略，未知错误事件转换为协议错误。

### 工具结果格式

发送给模型的结果统一编码为 JSON：

```json
{
  "success": true,
  "output": "...",
  "error": "",
  "truncated": false,
  "duration_ms": 12,
  "exit_code": 0
}
```

`exit_code` 仅 Bash 返回。Anthropic 失败结果额外设置原生 `is_error: true`；OpenAI 和 DeepSeek 通过统一 JSON 中的 `success` 表达失败。

### 路径与文件操作

选择 Java NIO 标准库，不引入原生代码或额外文件系统依赖。

- 所有文件工具使用同一个 `WorkspacePolicy`。
- 最终路径不接受符号链接或 Windows 重解析点。
- 目录遍历不使用 `FOLLOW_LINKS`。
- WriteFile 和 EditFile 使用同目录临时文件，优先原子移动。
- 原子移动不可用时重新校验并执行替换。
- UTF-8 解码器使用严格错误报告，不静默替换非法字节。
- 文本样本包含 NUL 字节时按二进制文件处理。

该策略防止常规路径逃逸，但不宣称抵御外部进程同时修改目录结构的竞态攻击。

### 稳定遍历

`WorkspaceWalker` 使用确定性深度优先遍历：

1. 枚举当前目录项。
2. 用有界结构保留字典序最小的可扫描项。
3. 排序后依次处理。
4. 跳过受保护目录和链接。
5. 达到扫描上限立即停止。

因此相同工作区和输入产生稳定顺序，同时不会无界保存整个目录树。

### Bash 进程管理

- 使用 `ProcessBuilder` 参数列表传递命令，不自行拼接 Shell 转义文本。
- stdout 和 stderr 使用 Java 21 虚拟线程并行排空；这只是单个命令的流读取，不构成多个工具并发执行。
- 进程终止使用 `ProcessHandle.descendants()` 先子后父处理。
- 当前进程保存在原子引用中，允许信号线程取消。
- 工具关闭、会话关闭和应用关闭均可重复调用。
- Bash 只固定启动目录，不提供 OS 沙箱。

### 敏感信息处理

`SecretRedactor` 接收当前配置的 API Key，并统一处理工具结果、异常安全消息和终端摘要：

- 精确替换当前 API Key；
- 脱敏 Bearer 认证值；
- 脱敏 `x-api-key` 等已知认证字段；
- 不记录原始 HTTP 响应正文；
- 不对未知字符串进行可能破坏正常输出的猜测式替换。

Bash 子进程启动前，删除名称中包含以下片段的环境变量，大小写不敏感：

```text
KEY
TOKEN
SECRET
PASSWORD
CREDENTIAL
```

这不会阻止命令主动读取文件，因此终端仍明确显示 Bash 的高风险标记。

### 历史提交

会话继续使用内存不可变快照和同步原子提交：

- 工具执行期间不修改正式历史；
- 工具结果保留结构化 `ToolResult`，不保存 UI 摘要；
- 完整工具轮次一次提交四类消息；
- 失败轮次完全不提交；
- 工具产生的文件或命令副作用不参与历史回滚。

### 兼容策略

- `StreamListener` 保持单抽象方法。
- `LlmClient.streamChat` 方法签名保持不变。
- `ChatMessage`、`ChatRequest`、`ChatResponse` 提供纯文本便捷构造。
- 原有厂商、HTTP、SSE 和终端测试继续作为回归测试。
- 不增加 Maven 依赖，继续使用 Java 21、Jackson、JLine 和 JUnit 5。

### 测试策略

| 范围 | 策略 |
|---|---|
| 文件工具 | JUnit 临时目录、中文路径、边界大小 |
| 路径安全 | `..`、绝对路径、敏感路径、符号链接和重解析点 |
| Bash | 按当前操作系统运行短命令、失败、超时和取消 |
| 注册中心 | 重名、启用、禁用、未知工具和稳定导出 |
| 流式拼接 | 手工构造碎片、交错调用、无效 JSON |
| 三家协议 | 本地模拟 HTTP/SSE，不调用真实服务 |
| 会话 | fake 客户端验证两次请求、串行结果和历史事务 |
| 终端 | fake 终端验证事件顺序、摘要和 ANSI 隔离 |
| 端到端 | 打包后在 tmux 中执行真实交互并逐项对照 checklist |

### 协议参考

- [OpenAI Responses 流式事件](https://platform.openai.com/docs/api-reference/responses-streaming/response/refusal/delta?lang=curl)
- [Anthropic 流式消息](https://platform.claude.com/docs/en/build-with-claude/streaming)
- [Anthropic 工具结果](https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls)
- [DeepSeek Chat Completion](https://api-docs.deepseek.com/api/create-chat-completion/)
- [DeepSeek Tool Calls](https://api-docs.deepseek.com/guides/tool_calls/)

## 需求追踪

| Spec 需求 | 设计归属 |
|---|---|
| F1、F2、F3 | `ToolDefinition`、`ToolResult`、`Tool`、`BaseTool`、`SecretRedactor` |
| F4 | 应用启动装配和六个核心工具 |
| F5 | `ReadFileTool`、`Utf8TextFile` |
| F6 | `WriteFileTool`、`AtomicFileWriter` |
| F7 | `EditFileTool`、`AtomicFileWriter` |
| F8、F9、F10 | `BashTool`、进程树终止、风险元信息 |
| F11 | `GlobPattern`、`GlobTool`、`WorkspaceWalker` |
| F12 | `GrepTool`、`Utf8TextFile`、`WorkspaceWalker` |
| F13 | `WorkspacePolicy` |
| F14 | `ToolLimits`、有界读取、遍历和进程输出 |
| F15、F16、F17 | `ToolRegistry`、`ToolDefinitionEncoder`、三个厂商编码器 |
| F18 | 三个模型客户端和注册中心注入 |
| F19、F20、F21 | `ToolCallAssembler`、三个厂商流式事件处理 |
| F22 | `MessagePart`、`ChatResponse`、文本流监听 |
| F23、F24 | `ToolExecutor`、`ConversationSession` |
| F25 | 会话第二响应检查和单轮限制错误 |
| F26、F27 | 临时轮次、原子历史提交、`ConversationException` |
| F28 | `ToolExecutionEvent`、`ToolSummaryFormatter`、终端扩展 |
| F29 | 终端信号、会话关闭、HTTP 取消和工具取消链路 |
| F30 | 统一消息模型、三个厂商适配器和客户端契约测试 |

## 非功能需求归属

| 需求范围 | 设计保障 |
|---|---|
| 敏感信息 | 敏感路径策略、子进程环境清理、已知密钥脱敏 |
| 工作区安全 | 路径规范化、逐级链接检查、访问前复查 |
| UTF-8 | 严格解码、统一编码、中文及 Unicode 测试 |
| 流式正确性 | 按位置独立缓冲、完成后一次解析 |
| 错误隔离 | 工具结果化、协议异常化、会话事务 |
| 向后兼容 | 纯文本便捷构造、现有接口和测试保留 |
| 顺序一致 | 同步执行器、稳定注册导出、稳定目录遍历 |
| 资源安全 | 固定字节、行数、结果、扫描及超时限制 |
| 进程清理 | 活动进程原子引用、子孙进程终止、幂等关闭 |
| 可测试性 | 临时工作区、fake 客户端、本地 HTTP/SSE |
| 跨平台 | 平台 Shell 边界、统一 `/` 路径语义 |
| 后续扩展 | 风险元信息、独立执行器和会话编排边界 |

## 设计自检

- F1–F30 均有明确组件和调用链负责，没有未归属需求。
- 六个工具共享框架、安全策略、限制和结果模型，没有重复实现关键安全逻辑。
- 工具层不依赖模型、会话或终端。
- 厂商适配器只依赖统一消息和工具定义，不执行工具。
- 会话层依赖 LLM 抽象与执行器，不依赖具体厂商。
- 终端只消费事件，不参与参数、结果或历史构造。
- 模块依赖单向，不存在循环依赖。
- 多工具只串行执行，没有提前实现并发调度。
- 会话最多执行一批工具，没有提前实现 Agent 循环。
- 风险等级只展示和保留，没有提前实现权限确认。
- Bash 的安全承诺与“不实现 OS 沙箱”边界保持一致。
- 路径安全承诺与 Java 跨平台能力保持一致。
- 所有固定限制都有明确数值，均已确定。
- 三家请求、流式调用及结果回传格式均有独立测试归属。
- 未增加第三方依赖，也未修改第二章配置格式。
- `claude.md` 不属于本章范围，继续保留用户现有修改。
- 当前设计无占位内容，也没有需要编码时临时决定的接口问题。
