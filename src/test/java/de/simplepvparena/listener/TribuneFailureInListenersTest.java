package de.simplepvparena.listener;

import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.match.Match;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.FakeSpectatorItems;
import net.kyori.adventure.text.Component;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TribuneFailureInListenersTest {

    private final RuntimeException teleportFailure = new IllegalStateException("the teleport was refused");
    private final List<LogRecord> logRecords = new ArrayList<>();
    private final Handler recordingHandler = new Handler() {
        @Override
        public void publish(LogRecord logRecord) {
            logRecords.add(logRecord);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    private ArenaMatchHarness harness;
    private Logger logger;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        logger = harness.getPlugin().getLogger();
        logger.addHandler(recordingHandler);
    }

    @AfterEach
    void tearDown() {
        logger.removeHandler(recordingHandler);
    }

    @Test
    void aPlayerWhoRejoinsDuringTheMatchStillHearsAboutItWhenTheTribuneFails() {
        ArenaMatchHarness.FakePlayer fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        Match match = harness.startMatch(fighter, mate, harness.addPlayer("Opponent"),
                harness.addPlayer("OpponentMate"));
        harness.getMatchService().handlePlayerEliminated(mate.uniqueId());
        mate.forgetReceivedTexts();
        mate.failTeleportWith(teleportFailure);

        connectionListener().onPlayerJoin(new PlayerJoinEvent(mate.player(), Component.empty()));

        assertEquals(List.of("You rejoined during match " + match.getMatchId()
                + ", you take part again from the next round on"), mate.messages());
        assertWarningLogged("Mate could not be sent to the tribune");
    }

    @Test
    void aJoiningPlayerIsLetInWhenTheTribuneFails() {
        harness.getArenaRegistry().setTeleportOnJoin(true);
        ArenaMatchHarness.FakePlayer visitor = harness.addPlayer("Visitor");
        visitor.failTeleportWith(teleportFailure);

        connectionListener().onPlayerJoin(new PlayerJoinEvent(visitor.player(), Component.empty()));

        assertWarningLogged("Visitor could not be sent to the tribune");
    }

    @Test
    void aRespawnGoesOnWhenTheTribuneFails() {
        ArenaMatchHarness.FakePlayer spectator = harness.addPlayer("Spectator");
        assertTrue(harness.getSpectatorService().sendToTribune(spectator.player()));
        spectator.failTeleportWith(teleportFailure);
        PlayerRespawnListener respawnListener = new PlayerRespawnListener(harness.getPlugin(),
                harness.getMatchService(), harness.getSpectatorService(), harness.getArenaRegistry());

        respawnListener.onPlayerRespawn(new PlayerRespawnEvent(spectator.player(), spectator.location(), false));
        harness.runTick();

        assertWarningLogged("Spectator could not be sent to the tribune");
    }

    @Test
    void aFlyingSpectatorWhoLeavesTheArenaGetsNoWrongHintWhenTheTribuneFails() {
        ArenaMatchHarness.FakePlayer spectator = harness.addPlayer("Spectator");
        assertTrue(harness.getSpectatorService().enterArenaFlight(spectator.player()));
        spectator.failTeleportWith(teleportFailure);
        spectator.forgetReceivedTexts();
        PlayerMoveListener moveListener = new PlayerMoveListener(harness.getMatchService(),
                harness.getSpectatorService(), harness.getArenaRegistry(), logger);

        moveListener.onPlayerMove(new PlayerMoveEvent(spectator.player(), harness.at(5.5D, 5.5D),
                harness.at(500.5D, 500.5D)));

        assertEquals(List.of(), spectator.messages());
        assertWarningLogged("Spectator could not be sent to the tribune");
    }

    @Test
    void theSpectatorToggleGetsNoWrongHintWhenTheSwitchFails() {
        ArenaMatchHarness.FakePlayer spectator = harness.addPlayer("Spectator");
        assertTrue(harness.getSpectatorService().sendToTribune(spectator.player()));
        spectator.failTeleportWith(teleportFailure);
        spectator.forgetReceivedTexts();
        PlayerInteractListener interactListener = new PlayerInteractListener(harness.getMatchService(),
                harness.getSpectatorService(), new BorderSelectorModeRegistry(), logger);
        PlayerInteractEvent event = new PlayerInteractEvent(spectator.player(), Action.RIGHT_CLICK_AIR,
                new FakeItemStack(FakeSpectatorItems.SPECTATOR_TOGGLE_LABEL), null, BlockFace.UP,
                EquipmentSlot.HAND);

        interactListener.onPlayerInteract(event);

        assertEquals(List.of(), spectator.messages());
        assertWarningLogged("The spectator mode of Spectator could not be switched");
    }

    private PlayerConnectionListener connectionListener() {
        return new PlayerConnectionListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getArenaRegistry(), new BorderSelectorModeRegistry(), harness.getKitService(), logger);
    }

    private void assertWarningLogged(String message) {
        assertTrue(logRecords.stream().anyMatch(logRecord -> logRecord.getLevel() == Level.WARNING
                        && logRecord.getMessage().equals(message) && logRecord.getThrown() == teleportFailure),
                logRecords.stream().map(LogRecord::getMessage).toList().toString());
    }
}
