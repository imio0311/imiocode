package io.imiocode.subagent.trace;

import io.imiocode.llm.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

class TraceRegistryTest {
    @Test void linksChildrenAndAggregatesAllUsageFields() {
        TraceRegistry traces=new TraceRegistry(); String parent=traces.start("parent",null);
        String child=traces.start("child",parent,"haiku",true);
        traces.addUsage(child,new TokenUsage(OptionalLong.of(10),OptionalLong.of(4),OptionalLong.of(2),OptionalLong.of(7),OptionalLong.of(3)));
        traces.finish(child,TraceStatus.COMPLETED,null);
        assertEquals(java.util.List.of(child),traces.find(parent).orElseThrow().children());
        assertEquals(new TraceTokenUsage(10,4,2,7,3),traces.find(child).orElseThrow().usage());
        assertEquals("haiku",traces.find(child).orElseThrow().model());
        assertTrue(traces.find(child).orElseThrow().background());
    }
}
