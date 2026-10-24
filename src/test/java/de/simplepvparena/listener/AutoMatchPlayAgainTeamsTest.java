package de.simplepvparena.listener;

import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.tournament.AutoMatchSettings;
import de.simplepvparena.tournament.AutoMatchState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoMatchPlayAgainTeamsTest {

    @Test
    void bothColouredTeamsAreDissolvedAndTheirPlayersUnregisteredAtTheEndOfTheMatch() {
        AutoMatchHarness harness = colouredTeams();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();

        harness.winMatch("Red");

        assertEquals(List.of(), harness.teamRegistry().getTeamNames());
        assertFalse(harness.isRegistered(harness.fighterOf("Red")));
        assertFalse(harness.isRegistered(harness.fighterOf("Blue")));
        assertEquals(1, harness.teamSaveCount(), "the teams are saved once after the dissolution");
        assertEquals(AutoMatchState.COOLDOWN, harness.state());
        assertTrue(harness.isCleaning(), "the cleaning after the match starts as before");
    }

    @Test
    void aColouredTeamStaysWithAMemberWhoDidNotPlayInTheMatch() {
        AutoMatchHarness harness = colouredTeams();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        ArenaMatchHarness.FakePlayer newcomer = harness.addPlayerFarOutside("Newcomer");
        harness.teamRegistry().addMember("Red", new TeamMember(newcomer.uniqueId(), newcomer.name()));
        harness.register(newcomer);

        harness.winMatch("Blue");

        Team red = harness.teamRegistry().findTeam("Red").orElseThrow();
        assertEquals(List.of(newcomer.uniqueId()), List.copyOf(red.getMemberUniqueIds()));
        assertEquals(TeamColour.RED, red.getColour().orElseThrow());
        assertTrue(harness.isRegistered(newcomer), "the member who did not play stays registered");
        assertFalse(harness.isRegistered(harness.fighterOf("Red")));
        assertTrue(harness.teamRegistry().findTeam("Blue").isEmpty());
    }

    @Test
    void aTeamWithoutAColourStaysWithItsMembersWhileItsPlayersAreUnregistered() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.registerEveryFighter();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();

        harness.winMatch("Alpha");

        assertEquals(List.of("Alpha", "Bravo"), harness.teamRegistry().getTeamNames());
        assertEquals("Alpha", harness.teamOf(harness.fighterOf("Alpha")).orElseThrow().getTeamName());
        assertEquals("Bravo", harness.teamOf(harness.fighterOf("Bravo")).orElseThrow().getTeamName());
        assertFalse(harness.isRegistered(harness.fighterOf("Alpha")));
        assertFalse(harness.isRegistered(harness.fighterOf("Bravo")));
        assertEquals(0, harness.teamSaveCount(), "no team changed");
        assertTrue(harness.playAgainService().isWaitingForPlayAgain(harness.teamRegistry().findTeam("Alpha")
                .orElseThrow()));
    }

    @Test
    void twoTeamsWithoutAColourAreNotPairedAgainRightAfterTheCleaningAndThePause() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Alpha");
        harness.finishCleaning();
        harness.forgetReceivedTexts();

        harness.advanceSeconds(AutoMatchHarness.PAUSE_SECONDS + 20);

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertEquals(List.of(), harness.messagesOf("Alpha"));
        assertTrue(harness.autoMatchService().findPairing().isEmpty());
    }

    @Test
    void teamsWithoutAColourArePairedAgainOnceThePlayAgainSecondsAreOver() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Alpha");
        harness.finishCleaning();
        harness.forgetReceivedTexts();

        harness.advanceSeconds(AutoMatchHarness.PLAY_AGAIN_SECONDS - 1);

        assertEquals(AutoMatchState.IDLE, harness.state());

        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
        assertEquals(List.of(AutoMatchHarness.announcement("Alpha", "Bravo")), harness.messagesOf("Alpha"));
    }

    @Test
    void aTeamThatHasNotPlayedWaitsForANewTeamInsteadOfTheTeamsOfTheLastMatch() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie");
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        assertEquals(List.of("Alpha", "Bravo"), harness.runningTeamNames());
        harness.winMatch("Alpha");
        harness.finishCleaning();

        harness.advanceSeconds(AutoMatchHarness.PAUSE_SECONDS + 5);

        assertEquals(AutoMatchState.IDLE, harness.state(), "Charlie does not play Alpha or Bravo again");

        harness.addColouredTeam("Red", TeamColour.RED);
        harness.advanceUntilTheAutoMatchRuns();

        assertEquals(List.of("Charlie", "Red"), harness.runningTeamNames());
    }

    @Test
    void newColouredTeamsOfTheTeamChoiceArePairedAfterThePause() {
        AutoMatchHarness harness = colouredTeams();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Red");
        harness.finishCleaning();
        ArenaMatchHarness.FakePlayer redFighter = harness.fighterOf("Red");
        ArenaMatchHarness.FakePlayer blueFighter = harness.fighterOf("Blue");
        harness.teamRegistry().registerTeam("Green", List.of(new TeamMember(redFighter.uniqueId(),
                redFighter.name())), TeamColour.GREEN);
        harness.teamRegistry().registerTeam("Yellow", List.of(new TeamMember(blueFighter.uniqueId(),
                blueFighter.name())), TeamColour.YELLOW);
        harness.forgetReceivedTexts();

        harness.advanceSeconds(AutoMatchHarness.PAUSE_SECONDS);

        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
        assertEquals(List.of(AutoMatchHarness.announcement("Green", "Yellow")), redFighter.messages());
    }

    @Test
    void withZeroSecondsTheTeamsStayRegisteredAndPlayAgainAsBefore() {
        AutoMatchHarness harness = colouredTeams();
        harness.switchOn();
        harness.advanceUntilTheAutoMatchRuns();

        harness.winMatch("Red");
        harness.finishCleaning();

        assertEquals(List.of("Red", "Blue"), harness.teamRegistry().getTeamNames());
        assertTrue(harness.isRegistered(harness.fighterOf("Red")));
        assertTrue(harness.isRegistered(harness.fighterOf("Blue")));
        assertEquals(0, harness.teamSaveCount());

        harness.advanceSeconds(AutoMatchHarness.PAUSE_SECONDS);

        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
    }

    @Test
    void aReloadDuringTheMatchCountsFromItsEnd() {
        AutoMatchHarness harness = colouredTeams();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();

        harness.switchOn();
        harness.winMatch("Red");

        assertEquals(List.of("Red", "Blue"), harness.teamRegistry().getTeamNames(),
                "the value at the end of the match counts");

        harness.finishCleaning();
        harness.advanceSeconds(AutoMatchHarness.PAUSE_SECONDS);
        harness.advanceSeconds(AutoMatchHarness.START_DELAY_SECONDS);
        assertEquals(AutoMatchState.RUNNING, harness.state());
        harness.autoMatchService().applySettings(new AutoMatchSettings(true, AutoMatchHarness.START_DELAY_SECONDS,
                AutoMatchHarness.PAUSE_SECONDS, AutoMatchHarness.PLAY_AGAIN_SECONDS));

        harness.winMatch("Blue");

        assertEquals(List.of(), harness.teamRegistry().getTeamNames());
    }

    @Test
    void aRenamedTeamWithoutAColourKeepsWaiting() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Alpha");
        harness.finishCleaning();

        harness.teamRegistry().renameTeam("Alpha", "Aleph");
        harness.advanceSeconds(AutoMatchHarness.PAUSE_SECONDS + 5);

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertTrue(harness.playAgainService().isWaitingForPlayAgain(harness.teamRegistry().findTeam("Aleph")
                .orElseThrow()));
    }

    @Test
    void aColouredTeamIsDissolvedAndATeamWithoutAColourWaitsInTheSameMatch() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha");
        harness.addColouredTeam("Red", TeamColour.RED);
        harness.registerEveryFighter();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();

        harness.winMatch("Red");

        assertEquals(List.of("Alpha"), harness.teamRegistry().getTeamNames());
        assertFalse(harness.isRegistered(harness.fighterOf("Alpha")));
        assertFalse(harness.isRegistered(harness.fighterOf("Red")));
        assertTrue(harness.playAgainService().isWaitingForPlayAgain(harness.teamRegistry().findTeam("Alpha")
                .orElseThrow()));
    }

    private static AutoMatchHarness colouredTeams() {
        AutoMatchHarness harness = new AutoMatchHarness();
        harness.addColouredTeam("Red", TeamColour.RED);
        harness.addColouredTeam("Blue", TeamColour.BLUE);
        harness.registerEveryFighter();
        return harness;
    }
}
