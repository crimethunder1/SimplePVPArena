package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.shape;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaRulesInTheEffectAreaTest {

    private static final String WORLD_NAME = "event";
    private static final String OTHER_WORLD_NAME = "lobby";
    private static final int FLOOR_Y = 64;
    private static final int WALL_TOP_Y = 65;
    private static final int MARGIN = 3;
    private static final boolean ADMIN = true;
    private static final boolean REGULAR_PLAYER = false;
    private static final boolean INSIDE_FIGHT_AREA = true;
    private static final boolean OUTSIDE_FIGHT_AREA = false;
    private static final boolean INSIDE_EFFECT_AREA = true;
    private static final boolean OUTSIDE_EFFECT_AREA = false;

    private final ArenaShape fightArea = shape(FLOOR_Y, WALL_TOP_Y, 0, 0, 1, 0);
    private final Optional<EffectArea> effectArea = Optional.of(EffectArea.around(fightArea, MARGIN));

    @Test
    void decidesEveryChangeInsideTheAreaOfEffectAsBefore() {
        for (boolean arenaAdmin : new boolean[] {ADMIN, REGULAR_PLAYER}) {
            for (boolean insideFightArea : new boolean[] {INSIDE_FIGHT_AREA, OUTSIDE_FIGHT_AREA}) {
                assertEquals(ArenaWorldBuildRule.isChangeAllowed(arenaAdmin, insideFightArea),
                        ArenaWorldBuildRule.isChangeAllowed(arenaAdmin, insideFightArea, INSIDE_EFFECT_AREA));
            }
        }
        assertFalse(ArenaWorldBuildRule.isChangeAllowed(REGULAR_PLAYER, OUTSIDE_FIGHT_AREA, INSIDE_EFFECT_AREA),
                "a regular player next to the arena is still kept out");
    }

    @Test
    void letsEverybodyChangeTheWorldOutsideOfTheAreaOfEffect() {
        assertTrue(ArenaWorldBuildRule.isChangeAllowed(REGULAR_PLAYER, OUTSIDE_FIGHT_AREA, OUTSIDE_EFFECT_AREA));
        assertTrue(ArenaWorldBuildRule.isChangeAllowed(ADMIN, OUTSIDE_FIGHT_AREA, OUTSIDE_EFFECT_AREA));
    }

    @Test
    void protectsThePositionsInsideTheAreaOfEffectAsBefore() {
        int[][] positions = {
                {4, FLOOR_Y, 0}, {1, FLOOR_Y, 4}, {2, WALL_TOP_Y + 20, 0}, {2, FLOOR_Y - 30, 0},
                {0, FLOOR_Y, 0}, {1, WALL_TOP_Y + 20, 0}, {0, FLOOR_Y - 1, 0}};
        for (int[] position : positions) {
            assertTrue(effectArea.orElseThrow().containsBlock(WORLD_NAME, position[0], position[2]));
            assertEquals(ArenaEnvironmentProtectionRule.isProtected(Optional.of(fightArea), FLOOR_Y, WORLD_NAME,
                            position[0], position[1], position[2]),
                    isProtected(position[0], position[1], position[2]),
                    "the block " + position[0] + "/" + position[1] + "/" + position[2]);
        }
        assertTrue(isProtected(4, FLOOR_Y, 0), "the last block inside the area beside the fight area");
        assertTrue(isProtected(1, FLOOR_Y, 4), "and on the other axis");
        assertFalse(isProtected(0, FLOOR_Y, 0), "the fight area itself is left to the match");
        assertTrue(isProtected(0, FLOOR_Y - 1, 0), "the ground under it stays protected");
    }

    @Test
    void leavesThePositionsOutsideOfTheAreaOfEffectAlone() {
        assertFalse(isProtected(5, FLOOR_Y, 0), "the first block outside of the area");
        assertFalse(isProtected(1, FLOOR_Y, 5), "and on the other axis");
        assertFalse(isProtected(500, FLOOR_Y, 500), "far away");
        assertFalse(isProtected(5, FLOOR_Y - 30, 0), "deep in the ground");
        assertFalse(isProtected(5, WALL_TOP_Y + 200, 0), "and high in the air");
    }

    @Test
    void leavesEveryOtherWorldAlone() {
        assertFalse(ArenaEnvironmentProtectionRule.isProtected(Optional.of(fightArea), effectArea, FLOOR_Y,
                OTHER_WORLD_NAME, 2, FLOOR_Y, 0));
    }

    @Test
    void protectsNothingWithoutAnAreaOfEffect() {
        assertFalse(ArenaEnvironmentProtectionRule.isProtected(Optional.empty(), Optional.empty(), FLOOR_Y,
                WORLD_NAME, 2, FLOOR_Y, 0));
        assertFalse(ArenaEnvironmentProtectionRule.isProtected(Optional.of(fightArea), Optional.empty(), FLOOR_Y,
                WORLD_NAME, 2, FLOOR_Y, 0));
    }

    private boolean isProtected(int blockX, int blockY, int blockZ) {
        return ArenaEnvironmentProtectionRule.isProtected(Optional.of(fightArea), effectArea, FLOOR_Y, WORLD_NAME,
                blockX, blockY, blockZ);
    }
}
