package io.imiocode.team;

import io.imiocode.team.model.*;
import io.imiocode.team.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TeamPersistenceTest {
    @TempDir Path temp;
    @Test void atomicallyRoundTripsRosterAndRejectsDamage() throws Exception {
        TeamPaths paths=new TeamPaths(temp);TeamStore store=new TeamStore(paths);Instant now=Instant.parse("2026-08-10T00:00:00Z");
        TeammateInfo lead=new TeammateInfo("lead","Lead","lead","model",TeamBackend.IN_PROCESS,"lead",temp,TeammateStatus.RUNNING,false,now);
        TeamConfig expected=new TeamConfig(1,"demo","说明","lead",TeamBackend.AUTO,Map.of("lead",lead),now,now);
        store.create(expected);assertEquals(expected,store.require("demo"));assertThrows(TeamException.class,()->store.create(expected));
        Path file=paths.teamDirectory("demo").resolve("team.json");Files.writeString(file,"{broken");assertThrows(TeamException.class,()->store.require("demo"));assertTrue(Files.exists(file));
    }
    @Test void rejectsUnsafeSlugsAndMissingLead(){TeamPaths paths=new TeamPaths(temp);assertThrows(IllegalArgumentException.class,()->paths.teamDirectory("../escape"));Instant now=Instant.now();assertThrows(IllegalArgumentException.class,()->new TeamConfig(1,"demo","","lead",TeamBackend.AUTO,Map.of(),now,now));}
    @Test void rejectsDuplicateJsonMemberKeysAndUnsafeAgentIds() throws Exception {
        TeamPaths paths=new TeamPaths(temp);TeamStore store=new TeamStore(paths);Path directory=paths.teamDirectory("demo");Files.createDirectories(directory);
        String root=temp.toAbsolutePath().toString().replace("\\","\\\\");
        String member="{\"agentId\":\"lead\",\"name\":\"Lead\",\"agentType\":\"lead\",\"backend\":\"in-process\",\"worktree\":\""+root+"\",\"status\":\"RUNNING\",\"lastActiveAt\":\"2026-01-01T00:00:00Z\"}";
        Files.writeString(directory.resolve("team.json"),"{\"schemaVersion\":1,\"name\":\"demo\",\"leadAgentId\":\"lead\",\"preferredBackend\":\"auto\",\"createdAt\":\"2026-01-01T00:00:00Z\",\"updatedAt\":\"2026-01-01T00:00:00Z\",\"members\":{\"lead\":"+member+",\"lead\":"+member+"}}");
        assertThrows(TeamException.class,()->store.require("demo"));
        TeammateInfo unsafe=new TeammateInfo("../worker","worker","general-purpose","",TeamBackend.IN_PROCESS,"",temp.resolve(".imiocode/worktrees/worker"),TeammateStatus.STOPPED,false,Instant.now());
        TeammateInfo lead=new TeammateInfo("lead","Lead","lead","",TeamBackend.IN_PROCESS,"lead",temp,TeammateStatus.RUNNING,false,Instant.now());
        assertThrows(IllegalArgumentException.class,()->store.create(new TeamConfig(1,"unsafe","","lead",TeamBackend.AUTO,Map.of("lead",lead,"../worker",unsafe),Instant.now(),Instant.now())));
    }
}
