package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.capture;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.shape;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaEnvironmentProtectionRuleTest {

    private static final String WORLD_NAME = "event";
    private static final String OTHER_WORLD_NAME = "lobby";
    private static final int FLOOR_Y = 64;
    private static final int WALL_TOP_Y = 65;
    private static final int DEPTH_BELOW_FLOOR = 10;

    private final ArenaShape fightArea = shape(FLOOR_Y, WALL_TOP_Y, 0, 0, 1, 0);

    @Test
    void protectsEveryPositionOutsideTheCellsOfTheFightArea() {
        assertTrue(isProtected(FLOOR_Y, 2, FLOOR_Y, 0), "next to the fight area at the height of its floor");
        assertTrue(isProtected(FLOOR_Y, 0, FLOOR_Y, 1), "on the other side");
        assertTrue(isProtected(FLOOR_Y, 2, WALL_TOP_Y + 20, 0), "high above the tribune");
        assertTrue(isProtected(FLOOR_Y, 2, FLOOR_Y - 30, 0), "and deep in the ground beside the arena");
    }

    @Test
    void leavesTheCellsFromTheCapturedFloorUpwardsAlone() {
        assertFalse(isProtected(FLOOR_Y, 0, FLOOR_Y, 0), "the captured floor itself");
        assertFalse(isProtected(FLOOR_Y, 1, WALL_TOP_Y, 0), "the top of the border wall");
        assertFalse(isProtected(FLOOR_Y, 1, WALL_TOP_Y + 20, 0), "and everything built above it");
    }

    @Test
    void protectsTheGroundUnderTheCapturedFloorInsideTheCells() {
        assertTrue(isProtected(FLOOR_Y, 0, FLOOR_Y - 1, 0));
        assertTrue(isProtected(FLOOR_Y, 1, FLOOR_Y - 30, 0));
    }

    @Test
    void leavesEveryOtherWorldAlone() {
        assertFalse(ArenaEnvironmentProtectionRule.isProtected(Optional.of(fightArea), FLOOR_Y, OTHER_WORLD_NAME,
                2, FLOOR_Y, 0), "outside the cells");
        assertFalse(ArenaEnvironmentProtectionRule.isProtected(Optional.of(fightArea), FLOOR_Y, OTHER_WORLD_NAME,
                0, FLOOR_Y - 1, 0), "and under the floor");
    }

    @Test
    void protectsNothingWhileNoFightAreaIsCalculated() {
        assertFalse(ArenaEnvironmentProtectionRule.isProtected(Optional.empty(), FLOOR_Y, WORLD_NAME,
                2, FLOOR_Y, 0));
        assertFalse(ArenaEnvironmentProtectionRule.isProtected(Optional.empty(), FLOOR_Y, WORLD_NAME,
                0, FLOOR_Y - 1, 0));
    }

    @Test
    void readsTheCapturedFloorOutOfTheBaseline() {
        Optional<ArenaBaselineSnapshot> baseline = Optional.of(capture(fightArea, Map.of(), 4, DEPTH_BELOW_FLOOR));
        int capturedFloorY = ArenaEnvironmentProtectionRule.findCapturedFloorY(fightArea, baseline);

        assertEquals(FLOOR_Y - DEPTH_BELOW_FLOOR, capturedFloorY);
        assertFalse(isProtected(capturedFloorY, 0, FLOOR_Y - 1, 0),
                "the ground the cleaning restores is left to the match");
        assertFalse(isProtected(capturedFloorY, 0, capturedFloorY, 0), "down to the deepest captured layer");
        assertTrue(isProtected(capturedFloorY, 0, capturedFloorY - 1, 0),
                "the first block under the captured volume is protected");
        assertTrue(baseline.orElseThrow().isBelowTheCapturedVolume(capturedFloorY - 1),
                "exactly where the break rule starts to count the arena as untouchable");
        assertFalse(baseline.orElseThrow().isBelowTheCapturedVolume(capturedFloorY));
        assertTrue(isProtected(capturedFloorY, 2, FLOOR_Y, 0), "and outside the cells nothing changes");
    }

    @Test
    void usesTheFloorOfTheFightAreaWithoutABaseline() {
        int capturedFloorY = ArenaEnvironmentProtectionRule.findCapturedFloorY(fightArea, Optional.empty());

        assertEquals(FLOOR_Y, capturedFloorY);
        assertFalse(isProtected(capturedFloorY, 0, FLOOR_Y, 0));
        assertTrue(isProtected(capturedFloorY, 0, FLOOR_Y - 1, 0));
        assertTrue(isProtected(capturedFloorY, 2, FLOOR_Y, 0));
    }

    private boolean isProtected(int capturedFloorY, int blockX, int blockY, int blockZ) {
        return ArenaEnvironmentProtectionRule.isProtected(Optional.of(fightArea), capturedFloorY, WORLD_NAME,
                blockX, blockY, blockZ);
    }
}
