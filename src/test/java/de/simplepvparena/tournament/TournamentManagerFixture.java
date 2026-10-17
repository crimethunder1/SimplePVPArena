package de.simplepvparena.tournament;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorItemFactory;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.gate.StartGates;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.FighterColours;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.TeamColourSettings;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TournamentManagerFixture {

    static final String SKIPPED_CLEANING_REASON = "no baseline of the fight area is stored";

    private static final String WORLD_NAME = "tournament-state";
    private static final int WALL_MINIMUM = 0;
    private static final int WALL_MAXIMUM = 6;
    private static final int GROUND_Y = 64;
    private static final int WALL_TOP_Y = GROUND_Y + 3;

    private final List<Object> scheduledTasks = new ArrayList<>();
    private final List<String> adminMessages = new ArrayList<>();
    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final ArenaRegistry arenaRegistry = new ArenaRegistry();
    private final Plugin plugin;
    private final MatchService matchService;
    private final TournamentManager tournamentManager;

    private TournamentManagerFixture(Optional<TournamentStateStorage> stateStorage, List<String> teamNames) {
        SharedBukkitServer.register(createWorld(), playerUniqueId -> null);
        this.plugin = createPlugin();
        this.matchService = new MatchService(plugin, new KitService(),
                new SpectatorService(arenaRegistry, new SpectatorItemFactory(plugin)),
                new SilentFighterColours(), StartGates.NONE);
        AdminNotifier adminNotifier = adminMessages::add;
        this.tournamentManager = stateStorage
                .map(storage -> new TournamentManager(plugin, teamRegistry, arenaRegistry, matchService,
                        new SkippedArenaCleanup(), adminNotifier, storage))
                .orElseGet(() -> new TournamentManager(plugin, teamRegistry, arenaRegistry, matchService,
                        new SkippedArenaCleanup(), adminNotifier));
        buildArena();
        for (String teamName : teamNames) {
            teamRegistry.registerTeam(teamName,
                    new TeamMember(UUID.randomUUID(), teamName + "-first"),
                    new TeamMember(UUID.randomUUID(), teamName + "-second"));
        }
    }

    static TournamentManagerFixture withStorage(TournamentStateStorage stateStorage, List<String> teamNames) {
        return new TournamentManagerFixture(Optional.of(stateStorage), teamNames);
    }

    static TournamentManagerFixture withoutStorage(List<String> teamNames) {
        return new TournamentManagerFixture(Optional.empty(), teamNames);
    }

    TournamentManager tournamentManager() {
        return tournamentManager;
    }

    MatchService matchService() {
        return matchService;
    }

    TeamRegistry teamRegistry() {
        return teamRegistry;
    }

    List<String> adminMessages() {
        return adminMessages;
    }

    List<String> adminMessagesContaining(String text) {
        return adminMessages.stream().filter(message -> message.contains(text)).toList();
    }

    void startTournament() {
        TournamentOperationResult start = tournamentManager.startTournament(false);
        assertTrue(start.successful(), start.message());
    }

    Bracket bracket() {
        return tournamentManager.getBracket().orElseThrow();
    }

    BracketMatch bracketMatch(String matchId) {
        return bracket().findMatch(matchId).orElseThrow();
    }

    Optional<String> runningMatchId() {
        return matchService.getActiveMatch().map(Match::getMatchId);
    }

    String forceRunningMatchForTheFirstTeam() {
        Match match = matchService.getActiveMatch().orElseThrow();
        String winnerTeamName = match.getFirstTeam().getTeamName();
        TournamentOperationResult decision = tournamentManager.forceMatchResult(match.getMatchId(), winnerTeamName);
        assertTrue(decision.successful(), decision.message());
        return winnerTeamName;
    }

    String endRunningMatchAsPlayedByTheFirstTeam() {
        Match match = matchService.getActiveMatch().orElseThrow();
        String winnerTeamName = match.getFirstTeam().getTeamName();
        matchService.cancelActiveMatch();
        tournamentManager.onMatchCompleted(match, winnerTeamName);
        return winnerTeamName;
    }

    void startMatchOutsideTheTournament(String firstTeamName, String secondTeamName) {
        matchService.startMatch(Match.regularMatch("dry-run", teamRegistry.findTeam(firstTeamName).orElseThrow(),
                teamRegistry.findTeam(secondTeamName).orElseThrow(), arenaRegistry.getArena()));
    }

    private void buildArena() {
        BorderMarkIndex borderMarks = new BorderMarkIndex();
        for (int coordinate = WALL_MINIMUM; coordinate <= WALL_MAXIMUM; coordinate++) {
            for (int wallY = GROUND_Y; wallY <= WALL_TOP_Y; wallY++) {
                borderMarks.mark(WORLD_NAME, coordinate, wallY, WALL_MINIMUM);
                borderMarks.mark(WORLD_NAME, coordinate, wallY, WALL_MAXIMUM);
                borderMarks.mark(WORLD_NAME, WALL_MINIMUM, wallY, coordinate);
                borderMarks.mark(WORLD_NAME, WALL_MAXIMUM, wallY, coordinate);
            }
        }
        ArenaShape shape = ArenaShapeFloodFill.calculate(WORLD_NAME, 3, GROUND_Y, 3,
                        ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT,
                        borderMarks.createColumnReader(WORLD_NAME))
                .getShape().orElseThrow();
        arenaRegistry.getArena().setShape(shape);
        arenaRegistry.getArena().setTribuneSpawn(new PositionSnapshot(WORLD_NAME,
                WALL_MAXIMUM + 5.5D, GROUND_Y, WALL_MAXIMUM + 5.5D, 0F, 0F));
    }

    private World createWorld() {
        return SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> WORLD_NAME;
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
    }

    private Plugin createPlugin() {
        Object server = createServer();
        Logger logger = Logger.getLogger("TournamentManagerFixture." + UUID.randomUUID());
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.OFF);
        return SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getServer" -> server;
            case "getLogger" -> logger;
            case "getName", "namespace" -> "simplepvparena";
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
    }

    private Object createServer() {
        Object scheduler = createScheduler();
        return SharedBukkitServer.proxy(org.bukkit.Server.class,
                (method, arguments) -> switch (method.getName()) {
                    case "getScheduler" -> scheduler;
                    case "getPlayer" -> null;
                    case "getWorld" -> null;
                    case "broadcast" -> 0;
                    default -> throw new UnsupportedOperationException("Server." + method.getName());
                });
    }

    private Object createScheduler() {
        return SharedBukkitServer.proxy(BukkitScheduler.class,
                (method, arguments) -> switch (method.getName()) {
                    case "runTask", "runTaskLater", "runTaskTimer" -> schedule(arguments[1]);
                    default -> throw new UnsupportedOperationException("BukkitScheduler." + method.getName());
                });
    }

    private Object schedule(Object runnable) {
        scheduledTasks.add(runnable);
        return SharedBukkitServer.proxy(BukkitTask.class,
                (method, arguments) -> switch (method.getName()) {
                    case "cancel" -> {
                        scheduledTasks.remove(runnable);
                        yield null;
                    }
                    case "isCancelled" -> !scheduledTasks.contains(runnable);
                    case "getTaskId" -> 1;
                    default -> throw new UnsupportedOperationException("BukkitTask." + method.getName());
                });
    }

    private static final class SkippedArenaCleanup implements ArenaCleanup {

        @Override
        public Optional<String> startCleaningAfterMatch(Consumer<String> completionHandler) {
            return Optional.of(SKIPPED_CLEANING_REASON);
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

    private static final class SilentFighterColours implements FighterColours {

        @Override
        public void applySettings(TeamColourSettings settings) {
        }

        @Override
        public void colourFighters(Match match) {
        }

        @Override
        public void clearColours() {
        }
    }
}
