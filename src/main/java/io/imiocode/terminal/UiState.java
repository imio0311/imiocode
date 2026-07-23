package io.imiocode.terminal;

/** 终端界面中可观察到的会话状态。 */
public enum UiState {
    READY("Ready"),
    THINKING("Thinking…"),
    STREAMING("Streaming"),
    ERROR("Error");

    private final String label;

    UiState(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
