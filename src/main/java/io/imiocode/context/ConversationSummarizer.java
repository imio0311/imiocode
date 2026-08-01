package io.imiocode.context;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessageRole;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmException;
import io.imiocode.tool.ToolSelection;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** 使用同一 LLM 发起一次无工具、静默的结构化摘要请求。 */
public final class ConversationSummarizer {
    private final LlmClient client;
    private final ConversationSerializer serializer;
    private final SummaryParser parser;

    public ConversationSummarizer(LlmClient client, ConversationSerializer serializer, SummaryParser parser) {
        this.client = Objects.requireNonNull(client, "client");
        this.serializer = Objects.requireNonNull(serializer, "serializer");
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public ParsedSummary summarize(List<ChatMessage> priorHistory, List<ChatMessage> activeTask)
            throws ContextException {
        String serialized = serializer.serialize(priorHistory, activeTask);
        ChatRequest request = new ChatRequest(
                List.of(new ChatMessage(MessageRole.USER, serialized)),
                List.of(), ToolSelection.only(java.util.Set.of()),
                OptionalInt.of(ContextPolicy.SUMMARY_OUTPUT_TOKENS), Optional.of(SummaryPrompt.SYSTEM));
        try {
            ChatResponse response = client.streamChat(request, event -> { });
            if (response.hasToolCalls() || response.text().isBlank()) {
                throw new ContextException("摘要响应无效，已保留原上下文", true);
            }
            return parser.parse(response.text());
        } catch (LlmException exception) {
            throw new ContextException("上下文摘要请求失败，已保留原上下文", true, exception);
        }
    }
}
