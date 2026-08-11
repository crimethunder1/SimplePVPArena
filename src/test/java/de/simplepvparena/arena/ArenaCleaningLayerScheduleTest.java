package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaCleaningLayerScheduleTest {

    @Test
    void splitsTheConfiguredTimeInHalfBetweenTheTwoPhases() {
        ArenaCleaningLayerSchedule schedule = ArenaCleaningLayerSchedule.of(4, 80L);

        assertEquals(40L, schedule.getPhaseDurationTicks(), "half of the configured time per phase");
        assertEquals(List.of(10L, 10L, 10L, 10L), ticksPerLayer(schedule));
        assertEquals(40L, schedule.getPhaseTickCount());
        assertEquals(80L, schedule.getTotalTickCount(), "and both phases together are the whole time");
    }

    @Test
    void keepsTheHalvesEqualAtEveryTimeAndEveryLayerCount() {
        for (long totalDurationTicks : List.of(20L, 40L, 120L, 133L, 2400L)) {
            for (int layerCount : List.of(1, 2, 3, 7, 16, 61)) {
                ArenaCleaningLayerSchedule schedule =
                        ArenaCleaningLayerSchedule.of(layerCount, totalDurationTicks);

                assertEquals(totalDurationTicks / 2L, schedule.getPhaseDurationTicks(),
                        layerCount + " layers in " + totalDurationTicks + " ticks");
                assertEquals(2L * schedule.getPhaseTickCount(), schedule.getTotalTickCount(),
                        "the two phases are the same schedule, so they are the same length");
                assertTrue(schedule.getTotalTickCount() <= totalDurationTicks + 2L * layerCount,
                        "a phase may only run over its half by the minimum of its layers: "
                                + schedule.getTotalTickCount());
            }
        }
    }

    @Test
    void dropsTheTickAnOddTimeCannotSplit() {
        ArenaCleaningLayerSchedule schedule = ArenaCleaningLayerSchedule.of(2, 21L);

        assertEquals(10L, schedule.getPhaseDurationTicks());
        assertEquals(20L, schedule.getTotalTickCount());
    }

    @Test
    void spreadsTheTicksThatDoNotDivideOverTheWholePhase() {
        ArenaCleaningLayerSchedule schedule = ArenaCleaningLayerSchedule.of(3, 20L);

        assertEquals(List.of(3L, 3L, 4L), ticksPerLayer(schedule));
        assertEquals(10L, schedule.getPhaseTickCount(), "and they still add up to the whole half");

        ArenaCleaningLayerSchedule sevenLayers = ArenaCleaningLayerSchedule.of(7, 120L);

        assertEquals(60L, sevenLayers.getPhaseTickCount());
        for (long layerTicks : ticksPerLayer(sevenLayers)) {
            assertTrue(layerTicks >= 8L && layerTicks <= 9L,
                    "no layer is worth more than one tick more than another: " + ticksPerLayer(sevenLayers));
        }
    }

    @Test
    void givesAVeryThinFightAreaTheWholePhaseForItsFewLayers() {
        ArenaCleaningLayerSchedule oneLayer = ArenaCleaningLayerSchedule.of(1, 120L);

        assertEquals(List.of(60L), ticksPerLayer(oneLayer));
        assertEquals(120L, oneLayer.getTotalTickCount(), "the configured time is kept to the tick");

        ArenaCleaningLayerSchedule twoLayers = ArenaCleaningLayerSchedule.of(2, 120L);

        assertEquals(List.of(30L, 30L), ticksPerLayer(twoLayers));
    }

    @Test
    void givesEveryLayerOfAVeryTallFightAreaAtLeastOneTick() {
        ArenaCleaningLayerSchedule schedule = ArenaCleaningLayerSchedule.of(200, 40L);

        assertEquals(20L, schedule.getPhaseDurationTicks());
        for (int layerIndex = 0; layerIndex < schedule.getLayerCount(); layerIndex++) {
            assertEquals(ArenaCleaningLayerSchedule.MINIMUM_ANIMATED_LAYER_TICKS,
                    schedule.ticksForLayer(layerIndex));
        }
        assertEquals(400L, schedule.getTotalTickCount(),
                "the configured time is what the animation aims at, the layers are what it is made of");
    }

    @Test
    void holdsNothingBackWhenThereIsNoAnimationAtAll() {
        for (long totalDurationTicks : List.of(0L, -20L, 1L)) {
            ArenaCleaningLayerSchedule schedule = ArenaCleaningLayerSchedule.of(6, totalDurationTicks);

            assertFalse(schedule.isAnimated(), totalDurationTicks + " ticks are no animation");
            assertEquals(List.of(0L, 0L, 0L, 0L, 0L, 0L), ticksPerLayer(schedule));
            assertEquals(0L, schedule.getTotalTickCount());
        }
    }

    @Test
    void answersAShapeWithoutALayerWithTheOneItAlwaysHas() {
        ArenaCleaningLayerSchedule schedule = ArenaCleaningLayerSchedule.of(0, 40L);

        assertEquals(1, schedule.getLayerCount());
        assertEquals(List.of(20L), ticksPerLayer(schedule));
    }

    @Test
    void refusesALayerThatTheShapeDoesNotHave() {
        ArenaCleaningLayerSchedule schedule = ArenaCleaningLayerSchedule.of(3, 40L);

        assertThrows(IndexOutOfBoundsException.class, () -> schedule.ticksForLayer(3));
        assertThrows(IndexOutOfBoundsException.class, () -> schedule.ticksForLayer(-1));
    }

    @Test
    void runsTheShippedTimeOverAWallOfTwentyLayers() {
        ArenaCleaningLayerSchedule schedule = ArenaCleaningLayerSchedule.of(20,
                ArenaCleaningParticleSettings.defaults().animationDurationTicks());

        assertEquals(120L, schedule.getTotalTickCount(), "six seconds");
        assertEquals(60L, schedule.getPhaseDurationTicks());
        for (int layerIndex = 0; layerIndex < schedule.getLayerCount(); layerIndex++) {
            assertEquals(3L, schedule.ticksForLayer(layerIndex));
        }
    }

    private static List<Long> ticksPerLayer(ArenaCleaningLayerSchedule schedule) {
        List<Long> ticks = new ArrayList<>(schedule.getLayerCount());
        for (int layerIndex = 0; layerIndex < schedule.getLayerCount(); layerIndex++) {
            ticks.add(schedule.ticksForLayer(layerIndex));
        }
        return ticks;
    }
}
