package de.simplepvparena.gui;

import de.simplepvparena.tournament.FinalMatchStage;
import de.simplepvparena.tournament.TournamentState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentMenuOptionsTest {

    @Test
    void offersTheStartWhileNoTournamentIsInProgress() {
        TournamentMenuOptions options = optionsFor(TournamentState.IDLE);

        assertTrue(options.startOffered());
        assertTrue(options.forceStartOffered());
        assertFalse(options.cancelOffered());
        assertFalse(options.pauseOffered());
        assertFalse(options.resumeOffered());
    }

    @Test
    void offersTheStartAgainAfterATournamentWasCancelledOrFinished() {
        assertTrue(optionsFor(TournamentState.CANCELLED).startOffered());
        assertTrue(optionsFor(TournamentState.FINISHED).startOffered());
    }

    @Test
    void offersThePauseAndTheCancelWhileATournamentRuns() {
        TournamentMenuOptions options = optionsFor(TournamentState.RUNNING);

        assertFalse(options.startOffered());
        assertFalse(options.forceStartOffered());
        assertTrue(options.pauseOffered());
        assertFalse(options.resumeOffered());
        assertTrue(options.cancelOffered());
    }

    @Test
    void offersTheContinuationInsteadOfThePauseWhileTheTournamentIsPaused() {
        TournamentMenuOptions options = optionsFor(TournamentState.PAUSED);

        assertFalse(options.pauseOffered());
        assertTrue(options.resumeOffered());
        assertTrue(options.cancelOffered());
        assertFalse(options.startOffered());
    }

    @Test
    void offersTheFinalOnlyWhileItIsReadyAndTheTournamentRuns() {
        assertTrue(TournamentMenuOptions.of(TournamentState.RUNNING, FinalMatchStage.READY, false)
                .finalStartOffered());
        assertFalse(TournamentMenuOptions.of(TournamentState.RUNNING, FinalMatchStage.PENDING, false)
                .finalStartOffered());
        assertFalse(TournamentMenuOptions.of(TournamentState.RUNNING, FinalMatchStage.RUNNING, true)
                .finalStartOffered());
        assertFalse(TournamentMenuOptions.of(TournamentState.RUNNING, FinalMatchStage.FINISHED, false)
                .finalStartOffered());
        assertFalse(TournamentMenuOptions.of(TournamentState.PAUSED, FinalMatchStage.READY, false)
                .finalStartOffered());
    }

    @Test
    void offersTheForcedResultOnlyWhileAMatchRuns() {
        assertTrue(TournamentMenuOptions.of(TournamentState.RUNNING, FinalMatchStage.PENDING, true)
                .forceResultOffered());
        assertFalse(TournamentMenuOptions.of(TournamentState.RUNNING, FinalMatchStage.PENDING, false)
                .forceResultOffered());
    }

    private TournamentMenuOptions optionsFor(TournamentState state) {
        return TournamentMenuOptions.of(state, FinalMatchStage.PENDING, false);
    }
}
