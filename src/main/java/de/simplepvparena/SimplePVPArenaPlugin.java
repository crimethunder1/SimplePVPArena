package de.simplepvparena;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.arena.SpectatorItemFactory;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.border.BorderMarkStorage;
import de.simplepvparena.border.BorderParticleService;
import de.simplepvparena.border.BorderVisualEffect;
import de.simplepvparena.border.BorderVisualEffects;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.OperatorBorderPassLog;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.gui.AdminMenuService;
import de.simplepvparena.kit.KitConfigurationLoader;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.listener.AdminMenuListener;
import de.simplepvparena.listener.FriendlyFireListener;
import de.simplepvparena.listener.GlobalPvpLockListener;
import de.simplepvparena.listener.PlayerConnectionListener;
import de.simplepvparena.listener.PlayerDeathListener;
import de.simplepvparena.listener.PlayerInteractListener;
import de.simplepvparena.listener.PlayerMoveListener;
import de.simplepvparena.listener.PlayerRespawnListener;
import de.simplepvparena.listener.SpectatorBorderListener;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.RoundTimeoutSettings;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamStorage;
import de.simplepvparena.tournament.OnlineOperatorNotifier;
import de.simplepvparena.tournament.OperatorNotifier;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SimplePVPArenaPlugin extends JavaPlugin {

    private static final String COMMAND_NAME = "tournament";
    private static final String KIT_SECTION = "kit";
    private static final String ROUND_RESET_DELAY_KEY = "match.round-reset-delay-ticks";
    private static final String ROUND_TIMEOUT_SECONDS_KEY = "match.round-timeout-seconds";
    private static final String SUDDEN_DEATH_WARNING_SECONDS_KEY = "match.sudden-death-warning-seconds";
    private static final String SUDDEN_DEATH_DAMAGE_PER_SECOND_KEY = "match.sudden-death-damage-per-second";
    private static final String SUDDEN_DEATH_DAMAGE_INCREASE_KEY = "match.sudden-death-damage-increase-per-second";
    private static final String ARENA_BLOCKS_PER_TICK_KEY = "arena.clean-blocks-per-tick";
    private static final String ARENA_MAXIMUM_CELLS_KEY = ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH;
    private static final String BORDER_PARTICLE_PERIOD_KEY = "border.particle-period-ticks";
    private static final String BORDER_PARTICLE_VIEW_RADIUS_KEY = "border.particle-view-radius";
    private static final String BORDER_PARTICLE_MAXIMUM_BLOCKS_KEY = "border.particle-maximum-blocks-per-player";
    private static final String BORDER_PARTICLE_MAXIMUM_LINES_KEY = "border.particle-maximum-lines-per-player";
    private static final String BORDER_WALL_PREVIEW_SECONDS_KEY = "border.wall-preview-seconds";
    private static final String TEAMS_FILE_NAME = "teams.yml";
    private static final String BORDERS_FILE_NAME = "borders.yml";
    private static final long DEFAULT_ROUND_RESET_DELAY_TICKS = 100L;
    private static final long OPERATOR_BORDER_PASS_REPORT_PERIOD_TICKS = 20L * 120L;
    private static final long TICKS_PER_SECOND = 20L;
    private static final int DEFAULT_BORDER_WALL_PREVIEW_SECONDS = 4;

    private ArenaRegistry arenaRegistry;
    private TeamRegistry teamRegistry;
    private TeamStorage teamStorage;
    private KitService kitService;
    private SpectatorService spectatorService;
    private BorderMarkIndex borderMarkIndex;
    private BorderMarkStorage borderMarkStorage;
    private OperatorBorderPassLog operatorBorderPassLog;
    private SpectatorBorderService spectatorBorderService;
    private BorderParticleService borderParticleService;
    private BorderWallPreview borderWallPreview;
    private ArenaCleaningService arenaCleaningService;
    private ArenaSetupService arenaSetupService;
    private AdminMenuService adminMenuService;
    private MatchService matchService;
    private TournamentManager tournamentManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        arenaRegistry = new ArenaRegistry();
        warnAboutLegacyMultiArenaConfiguration();
        warnAboutLegacyRegionConfiguration();
        arenaRegistry.loadFromConfiguration(getConfig());
        warnAboutUnreadableArenaShape();
        warnAboutIncompleteArena();

        teamRegistry = new TeamRegistry();
        teamStorage = new TeamStorage(new File(getDataFolder(), TEAMS_FILE_NAME), getLogger());
        teamStorage.load(teamRegistry);

        kitService = new KitService();
        kitService.setKitDefinition(new KitConfigurationLoader(getLogger()).load(getConfig().getConfigurationSection(KIT_SECTION)));

        spectatorService = new SpectatorService(arenaRegistry, new SpectatorItemFactory(this));

        borderMarkIndex = new BorderMarkIndex();
        borderMarkStorage = new BorderMarkStorage(new File(getDataFolder(), BORDERS_FILE_NAME), getLogger());
        borderMarkStorage.load(borderMarkIndex);
        operatorBorderPassLog = new OperatorBorderPassLog();
        spectatorBorderService = new SpectatorBorderService(this, borderMarkIndex, this::saveBorderMarks);

        arenaCleaningService = new ArenaCleaningService(this, spectatorBorderService::clearMarksInShape);
        arenaCleaningService.setBlocksPerTick(getConfig().getInt(ARENA_BLOCKS_PER_TICK_KEY,
                ArenaCleaningService.DEFAULT_BLOCKS_PER_TICK));

        matchService = new MatchService(this, kitService, spectatorService);
        matchService.setRoundResetDelayTicks(getConfig().getLong(ROUND_RESET_DELAY_KEY, DEFAULT_ROUND_RESET_DELAY_TICKS));
        matchService.setRoundTimeoutSettings(readRoundTimeoutSettings());

        tournamentManager = new TournamentManager(this, teamRegistry, arenaRegistry, matchService,
                new OnlineOperatorNotifier(getServer()));

        BorderVisualEffect borderVisualEffect = BorderVisualEffects.create(getServer().getPluginManager(), getLogger());
        borderParticleService = new BorderParticleService(this, borderMarkIndex, borderVisualEffect,
                matchService::isActiveFighter);
        borderParticleService.setPeriodTicks(getConfig().getLong(BORDER_PARTICLE_PERIOD_KEY,
                BorderParticleService.DEFAULT_PERIOD_TICKS));
        borderParticleService.setViewRadius(getConfig().getInt(BORDER_PARTICLE_VIEW_RADIUS_KEY,
                BorderParticleService.DEFAULT_VIEW_RADIUS));
        borderParticleService.setMaximumBlocksPerPlayer(getConfig().getInt(BORDER_PARTICLE_MAXIMUM_BLOCKS_KEY,
                BorderParticleService.DEFAULT_MAXIMUM_BLOCKS_PER_PLAYER));
        borderParticleService.setMaximumLinesPerPlayer(getConfig().getInt(BORDER_PARTICLE_MAXIMUM_LINES_KEY,
                BorderParticleService.DEFAULT_MAXIMUM_LINES_PER_PLAYER));
        borderWallPreview = new BorderWallPreview(this, borderVisualEffect);
        borderWallPreview.setDurationTicks(TICKS_PER_SECOND * getConfig().getInt(BORDER_WALL_PREVIEW_SECONDS_KEY,
                DEFAULT_BORDER_WALL_PREVIEW_SECONDS));

        arenaSetupService = new ArenaSetupService(arenaRegistry, matchService, spectatorBorderService,
                borderWallPreview, arenaCleaningService, readMaximumArenaCellCount(), this::saveArenaConfiguration);
        adminMenuService = new AdminMenuService(this, tournamentManager, teamRegistry, matchService,
                arenaSetupService, this::saveTeamConfiguration);

        registerCommand();
        registerListeners();
        startOperatorBorderPassReport();
        borderParticleService.start();
    }

    @Override
    public void onDisable() {
        if (borderParticleService != null) {
            borderParticleService.stop();
        }
        if (borderWallPreview != null) {
            borderWallPreview.stopAll();
        }
        if (arenaCleaningService != null) {
            arenaCleaningService.stopCleaning();
        }
        if (matchService != null) {
            matchService.cancelActiveMatch();
        }
        if (spectatorService != null) {
            spectatorService.clearAllAssignments();
        }
    }

    public void saveArenaConfiguration() {
        arenaRegistry.saveToConfiguration(getConfig());
        saveConfig();
    }

    public void saveTeamConfiguration() {
        teamStorage.save(teamRegistry);
    }

    public void saveBorderMarks() {
        borderMarkStorage.save(borderMarkIndex);
    }

    private void startOperatorBorderPassReport() {
        OperatorNotifier administratorNotifier = OnlineOperatorNotifier.administrators(getServer(),
                TournamentCommand.ADMIN_PERMISSION);
        getServer().getScheduler().runTaskTimer(this,
                () -> operatorBorderPassLog.drainReport().ifPresent(administratorNotifier::notifyOperators),
                OPERATOR_BORDER_PASS_REPORT_PERIOD_TICKS, OPERATOR_BORDER_PASS_REPORT_PERIOD_TICKS);
    }

    public TournamentManager getTournamentManager() {
        return tournamentManager;
    }

    private void warnAboutLegacyMultiArenaConfiguration() {
        if (!ArenaRegistry.containsLegacyMultiArenaConfiguration(getConfig())) {
            return;
        }
        getLogger().warning("The configuration still holds the entries " + ArenaRegistry.LEGACY_ARENAS_SECTION
                + " and " + ArenaRegistry.LEGACY_FINAL_ARENA_KEY + " of the former multi arena setup. They are "
                + "ignored and dropped with the next arena change, configure the single arena in the section "
                + ArenaRegistry.ARENA_SECTION + " again with " + Arena.CALCULATE_COMMAND);
    }

    private void warnAboutLegacyRegionConfiguration() {
        if (!ArenaRegistry.containsLegacyRegionConfiguration(getConfig())) {
            return;
        }
        getLogger().warning("The configuration still holds the rectangle regions of the former arena setup in "
                + ArenaRegistry.ARENA_SECTION + "." + ArenaRegistry.LEGACY_REGIONS_SECTION + ". They are ignored "
                + "and dropped with the next arena change, calculate the fight area again with "
                + Arena.CALCULATE_COMMAND);
    }

    private void warnAboutUnreadableArenaShape() {
        arenaRegistry.getShapeLoadFailure().ifPresent(failure -> getLogger().warning(
                "The stored fight area of the arena could not be read: " + failure));
    }

    private void warnAboutIncompleteArena() {
        if (arenaRegistry.isArenaFullyConfigured()) {
            return;
        }
        getLogger().warning("The arena is not fully configured, it still misses: "
                + String.join(", ", arenaRegistry.getArena().getMissingConfiguration()));
    }

    private int readMaximumArenaCellCount() {
        int maximumCellCount = getConfig().getInt(ARENA_MAXIMUM_CELLS_KEY,
                ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT);
        if (maximumCellCount >= 1) {
            return maximumCellCount;
        }
        getLogger().warning(ARENA_MAXIMUM_CELLS_KEY + " must be at least one cell, falling back to "
                + ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT);
        return ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT;
    }

    private RoundTimeoutSettings readRoundTimeoutSettings() {
        RoundTimeoutSettings defaultSettings = RoundTimeoutSettings.defaults();
        int roundTimeoutSeconds = getConfig().getInt(ROUND_TIMEOUT_SECONDS_KEY, defaultSettings.roundTimeoutSeconds());
        if (roundTimeoutSeconds < 1) {
            getLogger().warning(ROUND_TIMEOUT_SECONDS_KEY + " must be at least one second, falling back to "
                    + defaultSettings.roundTimeoutSeconds() + " seconds");
            roundTimeoutSeconds = defaultSettings.roundTimeoutSeconds();
        }
        return new RoundTimeoutSettings(roundTimeoutSeconds,
                getConfig().getInt(SUDDEN_DEATH_WARNING_SECONDS_KEY, defaultSettings.suddenDeathWarningSeconds()),
                getConfig().getDouble(SUDDEN_DEATH_DAMAGE_PER_SECOND_KEY, defaultSettings.suddenDeathBaseDamage()),
                getConfig().getDouble(SUDDEN_DEATH_DAMAGE_INCREASE_KEY,
                        defaultSettings.suddenDeathDamageIncreasePerSecond()));
    }

    private void registerCommand() {
        PluginCommand pluginCommand = getCommand(COMMAND_NAME);
        if (pluginCommand == null) {
            getLogger().severe("Command " + COMMAND_NAME + " is not declared in plugin.yml");
            return;
        }
        TournamentCommand tournamentCommand = new TournamentCommand(getServer(), tournamentManager, teamRegistry,
                matchService, spectatorService, spectatorBorderService, arenaSetupService, adminMenuService,
                this::saveTeamConfiguration);
        pluginCommand.setExecutor(tournamentCommand);
        pluginCommand.setTabCompleter(tournamentCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(matchService, spectatorService, arenaRegistry), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(matchService), this);
        getServer().getPluginManager().registerEvents(
                new PlayerRespawnListener(this, matchService, spectatorService, arenaRegistry), this);
        getServer().getPluginManager().registerEvents(
                new PlayerMoveListener(matchService, spectatorService, arenaRegistry), this);
        getServer().getPluginManager().registerEvents(
                new SpectatorBorderListener(matchService, spectatorBorderService, operatorBorderPassLog), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(matchService, spectatorService), this);
        getServer().getPluginManager().registerEvents(new AdminMenuListener(adminMenuService), this);
        getServer().getPluginManager().registerEvents(new GlobalPvpLockListener(matchService), this);
        getServer().getPluginManager().registerEvents(new FriendlyFireListener(matchService), this);
    }
}
