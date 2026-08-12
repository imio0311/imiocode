package io.imiocode.team.backend;

import java.nio.file.Path;
import java.util.List;

public record TeammateLaunchRequest(String teamName,String agentId,Path repositoryRoot,Path worktree,List<String> command,Runnable inProcessTask) {
    public TeammateLaunchRequest {repositoryRoot=repositoryRoot.toAbsolutePath().normalize();worktree=worktree.toAbsolutePath().normalize();command=List.copyOf(command==null?List.of():command);}
}
