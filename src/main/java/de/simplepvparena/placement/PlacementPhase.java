package de.simplepvparena.placement;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum PlacementPhase {
    TOURNAMENT_RUNNING,
    TOURNAMENT_PAUSED,
    TOURNAMENT_OVER,
    NONE;

    public boolean isTournament() {
        return this == TOURNAMENT_RUNNING || this == TOURNAMENT_PAUSED || this == TOURNAMENT_OVER;
    }
}
