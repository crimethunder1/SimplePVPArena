package de.simplepvparena.tournament;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum AutoMatchState {

    IDLE,
    ANNOUNCED,
    RUNNING,
    COOLDOWN
}
