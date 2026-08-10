package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaGapTargetTest {

    @Test
    void targetIsTheFrontColumnClosestToTheSeedAtTheCalculationHeight() {
        ArenaFillDiagnostics diagnostics = ArenaFillDiagnostics.fromFillOrder(0, 0,
                BorderWall.cells(0, 0, 200, 0, 30, 40, 400, 400));

        ArenaGapTarget target = ArenaGapTarget.fromDiagnostics("arena_world", 64, diagnostics);

        assertEquals("arena_world", target.worldName());
        assertEquals(30, target.blockX());
        assertEquals(64, target.blockY());
        assertEquals(40, target.blockZ());
    }

    @Test
    void targetKeepsTheSeedHeightEvenFarAwayFromTheSeed() {
        ArenaFillDiagnostics diagnostics = ArenaFillDiagnostics.fromFillOrder(-500, 900,
                BorderWall.cells(-500, 900, -1200, 1700));

        ArenaGapTarget target = ArenaGapTarget.fromDiagnostics("world", -12, diagnostics);

        assertEquals(-1200, target.blockX());
        assertEquals(-12, target.blockY());
        assertEquals(1700, target.blockZ());
    }

    @Test
    void targetIsPickedOutOfTheWholeFront() {
        Set<Long> filledCells = new LinkedHashSet<>();
        for (int cellZ = 0; cellZ < 30; cellZ++) {
            filledCells.add(HorizontalCellCodec.encode(8, cellZ));
        }
        ArenaFillDiagnostics diagnostics = ArenaFillDiagnostics.fromFillOrder(8, 0, filledCells);

        ArenaGapTarget target = ArenaGapTarget.fromDiagnostics("world", 70, diagnostics);

        assertEquals(8, target.blockX());
        assertEquals(30 - ArenaFillDiagnostics.FRONT_SAMPLE_SIZE, target.blockZ());
    }

    @Test
    void descriptionNamesTheCoordinatesAndTheWorld() {
        ArenaGapTarget target = new ArenaGapTarget("arena_world", 12, 64, -30);

        assertEquals("x/y/z 12/64/-30 in arena_world", target.describe());
    }
}
