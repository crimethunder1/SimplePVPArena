package de.simplepvparena.tournament;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.TournamentStatusMessage;

public enum FinalMatchStage {

    PENDING,
    READY,
    RUNNING,
    FINISHED;

    public String describe() {
        return Messages.text(switch (this) {
            case PENDING -> TournamentStatusMessage.STAGE_PENDING;
            case READY -> TournamentStatusMessage.STAGE_READY;
            case RUNNING -> TournamentStatusMessage.STAGE_RUNNING;
            case FINISHED -> TournamentStatusMessage.STAGE_FINISHED;
        });
    }
}
