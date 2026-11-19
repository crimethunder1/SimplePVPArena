package de.simplepvparena.match;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum MatchState {
    PREPARING,
    RUNNING,
    BETWEEN_ROUNDS,
    FINISHED,
    CANCELLED
}
