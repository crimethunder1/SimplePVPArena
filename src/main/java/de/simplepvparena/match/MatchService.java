package de.simplepvparena.match;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.team.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class MatchService {

    private static final long DEFAULT_ROUND_RESET_DELAY_TICKS = 100L;
    private static final long ROUND_TIMER_PERIOD_TICKS = 20L;
    private static final Title.Times SUDDEN_DEATH_TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(200L), Duration.ofSeconds(2L), Duration.ofMillis(500L));

    private final Plugin plugin;
    private final KitService kitService;
    private final SpectatorService spectatorService;
    private final Map<String, Match> activeMatchesById = new LinkedHashMap<>();
    private final Map<String, ScheduledRoundTimer> roundTimersByMatchId = new LinkedHashMap<>();
    private final Set<String> matchIdsInSuddenDeathTick = new LinkedHashSet<>();
    private final SuddenDeathTick suddenDeathTick = new SuddenDeathTick(new Random());

    private MatchCompletionHandler matchCompletionHandler = (match, winnerTeamName) -> { };
    private long roundResetDelayTicks = DEFAULT_ROUND_RESET_DELAY_TICKS;
    private RoundTimeoutSettings roundTimeoutSettings = RoundTimeoutSettings.defaults();
    private boolean roundTimersPaused;

    public MatchService(Plugin plugin, KitService kitService, SpectatorService spectatorService) {
        this.plugin = plugin;
        this.kitService = kitService;
        this.spectatorService = spectatorService;
    }

    public void setMatchCompletionHandler(MatchCompletionHandler matchCompletionHandler) {
        this.matchCompletionHandler = matchCompletionHandler;
    }

    public void setRoundResetDelayTicks(long roundResetDelayTicks) {
        this.roundResetDelayTicks = Math.max(0L, roundResetDelayTicks);
    }

    public void setRoundTimeoutSettings(RoundTimeoutSettings roundTimeoutSettings) {
        this.roundTimeoutSettings = Objects.requireNonNull(roundTimeoutSettings, "roundTimeoutSettings");
    }

    public RoundTimeoutSettings getRoundTimeoutSettings() {
        return roundTimeoutSettings;
    }

    public void startMatch(Match match) {
        activeMatchesById.put(match.getMatchId(), match);
        announceToMatch(match, "Match " + match.getMatchId() + " starts in arena " + match.getArena().getArenaName()
                + ": " + match.getFirstTeam().getTeamName() + " against " + match.getSecondTeam().getTeamName());
        beginRound(match);
    }

    public Collection<Match> getActiveMatches() {
        return List.copyOf(activeMatchesById.values());
    }

    public Optional<Match> findMatchById(String matchId) {
        if (matchId == null) {
            return Optional.empty();
        }
        return activeMatchesById.values().stream()
                .filter(match -> match.getMatchId().equalsIgnoreCase(matchId))
                .findFirst();
    }

    public Optional<Match> findMatchOfPlayer(UUID playerUniqueId) {
        return activeMatchesById.values().stream()
                .filter(match -> match.containsPlayer(playerUniqueId))
                .findFirst();
    }

    public Optional<Match> findMatchInArena(String arenaName) {
        return activeMatchesById.values().stream()
                .filter(match -> match.getArena().getArenaName().equalsIgnoreCase(arenaName))
                .findFirst();
    }

    public boolean isActiveFighter(UUID playerUniqueId) {
        return findMatchOfPlayer(playerUniqueId)
                .flatMap(Match::getRoundParticipants)
                .map(participants -> participants.isActive(playerUniqueId))
                .orElse(false);
    }

    public boolean isFriendlyFire(UUID attackerUniqueId, UUID victimUniqueId) {
        return findMatchOfPlayer(victimUniqueId)
                .filter(match -> match.getState() == MatchState.RUNNING)
                .flatMap(Match::getRoundParticipants)
                .map(participants -> participants.isSameActiveTeam(attackerUniqueId, victimUniqueId))
                .orElse(false);
    }

    public void handlePlayerEliminated(UUID playerUniqueId) {
        Optional<Match> matchOfPlayer = findMatchOfPlayer(playerUniqueId);
        if (matchOfPlayer.isEmpty()) {
            return;
        }
        Match match = matchOfPlayer.get();
        if (match.getState() != MatchState.RUNNING) {
            return;
        }
        Optional<RoundParticipants> roundParticipants = match.getRoundParticipants();
        if (roundParticipants.isEmpty() || !roundParticipants.get().eliminate(playerUniqueId)) {
            return;
        }
        if (matchIdsInSuddenDeathTick.contains(match.getMatchId())) {
            return;
        }
        roundParticipants.get().determineOutcome().ifPresent(outcome -> concludeRound(match, outcome));
    }

    public boolean forceResult(String matchId, String teamName) {
        Optional<Match> match = findMatchById(matchId);
        if (match.isEmpty()) {
            return false;
        }
        Optional<Team> winningTeam = match.get().findTeamByName(teamName);
        if (winningTeam.isEmpty()) {
            return false;
        }
        finishMatch(match.get(), winningTeam.get().getTeamName());
        return true;
    }

    public void cancelAllMatches() {
        for (Match match : List.copyOf(activeMatchesById.values())) {
            stopRoundTimer(match);
            match.setState(MatchState.CANCELLED);
            releaseParticipants(match);
            match.getArena().release();
            activeMatchesById.remove(match.getMatchId());
        }
    }

    public void pauseRoundTimers() {
        roundTimersPaused = true;
        roundTimersByMatchId.values().forEach(scheduledTimer -> scheduledTimer.timer().pause());
    }

    public void resumeRoundTimers() {
        roundTimersPaused = false;
        roundTimersByMatchId.values().forEach(scheduledTimer -> scheduledTimer.timer().resume());
    }

    public boolean areRoundTimersPaused() {
        return roundTimersPaused;
    }

    public Optional<RoundTimer> findRoundTimer(String matchId) {
        return Optional.ofNullable(roundTimersByMatchId.get(matchId)).map(ScheduledRoundTimer::timer);
    }

    private void beginRound(Match match) {
        Set<UUID> firstTeamPlayers = collectOnlineMembers(match.getFirstTeam());
        Set<UUID> secondTeamPlayers = collectOnlineMembers(match.getSecondTeam());
        match.beginRound(firstTeamPlayers, secondTeamPlayers);

        preparePlayers(match, match.getFirstTeam(), match.getArena().getFirstTeamSpawn());
        preparePlayers(match, match.getSecondTeam(), match.getArena().getSecondTeamSpawn());
        startRoundTimer(match);
        announceToMatch(match, "Round " + match.getRoundNumber() + " of match " + match.getMatchId()
                + " has started (" + match.getScore().describe() + "), time limit "
                + roundTimeoutSettings.roundTimeoutSeconds() + " seconds");

        Optional<RoundOutcome> immediateOutcome = match.getRoundParticipants().orElseThrow().determineOutcome();
        if (immediateOutcome.isPresent()) {
            concludeRound(match, immediateOutcome.get());
        }
    }

    private void concludeRound(Match match, RoundOutcome outcome) {
        stopRoundTimer(match);
        if (outcome.isDraw()) {
            match.setState(MatchState.BETWEEN_ROUNDS);
            for (String drawMessage : buildDrawMessages(match.getRoundNumber(), match.getMatchId(), outcome)) {
                announceToMatch(match, drawMessage);
            }
            if (hasAnyOnlineParticipant(match)) {
                scheduleNextRound(match);
                return;
            }
            announceToMatch(match, "Match " + match.getMatchId()
                    + " is halted because no participant is online, use /tournament match forceresult to resolve it");
            return;
        }
        String winnerTeamName = outcome.winnerTeamName().orElseThrow();
        match.getScore().awardPoint(winnerTeamName);
        announceToMatch(match, winnerTeamName + " scores a point (" + match.getScore().describe() + ")");
        Optional<String> matchWinner = match.getScore().getWinnerTeamName();
        if (matchWinner.isPresent()) {
            finishMatch(match, matchWinner.get());
            return;
        }
        match.setState(MatchState.BETWEEN_ROUNDS);
        scheduleNextRound(match);
    }

    static List<String> buildDrawMessages(int roundNumber, String matchId, RoundOutcome outcome) {
        String drawMessage = "Round " + roundNumber + " of match " + matchId
                + " ended without a surviving team, no point is awarded";
        if (!outcome.isSuddenDeathDraw()) {
            return List.of(drawMessage);
        }
        return List.of(drawMessage, "Both teams fell in the same sudden death second, round " + roundNumber
                + " of match " + matchId + " is repeated");
    }

    private void scheduleNextRound(Match match) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (match.getState() == MatchState.BETWEEN_ROUNDS) {
                beginRound(match);
            }
        }, roundResetDelayTicks);
    }

    private void finishMatch(Match match, String winnerTeamName) {
        stopRoundTimer(match);
        match.setState(MatchState.FINISHED);
        activeMatchesById.remove(match.getMatchId());
        announceToMatch(match, "Match " + match.getMatchId() + " is over, " + winnerTeamName + " wins ("
                + match.getScore().describe() + ")");
        releaseParticipants(match);
        match.getArena().release();
        matchCompletionHandler.onMatchCompleted(match, winnerTeamName);
    }

    private void startRoundTimer(Match match) {
        stopRoundTimer(match);
        RoundTimer roundTimer = new RoundTimer(roundTimeoutSettings);
        if (roundTimersPaused) {
            roundTimer.pause();
        }
        BukkitTask timerTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> handleRoundTimerTick(match), ROUND_TIMER_PERIOD_TICKS, ROUND_TIMER_PERIOD_TICKS);
        roundTimersByMatchId.put(match.getMatchId(), new ScheduledRoundTimer(roundTimer, timerTask));
    }

    private void stopRoundTimer(Match match) {
        ScheduledRoundTimer scheduledTimer = roundTimersByMatchId.remove(match.getMatchId());
        if (scheduledTimer != null) {
            scheduledTimer.task().cancel();
        }
    }

    private void handleRoundTimerTick(Match match) {
        ScheduledRoundTimer scheduledTimer = roundTimersByMatchId.get(match.getMatchId());
        if (scheduledTimer == null) {
            return;
        }
        if (match.getState() != MatchState.RUNNING) {
            return;
        }
        RoundTimer roundTimer = scheduledTimer.timer();
        switch (roundTimer.tick()) {
            case WARNING -> announceActionBarToMatch(match,
                    "Sudden death in " + roundTimer.getRemainingSeconds() + " seconds");
            case SUDDEN_DEATH_STARTED -> announceSuddenDeathStart(match);
            case SUDDEN_DEATH_DAMAGE -> applySuddenDeathDamage(match, roundTimer.getCurrentSuddenDeathDamage());
            case RUNNING, PAUSED -> { }
        }
    }

    private void announceSuddenDeathStart(Match match) {
        announceTitleToMatch(match, "Sudden death", "Both teams take damage until one of them falls");
        announceToMatch(match, "The time limit of round " + match.getRoundNumber() + " in match " + match.getMatchId()
                + " is over, sudden death has started");
    }

    private void applySuddenDeathDamage(Match match, double damageAmount) {
        Optional<RoundParticipants> roundParticipants = match.getRoundParticipants();
        if (roundParticipants.isEmpty()) {
            return;
        }
        Optional<RoundOutcome> outcome;
        matchIdsInSuddenDeathTick.add(match.getMatchId());
        try {
            outcome = suddenDeathTick.apply(roundParticipants.get(), damageAmount,
                    activePlayerUniqueId -> damagePlayer(activePlayerUniqueId, damageAmount));
        } finally {
            matchIdsInSuddenDeathTick.remove(match.getMatchId());
        }
        if (match.getState() == MatchState.RUNNING) {
            outcome.ifPresent(roundOutcome -> concludeRound(match, roundOutcome));
        }
    }

    private void damagePlayer(UUID playerUniqueId, double damageAmount) {
        Player player = plugin.getServer().getPlayer(playerUniqueId);
        if (player != null && player.isOnline()) {
            player.damage(damageAmount);
        }
    }

    private void preparePlayers(Match match, Team team, Optional<PositionSnapshot> spawnPosition) {
        Optional<Location> spawnLocation = spawnPosition.flatMap(PositionSnapshot::toLocation);
        for (Player player : collectOnlinePlayers(team)) {
            spectatorService.clearAssignment(player.getUniqueId());
            spawnLocation.ifPresent(player::teleport);
            kitService.applyKit(player);
        }
        if (spawnLocation.isEmpty()) {
            plugin.getLogger().warning("Arena " + match.getArena().getArenaName()
                    + " has no usable spawn position for team " + team.getTeamName());
        }
    }

    private void releaseParticipants(Match match) {
        Arena arena = match.getArena();
        for (Team team : match.getTeams()) {
            for (Player player : collectOnlinePlayers(team)) {
                kitService.clearKit(player);
                spectatorService.sendToTribune(player, arena);
            }
        }
    }

    private boolean hasAnyOnlineParticipant(Match match) {
        return match.getTeams().stream().anyMatch(team -> !collectOnlineMembers(team).isEmpty());
    }

    private Set<UUID> collectOnlineMembers(Team team) {
        Set<UUID> onlineMembers = new LinkedHashSet<>();
        for (UUID memberUniqueId : team.getMemberUniqueIds()) {
            Player player = plugin.getServer().getPlayer(memberUniqueId);
            if (player != null && player.isOnline()) {
                onlineMembers.add(memberUniqueId);
            }
        }
        return onlineMembers;
    }

    private List<Player> collectOnlinePlayers(Team team) {
        List<Player> onlinePlayers = new ArrayList<>();
        for (UUID memberUniqueId : team.getMemberUniqueIds()) {
            Player player = plugin.getServer().getPlayer(memberUniqueId);
            if (player != null && player.isOnline()) {
                onlinePlayers.add(player);
            }
        }
        return onlinePlayers;
    }

    private void announceToMatch(Match match, String message) {
        forEachMatchAudience(match, player -> player.sendMessage(message));
        plugin.getLogger().info(message);
    }

    private void announceActionBarToMatch(Match match, String message) {
        Component actionBarText = Component.text(message);
        forEachMatchAudience(match, player -> player.sendActionBar(actionBarText));
    }

    private void announceTitleToMatch(Match match, String titleText, String subtitleText) {
        Title title = Title.title(Component.text(titleText), Component.text(subtitleText), SUDDEN_DEATH_TITLE_TIMES);
        forEachMatchAudience(match, player -> player.showTitle(title));
    }

    private void forEachMatchAudience(Match match, Consumer<Player> action) {
        Set<UUID> receivers = new LinkedHashSet<>();
        match.getTeams().forEach(team -> receivers.addAll(team.getMemberUniqueIds()));
        receivers.addAll(spectatorService.getSpectatorsOfArena(match.getArena().getArenaName()));
        for (UUID receiverUniqueId : receivers) {
            Player player = plugin.getServer().getPlayer(receiverUniqueId);
            if (player != null && player.isOnline()) {
                action.accept(player);
            }
        }
    }

    private record ScheduledRoundTimer(RoundTimer timer, BukkitTask task) { }
}
