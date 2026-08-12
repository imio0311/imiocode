package io.imiocode.team.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.team.model.TeamPrincipal;
import io.imiocode.team.task.TeamTaskService;
import io.imiocode.tool.*;
import java.util.function.Supplier;
import java.io.IOException;
import java.time.Instant;

abstract class AbstractTaskTool extends BaseTool {
    protected final TeamTaskService tasks;private final Supplier<TeamPrincipal> principal;protected final ObjectMapper mapper=taskMapper();
    AbstractTaskTool(ToolDefinition definition,TeamTaskService tasks,Supplier<TeamPrincipal> principal,ToolLimits limits,SecretRedactor redactor){super(definition,limits,redactor);this.tasks=tasks;this.principal=principal;}
    protected TeamPrincipal principal(){return principal.get();}
    protected ToolResult json(Object value){try{return ToolResult.success(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value));}catch(Exception e){return ToolResult.failure("无法编码任务结果");}}
    protected static ObjectNode schema(){ObjectNode s=com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();s.put("type","object");s.putObject("properties");s.put("additionalProperties",false);return s;}
    private static ObjectMapper taskMapper(){SimpleModule module=new SimpleModule();module.addSerializer(Instant.class,new JsonSerializer<>(){@Override public void serialize(Instant value,JsonGenerator generator,SerializerProvider serializers)throws IOException{generator.writeString(value.toString());}});return new ObjectMapper().registerModule(module);}
}
