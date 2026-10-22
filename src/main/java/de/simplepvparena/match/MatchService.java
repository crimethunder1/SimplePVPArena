package de.simplepvparena.match;

import de.simplepvparena.arena.ArenaAudience;
import de.simplepvparena.arena.ArenaTemporaryEntityRemover;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.gate.StartGateCountdown;
import de.simplepvparena.gate.StartGates;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.message.MatchMenuMessage;
import de.simplepvparena.message.MatchMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.message.UnequalTeamVoteMessage;
import de.simplepvparena.team.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
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
import java.util.logging.Level;

public final class MatchService {

    private static final long DEFAULT_ROUND_RESET_DELAY_TICKS = 100L;
    private static final long ROUND_TIMER_PERIOD_TICKS = 20L;
    private static final long TICKS_PER_SECOND = 20L;

    public static final boolean DEFAULT_SUDDEN_DEATH_DISABLED_IN_FINAL = true;

    public static final int DEFAULT_RECONNECT_SECONDS = 60;
    public static final int MINIMUM_RECONNECT_SECONDS = 0;
    public static final int MAXIMUM_RECONNECT_SECONDS = 600;

    public static final int DEFAULT_COUNTDOWN_SECONDS = 3;
    public static final int MINIMUM_COUNTDOWN_SECONDS = 0;
    public static final int MAXIMUM_COUNTDOWN_SECONDS = 10;

    private static final int BLINDNESS_SECONDS_AFTER_THE_COUNTDOWN = 1;

    private static final Title.Times SUDDEN_DEATH_TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(200L), Duration.ofSeconds(2L), Duration.ofMillis(500L));

    private static final Title.Times START_GATE_SECOND_TITLE_TIMES = Title.Times.times(
            Duration.ZERO, Duration.ofMillis(800L), Duration.ofMillis(200L));

    private static final Title.Times START_GATE_RELEASE_TITLE_TIMES = Title.Times.times(
            Duration.ZERO, Duration.ofMillis(1200L), Duration.ofMillis(400L));

    private static final long SCORE_TITLE_FADE_IN_TICKS = 10L;
    private static final long SCORE_TITLE_STAY_TICKS = 50L;
    private static final long SCORE_TITLE_FADE_OUT_TICKS = 10L;

    private static final Title.Times SCORE_TITLE_TIMES = Title.Times.times(
            Ticks.duration(SCORE_TITLE_FADE_IN_TICKS), Ticks.duration(SCORE_TITLE_STAY_TICKS),
            Ticks.duration(SCORE_TITLE_FADE_OUT_TICKS));

    private final Plugin plugin;
    private final KitService kitService;
    private final SpectatorService spectatorService;
    private final FighterColours fighterColours;
    private final StartGates startGates;
    private final ArenaTemporaryEntityRemover temporaryEntityRemover;
    private final SuddenDeathTick suddenDeathTick = new SuddenDeathTick(new Random());
    private final Map<UUID, ReconnectWindow> reconnectWindows = new LinkedHashMap<>();
    private final Set<UUID> reconnectWindowsUsedThisRound = new HashSet<>();

    private MatchCompletionHandler matchCompletionHandler = (match, winnerTeamName) -> { };
    private MatchCompletionHandler autoMatchCompletionHandler = (match, winnerTeamName) -> { };
    private RoundStartEffects roundStartEffects = new VanillaRoundStartEffects();
    private CountdownImages countdownImages = CountdownImages.NONE;
    private ArenaAudience arenaAudience;
    private Match activeMatch;
    private ScheduledRoundTimer activeRoundTimer;
    private StartAnimation activeStartAnimation;
    private boolean insideSuddenDeathTick;
    private long roundResetDelayTicks = DEFAULT_ROUND_RESET_DELAY_TICKS;
    private RoundTimeoutSettings roundTimeoutSettings = RoundTimeoutSettings.defaults();
    private boolean suddenDeathDisabledInFinal = DEFAULT_SUDDEN_DEATH_DISABLED_IN_FINAL;
    private int reconnectSeconds = DEFAULT_RECONNECT_SECONDS;
    private int countdownSeconds = DEFAULT_COUNTDOWN_SECONDS;
    private UnequalTeamSettings unequalTeamSettings = UnequalTeamSettings.defaults();
    private UnequalTeamVoteService unequalTeamVoteService = new UnequalTeamVoteService();
    private boolean roundTimersPaused;

    public MatchService(Plugin plugin, KitService kitService, SpectatorService spectatorService,
                        FighterColours fighterColours, StartGates startGates) {
        this(plugin, kitService, spectatorService, fighterColours, startGates, ArenaTemporaryEntityRemover.NONE);
    }

    public MatchService(Plugin plugin, KitService kitService, SpectatorService spectatorService,
                        FighterColours fighterColours, StartGates startGates,
                        ArenaTemporaryEntityRemover temporaryEntityRemover) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.kitService = Objects.requireNonNull(kitService, "kitService");
        this.spectatorService = Objects.requireNonNull(spectatorService, "spectatorService");
        this.fighterColours = Objects.requireNonNull(fighterColours, "fighterColours");
        this.startGates = Objects.requireNonNull(startGates, "startGates");
        this.temporaryEntityRemover = Objects.requireNonNull(temporaryEntityRemover, "temporaryEntityRemover");
    }

    public static String describeMatchMenuTile(MatchMenuMessage tileTitle) {
        return Messages.text(MatchMessage.MENU_REFERENCE, Placeholder.of("tile", Messages.text(tileTitle)));
    }

    public void setMatchCompletionHandler(MatchCompletionHandler matchCompletionHandler) {
        this.matchCompletionHandler = matchCompletionHandler;
    }

    public void setAutoMatchCompletionHandler(MatchCompletionHandler autoMatchCompletionHandler) {
        this.autoMatchCompletionHandler = Objects.requireNonNull(autoMatchCompletionHandler,
                "autoMatchCompletionHandler");
    }

    public void setArenaAudience(ArenaAudience arenaAudience) {
        this.arenaAudience = Objects.requireNonNull(arenaAudience, "arenaAudience");
    }

    public void setRoundStartEffects(RoundStartEffects roundStartEffects) {
        this.roundStartEffects = Objects.requireNonNull(roundStartEffects, "roundStartEffects");
    }

    public void setCountdownImages(CountdownImages countdownImages) {
        this.countdownImages = Objects.requireNonNull(countdownImages, "countdownImages");
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

    public void setReconnectSeconds(int reconnectSeconds) {
        this.reconnectSeconds = Math.clamp(reconnectSeconds, MINIMUM_RECONNECT_SECONDS, MAXIMUM_RECONNECT_SECONDS);
    }

    public int getReconnectSeconds() {
        return reconnectSeconds;
    }

    public void setCountdownSeconds(int countdownSeconds) {
        this.countdownSeconds = Math.clamp(countdownSeconds, MINIMUM_COUNTDOWN_SECONDS, MAXIMUM_COUNTDOWN_SECONDS);
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public void setUnequalTeamSettings(UnequalTeamSettings unequalTeamSettings) {
        this.unequalTeamSettings = Objects.requireNonNull(unequalTeamSettings, "unequalTeamSettings");
    }

    public UnequalTeamSettings getUnequalTeamSettings() {
        return unequalTeamSettings;
    }

    public void setUnequalTeamVoteService(UnequalTeamVoteService unequalTeamVoteService) {
        this.unequalTeamVoteService = Objects.requireNonNull(unequalTeamVoteService, "unequalTeamVoteService");
    }

    public UnequalTeamVoteService getUnequalTeamVoteService() {
        return unequalTeamVoteService;
    }

    public void startMatch(Match match) {
        if (activeMatch != null) {
            throw new IllegalStateException("match " + activeMatch.getMatchId() + " is still running in the arena");
        }
        activeMatch = match;
        unequalTeamVoteService.begin(match, unequalTeamSettings, () -> beginFirstRound(match));
    }

    private void beginFirstRound(Match match) {
        if (activeMatch != match) {
            return;
        }
        match.findTeamWithChosenFighters().ifPresent(team -> announceChosenFighters(match, team));
        beginRound(match);
    }

    private void announceChosenFighters(Match match, Team team) {
        List<String> fighterNames = new ArrayList<>();
        for (UUID fighterUniqueId : match.findChosenFighters(team).orElseThrow()) {
            team.getMembers().stream()
                    .filter(member -> member.uniqueId().equals(fighterUniqueId))
                    .findFirst()
                    .ifPresent(member -> fighterNames.add(member.playerName()));
        }
        if (fighterNames.isEmpty()) {
            return;
        }
        announceToMatch(match, Messages.text(UnequalTeamVoteMessage.RESULT, Placeholder.of("team", team.getTeamName()),
                Placeholder.of("players", Messages.join(fighterNames))));
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

    public boolean isStartAnimationRunning() {
        return activeStartAnimation != null && activeStartAnimation.running;
    }

    public void handlePlayerDisconnected(Player player) {
        UUID playerUniqueId = player.getUniqueId();
        unequalTeamVoteService.handlePlayerLeft(playerUniqueId);
        Optional<Match> runningMatch = findRunningMatchOfActiveFighter(playerUniqueId);
        if (reconnectSeconds <= 0 || runningMatch.isEmpty() || isSuddenDeathRunning()
                || reconnectWindowsUsedThisRound.contains(playerUniqueId)) {
            handlePlayerEliminated(playerUniqueId);
            return;
        }
        reconnectWindowsUsedThisRound.add(playerUniqueId);
        openReconnectWindow(runningMatch.get(), playerUniqueId, player.getName());
        removeStartAnimationBlindness(playerUniqueId);
    }

    public boolean handlePlayerReturned(Player player) {
        UUID playerUniqueId = player.getUniqueId();
        if (!closeReconnectWindow(playerUniqueId)) {
            return false;
        }
        Optional<Match> runningMatch = findRunningMatchOfActiveFighter(playerUniqueId);
        if (runningMatch.isEmpty()) {
            return false;
        }
        announceToMatch(runningMatch.get(), Messages.text(MatchMessage.RECONNECT_RETURNED,
                Placeholder.of("player", player.getName())));
        if (isStartAnimationRunning()) {
            blindFighter(activeStartAnimation, player);
        }
        return true;
    }

    public boolean isWaitingForReconnect(UUID playerUniqueId) {
        return reconnectWindows.containsKey(playerUniqueId);
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
        removeStartAnimationBlindness(playerUniqueId);
        closeReconnectWindow(playerUniqueId);
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
        finishMatch(match.get(), winningTeam.get().getTeamName(), true);
        return true;
    }

    public void cancelActiveMatch() {
        if (activeMatch == null) {
            return;
        }
        Match cancelledMatch = activeMatch;
        unequalTeamVoteService.cancel();
        stopRoundTimer();
        stopStartAnimation();
        closeReconnectWindows();
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

    private Optional<Match> findRunningMatchOfActiveFighter(UUID playerUniqueId) {
        return getActiveMatch()
                .filter(match -> match.getState() == MatchState.RUNNING)
                .filter(match -> match.getRoundParticipants()
                        .map(participants -> participants.isActive(playerUniqueId))
                        .orElse(false));
    }

    private void openReconnectWindow(Match match, UUID playerUniqueId, String playerName) {
        closeReconnectWindow(playerUniqueId);
        int windowSeconds = reconnectSeconds;
        BukkitTask expiryTask = plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> expireReconnectWindow(match, playerUniqueId), windowSeconds * TICKS_PER_SECOND);
        reconnectWindows.put(playerUniqueId, new ReconnectWindow(playerName, expiryTask));
        announceWarningToMatch(match, Messages.plural(MatchMessage.RECONNECT_CONNECTION_LOST, windowSeconds,
                Placeholder.of("player", playerName)));
    }

    private void expireReconnectWindow(Match match, UUID playerUniqueId) {
        ReconnectWindow window = reconnectWindows.remove(playerUniqueId);
        if (window == null || findRunningMatchOfActiveFighter(playerUniqueId).filter(match::equals).isEmpty()) {
            return;
        }
        announceWarningToMatch(match, Messages.text(MatchMessage.RECONNECT_NOT_RETURNED,
                Placeholder.of("player", window.playerName())));
        handlePlayerEliminated(playerUniqueId);
    }

    private boolean closeReconnectWindow(UUID playerUniqueId) {
        ReconnectWindow window = reconnectWindows.remove(playerUniqueId);
        if (window == null) {
            return false;
        }
        window.expiryTask().cancel();
        return true;
    }

    private void closeReconnectWindows() {
        for (ReconnectWindow window : List.copyOf(reconnectWindows.values())) {
            window.expiryTask().cancel();
        }
        reconnectWindows.clear();
    }

    private void beginRound(Match match) {
        closeReconnectWindows();
        reconnectWindowsUsedThisRound.clear();
        stopStartAnimation();
        removeTemporaryEntities(match);
        Set<UUID> firstTeamPlayers = collectFighters(match, match.getFirstTeam());
        Set<UUID> secondTeamPlayers = collectFighters(match, match.getSecondTeam());
        match.beginRound(firstTeamPlayers, secondTeamPlayers);

        preparePlayers(match.getFirstTeam(), firstTeamPlayers, match.getArena().getFirstTeamSpawn());
        preparePlayers(match.getSecondTeam(), secondTeamPlayers, match.getArena().getSecondTeamSpawn());
        fighterColours.colourFighters(match);
        int roundCountdownSeconds = countdownSeconds;
        startGates.openAtRoundStart(remainingSeconds -> announceStartGateSecond(match, remainingSeconds));
        int gateCountdownSeconds = startGates.getCountdownSeconds();
        if (roundCountdownSeconds == 0) {
            startRoundTimer(match);
        }
        announceToMatch(match, describeRoundStart(match.getRoundNumber(), match.getMatchId(),
                match.getScore().describe(), isTimeLimited(match), roundTimeoutSettings.roundTimeoutSeconds()));

        Optional<RoundOutcome> immediateOutcome = match.getRoundParticipants().orElseThrow().determineOutcome();
        if (immediateOutcome.isPresent()) {
            concludeRound(match, immediateOutcome.get());
            return;
        }
        if (roundCountdownSeconds > 0) {
            scheduleStartAnimation(match, roundCountdownSeconds, gateCountdownSeconds);
        }
    }

    private void scheduleStartAnimation(Match match, int roundCountdownSeconds, int gateCountdownSeconds) {
        boolean gateCounting = gateCountdownSeconds > 0;
        int animationSeconds = gateCounting
                ? Math.min(roundCountdownSeconds, gateCountdownSeconds)
                : roundCountdownSeconds;
        StartAnimation animation = new StartAnimation(match, animationSeconds, !gateCounting);
        activeStartAnimation = animation;
        long delayTicks = gateCounting ? (gateCountdownSeconds - animationSeconds) * TICKS_PER_SECOND : 0L;
        if (delayTicks == 0L) {
            beginStartAnimation(animation);
            return;
        }
        animation.tasks.add(plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> beginStartAnimation(animation), delayTicks));
    }

    private void beginStartAnimation(StartAnimation animation) {
        if (activeStartAnimation != animation) {
            return;
        }
        animation.running = true;
        for (int elapsedSeconds = 1; elapsedSeconds <= animation.totalSeconds; elapsedSeconds++) {
            int remainingSeconds = animation.totalSeconds - elapsedSeconds;
            animation.tasks.add(plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> continueStartAnimation(animation, remainingSeconds), elapsedSeconds * TICKS_PER_SECOND));
        }
        if (animation.showsOwnTitles) {
            announceCountdownSecond(animation.match, animation.remainingSeconds);
        }
        for (Player fighter : collectOnlineActiveFighters(animation.match)) {
            blindFighter(animation, fighter);
        }
    }

    private void continueStartAnimation(StartAnimation animation, int remainingSeconds) {
        if (activeStartAnimation != animation) {
            return;
        }
        animation.remainingSeconds = remainingSeconds;
        if (!StartGateCountdown.isRelease(remainingSeconds)) {
            if (animation.showsOwnTitles) {
                announceCountdownSecond(animation.match, remainingSeconds);
            }
            return;
        }
        releaseStartAnimation(animation);
    }

    private void releaseStartAnimation(StartAnimation animation) {
        Match match = animation.match;
        activeStartAnimation = null;
        startRoundTimer(match);
        if (animation.showsOwnTitles) {
            announceCountdownSecond(match, StartGateCountdown.RELEASE_SECOND);
        }
        removeBlindness(animation);
        for (Player fighter : collectOnlineActiveFighters(match)) {
            roundStartEffects.playFightStartAnimation(fighter);
        }
    }

    private void blindFighter(StartAnimation animation, Player fighter) {
        animation.blindedFighters.add(fighter.getUniqueId());
        int durationTicks = (int) ((animation.remainingSeconds + BLINDNESS_SECONDS_AFTER_THE_COUNTDOWN)
                * TICKS_PER_SECOND);
        roundStartEffects.blind(fighter, durationTicks);
    }

    private void removeStartAnimationBlindness(UUID playerUniqueId) {
        StartAnimation animation = activeStartAnimation;
        if (animation == null || !animation.blindedFighters.remove(playerUniqueId)) {
            return;
        }
        Player player = plugin.getServer().getPlayer(playerUniqueId);
        if (player != null && player.isOnline()) {
            roundStartEffects.removeBlindness(player);
        }
    }

    private void stopStartAnimation() {
        StartAnimation animation = activeStartAnimation;
        if (animation == null) {
            return;
        }
        activeStartAnimation = null;
        for (BukkitTask task : animation.tasks) {
            task.cancel();
        }
        removeBlindness(animation);
    }

    private void removeBlindness(StartAnimation animation) {
        for (UUID blindedFighterUniqueId : List.copyOf(animation.blindedFighters)) {
            animation.blindedFighters.remove(blindedFighterUniqueId);
            Player player = plugin.getServer().getPlayer(blindedFighterUniqueId);
            if (player != null && player.isOnline()) {
                roundStartEffects.removeBlindness(player);
            }
        }
    }

    private List<Player> collectOnlineActiveFighters(Match match) {
        List<Player> fighters = new ArrayList<>();
        for (UUID fighterUniqueId : match.getRoundParticipants().orElseThrow().getAllActivePlayers()) {
            Player player = plugin.getServer().getPlayer(fighterUniqueId);
            if (player != null && player.isOnline()) {
                fighters.add(player);
            }
        }
        return fighters;
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
        stopStartAnimation();
        startGates.stopAndRestore();
        if (outcome.isDraw()) {
            match.setState(MatchState.BETWEEN_ROUNDS);
            for (String drawMessage : buildDrawMessages(match.getRoundNumber(), match.getMatchId(), outcome)) {
                announceToMatch(match, drawMessage);
            }
            if (hasAnyOnlineParticipant(match)) {
                announceRoundTitle(match, Messages.text(MatchMessage.ROUND_DRAW_TITLE,
                        Placeholder.of("round", match.getRoundNumber())));
                scheduleNextRound(match);
                return;
            }
            announceWarningToMatch(match, Messages.text(MatchMessage.HALTED,
                    Placeholder.of("match", match.getMatchId()),
                    Placeholder.of("action", describeMatchMenuTile(MatchMenuMessage.FORCE_RESULT_TITLE))));
            return;
        }
        String winnerTeamName = outcome.winnerTeamName().orElseThrow();
        match.getScore().awardPoint(winnerTeamName);
        announceSuccessToMatch(match, Messages.text(MatchMessage.POINT_SCORED, Placeholder.of("team", winnerTeamName),
                Placeholder.of("score", match.getScore().describe())));
        Optional<String> matchWinner = match.getScore().getWinnerTeamName();
        if (matchWinner.isPresent()) {
            finishMatch(match, matchWinner.get(), false);
            return;
        }
        match.setState(MatchState.BETWEEN_ROUNDS);
        announceRoundTitle(match, Messages.text(MatchMessage.ROUND_WON_TITLE, Placeholder.of("team", winnerTeamName),
                Placeholder.of("round", match.getRoundNumber())));
        scheduleNextRound(match);
    }

    static List<String> buildDrawMessages(int roundNumber, String matchId, RoundOutcome outcome) {
        Placeholder round = Placeholder.of("round", roundNumber);
        Placeholder match = Placeholder.of("match", matchId);
        String drawMessage = Messages.text(MatchMessage.ROUND_DRAW, round, match);
        if (!outcome.isSuddenDeathDraw()) {
            return List.of(drawMessage);
        }
        return List.of(drawMessage, Messages.text(MatchMessage.SUDDEN_DEATH_DRAW, round, match));
    }

    private void scheduleNextRound(Match match) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (match.getState() == MatchState.BETWEEN_ROUNDS) {
                beginRound(match);
            }
        }, roundResetDelayTicks);
    }

    private void finishMatch(Match match, String winnerTeamName, boolean decidedByAnAdmin) {
        unequalTeamVoteService.cancel();
        stopRoundTimer();
        stopStartAnimation();
        closeReconnectWindows();
        startGates.stopAndRestore();
        match.setState(MatchState.FINISHED);
        activeMatch = null;
        announceSuccessToMatch(match, Messages.text(MatchMessage.OVER, Placeholder.of("match", match.getMatchId()),
                Placeholder.of("team", winnerTeamName), Placeholder.of("score", match.getScore().describe())));
        releaseParticipants(match);
        announceMatchTitle(match, winnerTeamName, decidedByAnAdmin);
        MatchCompletionHandler completionHandler = match.isAutoMatch()
                ? autoMatchCompletionHandler
                : matchCompletionHandler;
        completionHandler.onMatchCompleted(match, winnerTeamName);
    }

    private boolean isTimeLimited(Match match) {
        return !(match.isFinalMatch() && suddenDeathDisabledInFinal);
    }

    private void startRoundTimer(Match match) {
        stopRoundTimer();
        if (!isTimeLimited(match)) {
            return;
        }
        RoundTimer roundTimer = new RoundTimer(roundTimeoutSettings);
        if (roundTimersPaused) {
            roundTimer.pause();
        }
        BukkitTask timerTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> handleRoundTimerTick(match), ROUND_TIMER_PERIOD_TICKS, ROUND_TIMER_PERIOD_TICKS);
        activeRoundTimer = new ScheduledRoundTimer(roundTimer, timerTask);
    }

    static String describeRoundStart(int roundNumber, String matchId, String score, boolean timeLimited,
                                     int roundTimeoutSeconds) {
        Placeholder round = Placeholder.of("round", roundNumber);
        Placeholder match = Placeholder.of("match", matchId);
        Placeholder scoreValue = Placeholder.of("score", score);
        if (timeLimited) {
            return Messages.plural(MatchMessage.ROUND_STARTED_WITH_TIME_LIMIT, roundTimeoutSeconds, round, match,
                    scoreValue);
        }
        return Messages.text(MatchMessage.ROUND_STARTED_WITHOUT_TIME_LIMIT, round, match, scoreValue);
    }

    private boolean isSuddenDeathRunning() {
        return activeRoundTimer != null && activeRoundTimer.timer().isSuddenDeathActive();
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
                    Messages.plural(MatchMessage.SUDDEN_DEATH_WARNING, roundTimer.getRemainingSeconds()));
            case SUDDEN_DEATH_STARTED -> announceSuddenDeathStart(match);
            case SUDDEN_DEATH_DAMAGE -> applySuddenDeathDamage(match, roundTimer.getCurrentSuddenDeathDamage());
            case RUNNING, PAUSED -> { }
        }
    }

    private void announceSuddenDeathStart(Match match) {
        announceTitleToMatch(match, Messages.text(MatchMessage.SUDDEN_DEATH_TITLE),
                Messages.text(MatchMessage.SUDDEN_DEATH_SUBTITLE));
        announceWarningToMatch(match, Messages.text(MatchMessage.SUDDEN_DEATH_STARTED,
                Placeholder.of("round", match.getRoundNumber()), Placeholder.of("match", match.getMatchId())));
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

    private void preparePlayers(Team team, Set<UUID> fighterUniqueIds, Optional<PositionSnapshot> spawnPosition) {
        Optional<Location> spawnLocation = spawnPosition.flatMap(PositionSnapshot::toLocation);
        for (Player player : collectOnlinePlayers(fighterUniqueIds)) {
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
            for (Player player : collectOnlinePlayers(collectMembersWhoMayFight(match, team))) {
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
        return match.getTeams().stream().anyMatch(team -> !collectFighters(match, team).isEmpty());
    }

    private Set<UUID> collectFighters(Match match, Team team) {
        Set<UUID> onlineMembers = new LinkedHashSet<>();
        for (UUID memberUniqueId : collectMembersWhoMayFight(match, team)) {
            Player player = plugin.getServer().getPlayer(memberUniqueId);
            if (player != null && player.isOnline()) {
                onlineMembers.add(memberUniqueId);
            }
        }
        return onlineMembers;
    }

    private static Set<UUID> collectMembersWhoMayFight(Match match, Team team) {
        return match.findChosenFighters(team)
                .<Set<UUID>>map(LinkedHashSet::new)
                .orElseGet(() -> new LinkedHashSet<>(team.getMemberUniqueIds()));
    }

    private List<Player> collectOnlinePlayers(Set<UUID> memberUniqueIds) {
        List<Player> onlinePlayers = new ArrayList<>();
        for (UUID memberUniqueId : memberUniqueIds) {
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
        forEachAreaAudience(match, player -> player.sendMessage(message));
        plugin.getLogger().info(logMessage);
    }

    private void announceActionBarToMatch(Match match, String message) {
        Component actionBarText = Component.text(message, TournamentMessages.WARNING_COLOUR);
        forEachAreaAudience(match, player -> player.sendActionBar(actionBarText));
    }

    private void announceStartGateSecond(Match match, int remainingSeconds) {
        showCountdownTitle(match, remainingSeconds, Component.text(StartGateCountdown.describeSubtitle(remainingSeconds),
                TournamentMessages.DETAIL_COLOUR));
    }

    private void announceCountdownSecond(Match match, int remainingSeconds) {
        showCountdownTitle(match, remainingSeconds, Component.empty());
    }

    private void showCountdownTitle(Match match, int remainingSeconds, Component subtitle) {
        boolean release = StartGateCountdown.isRelease(remainingSeconds);
        Component headline = countdownImages.findImage(remainingSeconds)
                .<Component>map(Component::text)
                .orElseGet(() -> Component.text(StartGateCountdown.describeTitle(remainingSeconds),
                        release ? TournamentMessages.SUCCESS_COLOUR : TournamentMessages.WARNING_COLOUR));
        Title title = Title.title(headline, subtitle,
                release ? START_GATE_RELEASE_TITLE_TIMES : START_GATE_SECOND_TITLE_TIMES);
        forEachAreaAudience(match, player -> player.showTitle(title));
    }

    private void announceTitleToMatch(Match match, String titleText, String subtitleText) {
        Title title = Title.title(Component.text(titleText, TournamentMessages.ERROR_COLOUR),
                Component.text(subtitleText, TournamentMessages.DETAIL_COLOUR), SUDDEN_DEATH_TITLE_TIMES);
        forEachAreaAudience(match, player -> player.showTitle(title));
    }

    private void announceRoundTitle(Match match, String roundResult) {
        showScoreTitle(match, Component.text(match.getScore().describe(), TournamentMessages.INFO_COLOUR),
                roundResult);
    }

    private void announceMatchTitle(Match match, String winnerTeamName, boolean decidedByAnAdmin) {
        MatchMessage headlineKey = match.isFinalMatch() ? MatchMessage.FINAL_WON_TITLE : MatchMessage.MATCH_WON_TITLE;
        String headline = Messages.text(headlineKey, Placeholder.of("team", winnerTeamName));
        Placeholder matchId = Placeholder.of("match", match.getMatchId());
        String result = decidedByAnAdmin
                ? Messages.text(MatchMessage.DECIDED_BY_AN_ADMIN_SUBTITLE, matchId)
                : Messages.text(MatchMessage.RESULT_SUBTITLE, matchId,
                        Placeholder.of("score", match.getScore().describe()));
        showScoreTitle(match, Component.text(headline, TournamentMessages.SUCCESS_COLOUR), result);
    }

    private void showScoreTitle(Match match, Component headline, String detail) {
        Title title = Title.title(headline, Component.text(detail, TournamentMessages.DETAIL_COLOUR),
                SCORE_TITLE_TIMES);
        forEachAreaAudience(match, player -> player.showTitle(title));
    }

    private void forEachAreaAudience(Match match, Consumer<Player> action) {
        if (arenaAudience == null) {
            forEachMatchAudience(match, action);
            return;
        }
        arenaAudience.findMatchAudience(match.getTeams()).forEach(action);
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

    private record ReconnectWindow(String playerName, BukkitTask expiryTask) { }

    private static final class StartAnimation {

        private final Match match;
        private final int totalSeconds;
        private final boolean showsOwnTitles;
        private final List<BukkitTask> tasks = new ArrayList<>();
        private final Set<UUID> blindedFighters = new LinkedHashSet<>();

        private int remainingSeconds;
        private boolean running;

        private StartAnimation(Match match, int totalSeconds, boolean showsOwnTitles) {
            this.match = match;
            this.totalSeconds = totalSeconds;
            this.showsOwnTitles = showsOwnTitles;
            this.remainingSeconds = totalSeconds;
        }
    }
}
