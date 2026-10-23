package de.simplepvparena.listener;

import de.simplepvparena.arena.EffectAreaPresence;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaInventoryListenerPresenceTest {

    @TempDir
    Path dataFolder;

    private final List<String> firstPresenceCalls = new ArrayList<>();
    private final List<String> secondPresenceCalls = new ArrayList<>();

    private AreaInventoryFixture fixture;
    private ArenaMatchHarness harness;
    private AreaInventoryListener listener;

    @BeforeEach
    void setUp() {
        fixture = new AreaInventoryFixture(dataFolder);
        harness = fixture.harness();
        listener = new AreaInventoryListener(harness.getPlugin(), fixture.service(),
                recording(firstPresenceCalls), recording(secondPresenceCalls));
    }

    @Test
    void handsEveryCheckOfTheAreaToTheFurtherPresencesAsWell() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Walker");

        player.moveTo(fixture.inside());
        listener.onPlayerMove(new PlayerMoveEvent(player.player(), fixture.outside(), fixture.inside()));
        listener.onPlayerTeleport(new PlayerTeleportEvent(player.player(), fixture.inside(), fixture.outside(),
                PlayerTeleportEvent.TeleportCause.COMMAND));
        player.moveTo(fixture.inTheLobby());
        listener.onPlayerChangedWorld(new PlayerChangedWorldEvent(player.player(), harness.getWorld()));
        player.moveTo(fixture.inside());
        listener.onPlayerRespawn(new PlayerRespawnEvent(player.player(), player.location(), false, false, false,
                PlayerRespawnEvent.RespawnReason.DEATH));
        harness.runTick();
        listener.onPlayerJoin(new PlayerJoinEvent(player.player(), Component.empty()));

        assertEquals(List.of("Walker 5.5/5.5", "Walker 500.5/500.5", "Walker 5.5/5.5", "Walker 5.5/5.5",
                "Walker 5.5/5.5"), firstPresenceCalls);
        assertEquals(firstPresenceCalls, secondPresenceCalls);
        assertTrue(fixture.service().hasDeposited(player.player()), "the inventory service is still asked first");
    }

    @Test
    void aMovementWithinTheSameBlockReachesNoPresence() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Turner");

        listener.onPlayerMove(new PlayerMoveEvent(player.player(), harness.at(5.1D, 5.1D), harness.at(5.9D, 5.9D)));

        assertEquals(List.of(), firstPresenceCalls);
    }

    private static EffectAreaPresence recording(List<String> calls) {
        return (Player player, Location location) -> calls.add(player.getName() + " " + location.getX() + "/"
                + location.getZ());
    }
}
