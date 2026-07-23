# 第二章：LLM API 与终端多轮对话 Tasks

## 文件清单

| 操作 | 文件 | 职责 | 对应设计/需求 |
|------|------|------|---------------|
| 新建 | `pom.xml` | Java 21、依赖、测试和可执行 JAR 配置 | 全局 |
| 新建 | `src/main/java/io/imiocode/ImioCodeApplication.java` | 应用组合根和生命周期 | F1、F8、F9 |
| 新建 | `src/main/java/io/imiocode/config/Provider.java` | 厂商枚举与解析 | F1、F2 |
| 新建 | `src/main/java/io/imiocode/config/AppConfig.java` | 不可变运行配置 | F1、F9 |
| 新建 | `src/main/java/io/imiocode/config/ConfigException.java` | 配置错误与安全消息 | F9 |
| 新建 | `src/main/java/io/imiocode/config/ConfigLoader.java` | 环境变量读取和校验 | F1、F9 |
| 新建 | `src/main/java/io/imiocode/conversation/MessageRole.java` | 统一消息角色 | F5 |
| 新建 | `src/main/java/io/imiocode/conversation/ChatMessage.java` | 统一消息 | F5 |
| 新建 | `src/main/java/io/imiocode/conversation/ChatRequest.java` | 不可变请求快照 | F2、F5 |
| 新建 | `src/main/java/io/imiocode/conversation/ChatResponse.java` | 完整模型回复 | F4、F5、F11 |
| 新建 | `src/main/java/io/imiocode/conversation/ConversationSession.java` | 多轮历史与原子提交 | F5、F11 |
| 新建 | `src/main/java/io/imiocode/conversation/ConversationLoop.java` | 输入、请求、展示和退出循环 | F3、F6-F8、F10 |
| 新建 | `src/main/java/io/imiocode/llm/LlmClient.java` | 统一流式客户端接口 | F2、F4、F12 |
| 新建 | `src/main/java/io/imiocode/llm/LlmClientFactory.java` | 创建选定厂商客户端 | F2 |
| 新建 | `src/main/java/io/imiocode/llm/LlmErrorType.java` | 统一错误分类 | F10、F11 |
| 新建 | `src/main/java/io/imiocode/llm/LlmException.java` | 安全错误和恢复属性 | F10、F11 |
| 新建 | `src/main/java/io/imiocode/llm/StreamListener.java` | 文本增量回调 | F4 |
| 新建 | `src/main/java/io/imiocode/llm/provider/openai/OpenAiClient.java` | OpenAI Responses API 适配 | F2、F4、F10-F12 |
| 新建 | `src/main/java/io/imiocode/llm/provider/anthropic/AnthropicClient.java` | Anthropic Messages API 适配 | F2、F4、F10-F12 |
| 新建 | `src/main/java/io/imiocode/llm/provider/deepseek/DeepSeekClient.java` | DeepSeek Chat Completions API 适配 | F2、F4、F10-F12 |
| 新建 | `src/main/java/io/imiocode/llm/transport/HttpClientFactory.java` | 创建共享 HTTP 客户端 | F2、F11 |
| 新建 | `src/main/java/io/imiocode/llm/transport/HttpErrorMapper.java` | HTTP 状态到统一错误的转换 | F10 |
| 新建 | `src/main/java/io/imiocode/llm/transport/SseEvent.java` | SSE 事件模型 | F4 |
| 新建 | `src/main/java/io/imiocode/llm/transport/SseEventReader.java` | UTF-8 SSE 流解析 | F4、F11 |
| 新建 | `src/main/java/io/imiocode/terminal/TerminalUi.java` | 可测试的终端边界 | F3、F4、F6、F8、F10 |
| 新建 | `src/main/java/io/imiocode/terminal/JLineTerminalUi.java` | JLine 滚动式终端实现 | F3、F4、F6、F8 |
| 新建 | `src/test/java/io/imiocode/config/ConfigLoaderTest.java` | 配置与脱敏测试 | AC1、AC2 |
| 新建 | `src/test/java/io/imiocode/conversation/ConversationSessionTest.java` | 多轮与回滚测试 | AC4、AC8、AC12 |
| 新建 | `src/test/java/io/imiocode/conversation/ConversationLoopTest.java` | 终端循环行为测试 | AC3、AC5-AC8 |
| 新建 | `src/test/java/io/imiocode/llm/LlmClientContractTest.java` | 三厂商公共行为契约 | AC3、AC9、AC11、AC12 |
| 新建 | `src/test/java/io/imiocode/llm/provider/openai/OpenAiClientTest.java` | OpenAI 请求、流和错误测试 | AC2、AC7-AC12 |
| 新建 | `src/test/java/io/imiocode/llm/provider/anthropic/AnthropicClientTest.java` | Anthropic 请求、流和错误测试 | AC2、AC7-AC12 |
| 新建 | `src/test/java/io/imiocode/llm/provider/deepseek/DeepSeekClientTest.java` | DeepSeek 请求、流和错误测试 | AC2、AC7-AC12 |
| 新建 | `src/test/java/io/imiocode/llm/transport/MockLlmServer.java` | 本地可编排 HTTP/SSE 服务 | AC2、AC7-AC12 |
| 新建 | `src/test/java/io/imiocode/llm/transport/SseEventReaderTest.java` | SSE 分片、UTF-8 和断流测试 | AC8、AC10、AC12 |
| 新建 | `src/test/java/io/imiocode/terminal/JLineTerminalUiTest.java` | 终端显示与中断测试 | AC3、AC6、AC10 |

## T1：建立 Maven 工程骨架

**文件：** `pom.xml`
**依赖：** 无
**覆盖：** Java 21、Maven、JUnit 5、JLine、Jackson、可执行 JAR

**步骤：**
1. 设置 `groupId=io.imiocode`、`artifactId=imiocode` 和项目版本。
2. 将编译版本设为 Java 21，源码编码设为 UTF-8。
3. 添加 JLine、Jackson Databind 和 JUnit Jupiter 依赖。
4. 配置 Surefire 运行 JUnit 5。
5. 配置 Shade 插件生成以 `io.imiocode.ImioCodeApplication` 为入口的可执行 JAR。

**验证：**
- 运行：`mvn -q help:effective-pom`
- 预期：命令成功，输出包含 Java 21、JUnit 5、JLine、Jackson 和主类配置。

## T2：定义配置模型和厂商枚举

**文件：** `Provider.java`、`AppConfig.java`、`ConfigException.java`
**依赖：** T1
**覆盖：** F1、F2、F9

**步骤：**
1. 定义 `Provider` 的三个枚举值及大小写不敏感的 `parse` 方法。
2. 定义包含厂商、模型、API Key、Base URI、两个超时和最大输出 Token 数的 `AppConfig`。
3. 校验不可为空的字段、正数超时和正数 Token 上限。
4. 避免 `AppConfig.toString()` 输出 API Key。
5. 定义只暴露安全消息的 `ConfigException`。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：配置类型编译通过，源码中不存在输出完整 API Key 的逻辑。

## T3：实现环境变量配置加载

**文件：** `ConfigLoader.java`
**依赖：** T2
**覆盖：** F1、F9、AC1

**步骤：**
1. 读取 `IMIO_PROVIDER` 和 `IMIO_MODEL`。
2. 根据厂商读取对应 API Key 和可选 Base URL。
3. 设置三家默认 Base URL。
4. 读取两个可选超时和 `IMIO_MAX_OUTPUT_TOKENS`，提供明确默认值。
5. 对缺失值、非法 URI、非数字和非正数执行一次性校验。
6. 错误消息只指出配置项名称，不包含配置值中的密钥。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 场景：使用代码检查三个厂商分支和所有默认值。
- 预期：所有配置路径都返回完整 `AppConfig` 或抛出 `ConfigException`。

## T4：覆盖配置加载行为

**文件：** `ConfigLoaderTest.java`
**依赖：** T3
**覆盖：** AC1、AC2

**步骤：**
1. 分别测试三个厂商的 Key、模型和默认地址。
2. 测试自定义 Base URL、超时和最大输出 Token 数。
3. 测试缺失厂商、模型、Key 和非法厂商。
4. 测试非法 URI、非法数字、零值和负值。
5. 断言异常和配置字符串均不包含测试 API Key。

**验证：**
- 运行：`mvn -q -Dtest=ConfigLoaderTest test`
- 预期：全部配置测试通过，密钥脱敏断言通过。

## T5：定义统一对话值对象

**文件：** `MessageRole.java`、`ChatMessage.java`、`ChatRequest.java`、`ChatResponse.java`
**依赖：** T1
**覆盖：** F3-F5、F11

**步骤：**
1. 定义 `USER` 和 `ASSISTANT` 角色。
2. 定义非空、非纯空白的 `ChatMessage`。
3. 让 `ChatRequest` 对传入消息列表执行防御性复制并暴露不可变列表。
4. 定义只接收完整非空内容的 `ChatResponse`。
5. 不加入系统、工具或持久化字段。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：值对象编译通过，消息列表无法从外部修改。

## T6：定义统一 LLM 接口和错误模型

**文件：** `LlmClient.java`、`StreamListener.java`、`LlmErrorType.java`、`LlmException.java`
**依赖：** T5
**覆盖：** F2、F4、F10-F12

**步骤：**
1. 定义同步阻塞式 `streamChat` 接口和幂等 `close` 契约。
2. 定义只接收非空文本增量的 `StreamListener`。
3. 定义计划中的九种错误分类。
4. 在 `LlmException` 中保存分类、可恢复属性、可选状态码和安全消息。
5. 原始异常仅作为 cause，不把原始响应体作为默认展示文本。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：会话值对象与 LLM 抽象共同编译通过。

## T7：实现通用 SSE 读取器

**文件：** `SseEvent.java`、`SseEventReader.java`
**依赖：** T1
**覆盖：** F4、F11、AC10

**步骤：**
1. 使用 UTF-8 逐行读取输入流。
2. 识别 `event:` 和 `data:` 字段。
3. 用换行符合并连续的多个 `data:` 行。
4. 在空行处提交完整事件。
5. 忽略注释行和无关字段。
6. EOF 前存在完整未提交事件时按 SSE 规则提交。
7. 不解释 `[DONE]` 或任何厂商事件名称。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：SSE 类型编译通过，读取器不依赖厂商包。

## T8：测试 SSE 分片、UTF-8 和异常结束

**文件：** `SseEventReaderTest.java`
**依赖：** T7
**覆盖：** AC8、AC10、AC12

**步骤：**
1. 测试单行与多行 `data:`。
2. 测试多个连续事件和空数据事件。
3. 测试中文、代码和跨缓冲区 UTF-8 内容。
4. 测试注释行和未知字段。
5. 使用会抛出 `IOException` 的输入流验证断流传播。

**验证：**
- 运行：`mvn -q -Dtest=SseEventReaderTest test`
- 预期：事件内容、顺序、UTF-8 和异常传播断言全部通过。

## T9：实现 HTTP 客户端和错误映射

**文件：** `HttpClientFactory.java`、`HttpErrorMapper.java`
**依赖：** T2、T6
**覆盖：** F10、F11、N5、N10

**步骤：**
1. 按 `connectTimeout` 创建复用的 JDK `HttpClient`。
2. 将 401/403 映射为认证错误，429 映射为限流，5xx 映射为服务端错误。
3. 结合安全的厂商错误代码识别模型不存在，其余状态映射为未知或协议错误。
4. 为网络异常、超时和线程中断提供统一转换方法。
5. 保证映射后的消息不包含请求头、API Key 或完整响应体。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：HTTP 工具编译通过，错误映射不依赖终端模块。

## T10：建立本地流式 API 测试服务

**文件：** `MockLlmServer.java`
**依赖：** T1
**覆盖：** N8、N9、AC2、AC7-AC12

**步骤：**
1. 使用 JDK 本地 HTTP 服务绑定随机端口。
2. 记录请求路径、请求头和 JSON 请求体。
3. 支持按片段和可选延迟返回 SSE。
4. 支持指定 HTTP 状态和 JSON 错误体。
5. 支持在发送部分响应后主动关闭连接。
6. 提供幂等关闭方法，测试结束后释放端口。

**验证：**
- 运行：`mvn -q test-compile`
- 预期：测试服务可编译，且不依赖真实网络或 API Key。

## T11：实现 OpenAI Responses API 适配器

**文件：** `OpenAiClient.java`
**依赖：** T2、T5-T10
**覆盖：** F2、F4、F10-F12

**步骤：**
1. 构造 `{baseUri}/v1/responses` 请求并设置 Bearer 认证。
2. 写入模型、完整消息输入、流式开关和最大输出 Token 数。
3. 通过 Jackson 树模型构造和解析 JSON。
4. 从 `response.output_text.delta` 事件读取非空 `delta`。
5. 收到 `response.completed` 后返回聚合文本。
6. 将错误事件、失败事件、非 2xx、超时、网络错误和异常断流转换为 `LlmException`。
7. 使用原子引用跟踪活动输入流，使 `close()` 幂等并可终止读取。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：OpenAI 客户端编译通过且只依赖统一模型与传输模块。

## T12：测试 OpenAI 请求和流式事件

**文件：** `OpenAiClientTest.java`
**依赖：** T10、T11
**覆盖：** AC2、AC7-AC12

**步骤：**
1. 断言请求路径、Bearer 头、模型、历史、流式开关和 Token 上限。
2. 返回多个文本增量和完成事件，断言监听顺序及聚合结果。
3. 测试中文、多行文本和代码片段。
4. 测试认证失败、限流、服务端错误和失败事件。
5. 测试缺少完成事件、非法 JSON、断流和 `close()`。

**验证：**
- 运行：`mvn -q -Dtest=OpenAiClientTest test`
- 预期：OpenAI 正常、错误和中断测试全部通过。

## T13：实现 Anthropic Messages API 适配器

**文件：** `AnthropicClient.java`
**依赖：** T2、T5-T10
**覆盖：** F2、F4、F10-F12

**步骤：**
1. 构造 `{baseUri}/v1/messages` 请求。
2. 设置 `x-api-key`、`anthropic-version` 和 JSON 头。
3. 写入模型、消息历史、`stream=true` 和必需的 `max_tokens`。
4. 从 `content_block_delta` 的 `text_delta` 中读取非空文本。
5. 收到 `message_stop` 后返回聚合文本。
6. 转换错误事件、HTTP 错误、非法事件顺序和异常断流。
7. 实现幂等关闭活动响应流。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：Anthropic 客户端编译通过，协议逻辑限制在自身包内。

## T14：测试 Anthropic 请求和流式事件

**文件：** `AnthropicClientTest.java`
**依赖：** T10、T13
**覆盖：** AC2、AC7-AC12

**步骤：**
1. 断言路径、API Key 头、版本头、模型、消息和 `max_tokens`。
2. 返回内容块文本增量与 `message_stop`，断言输出和聚合结果。
3. 测试非文本内容块不会产生文本增量。
4. 测试认证、限流、服务端和流内错误。
5. 测试缺少停止事件、非法 JSON、断流和 `close()`。

**验证：**
- 运行：`mvn -q -Dtest=AnthropicClientTest test`
- 预期：Anthropic 正常、错误和中断测试全部通过。

## T15：实现 DeepSeek Chat Completions 适配器

**文件：** `DeepSeekClient.java`
**依赖：** T2、T5-T10
**覆盖：** F2、F4、F10-F12

**步骤：**
1. 构造 `{baseUri}/chat/completions` 请求并设置 Bearer 认证。
2. 写入模型、完整消息历史、`stream=true` 和最大输出 Token 数。
3. 从 `choices[0].delta.content` 读取非空文本。
4. 记录合法 `finish_reason` 并识别 `[DONE]`。
5. 仅在正常结束后返回聚合文本。
6. 转换错误对象、HTTP 错误、非法 JSON、缺失选择项和异常断流。
7. 实现幂等关闭活动响应流。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：DeepSeek 客户端编译通过，协议逻辑限制在自身包内。

## T16：测试 DeepSeek 请求和流式事件

**文件：** `DeepSeekClientTest.java`
**依赖：** T10、T15
**覆盖：** AC2、AC7-AC12

**步骤：**
1. 断言路径、Bearer 头、模型、历史、流式开关和 Token 上限。
2. 返回空增量、多个文本增量、结束原因和 `[DONE]`。
3. 断言空增量不触发监听器，文本顺序和聚合结果正确。
4. 测试认证、限流、服务端和错误对象。
5. 测试缺失选择项、缺少结束标记、非法 JSON、断流和 `close()`。

**验证：**
- 运行：`mvn -q -Dtest=DeepSeekClientTest test`
- 预期：DeepSeek 正常、错误和中断测试全部通过。

## T17：实现客户端工厂

**文件：** `LlmClientFactory.java`
**依赖：** T9、T11、T13、T15
**覆盖：** F2、F12、AC2

**步骤：**
1. 接收已校验的 `AppConfig`。
2. 创建共享 `HttpClient`、Jackson `ObjectMapper` 和 SSE 读取器。
3. 按 `Provider` 返回且只返回对应厂商客户端。
4. 不在工厂中包含终端或会话逻辑。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：三个枚举值分别映射到正确客户端，无默认静默回退。

## T18：建立三厂商公共客户端契约测试

**文件：** `LlmClientContractTest.java`，三个厂商测试类
**依赖：** T12、T14、T16
**覆盖：** F12、AC3、AC9、AC11、AC12

**步骤：**
1. 定义可由三个厂商测试复用的抽象契约。
2. 契约覆盖文本片段顺序、完整响应、异常断流不成功、非 2xx 转换和关闭幂等。
3. 为每个厂商提供协议对应的成功和断流事件夹具。
4. 保留厂商专属请求映射测试，不强行统一协议 JSON。

**验证：**
- 运行：`mvn -q -Dtest=OpenAiClientTest,AnthropicClientTest,DeepSeekClientTest test`
- 预期：三个客户端通过同一组公共行为断言和各自协议断言。

## T19：实现内存会话和原子提交

**文件：** `ConversationSession.java`
**依赖：** T5、T6
**覆盖：** F5、F11、AC4、AC8、AC12

**步骤：**
1. 保存私有消息历史并通过防御性复制返回快照。
2. `send` 创建当前用户消息和临时请求。
3. 把流监听器原样传给客户端。
4. 仅在客户端返回完整 `ChatResponse` 后同时追加用户和助手消息。
5. 任何异常和中断路径都保持历史不变。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：会话只依赖 `LlmClient`，不依赖具体厂商或终端实现。

## T20：测试多轮历史和失败回滚

**文件：** `ConversationSessionTest.java`
**依赖：** T19
**覆盖：** AC4、AC8、AC12

**步骤：**
1. 使用手写 fake 客户端完成两轮成功对话。
2. 断言第二轮请求包含第一轮完整消息对。
3. 断言成功后历史按用户、助手顺序增长。
4. 模拟流出部分文本后抛出异常，断言历史完全不变。
5. 断言外部无法修改历史快照。

**验证：**
- 运行：`mvn -q -Dtest=ConversationSessionTest test`
- 预期：多轮、原子提交和失败回滚测试全部通过。

## T21：实现终端抽象和 JLine 界面

**文件：** `TerminalUi.java`、`JLineTerminalUi.java`
**依赖：** T1
**覆盖：** F3、F4、F6、F8、F10、AC10

**步骤：**
1. 定义计划中的输入、输出、错误、信息、中断回调和关闭接口。
2. 构建 JLine `Terminal` 与 `LineReader`。
3. 实现用户提示和助手前缀。
4. 每次追加文本后立即 flush。
5. 保证正常完成和错误路径都从完整新行继续。
6. 注册 SIGINT/Ctrl+C 处理器并调用外部停止回调。
7. 让 `close()` 幂等。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：终端实现编译通过，不依赖厂商包。

## T22：测试终端输出与中断行为

**文件：** `JLineTerminalUiTest.java`
**依赖：** T21
**覆盖：** AC3、AC6、AC10

**步骤：**
1. 使用内存输入输出构造非系统终端。
2. 测试提示符读取和 UTF-8 输入。
3. 测试助手前缀、多个增量、刷新和结束换行。
4. 测试错误信息后下一提示位于新行。
5. 触发中断处理器并断言回调只产生预期停止行为。
6. 测试重复关闭不抛出异常。

**验证：**
- 运行：`mvn -q -Dtest=JLineTerminalUiTest test`
- 预期：终端格式、UTF-8、中断和关闭断言全部通过。

## T23：实现对话循环

**文件：** `ConversationLoop.java`
**依赖：** T19、T21
**覆盖：** F3、F6-F8、F10、F11

**步骤：**
1. 注册终端中断回调到 `requestStop()`。
2. 循环读取输入，忽略空白。
3. 识别大小写不敏感的 `/exit` 和 `/quit`。
4. 请求开始前输出助手前缀，将文本增量直接交给终端。
5. 正常完成和异常路径均正确结束当前输出行。
6. 可恢复错误显示安全消息并继续读取。
7. 停止请求关闭客户端活动流且不再读取输入。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：循环编译通过，且不引用任何厂商专属类型。

## T24：测试输入循环、错误恢复和退出

**文件：** `ConversationLoopTest.java`
**依赖：** T23
**覆盖：** AC3、AC5-AC8

**步骤：**
1. 用内存终端输入空行、空白和有效消息，断言只有有效消息触发请求。
2. 断言流式文本按到达顺序输出，完成后重新读取。
3. 分别测试 `/exit`、`/quit` 和 Ctrl+C。
4. 测试可恢复错误后继续下一轮输入。
5. 测试流中断显示未完成提示且不会提交历史。
6. 测试不可恢复关闭状态结束循环。

**验证：**
- 运行：`mvn -q -Dtest=ConversationLoopTest test`
- 预期：输入过滤、流式展示、恢复和退出测试全部通过。

## T25：组装应用入口和资源生命周期

**文件：** `ImioCodeApplication.java`
**依赖：** T4、T17、T20、T22、T24
**覆盖：** F1-F12、AC1、AC3、AC6、AC11

**步骤：**
1. 从 `System.getenv()` 加载配置。
2. 创建厂商客户端、会话、JLine 终端和对话循环。
3. 输出不含密钥的启动信息，包含当前厂商和模型。
4. 使用统一关闭路径释放终端和客户端。
5. 配置失败时输出安全消息并返回非零退出码。
6. 不捕获后静默忽略未知异常，统一输出安全兜底消息。

**验证：**
- 运行：`mvn -q -DskipTests compile`
- 预期：应用主类编译通过，所有模块从单一组合根连接。

## T26：生成并验证可执行 JAR

**文件：** `pom.xml`
**依赖：** T25
**覆盖：** AC1、AC3、AC13

**步骤：**
1. 确认 Shade 插件写入正确主类清单。
2. 避免签名元数据导致合并 JAR 启动失败。
3. 使用未设置配置的环境启动 JAR，确认安全失败。
4. 检查构建产物中未包含 API Key 或本地配置文件。

**验证：**
- 运行：`mvn -q package`
- 运行：`java -jar target/imiocode-*.jar`
- 预期：打包成功；未配置时明确指出缺失的 `IMIO_PROVIDER`，不出现堆栈和密钥。

## T27：执行完整自动化验证

**文件：** 全部主代码和测试文件
**依赖：** T26
**覆盖：** AC1-AC12

**步骤：**
1. 清理后重新编译项目。
2. 运行所有单元、协议、契约和集成测试。
3. 检查测试数量和失败报告，禁止跳过测试。
4. 搜索源码、测试输出和构建产物中的测试密钥字面量。

**验证：**
- 运行：`mvn -q clean test`
- 运行：`mvn -q package`
- 预期：全部测试通过并成功生成可执行 JAR，无测试被意外跳过。

## T28：在 tmux 中验证 OpenAI 两轮真实对话

**文件：** 无代码修改
**依赖：** T27；已安全设置 OpenAI 环境变量
**覆盖：** AC2-AC6、AC9、AC10、AC13

**步骤：**
1. 设置 `IMIO_PROVIDER=openai`、`IMIO_MODEL` 和 `OPENAI_API_KEY`，不打印 Key。
2. 在 tmux 会话 `imiocode-openai` 中启动可执行 JAR。
3. 输入：“请记住验证码是 IMIO-2749，只回复已记住。”
4. 观察回复是否增量显示并重新出现输入提示。
5. 输入：“我刚才让你记住的验证码是什么？”
6. 确认回复包含 `IMIO-2749`。
7. 输入 `/exit` 并确认程序正常结束。
8. 使用 `tmux capture-pane -p -t imiocode-openai` 保存可观察证据。

**验证：**
- 运行：`tmux capture-pane -p -t imiocode-openai`
- 预期：输出包含两次用户输入、流式回复结果、正确验证码和正常退出，无密钥或未处理异常。

## T29：在 tmux 中验证 Anthropic 两轮真实对话

**文件：** 无代码修改
**依赖：** T27；已安全设置 Anthropic 环境变量
**覆盖：** AC2-AC6、AC9、AC10、AC13

**步骤：**
1. 设置 `IMIO_PROVIDER=anthropic`、`IMIO_MODEL` 和 `ANTHROPIC_API_KEY`，不打印 Key。
2. 在 tmux 会话 `imiocode-anthropic` 中启动同一可执行 JAR。
3. 输入：“请记住验证码是 IMIO-2749，只回复已记住。”
4. 等待流式回复完成后询问：“我刚才让你记住的验证码是什么？”
5. 确认回复包含 `IMIO-2749`，交互方式与 OpenAI 一致。
6. 输入 `/exit` 并捕获终端输出。

**验证：**
- 运行：`tmux capture-pane -p -t imiocode-anthropic`
- 预期：两轮上下文正确、回复增量可见、退出正常、无密钥或协议细节泄漏。

## T30：在 tmux 中验证 DeepSeek 两轮真实对话

**文件：** 无代码修改
**依赖：** T27；已安全设置 DeepSeek 环境变量
**覆盖：** AC2-AC6、AC9、AC10、AC13

**步骤：**
1. 设置 `IMIO_PROVIDER=deepseek`、`IMIO_MODEL` 和 `DEEPSEEK_API_KEY`，不打印 Key。
2. 在 tmux 会话 `imiocode-deepseek` 中启动同一可执行 JAR。
3. 输入：“请记住验证码是 IMIO-2749，只回复已记住。”
4. 等待流式回复完成后询问：“我刚才让你记住的验证码是什么？”
5. 确认回复包含 `IMIO-2749`，交互方式与另外两家一致。
6. 输入 `/exit` 并捕获终端输出。

**验证：**
- 运行：`tmux capture-pane -p -t imiocode-deepseek`
- 预期：两轮上下文正确、回复增量可见、退出正常、无密钥或未处理异常。

## 执行顺序

```text
T1
├─> T2 -> T3 -> T4
├─> T5 -> T6
├─> T7 -> T8
└─> T10

T2 + T6 -> T9
T2 + T5-T10 -> T11 -> T12
T2 + T5-T10 -> T13 -> T14
T2 + T5-T10 -> T15 -> T16
T11 + T13 + T15 -> T17
T12 + T14 + T16 -> T18
T5 + T6 -> T19 -> T20
T1 -> T21 -> T22
T19 + T21 -> T23 -> T24
T4 + T17 + T20 + T22 + T24 -> T25 -> T26 -> T27
T27 -> T28、T29、T30（具备对应 API Key 时可并行）
```

## 覆盖追踪

| Spec/Plan 项 | 实现任务 | 主要验证任务 |
|--------------|----------|--------------|
| F1、F9：启动配置 | T2、T3、T25 | T4、T26 |
| F2：三厂商原生 API | T11、T13、T15、T17 | T12、T14、T16、T28-T30 |
| F3、F6：终端输入循环 | T21、T23 | T22、T24、T28-T30 |
| F4：流式输出 | T7、T11、T13、T15、T21、T23 | T8、T12、T14、T16、T22、T24 |
| F5：多轮历史 | T19 | T20、T28-T30 |
| F7：空输入 | T23 | T24 |
| F8：退出与中断 | T11、T13、T15、T21、T23 | T12、T14、T16、T22、T24 |
| F10：统一错误 | T6、T9、T11、T13、T15、T23 | T12、T14、T16、T24 |
| F11：失败回滚 | T7、T11、T13、T15、T19 | T8、T12、T14、T16、T20 |
| F12：一致行为 | T6、T17、T23 | T18、T28-T30 |
| 编译与打包 | T1、T25、T26 | T27 |
| tmux 端到端 | T28-T30 | T28-T30 |

## 任务自检

- `plan.md` 中的每个模块和文件均有实现任务。
- F1-F12 与 AC1-AC13 均有任务和验证覆盖。
- 每个任务包含具体文件、依赖、步骤、命令和预期结果。
- 依赖图无循环，三个厂商适配器可以并行实现。
- 类型名和方法名与 `plan.md` 一致。
- 未包含工具调用、持久化会话、运行时切换模型或其他 Ch2 范围外功能。

## 执行记录（2026-07-20）

- T1-T27：已完成。证据：`mvn -q clean package` 成功；基础实现测试已纳入最新 35 个测试。
- T28：部分执行。真实 OpenAI 配置能够启动完整 JAR 和终端循环，但当前网络环境连接 OpenAI 超时；错误被安全展示，程序继续接受输入并通过 `/exit` 正常退出。
- T29：未执行。当前环境未配置 `ANTHROPIC_API_KEY`。
- T30：已执行等价验收。通过本地 `config.yaml` 读取 DeepSeek 配置，完成两轮验证码对话；第二轮正确返回 `IMIO-2749`，并通过 `/exit` 正常退出。
- tmux 验收：未执行。Windows 主机未安装 tmux，WSL 也未安装 Linux 发行版。

## T31：添加 YAML 依赖和配置数据模型

**文件：** `pom.xml`、`ConfigDocument.java`、`ProviderConfig.java`
**依赖：** T27

**步骤：**
1. 添加 Jackson YAML 依赖。
2. 定义 YAML 顶层和厂商配置 record。
3. 映射带连字符的 YAML 字段。
4. 对配置对象的字符串输出进行 API Key 脱敏。

**验证：** 使用 Java 21 运行 `mvn -q -DskipTests compile`，期望编译通过。

## T32：实现 YAML 文件读取

**文件：** `YamlConfigLoader.java`
**依赖：** T31

**步骤：**
1. 从指定工作目录定位 `config.yaml`。
2. 文件不存在时返回空配置。
3. 使用 UTF-8 和 Jackson YAML 读取。
4. 拒绝未知字段、错误类型和未知厂商。
5. 将解析异常转换为不泄漏字段值的 `ConfigException`。

**验证：** 运行 `YamlConfigLoaderTest`，期望合法、缺失和非法文件场景通过。

## T33：实现逐字段配置合并

**文件：** `ConfigLoader.java`
**依赖：** T32

**步骤：**
1. 新增工作目录参数入口。
2. 按环境变量、YAML、默认值逐字段合并。
3. 根据合并后的当前厂商选择对应 Key 和 Base URL。
4. 空字符串视为未配置。
5. 保留原有环境变量入口。

**验证：** 运行 `ConfigLoaderTest`，确认纯 YAML、纯环境变量和混合覆盖通过。

## T34：添加安全配置文件

**文件：** `.gitignore`、`config.example.yaml`、`config.yaml`
**依赖：** T33

**步骤：**
1. 将根目录 `/config.yaml` 加入忽略规则。
2. 创建不含真实 Key 的完整示例文件。
3. 创建本地 `config.yaml`，配置 DeepSeek 和 `deepseek-chat`。
4. 真实 Key 只存在于被忽略的本地文件。

**验证：** `git check-ignore config.yaml` 能确认忽略规则；`config.example.yaml` 中不存在真实 Key。

## T35：补齐 YAML 与安全测试

**文件：** `YamlConfigLoaderTest.java`、`ConfigLoaderTest.java`
**依赖：** T34

**步骤：**
1. 测试文件不存在。
2. 测试三厂商分组配置。
3. 测试逐字段环境变量覆盖。
4. 测试非法 YAML、未知字段、错误类型和未知厂商。
5. 测试异常及配置对象不泄漏 Key。
6. 测试示例配置和忽略规则。

**验证：** 运行 `mvn -q test`，期望全部测试通过。

## T36：构建并执行 DeepSeek 配置文件验收

**文件：** 全部实现与测试
**依赖：** T35

**步骤：**
1. 运行干净构建并生成可执行 JAR。
2. 清除 DeepSeek 相关环境变量，确认程序只依赖 `config.yaml` 启动。
3. 完成两轮验证码对话，验证多轮上下文。
4. 输入 `/exit` 并确认正常退出。
5. 检查输出、测试报告和 JAR 中没有 API Key。

**验证：** `mvn -q clean package` 成功；ImioCode 从 YAML 读取 `deepseek-chat`；第二轮回复包含第一轮验证码。

## YAML 配置执行记录（2026-07-21）

- T31-T35：已完成。证据：`mvn -q clean package` 成功；10 个测试套件、35 个测试，0 失败、0 错误、0 跳过。
- T36：已完成。清除 `IMIO_PROVIDER`、`IMIO_MODEL`、`DEEPSEEK_API_KEY` 和 `DEEPSEEK_BASE_URL` 后，程序仅依赖当前目录 `config.yaml` 启动 `deepseek-chat`。
- DeepSeek 端到端结果：第一轮“请记住验证码是 IMIO-2749”回复“已记住”；第二轮询问验证码回复 `IMIO-2749`；输入 `/exit` 后进程退出码为 0。
- 安全检查：真实 API Key 未出现在 `src/`、`docs/`、`pom.xml`、`config.example.yaml`、`.gitignore` 或 `target/*.jar` 中；仅存在于本地 `config.yaml`。
- Git 忽略检查：当前目录不是有效 Git 工作树，无法运行 `git check-ignore config.yaml`；已验证 `.gitignore` 包含 `/config.yaml`。

## T37：定义 UI 状态和上下文

**文件：** `UiState.java`、`UiContext.java`、`TerminalMode.java`、`VersionResolver.java`

1. 定义四种动态状态和三种终端模式。
2. 定义产品、版本、厂商、模型和目录上下文。
3. 从 Manifest 读取版本，开发环境回退为 `dev`。

**验证：** 编译通过，版本回退测试通过。

## T38：实现响应式终端布局

**文件：** `TerminalLayout.java`、`TerminalLayoutTest.java`

1. 生成完整启动面板、紧凑面板和纯文本面板。
2. 生成输入框顶部、提示符、续行提示符和底部状态栏。
3. 按 20、40、60、100 列选择模式。
4. 截断长模型名和目录，保证状态栏不越界。
5. 使用 JLine 列宽计算处理中文和 Unicode。

**验证：** 布局测试确认所有输出行不超过终端宽度。

## T39：扩展终端接口和启动面板

**文件：** `TerminalUi.java`、`JLineTerminalUi.java`、`ImioCodeApplication.java`

1. 增加 `showWelcome`、`updateState` 和 `state`。
2. 启动时构造 `UiContext`。
3. 显示 Logo、版本、厂商/模型、目录和 Ready。
4. dumb terminal 使用纯文本启动信息。

**验证：** 内存终端输出包含全部启动字段，无 API Key。

## T40：实现带边框的多行输入框

**文件：** `JLineTerminalUi.java`

1. 使用主提示符与续行提示符绘制输入框。
2. `Enter` 提交。
3. 注册 `Alt+Enter` 插入换行 widget。
4. 保留 JLine 历史、方向键、退格和 Ctrl+C。
5. 提交后打印 `chat · 状态` 与模型状态栏。
6. 紧凑和纯文本模式正确降级。

**验证：** 输入测试确认多行内容只提交一次，模型收到原始文本且不含 UI 字符。

## T41：接入动态请求状态

**文件：** `ConversationLoop.java`

1. 有效消息提交前切换为 Thinking。
2. 首个流式片段到达时切换为 Streaming。
3. 成功完成后恢复 Ready。
4. 失败后切换 Error。
5. 空输入、退出命令和 UI 状态不进入会话历史。

**验证：** fake 客户端测试状态变化顺序和发送消息内容。

## T42：补齐 UI 回归测试

**文件：** `JLineTerminalUiTest.java`、`ConversationLoopTest.java`

1. 完整、紧凑、dumb 三种模式。
2. 中文、代码、长目录和长模型名。
3. 多行输入、输入历史、退出和 Ctrl+C。
4. 两轮滚动历史不覆盖。
5. 流式片段顺序不受状态刷新影响。

**验证：** `mvn -q test` 全部通过。

## T43：构建并执行真实终端验收

**文件：** 全部 UI 实现

1. 运行干净构建。
2. 使用现有 DeepSeek YAML 配置启动。
3. 检查启动面板和输入框。
4. 完成两轮真实对话。
5. 观察 Ready、Thinking、Streaming 和 Ready。
6. 缩窄终端验证降级。
7. 输入 `/exit` 正常退出。

**验证：** `mvn -q clean package` 通过，真实终端界面符合 AC18-AC25。

## UI 执行记录（2026-07-23）

- T37：已完成。新增 `UiState`、`UiContext`、`TerminalMode` 和 `VersionResolver`；开发环境版本回退为 `dev`，可执行 JAR 从 Manifest 显示 `0.2.0-SNAPSHOT`。
- T38：已完成。新增纯渲染 `TerminalLayout`，覆盖 20、40、60、100 列，中文、长模型名和长目录均不会越界。
- T39：已完成。终端接口、应用入口和启动面板已接入；面板显示产品、版本、厂商、模型、目录和 `Ready`，不接触 API Key。
- T40：已完成。完整/紧凑模式使用带边框输入区，dumb/窄终端降级为纯文本；`Alt+Enter` 插入换行、`Enter` 提交的真实按键序列测试通过。
- T41：已完成。会话状态按 `Ready → Thinking… → Streaming → Ready` 更新，失败进入 `Error`；状态及边框文本不进入 LLM 请求或会话历史。
- T42：已完成。新增布局、dumb 降级、多行按键和状态顺序回归测试；Java 21 全量测试通过。
- T43：完成当前环境可执行部分。`mvn -q clean package` 成功；使用现有 DeepSeek YAML 配置运行可执行 JAR，两轮真实对话依次返回 `OK` 和 `IMIO-2749`，`/exit` 退出码为 0。
- tmux 验收未执行：Windows 主机没有 tmux，WSL 没有已安装发行版；未擅自安装系统组件。富终端宽度与布局由内存终端和纯渲染测试覆盖，仍需在具备 tmux 的环境补一次人工交互验收。
