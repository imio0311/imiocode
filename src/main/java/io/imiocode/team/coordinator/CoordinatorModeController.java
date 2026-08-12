package io.imiocode.team.coordinator;

import io.imiocode.conversation.SystemReminder;
import io.imiocode.team.TeamException;
import io.imiocode.team.model.TeamPrincipal;
import io.imiocode.tool.ToolSelection;
import java.util.*;

/** 双锁保护的 Coordinator Mode 与四阶段状态机。 */
public final class CoordinatorModeController {
    public static final String ENVIRONMENT_LOCK="IMIO_COORDINATOR_MODE";
    public static final Set<String> TOOL_WHITELIST=Set.of("agent","TaskGet","TaskList","TaskStop","SendMessage","TeamConverge","CoordinatorMode","CoordinatorAdvance");
    private final boolean featureEnabled,environmentEnabled;private CoordinatorStage stage=CoordinatorStage.OFF;private ToolSelection previous=ToolSelection.allEnabled();
    public CoordinatorModeController(boolean featureEnabled,Map<String,String> environment){this.featureEnabled=featureEnabled;this.environmentEnabled="true".equalsIgnoreCase(environment.get(ENVIRONMENT_LOCK));}
    public synchronized CoordinatorSnapshot enter(TeamPrincipal lead,ToolSelection current){if(!featureEnabled||!environmentEnabled)throw new TeamException("Coordinator Mode 需要 teams.coordinator-enabled 与 IMIO_COORDINATOR_MODE=true 双锁");if(!lead.lead())throw new TeamException("只有 Lead 可以进入 Coordinator Mode");if(stage!=CoordinatorStage.OFF)throw new TeamException("Coordinator Mode 已激活");previous=Objects.requireNonNull(current);stage=CoordinatorStage.RESEARCH;return snapshot();}
    public synchronized CoordinatorSnapshot advance(CoordinatorStage expected,CoordinatorStage next){if(stage==CoordinatorStage.OFF||stage!=expected)throw new TeamException("Coordinator 阶段不匹配");CoordinatorStage legal=switch(stage){case RESEARCH->CoordinatorStage.SYNTHESIS;case SYNTHESIS->CoordinatorStage.IMPLEMENTATION;case IMPLEMENTATION->CoordinatorStage.VERIFICATION;case VERIFICATION,OFF->CoordinatorStage.OFF;};if(next!=legal)throw new TeamException("Coordinator 阶段必须按 Research → Synthesis → Implementation → Verification 推进");stage=next;return snapshot();}
    public synchronized ToolSelection exit(){ToolSelection restore=previous;stage=CoordinatorStage.OFF;previous=ToolSelection.allEnabled();return restore;}
    public synchronized CoordinatorSnapshot snapshot(){return new CoordinatorSnapshot(stage!=CoordinatorStage.OFF,stage,stage==CoordinatorStage.OFF?previous:ToolSelection.only(TOOL_WHITELIST));}
    public synchronized Optional<SystemReminder> reminder(){if(stage==CoordinatorStage.OFF)return Optional.empty();String work=switch(stage){case RESEARCH->"并行委派调研，收集证据；不要直接修改文件。";case SYNTHESIS->"综合队员发现，消除冲突并决定是否返工；不得跳过综合。";case IMPLEMENTATION->"按已综合的规格委派实现；Lead 不直接修改代码。";case VERIFICATION->"委派独立验证，基于任务状态和证据收敛。";case OFF->"";};return Optional.of(new SystemReminder("Coordinator Mode / "+stage+"："+work));}
}
