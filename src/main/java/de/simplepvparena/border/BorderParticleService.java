package de.simplepvparena.border;

import de.simplepvparena.arena.ArenaBorderConfirmation;
import de.simplepvparena.arena.BlockPositionCodec;
import de.simplepvparena.arena.BorderColumnReader;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class BorderParticleService {

    public static final int DEFAULT_VIEW_RADIUS = 12;
    public static final int DEFAULT_MAXIMUM_BLOCKS_PER_PLAYER = 24;

    public static final int DEFAULT_MAXIMUM_LINES_PER_PLAYER = 2 * DEFAULT_MAXIMUM_BLOCKS_PER_PLAYER;

    public static final long DEFAULT_PERIOD_TICKS = 30L;

    private static final int CONNECTION_LINE_POINT_COUNT = 3;

    private final Plugin plugin;
    private final BorderMarkIndex borderMarks;
    private final BorderVisualEffect visualEffect;
    private final ActiveFighterLookup activeFighters;
    private final ArenaBorderConfirmation borderConfirmation;

    private int viewRadius = DEFAULT_VIEW_RADIUS;
    private int maximumBlocksPerPlayer = DEFAULT_MAXIMUM_BLOCKS_PER_PLAYER;
    private int maximumLinesPerPlayer = DEFAULT_MAXIMUM_LINES_PER_PLAYER;
    private long periodTicks = DEFAULT_PERIOD_TICKS;
    private BukkitTask activeTask;

    public BorderParticleService(Plugin plugin, BorderMarkIndex borderMarks, BorderVisualEffect visualEffect,
                                 ActiveFighterLookup activeFighters, ArenaBorderConfirmation borderConfirmation) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.borderMarks = Objects.requireNonNull(borderMarks, "borderMarks");
        this.visualEffect = Objects.requireNonNull(visualEffect, "visualEffect");
        this.activeFighters = Objects.requireNonNull(activeFighters, "activeFighters");
        this.borderConfirmation = Objects.requireNonNull(borderConfirmation, "borderConfirmation");
    }

    public void setViewRadius(int viewRadius) {
        this.viewRadius = Math.max(0, viewRadius);
    }

    public void setMaximumBlocksPerPlayer(int maximumBlocksPerPlayer) {
        this.maximumBlocksPerPlayer = Math.max(1, maximumBlocksPerPlayer);
    }

    public void setMaximumLinesPerPlayer(int maximumLinesPerPlayer) {
        this.maximumLinesPerPlayer = Math.max(0, maximumLinesPerPlayer);
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
        if (nearbyMarks.length == 0) {
            return;
        }
        if (borderConfirmation.isConfirmed()) {
            showConfirmedBlocks(player, world, nearbyMarks);
            return;
        }
        showBlocks(player, world, nearbyMarks);
        showConnectionLines(player, world, nearbyMarks);
    }

    private void showConfirmedBlocks(Player player, World world, long[] nearbyMarks) {
        for (long encodedPosition : nearbyMarks) {
            visualEffect.showConfirmedAt(player, topFaceCenterOf(world, encodedPosition));
        }
    }

    private void showBlocks(Player player, World world, long[] nearbyMarks) {
        BlockedColumnLookup blockedColumns = borderMarks.lookupIn(world.getName());
        for (long encodedPosition : nearbyMarks) {
            BorderConnectivity connectivity = BorderColumnConnectivity.of(blockedColumns,
                    BlockPositionCodec.decodeX(encodedPosition), BlockPositionCodec.decodeZ(encodedPosition));
            visualEffect.showAt(player, topFaceCenterOf(world, encodedPosition), connectivity);
        }
    }

    private void showConnectionLines(Player player, World world, long[] nearbyMarks) {
        if (maximumLinesPerPlayer == 0) {
            return;
        }
        BorderColumnReader borderColumns = borderMarks.createColumnReader(world.getName());
        for (BorderWallConnection connection
                : BorderColumnConnections.fromShownBlocks(nearbyMarks, borderColumns, maximumLinesPerPlayer)) {
            BorderLinePoints.walk(connection.firstPosition(), connection.secondPosition(),
                    BorderLinePoints.BLOCK_TOP_FACE_OFFSET, CONNECTION_LINE_POINT_COUNT,
                    (pointX, pointY, pointZ) -> visualEffect.showColumnConnection(player,
                            new Location(world, pointX, pointY, pointZ)));
        }
    }

    static Location topFaceCenterOf(World world, long encodedPosition) {
        return new Location(world,
                BlockPositionCodec.decodeX(encodedPosition) + 0.5D,
                BlockPositionCodec.decodeY(encodedPosition) + 1.0D,
                BlockPositionCodec.decodeZ(encodedPosition) + 0.5D);
    }
}
