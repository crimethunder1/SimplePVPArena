package de.simplepvparena.arena;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@NullMarked
public record ArenaHeightMarkResult(ArenaHeightMarkerMode markedEnd, ArenaHeightMarkerMode nextEnd,
                                    @Nullable String rejectionMessage) {

    public static ArenaHeightMarkResult taken(ArenaHeightMarkerMode markedEnd,
                                              ArenaHeightMarkerMode nextEnd) {
        return new ArenaHeightMarkResult(markedEnd, nextEnd, null);
    }

    public static ArenaHeightMarkResult rejected(ArenaHeightMarkerMode markedEnd,
                                                 ArenaHeightMarkerMode nextEnd, String rejectionMessage) {
        return new ArenaHeightMarkResult(markedEnd, nextEnd, rejectionMessage);
    }

    public boolean isTaken() {
        return rejectionMessage == null;
    }

    public boolean hasMovedOn() {
        return markedEnd != nextEnd;
    }

    public Optional<String> getRejectionMessage() {
        return Optional.ofNullable(rejectionMessage);
    }
}
