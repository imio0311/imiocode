package io.imiocode.subagent.trace;

import io.imiocode.llm.TokenUsage;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** 进程内子 Agent 追踪树。 */
public final class TraceRegistry {
    private final Clock clock;
    private final Map<String, MutableTrace> traces = new LinkedHashMap<>();
    public TraceRegistry() { this(Clock.systemUTC()); }
    public TraceRegistry(Clock clock) { this.clock = clock; }

    public synchronized String start(String agentName, String parentId) {
        return start(agentName, parentId, "unknown", false);
    }
    public synchronized String start(String agentName, String parentId, String model, boolean background) {
        String id = UUID.randomUUID().toString();
        MutableTrace trace = new MutableTrace(id, parentId, agentName, model, background, clock.instant());
        traces.put(id, trace);
        if (parentId != null && traces.containsKey(parentId)) traces.get(parentId).children.add(id);
        trace.status = TraceStatus.RUNNING;
        return id;
    }
    public synchronized void addUsage(String id, TokenUsage usage) { require(id).usage = require(id).usage.plus(usage); }
    public synchronized void finish(String id, TraceStatus status, String error) {
        finish(id, status, null, error);
    }
    public synchronized void finish(String id, TraceStatus status, String stopReason, String error) {
        MutableTrace trace = require(id);
        trace.status = status; trace.finishedAt = clock.instant(); trace.stopReason=stopReason; trace.error = error;
    }
    public synchronized Optional<TraceSnapshot> find(String id) {
        return Optional.ofNullable(traces.get(id)).map(this::snapshot);
    }
    public synchronized List<TraceSnapshot> list() {
        return traces.values().stream().map(this::snapshot)
                .sorted(Comparator.comparing(TraceSnapshot::startedAt).reversed()).toList();
    }
    private MutableTrace require(String id) {
        MutableTrace trace = traces.get(id);
        if (trace == null) throw new IllegalArgumentException("未知 trace: " + id);
        return trace;
    }
    private TraceSnapshot snapshot(MutableTrace t) {
        return new TraceSnapshot(t.id, Optional.ofNullable(t.parentId), t.agentName, t.model, t.background, t.status,
                t.startedAt, Optional.ofNullable(t.finishedAt), t.usage, List.copyOf(t.children),
                Optional.ofNullable(t.stopReason),
                Optional.ofNullable(t.error));
    }
    private static final class MutableTrace {
        final String id; final String parentId; final String agentName; final String model; final boolean background; final Instant startedAt;
        final List<String> children = new ArrayList<>();
        TraceStatus status = TraceStatus.PENDING; Instant finishedAt; TraceTokenUsage usage = TraceTokenUsage.zero(); String stopReason,error;
        MutableTrace(String id, String parentId, String agentName, String model, boolean background, Instant startedAt) {
            this.id=id; this.parentId=parentId; this.agentName=agentName; this.model=model; this.background=background; this.startedAt=startedAt;
        }
    }
}
