package io.imiocode.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessagePart;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.TextPart;
import io.imiocode.conversation.ThinkingMetadata;
import io.imiocode.conversation.ThinkingPart;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.tool.ToolCall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 将 Provider 原始增量统一为有序事件，并只在完整结束时构造响应。 */
public final class LlmStreamAssembler {
    private final LlmEventListener listener;
    private final ToolCallAssembler tools;
    private final List<MessagePart> parts = new ArrayList<>();
    private final Map<String, Integer> toolIndexes = new HashMap<>();
    private final StringBuilder text = new StringBuilder();
    private final Map<Integer, StringBuilder> thinking = new HashMap<>();
    private boolean completed;

    public LlmStreamAssembler(ObjectMapper objectMapper, LlmEventListener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
        this.tools = new ToolCallAssembler(Objects.requireNonNull(objectMapper, "objectMapper"));
    }

    public void emitText(String delta) throws LlmException {
        ensureActive();
        if (delta == null || delta.isEmpty()) {
            return;
        }
        if (!thinking.isEmpty()) {
            throw protocolError("文本事件到达时仍有未完成内容块");
        }
        text.append(delta);
        listener.onEvent(new LlmEvent.TextDelta(delta));
    }

    public void startThinking(int index) throws LlmException {
        ensureActive();
        flushText();
        if (index < 0 || thinking.putIfAbsent(index, new StringBuilder()) != null) {
            throw protocolError("Thinking 内容块重复或位置无效");
        }
    }

    public void appendThinking(int index, String delta) throws LlmException {
        ensureActive();
        StringBuilder buffer = thinking.get(index);
        if (buffer == null) {
            throw protocolError("Thinking 增量没有对应开始事件");
        }
        if (delta != null && !delta.isEmpty()) {
            buffer.append(delta);
            listener.onEvent(new LlmEvent.ThinkingDelta(index, delta));
        }
    }

    public void completeThinking(int index, ThinkingMetadata metadata) throws LlmException {
        ensureActive();
        StringBuilder buffer = thinking.remove(index);
        if (buffer == null) {
            throw protocolError("Thinking 完成事件没有对应开始事件");
        }
        ThinkingPart part = new ThinkingPart(buffer.toString(), metadata);
        parts.add(part);
        listener.onEvent(new LlmEvent.ThinkingCompleted(index, part));
    }

    public void startTool(int index, String id, String name) throws LlmException {
        ensureActive();
        flushText();
        try {
            tools.start(index, id, name);
            listener.onEvent(new LlmEvent.ToolCallStarted(index, id, name));
        } catch (IllegalArgumentException exception) {
            throw protocolError(exception.getMessage());
        }
    }

    public void appendToolArguments(int index, String jsonFragment) throws LlmException {
        ensureActive();
        String fragment = Objects.requireNonNullElse(jsonFragment, "");
        try {
            tools.appendArguments(index, fragment);
            if (!fragment.isEmpty()) {
                listener.onEvent(new LlmEvent.ToolCallDelta(index, fragment));
            }
        } catch (IllegalArgumentException exception) {
            throw protocolError(exception.getMessage());
        }
    }

    public void ensureEmptyToolArguments(int index) {
        tools.ensureEmptyArguments(index);
    }

    public void completeTool(int index) throws LlmException {
        ensureActive();
        ToolCall call = tools.complete(index);
        ToolCallPart part = new ToolCallPart(call);
        int insertionPoint = parts.size();
        for (int position = 0; position < parts.size(); position++) {
            MessagePart existing = parts.get(position);
            if (existing instanceof ToolCallPart existingTool
                    && toolIndexes.getOrDefault(existingTool.call().id(), Integer.MAX_VALUE) > index) {
                insertionPoint = position;
                break;
            }
        }
        parts.add(insertionPoint, part);
        toolIndexes.put(call.id(), index);
        listener.onEvent(new LlmEvent.ToolCallCompleted(index, call));
    }

    public ChatResponse complete(TokenUsage usage) throws LlmException {
        ensureActive();
        flushText();
        if (!thinking.isEmpty() || tools.hasOpenCalls()) {
            throw protocolError("响应结束时仍有未完成内容块");
        }
        if (parts.isEmpty()) {
            throw protocolError("模型完成响应但没有内容");
        }
        completed = true;
        TokenUsage safeUsage = Objects.requireNonNullElseGet(usage, TokenUsage::unknown);
        ChatResponse response = new ChatResponse(new ChatMessage(MessageRole.ASSISTANT, parts), safeUsage);
        listener.onEvent(new LlmEvent.StreamCompleted(safeUsage));
        return response;
    }

    private void flushText() {
        if (!text.isEmpty()) {
            parts.add(new TextPart(text.toString()));
            text.setLength(0);
        }
    }

    private void ensureActive() throws LlmException {
        if (completed) {
            throw protocolError("响应流已经完成");
        }
    }

    private static LlmException protocolError(String message) {
        return new LlmException(LlmErrorType.PROTOCOL, true, null, message);
    }
}
