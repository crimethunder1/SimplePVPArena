package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.ArenaCleanup;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentOperationResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TournamentAudienceHarness {

    static final double FAR_AWAY = 5000.5D;

    private final ArenaMatchHarness arena = new ArenaMatchHarness();
    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final List<String> adminMessages = new ArrayList<>();
    private final Map<String, ArenaMatchHarness.FakePlayer> fightersByTeamName = new LinkedHashMap<>();
    private final ControlledArenaCleanup arenaCleanup = new ControlledArenaCleanup();
    private final TournamentManager tournamentManager;

    private TournamentAudienceHarness(List<String> teamNames, boolean arenaAudienceUsed) {
        for (String teamName : teamNames) {
            ArenaMatchHarness.FakePlayer fighter = arena.addPlayer(teamName + "-fighter");
            fighter.moveTo(arena.at(FAR_AWAY, FAR_AWAY));
            fightersByTeamName.put(teamName, fighter);
            teamRegistry.registerTeam(teamName, List.of(new TeamMember(fighter.uniqueId(), fighter.name())));
        }
        tournamentManager = new TournamentManager(arena.getPlugin(), teamRegistry, arena.getArenaRegistry(),
                arena.getMatchService(), arenaCleanup, adminMessages::add);
        if (arenaAudienceUsed) {
            arena.useArenaAudience();
            tournamentManager.setArenaAudience(arena.getArenaAudience());
        }
    }

    static TournamentAudienceHarness withArenaAudience(String... teamNames) {
        return new TournamentAudienceHarness(List.of(teamNames), true);
    }

    static TournamentAudienceHarness withoutArenaAudience(String... teamNames) {
        return new TournamentAudienceHarness(List.of(teamNames), false);
    }

    ArenaMatchHarness arena() {
        return arena;
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

    List<String> adminMessages() {
        return adminMessages;
    }

    List<String> broadcasts() {
        return arena.getBroadcasts();
    }

    ArenaMatchHarness.FakePlayer fighterOf(String teamName) {
        return fightersByTeamName.get(teamName);
    }

    List<ArenaMatchHarness.FakePlayer> fighters() {
        return List.copyOf(fightersByTeamName.values());
    }

    ArenaMatchHarness.FakePlayer addPlayerInTheArea(String playerName) {
        return arena.addPlayer(playerName);
    }

    ArenaMatchHarness.FakePlayer addPlayerFarOutside(String playerName) {
        ArenaMatchHarness.FakePlayer player = arena.addPlayer(playerName);
        player.moveTo(arena.at(FAR_AWAY, FAR_AWAY));
        return player;
    }

    void startTournament() {
        TournamentOperationResult start = tournamentManager.startTournament(false);
        assertTrue(start.successful(), start.message());
    }

    void startFinal() {
        TournamentOperationResult start = tournamentManager.startFinalMatch();
        assertTrue(start.successful(), start.message());
    }

    Match runningMatch() {
        return matchService().getActiveMatch().orElseThrow();
    }

    String firstTeamName() {
        return runningMatch().getFirstTeam().getTeamName();
    }

    String secondTeamName() {
        return runningMatch().getSecondTeam().getTeamName();
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

    boolean isCleaning() {
        return arenaCleanup.isCleaning();
    }

    void finishCleaning() {
        arenaCleanup.finish();
    }

    void forgetReceivedTexts() {
        arena.getBroadcasts().clear();
        adminMessages.clear();
        for (ArenaMatchHarness.FakePlayer fighter : fightersByTeamName.values()) {
            fighter.forgetReceivedTexts();
        }
    }

    static long countMessages(ArenaMatchHarness.FakePlayer player, String message) {
        return player.messages().stream().filter(message::equals).count();
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
