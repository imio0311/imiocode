package io.imiocode.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** 模型可调用工具的统一接口。 */
public interface Tool {
    ToolDefinition definition();

    ToolResult execute(ObjectNode arguments);

    default void cancel() {
        // 大多数短时文件工具无需主动取消。
    }
}
