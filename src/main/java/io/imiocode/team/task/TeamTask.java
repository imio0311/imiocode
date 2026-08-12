package io.imiocode.team.task;

import java.time.Instant;
import java.util.*;

public record TeamTask(String id,String teamName,String title,String description,TeamTaskStatus status,
        String assigneeAgentId,Set<String> blocksOn,Set<String> blockedBy,long version,
        Instant createdAt,Instant updatedAt,String result) {
    public TeamTask {
        id=text(id,"id");teamName=text(teamName,"teamName");title=text(title,"title");description=description==null?"":description;
        Objects.requireNonNull(status);assigneeAgentId=assigneeAgentId==null?"":assigneeAgentId.trim();blocksOn=Set.copyOf(Objects.requireNonNullElse(blocksOn,Set.of()));blockedBy=Set.copyOf(Objects.requireNonNullElse(blockedBy,Set.of()));if(version<=0)throw new IllegalArgumentException("version 必须为正数");Objects.requireNonNull(createdAt);Objects.requireNonNull(updatedAt);result=result==null?"":result;
    }
    TeamTask changed(TeamTaskStatus next,String assignee,Set<String> nextBlocksOn,Set<String> nextBlockedBy,String nextResult,Instant now){return new TeamTask(id,teamName,title,description,next,assignee,nextBlocksOn,nextBlockedBy,version+1,createdAt,now,nextResult);}
    TeamTask dependencies(Set<String> nextBlocksOn,Set<String> nextBlockedBy,Instant now){return new TeamTask(id,teamName,title,description,status,assigneeAgentId,nextBlocksOn,nextBlockedBy,version+1,createdAt,now,result);}
    private static String text(String v,String n){if(v==null||v.isBlank())throw new IllegalArgumentException(n+" 不能为空");return v.trim();}
}
