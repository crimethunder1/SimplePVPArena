package de.simplepvparena.arena;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaHeadroomTest {

    private final ArenaShape fightArea = new ArenaShape("arena", 64, 63, 70,
            new long[] {HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(1, 0)});

    @Test
    void reachesFromAboveTheWallTopUpToTheTopOfTheWorld() {
        ArenaShape headroom = ArenaHeadroom.volumeAbove(fightArea, 319).orElseThrow();

        assertEquals(71, headroom.getFloorY());
        assertEquals(319, headroom.getWallTopY());
        assertTrue(headroom.coversTheSameArea(fightArea), "only the columns of the fight area are cleaned");
    }

    @Test
    void isEmptyWhenTheWorldEndsAtTheWallTop() {
        assertEquals(75, ArenaHeadroom.volumeAbove(fightArea, 75).orElseThrow().getWallTopY());
        assertEquals(Optional.empty(), ArenaHeadroom.volumeAbove(fightArea, 70));
    }

    @Test
    void cleansEverythingToAirWhenNothingWasRecorded() {
        assertFalse(ArenaHeadroom.ASSUMED_EMPTY.isRecorded());
        assertEquals(Material.AIR, ArenaHeadroom.ASSUMED_EMPTY.findMaterial(0, 75, 0));
        assertTrue(ArenaHeadroom.ASSUMED_EMPTY.findBlockData(0, 75, 0).isEmpty());
        assertTrue(ArenaHeadroom.ASSUMED_EMPTY.findBlockEntity(0, 75, 0).isEmpty());
        assertEquals(12, ArenaHeadroom.ASSUMED_EMPTY.findHighestRecordedY(12));
    }

    @Test
    void cleaningPlansWithoutABaselineCleanTheHeadroomToAir() {
        ArenaCleaningPlan plan = ArenaCleaningPlan.of(fightArea, Optional.empty());

        assertEquals(ArenaHeadroom.ASSUMED_EMPTY, plan.getHeadroomTarget());
    }

    @Test
    void walksAColumnOnlyUpToItsHighestBlockAndSkipsEmptyColumns() {
        World world = worldWithHighestBlocks(Map.of(0L, 250, 1L, 40));
        ArenaHeadroomWalk walk = new ArenaHeadroomWalk(fightArea, 319, Integer.MIN_VALUE);

        List<Long> positions = new ArrayList<>();
        while (walk.hasRemainingBlocks()) {
            for (long position : walk.nextBatch(world, 64)) {
                positions.add(position);
            }
        }

        assertEquals(250 - 71 + 1, positions.size(), "the tower column from above the wall up to its top");
        assertEquals(BlockPositionCodec.encode(0, 71, 0), positions.getFirst());
        assertEquals(BlockPositionCodec.encode(0, 250, 0), positions.getLast());
    }

    @Test
    void alsoWalksUpToTheHighestRecordedBlockSoABrokenRoofIsPutBack() {
        World world = worldWithHighestBlocks(Map.of(0L, 40, 1L, 40));
        ArenaHeadroomWalk walk = new ArenaHeadroomWalk(fightArea, 319, 75);

        long walked = 0L;
        while (walk.hasRemainingBlocks()) {
            walked += walk.nextBatch(world, 3).length;
        }

        assertEquals(2L * (75 - 71 + 1), walked);
    }

    private static World worldWithHighestBlocks(Map<Long, Integer> highestYByX) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] {World.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getHighestBlockYAt") && arguments.length == 3
                            && arguments[2] == HeightMap.WORLD_SURFACE) {
                        return highestYByX.get((long) (Integer) arguments[0]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
