package de.simplepvparena.gui;

import de.simplepvparena.tournament.BracketMatch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitingMatchResultAdminMenuSnapshotTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MATCH_DECIDE_WAITING_MATCH_SLOT = 14;
    private static final int FIRST_WAITING_MATCH_SLOT = 0;

    private static final String MATCH_ID = "round1-match2";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsBothTeamsOfTheWaitingMatch() {
        startATournamentOfFourTeams();

        openTheResultOfTheWaitingMatch();

        assertEquals("""
                Result of round1-match2 | 27
                4 | BOOK | Decide a waiting match | Match round1-match2 (%1$s vs %2$s) / Round 1 / \
                A click decides the match for that team
                11 | PLAYER_HEAD | %1$s | %1$s-first / %1$s-second / Decides the match for this team
                15 | PLAYER_HEAD | %2$s | %2$s-first / %2$s-second / Decides the match for this team
                22 | OAK_DOOR | Back | Returns to the waiting matches
                """.formatted(firstTeamName(), secondTeamName()), fixture.describeLastMenu());
    }

    @Test
    void showsATeamThatIsNoLongerRegisteredWithoutMembers() {
        startATournamentOfFourTeams();
        assertTrue(fixture.teamRegistry().removeTeam(firstTeamName()));

        openTheResultOfTheWaitingMatch();

        assertEquals("""
                Result of round1-match2 | 27
                4 | BOOK | Decide a waiting match | Match round1-match2 (%1$s vs %2$s) / Round 1 / \
                A click decides the match for that team
                11 | PLAYER_HEAD | %1$s | No longer registered / Decides the match for this team
                15 | PLAYER_HEAD | %2$s | %2$s-first / %2$s-second / Decides the match for this team
                22 | OAK_DOOR | Back | Returns to the waiting matches
                """.formatted(firstTeamName(), secondTeamName()), fixture.describeLastMenu());
    }

    @Test
    void showsBothTeamsThatAreNoLongerRegisteredWithoutMembers() {
        startATournamentOfFourTeams();
        assertTrue(fixture.teamRegistry().removeTeam(firstTeamName()));
        assertTrue(fixture.teamRegistry().removeTeam(secondTeamName()));

        openTheResultOfTheWaitingMatch();

        assertEquals("""
                Result of round1-match2 | 27
                4 | BOOK | Decide a waiting match | Match round1-match2 (%1$s vs %2$s) / Round 1 / \
                A click decides the match for that team
                11 | PLAYER_HEAD | %1$s | No longer registered / Decides the match for this team
                15 | PLAYER_HEAD | %2$s | No longer registered / Decides the match for this team
                22 | OAK_DOOR | Back | Returns to the waiting matches
                """.formatted(firstTeamName(), secondTeamName()), fixture.describeLastMenu());
    }

    @Test
    void showsThatTheMatchIsNotWaitingAnyMoreWhenTheMenuIsFilledAfterItWasDecided() {
        startATournamentOfFourTeams();
        openTheResultOfTheWaitingMatch();
        assertTrue(fixture.tournamentManager().forceMatchResult(MATCH_ID, firstTeamName()).successful());

        fixture.refillLastMenu();

        assertEquals("""
                Result of round1-match2 | 27
                4 | BOOK | Decide a waiting match | Match round1-match2 is not waiting any more
                22 | OAK_DOOR | Back | Returns to the waiting matches
                """, fixture.describeLastMenu());
    }

    @Test
    void showsThatTheMatchIsNotWaitingAnyMoreWhenTheMenuIsFilledAfterItStarted() {
        startATournamentOfFourTeams();
        openTheResultOfTheWaitingMatch();
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        assertEquals(MATCH_ID, fixture.runningMatch().getMatchId());

        fixture.refillLastMenu();

        assertEquals("""
                Result of round1-match2 | 27
                4 | BOOK | Decide a waiting match | Match round1-match2 is not waiting any more
                22 | OAK_DOOR | Back | Returns to the waiting matches
                """, fixture.describeLastMenu());
    }

    private void startATournamentOfFourTeams() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
    }

    private void openTheResultOfTheWaitingMatch() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);
        fixture.click(FIRST_WAITING_MATCH_SLOT);
        assertEquals(AdminMenuType.WAITING_MATCH_RESULT, fixture.lastMenu().holder().getMenuType());
    }

    private BracketMatch bracketMatch() {
        return fixture.tournamentManager().getBracket().orElseThrow().findMatch(MATCH_ID).orElseThrow();
    }

    private String firstTeamName() {
        return bracketMatch().getFirstTeamName().orElseThrow();
    }

    private String secondTeamName() {
        return bracketMatch().getSecondTeamName().orElseThrow();
    }
}
