package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaOutlineTest {

    private static final String WORLD_NAME = "event";
    private static final double TOLERANCE = 1.0E-9D;

    @Test
    void holdsTheRingOfASquareAndNothingInsideOfIt() {
        ArenaOutline outline = ArenaOutline.of(square(0, 0, 5));

        assertEquals(5 * 5 - 3 * 3, outline.getCellCount(), "the ring of a five by five square");
        assertTrue(outline.containsCell(0, 0), "a corner belongs to the contour");
        assertTrue(outline.containsCell(2, 0), "an edge belongs to the contour");
        assertTrue(outline.containsCell(4, 4));
        assertFalse(outline.containsCell(2, 2), "the middle is enclosed by the shape itself");
        assertFalse(outline.containsCell(5, 0), "a column outside of the shape is never contour");
    }

    @Test
    void holdsEveryCellOfAShapeThatHasNoInside() {
        assertEquals(1, ArenaOutline.of(shape(7, 7)).getCellCount());
        assertEquals(4, ArenaOutline.of(shape(0, 0, 1, 0, 2, 0, 3, 0)).getCellCount());
    }

    @Test
    void measuresTheDistanceToTheNearestContourCell() {
        ArenaOutline outline = ArenaOutline.of(square(0, 0, 5));

        assertEquals(0.0D, outline.distanceTo(0, 0), TOLERANCE, "a contour cell itself");
        assertEquals(2.0D, outline.distanceTo(2, 2), TOLERANCE,
                "the middle of the square is two columns from its ring");
        assertEquals(1.0D, outline.distanceTo(-1, 0), TOLERANCE, "straight next to the ring");
        assertEquals(Math.sqrt(2.0D), outline.distanceTo(-1, -1), TOLERANCE, "diagonally next to a corner");
        assertEquals(10.0D, outline.distanceTo(14, 2), TOLERANCE, "straight away from the edge");
        assertEquals(Math.sqrt(100.0D + 100.0D), outline.distanceTo(14, -10), TOLERANCE,
                "the distance is measured over both axes, not as their sum");
    }

    @Test
    void measuresAgainstTheShapeAndNotAgainstItsBoundingBox() {
        ArenaOutline outline = ArenaOutline.of(cross());

        assertEquals(0.0D, outline.distanceTo(0, 4), TOLERANCE, "the tip of an arm is contour");
        assertFalse(outline.containsCell(4, 4), "the middle of the cross is enclosed by its four arms");
        assertEquals(4.0D, outline.distanceTo(0, 0), TOLERANCE,
                "the corner of the bounding box lies four columns from the nearest arm");
        assertFalse(outline.isWithin(0, 0, 3), "and is outside of a zone the bounding box would put it into");
        assertTrue(outline.isWithin(0, 0, 4));
    }

    @Test
    void answersTheSameForNegativeCoordinates() {
        ArenaOutline outline = ArenaOutline.of(square(-40, -70, 5));

        assertEquals(0.0D, outline.distanceTo(-40, -70), TOLERANCE);
        assertEquals(1.0D, outline.distanceTo(-41, -70), TOLERANCE);
        assertEquals(2.0D, outline.distanceTo(-34, -68), TOLERANCE, "two columns east of the far edge");
        assertEquals(Math.sqrt(9.0D + 16.0D), outline.distanceTo(-43, -74), TOLERANCE);
    }

    @Test
    void answersTheSameForAShapeAroundTheOrigin() {
        ArenaOutline outline = ArenaOutline.of(square(-2, -2, 5));

        assertEquals(0.0D, outline.distanceTo(-2, 2), TOLERANCE);
        assertEquals(2.0D, outline.distanceTo(0, 0), TOLERANCE);
        assertEquals(1.0D, outline.distanceTo(3, 0), TOLERANCE);
        assertEquals(8.0D, outline.distanceTo(0, -10), TOLERANCE);
    }

    @Test
    void holdsTheInnerRingOfAShapeWithAHole() {
        List<Integer> coordinates = new ArrayList<>();
        for (int columnX = 0; columnX < 7; columnX++) {
            for (int columnZ = 0; columnZ < 7; columnZ++) {
                if (columnX != 3 || columnZ != 3) {
                    coordinates.add(columnX);
                    coordinates.add(columnZ);
                }
            }
        }
        ArenaOutline outline = ArenaOutline.of(shape(toArray(coordinates)));

        assertTrue(outline.containsCell(3, 2), "the cell above the hole is contour");
        assertTrue(outline.containsCell(2, 3));
        assertFalse(outline.containsCell(2, 2), "a cell that only touches the hole diagonally is enclosed");
        assertEquals(1.0D, outline.distanceTo(3, 3), TOLERANCE,
                "the hole is one column away from the ring around it");
    }

    @Test
    void answersTheRadiusInclusively() {
        ArenaOutline outline = ArenaOutline.of(square(0, 0, 5));

        assertTrue(outline.isWithin(68, 2, 64), "exactly 64 columns from the edge is still inside the zone");
        assertTrue(outline.isWithin(67, 2, 64), "one column closer");
        assertFalse(outline.isWithin(69, 2, 64), "one column further is outside of it");
        assertEquals(64.0D, outline.distanceTo(68, 2), TOLERANCE);
    }

    @Test
    void answersTheRadiusOverBothAxes() {
        ArenaOutline outline = ArenaOutline.of(square(0, 0, 5));

        assertTrue(outline.isWithin(-45, -45, 64), "45 by 45 columns off the corner is about 63.6 blocks");
        assertFalse(outline.isWithin(-46, -46, 64), "46 by 46 columns off the corner is about 65.1 blocks");
        assertFalse(outline.isWithin(-45, -45, 63));
    }

    @Test
    void refusesANegativeRadius() {
        ArenaOutline outline = ArenaOutline.of(square(0, 0, 5));

        assertFalse(outline.isWithin(0, 0, -1), "not even for a column of the contour itself");
        assertTrue(outline.isWithin(0, 0, 0), "a radius of zero still holds the contour itself");
    }

    @Test
    void answersAColumnFarAwayFromTheArena() {
        ArenaOutline outline = ArenaOutline.of(square(0, 0, 5));

        assertEquals(1000.0D, outline.distanceTo(1004, 2), TOLERANCE);
        assertFalse(outline.isWithin(1004, 2, 64));
        assertFalse(outline.isWithin(2, -100000, 64));
    }

    @Test
    void carriesAnEquallyWideRingAroundAnOvalShape() {
        List<Integer> coordinates = new ArrayList<>();
        for (int columnX = -12; columnX <= 12; columnX++) {
            for (int columnZ = -8; columnZ <= 8; columnZ++) {
                if (isInsideOval(columnX, columnZ)) {
                    coordinates.add(columnX);
                    coordinates.add(columnZ);
                }
            }
        }
        ArenaOutline outline = ArenaOutline.of(shape(toArray(coordinates)));

        assertTrue(outline.containsCell(12, 0), "the eastern tip of the oval");
        assertFalse(outline.containsCell(0, 0), "the middle of the oval is enclosed");
        for (int angleStep = 0; angleStep < 360; angleStep += 15) {
            double radians = Math.toRadians(angleStep);
            int columnX = (int) Math.round(Math.cos(radians) * 40.0D);
            int columnZ = (int) Math.round(Math.sin(radians) * 40.0D);
            assertTrue(outline.isWithin(columnX, columnZ, 64),
                    "40 columns off the middle of the oval lies inside a 64 block zone at " + columnX + "/" + columnZ);
            assertFalse(outline.isWithin(columnX * 3, columnZ * 3, 64),
                    "120 columns off the middle lies outside of it at " + columnX * 3 + "/" + columnZ * 3);
        }
    }

    @Test
    void holdsExactlyTheCellsWithAStraightNeighbourOutsideOfTheShape() {
        ArenaShape shape = raggedShape();
        ArenaOutline outline = ArenaOutline.of(shape);

        int expectedCount = 0;
        for (int columnX = -20; columnX <= 20; columnX++) {
            for (int columnZ = -20; columnZ <= 20; columnZ++) {
                boolean expected = shape.containsCell(columnX, columnZ)
                        && (!shape.containsCell(columnX + 1, columnZ)
                        || !shape.containsCell(columnX - 1, columnZ)
                        || !shape.containsCell(columnX, columnZ + 1)
                        || !shape.containsCell(columnX, columnZ - 1));
                assertEquals(expected, outline.containsCell(columnX, columnZ),
                        "the contour at " + columnX + "/" + columnZ);
                if (expected) {
                    expectedCount++;
                }
            }
        }
        assertEquals(expectedCount, outline.getCellCount());
    }

    @Test
    void findsTheSameNearestCellAPlainScanWouldFind() {
        ArenaShape shape = raggedShape();
        ArenaOutline outline = ArenaOutline.of(shape);
        List<int[]> contourCells = collectContourCells(shape);

        for (int columnX = -30; columnX <= 30; columnX += 3) {
            for (int columnZ = -30; columnZ <= 30; columnZ += 3) {
                assertEquals(nearestDistance(contourCells, columnX, columnZ), outline.distanceTo(columnX, columnZ),
                        TOLERANCE, "the distance at " + columnX + "/" + columnZ);
            }
        }
    }

    private static boolean isInsideOval(int columnX, int columnZ) {
        double relativeX = columnX / 12.0D;
        double relativeZ = columnZ / 8.0D;
        return relativeX * relativeX + relativeZ * relativeZ <= 1.0D;
    }

    private static ArenaShape raggedShape() {
        List<Integer> coordinates = new ArrayList<>();
        for (int columnX = -6; columnX <= 6; columnX++) {
            for (int columnZ = -6; columnZ <= 6; columnZ++) {
                boolean bay = columnX >= 2 && columnZ >= -1 && columnZ <= 1;
                boolean hole = columnX >= -2 && columnX <= -1 && columnZ >= 3 && columnZ <= 4;
                if (!bay && !hole) {
                    coordinates.add(columnX);
                    coordinates.add(columnZ);
                }
            }
        }
        for (int columnZ = 7; columnZ <= 13; columnZ++) {
            coordinates.add(-4);
            coordinates.add(columnZ);
        }
        return shape(toArray(coordinates));
    }

    private static List<int[]> collectContourCells(ArenaShape shape) {
        List<int[]> contourCells = new ArrayList<>();
        for (long cell : shape.getCells()) {
            int columnX = HorizontalCellCodec.decodeX(cell);
            int columnZ = HorizontalCellCodec.decodeZ(cell);
            if (!shape.containsCell(columnX + 1, columnZ) || !shape.containsCell(columnX - 1, columnZ)
                    || !shape.containsCell(columnX, columnZ + 1) || !shape.containsCell(columnX, columnZ - 1)) {
                contourCells.add(new int[] {columnX, columnZ});
            }
        }
        return contourCells;
    }

    private static double nearestDistance(List<int[]> contourCells, int blockX, int blockZ) {
        double nearest = Double.MAX_VALUE;
        for (int[] cell : contourCells) {
            double distanceX = cell[0] - blockX;
            double distanceZ = cell[1] - blockZ;
            nearest = Math.min(nearest, distanceX * distanceX + distanceZ * distanceZ);
        }
        return Math.sqrt(nearest);
    }

    private static ArenaShape square(int originX, int originZ, int edgeLength) {
        List<Integer> coordinates = new ArrayList<>();
        for (int columnX = originX; columnX < originX + edgeLength; columnX++) {
            for (int columnZ = originZ; columnZ < originZ + edgeLength; columnZ++) {
                coordinates.add(columnX);
                coordinates.add(columnZ);
            }
        }
        return shape(toArray(coordinates));
    }

    private static ArenaShape cross() {
        return shape(0, 4, 1, 4, 2, 4, 3, 4, 4, 4, 5, 4, 6, 4, 7, 4, 8, 4,
                4, 0, 4, 1, 4, 2, 4, 3, 4, 5, 4, 6, 4, 7, 4, 8);
    }

    private static int[] toArray(List<Integer> coordinates) {
        int[] values = new int[coordinates.size()];
        for (int index = 0; index < values.length; index++) {
            values[index] = coordinates.get(index);
        }
        return values;
    }

    private static ArenaShape shape(int... coordinatePairs) {
        long[] cells = new long[coordinatePairs.length / 2];
        for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
            cells[cellIndex] = HorizontalCellCodec.encode(coordinatePairs[cellIndex * 2],
                    coordinatePairs[cellIndex * 2 + 1]);
        }
        return new ArenaShape(WORLD_NAME, 64, 70, cells);
    }
}
