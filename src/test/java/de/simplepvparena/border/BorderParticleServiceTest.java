package de.simplepvparena.border;

import de.simplepvparena.arena.ArenaBorderConfirmation;
import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderParticleServiceTest {

    private static final String WORLD_NAME = "border-particles";
    private static final int GROUND_Y = 64;

    private final List<String> drawnEffects = new ArrayList<>();
    private final List<Runnable> repeatingTasks = new ArrayList<>();
    private final BorderMarkIndex borderMarks = new BorderMarkIndex();
    private final ArenaBorderConfirmation borderConfirmation = new ArenaBorderConfirmation(() -> true);
    private final World world = SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
        case "getName" -> WORLD_NAME;
        default -> throw new UnsupportedOperationException("World." + method.getName());
    });
    private final Player onlookerPlayer = createPlayer();
    private final BorderParticleService service = new BorderParticleService(createPlugin(), borderMarks,
            SharedBukkitServer.proxy(BorderVisualEffect.class, (method, arguments) -> {
                drawnEffects.add(method.getName());
                return null;
            }), playerUniqueId -> false, borderConfirmation);

    @Test
    void showsTheParticlesAtTheConfirmedWallByDefault() {
        markWall();
        borderConfirmation.confirmCalculation();
        service.start();

        runOneRedraw();

        assertTrue(BorderParticleService.DEFAULT_CONFIRMED_PARTICLE_ENABLED);
        assertTrue(service.isConfirmedParticleEnabled());
        assertEquals(List.of("showConfirmedAt", "showConfirmedAt", "showConfirmedAt"), drawnEffects);
    }

    @Test
    void hidesTheParticlesAtTheConfirmedWallWhileSwitchedOffAndShowsThemAgainWhenSwitchedOn() {
        markWall();
        borderConfirmation.confirmCalculation();
        service.start();

        service.setConfirmedParticleEnabled(false);
        runOneRedraw();

        assertFalse(service.isConfirmedParticleEnabled());
        assertEquals(List.of(), drawnEffects);

        service.setConfirmedParticleEnabled(true);
        runOneRedraw();

        assertTrue(service.isConfirmedParticleEnabled());
        assertEquals(List.of("showConfirmedAt", "showConfirmedAt", "showConfirmedAt"), drawnEffects);
    }

    @Test
    void keepsShowingTheMarksOfAWallThatIsNotConfirmedYetWhileSwitchedOff() {
        markWall();
        service.start();

        service.setConfirmedParticleEnabled(false);
        runOneRedraw();

        assertEquals(3, drawnEffects.stream().filter("showAt"::equals).count(), drawnEffects.toString());
        assertTrue(drawnEffects.contains("showColumnConnection"), drawnEffects.toString());
        assertFalse(drawnEffects.contains("showConfirmedAt"), drawnEffects.toString());
    }

    @Test
    void switchingKeepsTheRedrawRunningForTheMarksOfTheWallSelection() {
        service.start();

        service.setConfirmedParticleEnabled(false);
        service.setConfirmedParticleEnabled(true);
        service.setConfirmedParticleEnabled(false);

        assertTrue(service.isRunning());
        assertEquals(1, repeatingTasks.size());
    }

    @Test
    void showsTheParticlesAgainOnceTheWallIsMarkedAnewAfterTheConfirmation() {
        markWall();
        borderConfirmation.confirmCalculation();
        service.start();
        service.setConfirmedParticleEnabled(false);

        borderConfirmation.invalidate();
        runOneRedraw();

        assertTrue(drawnEffects.contains("showAt"), drawnEffects.toString());
        assertFalse(drawnEffects.contains("showConfirmedAt"), drawnEffects.toString());
    }

    private void markWall() {
        for (int blockX = 0; blockX < 3; blockX++) {
            borderMarks.mark(WORLD_NAME, blockX, GROUND_Y, 0);
        }
    }

    private void runOneRedraw() {
        for (Runnable repeatingTask : List.copyOf(repeatingTasks)) {
            repeatingTask.run();
        }
    }

    private Player createPlayer() {
        UUID playerUniqueId = UUID.randomUUID();
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> playerUniqueId;
            case "getLocation" -> new Location(world, 1.5D, GROUND_Y, 2.5D);
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }

    private Plugin createPlugin() {
        SharedBukkitServer.install();
        BukkitScheduler scheduler = SharedBukkitServer.proxy(BukkitScheduler.class,
                (method, arguments) -> switch (method.getName()) {
                    case "runTaskTimer" -> scheduleRepeating((Runnable) arguments[1]);
                    default -> throw new UnsupportedOperationException("BukkitScheduler." + method.getName());
                });
        Server server = SharedBukkitServer.proxy(Server.class, (method, arguments) -> switch (method.getName()) {
            case "getScheduler" -> scheduler;
            case "getOnlinePlayers" -> List.of(onlookerPlayer);
            default -> throw new UnsupportedOperationException("Server." + method.getName());
        });
        return SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getServer" -> server;
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
    }

    private BukkitTask scheduleRepeating(Runnable repeatingTask) {
        repeatingTasks.add(repeatingTask);
        return SharedBukkitServer.proxy(BukkitTask.class, (method, arguments) -> switch (method.getName()) {
            case "cancel" -> {
                repeatingTasks.remove(repeatingTask);
                yield null;
            }
            default -> throw new UnsupportedOperationException("BukkitTask." + method.getName());
        });
    }
}
