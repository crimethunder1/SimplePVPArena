package de.simplepvparena.listener;

import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.Match;
import org.bukkit.GameMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeadFighterReleaseTest {

    private final List<String> respawned = new ArrayList<>();
    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private Match match;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness(KitService::new);
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        match = harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.getMatchService().setDeadPlayerRespawner(player -> {
            respawned.add(player.getName());
            fighter.setDead(false);
        });
    }

    @Test
    void neverTouchesADeadFighterAtTheEndOfTheMatchUntilTheFighterIsRespawned() {
        fighter.setDead(true);

        harness.getMatchService().forceResult(match.getMatchId(), match.getSecondTeam().getTeamName());

        assertTrue(harness.getKitService().hasAppliedKit(fighter.player()),
                "the dead fighter keeps the kit marker, nothing is restored on a dead player");
        assertEquals(GameMode.SURVIVAL, fighter.gameMode(), "and is not sent to the tribune while dead");
        assertFalse(harness.getKitService().hasAppliedKit(mate.player()), "the living mate is released at once");
        assertEquals(GameMode.ADVENTURE, mate.gameMode());

        harness.advanceTicks(19);
        assertEquals(List.of(), respawned, "the client gets a second to respawn by itself");
        assertEquals(GameMode.SURVIVAL, fighter.gameMode());

        harness.advanceTicks(1);
        assertEquals(List.of("Fighter"), respawned, "then the dead fighter is respawned by the plugin");
        assertEquals(GameMode.SURVIVAL, fighter.gameMode(), "and is given time to load the world");

        harness.advanceTicks(3);
        assertFalse(harness.getKitService().hasAppliedKit(fighter.player()));
        assertEquals(GameMode.ADVENTURE, fighter.gameMode(), "before being released to the tribune");
        assertTrue(fighter.inventoryUpdateCount() > 0, "with the inventory sent to the client again");
    }

    @Test
    void waitsForAFighterWhoRespawnsByHimselfAndNeverForcesTheRespawn() {
        fighter.setDead(true);
        harness.getMatchService().forceResult(match.getMatchId(), match.getSecondTeam().getTeamName());
        assertTrue(harness.getMatchService().isAwaitingRespawn(fighter.uniqueId()));

        harness.advanceTicks(3);
        fighter.setDead(false);
        harness.advanceTicks(4);

        assertEquals(List.of(), respawned);
        assertEquals(GameMode.ADVENTURE, fighter.gameMode());
        assertFalse(harness.getMatchService().isAwaitingRespawn(fighter.uniqueId()));
    }

    @Test
    void releasesALivingFighterAtOnceWithoutARespawn() {
        harness.getMatchService().forceResult(match.getMatchId(), match.getFirstTeam().getTeamName());

        assertFalse(harness.getKitService().hasAppliedKit(fighter.player()));
        assertEquals(GameMode.ADVENTURE, fighter.gameMode());
        assertEquals(List.of(), respawned);
    }
}
