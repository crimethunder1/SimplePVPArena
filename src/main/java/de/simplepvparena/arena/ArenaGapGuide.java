package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaGapMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.UUID;

public final class ArenaGapGuide {

    public static final Particle TRAIL_PARTICLE = Particle.FLAME;

    public static final long DEFAULT_PERIOD_TICKS = 10L;

    private static final int SINGLE_PARTICLE_COUNT = 1;
    private static final double NO_OFFSET = 0.0D;
    private static final double NO_SPEED = 0.0D;

    private final Plugin plugin;
    private final ArenaGapTrailRegistry runningTrails = new ArenaGapTrailRegistry();

    private long periodTicks = DEFAULT_PERIOD_TICKS;

    public ArenaGapGuide(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void setPeriodTicks(long periodTicks) {
        this.periodTicks = Math.max(1L, periodTicks);
    }

    public boolean isGuiding(Player admin) {
        return runningTrails.isRunningFor(admin.getUniqueId());
    }

    public boolean guideTo(Player admin, World world, ArenaGapTarget target) {
        UUID adminUniqueId = admin.getUniqueId();
        BukkitTask trailTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> drawTrailOnce(adminUniqueId, target), 0L, periodTicks);
        runningTrails.replace(adminUniqueId, trailTask::cancel);
        return ArenaGapCompass.giveTo(admin, ArenaGapCompass.create(world, target));
    }

    public void stopFor(Player admin) {
        runningTrails.stop(admin.getUniqueId());
    }

    public void stopAll() {
        runningTrails.stopAll();
    }

    private void drawTrailOnce(UUID adminUniqueId, ArenaGapTarget target) {
        Player admin = plugin.getServer().getPlayer(adminUniqueId);
        if (admin == null || !admin.isOnline()) {
            runningTrails.stop(adminUniqueId);
            return;
        }
        Location adminLocation = admin.getLocation();
        World world = adminLocation.getWorld();
        if (world == null || !world.getName().equals(target.worldName())) {
            return;
        }
        if (ArenaGapTrailPoints.hasArrived(adminLocation.getX(), adminLocation.getZ(), target.blockX(),
                target.blockZ())) {
            runningTrails.stop(adminUniqueId);
            TournamentMessages.sendSuccess(admin, Messages.text(ArenaGapMessage.REACHED,
                    Placeholder.of("target", target.describe()),
                    Placeholder.of("hint", target.reason().getArrivalHint())));
            return;
        }
        ArenaGapTrailPoints.walkTowards(adminLocation.getX(), adminLocation.getY(), adminLocation.getZ(),
                target.blockX(), target.blockZ(),
                (pointX, pointY, pointZ) -> admin.spawnParticle(TRAIL_PARTICLE, pointX, pointY, pointZ,
                        SINGLE_PARTICLE_COUNT, NO_OFFSET, NO_OFFSET, NO_OFFSET, NO_SPEED));
    }
}
