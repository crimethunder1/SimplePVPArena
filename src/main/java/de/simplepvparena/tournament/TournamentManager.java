package de.simplepvparena.tournament;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaAudience;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchCompletionHandler;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.UnequalTeamsMode;
import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.MatchMenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessage;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.message.TournamentStatusMessage;
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
    private AutoMatchReservation autoMatchReservation = AutoMatchReservation.NONE;

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

    public void setAutoMatchReservation(AutoMatchReservation autoMatchReservation) {
        this.autoMatchReservation = Objects.requireNonNull(autoMatchReservation, "autoMatchReservation");
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

    public boolean isWaitingForArenaCleaning() {
        return waitingForArenaCleaning;
    }

    public Optional<String> findTeamRenameRejection(String teamName) {
        if (isTournamentInProgress()) {
            return Optional.of(Messages.text(TournamentMessage.RENAME_IN_TOURNAMENT));
        }
        return findTeamChangeRejection(teamName);
    }

    public Optional<String> findTeamChangeRejection(String teamName) {
        Optional<String> runningMatchRejection = matchService.getActiveMatch()
                .flatMap(match -> match.findTeamByName(teamName)
                        .map(team -> Messages.text(TournamentMessage.TEAM_IN_MATCH,
                                Placeholder.of("team", team.getTeamName()),
                                Placeholder.of("match", match.getMatchId()))));
        if (runningMatchRejection.isPresent()) {
            return runningMatchRejection;
        }
        Optional<String> autoMatchRejection = autoMatchReservation.findAnnouncedTeamName(teamName)
                .map(announcedTeamName -> Messages.text(TournamentMessage.TEAM_ABOUT_TO_PLAY_AUTO_MATCH,
                        Placeholder.of("team", announcedTeamName)));
        if (autoMatchRejection.isPresent()) {
            return autoMatchRejection;
        }
        return kitChoiceService.getPending()
                .flatMap(pending -> findBracketTeamName(pending.matchId(), teamName)
                        .map(bracketTeamName -> Messages.text(TournamentMessage.TEAM_WAITING_FOR_KIT_CHOICE,
                                Placeholder.of("team", bracketTeamName), Placeholder.of("match", pending.matchId()))));
    }

    private Optional<String> findBracketTeamName(String matchId, String teamName) {
        if (bracket == null || teamName == null) {
            return Optional.empty();
        }
        return bracket.findMatch(matchId).flatMap(bracketMatch -> resolveBracketTeamName(bracketMatch, teamName));
    }

    public TournamentOperationResult startTournament(boolean allowUnevenTeamCount) {
        if (state == TournamentState.RUNNING || state == TournamentState.PAUSED) {
            return TournamentOperationResult.failure(Messages.text(CommonMessage.TOURNAMENT_ALREADY_IN_PROGRESS));
        }
        if (autoMatchReservation.occupiesTheArena()) {
            return TournamentOperationResult.failure(Messages.text(TournamentMessage.AUTO_MATCH_RUNNING));
        }
        List<String> teamNames = new ArrayList<>(teamRegistry.getTeamNames());
        if (teamNames.size() < Bracket.MINIMUM_TEAM_COUNT) {
            return TournamentOperationResult.failure(Messages.plural(TournamentMessage.AT_LEAST_TEAMS_REQUIRED,
                    Bracket.MINIMUM_TEAM_COUNT));
        }
        if (!allowUnevenTeamCount && teamNames.size() % 2 != 0) {
            return TournamentOperationResult.failure(Messages.text(TournamentMessage.UNEVEN_TEAM_COUNT,
                    Placeholder.of("action", MatchService.describeMatchMenuTile(MatchMenuMessage.FORCE_START_TITLE))));
        }
        if (!arenaRegistry.isArenaFullyConfigured()) {
            return TournamentOperationResult.failure(describeArenaNotConfigured());
        }
        if (arenaCleanup.isCleaning()) {
            return TournamentOperationResult.failure(describeArenaBeingCleaned());
        }

        autoMatchReservation.cancelAnnouncementForATournament();
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

        broadcastSuccess(Messages.text(TournamentMessage.STARTED,
                Placeholder.of("teams", Messages.plural(TournamentMessage.TEAM_COUNT, teamNames.size())),
                Placeholder.of("rounds", Messages.plural(TournamentMessage.ROUND_COUNT, bracket.getRoundCount()))));
        List<String> teamNamesWithBye = List.copyOf(bracket.getTeamNamesWithFirstRoundBye());
        if (!teamNamesWithBye.isEmpty()) {
            broadcast(Messages.plural(TournamentMessage.BYES, teamNamesWithBye.size(),
                    Placeholder.of("teams", Messages.join(teamNamesWithBye))));
        }
        if (!arenaCleanup.hasBaseline()) {
            adminNotifier.notifyAdmins(describeNoBaselineWarning());
        }
        notifyAboutTeamsOfAnotherSize();
        startNextMatch();
        synchronizeFinalMatchStage();
        return TournamentOperationResult.success(Messages.text(TournamentMessage.STARTED_REPLY));
    }

    public TournamentOperationResult togglePause() {
        if (state == TournamentState.RUNNING) {
            state = TournamentState.PAUSED;
            kitChoiceService.cancel();
            matchService.pauseRoundTimer();
            broadcastWarning(Messages.text(TournamentMessage.PAUSED));
            saveState();
            return TournamentOperationResult.success(Messages.text(TournamentMessage.PAUSED_REPLY));
        }
        if (state == TournamentState.PAUSED) {
            if (arenaCleanup.isCleaning() && !waitingForArenaCleaning) {
                return TournamentOperationResult.failure(describeArenaBeingCleaned());
            }
            state = TournamentState.RUNNING;
            matchService.resumeRoundTimer();
            broadcast(Messages.text(TournamentMessage.CONTINUES));
            saveState();
            startNextMatch();
            return TournamentOperationResult.success(Messages.text(TournamentMessage.RESUMED_REPLY));
        }
        return TournamentOperationResult.failure(Messages.text(CommonMessage.NO_TOURNAMENT_IN_PROGRESS));
    }

    public TournamentOperationResult cancelTournament() {
        if (state != TournamentState.RUNNING && state != TournamentState.PAUSED) {
            return TournamentOperationResult.failure(Messages.text(CommonMessage.NO_TOURNAMENT_IN_PROGRESS));
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
        broadcastWarning(Messages.text(TournamentMessage.CANCELLED));
        return TournamentOperationResult.success(Messages.text(TournamentMessage.CANCELLED_REPLY));
    }

    public TournamentOperationResult forceMatchResult(String matchId, String teamName) {
        Optional<Match> activeMatch = matchService.findMatchById(matchId);
        if (bracket == null && activeMatch.filter(Match::isAutoMatch).isEmpty()) {
            return TournamentOperationResult.failure(Messages.text(CommonMessage.NO_TOURNAMENT_IN_PROGRESS));
        }
        if (activeMatch.isPresent()) {
            Optional<Team> winningTeam = activeMatch.get().findTeamByName(teamName);
            if (winningTeam.isEmpty()) {
                return TournamentOperationResult.failure(describeTeamNotInMatch(teamName, matchId));
            }
            forcedRunningMatchId = activeMatch.get().getMatchId();
            try {
                matchService.forceResult(activeMatch.get().getMatchId(), winningTeam.get().getTeamName());
            } finally {
                forcedRunningMatchId = null;
            }
            return TournamentOperationResult.success(describeMatchDecided(matchId, winningTeam.get().getTeamName()));
        }
        Optional<BracketMatch> bracketMatch = bracket.findMatch(matchId);
        if (bracketMatch.isEmpty()) {
            return TournamentOperationResult.failure(Messages.text(TournamentMessage.UNKNOWN_MATCH,
                    Placeholder.of("match", matchId)));
        }
        if (bracketMatch.get().isDecided()) {
            return TournamentOperationResult.failure(Messages.text(TournamentMessage.MATCH_ALREADY_DECIDED,
                    Placeholder.of("match", matchId)));
        }
        Optional<String> resolvedTeamName = resolveBracketTeamName(bracketMatch.get(), teamName);
        if (resolvedTeamName.isEmpty()) {
            return TournamentOperationResult.failure(describeTeamNotInMatch(teamName, matchId));
        }
        Optional<String> kitChoiceRejection = findKitChoiceRejection();
        if (kitChoiceRejection.isPresent()) {
            return TournamentOperationResult.failure(kitChoiceRejection.get());
        }
        if (arenaCleanup.isCleaning() && !waitingForArenaCleaning) {
            return TournamentOperationResult.failure(describeArenaBeingCleaned());
        }
        recordBracketWinner(matchId, resolvedTeamName.get(), MatchDecisionKind.FORCED);
        return TournamentOperationResult.success(describeMatchDecided(matchId, resolvedTeamName.get()));
    }

    public Optional<String> findRestartRejection() {
        Optional<Match> activeMatch = matchService.getActiveMatch();
        if (activeMatch.filter(Match::isAutoMatch).isPresent()) {
            return Optional.of(Messages.text(TournamentMessage.RESTART_AUTO_MATCH));
        }
        Optional<String> kitChoiceRejection = findKitChoiceRejection();
        if (kitChoiceRejection.isPresent()) {
            return kitChoiceRejection;
        }
        Optional<String> unequalTeamVoteRejection = matchService.getUnequalTeamVoteService().getPending()
                .map(vote -> Messages.text(TournamentMessage.UNEQUAL_TEAM_VOTE_RUNNING,
                        Placeholder.of("match", vote.getMatchId())));
        if (unequalTeamVoteRejection.isPresent()) {
            return unequalTeamVoteRejection;
        }
        if (activeMatch.isEmpty()) {
            return Optional.of(Messages.text(MatchMenuMessage.NO_MATCH_RUNNING));
        }
        if (bracket == null || !isTournamentInProgress()) {
            return Optional.of(Messages.text(CommonMessage.NO_TOURNAMENT_IN_PROGRESS));
        }
        String matchId = activeMatch.get().getMatchId();
        if (bracket.findMatch(matchId).isEmpty()) {
            return Optional.of(Messages.text(TournamentMessage.UNKNOWN_MATCH, Placeholder.of("match", matchId)));
        }
        return Optional.empty();
    }

    public TournamentOperationResult restartRunningMatch() {
        Optional<String> rejection = findRestartRejection();
        if (rejection.isPresent()) {
            return TournamentOperationResult.failure(rejection.get());
        }
        Match match = matchService.getActiveMatch().orElseThrow();
        BracketMatch bracketMatch = bracket.findMatch(match.getMatchId()).orElseThrow();
        boolean finalMatch = bracket.isFinalMatch(bracketMatch);
        matchService.cancelActiveMatch();
        kitChoiceService.activateDefaultKit();
        if (finalMatch) {
            finalMatchGate.returnToReady();
        }
        Placeholder matchId = Placeholder.of("match", match.getMatchId());
        announceToMatchAudience(match.getTeams(), Messages.text(TournamentMessage.RESTART_ANNOUNCEMENT, matchId));
        adminNotifier.notifyAdmins(Messages.text(TournamentMessage.RESTARTED_NOTICE, matchId));
        cleanArenaBeforeTheNextMatch(match.getMatchId(), false);
        startNextMatch();
        if (finalMatch) {
            return TournamentOperationResult.success(Messages.text(TournamentMessage.FINAL_RESTARTED_REPLY,
                    Placeholder.of("action", MatchService.describeMatchMenuTile(MatchMenuMessage.FINAL_TITLE))));
        }
        return TournamentOperationResult.success(Messages.text(TournamentMessage.RESTARTED_REPLY, matchId));
    }

    public Optional<String> findRevokeRejection(String matchId) {
        if (bracket == null || !isTournamentInProgress()) {
            return Optional.of(Messages.text(CommonMessage.NO_TOURNAMENT_IN_PROGRESS));
        }
        Optional<BracketMatch> bracketMatch = bracket.findMatch(matchId);
        if (bracketMatch.isEmpty()) {
            return Optional.of(Messages.text(TournamentMessage.UNKNOWN_MATCH, Placeholder.of("match", matchId)));
        }
        return findRevokeRejection(bracketMatch.get());
    }

    public List<BracketMatch> findRevocableMatches() {
        if (bracket == null || !isTournamentInProgress()) {
            return List.of();
        }
        return bracket.getAllMatches().stream()
                .filter(bracketMatch -> findRevokeRejection(bracketMatch).isEmpty())
                .toList();
    }

    public Optional<MatchDecisionKind> findDecisionKind(String matchId) {
        return Optional.ofNullable(decisionKindsByMatchId.get(matchId));
    }

    public TournamentOperationResult revokeMatchResult(String matchId) {
        Optional<String> rejection = findRevokeRejection(matchId);
        if (rejection.isPresent()) {
            return TournamentOperationResult.failure(rejection.get());
        }
        if (arenaCleanup.isCleaning() && !waitingForArenaCleaning) {
            return TournamentOperationResult.failure(describeArenaBeingCleaned());
        }
        BracketMatch bracketMatch = bracket.findMatch(matchId).orElseThrow();
        String winnerTeamName = bracketMatch.getWinnerTeamName().orElseThrow();
        bracket.findFollowUpMatch(bracketMatch)
                .ifPresent(followUpMatch -> matchIdsWarnedAboutMissingTeams.remove(followUpMatch.getMatchId()));
        bracket.revokeWinner(bracketMatch.getMatchId());
        decisionKindsByMatchId.remove(bracketMatch.getMatchId());
        synchronizeFinalMatchStage();
        saveState();
        Placeholder revokedMatchId = Placeholder.of("match", bracketMatch.getMatchId());
        adminNotifier.notifyAdmins(Messages.text(TournamentMessage.REVOKED_NOTICE, revokedMatchId,
                Placeholder.of("team", winnerTeamName)));
        startNextMatch();
        return TournamentOperationResult.success(Messages.text(TournamentMessage.REVOKED_REPLY, revokedMatchId));
    }

    private Optional<String> findRevokeRejection(BracketMatch bracketMatch) {
        if (bracket.isFinalMatch(bracketMatch)) {
            return Optional.of(Messages.text(TournamentMessage.REVOKE_FINAL));
        }
        if (bracketMatch.isFirstRoundBye()) {
            return Optional.of(Messages.text(TournamentMessage.REVOKE_BYE));
        }
        if (!bracketMatch.isDecided()) {
            return Optional.of(Messages.text(TournamentMessage.REVOKE_NOT_DECIDED,
                    Placeholder.of("match", bracketMatch.getMatchId())));
        }
        BracketMatch followUpMatch = bracket.findFollowUpMatch(bracketMatch).orElseThrow();
        Placeholder followUpMatchId = Placeholder.of("match", followUpMatch.getMatchId());
        if (followUpMatch.isDecided()) {
            return Optional.of(Messages.text(TournamentMessage.REVOKE_FOLLOWING_MATCH_DECIDED, followUpMatchId));
        }
        if (matchService.findMatchById(followUpMatch.getMatchId()).isPresent()) {
            return Optional.of(Messages.text(TournamentMessage.REVOKE_FOLLOWING_MATCH_RUNNING, followUpMatchId));
        }
        if (kitChoiceService.getPending().filter(pending -> pending.matchId().equals(followUpMatch.getMatchId()))
                .isPresent()) {
            return Optional.of(Messages.text(TournamentMessage.REVOKE_FOLLOWING_MATCH_KIT_CHOICE, followUpMatchId));
        }
        return Optional.empty();
    }

    private static String describeTeamNotInMatch(String teamName, String matchId) {
        return Messages.text(TournamentMessage.TEAM_NOT_IN_MATCH, Placeholder.of("team", teamName),
                Placeholder.of("match", matchId));
    }

    private static String describeMatchDecided(String matchId, String teamName) {
        return Messages.text(TournamentMessage.MATCH_DECIDED, Placeholder.of("match", matchId),
                Placeholder.of("team", teamName));
    }

    public TournamentOperationResult startFinalMatch() {
        if (bracket == null || (state != TournamentState.RUNNING && state != TournamentState.PAUSED)) {
            return TournamentOperationResult.failure(Messages.text(CommonMessage.NO_TOURNAMENT_IN_PROGRESS));
        }
        if (state == TournamentState.PAUSED) {
            return TournamentOperationResult.failure(Messages.text(TournamentMessage.FINAL_WHILE_PAUSED,
                    Placeholder.of("action", MatchService.describeMatchMenuTile(MatchMenuMessage.CONTINUE_TITLE))));
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
            return TournamentOperationResult.failure(Messages.text(TournamentMessage.FINAL_ARENA_BEING_CLEANED));
        }
        if (arenaCleanup.isCleaning()) {
            return TournamentOperationResult.failure(describeArenaBeingCleaned());
        }
        BracketMatch finalMatch = bracket.getFinalMatch();
        if (!resolveTeamsOrDeclareWalkover(finalMatch)) {
            if (finalMatch.isDecided()) {
                return TournamentOperationResult.success(Messages.text(TournamentMessage.FINAL_WALKOVER,
                        Placeholder.of("team", bracket.getChampionTeamName().orElseThrow())));
            }
            return TournamentOperationResult.failure(Messages.text(TournamentMessage.FINAL_FINALIST_MISSING));
        }
        startMatchInArena(finalMatch);
        if (isKitChoicePending()) {
            return TournamentOperationResult.success(Messages.text(TournamentMessage.FINAL_KIT_BEING_CHOSEN));
        }
        return TournamentOperationResult.success(Messages.text(TournamentMessage.FINAL_STARTED));
    }

    public boolean restoreSavedTournament() {
        if (state != TournamentState.IDLE || matchService.hasActiveMatch()) {
            return false;
        }
        TournamentStateLookup lookup = stateStorage.find();
        return switch (lookup.outcome()) {
            case MISSING -> false;
            case BROKEN -> {
                adminNotifier.notifyAdmins(describeUnreadableTournament(lookup));
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
            adminNotifier.notifyAdmins(Messages.text(TournamentMessage.RESTORE_INCONSISTENT,
                    Placeholder.of("file", fileName), Placeholder.of("reason", inconsistentTournament.getMessage())));
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
        adminNotifier.notifyAdmins(Messages.plural(TournamentMessage.RESTORED, savedTournament.results().size(),
                Placeholder.of("file", fileName), Placeholder.of("rounds", bracket.getRoundCount()),
                Placeholder.of("action", MatchService.describeMatchMenuTile(MatchMenuMessage.CONTINUE_TITLE))));
        if (!arenaCleanup.hasBaseline()) {
            adminNotifier.notifyAdmins(describeRestoredWithoutBaselineWarning());
        }
        warnAboutUnregisteredTeams();
        synchronizeFinalMatchStage();
        return true;
    }

    private static String describeUnreadableTournament(TournamentStateLookup lookup) {
        Placeholder file = Placeholder.of("file", lookup.fileName());
        return lookup.failureReason()
                .map(reason -> Messages.text(TournamentMessage.RESTORE_UNREADABLE, file,
                        Placeholder.of("reason", reason)))
                .orElseGet(() -> Messages.text(TournamentMessage.RESTORE_UNREADABLE_WITHOUT_REASON, file));
    }

    private void notifyAboutTeamsOfAnotherSize() {
        int teamSize = teamSizeSetting.getTeamSize();
        TeamSizeSetting.describeTeamsSmallerThan(teamSize, teamRegistry).ifPresent(smallerTeams ->
                adminNotifier.notifyAdmins(Messages.text(TournamentMessage.SMALLER_TEAMS,
                        Placeholder.of("teams", smallerTeams))));
        int largerTeamCount = TeamSizeSetting.findTeamsLargerThan(teamSize, teamRegistry).size();
        TournamentMessage largerTeamsNotice = matchService.getUnequalTeamSettings().mode() == UnequalTeamsMode.ALL
                ? TournamentMessage.LARGER_TEAMS : TournamentMessage.LARGER_TEAMS_VOTING;
        TeamSizeSetting.describeTeamsLargerThan(teamSize, teamRegistry).ifPresent(largerTeams ->
                adminNotifier.notifyAdmins(Messages.plural(largerTeamsNotice, largerTeamCount,
                        Placeholder.of("teams", largerTeams))));
    }

    private void warnAboutUnregisteredTeams() {
        List<String> unregisteredTeamNames = bracket.getParticipatingTeamNames().stream()
                .filter(teamName -> teamRegistry.findTeam(teamName).isEmpty())
                .toList();
        if (unregisteredTeamNames.isEmpty()) {
            return;
        }
        adminNotifier.notifyAdmins(Messages.plural(TournamentMessage.UNREGISTERED_TEAMS, unregisteredTeamNames.size(),
                Placeholder.of("teams", Messages.join(unregisteredTeamNames))));
    }

    @Override
    public void onMatchCompleted(Match match, String winnerTeamName) {
        if (match.isAutoMatch()) {
            return;
        }
        MatchDecisionKind decisionKind = match.getMatchId().equals(forcedRunningMatchId)
                ? MatchDecisionKind.FORCED
                : MatchDecisionKind.PLAYED;
        kitChoiceService.activateDefaultKit();
        cleanArenaBeforeTheNextMatch(match.getMatchId(), match.isFinalMatch());
        recordBracketWinner(match.getMatchId(), winnerTeamName, decisionKind);
    }

    private void cleanArenaBeforeTheNextMatch(String matchId, boolean finalMatch) {
        Optional<String> skipReason = arenaCleanup.startCleaningAfterMatch(this::onArenaCleaned);
        if (skipReason.isEmpty()) {
            waitingForArenaCleaning = true;
            return;
        }
        if (finalMatch) {
            plugin.getLogger().info("The arena is not cleaned automatically after the final: "
                    + toLogLine(skipReason.get()));
        } else {
            plugin.getLogger().info("The arena is not cleaned automatically after this match: "
                    + toLogLine(skipReason.get()) + ". The tournament carries on");
        }
        adminNotifier.notifyAdmins(buildSkippedCleaningWarning(matchId, skipReason.get(), finalMatch));
    }

    static String describeArenaNotConfigured() {
        return Messages.text(TournamentMessage.ARENA_NOT_CONFIGURED,
                Placeholder.of("calculateAction", Arena.describeCalculateAction()),
                Placeholder.of("tribuneSpawnAction", Arena.describeTribuneSpawnAction()));
    }

    static String describeArenaBeingCleaned() {
        return Messages.text(TournamentMessage.ARENA_BEING_CLEANED);
    }

    static String describeNoBaselineWarning() {
        return Messages.text(TournamentMessage.NO_BASELINE, Placeholder.of("action", Arena.describeCalculateAction()));
    }

    static String describeRestoredWithoutBaselineWarning() {
        return Messages.text(TournamentMessage.RESTORED_WITHOUT_BASELINE,
                Placeholder.of("action", Arena.describeCalculateAction()));
    }

    static String buildSkippedCleaningWarning(String matchId, String skipReason, boolean finalMatch) {
        Placeholder match = Placeholder.of("match", matchId);
        Placeholder reason = Placeholder.of("reason", toFirstLine(skipReason));
        if (finalMatch) {
            return Messages.text(TournamentMessage.CLEANING_SKIPPED_AFTER_FINAL, match, reason,
                    Placeholder.of("action", Arena.describeCalculateAction()));
        }
        return Messages.text(TournamentMessage.CLEANING_SKIPPED, match, reason);
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
        return Messages.text(TournamentMessage.KIT_CHOICE_RUNNING, Placeholder.of("match", matchId));
    }

    private Optional<String> findKitChoiceRejection() {
        return kitChoiceService.getPending().map(pending -> buildKitChoiceRejection(pending.matchId()));
    }

    static Optional<String> findArenaRejection(boolean arenaFullyConfigured, Optional<String> runningMatchId) {
        if (!arenaFullyConfigured) {
            return Optional.of(describeArenaNotConfigured());
        }
        return runningMatchId.map(matchId -> Messages.text(TournamentMessage.ARENA_OCCUPIED,
                Placeholder.of("match", matchId)));
    }

    public String describeStatus() {
        Placeholder stateName = Placeholder.of("state", state.describe());
        if (bracket == null) {
            return Messages.text(TournamentStatusMessage.STATUS, stateName);
        }
        Placeholder rounds = Placeholder.of("rounds", bracket.getRoundCount());
        Placeholder stage = Placeholder.of("stage", finalMatchGate.getStage().describe());
        Optional<String> champion = bracket.getChampionTeamName();
        Optional<String> runningMatchId = matchService.getActiveMatch().map(Match::getMatchId);
        if (runningMatchId.isPresent()) {
            Placeholder runningMatch = Placeholder.of("match", runningMatchId.get());
            return champion
                    .map(championName -> Messages.text(TournamentStatusMessage.STATUS_WITH_RUNNING_MATCH_AND_CHAMPION,
                            stateName, rounds, runningMatch, stage, Placeholder.of("champion", championName)))
                    .orElseGet(() -> Messages.text(TournamentStatusMessage.STATUS_WITH_RUNNING_MATCH, stateName,
                            rounds, runningMatch, stage));
        }
        return champion
                .map(championName -> Messages.text(TournamentStatusMessage.STATUS_WITHOUT_RUNNING_MATCH_AND_CHAMPION,
                        stateName, rounds, stage, Placeholder.of("champion", championName)))
                .orElseGet(() -> Messages.text(TournamentStatusMessage.STATUS_WITHOUT_RUNNING_MATCH, stateName,
                        rounds, stage));
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
            broadcastSuccess(Messages.text(TournamentMessage.WINNER,
                    Placeholder.of("team", bracket.getChampionTeamName().orElse(winnerTeamName))));
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
                    Messages.text(TournamentMessage.NEXT_MATCH, Placeholder.of("firstTeam", teams.get(0).getTeamName()),
                            Placeholder.of("secondTeam", teams.get(1).getTeamName()),
                            Placeholder.of("match", nextMatch.get().getMatchId())), true));
            return;
        }
        if (finalMatchGate.getStage() == FinalMatchStage.READY) {
            findRegisteredTeams(bracket.getFinalMatch()).ifPresent(teams -> announceToMatchAudience(teams,
                    Messages.text(TournamentMessage.FINAL_PLAYERS,
                            Placeholder.of("firstTeam", teams.get(0).getTeamName()),
                            Placeholder.of("secondTeam", teams.get(1).getTeamName())), true));
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
            adminNotifier.notifyAdmins(Messages.text(TournamentMessage.SAVE_FAILURE,
                    Placeholder.of("reason", describeFailure(saveFailure))));
        }
    }

    private void deleteSavedState() {
        try {
            stateStorage.delete();
        } catch (IOException | RuntimeException deleteFailure) {
            adminNotifier.notifyAdmins(Messages.text(TournamentMessage.DELETE_FAILURE,
                    Placeholder.of("reason", describeFailure(deleteFailure))));
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
            if (matchIdsWarnedAboutMissingTeams.add(bracketMatch.getMatchId())) {
                plugin.getLogger().warning("Both teams of match " + bracketMatch.getMatchId()
                        + " are no longer registered, the match needs an admin decision");
                adminNotifier.notifyAdmins(Messages.text(TournamentMessage.BOTH_TEAMS_MISSING,
                        Placeholder.of("match", bracketMatch.getMatchId()),
                        Placeholder.of("tile", Messages.text(MatchMenuMessage.DECIDE_WAITING_MATCH_TITLE))));
            }
            return false;
        }
        Team advancingTeam = firstTeam.orElseGet(secondTeam::orElseThrow);
        announceToMatchAudience(List.of(advancingTeam), Messages.text(TournamentMessage.WALKOVER,
                Placeholder.of("team", advancingTeam.getTeamName()),
                Placeholder.of("match", bracketMatch.getMatchId())));
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
        kitChoiceService.begin(match.getMatchId(), firstTeam.getTeamName(), secondTeam.getTeamName(),
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
        announceToMatchAudience(match.getTeams(), Messages.text(TournamentMessage.MATCH_ANNOUNCEMENT,
                Placeholder.of("match", match.getMatchId()),
                Placeholder.of("firstTeam", match.getFirstTeam().getTeamName()),
                Placeholder.of("secondTeam", match.getSecondTeam().getTeamName())));
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
