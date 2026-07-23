package io.imiocode.conversation;

import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.StreamListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversationSessionTest {
    @Test
    void carriesSuccessfulHistoryIntoSecondTurn() throws Exception {
        FakeClient client = new FakeClient(List.of("第一轮", "IMIO-2749"));
        ConversationSession session = new ConversationSession(client);

        session.send("记住 IMIO-2749", text -> { });
        session.send("验证码是什么", text -> { });

        assertEquals(2, client.requests.size());
        assertEquals(3, client.requests.get(1).messages().size());
        assertEquals("第一轮", client.requests.get(1).messages().get(1).content());
        assertEquals(4, session.historySnapshot().size());
        assertThrows(UnsupportedOperationException.class,
                () -> session.historySnapshot().add(new ChatMessage(MessageRole.USER, "x")));
    }

    @Test
    void doesNotCommitPartialFailedTurn() throws Exception {
        LlmClient client = new LlmClient() {
            @Override
            public ChatResponse streamChat(ChatRequest request, StreamListener listener) throws LlmException {
                listener.onTextDelta("部分");
                throw new LlmException(LlmErrorType.NETWORK, true, null, "断流");
            }

            @Override
            public void close() {
            }
        };
        ConversationSession session = new ConversationSession(client);

        assertThrows(LlmException.class, () -> session.send("失败消息", text -> { }));
        assertEquals(List.of(), session.historySnapshot());
    }

    private static final class FakeClient implements LlmClient {
        private final List<String> responses;
        private final List<ChatRequest> requests = new ArrayList<>();

        private FakeClient(List<String> responses) {
            this.responses = responses;
        }

        @Override
        public ChatResponse streamChat(ChatRequest request, StreamListener listener) {
            requests.add(request);
            String response = responses.get(requests.size() - 1);
            listener.onTextDelta(response);
            return new ChatResponse(response);
        }

        @Override
        public void close() {
        }
    }
}
