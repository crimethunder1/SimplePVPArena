package de.simplepvparena.listener;

import de.simplepvparena.placement.PlacementMatch;
import de.simplepvparena.placement.PlacementPhase;
import de.simplepvparena.placement.PlacementService;
import de.simplepvparena.placement.PlacementStandings;
import de.simplepvparena.tournament.AutoMatchState;
import de.simplepvparena.tournament.TournamentState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoMatchPlacementTest {

    @Test
    void withoutATournamentThePlacementShowsNothingNotEvenInTheAutoMatchMode() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie");
        PlacementService placementService = placementOf(harness);
        assertEquals(PlacementStandings.none(), placementService.findStandings());
        harness.switchOnWithPause(0);

        assertEquals(PlacementStandings.none(), placementService.findStandings());
        harness.advanceUntilTheAutoMatchRuns();
        harness.winRound("Alpha");

        assertEquals(PlacementStandings.none(), placementService.findStandings(), "not even the running auto match");

        harness.winMatch("Alpha");
        harness.finishCleaning();

        assertEquals(PlacementStandings.none(), placementService.findStandings());
    }

    @Test
    void theTournamentComesFirstWhileItIsInProgress() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie", "Delta");
        PlacementService placementService = placementOf(harness);
        harness.switchOn();

        harness.startTournament();

        PlacementStandings standings = placementService.findStandings();
        assertEquals(PlacementPhase.TOURNAMENT_RUNNING, standings.phase());
        assertEquals(1, standings.roundNumber());
        assertEquals(2, standings.roundCount());
        assertEquals(4, standings.remainingTeams().size());
        assertEquals(harness.matchService().getActiveMatch().map(PlacementMatch::of), standings.runningMatch());
    }

    @Test
    void aPausedTournamentIsShownInTheAutoMatchMode() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie", "Delta");
        PlacementService placementService = placementOf(harness);
        harness.switchOn();
        harness.startTournament();

        assertTrue(harness.tournamentManager().togglePause().successful());

        assertEquals(PlacementPhase.TOURNAMENT_PAUSED, placementService.findStandings().phase());
    }

    @Test
    void anOverTournamentStaysWithoutTheAutoMatchMode() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        PlacementService placementService = placementOf(harness);

        String champion = finishTheTournament(harness);

        PlacementStandings standings = placementService.findStandings();
        assertEquals(PlacementPhase.TOURNAMENT_OVER, standings.phase());
        assertEquals(champion, standings.ranks().get(0).team().name());
    }

    @Test
    void anOverTournamentStaysWhenTheAutoMatchModeIsSwitchedOnAndWhileAnAutoMatchRuns() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        PlacementService placementService = placementOf(harness);
        String champion = finishTheTournament(harness);
        PlacementStandings overTournament = placementService.findStandings();

        harness.switchOnWithPause(0);

        assertEquals(overTournament, placementService.findStandings());
        harness.advanceUntilTheAutoMatchRuns();
        harness.winRound("Alpha");
        PlacementStandings standings = placementService.findStandings();
        assertEquals(PlacementPhase.TOURNAMENT_OVER, standings.phase());
        assertEquals(Optional.empty(), standings.runningMatch(), "the auto match is not part of the placement");
        assertEquals(champion, standings.ranks().get(0).team().name());

        harness.winMatch("Alpha");
        harness.finishCleaning();

        assertEquals(AutoMatchState.COOLDOWN, harness.state());
        assertEquals(overTournament, placementService.findStandings());
    }

    private static String finishTheTournament(AutoMatchHarness harness) {
        harness.startTournament();
        assertTrue(harness.tournamentManager().startFinalMatch().successful());
        assertTrue(harness.tournamentManager().getKitChoiceService().choose(AutoMatchHarness.DEFAULT_KIT_NAME,
                "Admin"));
        List<String> teamNames = harness.runningTeamNames();
        harness.winMatch(teamNames.get(0));
        harness.finishCleaning();
        assertEquals(TournamentState.FINISHED, harness.tournamentManager().getState());
        return teamNames.get(0);
    }

    private static PlacementService placementOf(AutoMatchHarness harness) {
        return new PlacementService(harness.tournamentManager()::getState, harness.tournamentManager()::getBracket,
                harness.matchService()::getActiveMatch, harness.teamRegistry()::findTeam);
    }
}
