package io.imiocode.team.runtime;

import io.imiocode.team.persistence.TranscriptEntry;
import java.nio.file.Path;
import java.util.List;

/** 使用持久 transcript 和新消息执行成员的一轮续写。 */
@FunctionalInterface
public interface TeamMemberRunner {
    String run(String teamName,String agentId,String agentType,Path worktree,String prompt,List<TranscriptEntry> history);
}
