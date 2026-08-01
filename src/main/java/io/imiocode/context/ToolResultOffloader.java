package io.imiocode.context;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.MessagePart;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 第一层上下文管理：大结果落盘并在对话中保留安全预览。 */
public final class ToolResultOffloader {
    private static final String MARKER = "[Result of ";
    private final ToolResultSpillStore store;
    private final SecretRedactor redactor;

    public ToolResultOffloader(ToolResultSpillStore store, SecretRedactor redactor) {
        this.store = Objects.requireNonNull(store, "store");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
    }

    public OffloadResult offload(List<ChatMessage> messages) {
        Objects.requireNonNull(messages, "messages");
        MutableCount count = new MutableCount();
        List<ChatMessage> afterSingle = transform(messages, part -> contentLength(part) > ContextPolicy.SINGLE_RESULT_SPILL_CHARS,
                1_000, count);
        int aggregate = aggregateCharacters(afterSingle);
        if (aggregate <= ContextPolicy.AGGREGATE_RESULT_SPILL_CHARS) {
            return new OffloadResult(afterSingle, count.value);
        }
        Set<Integer> protectedToolMessages = newestToolMessageIndexes(afterSingle);
        List<ChatMessage> afterAggregate = transformIndexed(afterSingle,
                (index, part) -> !protectedToolMessages.contains(index) && !alreadyOffloaded(part),
                ContextPolicy.OLD_RESULT_PREVIEW_CHARS, count);
        return new OffloadResult(afterAggregate, count.value);
    }

    private List<ChatMessage> transform(List<ChatMessage> messages, PartPredicate predicate, int previewChars, MutableCount count) {
        return transformIndexed(messages, (index, part) -> predicate.test(part), previewChars, count);
    }

    private List<ChatMessage> transformIndexed(List<ChatMessage> messages, IndexedPartPredicate predicate, int previewChars, MutableCount count) {
        List<ChatMessage> output = new ArrayList<>(messages.size());
        for (int index = 0; index < messages.size(); index++) {
            ChatMessage message = messages.get(index);
            if (message.role() != MessageRole.TOOL) { output.add(message); continue; }
            List<MessagePart> parts = new ArrayList<>(message.parts().size());
            for (MessagePart raw : message.parts()) {
                ToolResultPart part = (ToolResultPart) raw;
                if (!alreadyOffloaded(part) && predicate.test(index, part)) {
                    ToolResultPart replacement = tryOffload(part, previewChars);
                    if (replacement != part) count.value++;
                    parts.add(replacement);
                } else parts.add(part);
            }
            output.add(parts.equals(message.parts()) ? message : new ChatMessage(MessageRole.TOOL, parts));
        }
        return List.copyOf(output);
    }

    private ToolResultPart tryOffload(ToolResultPart part, int previewChars) {
        try {
            SpilledResult spilled = store.spill(part);
            String source = part.result().success() ? part.result().output()
                    : part.result().error().isEmpty() ? part.result().output() : part.result().error();
            String preview = redactor.redact(source.substring(0, Math.min(previewChars, source.length())));
            String placeholder = MARKER + part.toolName() + " with " + spilled.originalCharacters()
                    + " chars saved to " + spilled.unixPath() + " — read with read_file]\nPreview:\n" + preview;
            ToolResult original = part.result();
            ToolResult replacement = original.success()
                    ? new ToolResult(true, placeholder, "", original.truncated(), original.duration(), original.exitCode())
                    : new ToolResult(false, "", placeholder, original.truncated(), original.duration(), original.exitCode());
            return new ToolResultPart(part.callId(), part.toolName(), replacement);
        } catch (IOException | RuntimeException exception) {
            return part;
        }
    }

    private static int aggregateCharacters(List<ChatMessage> messages) {
        long total = 0;
        for (ChatMessage message : messages) for (MessagePart raw : message.parts())
            if (raw instanceof ToolResultPart part && !alreadyOffloaded(part))
                total = Math.min(Integer.MAX_VALUE, total + contentLength(part));
        return (int) total;
    }

    private static Set<Integer> newestToolMessageIndexes(List<ChatMessage> messages) {
        Set<Integer> indexes = new HashSet<>();
        for (int i = messages.size() - 1; i >= 0 && indexes.size() < ContextPolicy.RECENT_TOOL_MESSAGES_TO_KEEP; i--)
            if (messages.get(i).role() == MessageRole.TOOL) indexes.add(i);
        return indexes;
    }

    private static int contentLength(ToolResultPart part) {
        return part.result().output().length() + part.result().error().length();
    }

    private static boolean alreadyOffloaded(ToolResultPart part) {
        return part.result().output().startsWith(MARKER) || part.result().error().startsWith(MARKER);
    }

    private interface PartPredicate { boolean test(ToolResultPart part); }
    private interface IndexedPartPredicate { boolean test(int index, ToolResultPart part); }
    private static final class MutableCount { private int value; }
}
