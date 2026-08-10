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

    public boolean isCleaning() {
        return activeTask != null;
    }

    public static long countBlocks(ArenaShape shape) {
        return shape.getBlockCount();
    }

    public boolean startCleaning(ArenaShape shape, Consumer<ArenaCleaningReport> completionHandler) {
        if (isCleaning()) {
            return false;
        }
        ArenaShapeBlockCursor cursor = new ArenaShapeBlockCursor(shape);
        CleaningProgress progress = new CleaningProgress();
        activeTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> cleanNextBatch(cursor, progress, completionHandler),
                BATCH_PERIOD_TICKS, BATCH_PERIOD_TICKS);
        return true;
    }

    private void cleanNextBatch(ArenaShapeBlockCursor cursor, CleaningProgress progress,
                                Consumer<ArenaCleaningReport> completionHandler) {
        ArenaShape shape = cursor.getShape();
        World world = plugin.getServer().getWorld(shape.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("The world " + shape.getWorldName()
                    + " is not loaded, the arena cleaning is stopped");
            finish(progress, completionHandler);
            return;
        }
        int remainingBlocksOfThisTick = blocksPerTick;
        while (remainingBlocksOfThisTick > 0 && cursor.hasRemainingBlocks()) {
            long[] batch = cursor.nextBatch(remainingBlocksOfThisTick);
            remainingBlocksOfThisTick -= batch.length;
            progress.checkedBlockCount += batch.length;
            progress.clearedBlockCount += clearBatch(world, batch);
        }
        if (cursor.hasRemainingBlocks()) {
            return;
        }
        progress.clearedBorderMarkCount += cleanedArenaHandler.onArenaCleaned(world, shape);
        finish(progress, completionHandler);
    }

    private void finish(CleaningProgress progress, Consumer<ArenaCleaningReport> completionHandler) {
        stopCleaning();
        completionHandler.accept(new ArenaCleaningReport(progress.checkedBlockCount, progress.clearedBlockCount,
                progress.clearedBorderMarkCount));
    }

    private long clearBatch(World world, long[] batch) {
        long clearedBlockCount = 0L;
        for (long encodedPosition : batch) {
            Block block = world.getBlockAt(BlockPositionCodec.decodeX(encodedPosition),
                    BlockPositionCodec.decodeY(encodedPosition),
                    BlockPositionCodec.decodeZ(encodedPosition));
            if (block.getType() == Material.AIR) {
                continue;
            }
            block.setType(Material.AIR, false);
            clearedBlockCount++;
        }
        return clearedBlockCount;
    }

    public void stopCleaning() {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }

    private static final class CleaningProgress {

        private long checkedBlockCount;
        private long clearedBlockCount;
        private int clearedBorderMarkCount;
    }
}
