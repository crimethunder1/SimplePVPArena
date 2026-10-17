package de.simplepvparena.tournament;

import de.simplepvparena.arena.Arena;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentManagerRestoreBaselineTest {

    private static final List<String> FOUR_TEAMS = List.of("Alpha", "Bravo", "Charlie", "Delta");

    private static final String NO_BASELINE_TEXT = "No baseline";

    @Test
    void warnsRightAfterTheRestoreThatNoBaselineIsStored() {
        InMemoryTournamentStateStorage storage = savedTournamentWithOneDecision();

        TournamentManagerFixture after = TournamentManagerFixture.withStorageWithoutBaseline(storage, FOUR_TEAMS);
        assertTrue(after.tournamentManager().restoreSavedTournament());

        List<String> messages = after.adminMessages();
        assertEquals(List.of(TournamentManager.RESTORED_WITHOUT_BASELINE_WARNING),
                after.adminMessagesContaining(NO_BASELINE_TEXT));
        int restoreMessageIndex = messages.indexOf(after.adminMessagesContaining("was restored").get(0));
        assertEquals(TournamentManager.RESTORED_WITHOUT_BASELINE_WARNING, messages.get(restoreMessageIndex + 1),
                messages.toString());
        assertEquals("No baseline of the fight area is stored after the restart. If the server crashed, repair "
                + "the arena first, then run \"Calculate the fight area\" in the arena menu of /tournament again, "
                + "that takes the arena as it is now as the new baseline, and only then continue the tournament",
                TournamentManager.RESTORED_WITHOUT_BASELINE_WARNING);
        assertTrue(TournamentManager.RESTORED_WITHOUT_BASELINE_WARNING.contains(Arena.CALCULATE_ACTION));
    }

    @Test
    void sendsNoBaselineWarningWhenTheRestoredTournamentFindsABaseline() {
        InMemoryTournamentStateStorage storage = savedTournamentWithOneDecision();

        TournamentManagerFixture after = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        assertTrue(after.tournamentManager().restoreSavedTournament());

        assertEquals(List.of(), after.adminMessagesContaining(NO_BASELINE_TEXT));
        assertEquals(1, after.adminMessagesContaining("was restored").size(), after.adminMessages().toString());
    }

    @Test
    void sendsNoBaselineWarningWhenNothingIsRestored() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorageWithoutBaseline(
                new InMemoryTournamentStateStorage(), FOUR_TEAMS);

        assertFalse(fixture.tournamentManager().restoreSavedTournament());

        assertEquals(List.of(), fixture.adminMessages());
    }

    @Test
    void doesNotRepeatTheBaselineWarningWhenTheRestoredTournamentContinues() {
        InMemoryTournamentStateStorage storage = savedTournamentWithOneDecision();
        TournamentManagerFixture after = TournamentManagerFixture.withStorageWithoutBaseline(storage, FOUR_TEAMS);
        assertTrue(after.tournamentManager().restoreSavedTournament());

        assertTrue(after.tournamentManager().togglePause().successful());

        assertEquals(List.of(TournamentManager.RESTORED_WITHOUT_BASELINE_WARNING),
                after.adminMessagesContaining(NO_BASELINE_TEXT));
    }

    private static InMemoryTournamentStateStorage savedTournamentWithOneDecision() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture before = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        before.startTournament();
        before.forceRunningMatchForTheFirstTeam();
        return storage;
    }
}
