package de.simplepvparena.tournament;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaAudience;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchCompletionHandler;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public final class TournamentManager implements MatchCompletionHandler {

    static final String ARENA_NOT_CONFIGURED_MESSAGE = "The arena is not fully configured"
            + TournamentMessages.LINE_BREAK + "Calculate its fight area with " + Arena.CALCULATE_ACTION
            + " and set the tribune spawn with " + Arena.TRIBUNE_SPAWN_ACTION;

    static final String ARENA_STILL_BEING_CLEANED_MESSAGE = "The arena is still being cleaned. Try again in a moment.";

    static final String NO_BASELINE_WARNING = "No baseline of the fight area is stored, so the arena is not "
            + "cleaned between the matches. A baseline is taken by " + Arena.CALCULATE_ACTION;

    static final String RESTORED_WITHOUT_BASELINE_WARNING = "No baseline of the fight area is stored after the "
            + "restart. If the server crashed, repair the arena first, then run " + Arena.CALCULATE_ACTION
            + " again, that takes the arena as it is now as the new baseline, and only then continue the tournament";

    static final String SMALLER_TEAMS_NOTICE = "The tournament starts anyway, a missing player counts as absent";

    static final String LARGER_TEAMS_NOTICE = "The tournament starts anyway, every online player of these teams "
            + "fights";

    static final String SAVE_FAILURE_WARNING = "The tournament state could not be saved, the tournament carries on "
            + "and the next decision tries again: ";

    static final String DELETE_FAILURE_WARNING = "The saved tournament state could not be removed, the next server "
            + "start would restore it: ";

    public static final String RENAME_IN_TOURNAMENT_REJECTION = "A team cannot be renamed while a tournament is in "
            + "progress, the bracket keeps its name";

    private static final String CHANGE_BEFORE_MATCH_END_REJECTION = ", it cannot be changed before the match is over";

    private final Plugin plugin;
    private final TeamRegistry teamRegistry;
    private final ArenaRegistry arenaRegistry;
    private final MatchService matchService;
    private final ArenaCleanup arenaCleanup;
    private final AdminNotifier adminNotifier;
    private final TournamentStateStorage stateStorage;
    private final KitChoiceService kitChoiceService;
    private final FinalMatchGate finalMatchGate;
    private final TeamSizeSetting teamSizeSetting;
    private final Random seedingRandom = new Random();
    private final Map<String, MatchDecisionKind> decisionKindsByMatchId = new HashMap<>();
    private final Set<String> matchIdsWarnedAboutMissingTeams = new HashSet<>();

    private TournamentState state = TournamentState.IDLE;
    private Bracket bracket;
    private Instant startedAt;
    private String forcedRunningMatchId;

    private boolean waitingForArenaCleaning;
    private ArenaAudience arenaAudience;

    public TournamentManager(Plugin plugin, TeamRegistry teamRegistry, ArenaRegistry arenaRegistry,
                             MatchService matchService, ArenaCleanup arenaCleanup,
                             AdminNotifier adminNotifier) {
        this(plugin, teamRegistry, arenaRegistry, matchService, arenaCleanup, adminNotifier,
                TournamentStateStorage.NONE);
    }

    public TournamentManager(Plugin plugin, TeamRegistry teamRegistry, ArenaRegistry arenaRegistry,
                             MatchService matchService, ArenaCleanup arenaCleanup,
                             AdminNotifier adminNotifier, TournamentStateStorage stateStorage) {
        this(plugin, teamRegistry, arenaRegistry, matchService, arenaCleanup, adminNotifier, stateStorage,
                new KitChoiceService());
    }

    public TournamentManager(Plugin plugin, TeamRegistry teamRegistry, ArenaRegistry arenaRegistry,
                             MatchService matchService, ArenaCleanup arenaCleanup,
                             AdminNotifier adminNotifier, TournamentStateStorage stateStorage,
                             KitChoiceService kitChoiceService) {
        this(plugin, teamRegistry, arenaRegistry, matchService, arenaCleanup, adminNotifier, stateStorage,
                kitChoiceService, new TeamSizeSetting());
    }

    public TournamentManager(Plugin plugin, TeamRegistry teamRegistry, ArenaRegistry arenaRegistry,
                             MatchService matchService, ArenaCleanup arenaCleanup,
                             AdminNotifier adminNotifier, TournamentStateStorage stateStorage,
                             KitChoiceService kitChoiceService, TeamSizeSetting teamSizeSetting) {
        this.plugin = plugin;
        this.teamRegistry = teamRegistry;
        this.arenaRegistry = arenaRegistry;
        this.matchService = matchService;
        this.arenaCleanup = arenaCleanup;
        this.adminNotifier = adminNotifier;
        this.stateStorage = Objects.requireNonNull(stateStorage, "stateStorage");
        this.kitChoiceService = Objects.requireNonNull(kitChoiceService, "kitChoiceService");
        this.teamSizeSetting = Objects.requireNonNull(teamSizeSetting, "teamSizeSetting");
        this.finalMatchGate = new FinalMatchGate(adminNotifier);
        this.matchService.setMatchCompletionHandler(this);
    }

    public void setArenaAudience(ArenaAudience arenaAudience) {
        this.arenaAudience = Objects.requireNonNull(arenaAudience, "arenaAudience");
    }

    public TournamentState getState() {
        return state;
    }

    public boolean isTournamentInProgress() {
        return state == TournamentState.RUNNING || state == TournamentState.PAUSED;
    }

    public Optional<Bracket> getBracket() {
        return Optional.ofNullable(bracket);
    }

    public FinalMatchStage getFinalMatchStage() {
        return finalMatchGate.getStage();
    }

    public KitChoiceService getKitChoiceService() {
        return kitChoiceService;
    }

    public TeamSizeSetting getTeamSizeSetting() {
        return teamSizeSetting;
    }

    public boolean isKitChoicePending() {
        return kitChoiceService.isPending();
    }

    public Optional<String> findTeamRenameRejection(String teamName) {
        if (isTournamentInProgress()) {
            return Optional.of(RENAME_IN_TOURNAMENT_REJECTION);
        }
        return findTeamChangeRejection(teamName);
    }

    public Optional<String> findTeamChangeRejection(String teamName) {
        Optional<String> runningMatchRejection = matchService.getActiveMatch()
                .flatMap(match -> match.findTeamByName(teamName)
                        .map(team -> "Team " + team.getTeamName() + " is in match " + match.getMatchId()
                                + CHANGE_BEFORE_MATCH_END_REJECTION));
        if (runningMatchRejection.isPresent()) {
            return runningMatchRejection;
        }
        return kitChoiceService.getPending()
                .flatMap(pending -> findBracketTeamName(pending.matchId(), teamName)
                        .map(bracketTeamName -> "Team " + bracketTeamName + " is waiting for the kit choice of match "
                                + pending.matchId() + CHANGE_BEFORE_MATCH_END_REJECTION));
    }

    private Optional<String> findBracketTeamName(String matchId, String teamName) {
        if (bracket == null || teamName == null) {
            return Optional.empty();
        }
        return bracket.findMatch(matchId).flatMap(bracketMatch -> resolveBracketTeamName(bracketMatch, teamName));
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
        decisionKindsByMatchId.clear();
        matchIdsWarnedAboutMissingTeams.clear();
        startedAt = Instant.now();
        finalMatchGate.reset();
        waitingForArenaCleaning = false;
        state = TournamentState.RUNNING;
        matchService.resumeRoundTimer();
        saveState();

        broadcastSuccess("The tournament has started with " + teamNames.size() + " teams in "
                + bracket.getRoundCount() + " rounds");
        if (!bracket.getTeamNamesWithFirstRoundBye().isEmpty()) {
            broadcast("Teams advancing without a first round opponent: "
                    + String.join(", ", bracket.getTeamNamesWithFirstRoundBye()));
        }
        if (!arenaCleanup.hasBaseline()) {
            adminNotifier.notifyAdmins(NO_BASELINE_WARNING);
        }
        notifyAboutTeamsOfAnotherSize();
        startNextMatch();
        synchronizeFinalMatchStage();
        return TournamentOperationResult.success("Tournament started");
    }

    public TournamentOperationResult togglePause() {
        if (state == TournamentState.RUNNING) {
            state = TournamentState.PAUSED;
            kitChoiceService.cancel();
            matchService.pauseRoundTimer();
            broadcastWarning("The tournament is paused, no further match is started until it is resumed"
                    + TournamentMessages.LINE_BREAK + "The round timer keeps its remaining time");
            saveState();
            return TournamentOperationResult.success("Tournament paused");
        }
        if (state == TournamentState.PAUSED) {
            if (arenaCleanup.isCleaning() && !waitingForArenaCleaning) {
                return TournamentOperationResult.failure(ARENA_STILL_BEING_CLEANED_MESSAGE);
            }
            state = TournamentState.RUNNING;
            matchService.resumeRoundTimer();
            broadcast("The tournament continues");
            saveState();
            startNextMatch();
            return TournamentOperationResult.success("Tournament resumed");
        }
        return TournamentOperationResult.failure("No tournament is in progress");
    }

    public TournamentOperationResult cancelTournament() {
        if (state != TournamentState.RUNNING && state != TournamentState.PAUSED) {
            return TournamentOperationResult.failure("No tournament is in progress");
        }
        kitChoiceService.cancel();
        matchService.cancelActiveMatch();
        kitChoiceService.activateDefaultKit();
        matchService.resumeRoundTimer();
        bracket = null;
        decisionKindsByMatchId.clear();
        matchIdsWarnedAboutMissingTeams.clear();
        finalMatchGate.reset();
        waitingForArenaCleaning = false;
        state = TournamentState.CANCELLED;
        deleteSavedState();
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
            forcedRunningMatchId = activeMatch.get().getMatchId();
            try {
                matchService.forceResult(activeMatch.get().getMatchId(), winningTeam.get().getTeamName());
            } finally {
                forcedRunningMatchId = null;
            }
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
        Optional<String> kitChoiceRejection = findKitChoiceRejection();
        if (kitChoiceRejection.isPresent()) {
            return TournamentOperationResult.failure(kitChoiceRejection.get());
        }
        if (arenaCleanup.isCleaning() && !waitingForArenaCleaning) {
            return TournamentOperationResult.failure(ARENA_STILL_BEING_CLEANED_MESSAGE);
        }
        recordBracketWinner(matchId, resolvedTeamName.get(), MatchDecisionKind.FORCED);
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
        Optional<String> kitChoiceRejection = findKitChoiceRejection();
        if (kitChoiceRejection.isPresent()) {
            return TournamentOperationResult.failure(kitChoiceRejection.get());
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
            if (finalMatch.isDecided()) {
                return TournamentOperationResult.success("The final was decided by a walkover, "
                        + bracket.getChampionTeamName().orElseThrow() + " wins the tournament");
            }
            return TournamentOperationResult.failure("The final cannot be started, at least one finalist is "
                    + "no longer registered");
        }
        startMatchInArena(finalMatch);
        if (isKitChoicePending()) {
            return TournamentOperationResult.success("The kit for the final is being chosen");
        }
        return TournamentOperationResult.success("The final has started");
    }

    public boolean restoreSavedTournament() {
        if (state != TournamentState.IDLE || matchService.hasActiveMatch()) {
            return false;
        }
        TournamentStateLookup lookup = stateStorage.find();
        return switch (lookup.outcome()) {
            case MISSING -> false;
            case BROKEN -> {
                adminNotifier.notifyAdmins("The saved tournament in " + lookup.fileName() + " could not be read: "
                        + lookup.failureReason().orElse("unknown reason") + ". No tournament was restored");
                yield false;
            }
            case FOUND -> restore(lookup.savedTournament().orElseThrow(), lookup.fileName());
        };
    }

    private boolean restore(SavedTournament savedTournament, String fileName) {
        Bracket restoredBracket;
        try {
            restoredBracket = savedTournament.restoreBracket();
        } catch (IllegalArgumentException inconsistentTournament) {
            adminNotifier.notifyAdmins("The saved tournament in " + fileName + " does not fit together: "
                    + inconsistentTournament.getMessage() + ". No tournament was restored");
            return false;
        }
        kitChoiceService.cancel();
        kitChoiceService.activateDefaultKit();
        bracket = restoredBracket;
        decisionKindsByMatchId.clear();
        matchIdsWarnedAboutMissingTeams.clear();
        for (SavedMatchResult savedResult : savedTournament.results()) {
            savedResult.decidedBy().ifPresent(decisionKind ->
                    decisionKindsByMatchId.put(savedResult.matchId(), decisionKind));
        }
        startedAt = savedTournament.startedAt();
        waitingForArenaCleaning = false;
        state = TournamentState.PAUSED;
        matchService.pauseRoundTimer();
        finalMatchGate.reset();
        adminNotifier.notifyAdmins("The tournament was restored from " + fileName + " (matches decided: "
                + savedTournament.results().size() + ", rounds: " + bracket.getRoundCount() + "). It is paused, "
                + "a match that was running before the restart starts again. Continue it with \"Continue the "
                + "tournament\" in the match menu of /tournament");
        if (!arenaCleanup.hasBaseline()) {
            adminNotifier.notifyAdmins(RESTORED_WITHOUT_BASELINE_WARNING);
        }
        warnAboutUnregisteredTeams();
        synchronizeFinalMatchStage();
        return true;
    }

    private void notifyAboutTeamsOfAnotherSize() {
        int teamSize = teamSizeSetting.getTeamSize();
        TeamSizeSetting.describeTeamsSmallerThan(teamSize, teamRegistry).ifPresent(smallerTeams ->
                adminNotifier.notifyAdmins(smallerTeams + ". " + SMALLER_TEAMS_NOTICE));
        TeamSizeSetting.describeTeamsLargerThan(teamSize, teamRegistry).ifPresent(largerTeams ->
                adminNotifier.notifyAdmins(largerTeams + ". " + LARGER_TEAMS_NOTICE));
    }

    private void warnAboutUnregisteredTeams() {
        List<String> unregisteredTeamNames = bracket.getParticipatingTeamNames().stream()
                .filter(teamName -> teamRegistry.findTeam(teamName).isEmpty())
                .toList();
        if (unregisteredTeamNames.isEmpty()) {
            return;
        }
        adminNotifier.notifyAdmins("Teams of the restored tournament that are no longer registered: "
                + String.join(", ", unregisteredTeamNames) + ". Their decided matches keep the name, an open "
                + "match of theirs goes to the opponent by walkover when the tournament continues");
    }

    @Override
    public void onMatchCompleted(Match match, String winnerTeamName) {
        MatchDecisionKind decisionKind = match.getMatchId().equals(forcedRunningMatchId)
                ? MatchDecisionKind.FORCED
                : MatchDecisionKind.PLAYED;
        kitChoiceService.activateDefaultKit();
        cleanArenaBeforeTheNextMatch(match);
        recordBracketWinner(match.getMatchId(), winnerTeamName, decisionKind);
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

    static String buildKitChoiceRejection(String matchId) {
        return "A kit is being chosen for match " + matchId + ", wait for it or pause the tournament";
    }

    private Optional<String> findKitChoiceRejection() {
        return kitChoiceService.getPending().map(pending -> buildKitChoiceRejection(pending.matchId()));
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

    public List<BracketMatch> findWaitingMatches() {
        if (bracket == null) {
            return List.of();
        }
        Optional<String> runningMatchId = matchService.getActiveMatch().map(Match::getMatchId);
        return bracket.getPlayableMatches().stream()
                .filter(bracketMatch -> !runningMatchId.map(bracketMatch.getMatchId()::equals).orElse(false))
                .sorted(Comparator.comparingInt(BracketMatch::getRoundIndex)
                        .thenComparingInt(BracketMatch::getPositionIndex))
                .toList();
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

    private void recordBracketWinner(String matchId, String winnerTeamName, MatchDecisionKind decisionKind) {
        if (bracket == null) {
            return;
        }
        bracket.recordWinner(matchId, winnerTeamName);
        decisionKindsByMatchId.put(matchId, decisionKind);
        matchIdsWarnedAboutMissingTeams.remove(matchId);
        synchronizeFinalMatchStage();
        if (bracket.isComplete()) {
            state = TournamentState.FINISHED;
            deleteSavedState();
            broadcastSuccess("The tournament is over, the winner is "
                    + bracket.getChampionTeamName().orElse(winnerTeamName));
            return;
        }
        saveState();
        announceNextMatch();
        startNextMatch();
    }

    private void announceNextMatch() {
        if (state != TournamentState.RUNNING || matchService.hasActiveMatch()) {
            return;
        }
        Optional<BracketMatch> nextMatch = MatchSequence.findNextMatch(bracket);
        if (nextMatch.isPresent()) {
            findRegisteredTeams(nextMatch.get()).ifPresent(teams -> announceToMatchAudience(teams,
                    "Next: " + teams.get(0).getTeamName() + " against " + teams.get(1).getTeamName()
                            + " (" + nextMatch.get().getMatchId() + ")", true));
            return;
        }
        if (finalMatchGate.getStage() == FinalMatchStage.READY) {
            findRegisteredTeams(bracket.getFinalMatch()).ifPresent(teams -> announceToMatchAudience(teams,
                    "The final will be played by " + teams.get(0).getTeamName() + " and "
                            + teams.get(1).getTeamName() + ", an admin starts it", true));
        }
    }

    private Optional<List<Team>> findRegisteredTeams(BracketMatch bracketMatch) {
        Optional<Team> firstTeam = teamRegistry.findTeam(bracketMatch.getFirstTeamName().orElse(null));
        Optional<Team> secondTeam = teamRegistry.findTeam(bracketMatch.getSecondTeamName().orElse(null));
        if (firstTeam.isEmpty() || secondTeam.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(List.of(firstTeam.get(), secondTeam.get()));
    }

    private void saveState() {
        if (bracket == null || (state != TournamentState.RUNNING && state != TournamentState.PAUSED)) {
            return;
        }
        try {
            stateStorage.save(SavedTournament.capture(bracket, state, startedAt, Instant.now(),
                    decisionKindsByMatchId));
        } catch (IOException | RuntimeException saveFailure) {
            adminNotifier.notifyAdmins(SAVE_FAILURE_WARNING + describeFailure(saveFailure));
        }
    }

    private void deleteSavedState() {
        try {
            stateStorage.delete();
        } catch (IOException | RuntimeException deleteFailure) {
            adminNotifier.notifyAdmins(DELETE_FAILURE_WARNING + describeFailure(deleteFailure));
        }
    }

    private static String describeFailure(Exception failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    private void synchronizeFinalMatchStage() {
        if (bracket == null) {
            return;
        }
        finalMatchGate.synchronizeWith(bracket.getFinalMatch());
    }

    private void startNextMatch() {
        while (state == TournamentState.RUNNING && bracket != null && !matchService.hasActiveMatch()
                && !kitChoiceService.isPending() && !waitingForArenaCleaning) {
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
            if (matchIdsWarnedAboutMissingTeams.add(bracketMatch.getMatchId())) {
                adminNotifier.notifyAdmins("Both teams of match " + bracketMatch.getMatchId() + " are no longer "
                        + "registered, the tournament waits for you. Decide the match in the match menu of "
                        + "/tournament under \"Decide a waiting match\"");
            }
            return false;
        }
        Team advancingTeam = firstTeam.orElseGet(secondTeam::orElseThrow);
        announceToMatchAudience(List.of(advancingTeam), advancingTeam.getTeamName() + " advances in match "
                + bracketMatch.getMatchId() + " because the opposing team is no longer registered");
        recordBracketWinner(bracketMatch.getMatchId(), advancingTeam.getTeamName(), MatchDecisionKind.WALKOVER);
        return false;
    }

    private void startMatchInArena(BracketMatch bracketMatch) {
        Team firstTeam = teamRegistry.findTeam(bracketMatch.getFirstTeamName().orElseThrow()).orElseThrow();
        Team secondTeam = teamRegistry.findTeam(bracketMatch.getSecondTeamName().orElseThrow()).orElseThrow();
        Arena arena = arenaRegistry.getArena();
        boolean finalMatch = bracket.isFinalMatch(bracketMatch);
        Match match = finalMatch
                ? Match.finalMatch(bracketMatch.getMatchId(), firstTeam, secondTeam, arena)
                : Match.regularMatch(bracketMatch.getMatchId(), firstTeam, secondTeam, arena);
        kitChoiceService.begin(match.getMatchId(), firstTeam.getTeamName() + " against " + secondTeam.getTeamName(),
                () -> startChosenMatch(bracketMatch, match, finalMatch));
    }

    private void startChosenMatch(BracketMatch bracketMatch, Match match, boolean finalMatch) {
        if (!areBothTeamsRegistered(bracketMatch)) {
            kitChoiceService.activateDefaultKit();
            resolveTeamsOrDeclareWalkover(bracketMatch);
            return;
        }
        if (finalMatch) {
            finalMatchGate.markRunning();
        }
        announceToMatchAudience(match.getTeams(), "Match " + match.getMatchId() + ": "
                + match.getFirstTeam().getTeamName() + " against " + match.getSecondTeam().getTeamName());
        matchService.startMatch(match);
    }

    private boolean areBothTeamsRegistered(BracketMatch bracketMatch) {
        return teamRegistry.findTeam(bracketMatch.getFirstTeamName().orElse(null)).isPresent()
                && teamRegistry.findTeam(bracketMatch.getSecondTeamName().orElse(null)).isPresent();
    }

    private Optional<String> resolveBracketTeamName(BracketMatch bracketMatch, String teamName) {
        return bracketMatch.getFirstTeamName().filter(name -> name.equalsIgnoreCase(teamName))
                .or(() -> bracketMatch.getSecondTeamName().filter(name -> name.equalsIgnoreCase(teamName)));
    }

    private void announceToMatchAudience(List<Team> teams, String message) {
        announceToMatchAudience(teams, message, false);
    }

    private void announceToMatchAudience(List<Team> teams, String message, boolean shownInTheActionBar) {
        Component announcement = TournamentMessages.info(message);
        if (arenaAudience == null) {
            broadcast(announcement);
            return;
        }
        Component actionBarText = Component.text(message, TournamentMessages.INFO_COLOUR);
        for (Player player : arenaAudience.findMatchAudience(teams)) {
            player.sendMessage(announcement);
            if (shownInTheActionBar) {
                player.sendActionBar(actionBarText);
            }
        }
        plugin.getLogger().info(message);
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
