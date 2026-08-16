package de.simplepvparena;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaBaselineRegistry;
import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaBorderConfirmation;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaCleaningVisualEffect;
import de.simplepvparena.arena.ArenaGapGuide;
import de.simplepvparena.arena.ActiveArenaState;
import de.simplepvparena.arena.ArenaHeightMarkerService;
import de.simplepvparena.arena.ArenaHeightMarkers;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaSafetyZone;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.SpectatorItemFactory;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.arena.VanillaArenaCleaningVisualEffect;
import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.border.BorderMarkStorage;
import de.simplepvparena.border.BorderParticleService;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.border.BorderVisualEffect;
import de.simplepvparena.border.BorderVisualEffects;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.config.ConfigurationReloadReport;
import de.simplepvparena.config.ConfigurationReloadService;
import de.simplepvparena.config.PluginLiveConfiguration;
import de.simplepvparena.gui.AdminMenuService;
import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.config.PluginSettings;
import de.simplepvparena.kit.KitConfigurationLoader;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.kit.SerializedKit;
import de.simplepvparena.listener.AdminMenuListener;
import de.simplepvparena.listener.ArenaHeightMarkerListener;
import de.simplepvparena.listener.ArenaSafetyZoneListener;
import de.simplepvparena.listener.BorderSelectorListener;
import de.simplepvparena.listener.ChatInputListener;
import de.simplepvparena.listener.FriendlyFireListener;
import de.simplepvparena.listener.GlobalPvpLockListener;
import de.simplepvparena.listener.PlayerConnectionListener;
import de.simplepvparena.listener.PlayerDeathListener;
import de.simplepvparena.listener.PlayerInteractListener;
import de.simplepvparena.listener.PlayerMoveListener;
import de.simplepvparena.listener.PlayerRespawnListener;
import de.simplepvparena.listener.SpectatorBorderListener;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.team.EventTeamImportFile;
import de.simplepvparena.team.EventTeamImportService;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamStorage;
import de.simplepvparena.tournament.OnlineOperatorNotifier;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SimplePVPArenaPlugin extends JavaPlugin {

    private static final String COMMAND_NAME = "tournament";
    private static final String TEAMS_FILE_NAME = "teams.yml";
    private static final String BORDERS_FILE_NAME = "borders.yml";

    private ArenaRegistry arenaRegistry;
    private TeamRegistry teamRegistry;
    private TeamStorage teamStorage;
    private EventTeamImportService eventTeamImportService;
    private KitService kitService;
    private KitSaveService kitSaveService;
    private SpectatorService spectatorService;
    private BorderMarkIndex borderMarkIndex;
    private BorderMarkStorage borderMarkStorage;
    private ArenaBorderConfirmation arenaBorderConfirmation;
    private SpectatorBorderService spectatorBorderService;
    private BorderSelectorModeRegistry borderSelectorModes;
    private BorderParticleService borderParticleService;
    private BorderWallPreview borderWallPreview;
    private ArenaCleaningService arenaCleaningService;
    private ArenaCleaningVisualEffect arenaCleaningVisualEffect;
    private ArenaBaselineService arenaBaselineService;
    private ArenaGapGuide arenaGapGuide;
    private ArenaSafetyZone arenaSafetyZone;
    private ArenaHeightMarkers arenaHeightMarkers;
    private ArenaHeightMarkerService arenaHeightMarkerService;
    private ActiveArenaState activeArenaState;
    private ArenaSetupService arenaSetupService;
    private ChatInputService chatInputService;
    private AdminMenuService adminMenuService;
    private MatchService matchService;
    private TournamentManager tournamentManager;
    private ConfigurationReloadService configurationReloadService;

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
        kitSaveService = new KitSaveService(kitService, this::saveKitConfiguration, getLogger()::warning);

        spectatorService = new SpectatorService(arenaRegistry, new SpectatorItemFactory(this));

        borderMarkIndex = new BorderMarkIndex();
        borderMarkStorage = new BorderMarkStorage(new File(getDataFolder(), BORDERS_FILE_NAME), getLogger());
        borderMarkStorage.load(borderMarkIndex);
        arenaBorderConfirmation = new ArenaBorderConfirmation(arenaRegistry.getArena()::hasShape);
        spectatorBorderService = new SpectatorBorderService(this, borderMarkIndex, this::onBorderMarksChanged);
        borderSelectorModes = new BorderSelectorModeRegistry();

        arenaCleaningVisualEffect = new VanillaArenaCleaningVisualEffect();
        arenaCleaningService = new ArenaCleaningService(this, spectatorBorderService::clearMarksInShape,
                arenaCleaningVisualEffect);

        arenaBaselineService = new ArenaBaselineService(this, new ArenaBaselineRegistry());

        matchService = new MatchService(this, kitService, spectatorService);

        tournamentManager = new TournamentManager(this, teamRegistry, arenaRegistry, matchService,
                new OnlineOperatorNotifier(getServer()));

        BorderVisualEffect borderVisualEffect = BorderVisualEffects.create(getServer().getPluginManager(),
                getLogger());
        borderParticleService = new BorderParticleService(this, borderMarkIndex, borderVisualEffect,
                matchService::isActiveFighter, arenaBorderConfirmation);
        borderWallPreview = new BorderWallPreview(this, borderVisualEffect);
        arenaGapGuide = new ArenaGapGuide(this);

        arenaHeightMarkers = new ArenaHeightMarkers();
        arenaHeightMarkerService = new ArenaHeightMarkerService(this, arenaHeightMarkers);

        activeArenaState = new ActiveArenaState(arenaRegistry, arenaHeightMarkers);

        arenaSafetyZone = new ArenaSafetyZone(activeArenaState::getCleanedVolume,
                matchService::isActiveFighter);

        arenaSetupService = new ArenaSetupService(activeArenaState, matchService, spectatorBorderService,
                borderWallPreview, arenaGapGuide, arenaCleaningService, arenaBaselineService,
                arenaBorderConfirmation, this::saveArenaConfiguration);
        chatInputService = new ChatInputService(this);
        TeamMemberLookup teamMemberLookup = new TeamMemberLookup(getServer());
        eventTeamImportService = new EventTeamImportService(
                new File(getDataFolder(), EventTeamImportFile.FILE_NAME), teamRegistry,
                teamMemberLookup::find, this::saveTeamConfiguration);
        adminMenuService = new AdminMenuService(this, tournamentManager, teamRegistry,
                teamMemberLookup, eventTeamImportService, matchService, arenaSetupService,
                arenaHeightMarkerService, spectatorBorderService, borderSelectorModes, kitSaveService,
                chatInputService, this::saveTeamConfiguration, this::reloadConfiguration);

        configurationReloadService = new ConfigurationReloadService(this::getConfig, this::reloadConfig,
                new PluginLiveConfiguration(kitService, new KitConfigurationLoader(getLogger()), matchService,
                        arenaCleaningService, arenaCleaningVisualEffect, arenaBaselineService, arenaSetupService,
                        arenaSafetyZone, borderParticleService, borderWallPreview, borderVisualEffect,
                        getLogger()::warning),
                getLogger()::warning);
        configurationReloadService.applyStoredConfiguration();

        registerCommand();
        registerListeners();
        borderParticleService.start();
        chatInputService.start();
    }

    @Override
    public void onDisable() {
        if (chatInputService != null) {
            chatInputService.stop();
        }
        if (borderParticleService != null) {
            borderParticleService.stop();
        }
        if (borderWallPreview != null) {
            borderWallPreview.stopAll();
        }
        if (arenaGapGuide != null) {
            arenaGapGuide.stopAll();
        }
        if (arenaCleaningService != null) {
            arenaCleaningService.stopCleaning();
        }
        if (arenaBaselineService != null) {
            arenaBaselineService.stopCapture();
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

    public void saveKitConfiguration(SerializedKit serializedKit) {
        serializedKit.writeInto(getConfig(), PluginSettings.KIT_SECTION_PATH);
        saveConfig();
    }

    public ConfigurationReloadReport reloadConfiguration() {
        return configurationReloadService.reloadFromFile();
    }

    public void onBorderMarksChanged() {
        borderMarkStorage.save(borderMarkIndex);
        arenaBorderConfirmation.invalidate();
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
                + ArenaRegistry.ARENA_SECTION + " again with " + Arena.CALCULATE_ACTION);
    }

    private void warnAboutLegacyRegionConfiguration() {
        if (!ArenaRegistry.containsLegacyRegionConfiguration(getConfig())) {
            return;
        }
        getLogger().warning("The configuration still holds the rectangle regions of the former arena setup in "
                + ArenaRegistry.ARENA_SECTION + "." + ArenaRegistry.LEGACY_REGIONS_SECTION + ". They are ignored "
                + "and dropped with the next arena change, calculate the fight area again with "
                + Arena.CALCULATE_ACTION);
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

    private void registerCommand() {
        PluginCommand pluginCommand = getCommand(COMMAND_NAME);
        if (pluginCommand == null) {
            getLogger().severe("Command " + COMMAND_NAME + " is not declared in plugin.yml");
            return;
        }
        TournamentCommand tournamentCommand = new TournamentCommand(matchService, spectatorService,
                adminMenuService);
        pluginCommand.setExecutor(tournamentCommand);
        pluginCommand.setTabCompleter(tournamentCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(matchService, spectatorService, arenaRegistry, borderSelectorModes), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(matchService), this);
        getServer().getPluginManager().registerEvents(
                new PlayerRespawnListener(this, matchService, spectatorService, arenaRegistry), this);
        getServer().getPluginManager().registerEvents(
                new PlayerMoveListener(matchService, spectatorService, arenaRegistry), this);
        getServer().getPluginManager().registerEvents(
                new SpectatorBorderListener(matchService, spectatorService, spectatorBorderService), this);
        getServer().getPluginManager().registerEvents(
                new PlayerInteractListener(matchService, spectatorService, borderSelectorModes), this);
        getServer().getPluginManager().registerEvents(
                new BorderSelectorListener(borderSelectorModes, spectatorBorderService), this);
        getServer().getPluginManager().registerEvents(
                new ArenaHeightMarkerListener(arenaHeightMarkerService), this);
        getServer().getPluginManager().registerEvents(new ArenaSafetyZoneListener(arenaSafetyZone), this);
        getServer().getPluginManager().registerEvents(new AdminMenuListener(adminMenuService), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(chatInputService), this);
        getServer().getPluginManager().registerEvents(new GlobalPvpLockListener(matchService), this);
        getServer().getPluginManager().registerEvents(new FriendlyFireListener(matchService), this);
    }
}
