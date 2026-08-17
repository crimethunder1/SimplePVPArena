package de.simplepvparena.gate;

import de.simplepvparena.arena.BlockPositionCodec;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public final class StartGateOpeningService implements StartGates {

    private static final long OPENING_PERIOD_TICKS = 1L;

    private final Plugin plugin;
    private final StartGateMarks gateMarks;
    private final StartGateDuration duration;
    private final StartGateVisualEffect visualEffect;

    private BukkitTask activeTask;

    public StartGateOpeningService(Plugin plugin, StartGateMarks gateMarks, StartGateDuration duration) {
        this(plugin, gateMarks, duration, StartGateVisualEffect.NONE);
    }

    public StartGateOpeningService(Plugin plugin, StartGateMarks gateMarks, StartGateDuration duration,
                                   StartGateVisualEffect visualEffect) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.gateMarks = Objects.requireNonNull(gateMarks, "gateMarks");
        this.duration = Objects.requireNonNull(duration, "duration");
        this.visualEffect = Objects.requireNonNull(visualEffect, "visualEffect");
    }

    public boolean isOpening() {
        return activeTask != null;
    }

    @Override
    public boolean openAtRoundStart(StartGateCountdownDisplay display) {
        Objects.requireNonNull(display, "display");
        stopOpening();
        restoreGate();
        if (gateMarks.isEmpty()) {
            return false;
        }
        int durationSeconds = duration.getSeconds();
        List<GateRun> runs = planRuns(durationSeconds);
        if (runs.isEmpty()) {
            return false;
        }
        OpeningRun run = new OpeningRun(runs, display, durationSeconds);
        showCountdown(run, durationSeconds);
        activeTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> runNextTick(run),
                OPENING_PERIOD_TICKS, OPENING_PERIOD_TICKS);
        return true;
    }

    @Override
    public void stopAndRestore() {
        stopOpening();
        restoreGate();
    }

    public int restoreGate() {
        int restoredBlockCount = 0;
        for (String worldName : gateMarks.getWorldNames()) {
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("The world " + worldName + " is not loaded, the start gate of "
                        + "that world could not be restored");
                continue;
            }
            for (Map.Entry<Long, Material> mark : gateMarks.getMarks(worldName).entrySet()) {
                restoredBlockCount += writeBlock(world, mark.getKey(), mark.getValue());
            }
        }
        return restoredBlockCount;
    }

    public void stopOpening() {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }

    private List<GateRun> planRuns(int durationSeconds) {
        List<GateRun> runs = new ArrayList<>();
        for (String worldName : gateMarks.getWorldNames()) {
            long[][] layers = gateMarks.collectLayersTopDown(worldName);
            runs.add(new GateRun(worldName, layers,
                    StartGateOpeningSchedule.of(layers.length, durationSeconds)));
        }
        return runs;
    }

    private void runNextTick(OpeningRun run) {
        try {
            run.elapsedTicks++;
            removeDueLayers(run);
            showElapsedSecond(run);
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.SEVERE, "The start gate ran into a failure and is stopped, "
                    + "the round itself carries on", failure);
            stopOpening();
            return;
        }
        if (run.isOver()) {
            stopOpening();
        }
    }

    private void removeDueLayers(OpeningRun run) {
        for (GateRun gate : run.gates) {
            while (gate.hasLayerLeft() && run.elapsedTicks >= gate.getRemovalTickOfNextLayer()) {
                removeNextLayer(run, gate);
            }
        }
    }

    private void removeNextLayer(OpeningRun run, GateRun gate) {
        World world = plugin.getServer().getWorld(gate.worldName);
        if (world == null) {
            plugin.getLogger().warning("The world " + gate.worldName + " is not loaded, the start gate of "
                    + "that world stays where it is");
            gate.giveUp();
            return;
        }
        long[] layer = gate.takeNextLayer();
        for (long encodedPosition : layer) {
            writeBlock(world, encodedPosition, Material.AIR);
        }
        showRemovedLayer(run, world, layer);
    }

    private void showRemovedLayer(OpeningRun run, World world, long[] layer) {
        if (run.visualEffectStopped) {
            return;
        }
        try {
            visualEffect.showRemovedLayer(world, layer);
        } catch (RuntimeException failure) {
            run.visualEffectStopped = true;
            plugin.getLogger().log(Level.WARNING, "The particles of the start gate could not be drawn, "
                    + "the gate itself carries on without them", failure);
        }
    }

    private void showElapsedSecond(OpeningRun run) {
        if (run.elapsedTicks % StartGateOpeningSchedule.TICKS_PER_SECOND != 0L) {
            return;
        }
        int elapsedSeconds = (int) (run.elapsedTicks / StartGateOpeningSchedule.TICKS_PER_SECOND);
        if (elapsedSeconds > run.durationSeconds) {
            return;
        }
        showCountdown(run, run.durationSeconds - elapsedSeconds);
    }

    private void showCountdown(OpeningRun run, int remainingSeconds) {
        if (run.displayStopped) {
            return;
        }
        try {
            run.display.show(remainingSeconds);
        } catch (RuntimeException failure) {
            run.displayStopped = true;
            plugin.getLogger().log(Level.WARNING, "The countdown of the start gate could not be shown, "
                    + "the gate itself carries on without it", failure);
        }
    }

    private static int writeBlock(World world, long encodedPosition, Material material) {
        Block block = world.getBlockAt(BlockPositionCodec.decodeX(encodedPosition),
                BlockPositionCodec.decodeY(encodedPosition), BlockPositionCodec.decodeZ(encodedPosition));
        if (block.getType() == material) {
            return 0;
        }
        block.setType(material, false);
        return 1;
    }

    private static final class OpeningRun {

        private final List<GateRun> gates;
        private final StartGateCountdownDisplay display;
        private final int durationSeconds;
        private final long totalDurationTicks;

        private long elapsedTicks;
        private boolean visualEffectStopped;
        private boolean displayStopped;

        private OpeningRun(List<GateRun> gates, StartGateCountdownDisplay display, int durationSeconds) {
            this.gates = gates;
            this.display = display;
            this.durationSeconds = durationSeconds;
            this.totalDurationTicks = durationSeconds * StartGateOpeningSchedule.TICKS_PER_SECOND;
        }

        private boolean isOver() {
            if (elapsedTicks < totalDurationTicks) {
                return false;
            }
            for (GateRun gate : gates) {
                if (gate.hasLayerLeft()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class GateRun {

        private final String worldName;
        private final long[][] layersTopDown;
        private final StartGateOpeningSchedule schedule;

        private int nextLayerIndex;

        private GateRun(String worldName, long[][] layersTopDown, StartGateOpeningSchedule schedule) {
            this.worldName = worldName;
            this.layersTopDown = layersTopDown;
            this.schedule = schedule;
        }

        private boolean hasLayerLeft() {
            return nextLayerIndex < layersTopDown.length;
        }

        private long getRemovalTickOfNextLayer() {
            return schedule.getRemovalTickOfLayer(nextLayerIndex);
        }

        private long[] takeNextLayer() {
            long[] layer = layersTopDown[nextLayerIndex];
            nextLayerIndex++;
            return layer;
        }

        private void giveUp() {
            nextLayerIndex = layersTopDown.length;
        }
    }
}
