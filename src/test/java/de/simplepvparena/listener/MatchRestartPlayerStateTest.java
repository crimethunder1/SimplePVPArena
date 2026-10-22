package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.ArenaCleanup;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentOperationResult;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchRestartPlayerStateTest {

    private static final int COUNTDOWN_SECONDS = 3;
    private static final int RECONNECT_SECONDS = 60;
    private static final String RESTART_ANNOUNCEMENT = "Match round1-match1 is restarted";

    @TempDir
    Path dataFolder;

    private AreaInventoryFixture fixture;
    private ArenaMatchHarness harness;
    private MatchService matchService;
    private RecordingRoundStartEffects effects;
    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final Map<String, ArenaMatchHarness.FakePlayer> fightersByTeamName = new LinkedHashMap<>();
    private final List<String> adminMessages = new ArrayList<>();
    private final ControlledArenaCleanup arenaCleanup = new ControlledArenaCleanup();
    private TournamentManager tournamentManager;

    @BeforeEach
    void setUp() {
        fixture = new AreaInventoryFixture(dataFolder);
        harness = fixture.harness();
        matchService = harness.getMatchService();
        matchService.setCountdownSeconds(COUNTDOWN_SECONDS);
        matchService.setReconnectSeconds(RECONNECT_SECONDS);
        effects = harness.getRoundStartEffects();
        for (String teamName : List.of("Alpha", "Bravo", "Charlie", "Delta")) {
            ArenaMatchHarness.FakePlayer fighter = fixture.addPlayerOutsideWithItems(teamName + "-fighter");
            fighter.moveTo(fixture.onTheTribune());
            fightersByTeamName.put(teamName, fighter);
            teamRegistry.registerTeam(teamName, List.of(new TeamMember(fighter.uniqueId(), fighter.name())));
        }
        fixture.service().reconcileOnlinePlayers();
        harness.useArenaAudience();
        tournamentManager = new TournamentManager(harness.getPlugin(), teamRegistry, harness.getArenaRegistry(),
                matchService, arenaCleanup, adminMessages::add);
        tournamentManager.setArenaAudience(harness.getArenaAudience());
    }

    @Test
    void aRestartDuringTheCountdownTakesTheKitTheBlindnessAndTheAnimationAwayAndLeavesTheAreaInventoryAlone() {
        List<ArenaMatchHarness.FakePlayer> everyFighter = List.copyOf(fightersByTeamName.values());
        Map<ArenaMatchHarness.FakePlayer, ItemStack[]> contentsBeforeTheMatch = new LinkedHashMap<>();
        everyFighter.forEach(fighter -> contentsBeforeTheMatch.put(fighter, fighter.contents()));
        startTournament();
        List<ArenaMatchHarness.FakePlayer> matchFighters = fightersOfTheRunningMatch();
        matchFighters.forEach(fighter -> assertTrue(effects.isBlind(fighter), fighter.name()));
        matchFighters.forEach(fighter -> assertTrue(fixture.kitService().hasAppliedKit(fighter.player())));
        adminMessages.clear();

        restart();

        assertFalse(matchService.hasActiveMatch());
        assertFalse(matchService.isStartAnimationRunning());
        assertEquals(List.of(), effects.blindPlayerNames());
        for (ArenaMatchHarness.FakePlayer fighter : matchFighters) {
            assertFalse(fixture.kitService().hasAppliedKit(fighter.player()), fighter.name());
            assertArrayEquals(contentsBeforeTheMatch.get(fighter), fighter.contents(), fighter.name());
            assertEquals(fixture.onTheTribune().getX(), fighter.location().getX(), fighter.name());
            assertEquals(fixture.onTheTribune().getZ(), fighter.location().getZ(), fighter.name());
            assertEquals(1, TournamentAudienceHarness.countMessages(fighter, RESTART_ANNOUNCEMENT), fighter.name());
        }
        for (ArenaMatchHarness.FakePlayer fighter : everyFighter) {
            assertTrue(fixture.service().hasDeposited(fighter.player()), fighter.name());
            assertArrayEquals(AreaInventoryFixture.ownContents(),
                    fixture.findSavedInventory(fighter).contents().orElseThrow(), fighter.name());
        }
        assertEquals(List.of("Match round1-match1 was restarted"), adminMessages);
        assertEquals(List.of(), fixture.warnings());
    }

    @Test
    void theRestartedMatchBeginsLikeANewMatchAfterTheCleaning() {
        startTournament();
        Match oldMatch = matchService.getActiveMatch().orElseThrow();
        List<ArenaMatchHarness.FakePlayer> matchFighters = fightersOfTheRunningMatch();
        harness.advanceSeconds(COUNTDOWN_SECONDS);
        oldMatch.getScore().awardPoint(oldMatch.getFirstTeam().getTeamName());

        restart();
        assertTrue(arenaCleanup.isCleaning());
        assertFalse(matchService.hasActiveMatch(), "the match waits for the cleaning");
        matchFighters.forEach(ArenaMatchHarness.FakePlayer::forgetReceivedTexts);
        effects.forgetEvents();

        arenaCleanup.finish();

        Match newMatch = matchService.getActiveMatch().orElseThrow();
        assertNotSame(oldMatch, newMatch);
        assertEquals("round1-match1", newMatch.getMatchId());
        assertEquals(MatchState.RUNNING, newMatch.getState());
        assertEquals(1, newMatch.getRoundNumber());
        assertEquals(0, newMatch.getScore().getPoints(newMatch.getFirstTeam().getTeamName()));
        assertEquals(0, newMatch.getScore().getPoints(newMatch.getSecondTeam().getTeamName()));
        String announcement = "Match round1-match1: " + newMatch.getFirstTeam().getTeamName() + " against "
                + newMatch.getSecondTeam().getTeamName();
        for (ArenaMatchHarness.FakePlayer fighter : matchFighters) {
            assertTrue(fixture.kitService().hasAppliedKit(fighter.player()), fighter.name());
            assertTrue(effects.isBlind(fighter), "the countdown runs again for " + fighter.name());
            assertEquals(1, TournamentAudienceHarness.countMessages(fighter, announcement), fighter.name());
        }
        assertTrue(matchService.isStartAnimationRunning());
    }

    @Test
    void aRestartClosesTheReconnectWindowOfTheOldMatchAndNothingOfItReachesTheNewMatch() {
        startTournament();
        List<ArenaMatchHarness.FakePlayer> matchFighters = fightersOfTheRunningMatch();
        ArenaMatchHarness.FakePlayer leaver = matchFighters.getFirst();
        harness.advanceSeconds(COUNTDOWN_SECONDS);
        matchService.handlePlayerDisconnected(leaver.player());
        leaver.setOnline(false);
        assertTrue(matchService.isWaitingForReconnect(leaver.uniqueId()));

        restart();

        assertFalse(matchService.isWaitingForReconnect(leaver.uniqueId()));
        leaver.setOnline(true);
        arenaCleanup.finish();
        Match newMatch = matchService.getActiveMatch().orElseThrow();
        harness.advanceSeconds(RECONNECT_SECONDS + COUNTDOWN_SECONDS);

        assertEquals(MatchState.RUNNING, newMatch.getState());
        assertEquals(1, newMatch.getRoundNumber());
        assertTrue(matchService.isActiveFighter(leaver.uniqueId()));
        assertTrue(leaver.messages().stream().noneMatch(message -> message.contains("did not come back")),
                leaver.messages().toString());
    }

    private void startTournament() {
        TournamentOperationResult start = tournamentManager.startTournament(false);
        assertTrue(start.successful(), start.message());
    }

    private void restart() {
        TournamentOperationResult restart = tournamentManager.restartRunningMatch();
        assertTrue(restart.successful(), restart.message());
    }

    private List<ArenaMatchHarness.FakePlayer> fightersOfTheRunningMatch() {
        return matchService.getActiveMatch().orElseThrow().getTeams().stream()
                .map(Team::getTeamName)
                .map(fightersByTeamName::get)
                .toList();
    }

    private static final class ControlledArenaCleanup implements ArenaCleanup {

        private Consumer<String> completionHandler;

        @Override
        public Optional<String> startCleaningAfterMatch(Consumer<String> completionHandler) {
            this.completionHandler = completionHandler;
            return Optional.empty();
        }

        @Override
        public boolean isCleaning() {
            return completionHandler != null;
        }

        @Override
        public boolean hasBaseline() {
            return true;
        }

        private void finish() {
            Consumer<String> handler = completionHandler;
            completionHandler = null;
            handler.accept("The arena is clean");
        }
    }
}
