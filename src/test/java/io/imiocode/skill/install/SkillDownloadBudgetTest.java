package io.imiocode.skill.install;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillDownloadBudgetTest {
    private static SkillInstallConfig limits() {
        return new SkillInstallConfig(Duration.ofSeconds(1), 2, 4, 8,
                SkillInstallConfig.TRUSTED_HOSTS);
    }

    @Test
    void enforcesFileAndByteBoundaries() {
        SkillDownloadBudget budget = new SkillDownloadBudget(limits());
        budget.claimFile();
        budget.consumeFileBytes(4);
        budget.claimFile();
        budget.consumeFileBytes(4);
        assertEquals(2, budget.files());
        assertEquals(8, budget.bytes());
        assertThrows(SkillInstallException.class, budget::claimFile);
    }

    @Test
    void rejectsSingleFileAndTotalOverflowAndCancellation() {
        SkillDownloadBudget single = new SkillDownloadBudget(limits());
        assertThrows(SkillInstallException.class, () -> single.consumeFileBytes(5));

        SkillDownloadBudget total = new SkillDownloadBudget(limits());
        total.consumeFileBytes(4);
        total.consumeFileBytes(4);
        assertThrows(SkillInstallException.class, () -> total.consumeFileBytes(1));

        SkillDownloadBudget cancelled = new SkillDownloadBudget(limits());
        cancelled.cancel();
        assertThrows(SkillInstallException.class, cancelled::claimFile);
    }
}
