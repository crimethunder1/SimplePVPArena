package de.simplepvparena.gui;

import de.simplepvparena.tournament.BracketMatch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitingMatchesAdminMenuSnapshotTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MATCH_DECIDE_WAITING_MATCH_SLOT = 14;
    private static final int NEXT_PAGE_SLOT = 53;

    private static final int TEAM_COUNT_FOR_TWO_PAGES = 128;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsThatNoMatchIsWaitingWhileTheFinalRuns() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.startFinal();

        openTheWaitingMatches();

        assertEquals("""
                Waiting matches (page 1) | 54
                47 | BOOK | Waiting matches | No match is waiting
                49 | OAK_DOOR | Back | Returns to the match menu
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheOneMatchThatWaitsBesideTheRunningMatch() {
        startATournamentOfFourTeams();

        openTheWaitingMatches();

        assertEquals("""
                Waiting matches (page 1) | 54
                0 | PLAYER_HEAD | round1-match2 | %1$s vs %2$s / Round 1
                47 | BOOK | Waiting matches | Waiting matches: 1 / Page 1 of 1 / \
                A click on a match decides it for one of its teams
                49 | OAK_DOOR | Back | Returns to the match menu
                """.formatted(firstTeamOf("round1-match2"), secondTeamOf("round1-match2")),
                fixture.describeLastMenu());
    }

    @Test
    void showsTheReadyFinalAsAWaitingMatch() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();

        openTheWaitingMatches();

        assertEquals("""
                Waiting matches (page 1) | 54
                0 | PLAYER_HEAD | round1-match1 | %1$s vs %2$s / Round 1
                47 | BOOK | Waiting matches | Waiting matches: 1 / Page 1 of 1 / \
                A click on a match decides it for one of its teams
                49 | OAK_DOOR | Back | Returns to the match menu
                """.formatted(firstTeamOf("round1-match1"), secondTeamOf("round1-match1")),
                fixture.describeLastMenu());
    }

    @Test
    void showsTheWaitingMatchesByRoundAndPosition() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        fixture.startTournament();
        decideForTheFirstTeam("round1-match4");
        decideForTheFirstTeam("round1-match3");

        openTheWaitingMatches();

        assertEquals("""
                Waiting matches (page 1) | 54
                0 | PLAYER_HEAD | round1-match2 | %1$s vs %2$s / Round 1
                1 | PLAYER_HEAD | round2-match2 | %3$s vs %4$s / Round 2
                47 | BOOK | Waiting matches | Waiting matches: 2 / Page 1 of 1 / \
                A click on a match decides it for one of its teams
                49 | OAK_DOOR | Back | Returns to the match menu
                """.formatted(firstTeamOf("round1-match2"), secondTeamOf("round1-match2"),
                        firstTeamOf("round2-match2"), secondTeamOf("round2-match2")),
                fixture.describeLastMenu());
    }

    @Test
    void namesATeamOfAWaitingMatchThatIsNoLongerRegistered() {
        startATournamentOfFourTeams();
        assertTrue(fixture.teamRegistry().removeTeam(secondTeamOf("round1-match2")));

        openTheWaitingMatches();

        assertEquals("""
                Waiting matches (page 1) | 54
                0 | PLAYER_HEAD | round1-match2 | %1$s vs %2$s / Round 1 / %2$s is no longer registered
                47 | BOOK | Waiting matches | Waiting matches: 1 / Page 1 of 1 / \
                A click on a match decides it for one of its teams
                49 | OAK_DOOR | Back | Returns to the match menu
                """.formatted(firstTeamOf("round1-match2"), secondTeamOf("round1-match2")),
                fixture.describeLastMenu());
    }

    @Test
    void namesBothTeamsOfAWaitingMatchThatAreNoLongerRegistered() {
        startATournamentOfFourTeams();
        assertTrue(fixture.teamRegistry().removeTeam(firstTeamOf("round1-match2")));
        assertTrue(fixture.teamRegistry().removeTeam(secondTeamOf("round1-match2")));

        openTheWaitingMatches();

        assertEquals("""
                Waiting matches (page 1) | 54
                0 | PLAYER_HEAD | round1-match2 | %1$s vs %2$s / Round 1 / %1$s is no longer registered / \
                %2$s is no longer registered
                47 | BOOK | Waiting matches | Waiting matches: 1 / Page 1 of 1 / \
                A click on a match decides it for one of its teams
                49 | OAK_DOOR | Back | Returns to the match menu
                """.formatted(firstTeamOf("round1-match2"), secondTeamOf("round1-match2")),
                fixture.describeLastMenu());
    }

    @Test
    void showsTheFirstPageOfManyWaitingMatchesWithTheNextPage() {
        startATournamentForTwoPages();

        openTheWaitingMatches();

        StringBuilder expected = new StringBuilder("Waiting matches (page 1) | 54\n");
        for (int slot = 0; slot < MenuPagination.DEFAULT_ITEMS_PER_PAGE; slot++) {
            expected.append(describeFirstRoundTile(slot, slot + 2));
        }
        expected.append("""
                47 | BOOK | Waiting matches | Waiting matches: 63 / Page 1 of 2 / \
                A click on a match decides it for one of its teams
                49 | OAK_DOOR | Back | Returns to the match menu
                53 | ARROW | Next page | Page 2 of 2
                """);
        assertEquals(expected.toString(), fixture.describeLastMenu());
    }

    @Test
    void showsTheSecondPageOfManyWaitingMatchesWithThePreviousPage() {
        startATournamentForTwoPages();
        openTheWaitingMatches();

        fixture.click(NEXT_PAGE_SLOT);

        StringBuilder expected = new StringBuilder("Waiting matches (page 2) | 54\n");
        for (int slot = 0; slot < 18; slot++) {
            expected.append(describeFirstRoundTile(slot, slot + 47));
        }
        expected.append("""
                45 | ARROW | Previous page | Page 1 of 2
                47 | BOOK | Waiting matches | Waiting matches: 63 / Page 2 of 2 / \
                A click on a match decides it for one of its teams
                49 | OAK_DOOR | Back | Returns to the match menu
                """);
        assertEquals(expected.toString(), fixture.describeLastMenu());
    }

    @Test
    void showsThatNoMatchIsWaitingWhenTheMenuIsFilledAfterTheLastWaitingMatchWasDecided() {
        startATournamentOfFourTeams();
        openTheWaitingMatches();
        decideForTheFirstTeam("round1-match2");

        fixture.refillLastMenu();

        assertEquals("""
                Waiting matches (page 1) | 54
                47 | BOOK | Waiting matches | No match is waiting
                49 | OAK_DOOR | Back | Returns to the match menu
                """, fixture.describeLastMenu());
    }

    private void startATournamentOfFourTeams() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
    }

    private void startATournamentForTwoPages() {
        fixture.configureArena();
        fixture.registerNumberedTeams(TEAM_COUNT_FOR_TWO_PAGES);
        fixture.startTournament();
        assertEquals("round1-match1", fixture.runningMatch().getMatchId());
    }

    private void openTheWaitingMatches() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);
    }

    private String describeFirstRoundTile(int slot, int matchNumber) {
        String matchId = "round1-match" + matchNumber;
        return slot + " | PLAYER_HEAD | " + matchId + " | " + firstTeamOf(matchId) + " vs " + secondTeamOf(matchId)
                + " / Round 1\n";
    }

    private void decideForTheFirstTeam(String matchId) {
        assertTrue(fixture.tournamentManager().forceMatchResult(matchId, firstTeamOf(matchId)).successful());
    }

    private BracketMatch bracketMatch(String matchId) {
        return fixture.tournamentManager().getBracket().orElseThrow().findMatch(matchId).orElseThrow();
    }

    private String firstTeamOf(String matchId) {
        return bracketMatch(matchId).getFirstTeamName().orElseThrow();
    }

    private String secondTeamOf(String matchId) {
        return bracketMatch(matchId).getSecondTeamName().orElseThrow();
    }
}
