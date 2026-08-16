package de.simplepvparena.listener;

import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FighterRespawnTest {

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer spectator;
    private PlayerRespawnListener respawnListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        spectator = harness.addPlayer("Spectator");
        harness.startMatch(fighter, mate, opponent, opponentMate);
        respawnListener = new PlayerRespawnListener(harness.getPlugin(), harness.getMatchService(),
                harness.getSpectatorService(), harness.getArenaRegistry());
    }

    @Test
    void respawnsAnActiveFighterOnHisTeamSpawn() {
        PlayerRespawnEvent event = respawn(fighter);

        assertEquals(2.5D, event.getRespawnLocation().getX(), 1.0E-9D);
        assertEquals(2.5D, event.getRespawnLocation().getZ(), 1.0E-9D);
        assertTrue(harness.getShape().contains(event.getRespawnLocation()),
                "the spawn he comes back on lies inside the fight area");
    }

    @Test
    void neverLeavesAnActiveFighterWithASpectatorState() {
        respawn(fighter);
        harness.runTick();

        assertTrue(harness.getSpectatorService().findSpectatorMode(fighter.uniqueId()).isEmpty());
        assertTrue(harness.getMatchService().isActiveFighter(fighter.uniqueId()),
                "and he still fights the round he never left");
    }

    @Test
    void handsAnActiveFighterHisKitBackOnTheNextTick() {
        int scheduledBefore = harness.getScheduledTaskCount();

        respawn(fighter);

        assertEquals(scheduledBefore + 1, harness.getScheduledTaskCount(),
                "the respawn leaves exactly one task behind, next to the round timer of the match");
    }

    @Test
    void keepsSendingEverybodyWhoIsNotFightingToTheTribune() {
        PlayerRespawnEvent event = respawn(spectator);

        assertFalse(harness.getShape().contains(event.getRespawnLocation()),
                "the tribune lies outside the fight area");
        assertEquals(ArenaMatchHarness.WALL_MAXIMUM + 5.5D, event.getRespawnLocation().getX(), 1.0E-9D);
    }

    private PlayerRespawnEvent respawn(ArenaMatchHarness.FakePlayer player) {
        PlayerRespawnEvent event = new PlayerRespawnEvent(player.player(), player.location(), false, false, false,
                PlayerRespawnEvent.RespawnReason.DEATH);
        respawnListener.onPlayerRespawn(event);
        return event;
    }
}
