package de.simplepvparena;

import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaRegionType;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.SpectatorItemFactory;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.kit.KitConfigurationLoader;
import de.simplepvparena.kit.KitService;
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
    private static final String TEAMS_FILE_NAME = "teams.yml";
    private static final long DEFAULT_ROUND_RESET_DELAY_TICKS = 100L;

    private ArenaRegistry arenaRegistry;
    private TeamRegistry teamRegistry;
    private TeamStorage teamStorage;
    private KitService kitService;
    private SpectatorService spectatorService;
    private SpectatorBorderService spectatorBorderService;
    private ArenaCleaningService arenaCleaningService;
    private MatchService matchService;
    private TournamentManager tournamentManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        arenaRegistry = new ArenaRegistry();
        warnAboutLegacyMultiArenaConfiguration();
        arenaRegistry.loadFromConfiguration(getConfig());
        warnAboutIncompleteArena();

        teamRegistry = new TeamRegistry();
        teamStorage = new TeamStorage(new File(getDataFolder(), TEAMS_FILE_NAME), getLogger());
        teamStorage.load(teamRegistry);

        kitService = new KitService();
        kitService.setKitDefinition(new KitConfigurationLoader(getLogger()).load(getConfig().getConfigurationSection(KIT_SECTION)));

        spectatorService = new SpectatorService(arenaRegistry, new SpectatorItemFactory(this));
        spectatorBorderService = new SpectatorBorderService(this);
        arenaCleaningService = new ArenaCleaningService(this, spectatorBorderService::clearMarksInRegion);
        arenaCleaningService.setBlocksPerTick(getConfig().getInt(ARENA_BLOCKS_PER_TICK_KEY,
                ArenaCleaningService.DEFAULT_BLOCKS_PER_TICK));

        matchService = new MatchService(this, kitService, spectatorService);
        matchService.setRoundResetDelayTicks(getConfig().getLong(ROUND_RESET_DELAY_KEY, DEFAULT_ROUND_RESET_DELAY_TICKS));
        matchService.setRoundTimeoutSettings(readRoundTimeoutSettings());

        tournamentManager = new TournamentManager(this, teamRegistry, arenaRegistry, matchService,
                new OnlineOperatorNotifier(getServer()));

        registerCommand();
        registerListeners();
    }

    @Override
    public void onDisable() {
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
                + ArenaRegistry.ARENA_SECTION + " or with /tournament arena setpos");
    }

    private void warnAboutIncompleteArena() {
        if (arenaRegistry.isArenaFullyConfigured()) {
            return;
        }
        getLogger().warning("The arena is not fully configured, it still misses: "
                + arenaRegistry.getArena().getMissingRegionTypes().stream()
                .map(ArenaRegionType::getCommandArgument).toList());
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
                arenaRegistry, matchService, spectatorService, spectatorBorderService, arenaCleaningService,
                this::saveArenaConfiguration, this::saveTeamConfiguration);
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
                new SpectatorBorderListener(matchService, spectatorBorderService), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(matchService, spectatorService), this);
        getServer().getPluginManager().registerEvents(new GlobalPvpLockListener(matchService), this);
        getServer().getPluginManager().registerEvents(new FriendlyFireListener(matchService), this);
    }
}
