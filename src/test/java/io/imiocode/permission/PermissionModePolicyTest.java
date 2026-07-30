package io.imiocode.permission;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionModePolicyTest {
    private final PermissionModePolicy policy = new PermissionModePolicy();

    @Test
    void implementsFiveModeSpectrum() {
        PermissionRequest read = request("read_file", PermissionOperation.READ);
        PermissionRequest write = request("write_file", PermissionOperation.WRITE);
        PermissionRequest command = request("bash", PermissionOperation.COMMAND);

        assertEquals(PermissionAction.DENY,
                policy.evaluate(read, PermissionMode.LOCKDOWN).action());
        assertEquals(PermissionAction.ALLOW,
                policy.evaluate(read, PermissionMode.READ_ONLY).action());
        assertEquals(PermissionAction.DENY,
                policy.evaluate(write, PermissionMode.READ_ONLY).action());
        assertEquals(PermissionAction.ASK,
                policy.evaluate(write, PermissionMode.ASK).action());
        assertEquals(PermissionAction.ALLOW,
                policy.evaluate(write, PermissionMode.AUTO_EDIT).action());
        assertEquals(PermissionAction.ASK,
                policy.evaluate(command, PermissionMode.AUTO_EDIT).action());
        assertEquals(PermissionAction.ALLOW,
                policy.evaluate(command, PermissionMode.FULL_ACCESS).action());
    }

    static PermissionRequest request(String tool, PermissionOperation operation) {
        return new PermissionRequest(
                new ToolCall("1", tool, JsonNodeFactory.instance.objectNode()),
                operation == PermissionOperation.READ ? ToolRisk.LOW : ToolRisk.HIGH,
                operation,
                "target",
                "target");
    }
}
