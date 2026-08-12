package io.imiocode.team.runtime;
import io.imiocode.team.model.TeamBackend;
public record TeammateSpawnRequest(String name,String agentType,String model,TeamBackend backend,Boolean planApprovalRequired,String prompt) { }
