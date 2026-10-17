package de.simplepvparena.listener;

import de.simplepvparena.match.RoundTimeoutSettings;
import net.kyori.adventure.title.Title;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreTitleAudienceTest {

    private static final RoundTimeoutSettings ONE_SECOND_ROUND = new RoundTimeoutSettings(1, 0, 2.0D, 1.0D);

    @Test
    void aRoundThatLeavesTheMatchOpenShowsTheScoreAndTheWinnerOfTheRound() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        ArenaMatchHarness.FakePlayer outsider = tournament.addPlayerFarOutside("Outsider");
        tournament.startTournament();
        String first = tournament.firstTeamName();
        String second = tournament.secondTeamName();
        tournament.fighterOf(first).moveTo(tournament.arena().at(TournamentAudienceHarness.FAR_AWAY,
                TournamentAudienceHarness.FAR_AWAY));

        tournament.winRound(first);
        tournament.winRound(second);

        List<String> expectedTitles = List.of(
                first + " 1 : 0 " + second + " / " + first + " wins round 1",
                first + " 1 : 1 " + second + " / " + second + " wins round 2");
        assertEquals(expectedTitles, bystander.titleTexts());
        assertEquals(expectedTitles, tournament.fighterOf(first).titleTexts(), "a member far outside sees it");
        assertEquals(expectedTitles, tournament.fighterOf(second).titleTexts());
        assertEquals(List.of(), outsider.titleTexts());
        for (ArenaMatchHarness.FakePlayer fighter : tournament.fighters()) {
            if (!tournament.runningMatch().containsPlayer(fighter.uniqueId())) {
                assertEquals(List.of(), fighter.titleTexts(), fighter.name() + " plays another match");
            }
        }
    }

    @Test
    void aDrawShowsTheScoreAndCountsAsARoundOfItsOwn() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.matchService().setRoundTimeoutSettings(ONE_SECOND_ROUND);
        for (ArenaMatchHarness.FakePlayer fighter : tournament.fighters()) {
            fighter.reactToDamageWith(() -> tournament.matchService().handlePlayerEliminated(fighter.uniqueId()));
        }
        tournament.startTournament();
        String first = tournament.firstTeamName();
        String second = tournament.secondTeamName();

        tournament.arena().runTick();
        tournament.arena().runRepeatingTasks();
        tournament.arena().runTick();
        tournament.winRound(first);

        assertEquals(List.of(
                "Sudden death / Both teams take damage until one of them falls",
                first + " 0 : 0 " + second + " / Round 1 is a draw",
                first + " 1 : 0 " + second + " / " + first + " wins round 2"), bystander.titleTexts());
    }

    @Test
    void theRoundThatDecidesTheMatchShowsOnlyTheTitleOfTheMatch() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        ArenaMatchHarness.FakePlayer outsider = tournament.addPlayerFarOutside("Outsider");
        tournament.startTournament();
        String matchId = tournament.runningMatch().getMatchId();
        String first = tournament.firstTeamName();
        String second = tournament.secondTeamName();

        tournament.winRound(first);
        tournament.winRound(second);
        tournament.winRound(first);

        List<String> expectedTitles = List.of(
                first + " 1 : 0 " + second + " / " + first + " wins round 1",
                first + " 1 : 1 " + second + " / " + second + " wins round 2",
                first + " wins / " + matchId + ", " + first + " 2 : 1 " + second);
        assertEquals(expectedTitles, bystander.titleTexts());
        assertEquals(expectedTitles, tournament.fighterOf(first).titleTexts());
        assertEquals(expectedTitles, tournament.fighterOf(second).titleTexts());
        assertEquals(List.of(), outsider.titleTexts());
    }

    @Test
    void aMatchWonWithoutALostRoundShowsTheRoundTitleOnlyForTheFirstRound() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        String matchId = tournament.runningMatch().getMatchId();
        String first = tournament.firstTeamName();
        String second = tournament.secondTeamName();

        tournament.winMatch(second);

        assertEquals(List.of(
                first + " 0 : 1 " + second + " / " + second + " wins round 1",
                second + " wins / " + matchId + ", " + first + " 0 : 2 " + second), bystander.titleTexts());
    }

    @Test
    void theFinalShowsThatTheWinnerWinsTheFinal() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        tournament.startTournament();
        tournament.winMatch(tournament.firstTeamName());
        tournament.finishCleaning();
        tournament.winMatch(tournament.firstTeamName());
        tournament.finishCleaning();
        tournament.startFinal();
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        String matchId = tournament.runningMatch().getMatchId();
        String first = tournament.firstTeamName();
        String second = tournament.secondTeamName();

        tournament.winMatch(first);

        assertEquals(List.of(
                first + " 1 : 0 " + second + " / " + first + " wins round 1",
                first + " wins the final / " + matchId + ", " + first + " 2 : 0 " + second),
                bystander.titleTexts());
    }

    @Test
    void aResultForcedByAnAdminShowsTheWinnerWithoutAScore() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();
        String matchId = tournament.runningMatch().getMatchId();
        String first = tournament.firstTeamName();
        String second = tournament.secondTeamName();
        tournament.winRound(first);

        assertTrue(tournament.tournamentManager().forceMatchResult(matchId, second).successful());

        assertEquals(List.of(
                first + " 1 : 0 " + second + " / " + first + " wins round 1",
                second + " wins / " + matchId + ", decided by an admin"), bystander.titleTexts());
    }

    @Test
    void theScoreTitlesFadeInForTenTicksStayForFiftyAndFadeOutForTen() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        tournament.startTournament();

        tournament.winMatch(tournament.firstTeamName());

        assertEquals(2, bystander.titles().size());
        for (Title title : bystander.titles()) {
            Title.Times times = title.times();
            assertEquals(Duration.ofMillis(500L), times.fadeIn());
            assertEquals(Duration.ofMillis(2500L), times.stay());
            assertEquals(Duration.ofMillis(500L), times.fadeOut());
        }
    }

    @Test
    void withoutTheArenaAudienceTheTitlesGoToTheTeamsAndTheSpectatorsAsBefore() {
        TournamentAudienceHarness tournament = TournamentAudienceHarness.withoutArenaAudience(
                "Alpha", "Bravo", "Charlie", "Delta");
        ArenaMatchHarness.FakePlayer bystander = tournament.addPlayerInTheArea("Bystander");
        ArenaMatchHarness.FakePlayer spectator = tournament.addPlayerFarOutside("Spectator");
        tournament.startTournament();
        tournament.arena().getSpectatorService().sendToTribune(spectator.player());
        String first = tournament.firstTeamName();
        String second = tournament.secondTeamName();

        tournament.winRound(first);

        List<String> expectedTitles = List.of(first + " 1 : 0 " + second + " / " + first + " wins round 1");
        assertEquals(expectedTitles, tournament.fighterOf(first).titleTexts());
        assertEquals(expectedTitles, spectator.titleTexts());
        assertEquals(List.of(), bystander.titleTexts());
    }
}
