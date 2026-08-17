package de.simplepvparena.gate;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartGateOpeningScheduleTest {

    private static final List<Integer> DURATIONS = List.of(1, 2, 3, 5, 7, 10, 15, 30, 60);

    @Test
    void countsTwentyTicksPerSecondAndTheConfiguredSeconds() {
        assertEquals(20L, StartGateOpeningSchedule.TICKS_PER_SECOND);
        assertEquals(10, StartGateDuration.DEFAULT_SECONDS, "the shipped countdown is the one it always was");

        assertEquals(200L, StartGateOpeningSchedule.of(4, 10).getTotalDurationTicks());
        assertEquals(100L, StartGateOpeningSchedule.of(4, 5).getTotalDurationTicks());
        assertEquals(5, StartGateOpeningSchedule.of(4, 5).getDurationSeconds());
    }

    @Test
    void givesAGateOfTenLayersOneSecondEachOverTenSeconds() {
        StartGateOpeningSchedule schedule = StartGateOpeningSchedule.of(10, 10);

        assertEquals(List.of(20L, 20L, 20L, 20L, 20L, 20L, 20L, 20L, 20L, 20L), ticksPerLayer(schedule));
        assertEquals(List.of(20L, 40L, 60L, 80L, 100L, 120L, 140L, 160L, 180L, 200L), removalTicks(schedule));
        assertEquals(200L, schedule.getTotalTickCount());
    }

    @Test
    void halvesEveryLayerWhenTheDurationIsHalved() {
        StartGateOpeningSchedule schedule = StartGateOpeningSchedule.of(10, 5);

        assertEquals(List.of(10L, 10L, 10L, 10L, 10L, 10L, 10L, 10L, 10L, 10L), ticksPerLayer(schedule));
        assertEquals(List.of(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L), removalTicks(schedule));
        assertEquals(100L, schedule.getTotalTickCount());
    }

    @Test
    void keepsItsSecondsAtEveryGateHeightAndEveryDuration() {
        for (int durationSeconds : DURATIONS) {
            long totalTicks = durationSeconds * StartGateOpeningSchedule.TICKS_PER_SECOND;
            for (int layerCount : List.of(1, 2, 3, 4, 5, 7, 12, 25, 64)) {
                if (layerCount > totalTicks) {
                    continue;
                }
                StartGateOpeningSchedule schedule = StartGateOpeningSchedule.of(layerCount, durationSeconds);
                String reason = layerCount + " layers over " + durationSeconds + " seconds";

                assertEquals(totalTicks, schedule.getTotalTickCount(),
                        reason + " take the whole countdown and no longer");
                List<Long> ticks = ticksPerLayer(schedule);
                long shortestLayer = ticks.stream().mapToLong(Long::longValue).min().orElseThrow();
                long longestLayer = ticks.stream().mapToLong(Long::longValue).max().orElseThrow();
                assertTrue(longestLayer - shortestLayer <= 1L, reason + " are spread evenly: " + ticks);
                assertTrue(shortestLayer >= StartGateOpeningSchedule.MINIMUM_LAYER_TICKS,
                        reason + " are all at least a tick long: " + ticks);
            }
        }
    }

    @Test
    void spreadsTheTicksThatDoNotDivideOverTheWholeCountdown() {
        StartGateOpeningSchedule threeLayers = StartGateOpeningSchedule.of(3, 10);

        assertEquals(List.of(66L, 67L, 67L), ticksPerLayer(threeLayers));
        assertEquals(List.of(66L, 133L, 200L), removalTicks(threeLayers));

        StartGateOpeningSchedule sevenLayersInThreeSeconds = StartGateOpeningSchedule.of(7, 3);

        assertEquals(60L, sevenLayersInThreeSeconds.getTotalTickCount());
        for (long layerTicks : ticksPerLayer(sevenLayersInThreeSeconds)) {
            assertTrue(layerTicks >= 8L && layerTicks <= 9L,
                    "no layer is worth more than one tick more than another: "
                            + ticksPerLayer(sevenLayersInThreeSeconds));
        }
    }

    @Test
    void holdsAGateOfASingleLayerForTheWholeCountdown() {
        assertEquals(List.of(200L), ticksPerLayer(StartGateOpeningSchedule.of(1, 10)));
        assertEquals(List.of(20L), ticksPerLayer(StartGateOpeningSchedule.of(1, 1)));
        assertEquals(List.of(600L), removalTicks(StartGateOpeningSchedule.of(1, 30)));
    }

    @Test
    void givesEveryLayerAtLeastOneTickWhenTheCountdownIsShorterThanTheGateIsTall() {
        StartGateOpeningSchedule shortCountdown = StartGateOpeningSchedule.of(40, 1);

        for (int layerIndex = 0; layerIndex < shortCountdown.getLayerCount(); layerIndex++) {
            assertEquals(StartGateOpeningSchedule.MINIMUM_LAYER_TICKS, shortCountdown.ticksForLayer(layerIndex));
        }
        assertEquals(40L, shortCountdown.getTotalTickCount(),
                "the countdown is what the opening aims at, the layers are what it is made of");

        StartGateOpeningSchedule absurdlyTallGate = StartGateOpeningSchedule.of(250, 10);

        assertEquals(250L, absurdlyTallGate.getTotalTickCount());
    }

    @Test
    void answersAGateWithoutALayerAndADurationBelowASecondWithTheOneTheyAlwaysHave() {
        StartGateOpeningSchedule withoutLayers = StartGateOpeningSchedule.of(0, 10);

        assertEquals(1, withoutLayers.getLayerCount());
        assertEquals(List.of(200L), ticksPerLayer(withoutLayers));

        StartGateOpeningSchedule withoutSeconds = StartGateOpeningSchedule.of(4, 0);

        assertEquals(StartGateDuration.MINIMUM_SECONDS, withoutSeconds.getDurationSeconds());
        assertEquals(20L, withoutSeconds.getTotalTickCount());
    }

    @Test
    void refusesALayerThatTheGateDoesNotHave() {
        StartGateOpeningSchedule schedule = StartGateOpeningSchedule.of(3, 10);

        assertThrows(IndexOutOfBoundsException.class, () -> schedule.ticksForLayer(3));
        assertThrows(IndexOutOfBoundsException.class, () -> schedule.getRemovalTickOfLayer(-1));
    }

    private static List<Long> ticksPerLayer(StartGateOpeningSchedule schedule) {
        List<Long> ticks = new ArrayList<>(schedule.getLayerCount());
        for (int layerIndex = 0; layerIndex < schedule.getLayerCount(); layerIndex++) {
            ticks.add(schedule.ticksForLayer(layerIndex));
        }
        return ticks;
    }

    private static List<Long> removalTicks(StartGateOpeningSchedule schedule) {
        List<Long> removalTicks = new ArrayList<>(schedule.getLayerCount());
        for (int layerIndex = 0; layerIndex < schedule.getLayerCount(); layerIndex++) {
            removalTicks.add(schedule.getRemovalTickOfLayer(layerIndex));
        }
        return removalTicks;
    }
}
