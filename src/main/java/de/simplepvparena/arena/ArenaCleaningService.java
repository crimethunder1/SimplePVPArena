package de.simplepvparena.arena;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

public final class ArenaCleaningService {

    public static final int DEFAULT_BLOCKS_PER_TICK = 4096;

    private static final long BATCH_PERIOD_TICKS = 1L;

    private final Plugin plugin;
    private final CleanedArenaHandler cleanedArenaHandler;

    private int blocksPerTick = DEFAULT_BLOCKS_PER_TICK;
    private BukkitTask activeTask;

    public ArenaCleaningService(Plugin plugin, CleanedArenaHandler cleanedArenaHandler) {
        this.plugin = plugin;
        this.cleanedArenaHandler = cleanedArenaHandler;
    }

    public void setBlocksPerTick(int blocksPerTick) {
        this.blocksPerTick = Math.max(1, blocksPerTick);
    }

    public int getBlocksPerTick() {
        return blocksPerTick;
    }

    public boolean isCleaning() {
        return activeTask != null;
    }

    public static long countBlocks(ArenaShape shape) {
        return shape.getBlockCount();
    }

    public boolean startCleaning(ArenaShape shape, ArenaCleaningTarget cleaningTarget,
                                 Consumer<ArenaCleaningReport> completionHandler) {
        if (isCleaning()) {
            return false;
        }
        ArenaShapeBlockCursor cursor = new ArenaShapeBlockCursor(shape);
        CleaningProgress progress = new CleaningProgress();
        activeTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> cleanNextBatch(cursor, cleaningTarget, progress, completionHandler),
                BATCH_PERIOD_TICKS, BATCH_PERIOD_TICKS);
        return true;
    }

    private void cleanNextBatch(ArenaShapeBlockCursor cursor, ArenaCleaningTarget cleaningTarget,
                                CleaningProgress progress, Consumer<ArenaCleaningReport> completionHandler) {
        ArenaShape shape = cursor.getShape();
        World world = plugin.getServer().getWorld(shape.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("The world " + shape.getWorldName()
                    + " is not loaded, the arena cleaning is stopped");
            finish(cleaningTarget, progress, completionHandler);
            return;
        }
        int remainingBlocksOfThisTick = blocksPerTick;
        while (remainingBlocksOfThisTick > 0 && cursor.hasRemainingBlocks()) {
            long[] batch = cursor.nextBatch(remainingBlocksOfThisTick);
            remainingBlocksOfThisTick -= batch.length;
            progress.checkedBlockCount += batch.length;
            progress.changedBlockCount += writeBatch(world, batch, cleaningTarget);
        }
        if (cursor.hasRemainingBlocks()) {
            return;
        }
        progress.clearedBorderMarkCount += cleanedArenaHandler.onArenaCleaned(world, shape);
        finish(cleaningTarget, progress, completionHandler);
    }

    private void finish(ArenaCleaningTarget cleaningTarget, CleaningProgress progress,
                        Consumer<ArenaCleaningReport> completionHandler) {
        stopCleaning();
        completionHandler.accept(new ArenaCleaningReport(progress.checkedBlockCount, progress.changedBlockCount,
                progress.clearedBorderMarkCount, cleaningTarget.describeChange()));
    }

    private long writeBatch(World world, long[] batch, ArenaCleaningTarget cleaningTarget) {
        long changedBlockCount = 0L;
        for (long encodedPosition : batch) {
            int blockX = BlockPositionCodec.decodeX(encodedPosition);
            int blockY = BlockPositionCodec.decodeY(encodedPosition);
            int blockZ = BlockPositionCodec.decodeZ(encodedPosition);
            Material targetMaterial = cleaningTarget.findMaterial(blockX, blockY, blockZ);
            Block block = world.getBlockAt(blockX, blockY, blockZ);
            if (block.getType() == targetMaterial) {
                continue;
            }
            block.setType(targetMaterial, false);
            changedBlockCount++;
        }
        return changedBlockCount;
    }

    public void stopCleaning() {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }

    private static final class CleaningProgress {

        private long checkedBlockCount;
        private long changedBlockCount;
        private int clearedBorderMarkCount;
    }
}
