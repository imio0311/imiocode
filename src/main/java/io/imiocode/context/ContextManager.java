package io.imiocode.context;

import io.imiocode.config.ContextConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.MessageRole;
import io.imiocode.prompt.ApiPayload;
import io.imiocode.prompt.PromptAssembler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 两层上下文管理主编排器：先本地落盘，再按预算生成全量摘要。 */
public final class ContextManager {
    private static final String COMPACTED_PREFIX = "[Compacted conversation summary]\n";
    private static final String ACKNOWLEDGEMENT = "Understood. I'll continue based on this context.";
    private final ContextConfig config;
    private final int defaultOutputTokens;
    private final PromptAssembler promptAssembler;
    private final ApproximateTokenEstimator estimator;
    private final ToolResultOffloader offloader;
    private final ConversationSummarizer summarizer;

    public ContextManager(ContextConfig config, int defaultOutputTokens, PromptAssembler promptAssembler,
                          ApproximateTokenEstimator estimator, ToolResultOffloader offloader,
                          ConversationSummarizer summarizer) {
        this.config = Objects.requireNonNull(config, "config");
        if (defaultOutputTokens <= 0) throw new IllegalArgumentException("defaultOutputTokens 必须为正数");
        this.defaultOutputTokens = defaultOutputTokens;
        this.promptAssembler = Objects.requireNonNull(promptAssembler, "promptAssembler");
        this.estimator = Objects.requireNonNull(estimator, "estimator");
        this.offloader = Objects.requireNonNull(offloader, "offloader");
        this.summarizer = Objects.requireNonNull(summarizer, "summarizer");
    }

    public ContextResult manage(ContextRequest request, ContextEventListener listener) {
        Objects.requireNonNull(request, "request");
        ContextEventListener events = listener == null ? ContextEventListener.NOOP : listener;
        List<ChatMessage> original = combined(request.committedHistory(), request.trajectory());
        if (original.isEmpty()) {
            return new ContextResult(List.of(), List.of(), List.of(), 0, 0, 0, false, ContextOutcome.UNCHANGED);
        }
        long before = estimate(original, request);
        OffloadResult offloaded = offloader.offload(original);
        List<ChatMessage> managed = offloaded.messages();
        int committedBoundary = Math.min(request.committedHistory().size(), managed.size());
        List<ChatMessage> offloadedCommitted = List.copyOf(managed.subList(0, committedBoundary));
        List<ChatMessage> offloadedTrajectory = List.copyOf(managed.subList(committedBoundary, managed.size()));
        long afterOffload = estimate(managed, request);
        if (offloaded.spilledCount() > 0) {
            events.onEvent(new ContextEvent.ResultsOffloaded(request.mode(), offloaded.spilledCount(), afterOffload));
        }

        boolean shouldSummarize = request.mode() != ContextManageMode.AUTO
                || afterOffload >= compactThresholdTokens();
        if (!shouldSummarize) {
            ContextOutcome outcome = offloaded.spilledCount() == 0 ? ContextOutcome.UNCHANGED : ContextOutcome.OFFLOADED;
            return new ContextResult(offloadedCommitted, offloadedTrajectory, managed,
                    before, afterOffload, offloaded.spilledCount(), false, outcome);
        }
        if (request.mode() == ContextManageMode.AUTO && request.trackingState().circuitOpen()) {
            return new ContextResult(offloadedCommitted, offloadedTrajectory, managed,
                    before, afterOffload, offloaded.spilledCount(), false, ContextOutcome.CIRCUIT_OPEN);
        }

        events.onEvent(new ContextEvent.Started(request.mode(), afterOffload));
        try {
            ParsedSummary summary = summarizer.summarize(offloadedCommitted, offloadedTrajectory);
            List<ChatMessage> rollbackHistory = summaryMessages(summary.priorHistory());
            List<ChatMessage> working = summaryMessages(
                    "Prior history:\n" + summary.priorHistory() + "\n\nActive task:\n" + summary.activeTask());
            long afterSummary = estimate(working, request);
            if (afterSummary >= afterOffload) {
                throw new ContextException("摘要未减少上下文，已保留原内容", true);
            }
            request.trackingState().recordSuccess();
            events.onEvent(new ContextEvent.Completed(request.mode(), before, afterSummary));
            return new ContextResult(rollbackHistory, List.of(), working,
                    before, afterSummary, offloaded.spilledCount(), true, ContextOutcome.COMPACTED);
        } catch (ContextException exception) {
            boolean circuitOpened = request.mode() == ContextManageMode.AUTO
                    && request.trackingState().recordFailure();
            events.onEvent(new ContextEvent.Failed(request.mode(), exception.getMessage()));
            if (circuitOpened) {
                events.onEvent(new ContextEvent.CircuitOpened(request.mode(),
                        request.trackingState().consecutiveFailures()));
            }
            return new ContextResult(offloadedCommitted, offloadedTrajectory, managed,
                    before, afterOffload, offloaded.spilledCount(), false,
                    circuitOpened ? ContextOutcome.CIRCUIT_OPEN : ContextOutcome.FAILED);
        }
    }

    public long estimate(List<ChatMessage> messages, ContextRequest request) {
        ApiPayload payload = promptAssembler.assembleApiPayload(new ChatRequest(
                messages, request.reminders(), request.toolSelection(), request.outputTokenLimit()));
        return estimator.estimate(payload, defaultOutputTokens);
    }

    private long compactThresholdTokens() {
        return (long) Math.floor(config.windowTokens() * config.autoCompactThreshold());
    }

    private static List<ChatMessage> combined(List<ChatMessage> first, List<ChatMessage> second) {
        List<ChatMessage> result = new ArrayList<>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return List.copyOf(result);
    }

    private static List<ChatMessage> summaryMessages(String summary) {
        return List.of(
                new ChatMessage(MessageRole.USER, COMPACTED_PREFIX + summary),
                new ChatMessage(MessageRole.ASSISTANT, ACKNOWLEDGEMENT));
    }
}
