package de.simplepvparena.arena;

import de.simplepvparena.border.BlockedColumnLookup;
import de.simplepvparena.border.SpectatorBorderMovementRule;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ArenaShapeFloodFillTest {

    private static final String WORLD_NAME = "event";
    private static final int SEED_HEIGHT = 64;

    private static final int SPECTATOR_WALK_LIMIT = 20_000;

    @Test
    void fillsASquareRoomUpToItsWall() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT);

        ArenaShape shape = calculate(wall, 2, 2, 1000).getShape().orElseThrow();

        assertEquals(9, shape.getCellCount());
        assertTrue(shape.containsCell(1, 1));
        assertTrue(shape.containsCell(3, 3));
        assertFalse(shape.containsCell(0, 2), "the wall itself does not belong to the arena");
        assertFalse(shape.containsCell(5, 2), "the fill never leaves the wall");
        assertEquals(WORLD_NAME, shape.getWorldName());
        assertEquals(SEED_HEIGHT, shape.getReferenceY());
    }

    @Test
    void fillsAShapeThatIsNotARectangle() {
        Set<Long> roomCells = BorderWall.cells(
                0, 0, 1, 0, 2, 0,
                0, 1,
                0, 2, 1, 2, 2, 2, 3, 2, 4, 2,
                4, 3,
                4, 4);
        BorderWall wall = BorderWall.around(roomCells, SEED_HEIGHT);

        ArenaShape shape = calculate(wall, 0, 0, 1000).getShape().orElseThrow();

        assertEquals(roomCells.size(), shape.getCellCount());
        assertEquals(roomCells, new LinkedHashSet<>(Arrays.stream(shape.getCells()).boxed().toList()));
        assertFalse(shape.containsCell(3, 0), "the notch of the shape stays outside");
        assertFalse(shape.containsCell(1, 1), "a cell walled off inside the shape stays outside");
    }

    @Test
    void neverSpreadsThroughTheDiagonalGapAtACornerOfTheWall() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT).removeColumn(4, 4);
        BlockedColumnLookup blockedColumns = wall.asBlockedColumns();

        ArenaShapeCalculation calculation = calculate(wall, 2, 2, 1000);

        assertFalse(blockedColumns.isBlockedColumn(4, 4), "the corner column itself is free");
        assertTrue(wall.hasTwoColumnsTouchingOnlyDiagonally(),
                "the two columns beside the corner touch only diagonally");
        assertTrue(SpectatorBorderMovementRule.isMovementBlocked(blockedColumns, 3, 3, 4, 4),
                "a spectator cannot squeeze through that corner");
        ArenaShape shape = calculation.getShape().orElseThrow();
        assertEquals(9, shape.getCellCount(), "the fill stays in the room the wall encloses");
        assertFalse(shape.containsCell(4, 4), "the free corner column is only reachable diagonally");
        assertFalse(shape.containsCell(5, 5), "and the fill never squeezes past it into the open");
    }

    @Test
    void fillsAnOvalRoomWhoseWallOnlyTouchesDiagonallyInPlaces() {
        BorderWall wall = ovalWall(12, 7, SEED_HEIGHT);

        ArenaShapeCalculation calculation = calculate(wall, 0, 0, 1000);

        assertTrue(wall.hasTwoColumnsTouchingOnlyDiagonally(),
                "the slanted parts of the oval are staircases of diagonally touching columns");
        assertTrue(calculation.isSuccessful(),
                "the oval is closed and is calculated instead of being reported as a leak: "
                        + calculation.getRejectionMessage().orElse(""));
        ArenaShape shape = calculation.getShape().orElseThrow();
        assertEquals(columnsASpectatorCanReach(wall, 0, 0), cellsOf(shape),
                "the fill covers exactly the columns a spectator can walk to");
        assertTrue(shape.getCellCount() > 200, "a filled oval of 12 by 7 columns: " + shape.getCellCount());
        assertTrue(shape.containsCell(0, 0));
        assertTrue(shape.containsCell(10, 0), "up to the flat parts of the wall");
        assertTrue(shape.containsCell(0, 5));
        assertFalse(shape.containsCell(20, 20), "and never outside of it");
    }

    @Test
    void stopsAtTheCellLimitWhenTheWallHasAGap() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT).removeColumn(4, 2);

        ArenaShapeCalculation calculation = calculate(wall, 2, 2, 200);

        assertEquals(ArenaShapeCalculation.Status.CELL_LIMIT_REACHED, calculation.getStatus());
        assertTrue(calculation.getShape().isEmpty());
        assertTrue(calculation.getRejectionMessage().orElseThrow().contains("gap"));
    }

    @Test
    void stopsAtTheCellLimitEvenWhenTheWallIsClosed() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 10, 10, SEED_HEIGHT);

        ArenaShapeCalculation calculation = calculate(wall, 5, 5, 20);

        assertEquals(ArenaShapeCalculation.Status.CELL_LIMIT_REACHED, calculation.getStatus());
        assertTrue(calculation.getRejectionMessage().orElseThrow().contains("20 cells"));
    }

    @Test
    void tellsTheAdminAboutBothReasonsAndTheSettingToRaise() {
        BorderWall wallWithAGap = new BorderWall()
                .addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT).removeColumn(4, 2);
        BorderWall closedWall = new BorderWall().addRectangleOutline(0, 0, 10, 10, SEED_HEIGHT);

        String gapRejection = calculate(wallWithAGap, 2, 2, 20).getRejectionMessage().orElseThrow();
        String limitRejection = calculate(closedWall, 5, 5, 20).getRejectionMessage().orElseThrow();

        assertEquals(reasonOf(gapRejection), reasonOf(limitRejection),
                "the calculation cannot tell the two cases apart");
        assertTrue(gapRejection.contains("gap"), "the wall may have a gap");
        assertTrue(gapRejection.contains("larger than that limit"), "or the area may just be too large");
        assertTrue(gapRejection.contains(ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH),
                "the setting to raise is named");
        assertTrue(gapRejection.contains("20 cells"), "the configured value that stopped the fill is named");
    }

    @Test
    void reportsWhereTheFillEscapedTo() {
        BorderWall wall = BorderWall.around(roomWithACorridorTowardsEast(), SEED_HEIGHT);

        ArenaShapeCalculation calculation = calculate(wall, 2, 2, 100);

        ArenaFillDiagnostics diagnostics = calculation.getDiagnostics().orElseThrow();
        assertEquals(100, diagnostics.getCellCount());
        assertEquals("east", diagnostics.getEscapeDirectionName());
        assertEquals(0, diagnostics.getMinimumX());
        assertEquals(79, diagnostics.getMaximumX(), "the fill ran 75 columns down the corridor");
        assertEquals(0, diagnostics.getMinimumZ());
        assertEquals(4, diagnostics.getMaximumZ());
        String rejection = calculation.getRejectionMessage().orElseThrow();
        assertTrue(rejection.contains("x 0..79, z 0..4"), "the box is named: " + rejection);
        assertTrue(rejection.contains("Walk along your wall towards east"),
                "the admin is sent in the direction the fill went: " + rejection);
    }

    @Test
    void reportsTheLastColumnsTheFillReached() {
        BorderWall wall = BorderWall.around(roomWithACorridorTowardsEast(), SEED_HEIGHT);

        ArenaFillDiagnostics diagnostics = calculate(wall, 2, 2, 100).getDiagnostics().orElseThrow();

        long[] frontCells = diagnostics.getFrontCells();
        assertEquals(ArenaFillDiagnostics.FRONT_SAMPLE_SIZE, frontCells.length);
        for (long frontCell : frontCells) {
            assertEquals(2, HorizontalCellCodec.decodeZ(frontCell), "every front column lies in the corridor");
        }
        assertEquals(HorizontalCellCodec.encode(79, 2), frontCells[frontCells.length - 1]);
    }

    @Test
    void reportsAFillThatWasStoppedBeforeItSpread() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT).removeColumn(4, 2);

        ArenaShapeCalculation calculation = calculate(wall, 2, 2, 1);

        assertFalse(calculation.getDiagnostics().orElseThrow().hasSpread());
        String rejection = calculation.getRejectionMessage().orElseThrow();
        assertTrue(rejection.contains("never left the column you are standing in"), rejection);
        assertTrue(rejection.contains("Walk along your wall and look"), rejection);
    }

    @Test
    void reportsBorderBlocksThatSitTooHighToCloseTheWall() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT)
                .raiseColumn(4, 2, SEED_HEIGHT + 6);

        ArenaShapeCalculation calculation = calculate(wall, 2, 2, 200);

        ArenaFillDiagnostics diagnostics = calculation.getDiagnostics().orElseThrow();
        assertTrue(diagnostics.hasHeightMismatch());
        assertEquals(1, diagnostics.getHeightMismatchColumnCount());
        ArenaHeightMismatchColumn column = diagnostics.getNearestHeightMismatchColumn().orElseThrow();
        assertEquals(4, column.blockX());
        assertEquals(2, column.blockZ());
        assertEquals(SEED_HEIGHT + 6, column.lowestBorderY());
        assertEquals(6, column.heightAboveSeed());
    }

    @Test
    void tellsTheAdminWhichWallPartStandsTooHighAndHowToFixIt() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT)
                .raiseColumn(4, 2, SEED_HEIGHT + 6);

        String rejection = calculate(wall, 2, 2, 200).getRejectionMessage().orElseThrow();

        assertTrue(rejection.contains("One of the columns the fill walked through does hold a border block"),
                rejection);
        assertTrue(rejection.contains("x/z 4/2 (lowest border block at y " + (SEED_HEIGHT + 6) + ", 6 above you)"),
                "the column and its height are named: " + rejection);
        assertTrue(rejection.contains("Build the wall down to your own height there"), rejection);
        assertTrue(rejection.contains("use " + Arena.CALCULATE_ACTION + " again"),
                "the second way out is named: " + rejection);
        assertTrue(rejection.indexOf("border block") < rejection.indexOf("The fill spread furthest"),
                "the wall part comes before the box and the direction: " + rejection);
        assertTrue(rejection.contains("larger than that limit"), "the two old reasons stay: " + rejection);
    }

    @Test
    void countsEveryColumnThatStandsTooHighAndListsOnlyTheNearestOnes() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 30, 30, SEED_HEIGHT);
        for (int blockZ = 0; blockZ <= 30; blockZ++) {
            wall.raiseColumn(30, blockZ, SEED_HEIGHT + 3);
        }

        ArenaShapeCalculation calculation = calculate(wall, 15, 15, 5000);

        ArenaFillDiagnostics diagnostics = calculation.getDiagnostics().orElseThrow();
        assertEquals(31, diagnostics.getHeightMismatchColumnCount(), "the whole eastern side is too high");
        assertEquals(ArenaFillDiagnostics.HEIGHT_MISMATCH_SAMPLE_SIZE, diagnostics.getHeightMismatchColumns().size(),
                "and only a sample of them is kept while the fill runs");
        ArenaHeightMismatchColumn nearestColumn = diagnostics.getNearestHeightMismatchColumn().orElseThrow();
        assertEquals(30, nearestColumn.blockX());
        assertEquals(15, nearestColumn.blockZ(), "the column straight east of the seed is the closest one");
        String rejection = calculation.getRejectionMessage().orElseThrow();
        assertTrue(rejection.contains("31 of the columns the fill walked through do hold a border block"),
                rejection);
        assertTrue(rejection.contains("The " + ArenaFillDiagnostics.REPORTED_HEIGHT_MISMATCH_COUNT
                + " nearest of them are at x/z 30/15"), rejection);
    }

    @Test
    void namesTheLowestBorderBlockOfAColumnThatHoldsSeveral() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT)
                .raiseColumn(4, 2, SEED_HEIGHT + 6)
                .add(4, 2, SEED_HEIGHT + 2)
                .add(4, 2, SEED_HEIGHT + 9);

        ArenaFillDiagnostics diagnostics = calculate(wall, 2, 2, 200).getDiagnostics().orElseThrow();

        ArenaHeightMismatchColumn column = diagnostics.getNearestHeightMismatchColumn().orElseThrow();
        assertEquals(SEED_HEIGHT + 2, column.lowestBorderY());
        assertEquals(2, column.heightAboveSeed());
    }

    @Test
    void reportsNoMismatchForAColumnThatBarsTheFillAnyway() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT)
                .removeColumn(4, 2)
                .add(4, 2, SEED_HEIGHT)
                .add(4, 2, SEED_HEIGHT + 6);

        ArenaShapeCalculation calculation = calculate(wall, 2, 2, 200);

        assertTrue(calculation.isSuccessful(), "the wall is closed");
        assertEquals(9, calculation.getShape().orElseThrow().getCellCount());
    }

    @Test
    void reportsNoHeightMismatchForAWallWithARealGap() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT).removeColumn(4, 2);

        ArenaShapeCalculation calculation = calculate(wall, 2, 2, 200);

        ArenaFillDiagnostics diagnostics = calculation.getDiagnostics().orElseThrow();
        assertFalse(diagnostics.hasHeightMismatch());
        assertEquals(0, diagnostics.getHeightMismatchColumnCount());
        assertTrue(diagnostics.getNearestHeightMismatchColumn().isEmpty());
        String rejection = calculation.getRejectionMessage().orElseThrow();
        assertFalse(rejection.contains("hold a border block"), "nothing is claimed about a wall: " + rejection);
        assertTrue(rejection.startsWith("The fight area could not be calculated, the flood fill was stopped at "
                + "200 cells, the limit of " + ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH
                + ". Either your border wall has a gap"), rejection);
    }

    @Test
    void walksThroughAColumnThatStandsTooHighExactlyAsItDidBefore() {
        BorderWall wallBuiltAtTheSeedHeight = new BorderWall().addRectangleOutline(0, 0, 8, 8, SEED_HEIGHT);
        BorderWall wallWithAnInnerRingAboveTheSeed = new BorderWall()
                .addRectangleOutline(0, 0, 8, 8, SEED_HEIGHT)
                .addRectangleOutline(2, 2, 6, 6, SEED_HEIGHT + 4);

        ArenaShape plainShape = calculate(wallBuiltAtTheSeedHeight, 4, 4, 1000).getShape().orElseThrow();
        ArenaShape shapeWithTheHighRing = calculate(wallWithAnInnerRingAboveTheSeed, 4, 4, 1000)
                .getShape().orElseThrow();

        assertEquals(49, plainShape.getCellCount());
        assertEquals(cellsOf(plainShape), cellsOf(shapeWithTheHighRing),
                "the ring above the seed changes nothing about which columns are the arena");
    }

    @Test
    void reportsNoDiagnosticsForACalculationThatWasNotStoppedByTheLimit() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT);

        assertTrue(calculate(wall, 2, 2, 1000).getDiagnostics().isEmpty(), "a finished fill has its shape");
        assertTrue(calculate(wall, 0, 0, 1000).getDiagnostics().isEmpty(), "a fill that never started has nothing");
    }

    private static Set<Long> roomWithACorridorTowardsEast() {
        Set<Long> roomCells = new LinkedHashSet<>();
        for (int cellX = 0; cellX <= 4; cellX++) {
            for (int cellZ = 0; cellZ <= 4; cellZ++) {
                roomCells.add(HorizontalCellCodec.encode(cellX, cellZ));
            }
        }
        for (int cellX = 5; cellX <= 300; cellX++) {
            roomCells.add(HorizontalCellCodec.encode(cellX, 2));
        }
        return roomCells;
    }

    private static String reasonOf(String rejectionMessage) {
        return rejectionMessage.substring(0, rejectionMessage.indexOf("The fill spread furthest"));
    }

    @Test
    void acceptsAnAreaThatUsesTheCellLimitExactly() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT);

        ArenaShapeCalculation calculation = calculate(wall, 2, 2, 9);

        assertTrue(calculation.isSuccessful());
        assertEquals(9, calculation.getShape().orElseThrow().getCellCount());
    }

    @Test
    void rejectsASeedThatStandsInABorderColumn() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT);

        ArenaShapeCalculation calculation = calculate(wall, 0, 0, 1000);

        assertEquals(ArenaShapeCalculation.Status.SEED_INSIDE_BORDER, calculation.getStatus());
        assertTrue(calculation.getShape().isEmpty());
    }

    @Test
    void ignoresBorderBlocksAboveTheHeightOfTheSeed() {
        BorderWall wall = new BorderWall()
                .addRectangleOutline(1, 1, 3, 3, SEED_HEIGHT + 10)
                .addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT);

        ArenaShape shape = calculate(wall, 2, 2, 1000).getShape().orElseThrow();

        assertEquals(9, shape.getCellCount(), "the wall above the seed does not stop the fill");
        assertTrue(shape.containsCell(1, 1));
    }

    @Test
    void reportsTheHighestWallBlockAsTheTopOfTheArena() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT);
        wall.add(2, 0, SEED_HEIGHT + 5);

        ArenaShape shape = calculate(wall, 2, 2, 1000).getShape().orElseThrow();

        assertEquals(SEED_HEIGHT + 5, shape.getWallTopY());
        assertEquals(9L * (5 + 1), shape.getBlockCount());
    }

    @Test
    void anchorsTheCleanedVolumeAtTheWallNoMatterWhichHeightTheFillRanAt() {
        BorderWall wall = new BorderWall()
                .addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT)
                .addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT + 1)
                .addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT + 2);

        ArenaShape fromTheFloor = calculateFrom(wall, 2, SEED_HEIGHT, 2).getShape().orElseThrow();
        ArenaShape fromAboveTheWall = calculateFrom(wall, 2, SEED_HEIGHT + 20, 2).getShape().orElseThrow();

        assertEquals(SEED_HEIGHT, fromAboveTheWall.getFloorY(), "the bottom of the wall is the floor");
        assertEquals(SEED_HEIGHT + 2, fromAboveTheWall.getWallTopY(), "the top of the wall is the top");
        assertEquals(fromTheFloor.getFloorY(), fromAboveTheWall.getFloorY());
        assertEquals(fromTheFloor.getWallTopY(), fromAboveTheWall.getWallTopY());
        assertEquals(9L * 3, fromAboveTheWall.getBlockCount());
        assertEquals(SEED_HEIGHT + 20, fromAboveTheWall.getReferenceY(),
                "the height the fill ran at is kept, the wall recognition still runs on it");
    }

    @Test
    void takesTheVolumeOfAWallThatStandsBelowTheHeightOfTheFill() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT - 2);

        ArenaShape shape = calculateFrom(wall, 2, SEED_HEIGHT, 2).getShape().orElseThrow();

        assertEquals(SEED_HEIGHT - 2, shape.getFloorY());
        assertEquals(SEED_HEIGHT - 2, shape.getWallTopY());
        assertEquals(9L, shape.getBlockCount());
        assertTrue(shape.getFloorY() <= shape.getReferenceY(),
                "the floor never lies above the height the calculation ran at");
    }

    @Test
    void takesTheLowestBlockOfTheWholeWallAsTheFloor() {
        BorderWall wall = new BorderWall().addRectangleOutline(0, 0, 4, 4, SEED_HEIGHT);
        wall.add(2, 0, SEED_HEIGHT - 4);

        ArenaShape shape = calculateFrom(wall, 2, SEED_HEIGHT + 9, 2).getShape().orElseThrow();

        assertEquals(SEED_HEIGHT - 4, shape.getFloorY());
        assertEquals(SEED_HEIGHT, shape.getWallTopY());
    }

    @Test
    void fillsASingleCellRoom() {
        ArenaShape shape = calculate(BorderWall.around(BorderWall.cells(7, -3), SEED_HEIGHT), 7, -3, 1000)
                .getShape().orElseThrow();

        assertEquals(1, shape.getCellCount());
        assertTrue(shape.containsCell(7, -3));
    }

    private ArenaShapeCalculation calculateFrom(BorderWall wall, int seedX, int seedY, int seedZ) {
        return ArenaShapeFloodFill.calculate(WORLD_NAME, seedX, seedY, seedZ, 1000, wall);
    }

    private ArenaShapeCalculation calculate(BorderWall wall, int seedX, int seedZ, int maximumCellCount) {
        return ArenaShapeFloodFill.calculate(WORLD_NAME, seedX, SEED_HEIGHT, seedZ, maximumCellCount, wall);
    }

    private static BorderWall ovalWall(int radiusX, int radiusZ, int borderHeight) {
        BorderWall wall = new BorderWall();
        for (int blockX = -radiusX; blockX <= radiusX; blockX++) {
            int blockZ = (int) Math.round(radiusZ * Math.sqrt(1 - (double) (blockX * blockX) / (radiusX * radiusX)));
            wall.add(blockX, blockZ, borderHeight);
            wall.add(blockX, -blockZ, borderHeight);
        }
        for (int blockZ = -radiusZ; blockZ <= radiusZ; blockZ++) {
            int blockX = (int) Math.round(radiusX * Math.sqrt(1 - (double) (blockZ * blockZ) / (radiusZ * radiusZ)));
            wall.add(blockX, blockZ, borderHeight);
            wall.add(-blockX, blockZ, borderHeight);
        }
        return wall;
    }

    private static Set<Long> columnsASpectatorCanReach(BorderWall wall, int seedX, int seedZ) {
        BlockedColumnLookup blockedColumns = wall.asBlockedColumns();
        int[] neighbourOffsetsX = {1, -1, 0, 0, 1, 1, -1, -1};
        int[] neighbourOffsetsZ = {0, 0, 1, -1, 1, -1, 1, -1};
        Set<Long> reachedColumns = new LinkedHashSet<>();
        Deque<Long> pendingColumns = new ArrayDeque<>();
        reachedColumns.add(HorizontalCellCodec.encode(seedX, seedZ));
        pendingColumns.add(HorizontalCellCodec.encode(seedX, seedZ));
        while (!pendingColumns.isEmpty()) {
            long column = pendingColumns.poll();
            int columnX = HorizontalCellCodec.decodeX(column);
            int columnZ = HorizontalCellCodec.decodeZ(column);
            for (int offsetIndex = 0; offsetIndex < neighbourOffsetsX.length; offsetIndex++) {
                int neighbourX = columnX + neighbourOffsetsX[offsetIndex];
                int neighbourZ = columnZ + neighbourOffsetsZ[offsetIndex];
                long neighbour = HorizontalCellCodec.encode(neighbourX, neighbourZ);
                if (reachedColumns.contains(neighbour)
                        || SpectatorBorderMovementRule.isMovementBlocked(blockedColumns, columnX, columnZ,
                        neighbourX, neighbourZ)) {
                    continue;
                }
                if (reachedColumns.size() >= SPECTATOR_WALK_LIMIT) {
                    fail("the spectator walked out of the wall, it is not closed for a player either");
                }
                reachedColumns.add(neighbour);
                pendingColumns.add(neighbour);
            }
        }
        return reachedColumns;
    }

    private static Set<Long> cellsOf(ArenaShape shape) {
        return new LinkedHashSet<>(Arrays.stream(shape.getCells()).boxed().toList());
    }
}
