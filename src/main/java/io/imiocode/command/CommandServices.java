package io.imiocode.command;

import io.imiocode.agent.AgentMode;
import io.imiocode.context.CompactReport;
import io.imiocode.memory.MemoryDocument;
import io.imiocode.memory.MemoryEntry;
import io.imiocode.memory.MemoryScope;
import io.imiocode.session.SessionId;
import io.imiocode.session.SessionLoadResult;
import io.imiocode.session.SessionSummary;
import io.imiocode.permission.PermissionMode;
import io.imiocode.skill.SkillCatalogSnapshot;
import io.imiocode.skill.install.SkillInstallListener;
import io.imiocode.skill.install.SkillInstallResult;
import java.util.List;
import java.util.Optional;

public interface CommandServices {
    AgentMode mode();
    void switchMode(AgentMode mode);
    CompactReport compact();
    SessionSummary currentSession();
    List<SessionSummary> listSessions();
    SessionSummary newSession();
    SessionLoadResult resumeSession(SessionId id);
    void deleteSession(SessionId id);
    boolean sessionsEnabled();
    List<MemoryDocument> listMemories(Optional<MemoryScope> scope);
    MemoryEntry addMemory(MemoryScope scope, String content);
    MemoryEntry editMemory(MemoryScope scope, String id, String content);
    void forgetMemory(MemoryScope scope, String id);
    boolean memoryEnabled();
    PermissionMode permissionMode();
    void switchPermissionMode(PermissionMode mode);
    CommandStatus status();

    default String prepareSkillInvocation(String name, String arguments) {
        throw new IllegalStateException("Skill 功能尚未初始化");
    }

    default SkillCatalogSnapshot skillCatalog() {
        return SkillCatalogSnapshot.empty();
    }

    default SkillCatalogSnapshot reloadSkills() {
        throw new IllegalStateException("Skill 功能尚未初始化");
    }

    default SkillInstallResult installSkill(String url, boolean force, SkillInstallListener listener) {
        throw new IllegalStateException("Skill 远程安装功能尚未初始化");
    }

    default void cancelActiveWork() {
        // 无 Agent 的命令测试环境无需取消。
    }
}
