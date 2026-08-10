package de.simplepvparena.tournament;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchCompletionHandler;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class TournamentManager implements MatchCompletionHandler {

    private final Plugin plugin;
    private final TeamRegistry teamRegistry;
    private final ArenaRegistry arenaRegistry;
    private final MatchService matchService;
    private final Random seedingRandom = new Random();

    private TournamentState state = TournamentState.IDLE;
    private Bracket bracket;

    public TournamentManager(Plugin plugin, TeamRegistry teamRegistry, ArenaRegistry arenaRegistry, MatchService matchService) {
        this.plugin = plugin;
        this.teamRegistry = teamRegistry;
        this.arenaRegistry = arenaRegistry;
        this.matchService = matchService;
        this.matchService.setMatchCompletionHandler(this);
    }

    public TournamentState getState() {
        return state;
    }

    public Optional<Bracket> getBracket() {
        return Optional.ofNullable(bracket);
    }

    public TournamentOperationResult startTournament(boolean allowUnevenTeamCount) {
        if (state == TournamentState.RUNNING || state == TournamentState.PAUSED) {
            return TournamentOperationResult.failure("A tournament is already in progress");
        }
        List<String> teamNames = new ArrayList<>(teamRegistry.getTeamNames());
        if (teamNames.size() < Bracket.MINIMUM_TEAM_COUNT) {
            return TournamentOperationResult.failure("At least " + Bracket.MINIMUM_TEAM_COUNT + " teams are required");
        }
        if (!allowUnevenTeamCount && teamNames.size() % 2 != 0) {
            return TournamentOperationResult.failure("The team count is uneven, use /tournament forcestart to start anyway");
        }
        if (arenaRegistry.getArenas().stream().noneMatch(Arena::isFullyConfigured)) {
            return TournamentOperationResult.failure("At least one fully configured arena is required");
        }

        java.util.Collections.shuffle(teamNames, seedingRandom);
        bracket = Bracket.createSingleElimination(teamNames);
        state = TournamentState.RUNNING;

        broadcast("The tournament has started with " + teamNames.size() + " teams in "
                + bracket.getRoundCount() + " rounds");
        if (!bracket.getTeamNamesWithFirstRoundBye().isEmpty()) {
            broadcast("Teams advancing without a first round opponent: "
                    + String.join(", ", bracket.getTeamNamesWithFirstRoundBye()));
        }
        dispatchPendingMatches();
        return TournamentOperationResult.success("Tournament started");
    }

    public TournamentOperationResult togglePause() {
        if (state == TournamentState.RUNNING) {
            state = TournamentState.PAUSED;
            broadcast("The tournament is paused, no further matches are started until it is resumed");
            return TournamentOperationResult.success("Tournament paused");
        }
        if (state == TournamentState.PAUSED) {
            state = TournamentState.RUNNING;
            broadcast("The tournament continues");
            dispatchPendingMatches();
            return TournamentOperationResult.success("Tournament resumed");
        }
        return TournamentOperationResult.failure("No tournament is in progress");
    }

    public TournamentOperationResult cancelTournament() {
        if (state != TournamentState.RUNNING && state != TournamentState.PAUSED) {
            return TournamentOperationResult.failure("No tournament is in progress");
        }
        matchService.cancelAllMatches();
        bracket = null;
        state = TournamentState.CANCELLED;
        broadcast("The tournament has been cancelled");
        return TournamentOperationResult.success("Tournament cancelled");
    }

    public TournamentOperationResult forceMatchResult(String matchId, String teamName) {
        if (bracket == null) {
            return TournamentOperationResult.failure("No tournament is in progress");
        }
        Optional<Match> activeMatch = matchService.findMatchById(matchId);
        if (activeMatch.isPresent()) {
            Optional<Team> winningTeam = activeMatch.get().findTeamByName(teamName);
            if (winningTeam.isEmpty()) {
                return TournamentOperationResult.failure(teamName + " does not take part in match " + matchId);
            }
            matchService.forceResult(activeMatch.get().getMatchId(), winningTeam.get().getTeamName());
            return TournamentOperationResult.success("Match " + matchId + " was decided for " + winningTeam.get().getTeamName());
        }
        Optional<BracketMatch> bracketMatch = bracket.findMatch(matchId);
        if (bracketMatch.isEmpty()) {
            return TournamentOperationResult.failure("Unknown match " + matchId);
        }
        if (bracketMatch.get().isDecided()) {
            return TournamentOperationResult.failure("Match " + matchId + " is already decided");
        }
        Optional<String> resolvedTeamName = resolveBracketTeamName(bracketMatch.get(), teamName);
        if (resolvedTeamName.isEmpty()) {
            return TournamentOperationResult.failure(teamName + " does not take part in match " + matchId);
        }
        recordBracketWinner(matchId, resolvedTeamName.get());
        return TournamentOperationResult.success("Match " + matchId + " was decided for " + resolvedTeamName.get());
    }

    @Override
    public void onMatchCompleted(Match match, String winnerTeamName) {
        recordBracketWinner(match.getMatchId(), winnerTeamName);
    }

    public String describeStatus() {
        StringBuilder statusText = new StringBuilder("Tournament state: " + state);
        if (bracket == null) {
            return statusText.toString();
        }
        statusText.append(" | rounds: ").append(bracket.getRoundCount());
        statusText.append(" | running matches: ").append(matchService.getActiveMatches().size());
        bracket.getChampionTeamName().ifPresent(champion -> statusText.append(" | champion: ").append(champion));
        return statusText.toString();
    }

    public List<String> describeBracket() {
        if (bracket == null) {
            return List.of("No tournament is in progress");
        }
        List<String> lines = new ArrayList<>();
        List<List<BracketMatch>> rounds = bracket.getRounds();
        for (int roundIndex = 0; roundIndex < rounds.size(); roundIndex++) {
            lines.add("Round " + (roundIndex + 1) + ":");
            for (BracketMatch bracketMatch : rounds.get(roundIndex)) {
                lines.add("  " + bracketMatch.getMatchId()
                        + " " + bracketMatch.getFirstTeamName().orElse("open")
                        + " vs " + bracketMatch.getSecondTeamName().orElse("open")
                        + bracketMatch.getWinnerTeamName().map(winner -> " -> " + winner).orElse(""));
            }
        }
        return lines;
    }

    private void recordBracketWinner(String matchId, String winnerTeamName) {
        if (bracket == null) {
            return;
        }
        bracket.recordWinner(matchId, winnerTeamName);
        if (bracket.isComplete()) {
            state = TournamentState.FINISHED;
            broadcast("The tournament is over, the winner is " + bracket.getChampionTeamName().orElse(winnerTeamName));
            return;
        }
        dispatchPendingMatches();
    }

    private void dispatchPendingMatches() {
        if (state != TournamentState.RUNNING || bracket == null) {
            return;
        }
        for (BracketMatch bracketMatch : bracket.getPlayableMatches()) {
            if (bracketMatch.isDecided() || matchService.findMatchById(bracketMatch.getMatchId()).isPresent()) {
                continue;
            }
            if (!resolveTeamsOrDeclareWalkover(bracketMatch)) {
                continue;
            }
            Optional<Arena> freeArena = arenaRegistry.findFreeArena();
            if (freeArena.isEmpty()) {
                return;
            }
            startMatchInArena(bracketMatch, freeArena.get());
        }
    }

    private boolean resolveTeamsOrDeclareWalkover(BracketMatch bracketMatch) {
        Optional<Team> firstTeam = teamRegistry.findTeam(bracketMatch.getFirstTeamName().orElse(null));
        Optional<Team> secondTeam = teamRegistry.findTeam(bracketMatch.getSecondTeamName().orElse(null));
        if (firstTeam.isPresent() && secondTeam.isPresent()) {
            return true;
        }
        if (firstTeam.isEmpty() && secondTeam.isEmpty()) {
            plugin.getLogger().warning("Both teams of match " + bracketMatch.getMatchId()
                    + " are no longer registered, the match needs an admin decision");
            return false;
        }
        Team advancingTeam = firstTeam.orElseGet(secondTeam::orElseThrow);
        broadcast(advancingTeam.getTeamName() + " advances in match " + bracketMatch.getMatchId()
                + " because the opposing team is no longer registered");
        recordBracketWinner(bracketMatch.getMatchId(), advancingTeam.getTeamName());
        return false;
    }

    private void startMatchInArena(BracketMatch bracketMatch, Arena arena) {
        Team firstTeam = teamRegistry.findTeam(bracketMatch.getFirstTeamName().orElseThrow()).orElseThrow();
        Team secondTeam = teamRegistry.findTeam(bracketMatch.getSecondTeamName().orElseThrow()).orElseThrow();
        Match match = new Match(bracketMatch.getMatchId(), firstTeam, secondTeam, arena);
        arena.occupy(match.getMatchId());
        broadcast("Match " + match.getMatchId() + ": " + firstTeam.getTeamName() + " against "
                + secondTeam.getTeamName() + " in arena " + arena.getArenaName());
        matchService.startMatch(match);
    }

    private Optional<String> resolveBracketTeamName(BracketMatch bracketMatch, String teamName) {
        return bracketMatch.getFirstTeamName().filter(name -> name.equalsIgnoreCase(teamName))
                .or(() -> bracketMatch.getSecondTeamName().filter(name -> name.equalsIgnoreCase(teamName)));
    }

    private void broadcast(String message) {
        plugin.getServer().broadcast(Component.text(message));
    }
}
