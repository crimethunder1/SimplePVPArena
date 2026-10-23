package de.simplepvparena.gui;

import de.simplepvparena.arena.MarkClearing;
import de.simplepvparena.arena.MarkClearingKind;
import de.simplepvparena.tournament.TournamentState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkClearingLockTest {

    private static final int MARKS_BEFORE = 5;
    private static final String RUNNING_MATCH = "A match is running, the arena can only be reset between matches";
    private static final String KIT_CHOICE = "The kit for the next match is being chosen, the arena is reserved for "
            + "that match until it starts";
    private static final String AUTO_MATCH = "An auto match is about to start, the arena is reserved for it";
    private static final String BEING_CLEANED = "The arena is still being cleaned. Try again in a moment.";

    private AdminMenuFixture fixture = new AdminMenuFixture();
    private int markCount = MARKS_BEFORE;
    private int forgettingCount;

    @Test
    void refusesBothClearingsWhileAMatchRuns() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        assertBothClearingsRefused(RUNNING_MATCH);
    }

    @Test
    void refusesBothClearingsWhileTheMatchIsPreparedForTheVoteOfUnequalTeams() {
        fixture.configureArena();
        AdminMenuFixture.OnlinePlayer anna = fixture.letAnotherPlayerJoin("Anna", false, true);
        AdminMenuFixture.OnlinePlayer ben = fixture.letAnotherPlayerJoin("Ben", false, true);
        AdminMenuFixture.OnlinePlayer cleo = fixture.letAnotherPlayerJoin("Cleo", false, true);
        fixture.registerTeamOfOnlinePlayers("Alpha", anna, ben);
        fixture.registerTeamOfOnlinePlayers("Bravo", cleo);
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        assertTrue(fixture.unequalTeamVoteService().getPending().isPresent());

        assertBothClearingsRefused(RUNNING_MATCH);
    }

    @Test
    void refusesBothClearingsWhileTheKitIsChosen() {
        fixture = AdminMenuFixture.withKitChoice();
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.startTournament();
        assertTrue(fixture.kitChoiceService().isPending());
        assertFalse(fixture.matchService().hasActiveMatch());

        assertBothClearingsRefused(KIT_CHOICE);
    }

    @Test
    void refusesBothClearingsWhileAnAutoMatchIsAnnounced() {
        fixture.configureArena();
        fixture.arenaSetupService().setArenaReservedForAutoMatch(() -> true);

        assertBothClearingsRefused(AUTO_MATCH);
    }

    @Test
    void refusesBothClearingsWhileTheArenaIsCleaned() {
        fixture.configureArena();
        fixture.standInsideTheFightArea();
        assertTrue(fixture.arenaSetupService().startCleaning(fixture.admin()));
        fixture.messages().clear();

        assertBothClearingsRefused(BEING_CLEANED);
    }

    @Test
    void allowsBothClearingsWhileATournamentWaitsForTheStartOfTheFinal() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
        assertFalse(fixture.matchService().hasActiveMatch());

        assertBothClearingsAllowed();
    }

    @Test
    void allowsBothClearingsWhileATournamentIsPausedBetweenTwoMatches() {
        startATournamentOfFourTeams();
        fixture.pauseTournament();
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        assertEquals(TournamentState.PAUSED, fixture.tournamentManager().getState());
        assertFalse(fixture.matchService().hasActiveMatch());

        assertBothClearingsAllowed();
    }

    @Test
    void allowsBothClearingsWithoutATournamentAndWithoutAMatch() {
        fixture.configureArena();

        assertBothClearingsAllowed();
    }

    @Test
    void refusesTheClearingWhenAMatchStartedAfterTheMenuWasShown() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        MarkClearing clearing = clearing(MarkClearingKind.BORDER_MARKS);
        assertEquals(Optional.empty(), clearing.findRejection());
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        clearing.clearEveryMark(fixture.admin());

        assertEquals(List.of(RUNNING_MATCH), fixture.messages());
        assertEquals(0, forgettingCount);
    }

    private void startATournamentOfFourTeams() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.messages().clear();
    }

    private void assertBothClearingsRefused(String reason) {
        for (MarkClearingKind kind : MarkClearingKind.values()) {
            MarkClearing clearing = clearing(kind);
            fixture.messages().clear();

            assertEquals(Optional.of(reason), clearing.findRejection(), kind.name());
            clearing.clearEveryMark(fixture.admin());

            assertEquals(List.of(reason), fixture.messages(), kind.name());
        }
        assertEquals(0, forgettingCount);
        assertEquals(MARKS_BEFORE, markCount);
    }

    private void assertBothClearingsAllowed() {
        fixture.messages().clear();

        clearing(MarkClearingKind.BORDER_MARKS).clearEveryMark(fixture.admin());
        markCount = MARKS_BEFORE;
        clearing(MarkClearingKind.START_GATE_MARKS).clearEveryMark(fixture.admin());

        assertEquals(List.of("5 border marks were forgotten", "5 start gate marks were forgotten"),
                fixture.messages());
        assertEquals(2, forgettingCount);
    }

    private MarkClearing clearing(MarkClearingKind kind) {
        return new MarkClearing(kind, () -> markCount, () -> {
            forgettingCount++;
            markCount = 0;
        }, fixture.arenaSetupService()::findResetRejection, warning -> { });
    }
}
