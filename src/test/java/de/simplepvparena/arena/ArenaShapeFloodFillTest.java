package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaShapeFloodFillTest {

    private static final String WORLD_NAME = "event";
    private static final int SEED_HEIGHT = 64;

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
    void fillsASingleCellRoom() {
        ArenaShape shape = calculate(BorderWall.around(BorderWall.cells(7, -3), SEED_HEIGHT), 7, -3, 1000)
                .getShape().orElseThrow();

        assertEquals(1, shape.getCellCount());
        assertTrue(shape.containsCell(7, -3));
    }

    private ArenaShapeCalculation calculate(BorderWall wall, int seedX, int seedZ, int maximumCellCount) {
        return ArenaShapeFloodFill.calculate(WORLD_NAME, seedX, SEED_HEIGHT, seedZ, maximumCellCount, wall);
    }
}
