package io.imiocode.command;

import io.imiocode.config.UiVerbosity;

/** 命令可使用的最小 UI 能力，避免依赖 JLine。 */
public interface UIController {
    void clearScreen();

    boolean confirm(ConfirmationPrompt prompt);

    UiVerbosity verbosity();

    void setVerbosity(UiVerbosity verbosity);

    void refreshStatus(CommandStatus status);
}
