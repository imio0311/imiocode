package io.imiocode.conversation;

import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.StreamListener;
import io.imiocode.terminal.TerminalUi;
import io.imiocode.terminal.UiContext;
import io.imiocode.terminal.UiState;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationLoopTest {
    @Test
    void ignoresBlankInputStreamsResponseAndExits() {
        FakeClient client = new FakeClient();
        FakeTerminal terminal = new FakeTerminal("", "   ", "hello", "/exit");

        new ConversationLoop(new ConversationSession(client), terminal).run();

        assertEquals(1, client.calls);
        assertEquals("hello", client.requests.getFirst().messages().getFirst().content());
        assertEquals(List.of("你", "好"), terminal.deltas);
        assertEquals(1, terminal.beginCount);
        assertEquals(1, terminal.endCount);
        assertEquals(List.of(
                UiState.THINKING,
                UiState.STREAMING,
                UiState.READY), terminal.states);
        assertTrue(client.closed);
    }

    @Test
    void recoversAfterRecoverableFailure() {
        FakeClient client = new FakeClient();
        client.failFirst = true;
        FakeTerminal terminal = new FakeTerminal("first", "second", "/quit");

        new ConversationLoop(new ConversationSession(client), terminal).run();

        assertEquals(2, client.calls);
        assertEquals(1, terminal.errors.size());
        assertTrue(terminal.errors.get(0).contains("本轮响应未完成"));
        assertEquals(List.of(
                UiState.THINKING,
                UiState.STREAMING,
                UiState.ERROR,
                UiState.THINKING,
                UiState.STREAMING,
                UiState.READY), terminal.states);
    }

    private static final class FakeClient implements LlmClient {
        private int calls;
        private boolean failFirst;
        private boolean closed;
        private final List<ChatRequest> requests = new ArrayList<>();

        @Override
        public ChatResponse streamChat(ChatRequest request, StreamListener listener) throws LlmException {
            calls++;
            requests.add(request);
            if (failFirst && calls == 1) {
                listener.onTextDelta("部分");
                throw new LlmException(LlmErrorType.NETWORK, true, null, "网络错误");
            }
            listener.onTextDelta("你");
            listener.onTextDelta("好");
            return new ChatResponse("你好");
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class FakeTerminal implements TerminalUi {
        private final Deque<String> inputs;
        private final List<String> deltas = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        private final List<UiState> states = new ArrayList<>();
        private int beginCount;
        private int endCount;
        private Runnable interruptHandler;
        private UiState state = UiState.READY;

        private FakeTerminal(String... inputs) {
            this.inputs = new ArrayDeque<>(List.of(inputs));
        }

        @Override
        public void showWelcome(UiContext context) {
        }

        @Override
        public void updateState(UiState state) {
            this.state = state;
            states.add(state);
        }

        @Override
        public UiState state() {
            return state;
        }

        @Override
        public String readLine(String prompt) {
            return inputs.isEmpty() ? null : inputs.removeFirst();
        }

        @Override
        public void beginAssistantResponse() {
            beginCount++;
        }

        @Override
        public void appendAssistantText(String text) {
            deltas.add(text);
        }

        @Override
        public void endAssistantResponse() {
            endCount++;
        }

        @Override
        public void printError(String message) {
            errors.add(message);
        }

        @Override
        public void printInfo(String message) {
        }

        @Override
        public void setInterruptHandler(Runnable handler) {
            interruptHandler = handler;
        }

        @Override
        public void close() {
        }
    }
}
