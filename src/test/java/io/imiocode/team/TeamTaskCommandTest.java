package io.imiocode.team;

import io.imiocode.command.builtin.TaskCommand;
import io.imiocode.subagent.task.TaskManager;
import io.imiocode.team.model.TeamBackend;
import io.imiocode.team.model.TeamConfig;
import io.imiocode.team.model.TeamPrincipal;
import io.imiocode.team.model.TeamRole;
import io.imiocode.team.model.TeammateInfo;
import io.imiocode.team.model.TeammateStatus;
import io.imiocode.team.persistence.TeamPaths;
import io.imiocode.team.persistence.TeamStore;
import io.imiocode.team.task.TeamTaskService;
import io.imiocode.team.task.TeamTaskStatus;
import io.imiocode.team.task.TeamTaskStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamTaskCommandTest {
    @TempDir Path temp;

    @Test void legacyTaskCommandObservesAndStopsCurrentTeamTasks() {
        TeamPaths paths=new TeamPaths(temp);TeamStore teams=new TeamStore(paths);Instant now=Instant.now();
        TeammateInfo lead=new TeammateInfo("lead","Lead","lead","",TeamBackend.IN_PROCESS,
                "lead",temp,TeammateStatus.RUNNING,false,now);
        teams.create(new TeamConfig(1,"demo","","lead",TeamBackend.IN_PROCESS,
                Map.of("lead",lead),now,now));
        TeamTaskStore store=new TeamTaskStore(paths,20);
        TeamTaskService service=new TeamTaskService(teams,store,(team,agent)->{});
        TeamPrincipal principal=new TeamPrincipal("demo","lead",TeamRole.LEAD);
        var task=service.create(principal,"统一任务视图","可由旧命令观察","");
        try(TaskManager oldTasks=new TaskManager((definition,prompt,history,background,mode,cancellation)->null,
                1,16,16)){
            TaskCommand command=new TaskCommand(oldTasks,service,()->Optional.of(principal));
            String listed=command.execute(null,List.of()).messages().getFirst().text();
            assertTrue(listed.contains(task.id()));assertTrue(listed.contains("统一任务视图"));
            String info=command.execute(null,List.of("info",task.id())).messages().getFirst().text();
            assertTrue(info.contains("团队任务"));
            command.execute(null,List.of("cancel",task.id()));
            assertEquals(TeamTaskStatus.STOPPED,store.require("demo",task.id()).status());
        }
    }
}
