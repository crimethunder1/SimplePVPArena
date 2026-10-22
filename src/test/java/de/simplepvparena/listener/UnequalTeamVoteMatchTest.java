package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.match.UnequalTeamSettings;
import de.simplepvparena.match.UnequalTeamsMode;
import de.simplepvparena.tournament.AutoMatchState;
import de.simplepvparena.tournament.TournamentOperationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnequalTeamVoteMatchTest {

    private final UnequalTeamVoteHarness harness = new UnequalTeamVoteHarness();

    @Test
    void letsTheMatchOfTwoAgainstOneWaitForTheVoteOfTheLargerTeam() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");

        harness.startTheFinalOfTwoTeams();

        Match match = harness.runningMatch();
        assertEquals(MatchState.PREPARING, match.getState());
        assertTrue(match.getRoundParticipants().isEmpty(), "no round has begun");
        assertEquals("Alpha", harness.pendingVote().getVotingTeamName());
        assertEquals(1, harness.pendingVote().getFighterCount());
        assertEquals(1, harness.shownVotes().size());
        assertEquals(List.of(), harness.arena().getRoundStartSteps(), "nobody is teleported or equipped yet");
        assertTrue(alpha.get(0).messages().contains("Your team has more players online than Bravo: vote within 30 "
                + "seconds who fights. /tournament vote opens the vote again"));
    }

    @Test
    void letsOnlyTheChosenPlayerOfTwoAgainstOneFightInEveryRound() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben");
        List<ArenaMatchHarness.FakePlayer> bravo = harness.addTeam("Bravo", "Cleo");
        ArenaMatchHarness.FakePlayer anna = alpha.get(0);
        ArenaMatchHarness.FakePlayer ben = alpha.get(1);
        ArenaMatchHarness.FakePlayer cleo = bravo.get(0);
        harness.startTheFinalOfTwoTeams();

        harness.vote(anna, ben);
        harness.vote(ben, ben);

        assertFalse(harness.voteService().isPending());
        assertEquals(Set.of(ben.uniqueId()), harness.activePlayersOf("Alpha"));
        assertEquals(Set.of(cleo.uniqueId()), harness.activePlayersOf("Bravo"));
        assertFalse(harness.matchService().isActiveFighter(anna.uniqueId()));
        assertEquals(List.of(), harness.roundStartStepsOf(anna), "Anna is neither teleported nor equipped");
        assertTrue(harness.roundStartStepsOf(ben).contains("teleport Ben"));
        for (ArenaMatchHarness.FakePlayer player : List.of(anna, ben, cleo)) {
            assertEquals(1, player.messages().stream().filter("Alpha fights with Ben"::equals).count(),
                    player.name() + " reads the result once");
        }

        harness.eliminate(cleo);

        assertEquals(2, harness.runningMatch().getRoundNumber());
        assertEquals(Set.of(ben.uniqueId()), harness.activePlayersOf("Alpha"), "the choice holds for every round");
        assertEquals(1, harness.shownVotes().size(), "there is no new vote for the second round");

        harness.eliminate(cleo);

        assertFalse(harness.matchService().hasActiveMatch());
        assertEquals(List.of(), harness.roundStartStepsOf(anna), "Anna is not sent anywhere after the match");
    }

    @Test
    void letsTheTwoChosenPlayersOfThreeAgainstTwoFightAndChanceFillsThePlaceNobodyVotedFor() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben", "Cleo");
        harness.addTeam("Bravo", "Dora", "Emil");
        harness.startTheFinalOfTwoTeams();
        assertEquals(2, harness.pendingVote().getFighterCount());

        harness.vote(alpha.get(0), alpha.get(2));
        harness.vote(alpha.get(1), alpha.get(2));
        harness.advanceSeconds(30);

        assertFalse(harness.voteService().isPending());
        assertEquals(Set.of(alpha.get(2).uniqueId(), alpha.get(0).uniqueId()),
                Set.copyOf(harness.activePlayersOf("Alpha")));
        assertEquals(2, harness.activePlayersOf("Bravo").size());
        assertTrue(alpha.get(1).messages().contains("Alpha fights with Cleo and Anna"));
    }

    @Test
    void letsTwoOfFourAgainstTwoFight() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben", "Cleo", "Dora");
        harness.addTeam("Bravo", "Emil", "Finn");
        harness.startTheFinalOfTwoTeams();

        harness.vote(alpha.get(0), alpha.get(1));
        harness.vote(alpha.get(1), alpha.get(3));
        harness.vote(alpha.get(2), alpha.get(3));
        harness.vote(alpha.get(3), alpha.get(1));

        assertEquals(Set.of(alpha.get(1).uniqueId(), alpha.get(3).uniqueId()), harness.activePlayersOf("Alpha"));
        assertEquals(2, harness.activePlayersOf("Bravo").size());
        assertTrue(alpha.get(0).messages().contains("Alpha fights with Ben and Dora"));
    }

    @Test
    void startsTheFirstRoundAtOnceWhenBothTeamsHaveAsManyPlayersOnline() {
        harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo", "Dora");

        harness.startTheFinalOfTwoTeams();

        assertFalse(harness.voteService().isPending());
        assertEquals(MatchState.RUNNING, harness.runningMatch().getState());
        assertEquals(2, harness.activePlayersOf("Alpha").size());
        assertEquals(2, harness.activePlayersOf("Bravo").size());
        assertEquals(List.of(), harness.shownVotes());
    }

    @Test
    void letsEveryOnlinePlayerFightInTheModeAll() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");
        harness.matchService().setUnequalTeamSettings(new UnequalTeamSettings(UnequalTeamsMode.ALL, 30));

        harness.startTheFinalOfTwoTeams();

        assertFalse(harness.voteService().isPending());
        assertEquals(Set.of(alpha.get(0).uniqueId(), alpha.get(1).uniqueId()), harness.activePlayersOf("Alpha"));
        assertEquals(List.of(), harness.shownVotes());
        assertTrue(alpha.get(0).messages().stream().noneMatch(message -> message.contains("vote")));
    }

    @Test
    void takesANewModeFromTheNextMatchOn() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");
        harness.startTheFinalOfTwoTeams();

        harness.matchService().setUnequalTeamSettings(new UnequalTeamSettings(UnequalTeamsMode.ALL, 30));

        assertTrue(harness.voteService().isPending(), "the running vote is not touched");
        harness.vote(alpha.get(0), alpha.get(0));
        harness.vote(alpha.get(1), alpha.get(0));
        assertEquals(Set.of(alpha.get(0).uniqueId()), harness.activePlayersOf("Alpha"));
    }

    @Test
    void choosesTheKitBeforeTheVoteOfATournamentMatchBegins() {
        harness.letAnAdminChooseTheKit();
        harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");

        harness.startTheFinalOfTwoTeams();

        assertTrue(harness.kitChoiceService().isPending());
        assertFalse(harness.voteService().isPending());
        assertFalse(harness.matchService().hasActiveMatch());

        harness.kitChoiceService().choose("Knight", "admin");

        assertTrue(harness.voteService().isPending());
        assertEquals(MatchState.PREPARING, harness.runningMatch().getState());
    }

    @Test
    void cancelsTheVoteWithTheTournament() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");
        harness.startTheFinalOfTwoTeams();
        Match match = harness.runningMatch();

        assertTrue(harness.tournamentManager().cancelTournament().successful());
        harness.advanceSeconds(31);

        assertFalse(harness.voteService().isPending());
        assertEquals(1, harness.closedVotes().size(), "the menus of the vote are closed");
        assertFalse(harness.matchService().hasActiveMatch());
        assertTrue(match.getRoundParticipants().isEmpty(), "no round starts after the cancellation");
        assertFalse(harness.voteService().vote(alpha.get(0).uniqueId(), alpha.get(0).uniqueId()));
    }

    @Test
    void cancelsTheVoteWithTheRunningMatch() {
        harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");
        harness.startTheFinalOfTwoTeams();
        Match match = harness.runningMatch();

        harness.matchService().cancelActiveMatch();
        harness.advanceSeconds(31);

        assertFalse(harness.voteService().isPending());
        assertEquals(1, harness.closedVotes().size());
        assertEquals(MatchState.CANCELLED, match.getState());
        assertTrue(match.getRoundParticipants().isEmpty());
    }

    @Test
    void cancelsTheVoteWhenAnAdminDecidesTheMatch() {
        harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");
        harness.startTheFinalOfTwoTeams();
        Match match = harness.runningMatch();

        assertTrue(harness.tournamentManager().forceMatchResult(match.getMatchId(), "Bravo").successful());

        assertFalse(harness.voteService().isPending());
        assertEquals(1, harness.closedVotes().size());
        assertEquals(MatchState.FINISHED, match.getState());
    }

    @Test
    void refusesARestartWhileThePlayersVoteAndAllowsItOnceTheVoteIsOver() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");
        harness.startTheFinalOfTwoTeams();
        String matchId = harness.runningMatch().getMatchId();

        TournamentOperationResult restart = harness.tournamentManager().restartRunningMatch();

        assertFalse(restart.successful());
        assertEquals("The players of match " + matchId + " are still voting who fights, wait until the vote is "
                + "over", restart.message());
        assertTrue(harness.voteService().isPending(), "the vote goes on");

        harness.vote(alpha.get(0), alpha.get(0));
        harness.vote(alpha.get(1), alpha.get(0));
        assertTrue(harness.tournamentManager().restartRunningMatch().successful());
        assertTrue(harness.tournamentManager().startFinalMatch().successful());

        assertTrue(harness.voteService().isPending(), "the restarted match votes again");
        assertEquals(2, harness.shownVotes().size());
    }

    @Test
    void refusesTeamChangesOfBothTeamsWhileThePlayersVote() {
        harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");
        harness.startTheFinalOfTwoTeams();

        assertTrue(harness.tournamentManager().findTeamChangeRejection("Alpha").isPresent());
        assertTrue(harness.tournamentManager().findTeamChangeRejection("Bravo").isPresent());
    }

    @Test
    void keepsTheArenaOccupiedWhileThePlayersVote() {
        harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");
        harness.startTheFinalOfTwoTeams();

        assertTrue(harness.matchService().hasActiveMatch());
        assertFalse(harness.tournamentManager().startFinalMatch().successful(),
                "the final cannot start a second time");
        assertTrue(harness.tournamentManager().findWaitingMatches().isEmpty());
    }

    @Test
    void letsTheLargerTeamOfAnAutoMatchVoteAfterTheAnnouncement() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");

        harness.startAnAutoMatch();

        assertEquals(AutoMatchState.RUNNING, harness.autoMatchService().getState());
        assertTrue(harness.runningMatch().isAutoMatch());
        assertTrue(harness.voteService().isPending());
        assertTrue(harness.autoMatchService().occupiesTheArena());
        assertFalse(harness.tournamentManager().startTournament(false).successful(),
                "no tournament starts while the auto match votes");

        harness.vote(alpha.get(0), alpha.get(1));
        harness.vote(alpha.get(1), alpha.get(1));

        assertEquals(Set.of(alpha.get(1).uniqueId()), harness.activePlayersOf("Alpha"));
        assertEquals(AutoMatchState.RUNNING, harness.autoMatchService().getState());
    }

    @Test
    void endsTheVoteOfAnAutoMatchAfterTheVoteSeconds() {
        harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");
        harness.startAnAutoMatch();

        harness.advanceSeconds(30);

        assertFalse(harness.voteService().isPending());
        assertEquals(1, harness.activePlayersOf("Alpha").size());
        assertEquals(MatchState.RUNNING, harness.runningMatch().getState());
    }

    @Test
    void fillsNoPlaceOfAChosenPlayerWhoIsOfflineWhenARoundBegins() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");
        harness.startTheFinalOfTwoTeams();
        harness.vote(alpha.get(0), alpha.get(1));
        harness.vote(alpha.get(1), alpha.get(1));
        Match match = harness.runningMatch();

        alpha.get(1).setOnline(false);
        harness.eliminate(alpha.get(1));

        assertEquals(MatchState.FINISHED, match.getState(),
                "the second round begins without a fighter of Alpha and ends at once");
        assertEquals("2:0", describeScore(match, "Bravo", "Alpha"));
        assertEquals(List.of(), harness.roundStartStepsOf(alpha.get(0)), "Anna does not take the place of Ben");
    }

    @Test
    void fillsNoPlaceOfAChosenPlayerWhoseReconnectWindowRanOut() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben", "Cleo");
        harness.addTeam("Bravo", "Dora", "Emil");
        harness.matchService().setReconnectSeconds(5);
        harness.startTheFinalOfTwoTeams();
        harness.vote(alpha.get(0), alpha.get(0));
        harness.vote(alpha.get(1), alpha.get(1));
        harness.vote(alpha.get(2), alpha.get(0));
        Match match = harness.runningMatch();
        ArenaMatchHarness.FakePlayer ben = alpha.get(1);

        ben.setOnline(false);
        harness.matchService().handlePlayerDisconnected(ben.player());
        harness.advanceSeconds(5);

        assertEquals(Set.of(alpha.get(0).uniqueId()), harness.activePlayersOf("Alpha"));
        assertFalse(match.getRoundParticipants().orElseThrow().isActive(alpha.get(2).uniqueId()),
                "Cleo was not chosen and does not step in");
    }

    @Test
    void endsTheVoteWhenAPlayerOfTheLargerTeamLeavesAndOnlyThePlacesAreLeft() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben");
        harness.addTeam("Bravo", "Cleo");
        harness.startTheFinalOfTwoTeams();
        ArenaMatchHarness.FakePlayer anna = alpha.get(0);

        anna.setOnline(false);
        harness.matchService().handlePlayerDisconnected(anna.player());

        assertFalse(harness.voteService().isPending());
        assertEquals(Set.of(alpha.get(1).uniqueId()), harness.activePlayersOf("Alpha"));
        assertTrue(alpha.get(1).messages().contains("Alpha fights with Ben"));
    }

    @Test
    void sendsTheResultToBothTeamsAndThePlayersInTheAreaButNotToPlayersFarAway() {
        List<ArenaMatchHarness.FakePlayer> alpha = harness.addTeam("Alpha", "Anna", "Ben");
        List<ArenaMatchHarness.FakePlayer> bravo = harness.addTeam("Bravo", "Cleo");
        ArenaMatchHarness.FakePlayer spectator = harness.arena().addPlayer("Spectator");
        ArenaMatchHarness.FakePlayer farAway = harness.arena().addPlayer("Far");
        farAway.moveTo(harness.arena().at(UnequalTeamVoteHarness.FAR_AWAY, UnequalTeamVoteHarness.FAR_AWAY));
        harness.startTheFinalOfTwoTeams();

        harness.vote(alpha.get(0), alpha.get(0));
        harness.vote(alpha.get(1), alpha.get(0));

        String result = "Alpha fights with Anna";
        assertTrue(alpha.get(1).messages().contains(result));
        assertTrue(bravo.get(0).messages().contains(result));
        assertTrue(spectator.messages().contains(result));
        assertFalse(farAway.messages().contains(result));
        assertTrue(spectator.messages().stream().noneMatch(message -> message.startsWith("Your team")),
                "only the voting team is asked to vote");
    }

    private static String describeScore(Match match, String firstTeamName, String secondTeamName) {
        return match.getScore().getPoints(firstTeamName) + ":" + match.getScore().getPoints(secondTeamName);
    }
}
