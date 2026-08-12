package io.imiocode.team;

import io.imiocode.config.ConfigLoader;
import io.imiocode.team.model.TeamBackend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TeamConfigLoaderTest {
    @TempDir Path temp;
    @Test void loadsTeamsSectionAndKeepsCoordinatorDefaultClosed() throws Exception {
        Files.writeString(temp.resolve("config.yaml"),"""
                provider: openai
                model: test-model
                providers:
                  openai:
                    api-key: test-key
                teams:
                  backend: in-process
                  coordinator-enabled: true
                  max-members-per-team: 3
                """);
        var runtime=new ConfigLoader().loadAll(temp,temp,Map.of());
        assertEquals(TeamBackend.IN_PROCESS,runtime.teams().preferredBackend());
        assertTrue(runtime.teams().coordinatorEnabled());
        assertEquals(3,runtime.teams().maxMembersPerTeam());
        assertFalse(new io.imiocode.team.coordinator.CoordinatorModeController(
                runtime.teams().coordinatorEnabled(),Map.of()).snapshot().active());
    }
}
