package de.simplepvparena.arena;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jspecify.annotations.NullMarked;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@NullMarked
public final class PlacedBlockTracker {

    private final Server server;
    private final Consumer<String> infoLogger;
    private final Map<Position, BlockData> originalStates = new LinkedHashMap<>();

    public PlacedBlockTracker(Server server, Consumer<String> infoLogger) {
        this.server = Objects.requireNonNull(server, "server");
        this.infoLogger = Objects.requireNonNull(infoLogger, "infoLogger");
    }

    public void recordReplacedStates(List<BlockState> replacedStates) {
        for (BlockState state : replacedStates) {
            record(state.getWorld().getName(), state.getX(), state.getY(), state.getZ(), state.getBlockData());
        }
    }

    public void recordBlock(Block block) {
        record(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getBlockData());
    }

    public int trackedCount() {
        return originalStates.size();
    }

    public int restoreAll() {
        if (originalStates.isEmpty()) {
            return 0;
        }
        int restored = 0;
        for (Map.Entry<Position, BlockData> entry : originalStates.entrySet()) {
            Position position = entry.getKey();
            World world = server.getWorld(position.worldName());
            if (world == null) {
                continue;
            }
            Block block = world.getBlockAt(position.x(), position.y(), position.z());
            if (!block.getBlockData().matches(entry.getValue())) {
                block.setBlockData(entry.getValue(), false);
                restored++;
            }
        }
        originalStates.clear();
        if (restored > 0) {
            infoLogger.accept("Removed " + restored + " blocks the fighters placed during the match");
        }
        return restored;
    }

    private void record(String worldName, int x, int y, int z, BlockData original) {
        originalStates.putIfAbsent(new Position(worldName, x, y, z), original.clone());
    }

    private record Position(String worldName, int x, int y, int z) {
    }
}
