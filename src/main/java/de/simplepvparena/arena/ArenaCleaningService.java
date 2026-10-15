package de.simplepvparena.arena;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class ArenaCleaningService {

    public static final int DEFAULT_BLOCKS_PER_TICK = 4096;

    private static final long BATCH_PERIOD_TICKS = 1L;

    private static final long TICKS_PER_SECOND = 20L;

    private final Plugin plugin;
    private final CleanedArenaHandler cleanedArenaHandler;
    private final ArenaCleaningVisualEffect visualEffect;
    private final ArenaCleaningExclusion exclusion;

    private int blocksPerTick = DEFAULT_BLOCKS_PER_TICK;
    private long animationDurationTicks = ArenaCleaningParticleSettings.defaults().animationDurationTicks();
    private BukkitTask activeTask;

    public ArenaCleaningService(Plugin plugin, CleanedArenaHandler cleanedArenaHandler) {
        this(plugin, cleanedArenaHandler, ArenaCleaningVisualEffect.NONE);
    }

    public ArenaCleaningService(Plugin plugin, CleanedArenaHandler cleanedArenaHandler,
                                ArenaCleaningVisualEffect visualEffect) {
        this(plugin, cleanedArenaHandler, visualEffect, ArenaCleaningExclusion.NONE);
    }

    public ArenaCleaningService(Plugin plugin, CleanedArenaHandler cleanedArenaHandler,
                                ArenaCleaningVisualEffect visualEffect,
                                ArenaCleaningExclusion exclusion) {
        this.plugin = plugin;
        this.cleanedArenaHandler = cleanedArenaHandler;
        this.visualEffect = Objects.requireNonNull(visualEffect, "visualEffect");
        this.exclusion = Objects.requireNonNull(exclusion, "exclusion");
    }

    public void setBlocksPerTick(int blocksPerTick) {
        this.blocksPerTick = Math.max(1, blocksPerTick);
    }

    public int getBlocksPerTick() {
        return blocksPerTick;
    }

    public void setAnimationDurationTicks(long animationDurationTicks) {
        this.animationDurationTicks = Math.max(0L, animationDurationTicks);
    }

    public long getAnimationDurationTicks() {
        return animationDurationTicks;
    }

    public boolean isCleaning() {
        return activeTask != null;
    }

    public static long countBlocks(ArenaShape shape) {
        return shape.getBlockCount();
    }

    public String describeAnimation() {
        if (animationDurationTicks <= 0L) {
            return "The two coloured phases are switched off, the layers are cleared as fast as the "
                    + "batches allow";
        }
        return "Layer by layer from the floor up: " + ArenaCleaningPhase.FLOOD.describe()
                + " announces the volume, " + ArenaCleaningPhase.CLEAR.describe() + " writes it, "
                + (animationDurationTicks / (double) TICKS_PER_SECOND) + " seconds for both together";
    }

    public boolean startCleaning(ArenaCleaningPlan plan,
                                 Consumer<ArenaCleaningReport> completionHandler) {
        if (isCleaning()) {
            return false;
        }
        ArenaShape shape = plan.getCleanedVolume();
        ArenaCleaningTarget cleaningTarget = plan.getTarget();
        CleaningRun run = new CleaningRun(shape,
                ArenaCleaningLayerSchedule.of(shape.getHeight(), animationDurationTicks));
        activeTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> runNextTick(run, cleaningTarget, completionHandler),
                BATCH_PERIOD_TICKS, BATCH_PERIOD_TICKS);
        return true;
    }

    private void runNextTick(CleaningRun run, ArenaCleaningTarget cleaningTarget,
                             Consumer<ArenaCleaningReport> completionHandler) {
        ArenaShape shape = run.shape;
        World world = plugin.getServer().getWorld(shape.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("The world " + shape.getWorldName()
                    + " is not loaded, the arena cleaning is stopped");
            finish(cleaningTarget, run, "the world " + shape.getWorldName() + " is not loaded",
                    completionHandler);
            return;
        }
        try {
            showFloodedLayer(world, run);
            if (run.phase.writesBlocks()) {
                clearCurrentLayer(world, run, cleaningTarget);
            }
            run.endTickOfLayer();
            if (!run.finished) {
                return;
            }
            run.clearedBorderMarkCount += cleanedArenaHandler.onArenaCleaned(world, shape);
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.SEVERE, "The arena cleaning in " + shape.getWorldName()
                    + " ran into a failure and is stopped", failure);
            finish(cleaningTarget, run, describeFailure(failure), completionHandler);
            return;
        }
        finish(cleaningTarget, run, null, completionHandler);
    }

    private void clearCurrentLayer(World world, CleaningRun run, ArenaCleaningTarget cleaningTarget) {
        int remainingBlocksOfThisTick = blocksPerTick;
        while (remainingBlocksOfThisTick > 0 && run.hasUnwrittenBlocksInLayer()) {
            long[] batch = run.nextBatchOfLayer(remainingBlocksOfThisTick);
            remainingBlocksOfThisTick -= batch.length;
            run.checkedBlockCount += batch.length;
            run.changedBlockCount += writeBatch(world, run.shape.getWorldName(), batch, cleaningTarget);
        }
    }

    private void finish(ArenaCleaningTarget cleaningTarget, CleaningRun run, String failureReason,
                        Consumer<ArenaCleaningReport> completionHandler) {
        stopCleaning();
        completionHandler.accept(new ArenaCleaningReport(run.checkedBlockCount, run.changedBlockCount,
                run.clearedBorderMarkCount, cleaningTarget.describeChange(), failureReason));
    }

    private static String describeFailure(RuntimeException failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private void showFloodedLayer(World world, CleaningRun run) {
        if (run.visualEffectStopped) {
            return;
        }
        try {
            visualEffect.showFloodedLayer(world, run.shape, run.phase, run.layerY());
        } catch (RuntimeException failure) {
            run.visualEffectStopped = true;
            plugin.getLogger().log(Level.WARNING, "The particles of the arena cleaning in "
                    + run.shape.getWorldName() + " could not be drawn, the cleaning itself carries on "
                    + "without them", failure);
        }
    }

    private long writeBatch(World world, String worldName, long[] batch,
                            ArenaCleaningTarget cleaningTarget) {
        long changedBlockCount = 0L;
        for (long encodedPosition : batch) {
            int blockX = BlockPositionCodec.decodeX(encodedPosition);
            int blockY = BlockPositionCodec.decodeY(encodedPosition);
            int blockZ = BlockPositionCodec.decodeZ(encodedPosition);
            if (exclusion.isExcluded(worldName, blockX, blockY, blockZ)) {
                continue;
            }
            Optional<BlockState> targetBlockEntity = cleaningTarget.findBlockEntity(blockX, blockY, blockZ);
            if (targetBlockEntity.isPresent()) {
                targetBlockEntity.get().update(true, false);
                changedBlockCount++;
                continue;
            }
            Block block = world.getBlockAt(blockX, blockY, blockZ);
            Optional<BlockData> targetBlockData = cleaningTarget.findBlockData(blockX, blockY, blockZ);
            if (targetBlockData.isPresent()) {
                if (block.getBlockData().equals(targetBlockData.get())) {
                    continue;
                }
                block.setBlockData(targetBlockData.get(), false);
                changedBlockCount++;
                continue;
            }
            Material targetMaterial = cleaningTarget.findMaterial(blockX, blockY, blockZ);
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

    private static final class CleaningRun {

        private final ArenaShape shape;
        private final ArenaCleaningLayerSchedule schedule;
        private final ArenaShapeBlockCursor cursor;
        private final int layerBlockCount;

        private ArenaCleaningPhase phase;
        private int layerIndex;
        private long ticksSpentInLayer;
        private long remainingBlocksOfLayer;
        private boolean finished;

        private long checkedBlockCount;
        private long changedBlockCount;
        private int clearedBorderMarkCount;

        private boolean visualEffectStopped;

        private CleaningRun(ArenaShape shape, ArenaCleaningLayerSchedule schedule) {
            this.shape = shape;
            this.schedule = schedule;
            this.cursor = new ArenaShapeBlockCursor(shape);
            this.layerBlockCount = shape.getCellCount();
            this.phase = schedule.isAnimated() ? ArenaCleaningPhase.FLOOD : ArenaCleaningPhase.CLEAR;
            this.remainingBlocksOfLayer = layerBlockCount;
        }

        private int layerY() {
            return shape.getFloorY() + layerIndex;
        }

        private boolean hasUnwrittenBlocksInLayer() {
            return remainingBlocksOfLayer > 0L;
        }

        private long[] nextBatchOfLayer(int maximumBlockCount) {
            long[] batch = cursor.nextBatch((int) Math.min(maximumBlockCount, remainingBlocksOfLayer));
            remainingBlocksOfLayer -= batch.length;
            return batch;
        }

        private void endTickOfLayer() {
            ticksSpentInLayer++;
            if (phase.writesBlocks() && hasUnwrittenBlocksInLayer()) {
                return;
            }
            if (ticksSpentInLayer < schedule.ticksForLayer(layerIndex)) {
                return;
            }
            startNextLayer();
        }

        private void startNextLayer() {
            layerIndex++;
            ticksSpentInLayer = 0L;
            remainingBlocksOfLayer = layerBlockCount;
            if (layerIndex < schedule.getLayerCount()) {
                return;
            }
            layerIndex = 0;
            if (phase.writesBlocks()) {
                finished = true;
                return;
            }
            phase = ArenaCleaningPhase.CLEAR;
        }
    }
}
