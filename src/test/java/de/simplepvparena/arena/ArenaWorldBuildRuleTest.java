package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaWorldBuildRuleTest {

    private static final boolean ARENA_ADMIN = true;
    private static final boolean REGULAR_PLAYER = false;
    private static final boolean INSIDE = true;
    private static final boolean OUTSIDE = false;

    @Test
    void keepsEverybodyWhoIsNoAdminOutOfTheWorldAroundTheArena() {
        assertFalse(ArenaWorldBuildRule.isChangeAllowed(REGULAR_PLAYER, OUTSIDE));
    }

    @Test
    void letsAnAdminChangeTheWorldAroundTheArena() {
        assertTrue(ArenaWorldBuildRule.isChangeAllowed(ARENA_ADMIN, OUTSIDE));
    }

    @Test
    void leavesEveryChangeInsideTheFightAreaToTheRulesOfTheArena() {
        assertTrue(ArenaWorldBuildRule.isChangeAllowed(REGULAR_PLAYER, INSIDE));
        assertTrue(ArenaWorldBuildRule.isChangeAllowed(ARENA_ADMIN, INSIDE));
    }

    @Test
    void namesTheAdminAsTheOnlyOneWhoChangesTheWorldAroundTheArena() {
        assertTrue(ArenaWorldBuildRule.REFUSAL.contains("admins"));
        assertTrue(ArenaWorldBuildRule.REFUSAL.contains("outside the arena"));
    }
}
