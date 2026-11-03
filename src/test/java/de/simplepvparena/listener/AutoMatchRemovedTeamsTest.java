package de.simplepvparena.listener;

import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.tournament.AutoMatchState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoMatchRemovedTeamsTest {

    @Test
    void aNewTeamNamedLikeADissolvedTeamIsPairedAsIfItHadNeverPlayed() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha");
        harness.addColouredTeam("Red", TeamColour.RED);
        harness.registerEveryFighter();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        assertEquals(List.of("Alpha", "Red"), harness.runningTeamNames());
        harness.winMatch("Alpha");
        harness.finishCleaning();
        harness.advanceSeconds(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        assertTrue(harness.teamRegistry().findTeam("Red").isEmpty(), "Red is dissolved");
        assertFalse(harness.playAgainService().isWaitingForPlayAgain(teamNamed(harness, "Alpha")));
        assertEquals(AutoMatchState.IDLE, harness.state());

        harness.addColouredTeam("Blue", TeamColour.BLUE);
        harness.addColouredTeam("Red", TeamColour.RED);

        assertEquals(List.of("Blue", "Red"), pairingOf(harness), "Alpha has played, Blue and the new Red have not");
    }

    @Test
    void aRenamedTeamKeepsItsWaitingTimeAndANewTeamWithItsFormerNameHasNeverPlayed() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie");
        harness.switchOnWithPause(0);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Bravo");
        harness.finishCleaning();

        harness.teamRegistry().renameTeam("Bravo", "Dragons");
        harness.addTeam("Bravo");

        assertEquals(List.of("Alpha", "Dragons", "Charlie", "Bravo"), harness.teamRegistry().getTeamNames());
        assertEquals(List.of("Charlie", "Bravo"), pairingOf(harness));
    }

    @Test
    void aTeamRemovedByAnAdminLosesItsWaitingTime() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie");
        harness.switchOnWithPause(0);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Bravo");
        harness.finishCleaning();

        assertTrue(harness.teamRegistry().removeTeam("Bravo"));
        harness.addTeam("Bravo");

        assertEquals(List.of("Charlie", "Bravo"), pairingOf(harness), "Alpha has played, the new Bravo has not");
    }

    @Test
    void aNewTeamWithoutAColourNamedLikeARemovedWaitingTeamDoesNotWait() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Alpha");
        harness.finishCleaning();

        assertTrue(harness.teamRegistry().removeTeam("Alpha"));
        harness.addTeam("Alpha");

        assertFalse(harness.playAgainService().isWaitingForPlayAgain(teamNamed(harness, "Alpha")));
        assertTrue(harness.playAgainService().isWaitingForPlayAgain(teamNamed(harness, "Bravo")));
    }

    @Test
    void clearingTheTeamsForgetsTheWaitingTimeOfEveryTeam() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie");
        harness.switchOnWithPause(0);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Bravo");
        harness.finishCleaning();

        harness.teamRegistry().clear();
        harness.addTeam("Alpha");
        harness.addTeam("Bravo");
        harness.addTeam("Charlie");

        assertEquals(List.of("Alpha", "Bravo"), pairingOf(harness), "none of the new teams has played");
    }

    @Test
    void clearingTheTeamsEndsTheWaitOfEveryTeamWithoutAColour() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Alpha");
        harness.finishCleaning();

        harness.teamRegistry().clear();
        harness.addTeam("Alpha");
        harness.addTeam("Bravo");

        assertFalse(harness.playAgainService().isWaitingForPlayAgain(teamNamed(harness, "Alpha")));
        assertFalse(harness.playAgainService().isWaitingForPlayAgain(teamNamed(harness, "Bravo")));
        assertEquals(List.of("Alpha", "Bravo"), pairingOf(harness));
    }

    private static List<String> pairingOf(AutoMatchHarness harness) {
        return harness.autoMatchService().findPairing().orElseThrow().stream()
                .map(Team::getTeamName)
                .toList();
    }

    private static Team teamNamed(AutoMatchHarness harness, String teamName) {
        return harness.teamRegistry().findTeam(teamName).orElseThrow();
    }
}
