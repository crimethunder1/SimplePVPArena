package de.simplepvparena.arena;

import de.simplepvparena.message.ListenerMessage;
import de.simplepvparena.message.Messages;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaWorldBuildRuleTest {

    private static final boolean ADMIN = true;
    private static final boolean REGULAR_PLAYER = false;
    private static final boolean INSIDE = true;
    private static final boolean OUTSIDE = false;

    @Test
    void keepsEverybodyWhoIsNoAdminOutOfTheWorldAroundTheArena() {
        assertFalse(ArenaWorldBuildRule.isChangeAllowed(REGULAR_PLAYER, OUTSIDE));
    }

    @Test
    void letsAnAdminChangeTheWorldAroundTheArena() {
        assertTrue(ArenaWorldBuildRule.isChangeAllowed(ADMIN, OUTSIDE));
    }

    @Test
    void leavesEveryChangeInsideTheFightAreaToTheRulesOfTheArena() {
        assertTrue(ArenaWorldBuildRule.isChangeAllowed(REGULAR_PLAYER, INSIDE));
        assertTrue(ArenaWorldBuildRule.isChangeAllowed(ADMIN, INSIDE));
    }

    @Test
    void namesTheAdminAsTheOnlyOneWhoChangesTheWorldAroundTheArena() {
        for (ListenerMessage refusal : new ListenerMessage[] {ListenerMessage.BREAK_OUTSIDE_THE_ARENA,
                ListenerMessage.BUILD_OUTSIDE_THE_ARENA}) {
            assertTrue(Messages.text(refusal).contains("admins"));
            assertTrue(Messages.text(refusal).contains("outside the arena"));
        }
    }
}
