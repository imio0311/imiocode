package io.imiocode.team;

import io.imiocode.team.persistence.TeamPaths;
import io.imiocode.team.task.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class TeamTaskGraphTest {
    @TempDir Path temp;
    @Test void maintainsBothDependencyDirectionsAndRejectsCycles(){TeamTaskStore store=new TeamTaskStore(new TeamPaths(temp),20);TeamTask a=store.create("demo","A","","");TeamTask b=store.create("demo","B","","");TeamTask next=store.addBlocksOn("demo",a.id(),b.id(),a.version());assertTrue(next.blocksOn().contains(b.id()));assertTrue(store.require("demo",b.id()).blockedBy().contains(a.id()));assertThrows(TeamException.class,()->store.addBlocksOn("demo",b.id(),a.id(),null));assertThrows(TeamException.class,()->store.addBlocksOn("demo",a.id(),a.id(),null));}
    @Test void optimisticVersionPreventsLostUpdate(){TeamTaskStore store=new TeamTaskStore(new TeamPaths(temp),20);TeamTask t=store.create("demo","A","","");store.update("demo",t.id(),TeamTaskStatus.RUNNING,null,null,t.version());assertThrows(TeamException.class,()->store.update("demo",t.id(),TeamTaskStatus.COMPLETED,null,null,t.version()));}
    @Test void combinedUpdateAndDependencyIsAtomic(){TeamTaskStore store=new TeamTaskStore(new TeamPaths(temp),20);TeamTask a=store.create("demo","A","","");TeamTask b=store.create("demo","B","","");TeamTask updated=store.update("demo",a.id(),TeamTaskStatus.RUNNING,"worker","result",b.id(),null,a.version());assertEquals(TeamTaskStatus.RUNNING,updated.status());assertTrue(updated.blocksOn().contains(b.id()));assertTrue(store.require("demo",b.id()).blockedBy().contains(a.id()));}
}
