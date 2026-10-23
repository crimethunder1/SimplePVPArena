package de.simplepvparena.tournament;

import de.simplepvparena.arena.ArenaAudience;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchCompletionHandler;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.AutoMatchMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class AutoMatchService implements MatchCompletionHandler, AutoMatchReservation {

    private static final String MATCH_ID_PREFIX = "auto-";
    private static final long TIMER_PERIOD_TICKS = 20L;
    private static final long NEVER_PLAYED = 0L;

    private final Plugin plugin;
    private final TeamRegistry teamRegistry;
    private final ArenaRegistry arenaRegistry;
    private final MatchService matchService;
    private final TournamentManager tournamentManager;
    private final KitLibrary kitLibrary;
    private final ArenaCleanup arenaCleanup;
    private final AdminNotifier adminNotifier;
    private final ArenaAudience arenaAudience;
    private final Map<String, Long> lastAutoMatchNumberByTeamKey = new HashMap<>();
    private final Map<String, Integer> winsByTeamName = new LinkedHashMap<>();

    private AutoMatchSettings settings = AutoMatchSettings.defaults();
    private AutoMatchState state = AutoMatchState.IDLE;
    private BukkitTask timerTask;
    private long autoMatchCount;
    private List<Team> announcedTeams;
    private Match runningMatch;
    private int secondsLeft;
    private Object runningCleaning;

    public AutoMatchService(Plugin plugin, TeamRegistry teamRegistry, ArenaRegistry arenaRegistry,
                            MatchService matchService, TournamentManager tournamentManager, KitLibrary kitLibrary,
                            ArenaCleanup arenaCleanup, AdminNotifier adminNotifier, ArenaAudience arenaAudience) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.kitLibrary = Objects.requireNonNull(kitLibrary, "kitLibrary");
        this.arenaCleanup = Objects.requireNonNull(arenaCleanup, "arenaCleanup");
        this.adminNotifier = Objects.requireNonNull(adminNotifier, "adminNotifier");
        this.arenaAudience = Objects.requireNonNull(arenaAudience, "arenaAudience");
        this.matchService.setAutoMatchCompletionHandler(this);
        this.tournamentManager.setAutoMatchReservation(this);
        this.teamRegistry.addRenameObserver(this::followRenamedTeam);
    }

    public void applySettings(AutoMatchSettings newSettings) {
        Objects.requireNonNull(newSettings, "newSettings");
        boolean wasEnabled = settings.enabled();
        settings = newSettings;
        if (newSettings.enabled() == wasEnabled) {
            return;
        }
        if (newSettings.enabled()) {
            startTimer();
            return;
        }
        switchOff();
    }

    public void setEnabled(boolean enabled) {
        applySettings(settings.withEnabled(enabled));
    }

    public boolean isEnabled() {
        return settings.enabled();
    }

    public AutoMatchState getState() {
        return state;
    }

    public boolean isAnnounced() {
        return state == AutoMatchState.ANNOUNCED;
    }

    @Override
    public boolean occupiesTheArena() {
        return state == AutoMatchState.RUNNING || (state == AutoMatchState.COOLDOWN && runningCleaning != null);
    }

    @Override
    public Optional<String> findAnnouncedTeamName(String teamName) {
        if (announcedTeams == null || teamName == null) {
            return Optional.empty();
        }
        return announcedTeams.stream()
                .map(Team::getTeamName)
                .filter(announcedTeamName -> announcedTeamName.equalsIgnoreCase(teamName))
                .findFirst();
    }

    @Override
    public void cancelAnnouncementForATournament() {
        if (state == AutoMatchState.ANNOUNCED) {
            cancelAnnouncement(AutoMatchMessage.CANCELLED_TOURNAMENT_STARTING);
        }
    }

    public Map<String, Integer> getWinsByTeamName() {
        return Map.copyOf(winsByTeamName);
    }

    private void followRenamedTeam(String formerTeamName, String newTeamName) {
        if (!winsByTeamName.containsKey(formerTeamName)) {
            return;
        }
        Map<String, Integer> renamedWins = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : winsByTeamName.entrySet()) {
            String teamName = entry.getKey().equals(formerTeamName) ? newTeamName : entry.getKey();
            renamedWins.merge(teamName, entry.getValue(), Integer::sum);
        }
        winsByTeamName.clear();
        winsByTeamName.putAll(renamedWins);
    }

    public Optional<List<Team>> findPairing() {
        List<Team> teamsByWaitingTime = teamRegistry.getTeams().stream()
                .filter(this::hasFreeOnlineMember)
                .sorted(Comparator.comparingLong(this::findLastAutoMatchNumber))
                .toList();
        if (teamsByWaitingTime.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(List.of(teamsByWaitingTime.get(0), teamsByWaitingTime.get(1)));
    }

    public void stop() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        state = AutoMatchState.IDLE;
        announcedTeams = null;
        runningMatch = null;
        runningCleaning = null;
        secondsLeft = 0;
    }

    @Override
    public void onMatchCompleted(Match match, String winnerTeamName) {
        if (match.isAutoMatch() && winnerTeamName != null) {
            winsByTeamName.merge(winnerTeamName, 1, Integer::sum);
        }
        if (state == AutoMatchState.RUNNING && match == runningMatch) {
            finishRunningMatch();
            stopTimerWhenIdle();
        }
    }

    private void startTimer() {
        if (timerTask == null) {
            timerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, TIMER_PERIOD_TICKS,
                    TIMER_PERIOD_TICKS);
        }
    }

    private void stopTimerWhenIdle() {
        if (!settings.enabled() && state == AutoMatchState.IDLE && timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private void switchOff() {
        switch (state) {
            case ANNOUNCED -> {
                cancelAnnouncement(AutoMatchMessage.CANCELLED_MODE_SWITCHED_OFF);
                secondsLeft = 0;
            }
            case COOLDOWN -> {
                state = AutoMatchState.IDLE;
                runningCleaning = null;
                secondsLeft = 0;
            }
            case IDLE, RUNNING -> { }
        }
        stopTimerWhenIdle();
    }

    private void tick() {
        switch (state) {
            case IDLE -> tickIdle();
            case ANNOUNCED -> tickAnnounced();
            case RUNNING -> tickRunning();
            case COOLDOWN -> tickCooldown();
        }
        stopTimerWhenIdle();
    }

    private void tickIdle() {
        if (secondsLeft > 0) {
            secondsLeft--;
            if (secondsLeft > 0) {
                return;
            }
        }
        if (!settings.enabled() || tournamentManager.isTournamentInProgress() || !isArenaFree()) {
            return;
        }
        findPairing().ifPresent(this::announce);
    }

    private void tickAnnounced() {
        Optional<AutoMatchMessage> cancelReason = findCancelReason();
        if (cancelReason.isPresent()) {
            cancelAnnouncement(cancelReason.get());
            return;
        }
        secondsLeft--;
        if (secondsLeft <= 0) {
            startAnnouncedMatch();
        }
    }

    private void tickRunning() {
        if (matchService.getActiveMatch().filter(match -> match == runningMatch).isEmpty()) {
            finishRunningMatch();
        }
    }

    private void tickCooldown() {
        if (runningCleaning != null) {
            return;
        }
        if (secondsLeft > 0) {
            secondsLeft--;
        }
        if (secondsLeft == 0) {
            state = AutoMatchState.IDLE;
            tickIdle();
        }
    }

    private void announce(List<Team> teams) {
        announcedTeams = teams;
        state = AutoMatchState.ANNOUNCED;
        secondsLeft = settings.startDelaySeconds();
        String announcement = Messages.plural(AutoMatchMessage.ANNOUNCEMENT, secondsLeft,
                Placeholder.of("firstTeam", teams.get(0).getTeamName()),
                Placeholder.of("secondTeam", teams.get(1).getTeamName()));
        sendToMatchAudience(teams, TournamentMessages.info(announcement), announcement);
    }

    private Optional<AutoMatchMessage> findCancelReason() {
        if (!settings.enabled()) {
            return Optional.of(AutoMatchMessage.CANCELLED_MODE_SWITCHED_OFF);
        }
        if (tournamentManager.isTournamentInProgress()) {
            return Optional.of(AutoMatchMessage.CANCELLED_TOURNAMENT_STARTING);
        }
        if (!isArenaFree()) {
            return Optional.of(AutoMatchMessage.CANCELLED_ARENA_NOT_READY);
        }
        boolean everyTeamHasAFreePlayer = announcedTeams.stream()
                .map(team -> teamRegistry.findTeam(team.getTeamName()))
                .allMatch(registeredTeam -> registeredTeam.filter(this::hasFreeOnlineMember).isPresent());
        if (!everyTeamHasAFreePlayer) {
            return Optional.of(AutoMatchMessage.CANCELLED_TEAM_OFFLINE);
        }
        return Optional.empty();
    }

    private void cancelAnnouncement(AutoMatchMessage reason) {
        List<Team> teams = announcedTeams;
        announcedTeams = null;
        state = AutoMatchState.IDLE;
        secondsLeft = settings.pauseSeconds();
        String cancellation = Messages.text(reason);
        sendToMatchAudience(teams, TournamentMessages.warning(cancellation), cancellation);
    }

    private void startAnnouncedMatch() {
        Team firstTeam = teamRegistry.findTeam(announcedTeams.get(0).getTeamName()).orElseThrow();
        Team secondTeam = teamRegistry.findTeam(announcedTeams.get(1).getTeamName()).orElseThrow();
        announcedTeams = null;
        autoMatchCount++;
        Match match = Match.autoMatch(MATCH_ID_PREFIX + autoMatchCount, firstTeam, secondTeam,
                arenaRegistry.getArena());
        runningMatch = match;
        state = AutoMatchState.RUNNING;
        kitLibrary.activateDefaultKit();
        matchService.startMatch(match);
    }

    private void finishRunningMatch() {
        Match match = runningMatch;
        runningMatch = null;
        for (Team team : match.getTeams()) {
            lastAutoMatchNumberByTeamKey.put(toKey(team.getTeamName()), autoMatchCount);
        }
        kitLibrary.activateDefaultKit();
        state = AutoMatchState.COOLDOWN;
        secondsLeft = settings.pauseSeconds();
        cleanTheArena(match);
        if (!settings.enabled()) {
            state = AutoMatchState.IDLE;
            runningCleaning = null;
            secondsLeft = 0;
        }
    }

    private void cleanTheArena(Match match) {
        Object cleaning = new Object();
        runningCleaning = cleaning;
        Optional<String> skipReason = arenaCleanup.startCleaningAfterMatch(outcome -> onArenaCleaned(cleaning));
        if (skipReason.isEmpty()) {
            return;
        }
        runningCleaning = null;
        adminNotifier.notifyAdmins(TournamentManager.buildSkippedCleaningWarning(match.getMatchId(),
                skipReason.get(), false));
    }

    private void onArenaCleaned(Object cleaning) {
        if (runningCleaning != cleaning) {
            return;
        }
        runningCleaning = null;
        secondsLeft = settings.pauseSeconds();
    }

    private boolean isArenaFree() {
        return arenaRegistry.isArenaFullyConfigured()
                && !matchService.hasActiveMatch()
                && !arenaCleanup.isCleaning()
                && !tournamentManager.isWaitingForArenaCleaning()
                && !tournamentManager.isKitChoicePending();
    }

    private boolean hasFreeOnlineMember(Team team) {
        for (UUID memberUniqueId : team.getMemberUniqueIds()) {
            Player player = plugin.getServer().getPlayer(memberUniqueId);
            if (player != null && player.isOnline() && !matchService.isActiveFighter(memberUniqueId)) {
                return true;
            }
        }
        return false;
    }

    private long findLastAutoMatchNumber(Team team) {
        return lastAutoMatchNumberByTeamKey.getOrDefault(toKey(team.getTeamName()), NEVER_PLAYED);
    }

    private void sendToMatchAudience(List<Team> teams, Component message, String logMessage) {
        for (Player player : arenaAudience.findMatchAudience(teams)) {
            player.sendMessage(message);
        }
        plugin.getLogger().info(logMessage);
    }

    private static String toKey(String teamName) {
        return teamName.toLowerCase(Locale.ROOT);
    }
}
