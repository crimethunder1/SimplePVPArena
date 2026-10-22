package de.simplepvparena.tournament;

import de.simplepvparena.message.FinalMessage;
import de.simplepvparena.message.MatchMenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;

import java.util.Objects;
import java.util.Optional;

public final class FinalMatchGate {

    private final AdminNotifier adminNotifier;

    private FinalMatchStage stage = FinalMatchStage.PENDING;

    public FinalMatchGate(AdminNotifier adminNotifier) {
        this.adminNotifier = Objects.requireNonNull(adminNotifier, "adminNotifier");
    }

    public FinalMatchStage getStage() {
        return stage;
    }

    public void synchronizeWith(BracketMatch finalMatch) {
        Objects.requireNonNull(finalMatch, "finalMatch");
        FinalMatchStage nextStage = determineStage(finalMatch);
        if (nextStage == stage) {
            return;
        }
        stage = nextStage;
        if (stage == FinalMatchStage.READY) {
            adminNotifier.notifyAdmins(buildReadyMessage(finalMatch));
        }
    }

    public void markRunning() {
        if (stage != FinalMatchStage.READY) {
            throw new IllegalStateException("the final match is not ready, its stage is " + stage);
        }
        stage = FinalMatchStage.RUNNING;
    }

    public void returnToReady() {
        if (stage != FinalMatchStage.RUNNING) {
            throw new IllegalStateException("the final match is not running, its stage is " + stage);
        }
        stage = FinalMatchStage.READY;
    }

    public void reset() {
        stage = FinalMatchStage.PENDING;
    }

    public Optional<String> findStartRejection() {
        return switch (stage) {
            case PENDING -> Optional.of(Messages.text(FinalMessage.NOT_READY));
            case READY -> Optional.empty();
            case RUNNING -> Optional.of(Messages.text(FinalMessage.ALREADY_RUNNING));
            case FINISHED -> Optional.of(Messages.text(FinalMessage.ALREADY_DECIDED));
        };
    }

    public static String describeFinalStartAction() {
        return Messages.text(FinalMessage.START_REFERENCE,
                Placeholder.of("tile", Messages.text(MatchMenuMessage.FINAL_TITLE)));
    }

    static String buildReadyMessage(BracketMatch finalMatch) {
        return Messages.text(FinalMessage.READY,
                Placeholder.of("firstTeam", finalMatch.getFirstTeamName().orElseThrow()),
                Placeholder.of("secondTeam", finalMatch.getSecondTeamName().orElseThrow()),
                Placeholder.of("action", describeFinalStartAction()));
    }

    private FinalMatchStage determineStage(BracketMatch finalMatch) {
        if (finalMatch.isDecided()) {
            return FinalMatchStage.FINISHED;
        }
        if (stage == FinalMatchStage.RUNNING) {
            return FinalMatchStage.RUNNING;
        }
        return finalMatch.hasBothParticipants() ? FinalMatchStage.READY : FinalMatchStage.PENDING;
    }
}
