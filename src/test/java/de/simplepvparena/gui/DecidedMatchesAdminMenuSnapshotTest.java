package de.simplepvparena.gui;

import de.simplepvparena.match.Match;
import de.simplepvparena.message.DecidedMatchMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.tournament.BracketMatch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecidedMatchesAdminMenuSnapshotTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MATCH_REVOKE_RESULT_SLOT = 20;
    private static final int FIRST_MATCH_SLOT = 0;
    private static final int NEXT_PAGE_SLOT = 53;

    private static final int TEAM_COUNT_FOR_TWO_PAGES = 128;
    private static final int LAST_DECIDED_MATCH_NUMBER_FOR_TWO_PAGES = 48;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsThatNoResultCanBeTakenBackBeforeAMatchIsDecided() {
        startATournamentOfFourTeams();

        openTheDecidedMatches();

        assertEquals("""
                Decided matches (page 1) | 54
                47 | BOOK | Decided matches | No result can be taken back
                49 | OAK_DOOR | Back | Returns to the match menu
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheMatchDecidedByAnAdminWithItsWinner() {
        startATournamentOfFourTeams();
        decideForTheFirstTeam("round1-match2");

        openTheDecidedMatches();

        assertEquals("""
                Decided matches (page 1) | 54
                0 | PLAYER_HEAD | round1-match2 | %1$s against %2$s / Won by %1$s / Decided by an admin / \
                Click to take the result back
                47 | BOOK | Decided matches | Results that can be taken back: 1 / Page 1 of 1
                49 | OAK_DOOR | Back | Returns to the match menu
                """.formatted(firstTeamOf("round1-match2"), secondTeamOf("round1-match2")),
                fixture.describeLastMenu());
    }

    @Test
    void showsHowEveryMatchWasDecidedByRoundAndPosition() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        fixture.startTournament();
        assertTrue(fixture.teamRegistry().removeTeam(secondTeamOf("round1-match2")));
        decideForTheFirstTeam("round1-match4");
        playTheRunningMatchForTheFirstTeam();
        assertEquals("round1-match3", fixture.runningMatch().getMatchId());

        openTheDecidedMatches();

        assertEquals("""
                Decided matches (page 1) | 54
                0 | PLAYER_HEAD | round1-match1 | %1$s against %2$s / Won by %1$s / Played / \
                Click to take the result back
                1 | PLAYER_HEAD | round1-match2 | %3$s against %4$s / Won by %3$s / Walkover / \
                Click to take the result back
                2 | PLAYER_HEAD | round1-match4 | %5$s against %6$s / Won by %5$s / Decided by an admin / \
                Click to take the result back
                47 | BOOK | Decided matches | Results that can be taken back: 3 / Page 1 of 1
                49 | OAK_DOOR | Back | Returns to the match menu
                """.formatted(firstTeamOf("round1-match1"), secondTeamOf("round1-match1"),
                        firstTeamOf("round1-match2"), secondTeamOf("round1-match2"),
                        firstTeamOf("round1-match4"), secondTeamOf("round1-match4")),
                fixture.describeLastMenu());
    }

    @Test
    void leavesTheSemifinalsOutOnceTheFinalRuns() {
        startATournamentOfFourTeams();
        decideForTheFirstTeam("round1-match2");
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        fixture.startFinal();

        openTheDecidedMatches();

        assertEquals("""
                Decided matches (page 1) | 54
                47 | BOOK | Decided matches | No result can be taken back
                49 | OAK_DOOR | Back | Returns to the match menu
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheFirstPageOfManyDecidedMatchesWithTheNextPage() {
        startATournamentForTwoPages();

        openTheDecidedMatches();

        StringBuilder expected = new StringBuilder("Decided matches (page 1) | 54\n");
        for (int slot = 0; slot < MenuPagination.DEFAULT_ITEMS_PER_PAGE; slot++) {
            expected.append(describeForcedFirstRoundTile(slot, slot + 2));
        }
        expected.append("""
                47 | BOOK | Decided matches | Results that can be taken back: 47 / Page 1 of 2
                49 | OAK_DOOR | Back | Returns to the match menu
                53 | ARROW | Next page | Page 2 of 2
                """);
        assertEquals(expected.toString(), fixture.describeLastMenu());
    }

    @Test
    void showsTheSecondPageOfManyDecidedMatchesWithThePreviousPage() {
        startATournamentForTwoPages();
        openTheDecidedMatches();

        fixture.click(NEXT_PAGE_SLOT);

        StringBuilder expected = new StringBuilder("Decided matches (page 2) | 54\n");
        for (int slot = 0; slot < 2; slot++) {
            expected.append(describeForcedFirstRoundTile(slot, slot + 47));
        }
        expected.append("""
                45 | ARROW | Previous page | Page 1 of 2
                47 | BOOK | Decided matches | Results that can be taken back: 47 / Page 2 of 2
                49 | OAK_DOOR | Back | Returns to the match menu
                """);
        assertEquals(expected.toString(), fixture.describeLastMenu());
    }

    @Test
    void asksForTheConfirmationWithTheWinnerAndTheMatchThatIsOpenAgain() {
        startATournamentOfFourTeams();
        decideForTheFirstTeam("round1-match2");
        openTheDecidedMatches();

        fixture.click(FIRST_MATCH_SLOT);

        assertEquals("""
                Take back the result of round1-match2? | 27
                4 | BOOK | Take back the result of round1-match2? | %1$s no longer advances / \
                Match round1-match2 is open again
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """.formatted(firstTeamOf("round1-match2")), fixture.describeLastMenu());
    }

    @Test
    void namesTheReasonInTheConfirmationOnceTheResultCanNoLongerBeTakenBack() {
        startATournamentOfFourTeams();
        decideForTheFirstTeam("round1-match2");
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        openTheDecidedMatches();
        fixture.click(FIRST_MATCH_SLOT);
        fixture.startFinal();

        fixture.refillLastMenu();

        assertEquals("""
                Take back the result of round1-match1? | 27
                4 | BOOK | Take back the result of round1-match1? | The following match round2-match1 is running
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
    }

    @Test
    @UseLanguage("de")
    void showsTheDecidedMatchesAndTheConfirmationInGerman() {
        startATournamentOfFourTeams();
        openTheDecidedMatches();

        assertEquals("""
                Entschiedene Matches (Seite 1) | 54
                47 | BOOK | Entschiedene Matches | Kein Ergebnis lässt sich zurücknehmen
                49 | OAK_DOOR | Zurück | Zurück zum Match-Menü
                """, fixture.describeLastMenu());

        decideForTheFirstTeam("round1-match2");
        openTheDecidedMatches();

        assertEquals("""
                Entschiedene Matches (Seite 1) | 54
                0 | PLAYER_HEAD | round1-match2 | %1$s gegen %2$s / Gewonnen von %1$s / Von einem Admin entschieden / \
                Klicke, um das Ergebnis zurückzunehmen
                47 | BOOK | Entschiedene Matches | Zurücknehmbare Ergebnisse: 1 / Seite 1 von 1
                49 | OAK_DOOR | Zurück | Zurück zum Match-Menü
                """.formatted(firstTeamOf("round1-match2"), secondTeamOf("round1-match2")),
                fixture.describeLastMenu());

        fixture.click(FIRST_MATCH_SLOT);

        assertEquals("""
                Ergebnis von round1-match2 zurücknehmen? | 27
                4 | BOOK | Ergebnis von round1-match2 zurücknehmen? | %1$s rückt nicht mehr vor / \
                Match round1-match2 ist wieder offen
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """.formatted(firstTeamOf("round1-match2")), fixture.describeLastMenu());
        assertEquals("Gespielt", Messages.text(DecidedMatchMessage.PLAYED));
        assertEquals("Kampflos", Messages.text(DecidedMatchMessage.WALKOVER));
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
        for (int matchNumber = 2; matchNumber <= LAST_DECIDED_MATCH_NUMBER_FOR_TWO_PAGES; matchNumber++) {
            decideForTheFirstTeam("round1-match" + matchNumber);
        }
        assertEquals("round1-match1", fixture.runningMatch().getMatchId());
    }

    private void openTheDecidedMatches() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_REVOKE_RESULT_SLOT);
    }

    private String describeForcedFirstRoundTile(int slot, int matchNumber) {
        String matchId = "round1-match" + matchNumber;
        return slot + " | PLAYER_HEAD | " + matchId + " | " + firstTeamOf(matchId) + " against "
                + secondTeamOf(matchId) + " / Won by " + firstTeamOf(matchId)
                + " / Decided by an admin / Click to take the result back\n";
    }

    private void decideForTheFirstTeam(String matchId) {
        assertTrue(fixture.tournamentManager().forceMatchResult(matchId, firstTeamOf(matchId)).successful());
    }

    private void playTheRunningMatchForTheFirstTeam() {
        Match match = fixture.runningMatch();
        fixture.matchService().cancelActiveMatch();
        fixture.tournamentManager().onMatchCompleted(match, match.getFirstTeam().getTeamName());
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
