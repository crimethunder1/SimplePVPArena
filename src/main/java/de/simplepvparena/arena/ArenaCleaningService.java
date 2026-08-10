package de.simplepvparena.arena;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public final class ArenaCleaningService {

    public static final int DEFAULT_BLOCKS_PER_TICK = 4096;

    private static final long BATCH_PERIOD_TICKS = 1L;

    private final Plugin plugin;
    private final CleanedRegionHandler cleanedRegionHandler;

    private int blocksPerTick = DEFAULT_BLOCKS_PER_TICK;
    private BukkitTask activeTask;

    public ArenaCleaningService(Plugin plugin, CleanedRegionHandler cleanedRegionHandler) {
        this.plugin = plugin;
        this.cleanedRegionHandler = cleanedRegionHandler;
    }

    public void setBlocksPerTick(int blocksPerTick) {
        this.blocksPerTick = Math.max(1, blocksPerTick);
    }

    public boolean isCleaning() {
        return activeTask != null;
    }

    public static long countBlocks(List<RegionBoundary> regions) {
        return regions.stream().mapToLong(region -> new RegionBlockCursor(region).getTotalBlockCount()).sum();
    }

    public boolean startCleaning(List<RegionBoundary> regions, Consumer<ArenaCleaningReport> completionHandler) {
        if (isCleaning() || regions.isEmpty()) {
            return false;
        }
        Deque<RegionBlockCursor> remainingCursors = new ArrayDeque<>();
        regions.forEach(region -> remainingCursors.add(new RegionBlockCursor(region)));
        CleaningProgress progress = new CleaningProgress();
        activeTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> cleanNextBatch(remainingCursors, progress, completionHandler),
                BATCH_PERIOD_TICKS, BATCH_PERIOD_TICKS);
        return true;
    }

    private void cleanNextBatch(Deque<RegionBlockCursor> remainingCursors, CleaningProgress progress,
                                Consumer<ArenaCleaningReport> completionHandler) {
        int remainingBlocksOfThisTick = blocksPerTick;
        while (remainingBlocksOfThisTick > 0 && !remainingCursors.isEmpty()) {
            RegionBlockCursor cursor = remainingCursors.peek();
            World world = plugin.getServer().getWorld(cursor.getRegion().getWorldName());
            if (world == null) {
                plugin.getLogger().warning("The world " + cursor.getRegion().getWorldName()
                        + " is not loaded, its region is skipped while cleaning the arena");
                remainingCursors.poll();
                continue;
            }
            long[] batch = cursor.nextBatch(remainingBlocksOfThisTick);
            remainingBlocksOfThisTick -= batch.length;
            progress.checkedBlockCount += batch.length;
            progress.clearedBlockCount += clearBatch(world, batch);
            if (!cursor.hasRemainingBlocks()) {
                progress.clearedBorderMarkCount += cleanedRegionHandler.onRegionCleaned(world, cursor.getRegion());
                remainingCursors.poll();
            }
        }
        if (!remainingCursors.isEmpty()) {
            return;
        }
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
