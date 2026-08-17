package de.simplepvparena.gate;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartGateOpeningServiceTest {

    private static final String WORLD = "event";

    private static final int[] FIRST_SPAWN_COLUMNS = {10, 11};

    private static final int[] SECOND_SPAWN_COLUMNS = {40, 41};

    private final GateServerHarness harness = new GateServerHarness(WORLD);
    private final StartGateMarks gateMarks = new StartGateMarks();
    private final StartGateDuration duration = new StartGateDuration();
    private final StartGateOpeningService openingService =
            new StartGateOpeningService(harness.getPlugin(), gateMarks, duration);
    private final RecordingDisplay display = new RecordingDisplay();

    @Test
    void takesTheGateApartFromTheTopOverTheConfiguredSeconds() {
        markGate(FIRST_SPAWN_COLUMNS, 64, 66, Material.BARRIER);

        assertTrue(openingService.openAtRoundStart(display));

        assertEquals(List.of(10), display.seconds, "the ten stands over the arena at once");
        harness.runTicks(65);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 66, Material.BARRIER);
        assertEquals(List.of(10, 9, 8, 7), display.seconds, "three seconds are through");

        harness.runTick();
        assertGateLayer(FIRST_SPAWN_COLUMNS, 66, Material.AIR);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 65, Material.BARRIER);

        harness.runTicks(133 - 66);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 65, Material.AIR);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.BARRIER);

        harness.runTicks(199 - 133);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.BARRIER);
        assertEquals(List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), display.seconds,
                "the lowest layer still stands while the last second runs");

        harness.runTick();
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.AIR);
        assertEquals(List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0), display.seconds,
                "the fighters are free on the tick they read Fight!");
        assertEquals(0, harness.getScheduledTaskCount(), "and the run is over");
        assertFalse(openingService.isOpening());
    }

    @Test
    void countsTheConfiguredFiveSecondsInsteadOfTen() {
        duration.setSeconds(5);
        markGate(FIRST_SPAWN_COLUMNS, 64, 66, Material.BARRIER);

        assertTrue(openingService.openAtRoundStart(display));

        assertEquals(List.of(5), display.seconds);
        harness.runTicks(33);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 66, Material.AIR);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 65, Material.BARRIER);

        harness.runTicks(100 - 33);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 65, Material.AIR);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.AIR);
        assertEquals(List.of(5, 4, 3, 2, 1, 0), display.seconds);
        assertEquals(0, harness.getScheduledTaskCount());
    }

    @Test
    void countsASingleConfiguredSecond() {
        duration.setSeconds(1);
        markGate(FIRST_SPAWN_COLUMNS, 64, 65, Material.BARRIER);

        assertTrue(openingService.openAtRoundStart(display));
        harness.runTicks(20);

        assertGateLayer(FIRST_SPAWN_COLUMNS, 65, Material.AIR);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.AIR);
        assertEquals(List.of(1, 0), display.seconds);
        assertEquals(0, harness.getScheduledTaskCount());
    }

    @Test
    void keepsTheSecondsARunningCountdownStartedWith() {
        markGate(FIRST_SPAWN_COLUMNS, 64, 65, Material.BARRIER);
        openingService.openAtRoundStart(display);
        harness.runTicks(60);

        duration.setSeconds(2);
        harness.runTicks(140);

        assertEquals(List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0), display.seconds,
                "the running round keeps its ten");
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.AIR);

        display.seconds.clear();
        openingService.openAtRoundStart(display);
        harness.runTicks(40);

        assertEquals(List.of(2, 1, 0), display.seconds, "and the next round is the two of the file");
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.AIR);
    }

    @Test
    void putsTheGateBackAtEveryRoundStart() {
        markGate(FIRST_SPAWN_COLUMNS, 64, 66, Material.BARRIER);
        openingService.openAtRoundStart(display);
        harness.runTicks(200);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.AIR);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 66, Material.AIR);
        display.seconds.clear();

        assertTrue(openingService.openAtRoundStart(display));

        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.BARRIER);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 65, Material.BARRIER);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 66, Material.BARRIER);
        assertEquals(List.of(10), display.seconds, "and the countdown starts at ten again");

        harness.runTicks(200);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 66, Material.AIR);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.AIR);
        assertEquals(11, display.seconds.size());
    }

    @Test
    void startsTheNextRoundWithAWholeGateEvenInTheMiddleOfACountdown() {
        markGate(FIRST_SPAWN_COLUMNS, 64, 66, Material.BARRIER);
        openingService.openAtRoundStart(display);
        harness.runTicks(140);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 65, Material.AIR);

        openingService.openAtRoundStart(display);

        assertGateLayer(FIRST_SPAWN_COLUMNS, 65, Material.BARRIER);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 66, Material.BARRIER);
        assertEquals(1, harness.getScheduledTaskCount(), "the countdown of the round before it is gone");

        harness.runTicks(200);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.AIR);
        assertEquals(0, harness.getScheduledTaskCount());
    }

    @Test
    void takesTheGateOfBothSpawnsApartInTheSameTicks() {
        markGate(FIRST_SPAWN_COLUMNS, 64, 65, Material.BARRIER);
        markGate(SECOND_SPAWN_COLUMNS, 64, 67, Material.GLASS);

        assertTrue(openingService.openAtRoundStart(display));
        assertEquals(1, harness.getScheduledTaskCount(), "one run, one gate");

        harness.runTicks(50);
        assertGateLayer(SECOND_SPAWN_COLUMNS, 67, Material.AIR);
        assertGateLayer(SECOND_SPAWN_COLUMNS, 66, Material.GLASS);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 65, Material.BARRIER);

        harness.runTicks(100);
        assertGateLayer(SECOND_SPAWN_COLUMNS, 65, Material.AIR);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 65, Material.AIR, "the same height goes at both spawns at once");
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.BARRIER);
        assertGateLayer(SECOND_SPAWN_COLUMNS, 64, Material.GLASS);

        harness.runTicks(50);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.AIR);
        assertGateLayer(SECOND_SPAWN_COLUMNS, 64, Material.AIR);
        assertEquals(List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0), display.seconds);
    }

    @Test
    void leavesEveryBlockThatIsNoPartOfTheGateAlone() {
        markGate(FIRST_SPAWN_COLUMNS, 64, 65, Material.BARRIER);
        harness.setBlock(SECOND_SPAWN_COLUMNS[0], 64, 20, Material.GLASS);

        assertTrue(openingService.openAtRoundStart(display));
        harness.runTicks(200);

        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.AIR);
        assertEquals(Material.GLASS, harness.getBlock(SECOND_SPAWN_COLUMNS[0], 64, 20));
        assertEquals(List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0), display.seconds);
    }

    @Test
    void doesNothingAtAllWhenNoGateIsPlaced() {
        harness.setBlock(10, 64, 20, Material.STONE);

        assertFalse(openingService.openAtRoundStart(display));

        assertEquals(0, harness.getScheduledTaskCount());
        assertEquals(List.of(), display.seconds);
        assertEquals(Material.STONE, harness.getBlock(10, 64, 20));
        assertFalse(openingService.isOpening());
    }

    @Test
    void putsTheGateBackWhenAMatchEndsInTheMiddleOfTheCountdown() {
        markGate(FIRST_SPAWN_COLUMNS, 64, 66, Material.BARRIER);
        openingService.openAtRoundStart(display);
        harness.runTicks(140);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 66, Material.AIR);

        openingService.stopAndRestore();

        assertGateLayer(FIRST_SPAWN_COLUMNS, 66, Material.BARRIER);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 65, Material.BARRIER);
        assertGateLayer(FIRST_SPAWN_COLUMNS, 64, Material.BARRIER);
        assertEquals(0, harness.getScheduledTaskCount());
        assertFalse(openingService.isOpening());
    }

    @Test
    void stopsItselfWhenATickThrows() {
        markGate(FIRST_SPAWN_COLUMNS, 64, 66, Material.BARRIER);
        openingService.openAtRoundStart(display);
        harness.failBlockAccessWith(new IllegalStateException("the chunk is gone"));

        harness.runTicks(200);

        assertEquals(0, harness.getScheduledTaskCount());
        assertFalse(openingService.isOpening());
    }

    @Test
    void leavesAGateOfAWorldThatIsNotLoadedAlone() {
        markGate(FIRST_SPAWN_COLUMNS, 64, 66, Material.BARRIER);
        harness.unloadWorld();

        assertTrue(openingService.openAtRoundStart(display));
        harness.runTicks(200);

        assertEquals(0, harness.getScheduledTaskCount());
        assertEquals(3, gateMarks.getLayerCount(WORLD), "and the marks are untouched");
    }

    private void markGate(int[] columns, int lowestY, int highestY, Material material) {
        for (int blockY = lowestY; blockY <= highestY; blockY++) {
            for (int blockX : columns) {
                harness.setBlock(blockX, blockY, 20, material);
                gateMarks.mark(WORLD, blockX, blockY, 20, material);
            }
        }
    }

    private void assertGateLayer(int[] columns, int blockY, Material material) {
        assertGateLayer(columns, blockY, material, "the layer at y " + blockY);
    }

    private void assertGateLayer(int[] columns, int blockY, Material material, String reason) {
        for (int blockX : columns) {
            assertEquals(material, harness.getBlock(blockX, blockY, 20), reason);
        }
    }

    private static final class RecordingDisplay implements StartGateCountdownDisplay {

        private final List<Integer> seconds = new ArrayList<>();

        @Override
        public void show(int remainingSeconds) {
            seconds.add(remainingSeconds);
        }
    }
}
