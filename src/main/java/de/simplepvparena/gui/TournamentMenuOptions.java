package de.simplepvparena.gui;

import de.simplepvparena.tournament.FinalMatchStage;
import de.simplepvparena.tournament.TournamentState;

public record TournamentMenuOptions(boolean startOffered,
                                    boolean forceStartOffered,
                                    boolean pauseOffered,
                                    boolean resumeOffered,
                                    boolean cancelOffered,
                                    boolean finalStartOffered,
                                    boolean forceResultOffered) {

    public static TournamentMenuOptions of(TournamentState state, FinalMatchStage finalMatchStage,
                                           boolean matchRunning) {
        boolean inProgress = state == TournamentState.RUNNING || state == TournamentState.PAUSED;
        return new TournamentMenuOptions(!inProgress, !inProgress,
                state == TournamentState.RUNNING,
                state == TournamentState.PAUSED,
                inProgress,
                state == TournamentState.RUNNING && finalMatchStage == FinalMatchStage.READY,
                matchRunning);
    }
}
