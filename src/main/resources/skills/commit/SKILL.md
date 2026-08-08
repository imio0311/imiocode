---
name: commit
description: 检查当前 Git 变更，生成符合约定的提交并验证结果
mode: inline
history: recent
allowedTools:
  - read_file
  - glob
  - grep
  - bash
---
# Commit 工作流

用户补充要求：$ARGUMENTS

1. 运行 `git status --short`，确认变更范围；不要把无关文件加入提交。
2. 阅读必要的 diff，识别本次变更的单一目的和潜在敏感信息。
3. 运行与改动相称的测试；失败时先说明并修复，不要提交已知失败状态。
4. 只暂存本次任务文件，使用 Conventional Commit 风格生成简洁提交信息。
5. 提交后再次检查 `git status` 和最新提交，向用户报告提交哈希、信息、测试结果及未提交文件。
6. 不执行 push，除非用户明确要求。
