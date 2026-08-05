package io.imiocode.terminal;

import io.imiocode.config.UiVerbosity;
import io.imiocode.command.CommandStatus;
import org.jline.utils.AttributedString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 不执行终端 I/O 的响应式布局渲染器。 */
public final class TerminalLayout {
    private static final List<String> LOGO = List.of(
            "  ___           _       ____          _      ",
            " |_ _|_ __ ___ (_) ___ / ___|___   __| | ___ ",
            "  | || '_ ` _ \\| |/ _ \\ |   / _ \\ / _` |/ _ \\",
            "  | || | | | | | | (_) | |__| (_) | (_| |  __/",
            " |___|_| |_| |_|_|\\___/ \\____\\___/ \\__,_|\\___|");

    public List<String> welcome(UiContext context, UiState state, int requestedWidth, TerminalMode mode) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(mode, "mode");
        int width = normalizeWidth(requestedWidth);
        if (mode == TerminalMode.PLAIN) {
            return plainWelcome(context, state, width);
        }

        List<String> lines = new ArrayList<>();
        lines.add(topBorder(width));
        if (mode == TerminalMode.FULL) {
            for (String logoLine : LOGO) {
                lines.add(boxLine(logoLine, width));
            }
        }
        lines.add(boxLine(context.productName() + " v" + context.version(), width));
        lines.add(boxLine("Provider  " + context.provider(), width));
        lines.add(boxLine("Model     " + context.model(), width));
        lines.add(boxLine("Directory " + context.workingDirectory(), width));
        lines.add(boxLine("Status    " + state.label(), width));
        lines.add(bottomBorder(width));
        return List.copyOf(lines);
    }

    public String inputTop(int requestedWidth, TerminalMode mode) {
        return inputTop(requestedWidth, mode, UiVerbosity.VERBOSE);
    }

    public String inputTop(int requestedWidth, TerminalMode mode, UiVerbosity verbosity) {
        if (verbosity == UiVerbosity.COMPACT) {
            return "";
        }
        int width = normalizeWidth(requestedWidth);
        if (mode == TerminalMode.PLAIN) {
            return "";
        }
        return truncate("┌─ Send a message " + "─".repeat(Math.max(0, width - 18)), width);
    }

    public String primaryPrompt(TerminalMode mode) {
        return primaryPrompt(mode, UiVerbosity.VERBOSE);
    }

    public String primaryPrompt(TerminalMode mode, UiVerbosity verbosity) {
        if (verbosity == UiVerbosity.COMPACT) {
            return mode == TerminalMode.PLAIN ? "> " : "› ";
        }
        return mode == TerminalMode.PLAIN ? "You> " : "│ › ";
    }

    public String continuationPrompt(TerminalMode mode) {
        return continuationPrompt(mode, UiVerbosity.VERBOSE);
    }

    public String continuationPrompt(TerminalMode mode, UiVerbosity verbosity) {
        if (verbosity == UiVerbosity.COMPACT) {
            return "  ";
        }
        return mode == TerminalMode.PLAIN ? "> " : "│   ";
    }

    public String statusLine(UiContext context, UiState state, int requestedWidth, TerminalMode mode) {
        return statusLine(context, state, requestedWidth, mode, UiVerbosity.VERBOSE);
    }

    public String statusLine(
            UiContext context,
            UiState state,
            int requestedWidth,
            TerminalMode mode,
            UiVerbosity verbosity) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(verbosity, "verbosity");
        if (verbosity == UiVerbosity.COMPACT) {
            return "";
        }
        int width = normalizeWidth(requestedWidth);
        String left = mode == TerminalMode.PLAIN
                ? "chat · " + state.label()
                : "└─ chat · " + state.label();
        String model = context.model();
        int leftWidth = columns(left);
        int modelWidth = columns(model);
        if (leftWidth + modelWidth + 1 <= width) {
            return left + " ".repeat(width - leftWidth - modelWidth) + model;
        }
        if (leftWidth >= width - 1) {
            return truncate(left, width);
        }
        return left + " " + truncate(model, width - leftWidth - 1);
    }

    public String commandStatusLine(
            CommandStatus status,
            UiState state,
            int requestedWidth,
            TerminalMode mode,
            UiVerbosity verbosity) {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(verbosity, "verbosity");
        String agentMode = status.agentMode().name().toLowerCase(java.util.Locale.ROOT);
        String permission = status.permissionMode().name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
        String session = status.session().value();
        if (session.length() > 8) session = session.substring(0, 8);
        String tokens = compactNumber(status.estimatedTokens()) + "/" + compactNumber(status.contextWindowTokens());
        String line = verbosity == UiVerbosity.COMPACT
                ? agentMode + " · " + permission + " · session " + session + " · " + tokens
                : "chat · " + state.label() + " · " + agentMode + " · " + permission
                    + " · session " + session + " · " + tokens + " · MCP "
                    + status.connectedMcpServers() + "/" + status.registeredMcpTools();
        if (mode != TerminalMode.PLAIN && verbosity == UiVerbosity.VERBOSE) line = "└─ " + line;
        return truncate(line, normalizeWidth(requestedWidth));
    }

    public static int columns(String value) {
        return new AttributedString(value == null ? "" : value).columnLength();
    }

    static String truncate(String value, int width) {
        if (width <= 0 || value == null || value.isEmpty()) {
            return "";
        }
        AttributedString attributed = new AttributedString(value);
        if (attributed.columnLength() <= width) {
            return value;
        }
        if (width == 1) {
            return "…";
        }
        return attributed.columnSubSequence(0, width - 1) + "…";
    }

    private static List<String> plainWelcome(UiContext context, UiState state, int width) {
        return List.of(
                truncate(context.productName() + " v" + context.version(), width),
                truncate(context.provider() + " | " + context.model(), width),
                truncate("目录: " + context.workingDirectory(), width),
                truncate("状态: " + state.label(), width));
    }

    private static String topBorder(int width) {
        return width == 1 ? "┌" : "┌" + "─".repeat(width - 2) + "┐";
    }

    private static String bottomBorder(int width) {
        return width == 1 ? "└" : "└" + "─".repeat(width - 2) + "┘";
    }

    private static String boxLine(String content, int width) {
        if (width < 4) {
            return truncate(content, width);
        }
        int innerWidth = width - 4;
        String visible = truncate(content, innerWidth);
        return "│ " + visible + " ".repeat(innerWidth - columns(visible)) + " │";
    }

    private static int normalizeWidth(int width) {
        return width > 0 ? width : 80;
    }

    private static String compactNumber(long value) {
        if (value < 1_000) return Long.toString(value);
        double scaled = value / 1_000.0;
        if (scaled >= 100) return Math.round(scaled) + "k";
        return String.format(java.util.Locale.ROOT, "%.1fk", scaled).replace(".0k", "k");
    }
}
