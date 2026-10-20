package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaHeightMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;

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
            return Optional.of(refusal(ArenaHeightMarkerMode.LOWEST, blockY, highestY));
        }
        lowestY = blockY;
        return Optional.empty();
    }

    private Optional<String> storeHighest(int blockY) {
        if (lowestY != null && blockY <= lowestY) {
            return Optional.of(refusal(ArenaHeightMarkerMode.HIGHEST, blockY, lowestY));
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
            return Messages.text(ArenaHeightMessage.NOTHING_MARKED);
        }
        if (highestY == null) {
            return Messages.text(ArenaHeightMessage.ONLY_LOWEST_MARKED, Placeholder.of("lowestY", lowestY));
        }
        if (lowestY == null) {
            return Messages.text(ArenaHeightMessage.ONLY_HIGHEST_MARKED, Placeholder.of("highestY", highestY));
        }
        return Messages.text(ArenaHeightMessage.BOTH_MARKED, Placeholder.of("lowestY", lowestY),
                Placeholder.of("highestY", highestY));
    }

    public Optional<String> findIncompleteHint() {
        if (isEmpty() || isComplete()) {
            return Optional.empty();
        }
        ArenaHeightMarkerMode markedMode = lowestY == null
                ? ArenaHeightMarkerMode.HIGHEST : ArenaHeightMarkerMode.LOWEST;
        return Optional.of(Messages.text(markedMode.select(ArenaHeightMessage.INCOMPLETE_ONLY_LOWEST,
                        ArenaHeightMessage.INCOMPLETE_ONLY_HIGHEST),
                Placeholder.of("markedY", lowestY == null ? highestY : lowestY)));
    }

    private String refusal(ArenaHeightMarkerMode mode, int blockY, int otherY) {
        return Messages.text(mode.select(ArenaHeightMessage.REFUSAL_LOWEST, ArenaHeightMessage.REFUSAL_HIGHEST),
                Placeholder.of("otherY", otherY), Placeholder.of("blockY", blockY));
    }
}
