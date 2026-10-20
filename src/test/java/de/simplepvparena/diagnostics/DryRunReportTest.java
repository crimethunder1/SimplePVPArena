package de.simplepvparena.diagnostics;

import de.simplepvparena.message.TournamentMessages;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DryRunReportTest {

    @Test
    void summarizesARunThatFoundNothingWrong() {
        DryRunReport report = DryRunReport.of(List.of(
                DryRunStep.passed("First", "one"),
                DryRunStep.passed("Second", "two")));

        assertTrue(report.isSuccessful());
        assertFalse(report.isRejected());
        assertEquals("Dry run of the tournament pipeline: 2 of 2 steps passed", report.describe().get(0));
    }

    @Test
    void countsASkippedStepWithoutTurningTheReportRed() {
        DryRunReport report = DryRunReport.of(List.of(
                DryRunStep.passed("First", "one"),
                DryRunStep.skipped("Second", "nothing to look at")));

        assertTrue(report.isSuccessful());
        assertEquals("Dry run of the tournament pipeline: 1 of 2 steps passed, 1 skipped",
                report.describe().get(0));
    }

    @Test
    void namesTheFailuresAndTheSkipsOfARunThatWentWrong() {
        DryRunReport report = DryRunReport.of(List.of(
                DryRunStep.passed("First", "one"),
                DryRunStep.failed("Second", "it broke"),
                DryRunStep.skipped("Third", "nothing to look at")));

        assertFalse(report.isSuccessful());
        assertEquals("Dry run of the tournament pipeline: 1 of 3 steps passed, 1 failed, 1 skipped",
                report.describe().get(0));
        assertEquals(1L, report.countByStatus(DryRunStatus.FAILED));
    }

    @Test
    void writesEveryStepWithItsOutcomeAndItsLines() {
        DryRunReport report = DryRunReport.of(List.of(
                DryRunStep.passed("Arena", "16 blocks", "both spawns set"),
                DryRunStep.failed("Kit", "the kit is empty")));

        List<String> lines = report.describe();

        assertEquals("  [ok] Arena", lines.get(1));
        assertEquals("    16 blocks", lines.get(2));
        assertEquals("    both spawns set", lines.get(3));
        assertEquals("  [FAILED] Kit", lines.get(4));
        assertEquals("    the kit is empty", lines.get(5));
    }

    @Test
    void alwaysEndsWithWhatOnlyARealTestPlayerCanShow() {
        List<String> lines = DryRunReport.of(List.of(DryRunStep.passed("First", "one"))).describe();

        assertEquals(DryRunReport.describeRealPlayerNote(),
                lines.subList(lines.size() - DryRunReport.describeRealPlayerNote().size(), lines.size()));
    }

    @Test
    void writesARefusedRunAsItsReasonAlone() {
        DryRunReport report = DryRunReport.rejected(
                "A match stands on the arena" + TournamentMessages.LINE_BREAK + "Decide it first");

        assertTrue(report.isRejected());
        assertFalse(report.isSuccessful());
        assertEquals(List.of("The dry run was not started", "A match stands on the arena",
                "Decide it first"), report.describe());
    }
}
