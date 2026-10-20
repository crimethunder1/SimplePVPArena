package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchAnnouncementAudienceTest {

    @Test
    void theMatchIsAnnouncedOnceToBothTeamsAndToThePlayersInTheArea() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        ArenaMatchHarness.FakePlayer outsider = tournament.addPlayerFarOutside("Outsider");

        tournament.startTournament();

        Match match = tournament.runningMatch();
        String announcement = "Match " + match.getMatchId() + ": " + match.getFirstTeam().getTeamName()
                + " against " + match.getSecondTeam().getTeamName();
        assertEquals(1, TournamentAudienceHarness.countMessages(
                tournament.fighterOf(match.getFirstTeam().getTeamName()), announcement));
        assertEquals(1, TournamentAudienceHarness.countMessages(
                tournament.fighterOf(match.getSecondTeam().getTeamName()), announcement));
        assertEquals(1, TournamentAudienceHarness.countMessages(bystander, announcement));
        assertEquals(0, TournamentAudienceHarness.countMessages(outsider, announcement));
        assertFalse(tournament.broadcasts().contains(announcement), "the announcement is no broadcast any more");
    }

    @Test
    void theMatchIsNotAnnouncedToTheTeamsOfOtherMatchesOutsideOfTheArea() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");

        tournament.startTournament();

        Match match = tournament.runningMatch();
        for (ArenaMatchHarness.FakePlayer fighter : tournament.fighters()) {
            boolean playsTheMatch = match.containsPlayer(fighter.uniqueId());
            boolean announced = fighter.messages().stream()
                    .anyMatch(message -> message.startsWith("Match " + match.getMatchId() + ": "));
            assertEquals(playsTheMatch, announced, fighter.name());
        }
    }

    @Test
    void theSecondAnnouncementOfTheMatchServiceIsGone() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");

        tournament.startTournament();

        Match match = tournament.runningMatch();
        assertTrue(bystander.messages().stream().noneMatch(message -> message.contains(" starts: ")),
                bystander.messages().toString());
        assertTrue(tournament.fighterOf(match.getFirstTeam().getTeamName()).messages().stream()
                .noneMatch(message -> message.contains(" starts: ")));
    }

    @Test
    void aWalkoverIsAnnouncedToTheRemainingTeamAndToThePlayersInTheArea() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        ArenaMatchHarness.FakePlayer outsider = tournament.addPlayerFarOutside("Outsider");
        tournament.startTournament();
        String secondMatchId = "round1-match2";
        List<String> secondMatchTeams = List.of(
                tournament.tournamentManager().getBracket().orElseThrow().findMatch(secondMatchId).orElseThrow()
                        .getFirstTeamName().orElseThrow(),
                tournament.tournamentManager().getBracket().orElseThrow().findMatch(secondMatchId).orElseThrow()
                        .getSecondTeamName().orElseThrow());
        String removedTeamName = secondMatchTeams.get(1);
        String advancingTeamName = secondMatchTeams.get(0);
        tournament.teamRegistry().removeTeam(removedTeamName);

        tournament.winMatch(tournament.firstTeamName());
        tournament.finishCleaning();

        String walkover = advancingTeamName + " advances in match " + secondMatchId
                + " because the opposing team is no longer registered";
        assertEquals(1, TournamentAudienceHarness.countMessages(tournament.fighterOf(advancingTeamName), walkover));
        assertEquals(1, TournamentAudienceHarness.countMessages(bystander, walkover));
        assertEquals(0, TournamentAudienceHarness.countMessages(outsider, walkover));
        assertEquals(0, TournamentAudienceHarness.countMessages(tournament.fighterOf(removedTeamName), walkover));
        assertFalse(tournament.broadcasts().contains(walkover), tournament.broadcasts().toString());
    }

    @Test
    void theStartPauseResumeAndCancellationOfTheTournamentStayServerWide() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");

        tournament.startTournament();
        tournament.tournamentManager().togglePause();
        tournament.tournamentManager().togglePause();
        tournament.tournamentManager().cancelTournament();

        assertEquals(List.of("The tournament has started with 4 teams in 2 rounds",
                "The tournament is paused, no further match is started until it is resumed\n"
                        + "The round timer keeps its remaining time",
                "The tournament continues",
                "The tournament has been cancelled"), tournament.broadcasts());
    }

    @Test
    void theTeamsWithAByeStayServerWide() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie");

        assertTrue(tournament.tournamentManager().startTournament(true).successful());

        String byeTeamName = tournament.tournamentManager().getBracket().orElseThrow()
                .getTeamNamesWithFirstRoundBye().iterator().next();
        assertEquals(List.of("The tournament has started with 3 teams in 2 rounds",
                "Team advancing without a first round opponent: " + byeTeamName), tournament.broadcasts());
    }

    @Test
    void theEndOfTheTournamentStaysServerWide() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        tournament.startTournament();
        tournament.winMatch(tournament.firstTeamName());
        tournament.finishCleaning();
        tournament.winMatch(tournament.firstTeamName());
        tournament.finishCleaning();
        tournament.startFinal();
        String championTeamName = tournament.firstTeamName();

        tournament.winMatch(championTeamName);

        assertEquals("The tournament is over, the winner is " + championTeamName,
                tournament.broadcasts().get(tournament.broadcasts().size() - 1));
        assertTrue(tournament.broadcasts().stream().noneMatch(message -> message.startsWith("Match ")),
                tournament.broadcasts().toString());
    }

    @Test
    void withoutTheArenaAudienceTheMatchIsStillAnnouncedOnceToTheWholeServer() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withoutArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");

        tournament.startTournament();

        Match match = tournament.runningMatch();
        String announcement = "Match " + match.getMatchId() + ": " + match.getFirstTeam().getTeamName()
                + " against " + match.getSecondTeam().getTeamName();
        assertEquals(1, tournament.broadcasts().stream().filter(announcement::equals).count());
        assertTrue(tournament.fighterOf(match.getFirstTeam().getTeamName()).messages().stream()
                .noneMatch(message -> message.contains(" starts: ")));
    }
}
