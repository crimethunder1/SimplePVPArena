package de.simplepvparena.border;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class BorderWallPreview {

    public static final long DEFAULT_DURATION_TICKS = 20L * 4L;

    private static final long REDRAW_PERIOD_TICKS = 20L;
    private static final int CONNECTION_POINT_COUNT = 2;

    private final Plugin plugin;
    private final BorderVisualEffect visualEffect;
    private final Map<UUID, BukkitTask> activeTasksByPlayer = new HashMap<>();

    private long durationTicks = DEFAULT_DURATION_TICKS;

    public BorderWallPreview(Plugin plugin, BorderVisualEffect visualEffect) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.visualEffect = Objects.requireNonNull(visualEffect, "visualEffect");
    }

    public void setDurationTicks(long durationTicks) {
        this.durationTicks = Math.max(1L, durationTicks);
    }

    public boolean show(Player player, World world, BorderWallOutline outline) {
        stopFor(player);
        if (outline.getConnections().isEmpty()) {
            return false;
        }
        UUID playerUniqueId = player.getUniqueId();
        long redrawCount = Math.max(1L, durationTicks / REDRAW_PERIOD_TICKS);
        RedrawProgress progress = new RedrawProgress(redrawCount);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> drawOnce(playerUniqueId, world, outline, progress), 0L, REDRAW_PERIOD_TICKS);
        activeTasksByPlayer.put(playerUniqueId, task);
        return true;
    }

    public void stopFor(Player player) {
        stopFor(player.getUniqueId());
    }

    public void stopAll() {
        for (BukkitTask runningTask : activeTasksByPlayer.values()) {
            runningTask.cancel();
        }
        activeTasksByPlayer.clear();
    }

    private void stopFor(UUID playerUniqueId) {
        BukkitTask runningTask = activeTasksByPlayer.remove(playerUniqueId);
        if (runningTask != null) {
            runningTask.cancel();
        }
    }

    private void drawOnce(UUID playerUniqueId, World world, BorderWallOutline outline, RedrawProgress progress) {
        Player player = plugin.getServer().getPlayer(playerUniqueId);
        if (player == null || !player.isOnline()) {
            stopFor(playerUniqueId);
            return;
        }
        for (BorderWallConnection connection : outline.getConnections()) {
            drawConnection(player, world, connection);
        }
        if (++progress.drawnCount >= progress.redrawCount) {
            stopFor(playerUniqueId);
        }
    }

    private void drawConnection(Player player, World world, BorderWallConnection connection) {
        BorderLinePoints.walk(connection.firstPosition(), connection.secondPosition(),
                BorderLinePoints.BLOCK_CENTER_OFFSET, CONNECTION_POINT_COUNT,
                (pointX, pointY, pointZ) -> visualEffect.showConnection(player,
                        new Location(world, pointX, pointY, pointZ)));
    }

    private static final class RedrawProgress {

        private final long redrawCount;

        private long drawnCount;

        private RedrawProgress(long redrawCount) {
            this.redrawCount = redrawCount;
        }
    }
}
