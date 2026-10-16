package de.simplepvparena.match;

import de.simplepvparena.arena.ArenaTemporaryEntityRemover;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.gate.StartGateCountdown;
import de.simplepvparena.gate.StartGates;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class MatchService {

    private static final long DEFAULT_ROUND_RESET_DELAY_TICKS = 100L;
    private static final long ROUND_TIMER_PERIOD_TICKS = 20L;

    public static final boolean DEFAULT_SUDDEN_DEATH_DISABLED_IN_FINAL = true;

    private static final Title.Times SUDDEN_DEATH_TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(200L), Duration.ofSeconds(2L), Duration.ofMillis(500L));

    private static final Title.Times START_GATE_SECOND_TITLE_TIMES = Title.Times.times(
            Duration.ZERO, Duration.ofMillis(800L), Duration.ofMillis(200L));

    private static final Title.Times START_GATE_RELEASE_TITLE_TIMES = Title.Times.times(
            Duration.ZERO, Duration.ofMillis(1200L), Duration.ofMillis(400L));

    private final Plugin plugin;
    private final KitService kitService;
    private final SpectatorService spectatorService;
    private final FighterColours fighterColours;
    private final StartGates startGates;
    private final ArenaTemporaryEntityRemover temporaryEntityRemover;
    private final SuddenDeathTick suddenDeathTick = new SuddenDeathTick(new Random());

    private MatchCompletionHandler matchCompletionHandler = (match, winnerTeamName) -> { };
    private Match activeMatch;
    private ScheduledRoundTimer activeRoundTimer;
    private boolean insideSuddenDeathTick;
    private long roundResetDelayTicks = DEFAULT_ROUND_RESET_DELAY_TICKS;
    private RoundTimeoutSettings roundTimeoutSettings = RoundTimeoutSettings.defaults();
    private boolean suddenDeathDisabledInFinal = DEFAULT_SUDDEN_DEATH_DISABLED_IN_FINAL;
    private boolean roundTimersPaused;

    public MatchService(Plugin plugin, KitService kitService, SpectatorService spectatorService,
                        FighterColours fighterColours, StartGates startGates) {
        this(plugin, kitService, spectatorService, fighterColours, startGates, ArenaTemporaryEntityRemover.NONE);
    }

    public MatchService(Plugin plugin, KitService kitService, SpectatorService spectatorService,
                        FighterColours fighterColours, StartGates startGates,
                        ArenaTemporaryEntityRemover temporaryEntityRemover) {
        this.plugin = plugin;
        this.kitService = kitService;
        this.spectatorService = spectatorService;
        this.fighterColours = fighterColours;
        this.startGates = Objects.requireNonNull(startGates, "startGates");
        this.temporaryEntityRemover = Objects.requireNonNull(temporaryEntityRemover, "temporaryEntityRemover");
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

    public void setSuddenDeathDisabledInFinal(boolean suddenDeathDisabledInFinal) {
        this.suddenDeathDisabledInFinal = suddenDeathDisabledInFinal;
    }

    public boolean isSuddenDeathDisabledInFinal() {
        return suddenDeathDisabledInFinal;
    }

    public void startMatch(Match match) {
        if (activeMatch != null) {
            throw new IllegalStateException("match " + activeMatch.getMatchId() + " is still running in the arena");
        }
        activeMatch = match;
        announceToMatch(match, "Match " + match.getMatchId() + " starts: " + match.getFirstTeam().getTeamName()
                + " against " + match.getSecondTeam().getTeamName());
        beginRound(match);
    }

    public boolean hasActiveMatch() {
        return activeMatch != null;
    }

    public Optional<Match> getActiveMatch() {
        return Optional.ofNullable(activeMatch);
    }

    public Optional<Match> findMatchById(String matchId) {
        return getActiveMatch().filter(match -> match.getMatchId().equalsIgnoreCase(matchId));
    }

    public Optional<Match> findMatchOfPlayer(UUID playerUniqueId) {
        return getActiveMatch().filter(match -> match.containsPlayer(playerUniqueId));
    }

    public boolean isActiveFighter(UUID playerUniqueId) {
        return findMatchOfPlayer(playerUniqueId)
                .flatMap(Match::getRoundParticipants)
                .map(participants -> participants.isActive(playerUniqueId))
                .orElse(false);
    }

    public Optional<Location> findActiveFighterSpawn(UUID playerUniqueId) {
        if (!isActiveFighter(playerUniqueId)) {
            return Optional.empty();
        }
        return findMatchOfPlayer(playerUniqueId)
                .flatMap(match -> findTeamSpawn(match, playerUniqueId))
                .flatMap(PositionSnapshot::toLocation);
    }

    public void equipActiveFighter(Player player) {
        if (isActiveFighter(player.getUniqueId())) {
            kitService.applyKit(player);
        }
    }

    private static Optional<PositionSnapshot> findTeamSpawn(Match match, UUID playerUniqueId) {
        if (match.getFirstTeam().containsPlayer(playerUniqueId)) {
            return match.getArena().getFirstTeamSpawn();
        }
        return match.getArena().getSecondTeamSpawn();
    }

    public boolean isFriendlyFire(UUID attackerUniqueId, UUID victimUniqueId) {
        return findRunningRoundParticipants()
                .map(participants -> participants.isSameActiveTeam(attackerUniqueId, victimUniqueId))
                .orElse(false);
    }

    public boolean isPvpAllowed(UUID attackerUniqueId, UUID victimUniqueId) {
        return isPvpAllowedInRound(findRunningRoundParticipants().orElse(null), attackerUniqueId, victimUniqueId);
    }

    static boolean isPvpAllowedInRound(RoundParticipants roundParticipants, UUID attackerUniqueId,
                                       UUID victimUniqueId) {
        if (attackerUniqueId.equals(victimUniqueId)) {
            return true;
        }
        return roundParticipants != null
                && roundParticipants.isActive(attackerUniqueId)
                && roundParticipants.isActive(victimUniqueId);
    }

    public boolean isApplyingSuddenDeathDamage() {
        return insideSuddenDeathTick;
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
        if (insideSuddenDeathTick) {
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

    public void cancelActiveMatch() {
        if (activeMatch == null) {
            return;
        }
        Match cancelledMatch = activeMatch;
        stopRoundTimer();
        startGates.stopAndRestore();
        cancelledMatch.setState(MatchState.CANCELLED);
        activeMatch = null;
        releaseParticipants(cancelledMatch);
    }

    public void pauseRoundTimer() {
        roundTimersPaused = true;
        if (activeRoundTimer != null) {
            activeRoundTimer.timer().pause();
        }
    }

    public void resumeRoundTimer() {
        roundTimersPaused = false;
        if (activeRoundTimer != null) {
            activeRoundTimer.timer().resume();
        }
    }

    public boolean isRoundTimerPaused() {
        return roundTimersPaused;
    }

    public Optional<RoundTimer> findRoundTimer() {
        return Optional.ofNullable(activeRoundTimer).map(ScheduledRoundTimer::timer);
    }

    private Optional<RoundParticipants> findRunningRoundParticipants() {
        return getActiveMatch()
                .filter(match -> match.getState() == MatchState.RUNNING)
                .flatMap(Match::getRoundParticipants);
    }

    private void beginRound(Match match) {
        removeTemporaryEntities(match);
        Set<UUID> firstTeamPlayers = collectOnlineMembers(match.getFirstTeam());
        Set<UUID> secondTeamPlayers = collectOnlineMembers(match.getSecondTeam());
        match.beginRound(firstTeamPlayers, secondTeamPlayers);

        preparePlayers(match.getFirstTeam(), match.getArena().getFirstTeamSpawn());
        preparePlayers(match.getSecondTeam(), match.getArena().getSecondTeamSpawn());
        fighterColours.colourFighters(match);
        startGates.openAtRoundStart(remainingSeconds -> announceStartGateSecond(match, remainingSeconds));
        boolean timeLimited = startRoundTimer(match);
        announceToMatch(match, "Round " + match.getRoundNumber() + " of match " + match.getMatchId()
                + " has started (" + match.getScore().describe() + "), "
                + describeRoundLimit(timeLimited, roundTimeoutSettings.roundTimeoutSeconds()));

        Optional<RoundOutcome> immediateOutcome = match.getRoundParticipants().orElseThrow().determineOutcome();
        if (immediateOutcome.isPresent()) {
            concludeRound(match, immediateOutcome.get());
        }
    }

    private void removeTemporaryEntities(Match match) {
        try {
            match.getArena().getShape().ifPresent(shape -> temporaryEntityRemover.removeTemporaryEntities(
                    plugin.getServer().getWorld(shape.getWorldName()), shape));
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING, "The temporary entities of the arena could not be removed, "
                    + "the round starts without that", failure);
        }
    }

    private void concludeRound(Match match, RoundOutcome outcome) {
        stopRoundTimer();
        startGates.stopAndRestore();
        if (outcome.isDraw()) {
            match.setState(MatchState.BETWEEN_ROUNDS);
            for (String drawMessage : buildDrawMessages(match.getRoundNumber(), match.getMatchId(), outcome)) {
                announceToMatch(match, drawMessage);
            }
            if (hasAnyOnlineParticipant(match)) {
                scheduleNextRound(match);
                return;
            }
            announceWarningToMatch(match, "Match " + match.getMatchId()
                    + " is halted, no participant is online"
                    + TournamentMessages.LINE_BREAK
                    + "Use \"Force a result\" in the match menu of /tournament to resolve it");
            return;
        }
        String winnerTeamName = outcome.winnerTeamName().orElseThrow();
        match.getScore().awardPoint(winnerTeamName);
        announceSuccessToMatch(match, winnerTeamName + " scores a point (" + match.getScore().describe() + ")");
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
        stopRoundTimer();
        startGates.stopAndRestore();
        match.setState(MatchState.FINISHED);
        activeMatch = null;
        announceSuccessToMatch(match, "Match " + match.getMatchId() + " is over, " + winnerTeamName + " wins ("
                + match.getScore().describe() + ")");
        releaseParticipants(match);
        matchCompletionHandler.onMatchCompleted(match, winnerTeamName);
    }

    private boolean startRoundTimer(Match match) {
        stopRoundTimer();
        if (match.isFinalMatch() && suddenDeathDisabledInFinal) {
            return false;
        }
        RoundTimer roundTimer = new RoundTimer(roundTimeoutSettings);
        if (roundTimersPaused) {
            roundTimer.pause();
        }
        BukkitTask timerTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> handleRoundTimerTick(match), ROUND_TIMER_PERIOD_TICKS, ROUND_TIMER_PERIOD_TICKS);
        activeRoundTimer = new ScheduledRoundTimer(roundTimer, timerTask);
        return true;
    }

    static String describeRoundLimit(boolean timeLimited, int roundTimeoutSeconds) {
        if (timeLimited) {
            return "time limit " + roundTimeoutSeconds + " seconds";
        }
        return "no time limit, the round runs until one of the teams falls";
    }

    private void stopRoundTimer() {
        if (activeRoundTimer != null) {
            activeRoundTimer.task().cancel();
            activeRoundTimer = null;
        }
    }

    private void handleRoundTimerTick(Match match) {
        if (activeRoundTimer == null || match.getState() != MatchState.RUNNING) {
            return;
        }
        RoundTimer roundTimer = activeRoundTimer.timer();
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
        announceWarningToMatch(match, "The time limit of round " + match.getRoundNumber() + " in match "
                + match.getMatchId() + " is over, sudden death has started");
    }

    private void applySuddenDeathDamage(Match match, double damageAmount) {
        Optional<RoundParticipants> roundParticipants = match.getRoundParticipants();
        if (roundParticipants.isEmpty()) {
            return;
        }
        Optional<RoundOutcome> outcome;
        insideSuddenDeathTick = true;
        try {
            outcome = suddenDeathTick.apply(roundParticipants.get(), damageAmount,
                    activePlayerUniqueId -> damagePlayer(activePlayerUniqueId, damageAmount));
        } finally {
            insideSuddenDeathTick = false;
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

    private void preparePlayers(Team team, Optional<PositionSnapshot> spawnPosition) {
        Optional<Location> spawnLocation = spawnPosition.flatMap(PositionSnapshot::toLocation);
        for (Player player : collectOnlinePlayers(team)) {
            spectatorService.clearAssignment(player.getUniqueId());
            spawnLocation.ifPresent(player::teleport);
            kitService.applyKit(player);
        }
        if (spawnLocation.isEmpty()) {
            plugin.getLogger().warning("The arena has no usable spawn position for team " + team.getTeamName());
        }
    }

    private void releaseParticipants(Match match) {
        for (Team team : match.getTeams()) {
            for (Player player : collectOnlinePlayers(team)) {
                try {
                    kitService.clearKit(player);
                } catch (RuntimeException failure) {
                    plugin.getLogger().log(Level.WARNING, "The participant " + player.getName()
                            + " could not be reset after match " + match.getMatchId()
                            + ", they are sent to the tribune anyway", failure);
                }
                try {
                    spectatorService.sendToTribune(player);
                } catch (RuntimeException failure) {
                    plugin.getLogger().log(Level.WARNING, "The participant " + player.getName()
                            + " could not be sent to the tribune after match " + match.getMatchId()
                            + ", the remaining participants are released anyway", failure);
                }
            }
        }
        fighterColours.clearColours();
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
        announceToMatch(match, TournamentMessages.info(message), message);
    }

    private void announceSuccessToMatch(Match match, String message) {
        announceToMatch(match, TournamentMessages.success(message), message);
    }

    private void announceWarningToMatch(Match match, String message) {
        announceToMatch(match, TournamentMessages.warning(message), message);
    }

    private void announceToMatch(Match match, Component message, String logMessage) {
        forEachMatchAudience(match, player -> player.sendMessage(message));
        plugin.getLogger().info(logMessage);
    }

    private void announceActionBarToMatch(Match match, String message) {
        Component actionBarText = Component.text(message, TournamentMessages.WARNING_COLOUR);
        forEachMatchAudience(match, player -> player.sendActionBar(actionBarText));
    }

    private void announceStartGateSecond(Match match, int remainingSeconds) {
        boolean release = StartGateCountdown.isRelease(remainingSeconds);
        Title title = Title.title(
                Component.text(StartGateCountdown.describeTitle(remainingSeconds),
                        release ? TournamentMessages.SUCCESS_COLOUR : TournamentMessages.WARNING_COLOUR),
                Component.text(StartGateCountdown.describeSubtitle(remainingSeconds),
                        TournamentMessages.DETAIL_COLOUR),
                release ? START_GATE_RELEASE_TITLE_TIMES : START_GATE_SECOND_TITLE_TIMES);
        forEachMatchAudience(match, player -> player.showTitle(title));
    }

    private void announceTitleToMatch(Match match, String titleText, String subtitleText) {
        Title title = Title.title(Component.text(titleText, TournamentMessages.ERROR_COLOUR),
                Component.text(subtitleText, TournamentMessages.DETAIL_COLOUR), SUDDEN_DEATH_TITLE_TIMES);
        forEachMatchAudience(match, player -> player.showTitle(title));
    }

    private void forEachMatchAudience(Match match, Consumer<Player> action) {
        Set<UUID> receivers = new LinkedHashSet<>();
        match.getTeams().forEach(team -> receivers.addAll(team.getMemberUniqueIds()));
        receivers.addAll(spectatorService.getSpectators());
        for (UUID receiverUniqueId : receivers) {
            Player player = plugin.getServer().getPlayer(receiverUniqueId);
            if (player != null && player.isOnline()) {
                action.accept(player);
            }
        }
    }

    private record ScheduledRoundTimer(RoundTimer timer, BukkitTask task) { }
}
