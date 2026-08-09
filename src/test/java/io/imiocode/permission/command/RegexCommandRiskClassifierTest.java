package io.imiocode.permission.command;

import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegexCommandRiskClassifierTest {
    @TempDir Path workspace;
    private RegexCommandRiskClassifier classifier;

    @BeforeEach
    void setUp() {
        ShellCommandScanner scanner = new ShellCommandScanner();
        ShellCommandTokenizer tokenizer = new ShellCommandTokenizer();
        classifier = new RegexCommandRiskClassifier(
                new StrictSafeCommandDetector(workspace, scanner, tokenizer), scanner, tokenizer);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "git status", "git diff --stat", "rg Agent src", "Get-ChildItem .", "java --version"
    })
    void classifiesStrictQueriesAsLow(String command) {
        assertEquals(ToolRisk.LOW, classifier.classify(command).risk(), command);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "mvn test", "gradle build", "npm test", "go test ./...", "cargo test",
            "dotnet test", "git add src", "git commit -m test", "git switch feature",
            "git merge main", "git pull", "git fetch", "echo hello", "mkdir build",
            "copy source.txt target.txt", "docker build ."
    })
    void classifiesNormalLocalDevelopmentAsMedium(String command) {
        assertEquals(ToolRisk.MEDIUM, classifier.classify(command).risk(), command);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "rm old.txt", "Remove-Item old.txt", "copy /y a.txt b.txt",
            "Copy-Item --force=true a.txt b.txt", "New-Item -Path=C:\\outside.txt",
            "git push origin main",
            "git reset --soft HEAD~1", "git rebase main", "git clean -fd", "git checkout -f main",
            "git branch -D old", "npm publish", "npm install -g pkg", "cargo install tool",
            "mvn deploy", "gradle publish", "apt install curl", "chmod 777 file",
            "choco install git", "icacls file /grant user:F", "reg add HKCU\\Software\\Demo",
            "schtasks /create /tn demo /tr calc", "Set-ExecutionPolicy Bypass",
            "Invoke-Expression secret", "docker run image",
            "echo hello > output.txt", "echo $(whoami)", "python -c print(1)",
            "node --eval process.exit()", "npm exec remote-tool", "npx remote-tool",
            "unknown-tool work"
    })
    void classifiesHighImpactOrUnknownCommandsAsHigh(String command) {
        assertEquals(ToolRisk.HIGH, classifier.classify(command).risk(), command);
    }

    @Test
    void aggregatesCompoundCommandsByHighestRisk() {
        assertEquals(ToolRisk.LOW, classifier.classify("git status && git diff").risk());
        assertEquals(ToolRisk.MEDIUM, classifier.classify("git status && mvn test").risk());
        assertEquals(ToolRisk.HIGH, classifier.classify("mvn test && git push origin main").risk());
        assertEquals(ToolRisk.MEDIUM, classifier.classify("echo \"a;b\"").risk());
    }

    @Test
    void failsClosedAndDoesNotEchoSensitiveCommandInReason() {
        CommandRiskAssessment result = classifier.classify("secret-runner --token super-secret");
        assertEquals(ToolRisk.HIGH, result.risk());
        assertFalse(result.reason().contains("super-secret"));
        assertThrows(IllegalArgumentException.class,
                () -> new CommandRiskAssessment(ToolRisk.LOW, " "));
    }

    @Test
    void classifierFailureFallsBackToHigh() {
        RegexCommandRiskClassifier failing = new RegexCommandRiskClassifier(
                command -> { throw new IllegalStateException("secret failure"); },
                new ShellCommandScanner(), new ShellCommandTokenizer());
        CommandRiskAssessment result = failing.classify("echo safe");
        assertEquals(ToolRisk.HIGH, result.risk());
        assertFalse(result.reason().contains("secret"));
    }
}
