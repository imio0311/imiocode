package io.imiocode.team.runtime;
import io.imiocode.team.model.TeamBackend;
public record TeamCreateRequest(String name,String description,String leadAgentId,String leadName,String defaultAgentType,TeamBackend backend) { }
