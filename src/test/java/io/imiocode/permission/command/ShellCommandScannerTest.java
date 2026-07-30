package io.imiocode.permission.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShellCommandScannerTest {
    private final ShellCommandScanner scanner = new ShellCommandScanner();

    @Test
    void splitsEverySupportedOperatorOutsideQuotes() {
        ShellCommandScanResult result =
                scanner.scan("git status && java -version | grep 21 || uname; pwd");

        assertTrue(result.eligible());
        assertEquals(List.of(
                "git status",
                "java -version",
                "grep 21",
                "uname",
                "pwd"), result.segments());
    }

    @Test
    void treatsNewlinesAsCommandBoundaries() {
        ShellCommandScanResult result = scanner.scan(
                "git status\r\njava -version\nGet-Date");

        assertTrue(result.eligible());
        assertEquals(
                List.of("git status", "java -version", "Get-Date"),
                result.segments());
    }

    @Test
    void preservesOperatorsInsideQuotes() {
        ShellCommandScanResult result =
                scanner.scan("grep 'a|b;c' file && Select-String \"x||y\" file");

        assertTrue(result.eligible());
        assertEquals(List.of(
                "grep 'a|b;c' file",
                "Select-String \"x||y\" file"), result.segments());
    }

    @Test
    void rejectsSideEffectAndCommandSubstitutionSyntax() {
        assertIneligible("cat file > output");
        assertIneligible("cat file < input");
        assertIneligible("git status &");
        assertIneligible("echo $(whoami)");
        assertIneligible("echo `whoami`");
        assertIneligible("echo \"$(whoami)\"");
        assertIneligible("echo \"`whoami`\"");
        assertIneligible("(git status)");
        assertIneligible("Get-Content @(Get-ChildItem)");
    }

    @Test
    void rejectsMalformedOrEmptySegments() {
        assertIneligible("git status &&");
        assertIneligible("&& git status");
        assertIneligible("git status ||| java -version");
        assertIneligible("grep 'unfinished");
        assertIneligible("   ");
    }

    @Test
    void permitsQuotedRedirectionCharactersAsPlainText() {
        ShellCommandScanResult result = scanner.scan("grep 'a>b' file");

        assertTrue(result.eligible());
        assertFalse(result.segments().isEmpty());
    }

    private void assertIneligible(String command) {
        assertFalse(scanner.scan(command).eligible(), command);
    }
}
