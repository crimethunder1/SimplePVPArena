package de.simplepvparena.listener;

import de.simplepvparena.kit.KitDefinition;
import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.InMemoryKitLibraryStorage;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.ArenaCleanup;
import de.simplepvparena.tournament.AutoMatchService;
import de.simplepvparena.tournament.AutoMatchSettings;
import de.simplepvparena.tournament.AutoMatchState;
import de.simplepvparena.tournament.KitChoicePresenter;
import de.simplepvparena.tournament.KitChoiceService;
import de.simplepvparena.tournament.PendingKitChoice;
import de.simplepvparena.tournament.SavedTournament;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentOperationResult;
import de.simplepvparena.tournament.TournamentStateLookup;
import de.simplepvparena.tournament.TournamentStateStorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AutoMatchHarness {

    static final int START_DELAY_SECONDS = 10;
    static final int PAUSE_SECONDS = 20;
    static final double FAR_AWAY = 5000.5D;
    static final String DEFAULT_KIT_NAME = "Archer";
    static final String OTHER_KIT_NAME = "Knight";
    static final String SKIPPED_CLEANING_REASON = "no baseline of the fight area is stored";

    private static final long TICKS_PER_SECOND = 20L;

    private final ArenaMatchHarness arena = new ArenaMatchHarness();
    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final List<String> adminMessages = new ArrayList<>();
    private final List<PendingKitChoice> shownKitChoices = new ArrayList<>();
    private final Map<String, ArenaMatchHarness.FakePlayer> fightersByTeamName = new LinkedHashMap<>();
    private final ControlledArenaCleanup arenaCleanup = new ControlledArenaCleanup();
    private final CountingTournamentStateStorage stateStorage = new CountingTournamentStateStorage();
    private final KitLibrary kitLibrary;
    private final TournamentManager tournamentManager;
    private final AutoMatchService autoMatchService;

    AutoMatchHarness(String... teamNames) {
        for (String teamName : teamNames) {
            addTeam(teamName);
        }
        kitLibrary = new KitLibrary(new InMemoryKitLibraryStorage(), arena.getKitService(),
                new FakeKitItemSerializer(), problem -> { }, problem -> { });
        kitLibrary.createKit(DEFAULT_KIT_NAME, FakeSerializedKits.carrying("bow"));
        kitLibrary.createKit(OTHER_KIT_NAME, FakeSerializedKits.carrying("sword"));
        KitChoiceService kitChoiceService = new KitChoiceService(arena.getPlugin(), kitLibrary, adminMessages::add);
        kitChoiceService.setPresenter(new KitChoicePresenter() {
            @Override
            public void show(PendingKitChoice pendingKitChoice) {
                shownKitChoices.add(pendingKitChoice);
            }

            @Override
            public void close(String message) {
            }

            @Override
            public boolean hasAudience() {
                return true;
            }
        });
        arena.useArenaAudience();
        arena.getMatchService().setRoundResetDelayTicks(1L);
        tournamentManager = new TournamentManager(arena.getPlugin(), teamRegistry, arena.getArenaRegistry(),
                arena.getMatchService(), arenaCleanup, adminMessages::add, stateStorage,
                kitChoiceService);
        tournamentManager.setArenaAudience(arena.getArenaAudience());
        autoMatchService = new AutoMatchService(arena.getPlugin(), teamRegistry, arena.getArenaRegistry(),
                arena.getMatchService(), tournamentManager, kitLibrary, arenaCleanup, adminMessages::add,
                arena.getArenaAudience());
    }

    ArenaMatchHarness.FakePlayer addTeam(String teamName) {
        ArenaMatchHarness.FakePlayer fighter = arena.addPlayer(teamName + "-fighter");
        fighter.moveTo(arena.at(FAR_AWAY, FAR_AWAY));
        fightersByTeamName.put(teamName, fighter);
        teamRegistry.registerTeam(teamName, List.of(new TeamMember(fighter.uniqueId(), fighter.name())));
        return fighter;
    }

    ArenaMatchHarness arena() {
        return arena;
    }

    AutoMatchService autoMatchService() {
        return autoMatchService;
    }

    TournamentManager tournamentManager() {
        return tournamentManager;
    }

    MatchService matchService() {
        return arena.getMatchService();
    }

    TeamRegistry teamRegistry() {
        return teamRegistry;
    }

    KitLibrary kitLibrary() {
        return kitLibrary;
    }

    List<String> adminMessages() {
        return adminMessages;
    }

    List<PendingKitChoice> shownKitChoices() {
        return shownKitChoices;
    }

    AutoMatchState state() {
        return autoMatchService.getState();
    }

    ArenaMatchHarness.FakePlayer fighterOf(String teamName) {
        return fightersByTeamName.get(teamName);
    }

    ArenaMatchHarness.FakePlayer addPlayerInTheArea(String playerName) {
        return arena.addPlayer(playerName);
    }

    ArenaMatchHarness.FakePlayer addPlayerFarOutside(String playerName) {
        ArenaMatchHarness.FakePlayer player = arena.addPlayer(playerName);
        player.moveTo(arena.at(FAR_AWAY, FAR_AWAY));
        return player;
    }

    void switchOn() {
        autoMatchService.applySettings(new AutoMatchSettings(true, START_DELAY_SECONDS, PAUSE_SECONDS));
    }

    void switchOnWithPause(int pauseSeconds) {
        autoMatchService.applySettings(new AutoMatchSettings(true, START_DELAY_SECONDS, pauseSeconds));
    }

    void switchOff() {
        autoMatchService.setEnabled(false);
    }

    void advanceSeconds(long seconds) {
        arena.advanceTicksWithRepeatingTasks(seconds * TICKS_PER_SECOND);
    }

    void advanceUntilTheAutoMatchRuns() {
        advanceSeconds(1);
        assertTrue(state() == AutoMatchState.ANNOUNCED, "the auto match is announced, the state is " + state());
        advanceSeconds(START_DELAY_SECONDS);
        assertTrue(state() == AutoMatchState.RUNNING, "the auto match runs, the state is " + state());
    }

    Match runningMatch() {
        return matchService().getActiveMatch().orElseThrow();
    }

    Optional<String> runningMatchId() {
        return matchService().getActiveMatch().map(Match::getMatchId);
    }

    List<String> runningTeamNames() {
        return runningMatch().getTeams().stream().map(Team::getTeamName).toList();
    }

    void winRound(String teamName) {
        Match match = runningMatch();
        String losingTeamName = match.getOpponentOf(match.findTeamByName(teamName).orElseThrow()).getTeamName();
        matchService().handlePlayerEliminated(fighterOf(losingTeamName).uniqueId());
        if (match.getState() == MatchState.BETWEEN_ROUNDS) {
            arena.runTick();
        }
    }

    void winMatch(String teamName) {
        Match match = runningMatch();
        while (!match.isFinished()) {
            winRound(teamName);
        }
    }

    void startTournament() {
        TournamentOperationResult start = tournamentManager.startTournament(false);
        assertTrue(start.successful(), start.message());
    }

    KitDefinition activeKitDefinition() {
        return arena.getKitService().getKitDefinition();
    }

    KitDefinition kitDefinitionOf(String kitName) {
        return kitLibrary.findKit(kitName).orElseThrow().kitDefinition();
    }

    boolean isCleaning() {
        return arenaCleanup.isCleaning();
    }

    void finishCleaning() {
        arenaCleanup.finish();
    }

    void skipEveryCleaning() {
        arenaCleanup.skipEveryCleaning = true;
    }

    int startedCleaningCount() {
        return arenaCleanup.startedCleaningCount;
    }

    int tournamentStorageWriteCount() {
        return stateStorage.writeCount;
    }

    List<String> messagesOf(String teamName) {
        return fighterOf(teamName).messages();
    }

    void forgetReceivedTexts() {
        adminMessages.clear();
        for (ArenaMatchHarness.FakePlayer fighter : fightersByTeamName.values()) {
            fighter.forgetReceivedTexts();
        }
    }

    static String announcement(String firstTeamName, String secondTeamName) {
        return "Auto match: " + firstTeamName + " against " + secondTeamName + " begins in " + START_DELAY_SECONDS
                + " seconds";
    }

    private static final class CountingTournamentStateStorage implements TournamentStateStorage {

        private int writeCount;

        @Override
        public void save(SavedTournament savedTournament) {
            writeCount++;
        }

        @Override
        public TournamentStateLookup find() {
            return TournamentStateStorage.NONE.find();
        }

        @Override
        public void delete() {
            writeCount++;
        }
    }

    private static final class ControlledArenaCleanup implements ArenaCleanup {

        private Consumer<String> completionHandler;
        private boolean skipEveryCleaning;
        private int startedCleaningCount;

        @Override
        public Optional<String> startCleaningAfterMatch(Consumer<String> completionHandler) {
            if (skipEveryCleaning) {
                return Optional.of(SKIPPED_CLEANING_REASON);
            }
            startedCleaningCount++;
            this.completionHandler = completionHandler;
            return Optional.empty();
        }

        @Override
        public boolean isCleaning() {
            return completionHandler != null;
        }

        @Override
        public boolean hasBaseline() {
            return !skipEveryCleaning;
        }

        private void finish() {
            Consumer<String> handler = completionHandler;
            assertTrue(handler != null, "no cleaning is running");
            completionHandler = null;
            handler.accept("The arena is clean");
        }
    }
}
