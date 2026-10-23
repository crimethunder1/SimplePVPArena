package de.simplepvparena.gui;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.tournament.BracketMatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchListClickAfterChangeTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MATCH_DECIDE_WAITING_MATCH_SLOT = 14;
    private static final int MATCH_REVOKE_RESULT_SLOT = 20;
    private static final int FIRST_MATCH_SLOT = 0;
    private static final int SECOND_MATCH_SLOT = 1;
    private static final int TEAM_COUNT = 8;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void opensTheResultChoiceOfTheShownWaitingMatchAfterTheListChanged() {
        startATournamentOfEightTeams();
        openTheWaitingMatches();
        decideForTheFirstTeam("round1-match2");

        fixture.click(SECOND_MATCH_SLOT);

        assertEquals(AdminMenuType.WAITING_MATCH_RESULT, fixture.lastMenu().holder().getMenuType());
        assertEquals(Optional.of("round1-match3"), fixture.lastMenu().holder().getSubject());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void reportsAShownMatchThatIsNoLongerWaitingAndRefreshesTheList() {
        startATournamentOfEightTeams();
        openTheWaitingMatches();
        AdminMenuHolder waitingMatchesHolder = fixture.lastMenu().holder();
        decideForTheFirstTeam("round1-match2");

        fixture.click(FIRST_MATCH_SLOT);

        assertEquals(List.of("Match round1-match2 is not waiting any more"), fixture.messages());
        assertEquals(TournamentMessages.warning("Match round1-match2 is not waiting any more"),
                fixture.lastSentMessage());
        assertSame(waitingMatchesHolder, fixture.lastMenu().holder());
        assertEquals(1, fixture.lastMenu().refreshCount());
        assertEquals(Optional.of("round1-match3"), waitingMatchesHolder.findMatchIdOfSlot(FIRST_MATCH_SLOT));
    }

    @Test
    void asksForTheShownDecidedMatchAfterTheListChanged() {
        startATournamentOfEightTeams();
        decideForTheFirstTeam("round1-match2");
        decideForTheFirstTeam("round1-match3");
        openTheDecidedMatches();
        assertTrue(fixture.tournamentManager().revokeMatchResult("round1-match2").successful());
        fixture.messages().clear();

        fixture.click(SECOND_MATCH_SLOT);

        assertEquals(Optional.of(ConfirmationRequest.REVOKE_RESULT),
                fixture.lastMenu().holder().getConfirmationRequest());
        assertEquals(Optional.of("round1-match3"), fixture.lastMenu().holder().getSubject());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void reportsWhyAShownDecidedMatchCanNoLongerBeTakenBackAndRefreshesTheList() {
        startATournamentOfEightTeams();
        decideForTheFirstTeam("round1-match2");
        decideForTheFirstTeam("round1-match3");
        openTheDecidedMatches();
        AdminMenuHolder decidedMatchesHolder = fixture.lastMenu().holder();
        assertTrue(fixture.tournamentManager().revokeMatchResult("round1-match2").successful());
        fixture.messages().clear();

        fixture.click(FIRST_MATCH_SLOT);

        assertEquals(List.of(fixture.tournamentManager().findRevokeRejection("round1-match2").orElseThrow()),
                fixture.messages());
        assertSame(decidedMatchesHolder, fixture.lastMenu().holder());
        assertEquals(1, fixture.lastMenu().refreshCount());
        assertEquals(Optional.of("round1-match3"), decidedMatchesHolder.findMatchIdOfSlot(FIRST_MATCH_SLOT));
        assertEquals(Optional.empty(), decidedMatchesHolder.findMatchIdOfSlot(SECOND_MATCH_SLOT));
    }

    private void startATournamentOfEightTeams() {
        fixture.configureArena();
        fixture.registerNumberedTeams(TEAM_COUNT);
        fixture.startTournament();
    }

    private void openTheWaitingMatches() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);
        assertEquals(AdminMenuType.WAITING_MATCHES, fixture.lastMenu().holder().getMenuType());
        fixture.messages().clear();
    }

    private void openTheDecidedMatches() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_REVOKE_RESULT_SLOT);
        assertEquals(AdminMenuType.DECIDED_MATCHES, fixture.lastMenu().holder().getMenuType());
        fixture.messages().clear();
    }

    private void decideForTheFirstTeam(String matchId) {
        BracketMatch bracketMatch = fixture.tournamentManager().getBracket().orElseThrow().findMatch(matchId)
                .orElseThrow();
        assertTrue(fixture.tournamentManager().forceMatchResult(matchId,
                bracketMatch.getFirstTeamName().orElseThrow()).successful());
    }
}
