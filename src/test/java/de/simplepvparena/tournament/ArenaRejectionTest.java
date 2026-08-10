package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaRejectionTest {

    @Test
    void rejectsAnArenaThatIsNotFullyConfigured() {
        assertEquals("The arena is not fully configured, set its regions with /tournament arena setpos",
                TournamentManager.findArenaRejection(false, Optional.empty()).orElseThrow());
    }

    @Test
    void rejectsAnArenaThatStillRunsAMatch() {
        assertEquals("The arena is occupied by match round2-match1",
                TournamentManager.findArenaRejection(true, Optional.of("round2-match1")).orElseThrow());
    }

    @Test
    void namesTheMissingConfigurationBeforeTheRunningMatch() {
        assertEquals("The arena is not fully configured, set its regions with /tournament arena setpos",
                TournamentManager.findArenaRejection(false, Optional.of("round2-match1")).orElseThrow());
    }

    @Test
    void acceptsAFreeAndFullyConfiguredArena() {
        assertTrue(TournamentManager.findArenaRejection(true, Optional.empty()).isEmpty());
    }
}
