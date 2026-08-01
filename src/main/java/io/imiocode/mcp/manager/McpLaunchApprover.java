package io.imiocode.mcp.manager;

/** stdio 子进程启动前的人类确认边界。 */
@FunctionalInterface
public interface McpLaunchApprover {
    boolean approve(McpLaunchRequest request);
}
