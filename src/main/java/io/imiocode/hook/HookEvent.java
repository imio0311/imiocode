package io.imiocode.hook;

import java.util.Arrays;

/** ImioCode 可挂载的生命周期事件。 */
public enum HookEvent {
    STARTUP("startup"),
    SHUTDOWN("shutdown"),
    SESSION_START("session_start"),
    SESSION_END("session_end"),
    TURN_START("turn_start"),
    TURN_END("turn_end"),
    PRE_SEND("pre_send"),
    POST_RECEIVE("post_receive"),
    PRE_TOOL_USE("pre_tool_use"),
    POST_TOOL_USE("post_tool_use"),
    PERMISSION_REQUEST("permission_request"),
    COMPACT("compact"),
    FILE_CHANGE("file_change"),
    COMMAND_EXECUTE("command_execute"),
    ERROR("error");

    private final String configName;

    HookEvent(String configName) {
        this.configName = configName;
    }

    public String configName() {
        return configName;
    }

    public static HookEvent parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hook event 不能为空");
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(event -> event.configName.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知 Hook event: " + normalized));
    }
}
