package io.imiocode.team.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public record TeammateInfo(String agentId, String name, String agentType, String model,
        TeamBackend backend, String backendHandle, Path worktree, TeammateStatus status,
        Boolean planApprovalRequired, Instant lastActiveAt) {
    public TeammateInfo {
        agentId=text(agentId,"agentId"); name=text(name,"name"); agentType=text(agentType,"agentType");
        model=model==null?"":model.trim(); Objects.requireNonNull(backend); backendHandle=backendHandle==null?"":backendHandle.trim();
        Objects.requireNonNull(worktree); worktree=worktree.toAbsolutePath().normalize(); Objects.requireNonNull(status); Objects.requireNonNull(lastActiveAt);
    }
    public TeammateInfo withStatus(TeammateStatus next, Instant now) {
        return new TeammateInfo(agentId,name,agentType,model,backend,backendHandle,worktree,next,planApprovalRequired,now);
    }
    public TeammateInfo withBackend(TeamBackend actual,String handle,TeammateStatus next,Instant now) {
        return new TeammateInfo(agentId,name,agentType,model,actual,handle,worktree,next,planApprovalRequired,now);
    }
    private static String text(String value,String name){if(value==null||value.isBlank())throw new IllegalArgumentException(name+" 不能为空");return value.trim();}
}
