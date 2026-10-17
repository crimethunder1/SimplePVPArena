package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaBuildRuleTest {

    private static final boolean ACTIVE_FIGHTER = true;
    private static final boolean NOT_FIGHTING = false;
    private static final boolean ADMIN = true;
    private static final boolean REGULAR_PLAYER = false;
    private static final boolean INSIDE = true;
    private static final boolean OUTSIDE = false;

    @Test
    void letsAnActiveFighterBuildInsideTheFightArea() {
        assertTrue(ArenaBuildRule.isPlacementAllowed(ACTIVE_FIGHTER, REGULAR_PLAYER, INSIDE));
    }

    @Test
    void keepsEverybodyWhoIsNotFightingOutOfTheFightArea() {
        assertFalse(ArenaBuildRule.isPlacementAllowed(NOT_FIGHTING, REGULAR_PLAYER, INSIDE));
    }

    @Test
    void leavesEveryPlacementOutsideTheFightAreaAlone() {
        assertTrue(ArenaBuildRule.isPlacementAllowed(NOT_FIGHTING, REGULAR_PLAYER, OUTSIDE));
        assertTrue(ArenaBuildRule.isPlacementAllowed(ACTIVE_FIGHTER, REGULAR_PLAYER, OUTSIDE));
    }

    @Test
    void letsAnAdminBuildEverywhere() {
        assertTrue(ArenaBuildRule.isPlacementAllowed(NOT_FIGHTING, ADMIN, INSIDE));
        assertTrue(ArenaBuildRule.isPlacementAllowed(NOT_FIGHTING, ADMIN, OUTSIDE));
    }
}
