package io.imiocode.skill;

import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandRegistry;
import io.imiocode.command.CommandServices;
import io.imiocode.command.CommandStatus;
import io.imiocode.command.ConfirmationPrompt;
import io.imiocode.command.UIController;
import io.imiocode.config.UiVerbosity;
import io.imiocode.skill.install.SkillInstallResult;
import io.imiocode.skill.install.SkillInstallStage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillManagementCommandTest {
    @Test
    void installSubcommandUsesLocalSharedServiceAndForwardsForce() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> url = new AtomicReference<>();
        AtomicBoolean force = new AtomicBoolean();
        CommandServices services = (CommandServices) Proxy.newProxyInstance(
                CommandServices.class.getClassLoader(), new Class<?>[]{CommandServices.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("installSkill")) {
                        calls.incrementAndGet();
                        url.set((String) arguments[0]);
                        force.set((boolean) arguments[1]);
                        var listener = (io.imiocode.skill.install.SkillInstallListener) arguments[2];
                        listener.onStage(SkillInstallStage.QUEUED, "Skill 安装已排队");
                        listener.onStage(SkillInstallStage.COMPLETED, "Skill 安装完成: demo");
                        return new SkillInstallResult("demo", Path.of(".imiocode/skills/demo"),
                                SkillOrigin.PROJECT, force.get(),
                                List.of(SkillInstallStage.QUEUED, SkillInstallStage.COMPLETED));
                    }
                    if (method.getName().equals("skillCatalog")) return SkillCatalogSnapshot.empty();
                    if (method.getDeclaringClass() == Object.class) return method.invoke(this, arguments);
                    throw new AssertionError("unexpected service call: " + method.getName());
                });
        CommandContext context = new CommandContext(services, new NoopUi(), new CommandRegistry());
        SkillManagementCommand command = new SkillManagementCommand();

        var normal = command.execute(context,
                List.of("install", "https://skills.sh/acme/repo/demo"));
        assertEquals(1, calls.get());
        assertEquals("https://skills.sh/acme/repo/demo", url.get());
        assertTrue(normal.messages().stream().noneMatch(message -> message.text().contains("排队")));
        assertTrue(normal.messages().stream().anyMatch(message -> message.text().contains("安装完成")));
        assertTrue(normal.messages().stream().anyMatch(message -> message.text().contains("可立即使用 /demo")));

        command.execute(context,
                List.of("install", "https://skills.sh/acme/repo/demo", "--force"));
        assertEquals(2, calls.get());
        assertTrue(force.get());
        assertThrows(IllegalArgumentException.class, () -> command.execute(context,
                List.of("install", "https://skills.sh/acme/repo/demo", "--unknown")));
    }

    private static final class NoopUi implements UIController {
        @Override public void clearScreen() { }
        @Override public boolean confirm(ConfirmationPrompt prompt) { return true; }
        @Override public UiVerbosity verbosity() { return UiVerbosity.COMPACT; }
        @Override public void setVerbosity(UiVerbosity verbosity) { }
        @Override public void refreshStatus(CommandStatus status) { }
    }
}
