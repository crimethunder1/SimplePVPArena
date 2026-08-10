package de.simplepvparena.border;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorBorderPassLogTest {

    private static final long FIRST_PASS_MILLIS = 1_000L;

    private final OperatorBorderPassLog passLog = new OperatorBorderPassLog();

    @Test
    void startsEmpty() {
        assertTrue(passLog.isEmpty());
        assertEquals(Optional.empty(), passLog.drainReport());
    }

    @Test
    void collectsTheOperatorsInTheOrderTheyFirstWalkedThroughTheBorder() {
        passLog.recordPass("Steve", FIRST_PASS_MILLIS);
        passLog.recordPass("Alex", FIRST_PASS_MILLIS + 10L);
        passLog.recordPass("Steve", FIRST_PASS_MILLIS + 20L);

        List<OperatorBorderPass> passes = passLog.drainPasses();

        assertEquals(List.of("Steve", "Alex"), passes.stream().map(OperatorBorderPass::playerName).toList());
    }

    @Test
    void countsTheRepeatedPassesOfTheSameOperatorAgainstItsTimestamps() {
        passLog.recordPass("Steve", FIRST_PASS_MILLIS);
        passLog.recordPass("Steve", FIRST_PASS_MILLIS + 20L);
        passLog.recordPass("Steve", FIRST_PASS_MILLIS + 90L);

        OperatorBorderPass pass = passLog.drainPasses().getFirst();

        assertEquals("Steve", pass.playerName());
        assertEquals(3, pass.passCount());
        assertEquals(FIRST_PASS_MILLIS, pass.firstPassMillis());
        assertEquals(FIRST_PASS_MILLIS + 90L, pass.lastPassMillis());
    }

    @Test
    void namesEveryOperatorAndHisPassCountInTheReport() {
        passLog.recordPass("Steve", FIRST_PASS_MILLIS);
        passLog.recordPass("Steve", FIRST_PASS_MILLIS + 20L);
        passLog.recordPass("Alex", FIRST_PASS_MILLIS + 30L);

        String report = passLog.drainReport().orElseThrow();

        assertTrue(report.contains("Steve (2 times)"), report);
        assertTrue(report.contains("Alex (1 time)"), report);
    }

    @Test
    void resetsItselfAfterTheReport() {
        passLog.recordPass("Steve", FIRST_PASS_MILLIS);

        assertFalse(passLog.isEmpty());
        assertTrue(passLog.drainReport().isPresent());

        assertTrue(passLog.isEmpty());
        assertEquals(Optional.empty(), passLog.drainReport());
        assertEquals(List.of(), passLog.drainPasses());
    }

    @Test
    void startsTheNextPeriodFromZeroAfterADrain() {
        passLog.recordPass("Steve", FIRST_PASS_MILLIS);
        passLog.drainPasses();

        passLog.recordPass("Steve", FIRST_PASS_MILLIS + 200L);
        OperatorBorderPass pass = passLog.drainPasses().getFirst();

        assertEquals(1, pass.passCount());
        assertEquals(FIRST_PASS_MILLIS + 200L, pass.firstPassMillis());
    }
}
