package de.simplepvparena.tournament;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchCompletionHandler;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class TournamentManager implements MatchCompletionHandler {

    static final String ARENA_NOT_CONFIGURED_MESSAGE = "The arena is not fully configured"
            + TournamentMessages.LINE_BREAK + "Calculate its fight area with " + Arena.CALCULATE_ACTION
            + " and set the tribune spawn with " + Arena.TRIBUNE_SPAWN_ACTION;

    static final String ARENA_STILL_BEING_CLEANED_MESSAGE = "The arena is still being cleaned. Try again in a moment.";

    static final String NO_BASELINE_WARNING = "No baseline of the fight area is stored, so the arena is not "
            + "cleaned between the matches. A baseline is taken by " + Arena.CALCULATE_ACTION;

    private final Plugin plugin;
    private final TeamRegistry teamRegistry;
    private final ArenaRegistry arenaRegistry;
    private final MatchService matchService;
    private final ArenaCleanup arenaCleanup;
    private final AdminNotifier adminNotifier;
    private final FinalMatchGate finalMatchGate;
    private final Random seedingRandom = new Random();

    private TournamentState state = TournamentState.IDLE;
    private Bracket bracket;

    private boolean waitingForArenaCleaning;

    public TournamentManager(Plugin plugin, TeamRegistry teamRegistry, ArenaRegistry arenaRegistry,
                             MatchService matchService, ArenaCleanup arenaCleanup,
                             AdminNotifier adminNotifier) {
        this.plugin = plugin;
        this.teamRegistry = teamRegistry;
        this.arenaRegistry = arenaRegistry;
        this.matchService = matchService;
        this.arenaCleanup = arenaCleanup;
        this.adminNotifier = adminNotifier;
        this.finalMatchGate = new FinalMatchGate(adminNotifier);
        this.matchService.setMatchCompletionHandler(this);
    }

    public TournamentState getState() {
        return state;
    }

    public Optional<Bracket> getBracket() {
        return Optional.ofNullable(bracket);
    }

    public FinalMatchStage getFinalMatchStage() {
        return finalMatchGate.getStage();
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
            return TournamentOperationResult.failure("The team count is uneven, use \"Force start\" in the "
                    + "match menu of /tournament to start anyway");
        }
        if (!arenaRegistry.isArenaFullyConfigured()) {
            return TournamentOperationResult.failure(ARENA_NOT_CONFIGURED_MESSAGE);
        }
        if (arenaCleanup.isCleaning()) {
            return TournamentOperationResult.failure(ARENA_STILL_BEING_CLEANED_MESSAGE);
        }

        Collections.shuffle(teamNames, seedingRandom);
        bracket = Bracket.createSingleElimination(teamNames);
        finalMatchGate.reset();
        waitingForArenaCleaning = false;
        state = TournamentState.RUNNING;
        matchService.resumeRoundTimer();

        broadcastSuccess("The tournament has started with " + teamNames.size() + " teams in "
                + bracket.getRoundCount() + " rounds");
        if (!bracket.getTeamNamesWithFirstRoundBye().isEmpty()) {
            broadcast("Teams advancing without a first round opponent: "
                    + String.join(", ", bracket.getTeamNamesWithFirstRoundBye()));
        }
        if (!arenaCleanup.hasBaseline()) {
            adminNotifier.notifyAdmins(NO_BASELINE_WARNING);
        }
        startNextMatch();
        synchronizeFinalMatchStage();
        return TournamentOperationResult.success("Tournament started");
    }

    public TournamentOperationResult togglePause() {
        if (state == TournamentState.RUNNING) {
            state = TournamentState.PAUSED;
            matchService.pauseRoundTimer();
            broadcastWarning("The tournament is paused, no further match is started until it is resumed"
                    + TournamentMessages.LINE_BREAK + "The round timer keeps its remaining time");
            return TournamentOperationResult.success("Tournament paused");
        }
        if (state == TournamentState.PAUSED) {
            if (arenaCleanup.isCleaning() && !waitingForArenaCleaning) {
                return TournamentOperationResult.failure(ARENA_STILL_BEING_CLEANED_MESSAGE);
            }
            state = TournamentState.RUNNING;
            matchService.resumeRoundTimer();
            broadcast("The tournament continues");
            startNextMatch();
            return TournamentOperationResult.success("Tournament resumed");
        }
        return TournamentOperationResult.failure("No tournament is in progress");
    }

    public TournamentOperationResult cancelTournament() {
        if (state != TournamentState.RUNNING && state != TournamentState.PAUSED) {
            return TournamentOperationResult.failure("No tournament is in progress");
        }
        matchService.cancelActiveMatch();
        matchService.resumeRoundTimer();
        bracket = null;
        finalMatchGate.reset();
        waitingForArenaCleaning = false;
        state = TournamentState.CANCELLED;
        broadcastWarning("The tournament has been cancelled");
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
            return TournamentOperationResult.success("Match " + matchId + " was decided for "
                    + winningTeam.get().getTeamName());
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
        if (arenaCleanup.isCleaning() && !waitingForArenaCleaning) {
            return TournamentOperationResult.failure(ARENA_STILL_BEING_CLEANED_MESSAGE);
        }
        recordBracketWinner(matchId, resolvedTeamName.get());
        return TournamentOperationResult.success("Match " + matchId + " was decided for " + resolvedTeamName.get());
    }

    public TournamentOperationResult startFinalMatch() {
        if (bracket == null || (state != TournamentState.RUNNING && state != TournamentState.PAUSED)) {
            return TournamentOperationResult.failure("No tournament is in progress");
        }
        if (state == TournamentState.PAUSED) {
            return TournamentOperationResult.failure("The tournament is paused"
                    + TournamentMessages.LINE_BREAK + "Continue it with \"Continue the tournament\" in the "
                    + "match menu of /tournament before starting the final");
        }
        Optional<String> stageRejection = finalMatchGate.findStartRejection();
        if (stageRejection.isPresent()) {
            return TournamentOperationResult.failure(stageRejection.get());
        }
        Optional<String> arenaRejection = findArenaRejection(arenaRegistry.isArenaFullyConfigured(),
                matchService.getActiveMatch().map(Match::getMatchId));
        if (arenaRejection.isPresent()) {
            return TournamentOperationResult.failure(arenaRejection.get());
        }
        if (waitingForArenaCleaning) {
            return TournamentOperationResult.failure("The arena is still being cleaned after the last match"
                    + TournamentMessages.LINE_BREAK + "Start the final again in a moment, the finalists "
                    + "are meant to fight in the arena the admin built");
        }
        if (arenaCleanup.isCleaning()) {
            return TournamentOperationResult.failure(ARENA_STILL_BEING_CLEANED_MESSAGE);
        }
        BracketMatch finalMatch = bracket.getFinalMatch();
        if (!resolveTeamsOrDeclareWalkover(finalMatch)) {
            return TournamentOperationResult.failure("The final cannot be started, at least one finalist is "
                    + "no longer registered");
        }
        finalMatchGate.markRunning();
        startMatchInArena(finalMatch);
        return TournamentOperationResult.success("The final has started");
    }

    @Override
    public void onMatchCompleted(Match match, String winnerTeamName) {
        cleanArenaBeforeTheNextMatch(match);
        recordBracketWinner(match.getMatchId(), winnerTeamName);
    }

    private void cleanArenaBeforeTheNextMatch(Match match) {
        Optional<String> skipReason = arenaCleanup.startCleaningAfterMatch(this::onArenaCleaned);
        if (skipReason.isEmpty()) {
            waitingForArenaCleaning = true;
            return;
        }
        if (match.isFinalMatch()) {
            plugin.getLogger().info("The arena is not cleaned automatically after the final: "
                    + toLogLine(skipReason.get()));
        } else {
            plugin.getLogger().info("The arena is not cleaned automatically after this match: "
                    + toLogLine(skipReason.get()) + ". The tournament carries on");
        }
        adminNotifier.notifyAdmins(buildSkippedCleaningWarning(match.getMatchId(), skipReason.get(),
                match.isFinalMatch()));
    }

    static String buildSkippedCleaningWarning(String matchId, String skipReason, boolean finalMatch) {
        String warning = "The arena was not cleaned after match " + matchId + ": " + toFirstLine(skipReason);
        if (finalMatch) {
            return warning + ". Run " + Arena.CALCULATE_ACTION + " again before the next tournament";
        }
        return warning + ". The next match starts in the arena as it is now";
    }

    private static String toFirstLine(String message) {
        int lineBreakIndex = message.indexOf(TournamentMessages.LINE_BREAK);
        return lineBreakIndex < 0 ? message : message.substring(0, lineBreakIndex);
    }

    private void onArenaCleaned(String outcome) {
        waitingForArenaCleaning = false;
        plugin.getLogger().info("The automatic arena cleaning after the match is over: "
                + toLogLine(outcome));
        startNextMatch();
    }

    private static String toLogLine(String message) {
        return message.replace(TournamentMessages.LINE_BREAK, " ");
    }

    static Optional<String> findArenaRejection(boolean arenaFullyConfigured, Optional<String> runningMatchId) {
        if (!arenaFullyConfigured) {
            return Optional.of(ARENA_NOT_CONFIGURED_MESSAGE);
        }
        return runningMatchId.map(matchId -> "The arena is occupied by match " + matchId);
    }

    public String describeStatus() {
        StringBuilder statusText = new StringBuilder("Tournament state: " + state);
        if (bracket == null) {
            return statusText.toString();
        }
        statusText.append(" | rounds: ").append(bracket.getRoundCount());
        statusText.append(" | running match: ")
                .append(matchService.getActiveMatch().map(Match::getMatchId).orElse("none"));
        statusText.append(" | final: ").append(finalMatchGate.getStage());
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
        synchronizeFinalMatchStage();
        if (bracket.isComplete()) {
            state = TournamentState.FINISHED;
            broadcastSuccess("The tournament is over, the winner is "
                    + bracket.getChampionTeamName().orElse(winnerTeamName));
            return;
        }
        startNextMatch();
    }

    private void synchronizeFinalMatchStage() {
        if (bracket == null) {
            return;
        }
        finalMatchGate.synchronizeWith(bracket.getFinalMatch());
    }

    private void startNextMatch() {
        if (waitingForArenaCleaning) {
            return;
        }
        while (state == TournamentState.RUNNING && bracket != null && !matchService.hasActiveMatch()) {
            Optional<BracketMatch> nextMatch = MatchSequence.findNextMatch(bracket);
            if (nextMatch.isEmpty()) {
                return;
            }
            if (!resolveTeamsOrDeclareWalkover(nextMatch.get())) {
                if (!nextMatch.get().isDecided()) {
                    return;
                }
                continue;
            }
            startMatchInArena(nextMatch.get());
            return;
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

    private void startMatchInArena(BracketMatch bracketMatch) {
        Team firstTeam = teamRegistry.findTeam(bracketMatch.getFirstTeamName().orElseThrow()).orElseThrow();
        Team secondTeam = teamRegistry.findTeam(bracketMatch.getSecondTeamName().orElseThrow()).orElseThrow();
        Arena arena = arenaRegistry.getArena();
        Match match = bracket.isFinalMatch(bracketMatch)
                ? Match.finalMatch(bracketMatch.getMatchId(), firstTeam, secondTeam, arena)
                : Match.regularMatch(bracketMatch.getMatchId(), firstTeam, secondTeam, arena);
        broadcast("Match " + match.getMatchId() + ": " + firstTeam.getTeamName() + " against "
                + secondTeam.getTeamName());
        matchService.startMatch(match);
    }

    private Optional<String> resolveBracketTeamName(BracketMatch bracketMatch, String teamName) {
        return bracketMatch.getFirstTeamName().filter(name -> name.equalsIgnoreCase(teamName))
                .or(() -> bracketMatch.getSecondTeamName().filter(name -> name.equalsIgnoreCase(teamName)));
    }

    private void broadcast(String message) {
        broadcast(TournamentMessages.info(message));
    }

    private void broadcastSuccess(String message) {
        broadcast(TournamentMessages.success(message));
    }

    private void broadcastWarning(String message) {
        broadcast(TournamentMessages.warning(message));
    }

    private void broadcast(Component message) {
        plugin.getServer().broadcast(message);
    }
}
