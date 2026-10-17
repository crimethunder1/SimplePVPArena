package de.simplepvparena.support;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class TickingScheduler {

    public static final Instant START = Instant.parse("2026-10-17T12:00:00Z");
    public static final long TICKS_PER_SECOND = 20L;

    private static final long MILLISECONDS_PER_TICK = 50L;

    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();
    private final List<String> logLines = new ArrayList<>();
    private final Plugin plugin;
    private final Clock clock = new TickingClock();

    private long currentTick;

    public TickingScheduler() {
        Logger logger = Logger.getLogger("TickingScheduler." + UUID.randomUUID());
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(new Handler() {

            @Override
            public void publish(LogRecord logRecord) {
                logLines.add(logRecord.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        BukkitScheduler scheduler = SharedBukkitServer.proxy(BukkitScheduler.class,
                (method, arguments) -> switch (method.getName()) {
                    case "runTaskLater" -> schedule((Runnable) arguments[1], (Long) arguments[2]);
                    default -> throw new UnsupportedOperationException("BukkitScheduler." + method.getName());
                });
        Server server = SharedBukkitServer.proxy(Server.class, (method, arguments) -> switch (method.getName()) {
            case "getScheduler" -> scheduler;
            default -> throw new UnsupportedOperationException("Server." + method.getName());
        });
        plugin = SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getServer" -> server;
            case "getLogger" -> logger;
            case "getName", "namespace" -> "simplepvparena";
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
    }

    public Plugin plugin() {
        return plugin;
    }

    public Clock clock() {
        return clock;
    }

    public List<String> logLines() {
        return logLines;
    }

    public void advanceSeconds(long seconds) {
        advanceTicks(seconds * TICKS_PER_SECOND);
    }

    public void advanceTicks(long tickCount) {
        for (long tick = 0; tick < tickCount; tick++) {
            currentTick++;
            for (ScheduledTask task : List.copyOf(scheduledTasks)) {
                if (task.dueTick == currentTick && !task.cancelled) {
                    scheduledTasks.remove(task);
                    task.runnable.run();
                }
            }
            scheduledTasks.removeIf(task -> task.cancelled);
        }
    }

    public int pendingTaskCount() {
        return (int) scheduledTasks.stream().filter(task -> !task.cancelled).count();
    }

    private BukkitTask schedule(Runnable runnable, long delayTicks) {
        ScheduledTask task = new ScheduledTask(runnable, currentTick + Math.max(1L, delayTicks));
        scheduledTasks.add(task);
        return SharedBukkitServer.proxy(BukkitTask.class, (method, arguments) -> switch (method.getName()) {
            case "cancel" -> {
                task.cancelled = true;
                yield null;
            }
            case "isCancelled" -> task.cancelled;
            case "getTaskId" -> 1;
            default -> throw new UnsupportedOperationException("BukkitTask." + method.getName());
        });
    }

    private static final class ScheduledTask {

        private final Runnable runnable;
        private final long dueTick;

        private boolean cancelled;

        private ScheduledTask(Runnable runnable, long dueTick) {
            this.runnable = runnable;
            this.dueTick = dueTick;
        }
    }

    private final class TickingClock extends Clock {

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException("the ticking clock stays in UTC");
        }

        @Override
        public Instant instant() {
            return START.plusMillis(currentTick * MILLISECONDS_PER_TICK);
        }
    }
}
