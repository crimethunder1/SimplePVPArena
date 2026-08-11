package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaHeightMarkersTest {

    private static final String WORLD_NAME = "event";

    @Test
    void keepsBothEndsOfTheVolumeFromTwoPlainPlacements() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();

        assertTrue(markers.isEmpty());
        assertFalse(markers.isComplete());
        assertEquals(ArenaHeightMarkerMode.LOWEST, markers.getNextEnd(), "a fresh pair starts at the floor");
        assertTrue(markers.mark(50).isTaken(), "the first mark is never refused");
        assertTrue(markers.mark(80).isTaken());

        assertTrue(markers.isComplete());
        assertEquals(OptionalInt.of(50), markers.getLowestY());
        assertEquals(OptionalInt.of(80), markers.getHighestY());
    }

    @Test
    void takesTheTwoEndsInEitherOrder() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();

        assertEquals(ArenaHeightMarkerMode.HIGHEST, markers.switchNextEnd());
        assertTrue(markers.mark(80).isTaken());
        assertEquals(ArenaHeightMarkerMode.LOWEST, markers.getNextEnd(),
                "the marking moves on to the end that is still missing, whichever it is");
        assertTrue(markers.mark(50).isTaken());

        assertTrue(markers.isComplete());
        assertEquals(OptionalInt.of(50), markers.getLowestY());
        assertEquals(OptionalInt.of(80), markers.getHighestY());
    }

    @Test
    void refusesALowestPointThatDoesNotLieBelowTheHighestOne() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();
        markers.switchNextEnd();
        markers.mark(80);

        ArenaHeightMarkResult aboveTheCeiling = markers.mark(90);
        ArenaHeightMarkResult atTheCeiling = markers.mark(80);

        assertFalse(aboveTheCeiling.isTaken(), "a floor above the ceiling is refused");
        assertFalse(atTheCeiling.isTaken(), "a floor at the ceiling is refused too");
        assertTrue(aboveTheCeiling.getRejectionMessage().orElseThrow().contains("y 80"),
                "the refusal names the mark that is in the way: " + aboveTheCeiling.getRejectionMessage());
        assertTrue(markers.getLowestY().isEmpty(), "a refused mark is never stored");
        assertFalse(markers.isComplete());
        assertEquals(OptionalInt.of(80), markers.getHighestY(), "the mark that was already there stays");
    }

    @Test
    void refusesAHighestPointThatDoesNotLieAboveTheLowestOne() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();
        markers.mark(50);

        ArenaHeightMarkResult belowTheFloor = markers.mark(40);
        ArenaHeightMarkResult atTheFloor = markers.mark(50);

        assertFalse(belowTheFloor.isTaken());
        assertFalse(atTheFloor.isTaken());
        assertTrue(belowTheFloor.getRejectionMessage().orElseThrow().contains("y 50"),
                "the refusal names the mark that is in the way: " + belowTheFloor.getRejectionMessage());
        assertTrue(markers.getHighestY().isEmpty(), "a refused mark is never stored");
        assertEquals(OptionalInt.of(50), markers.getLowestY());
    }

    @Test
    void replacesAMarkOfTheSameEnd() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();
        markers.mark(50);
        markers.mark(80);

        markers.switchNextEnd();
        assertTrue(markers.mark(60).isTaken());

        assertEquals(OptionalInt.of(60), markers.getLowestY());
        assertEquals(OptionalInt.of(80), markers.getHighestY());
    }

    @Test
    void changesNothingAboutAShapeUntilBothEndsAreMarked() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();
        ArenaShape shape = shape();

        assertTrue(markers.applyTo(shape).isEmpty(), "nothing is marked");
        markers.mark(50);
        assertTrue(markers.applyTo(shape).isEmpty(), "only the floor is marked");
        markers.mark(80);
        assertTrue(markers.applyTo(shape).isPresent(), "both ends are marked");
    }

    @Test
    void replacesOnlyTheVerticalExtentOfTheCalculatedArea() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();
        markers.mark(50);
        markers.mark(80);
        ArenaShape shape = shape();

        ArenaShape markedShape = markers.applyTo(shape).orElseThrow();

        assertEquals(50, markedShape.getFloorY());
        assertEquals(80, markedShape.getWallTopY());
        assertEquals(shape.getWorldName(), markedShape.getWorldName());
        assertEquals(shape.getReferenceY(), markedShape.getReferenceY());
        assertEquals(shape.getCellCount(), markedShape.getCellCount());
        assertTrue(markedShape.containsCell(0, 0));
        assertTrue(markedShape.containsCell(1, 0));
        assertFalse(markedShape.containsCell(5, 5), "no cell is added by a marked height");
    }

    @Test
    void reachesBelowTheBorderWall() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();
        markers.mark(20);
        markers.mark(30);

        ArenaShape markedShape = markers.applyTo(shape()).orElseThrow();

        assertEquals(20, markedShape.getFloorY());
        assertEquals(30, markedShape.getWallTopY());
        assertTrue(markedShape.containsBlock(0, 25, 0), "the marked volume is what a cleaning writes");
        assertFalse(markedShape.containsBlock(0, 64, 0),
                "the height of the wall is not part of it any more");
    }

    @Test
    void forgetsBothMarksAndStartsAtTheFloorAgainWhenTheyAreCleared() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();
        markers.mark(50);
        markers.mark(80);

        markers.clear();

        assertTrue(markers.isEmpty());
        assertFalse(markers.isComplete());
        assertTrue(markers.getLowestY().isEmpty());
        assertTrue(markers.getHighestY().isEmpty());
        assertEquals(ArenaHeightMarkerMode.LOWEST, markers.getNextEnd(),
                "a used up pair leaves the marking where a fresh one begins");
        assertTrue(markers.applyTo(shape()).isEmpty(), "a cleared pair never reshapes an area again");
    }

    @Test
    void namesTheSingleMarkThatWasNotUsed() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();

        assertTrue(markers.findIncompleteHint().isEmpty(), "nothing marked, nothing to say");

        markers.mark(50);
        String hint = markers.findIncompleteHint().orElseThrow();
        assertTrue(hint.contains("lowest point") && hint.contains("y 50"), hint);
        assertTrue(hint.contains("highest point"), "it names the mark that is missing: " + hint);

        markers.mark(80);
        assertTrue(markers.findIncompleteHint().isEmpty(), "a complete pair is used, not explained");
    }

    @Test
    void describesWhatIsMarkedRightNow() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();

        assertEquals("No height is marked", markers.describe());

        markers.mark(50);
        assertEquals("Lowest point: y 50, highest point: not marked", markers.describe());

        markers.mark(80);
        assertEquals("Lowest point: y 50, highest point: y 80", markers.describe());
    }

    @Test
    void switchesBetweenTheTwoEndsByHandWithoutTouchingTheMarks() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();
        markers.mark(50);

        assertEquals(ArenaHeightMarkerMode.HIGHEST, markers.getNextEnd());
        assertEquals(ArenaHeightMarkerMode.LOWEST, markers.switchNextEnd());
        assertEquals(ArenaHeightMarkerMode.HIGHEST, markers.switchNextEnd());
        assertEquals(OptionalInt.of(50), markers.getLowestY(), "switching stores and drops nothing");
    }

    @Test
    void movesOnToTheMissingEndAfterATakenMark() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();

        ArenaHeightMarkResult result = markers.mark(50);

        assertTrue(result.isTaken());
        assertEquals(ArenaHeightMarkerMode.LOWEST, result.markedEnd());
        assertEquals(ArenaHeightMarkerMode.HIGHEST, result.nextEnd());
        assertTrue(result.hasMovedOn());
        assertTrue(result.getRejectionMessage().isEmpty());
    }

    @Test
    void staysWhereItIsWhenTheMarkCompletesThePair() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();
        markers.mark(50);

        ArenaHeightMarkResult result = markers.mark(80);

        assertTrue(result.isTaken());
        assertEquals(ArenaHeightMarkerMode.HIGHEST, result.markedEnd());
        assertFalse(result.hasMovedOn(), "a complete pair moves the marking nowhere");
        assertEquals(ArenaHeightMarkerMode.HIGHEST, markers.getNextEnd());
        assertTrue(markers.isComplete());
    }

    @Test
    void staysWhereItIsWhenAnEndOfACompletePairIsMarkedAgain() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();
        markers.mark(50);
        markers.mark(80);

        ArenaHeightMarkResult result = markers.mark(90);

        assertTrue(result.isTaken());
        assertFalse(result.hasMovedOn(), "a correction stays on the end it corrects");
        assertEquals(OptionalInt.of(50), markers.getLowestY());
        assertEquals(OptionalInt.of(90), markers.getHighestY());
    }

    @Test
    void neverMovesOnAfterARefusedMark() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();
        markers.switchNextEnd();
        markers.mark(80);

        ArenaHeightMarkResult result = markers.mark(90);

        assertFalse(result.isTaken());
        assertEquals(ArenaHeightMarkerMode.LOWEST, result.markedEnd());
        assertFalse(result.hasMovedOn(), "a refused mark leaves the marking on the end it refused");
        assertEquals(ArenaHeightMarkerMode.LOWEST, markers.getNextEnd());
        assertTrue(result.getRejectionMessage().orElseThrow().contains("y 80"));
        assertTrue(markers.getLowestY().isEmpty());
    }

    @Test
    void answersWhichOfTheTwoEndsIsMarked() {
        ArenaHeightMarkers markers = new ArenaHeightMarkers();

        assertFalse(markers.isMarked(ArenaHeightMarkerMode.LOWEST));
        assertFalse(markers.isMarked(ArenaHeightMarkerMode.HIGHEST));

        markers.mark(50);

        assertTrue(markers.isMarked(ArenaHeightMarkerMode.LOWEST));
        assertFalse(markers.isMarked(ArenaHeightMarkerMode.HIGHEST));
    }

    @Test
    void namesAndTogglesTheTwoEnds() {
        assertEquals(ArenaHeightMarkerMode.HIGHEST, ArenaHeightMarkerMode.LOWEST.toggle());
        assertEquals(ArenaHeightMarkerMode.LOWEST, ArenaHeightMarkerMode.HIGHEST.toggle());
        assertEquals("lowest point", ArenaHeightMarkerMode.LOWEST.getDisplayName());
        assertEquals("highest point", ArenaHeightMarkerMode.HIGHEST.getDisplayName());
    }

    private static ArenaShape shape() {
        return new ArenaShape(WORLD_NAME, 64, 64, 70,
                new long[] {HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(1, 0)});
    }
}
