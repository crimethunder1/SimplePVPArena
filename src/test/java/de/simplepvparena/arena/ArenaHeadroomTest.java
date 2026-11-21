package de.simplepvparena.arena;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaHeadroomTest {

    private final ArenaShape fightArea = new ArenaShape("arena", 64, 63, 70,
            new long[] {HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(1, 0)});

    @Test
    void reachesFromAboveTheWallTopUpToTheHighestBlockTheFightersMayBuildAt() {
        ArenaShape headroom = ArenaHeadroom.volumeAbove(fightArea, 319).orElseThrow();

        assertEquals(71, headroom.getFloorY());
        assertEquals(70 + ArenaFightRegion.SEARCHED_BLOCKS_ABOVE_THE_WALL_TOP, headroom.getWallTopY());
        assertEquals(ArenaFightRegion.findHighestBlockY(fightArea), headroom.getWallTopY());
        assertTrue(headroom.coversTheSameArea(fightArea), "only the columns of the fight area are cleaned");
    }

    @Test
    void stopsAtTheTopOfTheWorld() {
        assertEquals(75, ArenaHeadroom.volumeAbove(fightArea, 75).orElseThrow().getWallTopY());
        assertEquals(Optional.empty(), ArenaHeadroom.volumeAbove(fightArea, 70));
    }

    @Test
    void cleansEverythingToAirWhenNothingWasRecorded() {
        assertFalse(ArenaHeadroom.ASSUMED_EMPTY.isRecorded());
        assertEquals(Material.AIR, ArenaHeadroom.ASSUMED_EMPTY.findMaterial(0, 75, 0));
        assertTrue(ArenaHeadroom.ASSUMED_EMPTY.findBlockData(0, 75, 0).isEmpty());
        assertTrue(ArenaHeadroom.ASSUMED_EMPTY.findBlockEntity(0, 75, 0).isEmpty());
    }

    @Test
    void cleaningPlansWithoutABaselineCleanTheHeadroomToAir() {
        ArenaCleaningPlan plan = ArenaCleaningPlan.of(fightArea, Optional.empty());

        assertEquals(ArenaHeadroom.ASSUMED_EMPTY, plan.getHeadroomTarget());
    }
}
