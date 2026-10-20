package de.simplepvparena.gate;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartGateCountdownSecondsTest {

    private static final String WORLD = "event";

    private final GateServerHarness harness = new GateServerHarness(WORLD);
    private final StartGateMarks gateMarks = new StartGateMarks();
    private final StartGateDuration duration = new StartGateDuration();
    private final StartGateOpeningService openingService =
            new StartGateOpeningService(harness.getPlugin(), gateMarks, duration);

    @Test
    void namesTheSecondsOfTheCountdownThatRuns() {
        duration.setSeconds(7);
        markGate();

        assertTrue(openingService.openAtRoundStart(remainingSeconds -> { }));

        assertEquals(7, openingService.getCountdownSeconds());
    }

    @Test
    void keepsTheSecondsTheCountdownStartedWithAfterAReload() {
        markGate();
        openingService.openAtRoundStart(remainingSeconds -> { });

        duration.setSeconds(4);

        assertEquals(10, openingService.getCountdownSeconds());
    }

    @Test
    void namesNoSecondsWithoutAGate() {
        assertFalse(openingService.openAtRoundStart(remainingSeconds -> { }));

        assertEquals(0, openingService.getCountdownSeconds());
        assertEquals(0, StartGates.NONE.getCountdownSeconds());
    }

    @Test
    void namesNoSecondsOnceTheGateIsDownOrStopped() {
        duration.setSeconds(1);
        markGate();
        openingService.openAtRoundStart(remainingSeconds -> { });

        harness.runTicks(20);

        assertFalse(openingService.isOpening());
        assertEquals(0, openingService.getCountdownSeconds());

        openingService.openAtRoundStart(remainingSeconds -> { });
        openingService.stopAndRestore();

        assertEquals(0, openingService.getCountdownSeconds());
    }

    private void markGate() {
        harness.setBlock(10, 64, 20, Material.BARRIER);
        gateMarks.mark(WORLD, 10, 64, 20, Material.BARRIER);
    }
}
