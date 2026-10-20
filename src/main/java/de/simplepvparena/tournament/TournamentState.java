package de.simplepvparena.tournament;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.TournamentStatusMessage;

public enum TournamentState {
    IDLE,
    RUNNING,
    PAUSED,
    CANCELLED,
    FINISHED;

    public String describe() {
        return Messages.text(switch (this) {
            case IDLE -> TournamentStatusMessage.STATE_IDLE;
            case RUNNING -> TournamentStatusMessage.STATE_RUNNING;
            case PAUSED -> TournamentStatusMessage.STATE_PAUSED;
            case CANCELLED -> TournamentStatusMessage.STATE_CANCELLED;
            case FINISHED -> TournamentStatusMessage.STATE_FINISHED;
        });
    }
}
