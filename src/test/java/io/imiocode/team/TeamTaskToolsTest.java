package io.imiocode.team;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.team.model.*;
import io.imiocode.team.persistence.TeamPaths;
import io.imiocode.team.persistence.TeamStore;
import io.imiocode.team.task.*;
import io.imiocode.team.tool.*;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolLimits;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TeamTaskToolsTest {
    @TempDir Path temp;

    @Test void allTaskToolsShareScopedPersistentGraphAndRejectSpoofedRole() {
        TeamPaths paths=new TeamPaths(temp);TeamStore teams=new TeamStore(paths);createTeam(teams,"one");createTeam(teams,"two");
        TeamTaskStore store=new TeamTaskStore(paths,20);AtomicReference<String> stopped=new AtomicReference<>();
        TeamTaskService service=new TeamTaskService(teams,store,(team,agent)->stopped.set(team+"/"+agent));
        TeamPrincipal lead=new TeamPrincipal("one","lead",TeamRole.LEAD);ToolLimits limits=ToolLimits.defaults();SecretRedactor redactor=new SecretRedactor("secret");
        TaskCreateTool create=new TaskCreateTool(service,()->lead,limits,redactor);
        var createdA=create.execute(JsonNodeFactory.instance.objectNode().put("title","A").put("assignee","worker"));
        assertTrue(createdA.success(),createdA.error());
        var createdB=create.execute(JsonNodeFactory.instance.objectNode().put("title","B"));
        assertTrue(createdB.success(),createdB.error());
        TeamTask a=store.list("one").get(0),b=store.list("one").get(1);
        TaskUpdateTool update=new TaskUpdateTool(service,()->lead,limits,redactor);
        assertTrue(update.execute(JsonNodeFactory.instance.objectNode().put("task_id",a.id()).put("status","RUNNING").put("addBlocksOn",b.id()).put("expected_version",a.version())).success());
        assertTrue(store.require("one",b.id()).blockedBy().contains(a.id()));
        assertTrue(new TaskGetTool(service,()->lead,limits,redactor).execute(JsonNodeFactory.instance.objectNode().put("task_id",a.id())).output().contains("RUNNING"));
        assertTrue(new TaskListTool(service,()->lead,limits,redactor).execute(JsonNodeFactory.instance.objectNode()).output().contains(b.id()));
        assertTrue(new TaskStopTool(service,()->lead,limits,redactor).execute(JsonNodeFactory.instance.objectNode().put("task_id",a.id())).success());
        assertEquals("one/worker",stopped.get());
        assertFalse(create.execute(JsonNodeFactory.instance.objectNode().put("title","bad").put("team","two")).success());
        assertEquals("[ ]",new TaskListTool(service,()->new TeamPrincipal("two","lead",TeamRole.LEAD),limits,redactor).execute(JsonNodeFactory.instance.objectNode()).output());
        assertThrows(TeamException.class,()->service.list(new TeamPrincipal("one","lead",TeamRole.MEMBER)));
    }

    private void createTeam(TeamStore teams,String name){Instant now=Instant.now();Path worker=temp.resolve(".imiocode/worktrees/"+name+"-worker");TeammateInfo lead=new TeammateInfo("lead","Lead","lead","",TeamBackend.IN_PROCESS,"lead",temp,TeammateStatus.RUNNING,false,now);TeammateInfo member=new TeammateInfo("worker","worker","general-purpose","",TeamBackend.IN_PROCESS,"",worker,TeammateStatus.STOPPED,false,now);teams.create(new TeamConfig(1,name,"","lead",TeamBackend.IN_PROCESS,Map.of("lead",lead,"worker",member),now,now));}
}
