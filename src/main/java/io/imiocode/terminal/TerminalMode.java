package io.imiocode.terminal;

/** 按终端能力选择的界面复杂度。 */
public enum TerminalMode {
    FULL,
    COMPACT,
    PLAIN;

    public static TerminalMode select(int width, boolean ansiSupported) {
        if (!ansiSupported || width < 36) {
            return PLAIN;
        }
        return width >= 60 ? FULL : COMPACT;
    }
}
