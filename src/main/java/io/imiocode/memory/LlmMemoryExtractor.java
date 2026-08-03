package io.imiocode.memory;

import io.imiocode.config.MemoryConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.MessageRole;
import io.imiocode.llm.LlmClient;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolSelection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/** 仅在用户显式启用 auto-extract 后，复用脱敏后的本轮用户/助手文本。 */
public final class LlmMemoryExtractor implements MemoryExtractor {
    private static final String SYSTEM_PROMPT = """
            你是 ImioCode 的长期记忆提取器。只保留稳定用户偏好、可复用项目事实和明确长期决策。
            禁止保存密钥、令牌、密码、个人敏感信息、临时任务状态和大段原文。
            只输出：<memories>{"items":[{"scope":"user","category":"preference","content":"...","replaces_id":null}]}</memories>
            没有值得保存的信息时输出：<memories>{"items":[]}</memories>
            """;
    private final LlmClient client;
    private final MemoryConfig config;
    private final MemoryResponseParser parser;
    private final SecretRedactor redactor;

    public LlmMemoryExtractor(LlmClient client, MemoryConfig config,
                              MemoryResponseParser parser, SecretRedactor redactor) {
        this.client = client; this.config = config; this.parser = parser; this.redactor = redactor;
    }

    @Override
    public MemoryExtractionResult extract(ChatMessage userMessage, List<ChatMessage> trajectory,
                                          List<MemoryDocument> currentMemory) {
        if (!config.autoExtract()) return new MemoryExtractionResult(List.of(), List.of());
        try {
            String assistantText = trajectory.stream()
                    .filter(message -> message.role() == MessageRole.ASSISTANT)
                    .map(ChatMessage::content).filter(value -> !value.isBlank())
                    .reduce((left, right) -> right).orElse("");
            String turn = "<completed_turn>\n<user>" + escape(redactor.redact(userMessage.content()))
                    + "</user>\n<assistant>" + escape(redactor.redact(assistantText))
                    + "</assistant>\n</completed_turn>";
            ChatRequest request = new ChatRequest(List.of(new ChatMessage(MessageRole.USER, turn)), List.of(),
                    ToolSelection.only(Set.of()), OptionalInt.of(config.extractionOutputTokens()), Optional.of(SYSTEM_PROMPT));
            String response = client.streamChat(request, ignored -> { }).text();
            return new MemoryExtractionResult(parser.parse(response), List.of());
        } catch (Exception exception) { return MemoryExtractionResult.warning("自动记忆提取失败"); }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
