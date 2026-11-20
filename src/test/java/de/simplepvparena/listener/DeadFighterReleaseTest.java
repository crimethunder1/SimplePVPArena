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

        harness.runTick();
        assertEquals(List.of("Fighter"), respawned, "the dead fighter is respawned on the next tick");

        harness.runTick();
        assertFalse(harness.getKitService().hasAppliedKit(fighter.player()));
        assertEquals(GameMode.ADVENTURE, fighter.gameMode(), "and then released to the tribune");
        assertTrue(fighter.inventoryUpdateCount() > 0, "with the inventory sent to the client again");
    }

    @Test
    void releasesALivingFighterAtOnceWithoutARespawn() {
        harness.getMatchService().forceResult(match.getMatchId(), match.getFirstTeam().getTeamName());

        assertFalse(harness.getKitService().hasAppliedKit(fighter.player()));
        assertEquals(GameMode.ADVENTURE, fighter.gameMode());
        assertEquals(List.of(), respawned);
    }
}
