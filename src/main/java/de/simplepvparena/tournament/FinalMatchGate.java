package de.simplepvparena.tournament;

import java.util.Objects;
import java.util.Optional;

public final class FinalMatchGate {

    public static final String FINAL_START_ACTION = "\"Start the final\" in /tournament";

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

    public void reset() {
        stage = FinalMatchStage.PENDING;
    }

    public Optional<String> findStartRejection() {
        return switch (stage) {
            case PENDING -> Optional.of("The final is not ready yet, both finalists have to be determined first");
            case READY -> Optional.empty();
            case RUNNING -> Optional.of("The final is already running");
            case FINISHED -> Optional.of("The final is already decided");
        };
    }

    static String buildReadyMessage(BracketMatch finalMatch) {
        return "The final " + finalMatch.getFirstTeamName().orElseThrow() + " against "
                + finalMatch.getSecondTeamName().orElseThrow()
                + " is ready, an admin starts it with " + FINAL_START_ACTION;
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
