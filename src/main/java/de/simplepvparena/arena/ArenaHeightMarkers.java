package de.simplepvparena.arena;

import de.simplepvparena.message.TournamentMessages;

import java.util.Optional;
import java.util.OptionalInt;

public final class ArenaHeightMarkers {

    private Integer lowestY;
    private Integer highestY;
    private ArenaHeightMarkerMode nextEnd = ArenaHeightMarkerMode.LOWEST;

    public ArenaHeightMarkerMode getNextEnd() {
        return nextEnd;
    }

    public ArenaHeightMarkerMode switchNextEnd() {
        nextEnd = nextEnd.toggle();
        return nextEnd;
    }

    public ArenaHeightMarkResult mark(int blockY) {
        ArenaHeightMarkerMode markedEnd = nextEnd;
        Optional<String> rejection = markedEnd == ArenaHeightMarkerMode.LOWEST
                ? storeLowest(blockY) : storeHighest(blockY);
        if (rejection.isPresent()) {
            return ArenaHeightMarkResult.rejected(markedEnd, nextEnd, rejection.get());
        }
        ArenaHeightMarkerMode missingEnd = markedEnd.toggle();
        if (!isMarked(missingEnd)) {
            nextEnd = missingEnd;
        }
        return ArenaHeightMarkResult.taken(markedEnd, nextEnd);
    }

    private Optional<String> storeLowest(int blockY) {
        if (highestY != null && blockY >= highestY) {
            return Optional.of(refusal(ArenaHeightMarkerMode.LOWEST, blockY, "below", highestY));
        }
        lowestY = blockY;
        return Optional.empty();
    }

    private Optional<String> storeHighest(int blockY) {
        if (lowestY != null && blockY <= lowestY) {
            return Optional.of(refusal(ArenaHeightMarkerMode.HIGHEST, blockY, "above", lowestY));
        }
        highestY = blockY;
        return Optional.empty();
    }

    public OptionalInt getLowestY() {
        return lowestY == null ? OptionalInt.empty() : OptionalInt.of(lowestY);
    }

    public OptionalInt getHighestY() {
        return highestY == null ? OptionalInt.empty() : OptionalInt.of(highestY);
    }

    public boolean isMarked(ArenaHeightMarkerMode mode) {
        return (mode == ArenaHeightMarkerMode.LOWEST ? lowestY : highestY) != null;
    }

    public boolean isComplete() {
        return lowestY != null && highestY != null;
    }

    public boolean isEmpty() {
        return lowestY == null && highestY == null;
    }

    public Optional<ArenaShape> applyTo(ArenaShape shape) {
        if (!isComplete()) {
            return Optional.empty();
        }
        return Optional.of(shape.withVerticalExtent(lowestY, highestY));
    }

    public void clear() {
        lowestY = null;
        highestY = null;
        nextEnd = ArenaHeightMarkerMode.LOWEST;
    }

    public String describe() {
        if (isEmpty()) {
            return "No height is marked";
        }
        return "Lowest point: " + describePoint(lowestY) + ", highest point: " + describePoint(highestY);
    }

    public Optional<String> findIncompleteHint() {
        if (isEmpty() || isComplete()) {
            return Optional.empty();
        }
        ArenaHeightMarkerMode markedMode = lowestY == null
                ? ArenaHeightMarkerMode.HIGHEST : ArenaHeightMarkerMode.LOWEST;
        return Optional.of("Only the " + markedMode.getDisplayName() + " of the fight area is marked, at y "
                + (lowestY == null ? highestY : lowestY)
                + TournamentMessages.LINE_BREAK + "A single mark is not used, the area keeps the height of the "
                + "border wall. Mark the " + markedMode.toggle().getDisplayName() + " as well and calculate again");
    }

    private String refusal(ArenaHeightMarkerMode mode, int blockY, String side, int otherY) {
        return "The " + mode.getDisplayName() + " has to lie " + side + " the "
                + mode.toggle().getDisplayName() + ", which is marked at y " + otherY + ", and y " + blockY
                + " does not"
                + TournamentMessages.LINE_BREAK + "Nothing was stored. Mark this end " + side + " y " + otherY
                + ", or switch to the " + mode.toggle().getDisplayName() + " and mark that one again first";
    }

    private static String describePoint(Integer blockY) {
        return blockY == null ? "not marked" : "y " + blockY;
    }
}
