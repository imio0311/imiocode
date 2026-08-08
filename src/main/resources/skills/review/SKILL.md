---
name: review
description: 隔离审查当前代码变更，按严重程度报告可操作问题
mode: fork
history: recent
aliases:
  - rv
allowedTools:
  - read_file
  - glob
  - grep
  - bash
---
# Review 工作流

审查重点：$ARGUMENTS

1. 读取 Git 状态和相关 diff，明确审查范围。
2. 查看必要的调用方、测试和配置，验证问题确实可触发。
3. 优先寻找正确性、安全性、数据丢失、并发、兼容性和缺失测试问题。
4. 每条发现包含严重程度、文件位置、触发条件和影响；不要报告纯风格偏好。
5. 若没有可操作问题，明确说明未发现问题，并指出仍未覆盖的验证风险。
6. 只做审查，不修改文件、不提交代码。
