package io.imiocode.team.runtime;

import io.imiocode.team.persistence.TranscriptEntry;
import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
public interface TeamMemberRunner {
    String run(String teamName,String agentId,String agentType,Path worktree,String prompt,List<TranscriptEntry> history);
}
