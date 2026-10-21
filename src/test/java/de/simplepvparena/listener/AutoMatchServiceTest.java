package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.team.Team;
import de.simplepvparena.tournament.AutoMatchState;
import de.simplepvparena.tournament.TournamentOperationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoMatchServiceTest {

    private static final String TEAM_OFFLINE = "Auto match cancelled: a team has no player online any more";
    private static final String MODE_SWITCHED_OFF = "Auto match cancelled: the mode was switched off";
    private static final String ARENA_NOT_READY = "Auto match cancelled: the arena is not ready";

    @Test
    void doesNothingAndSchedulesNoTaskWhileTheModeIsOff() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");

        harness.advanceSeconds(60);

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertEquals(0, harness.arena().getPendingTaskCount());
        assertEquals(List.of(), harness.messagesOf("Alpha"));
    }

    @Test
    void announcesNothingWithOnlyOneTeam() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha");
        harness.switchOn();

        harness.advanceSeconds(60);

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertEquals(List.of(), harness.messagesOf("Alpha"));
    }

    @Test
    void announcesNothingUntilTwoTeamsHaveAPlayerOnline() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.fighterOf("Bravo").setOnline(false);
        harness.switchOn();

        harness.advanceSeconds(60);

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertEquals(List.of(), harness.messagesOf("Alpha"));

        harness.fighterOf("Bravo").setOnline(true);
        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
    }

    @Test
    void announcesThePairingWithTheDelayToBothTeamsAndTheArea() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        ArenaMatchHarness.FakePlayer bystander = harness.addPlayerInTheArea("Bystander");
        ArenaMatchHarness.FakePlayer outsider = harness.addPlayerFarOutside("Outsider");
        harness.switchOn();

        harness.advanceSeconds(1);

        String announcement = "Auto match: Alpha against Bravo begins in 10 seconds";
        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
        assertEquals(List.of(announcement), harness.messagesOf("Alpha"));
        assertEquals(List.of(announcement), harness.messagesOf("Bravo"));
        assertEquals(List.of(announcement), bystander.messages());
        assertEquals(List.of(), outsider.messages());
        assertFalse(harness.matchService().hasActiveMatch(), "the match waits for the delay");
    }

    @Test
    void anAdminInATeamCountsLikeEveryOtherPlayer() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.fighterOf("Alpha").makeOperator();
        harness.switchOn();

        harness.advanceSeconds(1);

        assertEquals(List.of(AutoMatchHarness.announcement("Alpha", "Bravo")), harness.messagesOf("Alpha"));
    }

    @Test
    void theMatchStartsAfterTheDelayWithTheDefaultKitAndWithoutAKitChoice() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.kitLibrary().activate(AutoMatchHarness.OTHER_KIT_NAME);
        harness.switchOn();
        harness.advanceSeconds(1);

        harness.advanceSeconds(AutoMatchHarness.START_DELAY_SECONDS - 1);

        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
        assertFalse(harness.matchService().hasActiveMatch());

        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.RUNNING, harness.state());
        Match match = harness.runningMatch();
        assertEquals("auto-1", match.getMatchId());
        assertTrue(match.isAutoMatch());
        assertFalse(match.isFinalMatch());
        assertEquals(List.of("Alpha", "Bravo"), harness.runningTeamNames());
        assertEquals(harness.kitDefinitionOf(AutoMatchHarness.DEFAULT_KIT_NAME), harness.activeKitDefinition());
        assertEquals(List.of(), harness.shownKitChoices());
        assertFalse(harness.tournamentManager().isKitChoicePending());
        assertTrue(harness.matchService().isActiveFighter(harness.fighterOf("Alpha").uniqueId()));
        assertTrue(harness.messagesOf("Bravo").stream()
                        .anyMatch(message -> message.startsWith("Round 1 of match auto-1 has started")),
                harness.messagesOf("Bravo").toString());
    }

    @Test
    void theEndOfTheMatchSetsTheDefaultKitCleansTheArenaAndPausesBeforeTheNextAnnouncement() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();
        harness.advanceUntilTheAutoMatchRuns();
        harness.kitLibrary().activate(AutoMatchHarness.OTHER_KIT_NAME);
        harness.forgetReceivedTexts();

        harness.winMatch("Alpha");

        assertEquals(AutoMatchState.COOLDOWN, harness.state());
        assertFalse(harness.matchService().hasActiveMatch());
        assertTrue(harness.isCleaning());
        assertEquals(1, harness.startedCleaningCount());
        assertEquals(harness.kitDefinitionOf(AutoMatchHarness.DEFAULT_KIT_NAME), harness.activeKitDefinition());
        assertTrue(harness.messagesOf("Bravo").stream()
                        .anyMatch(message -> message.startsWith("Match auto-1 is over, Alpha wins")),
                harness.messagesOf("Bravo").toString());
        assertTrue(harness.fighterOf("Bravo").titleTexts().stream()
                        .anyMatch(title -> title.equals("Alpha wins / auto-1, Alpha 2 : 0 Bravo")),
                harness.fighterOf("Bravo").titleTexts().toString());

        harness.advanceSeconds(60);

        assertEquals(AutoMatchState.COOLDOWN, harness.state(), "nothing is announced while the arena is cleaned");
        harness.forgetReceivedTexts();

        harness.finishCleaning();
        harness.advanceSeconds(AutoMatchHarness.PAUSE_SECONDS - 1);

        assertEquals(AutoMatchState.COOLDOWN, harness.state());
        assertEquals(List.of(), harness.messagesOf("Alpha"));

        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
        assertEquals(List.of(AutoMatchHarness.announcement("Alpha", "Bravo")), harness.messagesOf("Alpha"));
    }

    @Test
    void theMatchIdentifiersCountUp() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOnWithPause(0);
        harness.advanceUntilTheAutoMatchRuns();
        assertEquals("auto-1", harness.runningMatch().getMatchId());
        harness.winMatch("Bravo");
        harness.finishCleaning();

        harness.advanceUntilTheAutoMatchRuns();

        assertEquals("auto-2", harness.runningMatch().getMatchId());
    }

    @Test
    void withoutACleaningTheAdminsAreToldAndThePauseStartsAtOnce() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.skipEveryCleaning();
        harness.switchOn();
        harness.advanceUntilTheAutoMatchRuns();

        harness.winMatch("Alpha");

        assertEquals(List.of("The arena was not cleaned after match auto-1: "
                + AutoMatchHarness.SKIPPED_CLEANING_REASON + ". The next match starts in the arena as it is now"),
                harness.adminMessages());
        assertFalse(harness.isCleaning());
        assertEquals(AutoMatchState.COOLDOWN, harness.state());

        harness.advanceSeconds(AutoMatchHarness.PAUSE_SECONDS - 1);

        assertEquals(AutoMatchState.COOLDOWN, harness.state());

        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
    }

    @Test
    void ofThreeTeamsTheTwoThatWaitedLongestPlayNext() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie");
        harness.switchOnWithPause(0);

        assertEquals(List.of("Alpha", "Bravo"), playNextAutoMatch(harness));
        assertEquals(List.of("Charlie", "Alpha"), playNextAutoMatch(harness));
        assertEquals(List.of("Bravo", "Alpha"), playNextAutoMatch(harness));
        assertEquals(List.of("Charlie", "Alpha"), playNextAutoMatch(harness));
    }

    @Test
    void ofFourTeamsThePairsTakeTurns() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie", "Delta");
        harness.switchOnWithPause(0);

        assertEquals(List.of("Alpha", "Bravo"), playNextAutoMatch(harness));
        assertEquals(List.of("Charlie", "Delta"), playNextAutoMatch(harness));
        assertEquals(List.of("Alpha", "Bravo"), playNextAutoMatch(harness));
        assertEquals(List.of("Charlie", "Delta"), playNextAutoMatch(harness));
    }

    @Test
    void theAnnouncementNamesTheTeamsInTheOrderOfTheirSides() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie");
        harness.switchOnWithPause(0);
        playNextAutoMatch(harness);
        harness.forgetReceivedTexts();

        harness.advanceSeconds(1);

        assertEquals(List.of(AutoMatchHarness.announcement("Charlie", "Alpha")), harness.messagesOf("Charlie"));
        harness.advanceSeconds(AutoMatchHarness.START_DELAY_SECONDS);
        assertEquals(List.of("Charlie", "Alpha"), harness.runningTeamNames());
        assertEquals("Charlie", harness.runningMatch().getFirstTeam().getTeamName());
    }

    @Test
    void aTeamWithoutAPlayerOnlineCancelsTheAnnouncementAndThePauseKeepsItFromComingBackAtOnce() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        ArenaMatchHarness.FakePlayer bystander = harness.addPlayerInTheArea("Bystander");
        harness.switchOn();
        harness.advanceSeconds(1);
        harness.forgetReceivedTexts();
        bystander.forgetReceivedTexts();

        harness.fighterOf("Bravo").setOnline(false);
        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertEquals(List.of(TEAM_OFFLINE), harness.messagesOf("Alpha"));
        assertEquals(List.of(TEAM_OFFLINE), bystander.messages());
        assertFalse(harness.matchService().hasActiveMatch());

        harness.fighterOf("Bravo").setOnline(true);
        harness.advanceSeconds(AutoMatchHarness.PAUSE_SECONDS - 1);

        assertEquals(AutoMatchState.IDLE, harness.state());

        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
    }

    @Test
    void switchingTheModeOffCancelsTheAnnouncementAtOnceAndStopsTheTimer() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();
        harness.advanceSeconds(1);
        harness.forgetReceivedTexts();

        harness.switchOff();

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertEquals(List.of(MODE_SWITCHED_OFF), harness.messagesOf("Bravo"));
        assertEquals(0, harness.arena().getPendingTaskCount());

        harness.advanceSeconds(120);

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertEquals(List.of(MODE_SWITCHED_OFF), harness.messagesOf("Bravo"));
    }

    @Test
    void anArenaThatIsNoLongerReadyCancelsTheAnnouncement() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();
        harness.advanceSeconds(1);
        harness.forgetReceivedTexts();

        harness.arena().forgetTribuneSpawn();
        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertEquals(List.of(ARENA_NOT_READY), harness.messagesOf("Alpha"));
        harness.advanceSeconds(120);
        assertEquals(AutoMatchState.IDLE, harness.state(), "no announcement while the arena is not ready");
    }

    @Test
    void anAdminWhoCancelsTheMatchEndsItAndTheArenaIsCleaned() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie");
        harness.switchOnWithPause(0);
        harness.advanceUntilTheAutoMatchRuns();
        harness.kitLibrary().activate(AutoMatchHarness.OTHER_KIT_NAME);

        harness.matchService().cancelActiveMatch();
        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.COOLDOWN, harness.state());
        assertTrue(harness.isCleaning());
        assertEquals(harness.kitDefinitionOf(AutoMatchHarness.DEFAULT_KIT_NAME), harness.activeKitDefinition());

        harness.finishCleaning();
        harness.advanceUntilTheAutoMatchRuns();

        assertEquals(List.of("Charlie", "Alpha"), harness.runningTeamNames(),
                "the teams of the cancelled match count as having played");
    }

    @Test
    void aForcedResultEndsTheMatchLikeAPlayedOne() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();
        harness.advanceUntilTheAutoMatchRuns();

        assertTrue(harness.matchService().forceResult("auto-1", "Bravo"));

        assertEquals(AutoMatchState.COOLDOWN, harness.state());
        assertTrue(harness.isCleaning());
    }

    @Test
    void switchingTheModeOffDuringTheMatchLetsItEndAndAnnouncesNothingAfterwards() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();
        harness.advanceUntilTheAutoMatchRuns();

        harness.switchOff();
        harness.advanceSeconds(5);

        assertEquals(AutoMatchState.RUNNING, harness.state());
        assertEquals("auto-1", harness.runningMatchId().orElseThrow());

        harness.winMatch("Alpha");

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertTrue(harness.isCleaning(), "the arena is still cleaned after the last auto match");
        harness.finishCleaning();
        harness.forgetReceivedTexts();
        harness.advanceSeconds(120);
        assertEquals(AutoMatchState.IDLE, harness.state());
        assertEquals(List.of(), harness.messagesOf("Alpha"));
        assertEquals(0, harness.arena().getPendingTaskCount());
    }

    @Test
    void switchingTheModeOffDuringThePauseEndsIt() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Alpha");
        harness.finishCleaning();

        harness.switchOff();

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertEquals(0, harness.arena().getPendingTaskCount());
    }

    @Test
    void aRunningOrPausedTournamentHasPriorityAndItsEndLetsTheAutoMatchesContinue() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.startTournament();
        harness.switchOn();

        harness.advanceSeconds(60);

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertTrue(harness.autoMatchService().isEnabled(), "the mode stays on");

        assertTrue(harness.tournamentManager().togglePause().successful());
        harness.advanceSeconds(60);

        assertEquals(AutoMatchState.IDLE, harness.state());

        TournamentOperationResult cancellation = harness.tournamentManager().cancelTournament();
        assertTrue(cancellation.successful(), cancellation.message());
        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
    }

    @Test
    void afterAFinishedTournamentTheAutoMatchesWaitForItsCleaningAndContinue() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.startTournament();
        harness.switchOn();
        TournamentOperationResult finalStart = harness.tournamentManager().startFinalMatch();
        assertTrue(finalStart.successful(), finalStart.message());
        assertTrue(harness.tournamentManager().getKitChoiceService().choose(AutoMatchHarness.DEFAULT_KIT_NAME,
                "admin"));

        harness.winMatch("Alpha");
        harness.advanceSeconds(30);

        assertFalse(harness.tournamentManager().isTournamentInProgress());
        assertTrue(harness.tournamentManager().isWaitingForArenaCleaning());
        assertEquals(AutoMatchState.IDLE, harness.state());

        harness.finishCleaning();
        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
    }

    @Test
    void aPlayerWhoIsFightingInAMatchIsNotFree() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie", "Delta");
        Team alpha = harness.teamRegistry().findTeam("Alpha").orElseThrow();
        Team bravo = harness.teamRegistry().findTeam("Bravo").orElseThrow();
        harness.matchService().startMatch(Match.regularMatch("M1", alpha, bravo,
                harness.arena().getArenaRegistry().getArena()));
        harness.switchOn();

        assertEquals(List.of("Charlie", "Delta"), harness.autoMatchService().findPairing().orElseThrow().stream()
                .map(Team::getTeamName).toList());

        harness.advanceSeconds(30);

        assertEquals(AutoMatchState.IDLE, harness.state(), "no auto match while another match runs");
    }

    @Test
    void stoppingTheServiceRemovesItsTimer() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();
        harness.advanceSeconds(1);
        harness.forgetReceivedTexts();

        harness.autoMatchService().stop();

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertEquals(0, harness.arena().getPendingTaskCount());
        harness.advanceSeconds(60);
        assertEquals(List.of(), harness.messagesOf("Alpha"));
        assertFalse(harness.matchService().hasActiveMatch());
    }

    private static List<String> playNextAutoMatch(AutoMatchHarness harness) {
        harness.advanceUntilTheAutoMatchRuns();
        List<String> teamNames = harness.runningTeamNames();
        harness.winMatch(teamNames.get(0));
        harness.finishCleaning();
        return teamNames;
    }
}
