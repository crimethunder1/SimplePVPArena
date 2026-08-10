package de.simplepvparena.border;

import de.simplepvparena.arena.BlockPositionCodec;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class BorderParticleService {

    public static final int DEFAULT_VIEW_RADIUS = 12;
    public static final int DEFAULT_MAXIMUM_BLOCKS_PER_PLAYER = 24;
    public static final long DEFAULT_PERIOD_TICKS = 30L;

    private final Plugin plugin;
    private final BorderMarkIndex borderMarks;
    private final BorderVisualEffect visualEffect;
    private final ActiveFighterLookup activeFighters;

    private int viewRadius = DEFAULT_VIEW_RADIUS;
    private int maximumBlocksPerPlayer = DEFAULT_MAXIMUM_BLOCKS_PER_PLAYER;
    private long periodTicks = DEFAULT_PERIOD_TICKS;
    private BukkitTask activeTask;

    public BorderParticleService(Plugin plugin, BorderMarkIndex borderMarks, BorderVisualEffect visualEffect,
                                 ActiveFighterLookup activeFighters) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.borderMarks = Objects.requireNonNull(borderMarks, "borderMarks");
        this.visualEffect = Objects.requireNonNull(visualEffect, "visualEffect");
        this.activeFighters = Objects.requireNonNull(activeFighters, "activeFighters");
    }

    public void setViewRadius(int viewRadius) {
        this.viewRadius = Math.max(0, viewRadius);
    }

    public void setMaximumBlocksPerPlayer(int maximumBlocksPerPlayer) {
        this.maximumBlocksPerPlayer = Math.max(1, maximumBlocksPerPlayer);
    }

    public void setPeriodTicks(long periodTicks) {
        this.periodTicks = Math.max(1L, periodTicks);
    }

    public boolean isRunning() {
        return activeTask != null;
    }

    public void start() {
        if (isRunning()) {
            return;
        }
        activeTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::showBorderToNearbyPlayers,
                periodTicks, periodTicks);
    }

    public void stop() {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }

    private void showBorderToNearbyPlayers() {
        if (borderMarks.getWorldNames().isEmpty()) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (activeFighters.isActiveFighter(player.getUniqueId())) {
                continue;
            }
            showBorderTo(player);
        }
    }

    private void showBorderTo(Player player) {
        Location playerLocation = player.getLocation();
        World world = playerLocation.getWorld();
        if (world == null) {
            return;
        }
        long[] nearbyMarks = borderMarks.collectMarksNear(world.getName(), playerLocation.getBlockX(),
                playerLocation.getBlockY(), playerLocation.getBlockZ(), viewRadius, maximumBlocksPerPlayer);
        for (long encodedPosition : nearbyMarks) {
            visualEffect.showAt(player, topFaceCenterOf(world, encodedPosition));
        }
    }

    static Location topFaceCenterOf(World world, long encodedPosition) {
        return new Location(world,
                BlockPositionCodec.decodeX(encodedPosition) + 0.5D,
                BlockPositionCodec.decodeY(encodedPosition) + 1.0D,
                BlockPositionCodec.decodeZ(encodedPosition) + 0.5D);
    }
}
