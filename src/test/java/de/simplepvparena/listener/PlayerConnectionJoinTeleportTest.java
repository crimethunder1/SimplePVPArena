package de.simplepvparena.listener;

import de.simplepvparena.arena.SpectatorMode;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.kit.KitService;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerConnectionJoinTeleportTest {

    private static final double TRIBUNE_COORDINATE = ArenaMatchHarness.WALL_MAXIMUM + 5.5D;
    private static final double FAR_X = 300.5D;
    private static final double FAR_Z = 40.5D;

    private ArenaMatchHarness harness;
    private KitService kitService;
    private PlayerConnectionListener listener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness(KitService::new);
        kitService = harness.getKitService();
        listener = new PlayerConnectionListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getArenaRegistry(), new BorderSelectorModeRegistry(), kitService);
    }

    @Test
    void sendsAJoiningPlayerToTheTribuneWhileTheSettingIsOn() {
        assertTrue(harness.getArenaRegistry().isTeleportOnJoin(), "the setting is on by default");
        ArenaMatchHarness.FakePlayer joiner = joinFarAway("Joiner");

        assertAtTheTribune(joiner);
        assertEquals(Optional.of(SpectatorMode.TRIBUNE),
                harness.getSpectatorService().findSpectatorMode(joiner.uniqueId()));
    }

    @Test
    void leavesAJoiningPlayerWhereTheyAreWhileTheSettingIsOff() {
        harness.getArenaRegistry().setTeleportOnJoin(false);

        ArenaMatchHarness.FakePlayer joiner = joinFarAway("Joiner");

        assertEquals(harness.at(FAR_X, FAR_Z), joiner.location());
        assertEquals(Optional.empty(), harness.getSpectatorService().findSpectatorMode(joiner.uniqueId()));
    }

    @Test
    void stillSendsAParticipantOfTheRunningMatchToTheTribuneWhileTheSettingIsOff() {
        harness.getArenaRegistry().setTeleportOnJoin(false);
        ArenaMatchHarness.FakePlayer fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.getMatchService().handlePlayerEliminated(fighter.uniqueId());
        assertTrue(harness.getMatchService().findMatchOfPlayer(fighter.uniqueId()).isPresent());
        fighter.moveTo(harness.at(FAR_X, FAR_Z));

        join(fighter);

        assertAtTheTribune(fighter);
        assertEquals(Optional.of(SpectatorMode.TRIBUNE),
                harness.getSpectatorService().findSpectatorMode(fighter.uniqueId()));
    }

    @Test
    void stillRestoresAPlayerWithTheKitMarkerWhileTheSettingIsOff() {
        harness.getArenaRegistry().setTeleportOnJoin(false);
        ArenaMatchHarness.FakePlayer returner = harness.addPlayer("Returner");
        kitService.applyKit(returner.player());
        int inventoryClearCountBeforeTheJoin = returner.inventoryClearCount();
        returner.moveTo(harness.at(FAR_X, FAR_Z));

        join(returner);

        assertFalse(kitService.hasAppliedKit(returner.player()), "the marker is gone after the join");
        assertEquals(inventoryClearCountBeforeTheJoin + 1, returner.inventoryClearCount());
        assertEquals(harness.at(FAR_X, FAR_Z), returner.location(), "and the player stays where they are");
    }

    @Test
    void sendsNobodyToTheTribuneOfAnIncompleteArenaWhileTheSettingIsOn() {
        harness.forgetTribuneSpawn();

        ArenaMatchHarness.FakePlayer joiner = joinFarAway("Joiner");

        assertEquals(harness.at(FAR_X, FAR_Z), joiner.location());
    }

    private ArenaMatchHarness.FakePlayer joinFarAway(String playerName) {
        ArenaMatchHarness.FakePlayer joiner = harness.addPlayer(playerName);
        joiner.moveTo(harness.at(FAR_X, FAR_Z));
        join(joiner);
        return joiner;
    }

    private void assertAtTheTribune(ArenaMatchHarness.FakePlayer player) {
        Location location = player.location();
        assertEquals(TRIBUNE_COORDINATE, location.getX());
        assertEquals(TRIBUNE_COORDINATE, location.getZ());
    }

    private void join(ArenaMatchHarness.FakePlayer player) {
        listener.onPlayerJoin(new PlayerJoinEvent(player.player(), Component.empty()));
    }
}
