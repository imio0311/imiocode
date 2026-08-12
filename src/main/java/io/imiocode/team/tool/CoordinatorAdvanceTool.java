package io.imiocode.team.tool;

import com.fasterxml.jackson.databind.node.*;
import io.imiocode.team.coordinator.*;
import io.imiocode.tool.*;

public final class CoordinatorAdvanceTool extends BaseTool {
    private final CoordinatorModeController controller;
    public CoordinatorAdvanceTool(CoordinatorModeController controller,ToolLimits limits,SecretRedactor redactor){super(createDefinition(),limits,redactor);this.controller=controller;}
    private static ToolDefinition createDefinition(){ObjectNode s=JsonNodeFactory.instance.objectNode();s.put("type","object");ObjectNode p=s.putObject("properties");p.putObject("expected").put("type","string");p.putObject("next").put("type","string");s.putArray("required").add("expected").add("next");s.put("additionalProperties",false);return new ToolDefinition("CoordinatorAdvance","按固定顺序推进 Coordinator 阶段。",s,ToolRisk.LOW);}
    @Override protected ToolResult executeValidated(ObjectNode a){rejectUnknownFields(a,"expected","next");var s=controller.advance(CoordinatorStage.valueOf(requireText(a,"expected").toUpperCase()),CoordinatorStage.valueOf(requireText(a,"next").toUpperCase()));return ToolResult.success("Coordinator 阶段已推进: "+s.stage());}
}
