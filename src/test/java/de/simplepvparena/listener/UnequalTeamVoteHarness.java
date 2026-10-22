package de.simplepvparena.listener;

import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.match.PendingUnequalTeamVote;
import de.simplepvparena.match.UnequalTeamVotePresenter;
import de.simplepvparena.match.UnequalTeamVoteService;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.InMemoryKitLibraryStorage;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.ArenaCleanup;
import de.simplepvparena.tournament.AutoMatchService;
import de.simplepvparena.tournament.AutoMatchSettings;
import de.simplepvparena.tournament.KitChoicePresenter;
import de.simplepvparena.tournament.KitChoiceService;
import de.simplepvparena.tournament.PendingKitChoice;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentOperationResult;
import de.simplepvparena.tournament.TournamentStateStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class UnequalTeamVoteHarness {

    static final int AUTO_MATCH_START_DELAY_SECONDS = 10;
    static final double FAR_AWAY = 5000.5D;

    private static final long TICKS_PER_SECOND = 20L;

    private final ArenaMatchHarness arena = new ArenaMatchHarness();
    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final List<String> adminMessages = new ArrayList<>();
    private final List<PendingUnequalTeamVote> shownVotes = new ArrayList<>();
    private final List<PendingUnequalTeamVote> closedVotes = new ArrayList<>();
    private final KitLibrary kitLibrary;
    private final KitChoiceService kitChoiceService;
    private final UnequalTeamVoteService voteService;
    private final TournamentManager tournamentManager;
    private final AutoMatchService autoMatchService;

    private boolean adminOnlineForTheKitChoice;

    UnequalTeamVoteHarness() {
        arena.useArenaAudience();
        arena.getMatchService().setRoundResetDelayTicks(1L);
        voteService = new UnequalTeamVoteService(arena.getPlugin(), arena.getPlugin().getServer()::getPlayer,
                new KeepOrderRandom());
        voteService.setPresenter(new UnequalTeamVotePresenter() {
            @Override
            public void show(PendingUnequalTeamVote vote) {
                shownVotes.add(vote);
            }

            @Override
            public void refresh(PendingUnequalTeamVote vote) {
            }

            @Override
            public void close(PendingUnequalTeamVote vote) {
                closedVotes.add(vote);
            }
        });
        arena.getMatchService().setUnequalTeamVoteService(voteService);
        kitLibrary = new KitLibrary(new InMemoryKitLibraryStorage(), arena.getKitService(),
                new FakeKitItemSerializer(), problem -> { }, problem -> { });
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));
        kitLibrary.createKit("Knight", FakeSerializedKits.carrying("sword"));
        kitChoiceService = new KitChoiceService(arena.getPlugin(), kitLibrary, adminMessages::add);
        kitChoiceService.setPresenter(new KitChoicePresenter() {
            @Override
            public void show(PendingKitChoice pendingKitChoice) {
            }

            @Override
            public void close(String message) {
            }

            @Override
            public boolean hasAudience() {
                return adminOnlineForTheKitChoice;
            }
        });
        ArenaCleanup arenaCleanup = new SkippedArenaCleanup();
        tournamentManager = new TournamentManager(arena.getPlugin(), teamRegistry, arena.getArenaRegistry(),
                arena.getMatchService(), arenaCleanup, adminMessages::add, TournamentStateStorage.NONE,
                kitChoiceService);
        tournamentManager.setArenaAudience(arena.getArenaAudience());
        autoMatchService = new AutoMatchService(arena.getPlugin(), teamRegistry, arena.getArenaRegistry(),
                arena.getMatchService(), tournamentManager, kitLibrary, arenaCleanup, adminMessages::add,
                arena.getArenaAudience());
    }

    List<ArenaMatchHarness.FakePlayer> addTeam(String teamName, String... playerNames) {
        List<ArenaMatchHarness.FakePlayer> players = new ArrayList<>();
        List<TeamMember> members = new ArrayList<>();
        for (String playerName : playerNames) {
            ArenaMatchHarness.FakePlayer player = arena.addPlayer(playerName);
            players.add(player);
            members.add(new TeamMember(player.uniqueId(), player.name()));
        }
        teamRegistry.registerTeam(teamName, members);
        return players;
    }

    ArenaMatchHarness arena() {
        return arena;
    }

    MatchService matchService() {
        return arena.getMatchService();
    }

    UnequalTeamVoteService voteService() {
        return voteService;
    }

    TournamentManager tournamentManager() {
        return tournamentManager;
    }

    AutoMatchService autoMatchService() {
        return autoMatchService;
    }

    KitChoiceService kitChoiceService() {
        return kitChoiceService;
    }

    List<PendingUnequalTeamVote> shownVotes() {
        return shownVotes;
    }

    List<PendingUnequalTeamVote> closedVotes() {
        return closedVotes;
    }

    void letAnAdminChooseTheKit() {
        adminOnlineForTheKitChoice = true;
    }

    PendingUnequalTeamVote pendingVote() {
        return voteService.getPending().orElseThrow(() -> new AssertionError("no vote is running"));
    }

    void startTheFinalOfTwoTeams() {
        assertSuccessful(tournamentManager.startTournament(false));
        assertSuccessful(tournamentManager.startFinalMatch());
    }

    void startAnAutoMatch() {
        autoMatchService.applySettings(new AutoMatchSettings(true, AUTO_MATCH_START_DELAY_SECONDS, 20));
        advanceSeconds(1);
        advanceSeconds(AUTO_MATCH_START_DELAY_SECONDS);
    }

    void vote(ArenaMatchHarness.FakePlayer voter, ArenaMatchHarness.FakePlayer candidate) {
        assertTrue(voteService.vote(voter.uniqueId(), candidate.uniqueId()),
                voter.name() + " could not vote for " + candidate.name());
    }

    void advanceSeconds(long seconds) {
        arena.advanceTicksWithRepeatingTasks(seconds * TICKS_PER_SECOND);
    }

    Match runningMatch() {
        return matchService().getActiveMatch().orElseThrow();
    }

    Set<UUID> activePlayersOf(String teamName) {
        return runningMatch().getRoundParticipants().orElseThrow().getActivePlayers(teamName);
    }

    void eliminate(ArenaMatchHarness.FakePlayer player) {
        Match match = runningMatch();
        matchService().handlePlayerEliminated(player.uniqueId());
        if (match.getState() == MatchState.BETWEEN_ROUNDS) {
            arena.advanceTicksWithRepeatingTasks(1L);
        }
    }

    List<String> roundStartStepsOf(ArenaMatchHarness.FakePlayer player) {
        return arena.getRoundStartSteps().stream().filter(step -> step.endsWith(" " + player.name())).toList();
    }

    private static void assertSuccessful(TournamentOperationResult result) {
        assertTrue(result.successful(), result.message());
    }

    private static final class SkippedArenaCleanup implements ArenaCleanup {

        @Override
        public Optional<String> startCleaningAfterMatch(Consumer<String> completionHandler) {
            return Optional.of("no baseline of the fight area is stored");
        }

        @Override
        public boolean isCleaning() {
            return false;
        }

        @Override
        public boolean hasBaseline() {
            return true;
        }
    }

    private static final class KeepOrderRandom implements RandomGenerator {

        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("only bounded integers are drawn");
        }
    }
}
