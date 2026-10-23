package de.simplepvparena.listener;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaMatchHarnessSchedulerTest {

    private final ArenaMatchHarness harness = new ArenaMatchHarness();
    private final Plugin plugin = harness.getPlugin();

    @Test
    void aRepeatingTaskKeepsRunningAfterRunTick() {
        AtomicInteger runCount = new AtomicInteger();
        plugin.getServer().getScheduler().runTaskTimer(plugin, runCount::incrementAndGet, 20L, 20L);

        harness.runTick();
        harness.advanceTicksWithRepeatingTasks(20L);

        assertEquals(2, runCount.get());
        assertEquals(1, harness.getPendingTaskCount());
    }

    @Test
    void runTickRemovesASingleTaskAndACancelledRepeatingTask() {
        AtomicInteger singleRunCount = new AtomicInteger();
        AtomicInteger repeatingRunCount = new AtomicInteger();
        plugin.getServer().getScheduler().runTask(plugin, singleRunCount::incrementAndGet);
        BukkitTask repeatingTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                repeatingRunCount::incrementAndGet, 1L, 1L);
        repeatingTask.cancel();

        harness.runTick();
        harness.runTick();

        assertEquals(1, singleRunCount.get());
        assertEquals(0, repeatingRunCount.get());
        assertEquals(0, harness.getScheduledTaskCount());
    }
}
