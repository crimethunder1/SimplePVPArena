package de.simplepvparena.arena;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class ArenaBaselineService {

    public static final int DEFAULT_BLOCKS_PER_TICK = 16_384;

    public static final int DEFAULT_DEPTH_BELOW_FLOOR = 11;
    public static final int MAXIMUM_DEPTH_BELOW_FLOOR = 64;

    private static final long BATCH_PERIOD_TICKS = 1L;

    private final Plugin plugin;
    private final ArenaBaselineRegistry baselineRegistry;

    private int blocksPerTick = DEFAULT_BLOCKS_PER_TICK;
    private int depthBelowFloor = DEFAULT_DEPTH_BELOW_FLOOR;
    private BukkitTask activeTask;

    public ArenaBaselineService(Plugin plugin, ArenaBaselineRegistry baselineRegistry) {
        this.plugin = plugin;
        this.baselineRegistry = baselineRegistry;
    }

    public void setBlocksPerTick(int blocksPerTick) {
        this.blocksPerTick = Math.max(1, blocksPerTick);
    }

    public int getBlocksPerTick() {
        return blocksPerTick;
    }

    public void setDepthBelowFloor(int depthBelowFloor) {
        this.depthBelowFloor = Math.max(0, Math.min(MAXIMUM_DEPTH_BELOW_FLOOR, depthBelowFloor));
    }

    public int getDepthBelowFloor() {
        return depthBelowFloor;
    }

    public boolean isCapturing() {
        return activeTask != null;
    }

    public ArenaShape findCapturedVolume(ArenaShape shape) {
        return shape.withFloorLoweredBy(depthBelowFloor);
    }

    public boolean isTooLargeForBaseline(ArenaShape shape) {
        return findCapturedVolume(shape).getBlockCount() > ArenaBaselineSnapshot.MAXIMUM_BLOCK_COUNT;
    }

    public Optional<ArenaBaselineSnapshot> findSnapshot(ArenaShape shape) {
        return baselineRegistry.find(shape);
    }

    public Optional<ArenaBaselineSnapshot> findStoredSnapshot() {
        return baselineRegistry.findStored();
    }

    public void discardSnapshot() {
        stopCapture();
        baselineRegistry.discard();
    }

    public boolean startCapture(ArenaShape shape, Consumer<ArenaBaselineReport> completionHandler) {
        discardSnapshot();
        if (isTooLargeForBaseline(shape)) {
            return false;
        }
        ArenaBaselineSnapshotBuilder builder = new ArenaBaselineSnapshotBuilder(shape, depthBelowFloor);
        ArenaShapeBlockCursor cursor = new ArenaShapeBlockCursor(builder.getCapturedVolume());
        activeTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> captureNextBatch(cursor, builder, completionHandler),
                BATCH_PERIOD_TICKS, BATCH_PERIOD_TICKS);
        return true;
    }

    private void captureNextBatch(ArenaShapeBlockCursor cursor, ArenaBaselineSnapshotBuilder builder,
                                  Consumer<ArenaBaselineReport> completionHandler) {
        ArenaShape shape = cursor.getShape();
        World world = plugin.getServer().getWorld(shape.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("The world " + shape.getWorldName()
                    + " is not loaded, no baseline of the fight area is taken");
            stopCapture();
            completionHandler.accept(ArenaBaselineReport.failed("the world " + shape.getWorldName()
                    + " is not loaded"));
            return;
        }
        ArenaBaselineSnapshot snapshot;
        try {
            int remainingBlocksOfThisTick = blocksPerTick;
            while (remainingBlocksOfThisTick > 0 && cursor.hasRemainingBlocks()) {
                long[] batch = cursor.nextBatch(remainingBlocksOfThisTick);
                remainingBlocksOfThisTick -= batch.length;
                readBatch(world, batch, builder);
            }
            if (cursor.hasRemainingBlocks()) {
                return;
            }
            snapshot = builder.build();
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.SEVERE, "The baseline of the fight area in " + shape.getWorldName()
                    + " could not be taken", failure);
            stopCapture();
            completionHandler.accept(ArenaBaselineReport.failed(describeFailure(failure)));
            return;
        }
        stopCapture();
        baselineRegistry.store(snapshot);
        completionHandler.accept(ArenaBaselineReport.captured(snapshot));
    }

    private static String describeFailure(RuntimeException failure) {
        String message = failure.getMessage();
        return "its blocks could not be read ("
                + (message == null || message.isBlank() ? failure.getClass().getSimpleName() : message) + ")";
    }

    private void readBatch(World world, long[] batch, ArenaBaselineSnapshotBuilder builder) {
        for (long encodedPosition : batch) {
            int blockX = BlockPositionCodec.decodeX(encodedPosition);
            int blockY = BlockPositionCodec.decodeY(encodedPosition);
            int blockZ = BlockPositionCodec.decodeZ(encodedPosition);
            Block block = world.getBlockAt(blockX, blockY, blockZ);
            builder.record(blockX, blockY, blockZ, block.getBlockData(), () -> block.getState(true));
        }
    }

    public void stopCapture() {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }
}
