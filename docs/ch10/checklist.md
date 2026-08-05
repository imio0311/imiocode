# CH10 Slash Command 内置命令框架 Checklist

> 每一项均通过运行代码、捕获调用次数或观察真实终端行为验证。自动化测试使用临时 workspace、临时 userHome 和本地假 Provider，不访问用户真实会话、记忆或密钥。

## 命令契约、注册与解析

- [x] **AC1：十个核心命令完整注册。** 注册目录中 help、compact、clear、plan、do、session、memory、permission、status、review 各出现一次。（验证：运行注册中心目录测试，比较主名集合）
- [x] **AC1：兼容命令完整注册。** `/exit`、`/quit`、`/verbose`、`/compact-ui` 均可查找，其中 quit 指向 exit 的同一处理器。（验证：按主名和别名查找并比较对象身份）
- [x] **AC1：别名可查找。** h、?、cls、sessions、mem、perm、st、rv 均映射到预期核心命令。（验证：运行别名参数化测试）
- [x] **AC1：注册冲突立即失败。** 主名重复、别名重复、主名占用别名和别名占用主名均被拒绝，失败后原目录不变。（验证：逐类注册冲突并再次列举）
- [x] **AC1/N2：列举顺序稳定。** 不同注册顺序得到相同的按主名排序目录，每个命令只出现一次。（验证：构造两份逆序 Registry 并比较结果）
- [x] **AC2：基础解析正确。** 大小写、多余空白、单/双引号、转义和中文参数得到预期命令名与参数。（验证：运行解析器参数化测试）
- [x] **AC2/N6：Windows 路径不被破坏。** `C:\work\project`、带空格路径及反斜杠结尾按原义保留。（验证：解析后逐字符比较）
- [x] **AC2：非命令保持普通消息。** 不以 `/` 开头的文本返回非命令，不被修改。（验证：解析和 Registry dispatch 均返回空）
- [x] **AC2：未知和错误命令本地消费。** 未知命令、空命令、未闭合引号和错误参数返回安全错误，不形成普通消息。（验证：循环测试捕获模型调用次数为 0）
- [x] **AC4：三类结果互斥。** HANDLED、FORWARD_TO_AGENT、EXIT_REQUESTED 的消息和 Prompt 组合符合约束，非法组合构造失败。（验证：运行 `CommandResultTest`）

## 十个核心命令

- [x] **AC5：`/help` 动态展示目录。** 输出分核心与兼容两组，usage 和 description 来自注册描述，不重复命令。（验证：假 Registry 快照测试）
- [x] **AC5：`/compact` 保留 CH8 行为。** 有历史时显示压缩前后 Token；无历史时显示无需压缩；结果按 REPLACE 事务保存。（验证：协调器压缩集成测试和 JSONL 重载）
- [x] **AC5/AC8：`/clear` 只操作 UI。** 清屏调用一次，当前会话 ID、历史、模型调用数和工具调用数均不变。（验证：清屏前后比较服务快照和调用计数）
- [x] **AC5：`/plan` 切换到 Plan Mode。** 命令完成后模式为 PLAN，后续 Agent 只获得只读工具。（验证：模式测试和一次假 Agent 请求）
- [x] **AC5：`/do` 恢复 Do Mode。** 从 PLAN 切换后模式为 DO，正常工具选择恢复。（验证：模式测试和工具清单断言）
- [x] **AC5：`/session` 子命令完整。** current、list、new、resume、delete 的正常、禁用及错误参数路径保持 CH9 行为。（验证：运行 SessionCommand 参数化回归）
- [x] **AC5：会话删除仍需确认。** 当前会话拒绝删除；非当前会话在拒绝确认时保留，在同意时删除。（验证：假 UI 的 false/true 两路径）
- [x] **AC5：`/memory` 子命令完整。** list、add、edit、forget 的正常、禁用及错误参数路径保持 CH9 行为。（验证：运行 MemoryCommand 参数化回归）
- [x] **AC5/AC9：`/permission` 查询正确。** 无参数显示当前模式和五个合法模式，不显示规则正文。（验证：比较输出并搜索测试规则标记不存在）
- [x] **AC5/AC9：`/permission` 切换完整。** ask、auto-edit、read-only、full-access、lockdown 均可切换；非法或多余参数被拒绝。（验证：五模式参数化测试）
- [x] **AC5/AC10：`/status` 字段完整。** 输出 Provider、模型、目录、Agent 模式、权限模式、会话、Token 用量和 MCP 摘要。（验证：固定状态快照文本比较）
- [x] **AC10：`/status` 不泄密。** API Key、Authorization、MCP Header 和敏感环境变量测试值均不出现在状态输出。（验证：对完整输出搜索秘密标记）
- [x] **AC5/AC7：`/review` 构造固定 Prompt。** 无参数包含完整审查规则，有参数追加 Additional focus，结果类型为 FORWARD_TO_AGENT。（验证：Prompt 快照测试）
- [x] **AC7：`/review` 不改变模式。** 在 PLAN 和 DO 下执行后模式保持原值，命令自身不调用 UI 或工具。（验证：两模式服务探针）
- [x] **AC5：全部核心命令错误带 usage。** 缺失、额外或非法参数显示对应命令用法，交互循环继续。（验证：命令错误矩阵后发送正常输入）

## Agent、模型和历史隔离

- [x] **AC6/N9：八个非压缩本地/UI 命令零模型调用。** help、clear、plan、do、session、memory、permission、status 依次执行后假 LLM 调用数不增加。（验证：循环集成测试计数）
- [x] **AC6：八个非压缩本地/UI 命令零工具调用。** 同一命令序列后工具执行探针保持 0。（验证：假工具执行器计数）
- [x] **AC6：本地命令不进入历史。** 命令前后历史条数与内容不变，只有命令请求的状态副作用生效。（验证：比较 `historySnapshot()`）
- [x] **AC6：`/compact` 只走摘要流程。** 需要压缩时产生专用摘要请求，不出现 Agent iteration 或工具事件。（验证：假 LLM 请求类型和事件探针）
- [x] **AC7：`/review` 恰好进入一次 Agent。** 一次 review 产生一次普通 Agent 用户请求，固定模板和关注点均出现。（验证：本地 Provider 记录请求数和 body）
- [x] **AC7：原始 `/review` 不入历史。** 提交历史中保存生成后的 Prompt，不保存原始斜杠命令行。（验证：检查内存历史和 JSONL）
- [x] **AC14：命令异常不终止循环。** 服务抛出异常后只显示安全错误，下一条普通消息仍获得模型回复。（验证：失败服务 + 成功假 Provider 场景）

## 动态权限与运行时状态

- [x] **AC9：权限快照即时生效。** 同一个 PermissionChecker 在切换模式前后对同一请求给出对应的新决策。（验证：不重建 Checker 的连续决策测试）
- [x] **AC9：规则保持不变。** 切换五种模式后用户、项目和本地规则集合与启动快照完全相同。（验证：逐次比较不可变规则列表）
- [x] **AC9：重启恢复配置模式。** 第一进程临时切换后退出，第二进程从同一 config 启动并显示原模式。（验证：两个真实 Java 子进程输出比较）
- [x] **AC9：权限链顺序无回归。** 危险命令和沙箱拒绝不能被 full-access 绕过，规则与 HITL 顺序保持 CH6 行为。（验证：运行完整权限回归测试）
- [x] **AC11/N3：运行中状态修改被拒绝。** Agent active 时 plan、do、permission、session new/resume 均不改变当前状态。（验证：阻塞假 Agent 并并发调用服务）
- [x] **AC12：状态快照随模式变化。** plan/do 和 permission 切换后下一次快照立即显示新模式。（验证：连续获取快照比较）
- [x] **AC12：状态快照随会话变化。** new/resume 后会话 ID、消息数和 Token 估算更新。（验证：会话切换集成测试）
- [x] **AC10：Token 估算无副作用。** 获取状态不调用模型、不扫描文件、不改历史。（验证：三类探针计数为 0）

## Tab 补全与终端 UI

- [x] **AC11：单候选直接补齐。** `/perm<Tab>` 补为 `/permission`。（验证：JLine 输入序列测试缓冲区）
- [x] **AC11：多候选稳定展示。** `/c<Tab>` 展示 compact、clear、cls、compact-ui 的稳定排序候选。（验证：捕获候选菜单文本）
- [x] **AC11：别名参与补全。** h、?、cls、sessions、mem、perm、st、rv、quit 均可由对应前缀获得。（验证：补全参数化测试）
- [x] **AC11：参数不补全。** `/session r<Tab>`、普通文本和非首段 `/` 均不返回命令候选。（验证：Completer buffer/cursor 测试）
- [x] **N7：补全无外部副作用。** 连续补全不访问文件、模型、工具或 MCP。（验证：补全源只读取注册索引的架构测试）
- [x] **AC8：ANSI 清屏正确。** 支持 ANSI 的终端收到 clear-screen capability，随后显示最新状态。（验证：虚拟 ANSI 终端输出）
- [x] **AC8/N6：纯文本清屏安全。** dumb terminal 不输出 ANSI 控制序列，使用文本分隔并可继续输入。（验证：字节输出检查）
- [x] **AC12：详细状态栏实时更新。** Agent 状态、模式、权限和会话改变时详细状态行包含新值。（验证：顺序触发事件并捕获输出）
- [x] **AC12：精简状态栏不刷屏。** 相同状态不重复输出，状态改变或清屏后只输出一条紧凑状态。（验证：重复刷新计数）
- [x] **AC12：窄终端布局安全。** 各宽度下状态栏不越界、不破坏中文列宽并使用省略号。（验证：TerminalLayout 宽度参数化测试）
- [x] **N1：启动完整面板不变。** CH10 前后的产品名、版本、Provider、模型、目录和 Ready 行结构一致。（验证：现有 welcome 快照测试）
- [x] **N1：原确认 UI 不回归。** 权限、MCP 启动和会话删除确认仍接受原输入并默认安全拒绝。（验证：运行全部 JLine 确认测试）

## 兼容、架构与安全

- [x] **AC13：兼容帮助分组正确。** help 的兼容区显示 exit/quit、verbose、compact-ui，不混入十个核心清单。（验证：帮助输出分组断言）
- [x] **AC13：退出行为兼容。** exit 和 quit 均安全关闭且不调用模型；Ctrl+C 行为不变。（验证：真实子进程退出码和调用计数）
- [x] **AC13：详细度切换兼容。** verbose 和 compact-ui 保持原提示与后续展示策略，不改变 Agent 模式。（验证：原 UI 命令回归测试）
- [x] **AC3/N5：命令包不依赖具体 UI。** command 源码不引用 JLineTerminalUi、org.jline 或 runtime 循环。（验证：架构扫描测试）
- [x] **AC3：假 UI 可执行全部命令。** 十个核心命令及兼容命令均可在无终端实例下测试。（验证：BuiltinCommandTest 只使用内存假对象）
- [x] **N4/AC14：异常输出安全。** 构造含密钥的内部异常，用户输出不含密钥和堆栈类名。（验证：SecretRedactor 标记与异常文本搜索）
- [x] **N6：跨平台语法一致。** Windows/Unix 路径、CRLF/LF、中文和引号用例在当前平台均按逻辑路径处理。（验证：平台无关参数化测试）
- [x] **N10：用户文件隔离。** `claude.md` 和 `hello.txt` 未被编辑或暂存。（验证：提交前 status 与 staged 文件清单）

## 编译与自动化测试

- [x] **AC15：命令定向测试通过。** command 包全部测试 Failures=0、Errors=0。（验证：运行 command 测试集合）
- [x] **AC15：权限与运行时测试通过。** permission、runtime 相关测试 Failures=0、Errors=0。（验证：运行对应测试集合）
- [x] **AC15：终端测试通过。** terminal 包全部测试 Failures=0、Errors=0。（验证：运行 terminal 测试集合）
- [x] **AC15/N1：既有全量测试通过。** Agent、工具、权限、MCP、Prompt、上下文、会话、记忆和 UI 无回归。（验证：`mvn test` 退出码 0并记录总数）
- [x] **AC15：可运行 JAR 打包成功。** shaded JAR 生成且包含 Main-Class。（验证：`mvn -DskipTests package` 后启动 `--help` 或受控进程）
- [x] **代码与文档无空白错误。**（验证：`git diff --check` 无输出且退出码 0）
- [x] **工作树无运行数据和秘密。** 不包含 config.yaml、会话、记忆、target 或真实密钥。（验证：`git status --short` 与敏感模式扫描）

## 端到端场景

- [x] **AC16 E2E-1：本地命令零 Agent 调用。** 在真实 Java 子进程依次执行 help、clear、plan、do、session、memory、permission、status，Provider 收到 0 个请求，终端继续可输入。（验证：本地可控 Provider 请求队列）
- [x] **AC16 E2E-2：专用压缩路径。** 先产生可压缩历史，再执行 compact；只收到摘要请求，终端显示 Token 变化，工具事件数为 0。（验证：Provider 请求 body、JSONL REPLACE 和终端输出）
- [x] **AC16 E2E-3：Review Prompt 路径。** 执行 `/review 重点关注并发安全`，恰好收到一次 Agent 请求，包含固定规则和 Additional focus，回复正常写入会话。（验证：Provider 请求和恢复后的 JSONL）
- [x] **AC16 E2E-4：Tab 和状态栏。** 真实 JLine 测试终端完成单/多候选补全；切换 plan、permission、session 后状态栏立即变化。（验证：终端缓冲区与输出快照）
- [x] **AC9 E2E-5：权限只在当前进程生效。** 进程 A 切换 full-access 后退出，进程 B 启动显示 config 中原模式。（验证：两次启动输出）
- [x] **AC14 E2E-6：错误后恢复。** 输入未知命令和非法参数后，再发送普通消息，模型正常响应且错误命令未进入请求历史。（验证：终端输出与 Provider body）

## 验收记录

验收日期：2026-08-05。

- **命令、权限、状态与终端：通过。** 定向运行 command、permission、runtime、terminal 和 context 测试，覆盖注册冲突、解析、十个命令、动态权限、busy 拒绝、Tab 键绑定、ANSI/plain 清屏、状态去重和窄终端布局，Failures=0、Errors=0。
- **真实端到端：通过。** `mvn -q -Dtest=Ch10ApplicationIT test` 执行 5 个真实 Java 子进程场景：本地命令 Provider 请求数为 0；Review 恰好 1 次请求且 JSONL 不含原始 `/review`；Compact 产生 1 次额外无工具摘要请求并写入 REPLACE 事务；权限重启恢复 ask；错误命令后普通消息正常响应。
- **全量回归：通过。** `mvn test` 实际结果为 Tests run: 405、Failures: 0、Errors: 0、Skipped: 3；跳过项均为既有平台能力条件测试。
- **打包：通过。** `mvn -q -DskipTests package` 退出码 0；`target/imiocode-0.2.0-SNAPSHOT-all.jar` 的 Manifest 包含 `Main-Class: io.imiocode.ImioCodeApplication`。
- **工作树检查：通过。** `git diff --check` 退出码 0；没有暂存文件；未纳入 `target`、运行时 `config.yaml`、会话、记忆或真实密钥。用户原有 `claude.md` 修改和未跟踪 `hello.txt` 保持未暂存、未纳入 CH10。
- **环境说明：** Windows 环境无 tmux，按计划使用临时 workspace、临时 userHome、本地 Mock Provider 和真实 Java 子进程完成等价端到端验收。
