package io.imiocode.team;

import io.imiocode.team.model.TeamPrincipal;
import io.imiocode.team.model.TeamRole;
import io.imiocode.team.tool.TeamToolContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamToolContextTest {
    @Test void lifecycleCallbacksHideToolsUntilARealTeamIsSelected() {
        TeamToolContext context=new TeamToolContext();AtomicBoolean visible=new AtomicBoolean(true);
        context.configureLifecycle(()->visible.set(true),()->visible.set(false));
        assertFalse(visible.get());
        context.select(new TeamPrincipal("demo","lead",TeamRole.LEAD));
        assertTrue(visible.get());
        context.clear("other");assertTrue(visible.get());
        context.clear("demo");assertFalse(visible.get());
    }
}
