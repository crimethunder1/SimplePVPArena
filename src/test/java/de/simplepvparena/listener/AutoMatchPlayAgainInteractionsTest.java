package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.UnequalTeamVoteService;
import de.simplepvparena.registration.RandomTeamFormation;
import de.simplepvparena.team.TeamColour;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.tournament.AutoMatchState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoMatchPlayAgainInteractionsTest {

    private static final String QUESTION = "Play again? Click here: [Play again]";
    private static final String REGISTRATION_CLOSED = "Registration is closed while a tournament is in progress";
    private static final RandomGenerator FIRST_CHOICE = new RandomGenerator() {

        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("only bounded integers are drawn");
        }
    };

    private ArenaMatchHarness.FakePlayer mate;

    @Test
    void aTournamentMatchKeepsTheTeamsAndTheRegistrationsAndAsksNothing() {
        AutoMatchHarness harness = new AutoMatchHarness();
        harness.addColouredTeam("Red", TeamColour.RED);
        harness.addColouredTeam("Blue", TeamColour.BLUE);
        harness.addColouredTeam("Green", TeamColour.GREEN);
        harness.addColouredTeam("Yellow", TeamColour.YELLOW);
        harness.registerEveryFighter();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.startTournament();
        assertTrue(harness.tournamentManager().getKitChoiceService().choose(AutoMatchHarness.DEFAULT_KIT_NAME,
                "admin"));
        Match tournamentMatch = harness.runningMatch();
        assertFalse(tournamentMatch.isAutoMatch());

        harness.winMatch(tournamentMatch.getFirstTeam().getTeamName());

        assertEquals(List.of("Red", "Blue", "Green", "Yellow"), harness.teamRegistry().getTeamNames());
        for (String teamName : List.of("Red", "Blue", "Green", "Yellow")) {
            ArenaMatchHarness.FakePlayer fighter = harness.fighterOf(teamName);
            assertTrue(harness.isRegistered(fighter), teamName);
            assertFalse(fighter.messages().contains(QUESTION), fighter.messages().toString());
            assertFalse(harness.hasOpenQuestion(fighter), teamName);
        }
    }

    @Test
    void aCancelledAutoMatchNeitherDissolvesTheTeamsNorAsks() {
        AutoMatchHarness harness = colouredTeams();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();

        harness.matchService().cancelActiveMatch();
        harness.advanceSeconds(1);

        assertEquals(AutoMatchState.COOLDOWN, harness.state());
        assertEquals(List.of("Red", "Blue"), harness.teamRegistry().getTeamNames());
        assertTrue(harness.isRegistered(harness.fighterOf("Red")));
        assertFalse(harness.fighterOf("Red").messages().contains(QUESTION));
        assertFalse(harness.hasOpenQuestion(harness.fighterOf("Red")));
        assertEquals(0, harness.teamSaveCount());
    }

    @Test
    void aResultForcedByAnAdminCountsLikeAPlayedMatch() {
        AutoMatchHarness harness = colouredTeams();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();

        assertTrue(harness.matchService().forceResult("auto-1", "Blue"));

        assertEquals(List.of(), harness.teamRegistry().getTeamNames());
        assertTrue(harness.fighterOf("Red").messages().contains(QUESTION));
    }

    @Test
    void anAdminInATeamGetsTheSameAsEveryOtherPlayer() {
        AutoMatchHarness harness = colouredTeams();
        ArenaMatchHarness.FakePlayer admin = harness.fighterOf("Red");
        admin.grantAdminPermission();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();

        harness.winMatch("Red");

        assertTrue(harness.teamOf(admin).isEmpty());
        assertFalse(harness.isRegistered(admin));
        assertTrue(admin.messages().contains(QUESTION), admin.messages().toString());
        harness.playAgain(admin);
        assertTrue(harness.isRegistered(admin));
        assertEquals(List.of(admin.uniqueId()), harness.shownTeamChoices());
    }

    @Test
    void aNonFighterOfUnequalTeamsWhoLeftTheAreaGetsNoQuestionButLeavesRegistrationAndTeam() {
        AutoMatchHarness harness = unequalColouredTeams();
        ArenaMatchHarness.FakePlayer leftOut = startTheVotedAutoMatchAndFindTheLeftOutMember(harness);
        leftOut.moveTo(harness.arena().at(AutoMatchHarness.FAR_AWAY, AutoMatchHarness.FAR_AWAY));

        harness.winMatch("Red");

        assertFalse(leftOut.messages().contains(QUESTION), leftOut.messages().toString());
        assertFalse(harness.hasOpenQuestion(leftOut));
        assertFalse(harness.isRegistered(leftOut));
        assertTrue(harness.teamOf(leftOut).isEmpty());
        assertEquals(List.of(), harness.teamRegistry().getTeamNames());
    }

    @Test
    void aNonFighterOfUnequalTeamsInTheAreaGetsTheQuestionLikeTheFighters() {
        AutoMatchHarness harness = unequalColouredTeams();
        ArenaMatchHarness.FakePlayer leftOut = startTheVotedAutoMatchAndFindTheLeftOutMember(harness);
        leftOut.moveTo(harness.arena().at(ArenaMatchHarness.WALL_MAXIMUM + 5.5D,
                ArenaMatchHarness.WALL_MAXIMUM + 5.5D));

        harness.winMatch("Red");

        assertTrue(leftOut.messages().contains(QUESTION), leftOut.messages().toString());
        assertFalse(harness.isRegistered(leftOut));
        assertTrue(harness.teamOf(leftOut).isEmpty());
    }

    @Test
    void anOfflineParticipantLeavesRegistrationAndTeamWithoutAQuestion() {
        AutoMatchHarness harness = colouredTeams();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winRound("Red");
        ArenaMatchHarness.FakePlayer blueFighter = harness.fighterOf("Blue");
        blueFighter.setOnline(false);

        harness.winRound("Red");
        blueFighter.setOnline(true);

        assertFalse(blueFighter.messages().contains(QUESTION), blueFighter.messages().toString());
        assertFalse(harness.hasOpenQuestion(blueFighter));
        assertFalse(harness.isRegistered(blueFighter));
        assertTrue(harness.teamOf(blueFighter).isEmpty());
    }

    @Test
    void theTeamsOfAMatchAreDissolvedAtMostOnce() {
        AutoMatchHarness harness = colouredTeams();
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        Match match = harness.runningMatch();
        harness.winMatch("Red");
        ArenaMatchHarness.FakePlayer redFighter = harness.fighterOf("Red");
        harness.playAgain(redFighter);
        harness.teamRegistry().registerTeam("Red", List.of(new TeamMember(redFighter.uniqueId(),
                redFighter.name())), TeamColour.RED);
        int teamSavesBefore = harness.teamSaveCount();

        harness.autoMatchService().onMatchCompleted(match, "Red");

        assertEquals(List.of("Red"), harness.teamRegistry().getTeamNames(), "the chosen new team stays");
        assertTrue(harness.isRegistered(redFighter));
        assertFalse(harness.hasOpenQuestion(redFighter), "no second question");
        assertEquals(1, redFighter.messages().stream().filter(QUESTION::equals).count());
        assertEquals(teamSavesBefore, harness.teamSaveCount());
    }

    @Test
    void duringATournamentTheRegistrationIsClosedAndTheQuestionStaysOpen() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie", "Delta");
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Alpha");
        harness.finishCleaning();
        harness.startTournament();
        ArenaMatchHarness.FakePlayer alphaFighter = harness.fighterOf("Alpha");
        alphaFighter.forgetReceivedTexts();

        harness.playAgain(alphaFighter);

        assertEquals(List.of(REGISTRATION_CLOSED), alphaFighter.messages());
        assertFalse(harness.isRegistered(alphaFighter));
        assertTrue(harness.hasOpenQuestion(alphaFighter));
    }

    @Test
    void clearingTheRegistrationsAndFormingRandomTeamsWorkAsBeforeWhileTheQuestionIsOpen() {
        AutoMatchHarness harness = colouredTeams();
        harness.formTeamsAtRandom();
        harness.tournamentManager().getTeamSizeSetting().setTeamSize(1);
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Red");
        ArenaMatchHarness.FakePlayer redFighter = harness.fighterOf("Red");
        ArenaMatchHarness.FakePlayer blueFighter = harness.fighterOf("Blue");
        ArenaMatchHarness.FakePlayer newcomer = harness.addPlayerInTheArea("Newcomer");
        harness.register(newcomer);

        harness.registrationService().clearRegistrations();

        assertEquals(0, harness.registrationList().getCount());
        assertTrue(harness.hasOpenQuestion(redFighter), "clearing the registrations leaves the question open");

        harness.playAgain(redFighter);
        harness.playAgain(blueFighter);
        RandomTeamFormation teamFormation = new RandomTeamFormation(harness.registrationService(),
                harness.teamRegistry(), harness.tournamentManager().getTeamSizeSetting(),
                harness.tournamentManager()::isTournamentInProgress,
                () -> harness.autoMatchService().isAnnounced() || harness.state() == AutoMatchState.RUNNING,
                () -> { }, uniqueId -> harness.arena().getPlugin().getServer().getPlayer(uniqueId), FIRST_CHOICE);

        assertTrue(teamFormation.form(newcomer.player()));

        assertEquals(2, harness.teamRegistry().getTeamCount());
        assertTrue(harness.teamOf(redFighter).orElseThrow().getColour().isPresent());
        assertTrue(harness.teamOf(blueFighter).orElseThrow().getColour().isPresent());
    }

    private static AutoMatchHarness colouredTeams() {
        AutoMatchHarness harness = new AutoMatchHarness();
        harness.addColouredTeam("Red", TeamColour.RED);
        harness.addColouredTeam("Blue", TeamColour.BLUE);
        harness.registerEveryFighter();
        return harness;
    }

    private AutoMatchHarness unequalColouredTeams() {
        AutoMatchHarness harness = colouredTeams();
        mate = harness.addPlayerInTheArea("Mate");
        harness.teamRegistry().addMember("Red", new TeamMember(mate.uniqueId(), mate.name()));
        harness.register(mate);
        harness.matchService().setUnequalTeamVoteService(new UnequalTeamVoteService(harness.arena().getPlugin(),
                uniqueId -> harness.arena().getPlugin().getServer().getPlayer(uniqueId), FIRST_CHOICE));
        return harness;
    }

    private ArenaMatchHarness.FakePlayer startTheVotedAutoMatchAndFindTheLeftOutMember(AutoMatchHarness harness) {
        harness.switchOnWithPlayAgain(AutoMatchHarness.PLAY_AGAIN_SECONDS);
        harness.advanceUntilTheAutoMatchRuns();
        harness.advanceSeconds(harness.matchService().getUnequalTeamSettings().voteSeconds() + 1);
        Match match = harness.runningMatch();
        assertTrue(match.findChosenFighters(match.findTeamByName("Red").orElseThrow()).isPresent(),
                "the larger team has chosen its fighter");
        return List.of(harness.fighterOf("Red"), mate).stream()
                .filter(member -> harness.matchService().isLeftOutOfTheChosenFighters(match, member.uniqueId()))
                .findFirst()
                .orElseThrow();
    }
}
