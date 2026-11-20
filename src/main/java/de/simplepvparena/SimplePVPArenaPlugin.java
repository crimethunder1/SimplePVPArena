package de.simplepvparena;

import de.simplepvparena.arena.ActiveArenaState;
import de.simplepvparena.arena.AreaInventoryService;
import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaAudience;
import de.simplepvparena.arena.ArenaBaselineFile;
import de.simplepvparena.arena.ArenaBaselineRegistry;
import de.simplepvparena.arena.ArenaBaselineSnapshot;
import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaBorderConfirmation;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaCleaningVisualEffect;
import de.simplepvparena.arena.ArenaGapGuide;
import de.simplepvparena.arena.ArenaHeightMarkerService;
import de.simplepvparena.arena.ArenaHeightMarkers;
import de.simplepvparena.arena.ArenaMobRemovalService;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaSafetyZone;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.ArenaTemporaryEntityRemover;
import de.simplepvparena.arena.BukkitArenaTemporaryEntityRemover;
import de.simplepvparena.arena.BukkitPlayerPotionEffects;
import de.simplepvparena.arena.EffectMarginSetting;
import de.simplepvparena.arena.FileAreaInventoryStorage;
import de.simplepvparena.arena.MarkClearing;
import de.simplepvparena.arena.MarkClearingKind;
import de.simplepvparena.arena.SpectatorItemFactory;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.arena.StandingsBoardToolService;
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
import de.simplepvparena.config.ArenaConfigurationFileWriter;
import de.simplepvparena.config.AutoMatchConfigurationWriter;
import de.simplepvparena.config.ConfigurationReloadReport;
import de.simplepvparena.config.ConfigurationReloadService;
import de.simplepvparena.config.ConfirmedBorderParticleConfigurationWriter;
import de.simplepvparena.config.EffectMarginConfigurationWriter;
import de.simplepvparena.config.PluginLiveConfiguration;
import de.simplepvparena.config.PluginSettings;
import de.simplepvparena.config.ServerModeConfigurationWriter;
import de.simplepvparena.config.ServerModeSetting;
import de.simplepvparena.config.TeamModeConfigurationWriter;
import de.simplepvparena.config.TeamSizeConfigurationWriter;
import de.simplepvparena.diagnostics.PluginDryRunArena;
import de.simplepvparena.diagnostics.TournamentDryRun;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.gate.StartGateItemService;
import de.simplepvparena.gate.StartGateMarkStorage;
import de.simplepvparena.gate.StartGateMarks;
import de.simplepvparena.gate.StartGateOpeningService;
import de.simplepvparena.gate.VanillaStartGateVisualEffect;
import de.simplepvparena.gui.AdminMenuDependencies;
import de.simplepvparena.gui.AdminMenuService;
import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.kit.BukkitItemStackCodec;
import de.simplepvparena.kit.BukkitKitItemSerializer;
import de.simplepvparena.kit.FileKitLibraryStorage;
import de.simplepvparena.kit.FilePlayerStateSnapshotStorage;
import de.simplepvparena.kit.KitConfigurationLoader;
import de.simplepvparena.kit.KitItem;
import de.simplepvparena.kit.KitItemSerializer;
import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.kit.KitLibraryLoader;
import de.simplepvparena.kit.KitLibraryStorage;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.listener.AdminMenuListener;
import de.simplepvparena.listener.AreaInventoryListener;
import de.simplepvparena.listener.ArenaBreakListener;
import de.simplepvparena.listener.ArenaBuildListener;
import de.simplepvparena.listener.ArenaDamageListener;
import de.simplepvparena.listener.ArenaEnvironmentProtectionListener;
import de.simplepvparena.listener.ArenaHeightMarkerListener;
import de.simplepvparena.listener.ArenaItemRuleListener;
import de.simplepvparena.listener.ArenaMobRuleListener;
import de.simplepvparena.listener.ArenaSafetyZoneListener;
import de.simplepvparena.listener.BorderSelectorListener;
import de.simplepvparena.listener.ChatInputListener;
import de.simplepvparena.listener.FriendlyFireListener;
import de.simplepvparena.listener.GlobalPvpLockListener;
import de.simplepvparena.listener.PlacementBoardWorldListener;
import de.simplepvparena.listener.PlayerConnectionListener;
import de.simplepvparena.listener.PlayerDeathListener;
import de.simplepvparena.listener.PlayerInteractListener;
import de.simplepvparena.listener.PlayerMoveListener;
import de.simplepvparena.listener.PlayerRespawnListener;
import de.simplepvparena.listener.RegistrationStationListener;
import de.simplepvparena.listener.BracketBoardListener;
import de.simplepvparena.listener.RegistrationMachineListener;
import de.simplepvparena.listener.RegistrationStationButtonListener;
import de.simplepvparena.listener.RegistrationStationWorldListener;
import de.simplepvparena.listener.SpectatorBorderListener;
import de.simplepvparena.listener.StandingsBoardToolListener;
import de.simplepvparena.listener.StartGateListener;
import de.simplepvparena.match.ConfiguredRoundStartEffects;
import de.simplepvparena.match.FighterColours;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.ScoreboardFighterColours;
import de.simplepvparena.match.UnequalTeamVoteService;
import de.simplepvparena.match.VanillaRoundStartEffects;
import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.PluginMessage;
import de.simplepvparena.placement.AutoMatchLeaderboard;
import de.simplepvparena.placement.BracketBoardDisplays;
import de.simplepvparena.placement.BracketBoardService;
import de.simplepvparena.placement.BracketBoardText;
import de.simplepvparena.placement.BracketBoards;
import de.simplepvparena.placement.FileBracketBoardStorage;
import de.simplepvparena.placement.FileAutoMatchLeaderboardStorage;
import de.simplepvparena.placement.PaperPlacementBoardEntities;
import de.simplepvparena.placement.PlacementBoard;
import de.simplepvparena.placement.PlacementBoardText;
import de.simplepvparena.placement.PlacementService;
import de.simplepvparena.placement.PlacementSidebar;
import de.simplepvparena.registration.FileRegistrationStorage;
import de.simplepvparena.registration.FileRegistrationMachineStorage;
import de.simplepvparena.registration.PaperStationDisplayEntities;
import de.simplepvparena.registration.RegistrationMachineDisplays;
import de.simplepvparena.registration.RegistrationMachineService;
import de.simplepvparena.registration.RegistrationMachines;
import de.simplepvparena.registration.PaperPlayerNameResolver;
import de.simplepvparena.registration.RandomTeamFormation;
import de.simplepvparena.registration.RegistrationList;
import de.simplepvparena.registration.RegistrationService;
import de.simplepvparena.registration.TeamChoiceService;
import de.simplepvparena.registration.TeamImportService;
import de.simplepvparena.registration.TeamModeSetting;
import de.simplepvparena.registration.RegistrationStationButtonCheck;
import de.simplepvparena.registration.RegistrationStationDisplays;
import de.simplepvparena.registration.RegistrationStationService;
import de.simplepvparena.registration.RegistrationStations;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import de.simplepvparena.team.TeamStorage;
import de.simplepvparena.tournament.AdminNotifier;
import de.simplepvparena.tournament.AutoMatchService;
import de.simplepvparena.tournament.AutoMatchState;
import de.simplepvparena.tournament.AutomaticArenaCleanup;
import de.simplepvparena.tournament.FileTournamentStateStorage;
import de.simplepvparena.tournament.KitChoiceService;
import de.simplepvparena.tournament.OnlineAdminNotifier;
import de.simplepvparena.tournament.PlayAgainService;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.visual.ConfiguredVisuals;
import de.simplepvparena.visual.CustomVisuals;
import de.simplepvparena.visual.CustomVisualsSelection;
import de.simplepvparena.visual.ItemsAdderContentInstaller;
import de.simplepvparena.visual.ItemsAdderVisuals;
import de.simplepvparena.voice.PlayerVoiceNotices;
import de.simplepvparena.voice.SimpleVoiceChatBridge;
import de.simplepvparena.voice.VoiceChatHook;
import de.simplepvparena.voice.VoiceIsolation;
import de.simplepvparena.voice.VoiceRoster;
import org.bukkit.Tag;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

@NullMarked
public final class SimplePVPArenaPlugin extends JavaPlugin {

    private static final String COMMAND_NAME = "tournament";
    private static final String TEAMS_FILE_NAME = "teams.yml";
    private static final String BORDERS_FILE_NAME = "borders.yml";
    private static final String CONFIGURATION_FILE_NAME = "config.yml";
    private static final long VISUALS_READINESS_CHECK_PERIOD_TICKS = 20L;
    private static final String ITEMS_ADDER_ZIP_COMMAND = "iazip";
    private static final long STATION_BUTTON_CHECK_PERIOD_TICKS = 100L;
    private static final long STATION_COUNTER_PERIOD_TICKS = 20L;
    private static final long BRACKET_BOARD_PERIOD_TICKS = 20L;

    private ArenaRegistry arenaRegistry;
    private TeamRegistry teamRegistry;
    private TeamStorage teamStorage;
    private RegistrationList registrationList;
    private FileRegistrationStorage registrationStorage;
    private RegistrationStations registrationStations;
    private RegistrationService registrationService;
    private RegistrationStationService registrationStationService;
    private RegistrationStationDisplays registrationStationDisplays;
    private RegistrationMachineDisplays registrationMachineDisplays;
    private RegistrationMachineService registrationMachineService;
    private BracketBoardDisplays bracketBoardDisplays;
    private BracketBoardService bracketBoardService;
    private volatile boolean machineVisualsWereReady;
    private boolean itemsAdderContentChanged;
    private boolean itemsAdderZipRequested;
    private IOException itemsAdderContentFailure;
    private ArenaBaselineFile arenaBaselineFile;
    private RegistrationStationButtonCheck registrationStationButtonCheck;
    private KitService kitService;
    private KitLibrary kitLibrary;
    private KitSaveService kitSaveService;
    private SpectatorService spectatorService;
    private BorderMarkIndex borderMarkIndex;
    private BorderMarkStorage borderMarkStorage;
    private ArenaBorderConfirmation arenaBorderConfirmation;
    private SpectatorBorderService spectatorBorderService;
    private BorderSelectorModeRegistry borderSelectorModes;
    private BorderParticleService borderParticleService;
    private BorderWallPreview borderWallPreview;
    private StartGateMarks startGateMarks;
    private StartGateMarkStorage startGateMarkStorage;
    private StartGateDuration startGateDuration;
    private StartGateItemService startGateItemService;
    private StartGateOpeningService startGateOpeningService;
    private ArenaCleaningService arenaCleaningService;
    private ArenaCleaningVisualEffect arenaCleaningVisualEffect;
    private ArenaBaselineService arenaBaselineService;
    private ArenaGapGuide arenaGapGuide;
    private ArenaSafetyZone arenaSafetyZone;
    private ArenaHeightMarkers arenaHeightMarkers;
    private ArenaHeightMarkerService arenaHeightMarkerService;
    private ArenaMobRemovalService arenaMobRemovalService;
    private ActiveArenaState activeArenaState;
    private ArenaSetupService arenaSetupService;
    private ChatInputService chatInputService;
    private AdminMenuService adminMenuService;
    private FighterColours fighterColours;
    private MatchService matchService;
    private TournamentManager tournamentManager;
    private AutoMatchService autoMatchService;
    private PlayAgainService playAgainService;
    private TeamImportService teamImportService;
    private final ServerModeSetting serverModeSetting = new ServerModeSetting();
    private TournamentDryRun tournamentDryRun;
    private ConfigurationReloadService configurationReloadService;
    private PluginLiveConfiguration liveConfiguration;
    private AreaInventoryService areaInventoryService;
    private ConfiguredVisuals configuredVisuals;
    private PlacementSidebar placementSidebar;
    private PlacementBoard placementBoard;
    private AutoMatchLeaderboard autoMatchLeaderboard;
    private FileAutoMatchLeaderboardStorage autoMatchLeaderboardStorage;
    private StandingsBoardToolService standingsBoardToolService;
    private VoiceIsolation voiceIsolation;

    @SuppressWarnings("null")
    public SimplePVPArenaPlugin() {
    }

    @Override
    public void onLoad() {
        Path pluginsFolder = getDataFolder().toPath().toAbsolutePath().getParent();
        if (pluginsFolder == null || !Files.isDirectory(pluginsFolder.resolve(
                ItemsAdderContentInstaller.ITEMS_ADDER_FOLDER))) {
            return;
        }
        try {
            itemsAdderContentChanged = new ItemsAdderContentInstaller(
                    ItemsAdderContentInstaller.namespaceFolder(pluginsFolder))
                    .install(ItemsAdderContentInstaller.readBundledFiles(getFile().toPath()));
        } catch (IOException | RuntimeException failure) {
            itemsAdderContentFailure = failure instanceof IOException io ? io : new IOException(failure);
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        AdminNotifier adminNotifier = new OnlineAdminNotifier(getServer());
        MessageCatalogLoader messageCatalogLoader = new MessageCatalogLoader(
                new File(getDataFolder(), MessageCatalogLoader.FOLDER_NAME).toPath(), getLogger()::warning,
                adminNotifier::notifyAdmins);
        messageCatalogLoader.installShippedFiles();
        Messages.useWarningLogger(getLogger()::warning);
        messageCatalogLoader.useLanguage(PluginSettings.readLanguage(getConfig(), problem -> { }));
        reportItemsAdderContent();

        arenaRegistry = new ArenaRegistry();
        warnAboutLegacyMultiArenaConfiguration();
        warnAboutLegacyRegionConfiguration();
        arenaRegistry.loadFromConfiguration(getConfig());
        warnAboutUnreadableArenaShape();
        warnAboutIncompleteArena();

        teamRegistry = new TeamRegistry();
        teamStorage = new TeamStorage(new File(getDataFolder(), TEAMS_FILE_NAME), getLogger());
        teamStorage.load(teamRegistry);

        registrationList = new RegistrationList();
        registrationStations = new RegistrationStations();
        registrationStorage = new FileRegistrationStorage(
                new File(getDataFolder(), FileRegistrationStorage.FILE_NAME).toPath(), getLogger(),
                adminNotifier::notifyAdmins);
        registrationStorage.load(registrationList, registrationStations);

        kitService = new KitService(this, new FilePlayerStateSnapshotStorage(
                new File(getDataFolder(), FilePlayerStateSnapshotStorage.DIRECTORY_NAME).toPath(),
                new BukkitItemStackCodec()), adminNotifier::notifyAdmins);
        KitItemSerializer kitItemSerializer = new BukkitKitItemSerializer();
        KitLibraryStorage kitLibraryStorage = new FileKitLibraryStorage(
                new File(getDataFolder(), FileKitLibraryStorage.FILE_NAME).toPath(), getLogger()::warning);
        kitLibrary = new KitLibrary(kitLibraryStorage, kitService, kitItemSerializer, getLogger()::warning,
                adminNotifier::notifyAdmins);
        loadKitLibrary(new KitLibraryLoader(kitLibraryStorage, kitLibrary,
                        new KitConfigurationLoader(getLogger()::warning, kitItemSerializer, KitItem::toItemStack),
                        adminNotifier::notifyAdmins, getLogger()::warning),
                getConfig().getConfigurationSection(PluginSettings.KIT_SECTION_PATH), getLogger(),
                adminNotifier::notifyAdmins);
        kitSaveService = new KitSaveService(kitLibrary, kitItemSerializer);

        spectatorService = new SpectatorService(arenaRegistry, new SpectatorItemFactory(this));
        areaInventoryService = new AreaInventoryService(this, arenaRegistry::findEffectArea,
                new FileAreaInventoryStorage(
                        new File(getDataFolder(), FileAreaInventoryStorage.DIRECTORY_NAME).toPath(),
                        new BukkitItemStackCodec()),
                kitService::hasAppliedKit, player -> matchService.isActiveFighter(player.getUniqueId()),
                spectatorService, new BukkitPlayerPotionEffects(), adminNotifier::notifyAdmins,
                System::currentTimeMillis);

        borderMarkIndex = new BorderMarkIndex();
        borderMarkStorage = new BorderMarkStorage(new File(getDataFolder(), BORDERS_FILE_NAME), getLogger());
        borderMarkStorage.load(borderMarkIndex);
        arenaBorderConfirmation = new ArenaBorderConfirmation(arenaRegistry.getArena()::hasShape);
        spectatorBorderService = new SpectatorBorderService(borderMarkIndex, this::onBorderMarksChanged);
        borderSelectorModes = new BorderSelectorModeRegistry();

        arenaBaselineService = new ArenaBaselineService(this, new ArenaBaselineRegistry());
        arenaBaselineFile = new ArenaBaselineFile(getServer(), getDataFolder());
        arenaBaselineService.setCaptureListener(this::saveArenaBaseline);

        fighterColours = new ScoreboardFighterColours(getServer(), getLogger()::warning);
        fighterColours.clearColours();

        startGateMarks = new StartGateMarks();
        startGateMarkStorage = new StartGateMarkStorage(
                new File(getDataFolder(), StartGateMarkStorage.FILE_NAME), getLogger());
        startGateMarkStorage.load(startGateMarks);
        startGateDuration = new StartGateDuration();
        startGateItemService = new StartGateItemService(this, startGateMarks, startGateDuration,
                this::onStartGateMarksChanged);
        startGateOpeningService = new StartGateOpeningService(this, startGateMarks, startGateDuration,
                new VanillaStartGateVisualEffect());

        ArenaTemporaryEntityRemover temporaryEntityRemover = new BukkitArenaTemporaryEntityRemover();
        arenaMobRemovalService = new ArenaMobRemovalService(this, arenaRegistry);

        arenaCleaningVisualEffect = new VanillaArenaCleaningVisualEffect();
        arenaCleaningService = new ArenaCleaningService(this, spectatorBorderService::clearMarksInShape,
                arenaCleaningVisualEffect, startGateMarks::isMarked, temporaryEntityRemover);

        matchService = new MatchService(this, kitService, spectatorService, fighterColours,
                startGateOpeningService, temporaryEntityRemover);
        ArenaAudience arenaAudience = new ArenaAudience(getServer(), arenaRegistry);
        matchService.setArenaAudience(arenaAudience);
        matchService.setUnequalTeamVoteService(new UnequalTeamVoteService(this, getServer()::getPlayer,
                RandomGenerator.getDefault()));

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
                arenaBorderConfirmation, new ArenaConfigurationFileWriter(arenaRegistry, this::getConfig,
                        new File(getDataFolder(), CONFIGURATION_FILE_NAME), getLogger()));
        KitChoiceService kitChoiceService = new KitChoiceService(this, kitLibrary, adminNotifier);
        arenaSetupService.setArenaReservedForMatch(kitChoiceService::isPending);

        TeamSizeSetting teamSizeSetting = new TeamSizeSetting();
        AutomaticArenaCleanup arenaCleanup = new AutomaticArenaCleanup(arenaSetupService, arenaCleaningService);
        matchService.setRoundCleaning(arenaCleanup);
        matchService.setAdminNotifier(adminNotifier::notifyAdmins);
        tournamentManager = new TournamentManager(this, teamRegistry, arenaRegistry, matchService, arenaCleanup,
                adminNotifier, new FileTournamentStateStorage(
                        new File(getDataFolder(), FileTournamentStateStorage.FILE_NAME).toPath()),
                kitChoiceService, teamSizeSetting);
        tournamentManager.setArenaAudience(arenaAudience);
        tournamentManager.restoreSavedTournament();
        autoMatchService = new AutoMatchService(this, teamRegistry, arenaRegistry, matchService, tournamentManager,
                kitLibrary, arenaCleanup, adminNotifier, arenaAudience);
        arenaSetupService.setArenaReservedForAutoMatch(autoMatchService::isAnnounced);
        PlacementService placementService = new PlacementService(tournamentManager::getState,
                tournamentManager::getBracket, matchService::getActiveMatch, teamRegistry::findTeam);
        placementSidebar = new PlacementSidebar(this, arenaRegistry::findEffectArea,
                placementService::findStandings);
        autoMatchLeaderboardStorage = new FileAutoMatchLeaderboardStorage(
                new File(getDataFolder(), FileAutoMatchLeaderboardStorage.FILE_NAME).toPath());
        autoMatchLeaderboard = new AutoMatchLeaderboard(this::onAutoMatchLeaderboardChanged);
        loadAutoMatchLeaderboard();
        autoMatchService.addWinObserver(team -> autoMatchLeaderboard.recordWin(team.getMembers()));
        PlacementBoardText placementBoardText = new PlacementBoardText(
                () -> autoMatchLeaderboard.top(PlacementBoardText.LEADERBOARD_LIMIT),
                () -> configuredVisuals == null ? Optional.empty() : configuredVisuals.findBoardTitleImage(),
                serverModeSetting::getServerMode);
        placementBoard = new PlacementBoard(this, arenaRegistry.getArena()::getStandingsBoard, getServer()::getWorld,
                new PaperPlacementBoardEntities(this, part -> configuredVisuals == null ? Optional.empty()
                        : configuredVisuals.findMachinePart(part), model -> configuredVisuals == null
                        ? Optional.empty() : configuredVisuals.findBoardModel(model)),
                placementService::findStandings, placementBoardText::build);
        matchService.setMatchProgressListener(this::onPlacementChanged);
        arenaSetupService.setStandingsBoardListener(placementBoard::refresh);
        standingsBoardToolService = new StandingsBoardToolService(this, arenaSetupService, ItemStack::new);
        registrationService = new RegistrationService(registrationList,
                tournamentManager::isTournamentInProgress, this::saveRegistrations);
        registrationStationService = new RegistrationStationService(this, registrationStations,
                registrationService, Tag.BUTTONS::isTagged, this::saveRegistrations, ItemStack::new);
        RandomTeamFormation randomTeamFormation = new RandomTeamFormation(registrationService, teamRegistry,
                teamSizeSetting, tournamentManager::isTournamentInProgress,
                () -> autoMatchService.isAnnounced() || autoMatchService.getState() == AutoMatchState.RUNNING,
                this::saveTeamConfiguration, getServer()::getPlayer, RandomGenerator.getDefault());
        teamImportService = new TeamImportService(new File(getDataFolder(), TeamImportService.FOLDER_NAME).toPath(),
                teamRegistry, teamSizeSetting, tournamentManager::isTournamentInProgress,
                () -> autoMatchService.isAnnounced() || autoMatchService.getState() == AutoMatchState.RUNNING,
                this::saveTeamConfiguration, getServer()::getPlayer, RandomGenerator.getDefault(),
                new PaperPlayerNameResolver(getServer()), task -> getServer().getScheduler().runTask(this, task));
        createImportFolder();
        TeamModeSetting teamModeSetting = new TeamModeSetting();
        TeamChoiceService teamChoiceService = new TeamChoiceService(registrationService, teamRegistry,
                teamSizeSetting, teamModeSetting, tournamentManager::isTournamentInProgress,
                tournamentManager::findTeamChangeRejection, this::saveTeamConfiguration, getServer()::getPlayer);
        registrationService.addObserver(teamChoiceService);
        playAgainService = new PlayAgainService(autoMatchService, teamRegistry, registrationService, teamModeSetting,
                tournamentManager::isTournamentInProgress, this::saveTeamConfiguration, getServer()::getPlayer,
                arenaRegistry::findEffectArea, System::currentTimeMillis);

        tournamentDryRun = new TournamentDryRun(tournamentManager::getState,
                matchService::hasActiveMatch, teamRegistry,
                new PluginDryRunArena(arenaRegistry.getArena(), arenaSetupService), kitService,
                startGateMarks, startGateDuration, teamSizeSetting, getLogger());

        chatInputService = new ChatInputService(this);
        ServerModeConfigurationWriter serverModeWriter = new ServerModeConfigurationWriter(this::getConfig,
                new File(getDataFolder(), CONFIGURATION_FILE_NAME), getLogger());
        TeamMemberLookup teamMemberLookup = new TeamMemberLookup(getServer());
        adminMenuService = new AdminMenuService(new AdminMenuDependencies(this, chatInputService,
                this::reloadConfiguration,
                new AdminMenuDependencies.TournamentServices(tournamentManager, tournamentDryRun, matchService,
                        autoMatchService, new AutoMatchConfigurationWriter(this::getConfig,
                                new File(getDataFolder(), CONFIGURATION_FILE_NAME), getLogger()),
                        serverModeSetting, serverMode -> {
                            serverModeWriter.write(serverMode);
                            reloadConfiguration();
                        }),
                new AdminMenuDependencies.TeamServices(teamRegistry, teamMemberLookup, this::saveTeamConfiguration,
                        new TeamSizeConfigurationWriter(this::getConfig,
                                new File(getDataFolder(), CONFIGURATION_FILE_NAME), getLogger())),
                new AdminMenuDependencies.ArenaServices(arenaSetupService, arenaHeightMarkerService,
                        startGateItemService, borderSelectorModes, borderParticleService,
                        new ConfirmedBorderParticleConfigurationWriter(this::getConfig,
                                new File(getDataFolder(), CONFIGURATION_FILE_NAME), getLogger()),
                        new MarkClearing(MarkClearingKind.BORDER_MARKS, borderMarkIndex::getMarkedBlockCount,
                                this::forgetEveryBorderMark, arenaSetupService::findResetRejection,
                                getLogger()::warning),
                        new MarkClearing(MarkClearingKind.START_GATE_MARKS, startGateMarks::getMarkedBlockCount,
                                () -> startGateMarkStorage.clearEveryMark(startGateMarks),
                                arenaSetupService::findResetRejection, getLogger()::warning),
                        new EffectMarginSetting(arenaRegistry, margin -> liveConfiguration.setArenaEffectMargin(margin)),
                        new EffectMarginConfigurationWriter(this::getConfig,
                                new File(getDataFolder(), CONFIGURATION_FILE_NAME), getLogger()),
                        standingsBoardToolService, () -> bracketBoardService.createBoardItem()),
                new AdminMenuDependencies.KitServices(kitLibrary, kitSaveService),
                new AdminMenuDependencies.RegistrationServices(registrationService, registrationStationService,
                        randomTeamFormation, teamModeSetting, new TeamModeConfigurationWriter(this::getConfig,
                                new File(getDataFolder(), CONFIGURATION_FILE_NAME), getLogger()),
                        teamChoiceService)));

        configuredVisuals = new ConfiguredVisuals(CustomVisualsSelection.select(getServer().getPluginManager(),
                PluginSettings.readItemsAdderEnabled(getConfig(), problem -> { }), this::createItemsAdderVisuals,
                getLogger()::info), getLogger()::warning);
        matchService.setCountdownImages(configuredVisuals);
        matchService.setRoundStartEffects(new ConfiguredRoundStartEffects(new VanillaRoundStartEffects(),
                configuredVisuals::playTotemAnimation));

        liveConfiguration = new PluginLiveConfiguration(matchService, startGateDuration, fighterColours,
                        arenaCleaningService, arenaCleaningVisualEffect, arenaBaselineService, arenaSetupService,
                        arenaSafetyZone, arenaRegistry, borderParticleService, borderWallPreview,
                        borderVisualEffect, teamSizeSetting, teamRegistry, tournamentManager::isTournamentInProgress,
                        adminNotifier::notifyAdmins,
                        getLogger()::warning, messageCatalogLoader::useLanguage, autoMatchService::applySettings,
                        teamModeSetting::setTeamMode, areaInventoryService::setEnabled,
                        configuredVisuals::applySettings, placementSidebar::setEnabled, placementBoard::setEnabled,
                        serverModeSetting::setServerMode);
        voiceIsolation = new VoiceIsolation(this::buildVoiceRoster, new PlayerVoiceNotices(getServer()::getPlayer));
        liveConfiguration.setVoiceChatIsolationEnabledApplier(voiceIsolation::setEnabled);
        matchService.setFighterChangeListener(voiceIsolation::refresh);
        configurationReloadService = new ConfigurationReloadService(this::getConfig, this::reloadConfig,
                liveConfiguration, getLogger()::warning);
        configurationReloadService.applyStoredConfiguration();
        loadArenaBaseline();

        registrationStationDisplays = new RegistrationStationDisplays(registrationStations, getServer()::getWorld,
                new PaperStationDisplayEntities(this), configuredVisuals::findStationItem,
                configuredVisuals::findStationTitleImage, registrationService::getCount,
                configuredVisuals.getCustomVisuals()::isReady);
        registrationStationService.addObserver(registrationStationDisplays);
        registrationStationService.setStationButtonLook(configuredVisuals::findStationItem);
        RegistrationMachines registrationMachines = new RegistrationMachines();
        registrationMachineDisplays = new RegistrationMachineDisplays(this, registrationMachines,
                getServer()::getWorld, registrationService::getCount, configuredVisuals::findMachinePart);
        registrationMachineService = new RegistrationMachineService(this, registrationMachines,
                registrationMachineDisplays, registrationService, new FileRegistrationMachineStorage(
                        new File(getDataFolder(), FileRegistrationMachineStorage.FILE_NAME).toPath()),
                getLogger()::warning);
        registrationMachineService.load();
        registrationStationService.setHandOutItemFactory(registrationMachineService::createMachineItem);
        BracketBoards bracketBoards = new BracketBoards();
        BracketBoardText bracketBoardText = new BracketBoardText(tournamentManager::getState,
                tournamentManager::getBracket,
                () -> matchService.getActiveMatch().filter(match -> !match.isAutoMatch()).map(Match::getMatchId),
                teamRegistry::findTeam, configuredVisuals::findBracketTitleImage, System::currentTimeMillis);
        bracketBoardDisplays = new BracketBoardDisplays(this, bracketBoards, getServer()::getWorld,
                bracketBoardText::build, configuredVisuals::findMachinePart, configuredVisuals::findBoardModel);
        bracketBoardService = new BracketBoardService(this, bracketBoards, bracketBoardDisplays,
                new FileBracketBoardStorage(new File(getDataFolder(), FileBracketBoardStorage.FILE_NAME).toPath()),
                getLogger()::warning);
        bracketBoardService.load();
        registrationStationButtonCheck = new RegistrationStationButtonCheck(registrationStations,
                registrationStationService, getServer()::getWorld, Tag.BUTTONS::isTagged,
                task -> getServer().getScheduler().runTask(this, task), getLogger()::warning);

        registerCommand();
        registerListeners();
        hookSimpleVoiceChat();
        borderParticleService.start();
        arenaMobRemovalService.start();
        chatInputService.start();
        areaInventoryService.start();
        placementSidebar.start();
        placementBoard.refresh();
        placementBoard.start();
        registrationStationButtonCheck.checkLoadedStations();
        registrationStationDisplays.showAll();
        getServer().getScheduler().runTaskTimer(this, registrationStationDisplays::refreshOnceReady,
                VISUALS_READINESS_CHECK_PERIOD_TICKS, VISUALS_READINESS_CHECK_PERIOD_TICKS);
        registrationMachineDisplays.showAll();
        bracketBoardDisplays.showAll();
        getServer().getScheduler().runTaskTimer(this, bracketBoardDisplays::refresh, BRACKET_BOARD_PERIOD_TICKS,
                BRACKET_BOARD_PERIOD_TICKS);
        machineVisualsWereReady = configuredVisuals.getCustomVisuals().isReady();
        getServer().getScheduler().runTaskTimer(this, this::rebuildMachinesOnceVisualsAreReady,
                VISUALS_READINESS_CHECK_PERIOD_TICKS, VISUALS_READINESS_CHECK_PERIOD_TICKS);
        getServer().getScheduler().runTaskTimer(this, registrationMachineDisplays::updateCounters,
                STATION_COUNTER_PERIOD_TICKS, STATION_COUNTER_PERIOD_TICKS);
        getServer().getScheduler().runTaskTimer(this, registrationStationDisplays::updateCounters,
                STATION_COUNTER_PERIOD_TICKS, STATION_COUNTER_PERIOD_TICKS);
        getServer().getScheduler().runTaskTimer(this, registrationStationButtonCheck::checkLoadedStations,
                STATION_BUTTON_CHECK_PERIOD_TICKS, STATION_BUTTON_CHECK_PERIOD_TICKS);
    }

    @Override
    public void onDisable() {
        if (chatInputService != null) {
            chatInputService.stop();
        }
        if (borderParticleService != null) {
            borderParticleService.stop();
        }
        if (arenaMobRemovalService != null) {
            arenaMobRemovalService.stop();
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
        if (autoMatchService != null) {
            autoMatchService.stop();
        }
        if (matchService != null) {
            matchService.cancelActiveMatch();
        }
        if (voiceIsolation != null) {
            voiceIsolation.detach();
        }
        if (areaInventoryService != null) {
            areaInventoryService.stop();
            areaInventoryService.giveEveryInventoryBackQuietly();
        }
        if (startGateOpeningService != null) {
            startGateOpeningService.stopAndRestore();
        }
        if (placementSidebar != null) {
            placementSidebar.stop();
            placementSidebar.giveEveryScoreboardBack();
        }
        if (placementBoard != null) {
            placementBoard.stop();
            placementBoard.hide();
        }
        if (fighterColours != null) {
            fighterColours.clearColours();
        }
        if (spectatorService != null) {
            spectatorService.clearAllAssignments();
        }
        if (registrationStationDisplays != null) {
            registrationStationDisplays.hideAll();
        }
        if (registrationMachineDisplays != null) {
            registrationMachineDisplays.hideAll();
        }
        if (bracketBoardDisplays != null) {
            bracketBoardDisplays.hideAll();
        }
        Messages.reset();
    }

    public void saveTeamConfiguration() {
        teamStorage.save(teamRegistry);
    }

    public void saveRegistrations() {
        registrationStorage.save(registrationList, registrationStations);
    }

    public ConfigurationReloadReport reloadConfiguration() {
        ConfigurationReloadReport report = configurationReloadService.reloadFromFile();
        registrationStationDisplays.refreshAll();
        registrationMachineDisplays.hideAll();
        registrationMachineDisplays.showAll();
        bracketBoardDisplays.hideAll();
        bracketBoardDisplays.showAll();
        placementBoard.rebuild();
        return report;
    }

    public void onPlacementChanged() {
        if (voiceIsolation != null) {
            voiceIsolation.refresh();
        }
        placementSidebar.refresh();
        placementBoard.refresh();
        if (bracketBoardDisplays != null) {
            bracketBoardDisplays.refresh();
        }
    }

    private void rebuildMachinesOnceVisualsAreReady() {
        if (machineVisualsWereReady || !configuredVisuals.getCustomVisuals().isReady()) {
            return;
        }
        machineVisualsWereReady = true;
        registrationMachineDisplays.hideAll();
        registrationMachineDisplays.showAll();
        bracketBoardDisplays.hideAll();
        bracketBoardDisplays.showAll();
        placementBoard.rebuild();
    }

    private void createImportFolder() {
        try {
            teamImportService.createFolder();
        } catch (IOException | RuntimeException failure) {
            getLogger().log(Level.WARNING, Messages.text(PluginMessage.IMPORT_FOLDER_NOT_CREATED,
                    Placeholder.of("folder", teamImportService.describeFolder())), failure);
        }
    }

    private void saveArenaBaseline(ArenaBaselineSnapshot snapshot) {
        try {
            arenaBaselineFile.save(snapshot);
        } catch (IOException | RuntimeException failure) {
            getLogger().log(Level.WARNING, Messages.text(PluginMessage.ARENA_BASELINE_NOT_SAVED,
                    Placeholder.of("file", ArenaBaselineFile.STRUCTURE_FILE_NAME)), failure);
        }
    }

    private void loadArenaBaseline() {
        arenaRegistry.getArena().getShape().ifPresent(shape -> {
            try {
                arenaBaselineFile.load(shape, arenaBaselineService.getDepthBelowFloor()).ifPresent(snapshot -> {
                    arenaBaselineService.restoreSnapshot(snapshot);
                    getLogger().info(Messages.text(PluginMessage.ARENA_BASELINE_LOADED,
                            Placeholder.of("file", ArenaBaselineFile.STRUCTURE_FILE_NAME)));
                });
            } catch (IOException | RuntimeException failure) {
                getLogger().log(Level.WARNING, Messages.text(PluginMessage.ARENA_BASELINE_NOT_LOADED,
                        Placeholder.of("file", ArenaBaselineFile.STRUCTURE_FILE_NAME)), failure);
            }
        });
    }

    private void loadAutoMatchLeaderboard() {
        try {
            autoMatchLeaderboard.load(autoMatchLeaderboardStorage.load());
        } catch (IOException failure) {
            getLogger().log(Level.WARNING, Messages.text(PluginMessage.AUTO_MATCH_WINS_NOT_LOADED,
                    Placeholder.of("file", FileAutoMatchLeaderboardStorage.FILE_NAME)), failure);
        }
    }

    private void onAutoMatchLeaderboardChanged() {
        try {
            autoMatchLeaderboardStorage.save(autoMatchLeaderboard.all());
        } catch (IOException failure) {
            getLogger().log(Level.WARNING, Messages.text(PluginMessage.AUTO_MATCH_WINS_NOT_SAVED,
                    Placeholder.of("file", FileAutoMatchLeaderboardStorage.FILE_NAME)), failure);
        }
        if (placementBoard != null) {
            placementBoard.refresh();
        }
    }

    public void onBorderMarksChanged() {
        borderMarkStorage.save(borderMarkIndex);
        arenaBorderConfirmation.invalidate();
    }

    public void onStartGateMarksChanged() {
        startGateMarkStorage.save(startGateMarks);
    }

    private void forgetEveryBorderMark() throws IOException {
        arenaBorderConfirmation.invalidate();
        borderMarkStorage.clearEveryMark(borderMarkIndex);
    }

    public TournamentManager getTournamentManager() {
        return tournamentManager;
    }

    private VoiceRoster buildVoiceRoster() {
        if (!matchService.hasActiveMatch()) {
            return VoiceRoster.EMPTY;
        }
        Set<UUID> bypassPlayers = new HashSet<>();
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.hasPermission(VoiceIsolation.BYPASS_PERMISSION_NAME)) {
                bypassPlayers.add(player.getUniqueId());
            }
        }
        return VoiceRoster.of(matchService.getActiveMatch(), matchService::isLeftOutOfTheChosenFighters,
                bypassPlayers);
    }

    private void hookSimpleVoiceChat() {
        boolean hooked = VoiceChatHook.hook(getServer().getPluginManager(),
                () -> SimpleVoiceChatBridge.register(getServer(), voiceIsolation,
                        task -> getServer().getScheduler().runTask(this, task)),
                voiceIsolation.isEnabled(), getLogger()::info, getLogger()::warning);
        if (!hooked) {
            return;
        }
        getServer().getScheduler().runTaskTimer(this, voiceIsolation::refresh, VoiceIsolation.REFRESH_PERIOD_TICKS,
                VoiceIsolation.REFRESH_PERIOD_TICKS);
        getServer().getScheduler().runTaskLater(this,
                () -> VoiceChatHook.checkConnection(voiceIsolation, getLogger()::warning),
                VoiceChatHook.CONNECTION_CHECK_DELAY_TICKS);
    }

    private void reportItemsAdderContent() {
        if (itemsAdderContentFailure != null) {
            getLogger().log(Level.WARNING, Messages.text(PluginMessage.ITEMS_ADDER_CONTENT_NOT_INSTALLED),
                    itemsAdderContentFailure);
        } else if (itemsAdderContentChanged) {
            getLogger().info(Messages.text(PluginMessage.ITEMS_ADDER_CONTENT_INSTALLED));
        }
    }

    private void onItemsAdderLoaded() {
        machineVisualsWereReady = false;
        if (!itemsAdderContentChanged || itemsAdderZipRequested) {
            return;
        }
        itemsAdderZipRequested = true;
        getServer().getScheduler().runTask(this, () -> {
            getLogger().info(Messages.text(PluginMessage.ITEMS_ADDER_PACK_REBUILT));
            getServer().dispatchCommand(getServer().getConsoleSender(), ITEMS_ADDER_ZIP_COMMAND);
        });
    }

    private CustomVisuals createItemsAdderVisuals() {
        ItemsAdderVisuals itemsAdderVisuals = new ItemsAdderVisuals(this::onItemsAdderLoaded);
        getServer().getPluginManager().registerEvents(itemsAdderVisuals, this);
        return itemsAdderVisuals;
    }

    static void loadKitLibrary(KitLibraryLoader kitLibraryLoader, ConfigurationSection legacyKitSection,
                               Logger logger, Consumer<String> adminNotifier) {
        try {
            kitLibraryLoader.load(legacyKitSection);
        } catch (RuntimeException failure) {
            String notice = Messages.text(PluginMessage.KITS_NOT_LOADED, Placeholder.of("reason", describe(failure)));
            logger.log(Level.WARNING, notice, failure);
            adminNotifier.accept(notice);
        }
    }

    private static String describe(RuntimeException failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    private void warnAboutLegacyMultiArenaConfiguration() {
        if (!ArenaRegistry.containsLegacyMultiArenaConfiguration(getConfig())) {
            return;
        }
        getLogger().warning("The configuration still holds the entries " + ArenaRegistry.LEGACY_ARENAS_SECTION
                + " and " + ArenaRegistry.LEGACY_FINAL_ARENA_KEY + " of the former multi arena setup. They are "
                + "ignored and dropped with the next arena change, configure the single arena in the section "
                + ArenaRegistry.ARENA_SECTION + " again with " + Arena.describeCalculateAction());
    }

    private void warnAboutLegacyRegionConfiguration() {
        if (!ArenaRegistry.containsLegacyRegionConfiguration(getConfig())) {
            return;
        }
        getLogger().warning("The configuration still holds the rectangle regions of the former arena setup in "
                + ArenaRegistry.ARENA_SECTION + "." + ArenaRegistry.LEGACY_REGIONS_SECTION + ". They are ignored "
                + "and dropped with the next arena change, calculate the fight area again with "
                + Arena.describeCalculateAction());
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
                adminMenuService, registrationService, playAgainService, teamImportService, getLogger());
        pluginCommand.setExecutor(tournamentCommand);
        pluginCommand.setTabCompleter(tournamentCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(matchService, spectatorService, arenaRegistry,
                        borderSelectorModes, kitService, getLogger()), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(matchService), this);
        getServer().getPluginManager().registerEvents(
                new PlayerRespawnListener(this, matchService, spectatorService, arenaRegistry), this);
        getServer().getPluginManager().registerEvents(
                new PlayerMoveListener(matchService, spectatorService, arenaRegistry, getLogger()), this);
        getServer().getPluginManager().registerEvents(
                new SpectatorBorderListener(matchService, spectatorService, spectatorBorderService), this);
        getServer().getPluginManager().registerEvents(
                new PlayerInteractListener(matchService, spectatorService, borderSelectorModes, getLogger()),
                this);
        getServer().getPluginManager().registerEvents(
                new BorderSelectorListener(borderSelectorModes, spectatorBorderService,
                        itemStack -> registrationStationService.isTool(itemStack)
                                || standingsBoardToolService.isTool(itemStack)), this);
        getServer().getPluginManager().registerEvents(
                new ArenaHeightMarkerListener(arenaHeightMarkerService), this);
        getServer().getPluginManager().registerEvents(
                new StartGateListener(startGateItemService), this);
        getServer().getPluginManager().registerEvents(
                new ArenaBuildListener(matchService, arenaRegistry), this);
        getServer().getPluginManager().registerEvents(
                new ArenaBreakListener(matchService, arenaRegistry, arenaBaselineService), this);
        getServer().getPluginManager().registerEvents(
                new ArenaEnvironmentProtectionListener(arenaRegistry, arenaBaselineService), this);
        getServer().getPluginManager().registerEvents(
                new ArenaItemRuleListener(matchService, spectatorService, arenaRegistry), this);
        getServer().getPluginManager().registerEvents(new ArenaMobRuleListener(arenaRegistry), this);
        getServer().getPluginManager().registerEvents(new ArenaSafetyZoneListener(arenaSafetyZone), this);
        getServer().getPluginManager().registerEvents(new AdminMenuListener(adminMenuService), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(chatInputService), this);
        getServer().getPluginManager().registerEvents(
                new GlobalPvpLockListener(matchService, arenaRegistry, getServer()), this);
        getServer().getPluginManager().registerEvents(new FriendlyFireListener(matchService), this);
        getServer().getPluginManager().registerEvents(
                new ArenaDamageListener(matchService, spectatorService, arenaRegistry, getServer()), this);
        getServer().getPluginManager().registerEvents(
                new AreaInventoryListener(this, areaInventoryService, placementSidebar), this);
        getServer().getPluginManager().registerEvents(
                new RegistrationStationListener(registrationStationService), this);
        getServer().getPluginManager().registerEvents(
                new RegistrationStationWorldListener(registrationStationDisplays, registrationStationButtonCheck),
                this);
        getServer().getPluginManager().registerEvents(
                new RegistrationStationButtonListener(registrationStationDisplays, registrationStationService), this);
        getServer().getPluginManager().registerEvents(
                new RegistrationMachineListener(registrationMachineService, registrationMachineDisplays), this);
        getServer().getPluginManager().registerEvents(
                new BracketBoardListener(bracketBoardService, bracketBoardDisplays), this);
        getServer().getPluginManager().registerEvents(new PlacementBoardWorldListener(placementBoard), this);
        getServer().getPluginManager().registerEvents(
                new StandingsBoardToolListener(standingsBoardToolService), this);
    }
}
