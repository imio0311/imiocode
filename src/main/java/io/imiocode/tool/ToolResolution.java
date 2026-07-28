package io.imiocode.tool;

import java.util.Objects;
import java.util.Optional;

/**
 * 工具解析结果。只有 AVAILABLE 状态携带可执行工具。
 */
public record ToolResolution(ToolAvailability availability, Optional<Tool> tool) {
    public ToolResolution {
        availability = Objects.requireNonNull(availability, "availability 不能为空");
        tool = Objects.requireNonNullElse(tool, Optional.empty());
        if ((availability == ToolAvailability.AVAILABLE) != tool.isPresent()) {
            throw new IllegalArgumentException("只有 AVAILABLE 状态可以携带工具");
        }
    }

    public static ToolResolution available(Tool tool) {
        return new ToolResolution(ToolAvailability.AVAILABLE, Optional.of(tool));
    }

    public static ToolResolution unavailable(ToolAvailability availability) {
        if (availability == ToolAvailability.AVAILABLE) {
            throw new IllegalArgumentException("AVAILABLE 必须携带工具");
        }
        return new ToolResolution(availability, Optional.empty());
    }
}
