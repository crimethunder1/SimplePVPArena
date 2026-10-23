package de.simplepvparena.listener;

import de.simplepvparena.placement.AutoMatchWinCount;
import de.simplepvparena.placement.PlacementMatch;
import de.simplepvparena.placement.PlacementPhase;
import de.simplepvparena.placement.PlacementService;
import de.simplepvparena.placement.PlacementStandings;
import de.simplepvparena.placement.PlacementTeam;
import de.simplepvparena.tournament.AutoMatchState;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoMatchPlacementTest {

    private static final PlacementTeam ALPHA = new PlacementTeam("Alpha", NamedTextColor.WHITE);
    private static final PlacementTeam BRAVO = new PlacementTeam("Bravo", NamedTextColor.WHITE);
    private static final PlacementTeam CHARLIE = new PlacementTeam("Charlie", NamedTextColor.WHITE);

    @Test
    void countsAWinForTheWinnerOfEveryAutoMatch() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie");
        harness.switchOnWithPause(0);

        harness.advanceUntilTheAutoMatchRuns();
        assertEquals(Map.of(), harness.autoMatchService().getWinsByTeamName());
        harness.winMatch("Bravo");
        harness.finishCleaning();
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Charlie");
        harness.finishCleaning();
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Bravo");

        assertEquals(Map.of("Bravo", 2, "Charlie", 1), harness.autoMatchService().getWinsByTeamName());
    }

    @Test
    void aCancelledAutoMatchCountsNoWin() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();
        harness.advanceUntilTheAutoMatchRuns();
        harness.winRound("Alpha");

        harness.matchService().cancelActiveMatch();
        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.COOLDOWN, harness.state());
        assertEquals(Map.of(), harness.autoMatchService().getWinsByTeamName());
    }

    @Test
    void thePlacementShowsTheRunningAutoMatchAndTheTableOfWins() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie");
        PlacementService placementService = placementOf(harness);
        assertEquals(PlacementPhase.NONE, placementService.findStandings().phase());
        harness.switchOnWithPause(0);

        assertEquals(PlacementStandings.autoMatch(Optional.empty(), List.of()), placementService.findStandings());
        harness.advanceUntilTheAutoMatchRuns();
        harness.winRound("Alpha");

        assertEquals(PlacementStandings.autoMatch(Optional.of(new PlacementMatch(ALPHA, 1, BRAVO, 0)), List.of()),
                placementService.findStandings());

        harness.winMatch("Alpha");
        harness.finishCleaning();
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Charlie");

        assertEquals(PlacementStandings.autoMatch(Optional.empty(), List.of(new AutoMatchWinCount(ALPHA, 1),
                new AutoMatchWinCount(CHARLIE, 1))), placementService.findStandings());
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
                harness.autoMatchService()::getWinsByTeamName, harness.teamRegistry()::findTeam);
    }
}
