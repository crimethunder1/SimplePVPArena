package de.simplepvparena.arena;

import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaFightRegionTest {

    private static final String WORLD_NAME = "event";
    private static final String OTHER_WORLD_NAME = "lobby";
    private static final int FLOOR_Y = 64;
    private static final int WALL_TOP_Y = 66;
    private static final int HIGHEST_BLOCK_Y = WALL_TOP_Y + 20;

    private final Optional<ArenaShape> fightArea = Optional.of(new ArenaShape(WORLD_NAME, FLOOR_Y, FLOOR_Y,
            WALL_TOP_Y, new long[] {
                    HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(1, 0),
                    HorizontalCellCodec.encode(2, 0), HorizontalCellCodec.encode(0, 1)}));

    @Test
    void containsTheCellsFromTheFloorToTwentyBlocksAboveTheWallTop() {
        assertTrue(contains(WORLD_NAME, 0, FLOOR_Y, 0), "the floor");
        assertTrue(contains(WORLD_NAME, 2, WALL_TOP_Y, 0), "the top of the wall");
        assertTrue(contains(WORLD_NAME, 0, HIGHEST_BLOCK_Y, 1), "twenty blocks above the wall top");
        assertEquals(HIGHEST_BLOCK_Y, ArenaFightRegion.findHighestBlockY(fightArea.orElseThrow()));
    }

    @Test
    void leavesEverythingOutsideTheCellsOut() {
        assertFalse(contains(WORLD_NAME, 1, FLOOR_Y, 1), "a gap between the cells");
        assertFalse(contains(WORLD_NAME, 3, FLOOR_Y, 0), "next to the cells");
        assertFalse(contains(WORLD_NAME, -1, WALL_TOP_Y, 0), "on the other side");
    }

    @Test
    void leavesEverythingUnderTheFloorAndAboveTheSearchedHeightOut() {
        assertFalse(contains(WORLD_NAME, 0, FLOOR_Y - 1, 0), "under the floor");
        assertFalse(contains(WORLD_NAME, 0, HIGHEST_BLOCK_Y + 1, 0), "above the searched height");
    }

    @Test
    void leavesAnotherWorldOut() {
        assertFalse(contains(OTHER_WORLD_NAME, 0, FLOOR_Y, 0));
    }

    @Test
    void containsNothingWhileNoFightAreaIsCalculated() {
        assertFalse(ArenaFightRegion.contains(Optional.empty(), WORLD_NAME, 0, FLOOR_Y, 0));
    }

    @Test
    void decidesForALocationByItsBlock() {
        World world = SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> WORLD_NAME;
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });

        assertTrue(ArenaFightRegion.contains(fightArea, new Location(world, 0.5D, HIGHEST_BLOCK_Y + 0.9D, 0.5D)),
                "inside the highest searched block");
        assertFalse(ArenaFightRegion.contains(fightArea, new Location(world, 0.5D, FLOOR_Y - 0.1D, 0.5D)),
                "just under the floor");
        assertFalse(ArenaFightRegion.contains(fightArea, new Location(null, 0.5D, FLOOR_Y, 0.5D)),
                "without a world");
        assertFalse(ArenaFightRegion.contains(fightArea, null), "and without a location");
    }

    private boolean contains(String worldName, int blockX, int blockY, int blockZ) {
        return ArenaFightRegion.contains(fightArea, worldName, blockX, blockY, blockZ);
    }
}
