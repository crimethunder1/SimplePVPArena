package de.simplepvparena.arena;

import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlacedBlockTrackerTest {

    private final Map<String, BlockData> worldBlocks = new HashMap<>();
    private final List<String> logLines = new ArrayList<>();
    private final BlockData air = data("air");
    private final BlockData stone = data("stone");
    private final BlockData water = data("water");
    private final World world = SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
        case "getName" -> "arena";
        case "getBlockAt" -> block((Integer) arguments[0], (Integer) arguments[1], (Integer) arguments[2]);
        default -> throw new UnsupportedOperationException("World." + method.getName());
    });
    private final Server server = SharedBukkitServer.proxy(Server.class, (method, arguments) -> switch (method.getName()) {
        case "getWorld" -> "arena".equals(arguments[0]) ? world : null;
        default -> throw new UnsupportedOperationException("Server." + method.getName());
    });
    private final PlacedBlockTracker tracker = new PlacedBlockTracker(server, logLines::add);

    @Test
    void putsBackWhatWasThereBeforeTheFightersPlacedTheirBlocks() {
        worldBlocks.put("1/64/1", stone);
        worldBlocks.put("2/64/1", stone);

        tracker.recordReplacedStates(List.of(state(1, 64, 1, air), state(2, 64, 1, water)));

        assertEquals(2, tracker.trackedCount());
        assertEquals(2, tracker.restoreAll());
        assertEquals(air, worldBlocks.get("1/64/1"));
        assertEquals(water, worldBlocks.get("2/64/1"));
        assertEquals(List.of("Removed 2 blocks the fighters placed during the match"), logLines);
        assertEquals(0, tracker.trackedCount(), "the tracker starts empty for the next round");
    }

    @Test
    void keepsTheFirstStateWhenTheSamePlaceIsBuiltOnTwice() {
        worldBlocks.put("1/64/1", stone);

        tracker.recordReplacedStates(List.of(state(1, 64, 1, air)));
        tracker.recordReplacedStates(List.of(state(1, 64, 1, water)));
        tracker.restoreAll();

        assertEquals(air, worldBlocks.get("1/64/1"));
    }

    @Test
    void changesNothingAndSaysNothingWhenTheBlockIsAlreadyBack() {
        worldBlocks.put("1/64/1", air);

        tracker.recordReplacedStates(List.of(state(1, 64, 1, air)));

        assertEquals(0, tracker.restoreAll());
        assertEquals(List.of(), logLines);
    }

    private BlockState state(int x, int y, int z, BlockData before) {
        return SharedBukkitServer.proxy(BlockState.class, (method, arguments) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "getX" -> x;
            case "getY" -> y;
            case "getZ" -> z;
            case "getBlockData" -> before;
            default -> throw new UnsupportedOperationException("BlockState." + method.getName());
        });
    }

    private Block block(int x, int y, int z) {
        String key = x + "/" + y + "/" + z;
        return SharedBukkitServer.proxy(Block.class, (method, arguments) -> switch (method.getName()) {
            case "getBlockData" -> worldBlocks.getOrDefault(key, air);
            case "setBlockData" -> {
                worldBlocks.put(key, (BlockData) arguments[0]);
                yield null;
            }
            default -> throw new UnsupportedOperationException("Block." + method.getName());
        });
    }

    private static BlockData data(String name) {
        BlockData[] self = new BlockData[1];
        self[0] = SharedBukkitServer.proxy(BlockData.class, (method, arguments) -> switch (method.getName()) {
            case "matches" -> arguments[0] == self[0];
            case "clone" -> self[0];
            case "getAsString" -> name;
            default -> throw new UnsupportedOperationException("BlockData." + method.getName());
        });
        return self[0];
    }
}
