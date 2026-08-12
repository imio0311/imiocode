package io.imiocode.team.tool;

import com.fasterxml.jackson.databind.node.*;
import io.imiocode.team.coordinator.*;
import io.imiocode.tool.*;

/** 在双锁和 Lead 身份校验后进入或退出 Coordinator Mode。 */
public final class CoordinatorModeTool extends BaseTool {
    private final CoordinatorModeController controller;private final TeamToolContext context;private final java.util.function.Supplier<ToolSelection> currentSelection;
    public CoordinatorModeTool(CoordinatorModeController controller,TeamToolContext context,ToolLimits limits,SecretRedactor redactor){this(controller,context,ToolSelection::allEnabled,limits,redactor);}
    public CoordinatorModeTool(CoordinatorModeController controller,TeamToolContext context,java.util.function.Supplier<ToolSelection> currentSelection,ToolLimits limits,SecretRedactor redactor){super(createDefinition(),limits,redactor);this.controller=controller;this.context=context;this.currentSelection=currentSelection;}
    private static ToolDefinition createDefinition(){ObjectNode s=JsonNodeFactory.instance.objectNode();s.put("type","object");s.putObject("properties").putObject("action").put("type","string").putArray("enum").add("enter").add("exit");s.putArray("required").add("action");s.put("additionalProperties",false);return new ToolDefinition("CoordinatorMode","进入或退出双锁保护的 Coordinator Mode。",s,ToolRisk.LOW);}
    @Override protected ToolResult executeValidated(ObjectNode a){rejectUnknownFields(a,"action");String action=requireText(a,"action");if("enter".equals(action)){var s=controller.enter(context.require(),currentSelection.get());return ToolResult.success("Coordinator Mode 已进入: "+s.stage());}controller.exit();return ToolResult.success("Coordinator Mode 已退出，原工具选择已恢复");}
}
