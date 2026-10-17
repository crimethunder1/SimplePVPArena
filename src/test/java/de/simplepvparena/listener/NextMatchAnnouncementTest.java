package de.simplepvparena.listener;

import de.simplepvparena.tournament.BracketMatch;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NextMatchAnnouncementTest {

    private static final String SECOND_MATCH_ID = "round1-match2";
    private static final String THIRD_MATCH_ID = "round1-match3";
    private static final String FOURTH_MATCH_ID = "round1-match4";
    private static final String[] EIGHT_TEAM_NAMES = {
            "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel"};

    @Test
    void theNextMatchIsAnnouncedInChatAndActionBarAsSoonAsTheWinnerIsRecorded() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        ArenaMatchHarness.FakePlayer outsider = tournament.addPlayerFarOutside("Outsider");
        tournament.startTournament();
        BracketMatch secondMatch = bracketMatch(tournament, SECOND_MATCH_ID);
        String nextFirst = secondMatch.getFirstTeamName().orElseThrow();
        String nextSecond = secondMatch.getSecondTeamName().orElseThrow();
        String finishedFirst = tournament.firstTeamName();
        String finishedSecond = tournament.secondTeamName();

        tournament.winMatch(finishedFirst);

        String announcement = "Next: " + nextFirst + " against " + nextSecond + " (" + SECOND_MATCH_ID + ")";
        assertTrue(tournament.isCleaning(), "the arena is still being cleaned");
        assertFalse(tournament.matchService().hasActiveMatch(), "the next match has not started yet");
        for (ArenaMatchHarness.FakePlayer receiver : List.of(tournament.fighterOf(nextFirst),
                tournament.fighterOf(nextSecond), tournament.fighterOf(finishedFirst),
                tournament.fighterOf(finishedSecond), bystander)) {
            assertEquals(1, TournamentAudienceHarness.countMessages(receiver, announcement), receiver.name());
            assertEquals(List.of(announcement), receiver.actionBars(), receiver.name());
        }
        assertEquals(0, TournamentAudienceHarness.countMessages(outsider, announcement));
        assertEquals(List.of(), outsider.actionBars());
        assertFalse(tournament.broadcasts().contains(announcement));
    }

    @Test
    void theNextMatchIsAnnouncedBeforeItsOwnAnnouncementAfterTheCleaning() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        BracketMatch secondMatch = bracketMatch(tournament, SECOND_MATCH_ID);
        String nextFirst = secondMatch.getFirstTeamName().orElseThrow();
        String nextSecond = secondMatch.getSecondTeamName().orElseThrow();
        tournament.winMatch(tournament.firstTeamName());
        assertEquals(1, bystander.messages().stream().filter(message -> message.startsWith("Next: ")).count());
        bystander.forgetReceivedTexts();

        tournament.finishCleaning();

        assertEquals(List.of("Match " + SECOND_MATCH_ID + ": " + nextFirst + " against " + nextSecond),
                bystander.messages().stream().filter(message -> message.startsWith("Match " + SECOND_MATCH_ID
                        + ": ") || message.startsWith("Next: ")).toList(),
                "the cleaning brings only the match announcement, the line about the next match came before");
    }

    @Test
    void aNextMatchWithAMissingTeamIsNotAnnounced() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        tournament.teamRegistry().removeTeam(bracketMatch(tournament, SECOND_MATCH_ID).getSecondTeamName()
                .orElseThrow());

        tournament.winMatch(tournament.firstTeamName());

        assertTrue(bystander.messages().stream().noneMatch(message -> message.startsWith("Next: ")),
                bystander.messages().toString());
        assertEquals(List.of(), bystander.actionBars());
    }

    @Test
    void aNextMatchThatWaitsForAnAdminDecisionIsNotAnnounced() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        BracketMatch secondMatch = bracketMatch(tournament, SECOND_MATCH_ID);
        tournament.teamRegistry().removeTeam(secondMatch.getFirstTeamName().orElseThrow());
        tournament.teamRegistry().removeTeam(secondMatch.getSecondTeamName().orElseThrow());

        tournament.winMatch(tournament.firstTeamName());
        tournament.finishCleaning();

        assertTrue(bystander.messages().stream().noneMatch(message -> message.startsWith("Next: ")
                        || message.startsWith("The final will be played")), bystander.messages().toString());
        assertEquals(List.of(), bystander.actionBars());
    }

    @Test
    void theFinalIsAnnouncedOnceBothFinalistsAreKnownAndTheAdminsAreToldOnlyOnce() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        ArenaMatchHarness.FakePlayer outsider = tournament.addPlayerFarOutside("Outsider");
        tournament.startTournament();
        tournament.winMatch(tournament.firstTeamName());
        tournament.finishCleaning();
        bystander.forgetReceivedTexts();
        tournament.forgetReceivedTexts();
        String secondMatchFirst = tournament.firstTeamName();

        tournament.winMatch(secondMatchFirst);

        BracketMatch finalMatch = tournament.tournamentManager().getBracket().orElseThrow().getFinalMatch();
        String firstFinalist = finalMatch.getFirstTeamName().orElseThrow();
        String secondFinalist = finalMatch.getSecondTeamName().orElseThrow();
        String announcement = "The final will be played by " + firstFinalist + " and " + secondFinalist
                + ", an admin starts it";
        for (ArenaMatchHarness.FakePlayer receiver : List.of(tournament.fighterOf(firstFinalist),
                tournament.fighterOf(secondFinalist), bystander)) {
            assertEquals(1, TournamentAudienceHarness.countMessages(receiver, announcement), receiver.name());
            assertEquals(List.of(announcement), receiver.actionBars(), receiver.name());
        }
        assertEquals(List.of(), outsider.actionBars());
        assertEquals(0, TournamentAudienceHarness.countMessages(outsider, announcement));
        assertTrue(bystander.messages().stream().noneMatch(message -> message.startsWith("Next: ")));
        assertEquals(List.of("The final " + firstFinalist + " against " + secondFinalist + " is ready, an admin "
                        + "starts it with \"Start the final\" in /tournament"),
                tournament.adminMessages().stream().filter(message -> message.startsWith("The final ")).toList());
    }

    @Test
    void theEndOfTheTournamentAnnouncesNoNextMatch() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        tournament.winMatch(tournament.firstTeamName());
        tournament.finishCleaning();
        tournament.winMatch(tournament.firstTeamName());
        tournament.finishCleaning();
        tournament.startFinal();
        bystander.forgetReceivedTexts();

        tournament.winMatch(tournament.firstTeamName());

        assertTrue(bystander.messages().stream().noneMatch(message -> message.startsWith("Next: ")
                || message.startsWith("The final will be played")), bystander.messages().toString());
        assertEquals(List.of(), bystander.actionBars());
        assertTrue(tournament.broadcasts().get(tournament.broadcasts().size() - 1)
                .startsWith("The tournament is over, the winner is "));
    }

    @Test
    void aPausedTournamentAnnouncesNoNextMatchAndTheMatchItselfIsAnnouncedAfterTheResume() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        BracketMatch secondMatch = bracketMatch(tournament, SECOND_MATCH_ID);
        String nextFirst = secondMatch.getFirstTeamName().orElseThrow();
        String nextSecond = secondMatch.getSecondTeamName().orElseThrow();
        tournament.tournamentManager().togglePause();

        tournament.winMatch(tournament.firstTeamName());

        assertTrue(bystander.messages().stream().noneMatch(message -> message.startsWith("Next: ")),
                bystander.messages().toString());
        assertEquals(List.of(), bystander.actionBars());

        tournament.tournamentManager().togglePause();
        tournament.finishCleaning();

        assertEquals(1, TournamentAudienceHarness.countMessages(bystander,
                "Match " + SECOND_MATCH_ID + ": " + nextFirst + " against " + nextSecond));
    }

    @Test
    void withoutTheArenaAudienceTheNextMatchIsBroadcastInTheChat() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withoutArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        tournament.startTournament();
        BracketMatch secondMatch = bracketMatch(tournament, SECOND_MATCH_ID);

        tournament.winMatch(tournament.firstTeamName());

        assertTrue(tournament.broadcasts().contains("Next: " + secondMatch.getFirstTeamName().orElseThrow()
                + " against " + secondMatch.getSecondTeamName().orElseThrow() + " (" + SECOND_MATCH_ID + ")"),
                tournament.broadcasts().toString());
    }

    @Test
    void aWalkoverAnnouncesTheNextMatchBeforeItStarts() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(EIGHT_TEAM_NAMES);
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        ArenaMatchHarness.FakePlayer outsider = tournament.addPlayerFarOutside("Outsider");
        tournament.startTournament();
        tournament.teamRegistry().removeTeam(bracketMatch(tournament, SECOND_MATCH_ID).getSecondTeamName()
                .orElseThrow());
        tournament.winMatch(tournament.firstTeamName());
        bystander.forgetReceivedTexts();
        tournament.forgetReceivedTexts();

        tournament.finishCleaning();

        BracketMatch thirdMatch = bracketMatch(tournament, THIRD_MATCH_ID);
        String nextFirst = thirdMatch.getFirstTeamName().orElseThrow();
        String nextSecond = thirdMatch.getSecondTeamName().orElseThrow();
        String announcement = "Next: " + nextFirst + " against " + nextSecond + " (" + THIRD_MATCH_ID + ")";
        assertEquals(List.of(announcement, "Match " + THIRD_MATCH_ID + ": " + nextFirst + " against " + nextSecond),
                bystander.messages().stream().filter(message -> message.startsWith("Next: ")
                        || message.startsWith("Match " + THIRD_MATCH_ID + ": ")).toList());
        assertEquals(List.of(announcement), bystander.actionBars());
        assertEquals(1, TournamentAudienceHarness.countMessages(tournament.fighterOf(nextFirst), announcement));
        assertEquals(1, TournamentAudienceHarness.countMessages(tournament.fighterOf(nextSecond), announcement));
        assertEquals(0, TournamentAudienceHarness.countMessages(outsider, announcement));
    }

    @Test
    void aChainOfWalkoversAnnouncesOnlyTheMatchThatIsStillOpen() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(EIGHT_TEAM_NAMES);
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        tournament.teamRegistry().removeTeam(bracketMatch(tournament, SECOND_MATCH_ID).getSecondTeamName()
                .orElseThrow());
        tournament.teamRegistry().removeTeam(bracketMatch(tournament, THIRD_MATCH_ID).getSecondTeamName()
                .orElseThrow());
        tournament.winMatch(tournament.firstTeamName());
        bystander.forgetReceivedTexts();

        tournament.finishCleaning();

        BracketMatch fourthMatch = bracketMatch(tournament, FOURTH_MATCH_ID);
        String announcement = "Next: " + fourthMatch.getFirstTeamName().orElseThrow() + " against "
                + fourthMatch.getSecondTeamName().orElseThrow() + " (" + FOURTH_MATCH_ID + ")";
        assertTrue(bracketMatch(tournament, SECOND_MATCH_ID).isDecided());
        assertTrue(bracketMatch(tournament, THIRD_MATCH_ID).isDecided());
        assertEquals(FOURTH_MATCH_ID, tournament.runningMatch().getMatchId());
        assertEquals(List.of(announcement), bystander.messages().stream()
                .filter(message -> message.startsWith("Next: ")).toList());
        assertEquals(List.of(announcement), bystander.actionBars());
    }

    @Test
    void aResultForcedForTheRunningMatchAnnouncesTheNextMatch() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        BracketMatch secondMatch = bracketMatch(tournament, SECOND_MATCH_ID);
        String announcement = "Next: " + secondMatch.getFirstTeamName().orElseThrow() + " against "
                + secondMatch.getSecondTeamName().orElseThrow() + " (" + SECOND_MATCH_ID + ")";

        assertTrue(tournament.tournamentManager().forceMatchResult(tournament.runningMatch().getMatchId(),
                tournament.firstTeamName()).successful());

        assertEquals(1, TournamentAudienceHarness.countMessages(bystander, announcement));
        assertEquals(List.of(announcement), bystander.actionBars());
    }

    @Test
    void aWaitingMatchDecidedByAnAdminAnnouncesTheNextMatch() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(EIGHT_TEAM_NAMES);
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        ArenaMatchHarness.FakePlayer outsider = tournament.addPlayerFarOutside("Outsider");
        tournament.startTournament();
        tournament.winMatch(tournament.firstTeamName());
        bystander.forgetReceivedTexts();
        BracketMatch secondMatch = bracketMatch(tournament, SECOND_MATCH_ID);
        BracketMatch thirdMatch = bracketMatch(tournament, THIRD_MATCH_ID);
        String announcement = "Next: " + thirdMatch.getFirstTeamName().orElseThrow() + " against "
                + thirdMatch.getSecondTeamName().orElseThrow() + " (" + THIRD_MATCH_ID + ")";

        assertTrue(tournament.tournamentManager().forceMatchResult(SECOND_MATCH_ID,
                secondMatch.getFirstTeamName().orElseThrow()).successful());

        assertFalse(tournament.matchService().hasActiveMatch(), "the arena is still being cleaned");
        assertEquals(List.of(announcement), bystander.messages().stream()
                .filter(message -> message.startsWith("Next: ")).toList());
        assertEquals(List.of(announcement), bystander.actionBars());
        assertEquals(0, TournamentAudienceHarness.countMessages(outsider, announcement));
    }

    @Test
    void theFinalIsAnnouncedWhenAWaitingMatchDecidedByAnAdminCompletesTheFinalists() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        tournament.winMatch(tournament.firstTeamName());
        bystander.forgetReceivedTexts();
        tournament.forgetReceivedTexts();

        assertTrue(tournament.tournamentManager().forceMatchResult(SECOND_MATCH_ID,
                bracketMatch(tournament, SECOND_MATCH_ID).getFirstTeamName().orElseThrow()).successful());

        assertFinalAnnouncedOnce(tournament, bystander);
    }

    @Test
    void theFinalIsAnnouncedWhenAWalkoverCompletesTheFinalists() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        tournament.teamRegistry().removeTeam(bracketMatch(tournament, SECOND_MATCH_ID).getSecondTeamName()
                .orElseThrow());
        tournament.winMatch(tournament.firstTeamName());
        bystander.forgetReceivedTexts();
        tournament.forgetReceivedTexts();

        tournament.finishCleaning();

        assertFinalAnnouncedOnce(tournament, bystander);
    }

    @Test
    void aWaitingMatchDecidedDuringThePauseAnnouncesNoNextMatch() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(EIGHT_TEAM_NAMES);
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        tournament.winMatch(tournament.firstTeamName());
        tournament.tournamentManager().togglePause();
        bystander.forgetReceivedTexts();

        assertTrue(tournament.tournamentManager().forceMatchResult(SECOND_MATCH_ID,
                bracketMatch(tournament, SECOND_MATCH_ID).getFirstTeamName().orElseThrow()).successful());

        assertTrue(bystander.messages().stream().noneMatch(message -> message.startsWith("Next: ")),
                bystander.messages().toString());
        assertEquals(List.of(), bystander.actionBars());
    }

    @Test
    void aFinalDecidedByAnAdminBeforeItStartsAnnouncesNoNextMatch() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        tournament.winMatch(tournament.firstTeamName());
        tournament.finishCleaning();
        tournament.winMatch(tournament.firstTeamName());
        tournament.finishCleaning();
        BracketMatch finalMatch = tournament.tournamentManager().getBracket().orElseThrow().getFinalMatch();
        bystander.forgetReceivedTexts();

        assertTrue(tournament.tournamentManager().forceMatchResult(finalMatch.getMatchId(),
                finalMatch.getFirstTeamName().orElseThrow()).successful());

        assertTrue(bystander.messages().stream().noneMatch(message -> message.startsWith("Next: ")
                || message.startsWith("The final will be played")), bystander.messages().toString());
        assertEquals(List.of(), bystander.actionBars());
        assertTrue(tournament.broadcasts().get(tournament.broadcasts().size() - 1)
                .startsWith("The tournament is over, the winner is "));
    }

    @Test
    void aWaitingMatchDecidedWhileAnotherMatchRunsDoesNotAnnounceTheRunningMatch() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        String runningMatchId = tournament.runningMatch().getMatchId();
        bystander.forgetReceivedTexts();

        assertTrue(tournament.tournamentManager().forceMatchResult(SECOND_MATCH_ID,
                bracketMatch(tournament, SECOND_MATCH_ID).getFirstTeamName().orElseThrow()).successful());

        assertEquals(runningMatchId, tournament.runningMatch().getMatchId());
        assertTrue(bystander.messages().stream().noneMatch(message -> message.startsWith("Next: ")
                || message.startsWith("The final will be played")), bystander.messages().toString());
        assertEquals(List.of(), bystander.actionBars());

        tournament.forgetReceivedTexts();
        tournament.winMatch(tournament.firstTeamName());

        assertFinalAnnouncedOnce(tournament, bystander);
    }

    private static void assertFinalAnnouncedOnce(TournamentAudienceHarness tournament,
                                                 ArenaMatchHarness.FakePlayer bystander) {
        BracketMatch finalMatch = tournament.tournamentManager().getBracket().orElseThrow().getFinalMatch();
        String firstFinalist = finalMatch.getFirstTeamName().orElseThrow();
        String secondFinalist = finalMatch.getSecondTeamName().orElseThrow();
        String announcement = "The final will be played by " + firstFinalist + " and " + secondFinalist
                + ", an admin starts it";
        for (ArenaMatchHarness.FakePlayer receiver : List.of(tournament.fighterOf(firstFinalist),
                tournament.fighterOf(secondFinalist), bystander)) {
            assertEquals(1, TournamentAudienceHarness.countMessages(receiver, announcement), receiver.name());
            assertEquals(List.of(announcement), receiver.actionBars(), receiver.name());
        }
    }

    private static BracketMatch bracketMatch(TournamentAudienceHarness tournament, String matchId) {
        return tournament.tournamentManager().getBracket().orElseThrow().findMatch(matchId).orElseThrow();
    }
}
