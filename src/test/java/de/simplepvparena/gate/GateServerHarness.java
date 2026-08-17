package de.simplepvparena.gate;

import de.simplepvparena.arena.BlockPositionCodec;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

final class GateServerHarness {

    private final String worldName;
    private final Map<Long, Material> blocks = new HashMap<>();
    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();
    private final Plugin plugin;
    private final Object world;

    private boolean worldLoaded = true;
    private RuntimeException blockAccessFailure;

    GateServerHarness(String worldName) {
        this.worldName = worldName;
        this.world = createWorld();
        this.plugin = createPlugin();
    }

    Plugin getPlugin() {
        return plugin;
    }

    void setBlock(int blockX, int blockY, int blockZ, Material material) {
        blocks.put(BlockPositionCodec.encode(blockX, blockY, blockZ), material);
    }

    Material getBlock(int blockX, int blockY, int blockZ) {
        return blocks.getOrDefault(BlockPositionCodec.encode(blockX, blockY, blockZ), Material.AIR);
    }

    Block blockAt(int blockX, int blockY, int blockZ) {
        return (Block) createBlock(blockX, blockY, blockZ);
    }

    void unloadWorld() {
        worldLoaded = false;
    }

    void failBlockAccessWith(RuntimeException failure) {
        this.blockAccessFailure = failure;
    }

    void runTick() {
        for (ScheduledTask task : new ArrayList<>(scheduledTasks)) {
            if (task.cancelled) {
                scheduledTasks.remove(task);
                continue;
            }
            task.runnable.run();
            if (!task.repeating) {
                task.cancelled = true;
            }
            if (task.cancelled) {
                scheduledTasks.remove(task);
            }
        }
    }

    void runTicks(int tickCount) {
        for (int tick = 0; tick < tickCount; tick++) {
            runTick();
        }
    }

    int getScheduledTaskCount() {
        return scheduledTasks.size();
    }

    private Plugin createPlugin() {
        Object server = createServer();
        Logger quietLogger = Logger.getLogger("GateServerHarness");
        quietLogger.setLevel(Level.OFF);
        return proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getServer" -> server;
            case "getLogger" -> quietLogger;
            case "getName", "namespace" -> "simplepvparena";
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
    }

    private Object createServer() {
        Object scheduler = createScheduler();
        return proxy(org.bukkit.Server.class, (method, arguments) -> switch (method.getName()) {
            case "getScheduler" -> scheduler;
            case "getWorld" -> worldLoaded && worldName.equals(arguments[0]) ? world : null;
            default -> throw new UnsupportedOperationException("Server." + method.getName());
        });
    }

    private Object createScheduler() {
        return proxy(BukkitScheduler.class, (method, arguments) -> switch (method.getName()) {
            case "runTaskTimer" -> schedule((Runnable) arguments[1], true);
            case "runTask" -> schedule((Runnable) arguments[1], false);
            default -> throw new UnsupportedOperationException("BukkitScheduler." + method.getName());
        });
    }

    private Object schedule(Runnable runnable, boolean repeating) {
        ScheduledTask task = new ScheduledTask(runnable, repeating);
        scheduledTasks.add(task);
        return proxy(BukkitTask.class, (method, arguments) -> switch (method.getName()) {
            case "cancel" -> {
                task.cancelled = true;
                scheduledTasks.remove(task);
                yield null;
            }
            case "isCancelled" -> task.cancelled;
            case "getTaskId" -> 1;
            default -> throw new UnsupportedOperationException("BukkitTask." + method.getName());
        });
    }

    private Object createWorld() {
        return proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> worldName;
            case "getBlockAt" -> createBlock((Integer) arguments[0], (Integer) arguments[1],
                    (Integer) arguments[2]);
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
    }

    private Object createBlock(int blockX, int blockY, int blockZ) {
        long encodedPosition = BlockPositionCodec.encode(blockX, blockY, blockZ);
        return proxy(Block.class, (method, arguments) -> switch (method.getName()) {
            case "getType" -> {
                failBlockAccessIfAsked();
                yield blocks.getOrDefault(encodedPosition, Material.AIR);
            }
            case "setType" -> {
                failBlockAccessIfAsked();
                blocks.put(encodedPosition, (Material) arguments[0]);
                yield null;
            }
            case "getX" -> blockX;
            case "getY" -> blockY;
            case "getZ" -> blockZ;
            case "getWorld" -> world;
            default -> throw new UnsupportedOperationException("Block." + method.getName());
        });
    }

    private void failBlockAccessIfAsked() {
        if (blockAccessFailure != null) {
            throw blockAccessFailure;
        }
    }

    private static <T> T proxy(Class<T> type, FakeCall call) {
        InvocationHandler handler = (proxyInstance, method, arguments) -> switch (method.getName()) {
            case "equals" -> proxyInstance == arguments[0];
            case "hashCode" -> System.identityHashCode(proxyInstance);
            case "toString" -> "fake " + type.getSimpleName();
            default -> call.invoke(method, arguments);
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    @FunctionalInterface
    private interface FakeCall {

        Object invoke(Method method, Object[] arguments);
    }

    private static final class ScheduledTask {

        private final Runnable runnable;
        private final boolean repeating;

        private boolean cancelled;

        private ScheduledTask(Runnable runnable, boolean repeating) {
            this.runnable = runnable;
            this.repeating = repeating;
        }
    }
}
