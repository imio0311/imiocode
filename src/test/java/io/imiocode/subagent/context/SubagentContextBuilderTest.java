package io.imiocode.subagent.context;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.MessageRole;
import io.imiocode.subagent.definition.AgentDefinitionParser;
import io.imiocode.subagent.definition.AgentDefinitionSource;
import io.imiocode.subagent.runtime.SubagentRunMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubagentContextBuilderTest {
    @Test void definitionStartsFreshWhileForkKeepsParentAsExactPrefix() {
        var definition=new AgentDefinitionParser().parse("""
                ---
                name: custom
                description: custom
                initialPrompt: initialize first
                ---
                definition body
                """, AgentDefinitionSource.PROJECT,null);
        List<ChatMessage> parent=List.of(new ChatMessage(MessageRole.USER,"parent"));
        SubagentContextBuilder builder=new SubagentContextBuilder();
        List<ChatMessage> defined=builder.history(definition,parent,SubagentRunMode.DEFINITION);
        assertEquals(2,defined.size());
        assertTrue(defined.get(0).content().contains("definition body"));
        assertTrue(defined.get(1).content().contains("initialize first"));
        List<ChatMessage> forked=builder.history(definition,parent,SubagentRunMode.FORK);
        assertEquals(parent.get(0),forked.get(0));
        assertEquals("parent",forked.get(0).content());
        assertTrue(forked.get(1).content().contains("system-reminder"));
    }
}
