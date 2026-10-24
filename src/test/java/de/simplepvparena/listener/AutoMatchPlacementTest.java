package de.simplepvparena.listener;

import de.simplepvparena.placement.PlacementMatch;
import de.simplepvparena.placement.PlacementPhase;
import de.simplepvparena.placement.PlacementService;
import de.simplepvparena.placement.PlacementStandings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoMatchPlacementTest {

    @Test
    void thePlacementShowsNothingInTheAutoMatchMode() {
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

    private static PlacementService placementOf(AutoMatchHarness harness) {
        return new PlacementService(harness.tournamentManager()::getState, harness.tournamentManager()::getBracket,
                harness.matchService()::getActiveMatch, harness.autoMatchService()::isEnabled,
                harness.teamRegistry()::findTeam);
    }
}
