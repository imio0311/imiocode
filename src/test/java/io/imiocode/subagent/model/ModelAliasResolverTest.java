package io.imiocode.subagent.model;

import io.imiocode.subagent.config.SubagentConfig;
import io.imiocode.subagent.definition.AgentDefinitionParser;
import io.imiocode.subagent.definition.AgentDefinitionSource;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ModelAliasResolverTest {
    @Test void mapsAliasAndFallsBackObservably() {
        var definition=new AgentDefinitionParser().parse("---\nname: x\ndescription: x\nmodel: haiku\n---\nprompt",
                AgentDefinitionSource.BUILTIN,null);
        var mapped=new ModelAliasResolver(new SubagentConfig(Map.of("haiku","real-haiku"),Set.of(),Set.of(),1,8,8))
                .resolve(definition,"parent");
        assertEquals("real-haiku",mapped.model()); assertTrue(mapped.warning().isEmpty());
        var fallback=new ModelAliasResolver(SubagentConfig.defaults()).resolve(definition,"parent");
        assertEquals("parent",fallback.model()); assertTrue(fallback.warning().isPresent());
        var concrete=new ModelAliasResolver(SubagentConfig.defaults())
                .resolve(definition.withModel("provider-model-v2"),"parent");
        assertEquals("provider-model-v2",concrete.model()); assertTrue(concrete.warning().isEmpty());
    }
}
