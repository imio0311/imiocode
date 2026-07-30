# ch6 权限系统补充 Spec

## 背景

当前 ch6 已完成统一权限决策、危险命令拦截、路径沙箱、三层规则、五种权限模式和 HITL。对照新的参考设计后，发现两个可以在不破坏既有约定的前提下补充的能力：

- 危险命令黑名单尚未覆盖通过管道下载并执行脚本、递归开放系统权限等模式。
- `ASK` 模式下所有 Bash 命令都会询问，缺少对确定性只读命令的快速放行。

## 目标

- 扩展跨平台危险命令检测范围，堵住常见管道执行和权限破坏方式。
- 增加严格的安全只读命令检测器，减少无意义确认。
- 保持危险命令、显式规则和沙箱的优先级不变。
- 保持工作区单根沙箱、五种权限模式和“同文件第一条匹配”语义。
- 为未来 Plan 工具扩展提供清晰接入点，但不创建尚不存在的工具。

## 功能需求

- **SF1：扩展危险命令硬拦截**
  - 拦截递归开放系统根目录权限，例如 `chmod -R 777 /`、`chmod -R a+rwx /`。
  - 拦截 Windows 系统盘权限接管，例如对系统盘递归执行 `icacls ... /grant Everyone:F`。
  - 拦截下载后通过管道直接执行：
    - `curl ... | sh/bash/zsh/pwsh/powershell`
    - `wget ... | sh/bash/zsh/pwsh/powershell`
    - PowerShell `iwr/irm/Invoke-WebRequest/Invoke-RestMethod | iex`
  - 拦截通过命令替换或 `eval` 执行下载内容的常见变体。
  - 新增模式继续遵守硬拦截原则，不能被模式、规则、安全白名单或 HITL 覆盖。

- **SF2：安全只读命令识别**
  - 新增独立安全命令检测器。
  - 支持 Bash、PowerShell 和常见跨平台开发命令。
  - 首批白名单覆盖：
    - 目录与文件读取：`ls`、`dir`、`pwd`、`cat`、`type`、`head`、`tail`、`wc`、`stat`、`file`
    - PowerShell 读取：`Get-ChildItem`、`Get-Content`、`Get-Location`、`Get-Date`、`Test-Path`、`Resolve-Path`
    - 搜索与定位：`grep`、`rg`、`Select-String`、`which`、`where`、`Get-Command`
    - Git 只读子命令：`status`、`diff`、`log`、`show`、`rev-parse`、`ls-files`、`remote -v`、`branch --show-current`
    - 版本和环境查询：`java -version`、`javac -version`、`mvn -version`、`node --version`、`npm --version`、`python --version`、`go version`、`rustc --version`、`cargo --version`、`gradle --version`
    - 系统信息：`uname`、`whoami`、`hostname`、`date`
  - Git 写操作、构建、测试、包管理、网络请求和任意脚本执行不属于安全白名单。

- **SF3：完整命令判定**
  - 单命令必须整体匹配安全语法，不能只检查字符串前缀。
  - 对 `|`、`&&`、`||`、`;` 分隔的全部命令段逐一判断，只有每段都安全时才判定安全。
  - 引号内的分隔符不作为命令边界。
  - 出现输出重定向、后台执行、命令替换、反引号、Shell 子进程、`eval`、`iex` 或无法确定的语法时，不自动放行。
  - `find` 等具有执行或删除能力且难以可靠限制的命令不进入首批白名单。

- **SF4：检查顺序**
  - 最终顺序调整为：
    1. 危险命令硬拦截；
    2. 路径沙箱；
    3. `LOCKDOWN/READ_ONLY` 模式上限；
    4. 用户级、项目级、本地级显式规则；
    5. 安全只读命令自动允许；
    6. 当前权限模式默认行为；
    7. 必要时进入 HITL。
  - 显式 `deny` 或 `ask` 规则可以覆盖安全命令自动允许。
  - `ASK` 和 `AUTO_EDIT` 模式下，未命中规则的安全命令自动允许。
  - Plan Mode 仍不开放 Bash，因此安全命令白名单不能绕过 Plan Mode 的工具选择。

- **SF5：兼容性**
  - 不修改现有权限文件格式。
  - 不改变同一文件“第一条匹配生效”的语义。
  - 不增加额外沙箱根目录。
  - 不改变现有五种权限模式名称。
  - 不自动持久化 HITL 回复。
  - 不创建当前项目尚不存在的未来工具。

## 非功能需求

- **SN1：安全优先**
  - 安全命令检测器只能产生“确定安全”或“无法确认”。
  - 任何解析异常、未知语法或不完整命令均按“无法确认”处理，继续交给模式或 HITL，不能自动允许。
  - 危险检测器与安全检测器发生冲突时，危险结果永远优先。

- **SN2：低误放行**
  - 不使用简单的 `startsWith` 作为最终判断。
  - 命令名必须形成完整词边界，避免把 `git status-malicious` 误判为 `git status`。
  - Git、版本查询等命令必须验证子命令和允许参数。

- **SN3：低误拦截**
  - 新增硬拦截规则必须包含正常命令反例。
  - 单独下载文件、查看权限或查看 Git 状态不作为危险命令硬拦截；它们仍可由规则、模式或 HITL 决策。

- **SN4：跨平台**
  - 命令名称匹配不区分大小写。
  - 同时识别 PowerShell 和 Bash 常见别名。
  - 分隔符和引号扫描不依赖当前操作系统 Shell。

- **SN5：性能**
  - 危险规则和安全规则在初始化时预编译。
  - 单次判断只处理当前命令文本，不读取文件、不启动进程、不访问网络。

- **SN6：可测试性**
  - 每个安全命令族至少包含一个允许用例和一个相近的拒绝用例。
  - 混合安全/危险命令、重定向、命令替换和引号场景必须有自动化测试。
  - 既有 218 项测试必须继续通过。

## 不做的事

- 修改现有五种权限模式为截图中的四种模式。
- 改变同一规则文件的第一条匹配语义。
- 新增运行时 `AppendLocalRule` 或自动写入权限文件。
- 把系统临时目录或额外目录加入路径沙箱。
- 允许 Plan Mode 执行 Bash。
- 创建 `ToolSearch`、`AskUserQuestion`、`PlanFile` 等当前不存在的工具。
- 实现完整 PowerShell、Bash 或 CMD 语法解析器。
- 自动判断任意脚本文件内部是否安全。
- 将 `mvn test`、`npm install`、`git pull` 等可能产生文件或网络副作用的操作列入安全白名单。
- 网络域名限制、下载内容扫描或远程脚本沙箱。
- 资源配额和审计日志。

## 验收标准

- **SAC1（SF1）**：`chmod -R 777 /`、`chmod -R a+rwx /` 和 Windows 系统盘递归完全授权均被硬拦截。
- **SAC2（SF1）**：`curl|sh`、`wget|bash`、`iwr|iex` 以及带额外空白、大小写变化的等价形式均被硬拦截。
- **SAC3（SF1）**：通过 `eval`、命令替换或 PowerShell 表达式直接执行下载内容的代表性形式被硬拦截。
- **SAC4（SF1）**：上述命令在 `FULL_ACCESS` 和显式 `allow` 规则下仍然不能执行。
- **SAC5（SF1）**：单独使用 `curl` 下载、运行 `chmod --help` 或查看普通文件权限不会被危险检测器误判为硬拦截。
- **SAC6（SF2）**：白名单中的目录读取、文本读取、搜索、Git 查询、版本查询和系统信息命令能够被识别为安全。
- **SAC7（SF2）**：`git reset`、`git clean`、`git checkout`、`git switch`、`git branch -D`、构建、测试、安装和网络命令不会被识别为安全。
- **SAC8（SF2）**：`ASK` 与 `AUTO_EDIT` 模式下，未匹配显式规则的安全命令无需弹出 HITL。
- **SAC9（SF2）**：普通 `FULL_ACCESS` 行为保持不变；`READ_ONLY` 与 Plan Mode 仍不能借安全白名单执行 Bash。
- **SAC10（SF3）**：`git status && java -version` 可判定为安全。
- **SAC11（SF3）**：`git status && rm -rf .`、`cat file > output`、`echo $(dangerous)` 和带后台执行的命令不能自动放行。
- **SAC12（SF3）**：引号中的 `|`、`;` 不会被错误拆成命令边界。
- **SAC13（SF3）**：未知命令、缺失子命令和畸形引号按“无法确认”处理。
- **SAC14（SF4）**：显式 `deny` 规则可以拒绝 `git status`；显式 `ask` 规则可以强制其进入 HITL。
- **SAC15（SF4）**：危险命令同时匹配安全命令前缀时仍返回危险硬拦截来源。
- **SAC16（SF4）**：决策来源能够区分安全命令自动允许、显式规则、权限模式和危险硬拦截。
- **SAC17（SF5）**：既有权限 YAML 无需修改即可继续加载。
- **SAC18（SF5）**：同文件第一条匹配、工作区单根沙箱、五种模式及 HITL 三选一行为保持不变。
- **SAC19（整体）**：新增专项测试和全部既有测试通过，0 failures、0 errors。
- **SAC20（端到端）**：真实 Agent 请求 `git status` 时无需确认即可执行；请求非白名单安全命令时仍显示 HITL；请求下载并执行脚本时直接拒绝且不显示允许选项。
