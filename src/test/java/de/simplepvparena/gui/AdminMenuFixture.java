package de.simplepvparena.gui;

import de.simplepvparena.arena.ActiveArenaState;
import de.simplepvparena.arena.ArenaAudience;
import de.simplepvparena.arena.ArenaBaselineRegistry;
import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaBorderConfirmation;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaGapGuide;
import de.simplepvparena.arena.ArenaHeightMarkerService;
import de.simplepvparena.arena.ArenaHeightMarkers;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorItemFactory;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.border.BorderParticleService;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.border.BorderVisualEffect;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.config.ConfigurationReloadReport;
import de.simplepvparena.diagnostics.PluginDryRunArena;
import de.simplepvparena.diagnostics.TournamentDryRun;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.gate.StartGateItemService;
import de.simplepvparena.gate.StartGateMarks;
import de.simplepvparena.gate.StartGates;
import de.simplepvparena.input.ChatInputRegistry;
import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.kit.KitDefinition;
import de.simplepvparena.kit.KitEquipmentSlot;
import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.listener.ChatInputListener;
import de.simplepvparena.match.FighterColours;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.UnequalTeamSettings;
import de.simplepvparena.match.UnequalTeamVoteService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.permission.ViewPermission;
import de.simplepvparena.registration.RandomTeamFormation;
import de.simplepvparena.registration.RegistrationList;
import de.simplepvparena.registration.RegistrationService;
import de.simplepvparena.registration.RegistrationStationService;
import de.simplepvparena.registration.RegistrationStations;
import de.simplepvparena.registration.TeamChoiceService;
import de.simplepvparena.registration.TeamMode;
import de.simplepvparena.registration.TeamModeSetting;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.InMemoryKitLibraryStorage;
import de.simplepvparena.support.RecordingItemStack;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.TickingScheduler;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.AutoMatchService;
import de.simplepvparena.tournament.AutoMatchState;
import de.simplepvparena.tournament.AutomaticArenaCleanup;
import de.simplepvparena.tournament.KitChoiceService;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentOperationResult;
import de.simplepvparena.tournament.TournamentStateStorage;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdminMenuFixture {

    private static final String WORLD_NAME = "admin-menu";
    private static final int GROUND_Y = 64;
    private static final double INSIDE_COORDINATE = 3.5D;
    private static final double OUTSIDE_COORDINATE = 11.5D;

    private static final int WALL_MINIMUM = 0;
    private static final int WALL_MAXIMUM = 6;
    private static final int WALL_TOP_Y = GROUND_Y + 3;
    private static final int SEED_COORDINATE = 3;

    private static final Logger LOGGER = createQuietLogger();

    private final RecordingAdminMenuWindows windows = new RecordingAdminMenuWindows();
    private final List<String> messages = new ArrayList<>();
    private final List<Component> sentMessages = new ArrayList<>();
    private final UUID adminUniqueId = UUID.randomUUID();
    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final ArenaRegistry arenaRegistry = new ArenaRegistry();
    private final BorderMarkIndex borderMarks = new BorderMarkIndex();
    private final ArenaHeightMarkers heightMarkers = new ArenaHeightMarkers();
    private final StartGateMarks gateMarks = new StartGateMarks();
    private final StartGateDuration gateDuration = new StartGateDuration();
    private final BorderSelectorModeRegistry borderSelectorModes = new BorderSelectorModeRegistry();
    private final KitService kitService = new KitService();
    private final InMemoryKitLibraryStorage kitStorage = new InMemoryKitLibraryStorage();
    private final List<String> kitWriteFailures = new ArrayList<>();
    private final KitLibrary kitLibrary = new KitLibrary(kitStorage, kitService, new FakeKitItemSerializer(),
            problem -> { }, kitWriteFailures::add);
    private final List<List<String>> reloadedConfigurations = new ArrayList<>();
    private final ChatInputRegistry chatInputRegistry = new ChatInputRegistry();
    private final List<Runnable> scheduledTasks = new ArrayList<>();
    private final Map<String, TeamMember> knownPlayersByLowerCaseName = new LinkedHashMap<>();
    private final List<ItemStack> handedOutItems = new ArrayList<>();
    private final Map<Integer, ItemStack> adminInventoryItems = new HashMap<>();
    private final List<Player> onlinePlayers = new ArrayList<>();
    private final List<String> adminNotifications = new ArrayList<>();
    private final List<Integer> writtenTeamSizes = new ArrayList<>();
    private final List<Boolean> writtenAutoMatchModes = new ArrayList<>();
    private final List<Boolean> writtenBorderParticleSwitches = new ArrayList<>();
    private final List<TeamMode> writtenTeamModes = new ArrayList<>();
    private final TeamModeSetting teamModeSetting = new TeamModeSetting();
    private final TickingScheduler kitChoiceScheduler = new TickingScheduler();
    private final TickingScheduler voteScheduler = new TickingScheduler();
    private final RegistrationList registrationList = new RegistrationList();
    private final RegistrationService registrationService;
    private final RegistrationStations registrationStations = new RegistrationStations();
    private final RegistrationStationService registrationStationService;
    private final TeamChoiceService teamChoiceService;
    private final KitChoiceService kitChoiceService;
    private final UnequalTeamVoteService unequalTeamVoteService;
    private final World world;
    private final Plugin plugin;
    private final MatchService matchService;
    private final ArenaSetupService arenaSetupService;
    private final TournamentManager tournamentManager;
    private final AutoMatchService autoMatchService;
    private final BorderParticleService borderParticleService;
    private final ChatInputService chatInputService;
    private final ChatInputListener chatInputListener;
    private final AdminMenuService menuService;
    private final PlayerInventory adminInventory;
    private final Player admin;

    private boolean adminPermissionGranted = true;
    private boolean adminInventoryFull;
    private Location adminLocation;
    private int teamConfigurationSaveCount;
    private int arenaConfigurationSaveCount;
    private int registrationSaveCount;
    private RandomGenerator randomGenerator = new OrderKeepingRandomGenerator();
    private IOException teamSizeWriteFailure;
    private IOException autoMatchModeWriteFailure;
    private IOException borderParticleSwitchWriteFailure;
    private IOException teamModeWriteFailure;

    AdminMenuFixture() {
        this(false);
    }

    private AdminMenuFixture(boolean kitChoiceEnabled) {
        SharedBukkitServer.install();
        world = SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> WORLD_NAME;
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
        plugin = createPlugin(createServer());
        adminLocation = new Location(world, INSIDE_COORDINATE, GROUND_Y, INSIDE_COORDINATE);
        matchService = new MatchService(plugin, kitService,
                new SpectatorService(arenaRegistry, new SpectatorItemFactory(plugin)),
                SharedBukkitServer.proxy(FighterColours.class, (method, arguments) -> null), StartGates.NONE);
        unequalTeamVoteService = new UnequalTeamVoteService(voteScheduler.plugin(), this::findOnlinePlayer,
                new OrderKeepingRandomGenerator());
        matchService.setUnequalTeamVoteService(unequalTeamVoteService);
        ArenaCleaningService cleaningService = new ArenaCleaningService(plugin, (cleanedWorld, shape) -> 0);
        ArenaBorderConfirmation borderConfirmation = new ArenaBorderConfirmation(arenaRegistry.getArena()::hasShape);
        borderParticleService = new BorderParticleService(plugin, borderMarks,
                SharedBukkitServer.proxy(BorderVisualEffect.class, (method, arguments) -> null),
                matchService::isActiveFighter, borderConfirmation);
        arenaSetupService = new ArenaSetupService(
                new ActiveArenaState(arenaRegistry, heightMarkers), matchService,
                new SpectatorBorderService(borderMarks, () -> { }),
                new BorderWallPreview(plugin, SharedBukkitServer.proxy(BorderVisualEffect.class,
                        (method, arguments) -> null)),
                new ArenaGapGuide(plugin), cleaningService,
                new ArenaBaselineService(plugin, new ArenaBaselineRegistry()),
                borderConfirmation,
                () -> arenaConfigurationSaveCount++);
        AutomaticArenaCleanup arenaCleanup = new AutomaticArenaCleanup(arenaSetupService, cleaningService);
        if (kitChoiceEnabled) {
            kitChoiceService = new KitChoiceService(kitChoiceScheduler.plugin(), kitLibrary,
                    adminNotifications::add, kitChoiceScheduler.clock());
            tournamentManager = new TournamentManager(plugin, teamRegistry, arenaRegistry, matchService,
                    arenaCleanup, message -> { }, TournamentStateStorage.NONE, kitChoiceService);
        } else {
            tournamentManager = new TournamentManager(plugin, teamRegistry, arenaRegistry, matchService,
                    arenaCleanup, message -> { });
            kitChoiceService = tournamentManager.getKitChoiceService();
        }
        arenaSetupService.setArenaReservedForMatch(kitChoiceService::isPending);
        autoMatchService = new AutoMatchService(plugin, teamRegistry, arenaRegistry, matchService, tournamentManager,
                kitLibrary, arenaCleanup, adminNotifications::add,
                new ArenaAudience(() -> List.copyOf(onlinePlayers), arenaRegistry));
        arenaSetupService.setArenaReservedForAutoMatch(autoMatchService::isAnnounced);
        registrationService = new RegistrationService(registrationList, tournamentManager::isTournamentInProgress,
                () -> registrationSaveCount++);
        registrationStationService = new RegistrationStationService(plugin, registrationStations,
                registrationService, material -> material.name().endsWith("_BUTTON"), () -> registrationSaveCount++,
                material -> new RecordingItemStack(material, 1));
        teamChoiceService = new TeamChoiceService(registrationService, teamRegistry,
                tournamentManager.getTeamSizeSetting(), teamModeSetting, tournamentManager::isTournamentInProgress,
                tournamentManager::findTeamChangeRejection, this::saveTeamConfiguration, this::findOnlinePlayer);
        registrationService.addObserver(teamChoiceService);
        TournamentDryRun tournamentDryRun = new TournamentDryRun(tournamentManager::getState,
                matchService::hasActiveMatch, teamRegistry,
                new PluginDryRunArena(arenaRegistry.getArena(), arenaSetupService), kitService, gateMarks,
                gateDuration, tournamentManager.getTeamSizeSetting());
        chatInputService = new ChatInputService(plugin, chatInputRegistry);
        chatInputListener = new ChatInputListener(chatInputService);
        adminInventory = createAdminInventory();
        admin = createAdmin();
        onlinePlayers.add(admin);
        Server server = plugin.getServer();
        menuService = new AdminMenuService(plugin, tournamentManager, tournamentDryRun, teamRegistry,
                new TeamMemberLookup(server),
                matchService, arenaSetupService, new ArenaHeightMarkerService(plugin, heightMarkers),
                new StartGateItemService(plugin, gateMarks, gateDuration, () -> { }),
                borderSelectorModes,
                kitLibrary,
                new KitSaveService(kitLibrary, new FakeKitItemSerializer()),
                chatInputService, this::saveTeamConfiguration,
                this::reloadConfiguration,
                this::writeTeamSize,
                autoMatchService,
                this::writeAutoMatchMode,
                borderParticleService,
                this::writeBorderParticleSwitch,
                registrationService,
                registrationStationService,
                new RandomTeamFormation(registrationService, teamRegistry, tournamentManager.getTeamSizeSetting(),
                        tournamentManager::isTournamentInProgress,
                        () -> autoMatchService.isAnnounced() || autoMatchService.getState() == AutoMatchState.RUNNING,
                        this::saveTeamConfiguration, this::findOnlinePlayer,
                        new SwitchableRandomGenerator()),
                teamModeSetting,
                this::writeTeamMode,
                teamChoiceService,
                windows,
                () -> List.copyOf(onlinePlayers));
    }

    static AdminMenuFixture withKitChoice() {
        return new AdminMenuFixture(true);
    }

    OnlinePlayer letAnotherPlayerJoin(String playerName, boolean adminPermission) {
        return letAnotherPlayerJoin(playerName, adminPermission, false);
    }

    OnlinePlayer letAnotherPlayerJoin(String playerName, boolean adminPermission, boolean viewPermission) {
        List<String> playerMessages = new ArrayList<>();
        UUID playerUniqueId = UUID.randomUUID();
        Player player = SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> playerUniqueId;
            case "getName" -> playerName;
            case "isOnline" -> true;
            case "hasPermission" -> AdminPermission.ADMIN_PERMISSION_NAME.equals(arguments[0]) && adminPermission
                    || ViewPermission.VIEW_PERMISSION_NAME.equals(arguments[0]) && viewPermission;
            case "sendMessage" -> {
                playerMessages.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                yield null;
            }
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
        onlinePlayers.add(player);
        return new OnlinePlayer(player, playerMessages);
    }

    TournamentCommand createTournamentCommand() {
        return new TournamentCommand(matchService,
                new SpectatorService(arenaRegistry, new SpectatorItemFactory(plugin)), menuService,
                registrationService);
    }

    void letTheAdminLeave() {
        onlinePlayers.remove(admin);
    }

    void openMainMenuFor(Player player) {
        menuService.openMainMenu(player);
    }

    void clickAs(Player player, int slot) {
        menuService.handleClick(player, windows.lastOpenedMenu(player).holder(), slot);
    }

    void clickInAs(Player player, AdminMenuHolder holder, int slot) {
        menuService.handleClick(player, holder, slot);
    }

    void openBracketViewFor(Player player) {
        menuService.openBracketView(player);
    }

    Optional<RecordingAdminMenuWindows.RecordedMenu> findLastMenuOf(Player player) {
        return windows.findLastOpenedMenu(player);
    }

    boolean isMenuClosedFor(Player player) {
        return windows.isMenuClosed(player);
    }

    KitChoiceService kitChoiceService() {
        return kitChoiceService;
    }

    TickingScheduler kitChoiceScheduler() {
        return kitChoiceScheduler;
    }

    UnequalTeamVoteService unequalTeamVoteService() {
        return unequalTeamVoteService;
    }

    TickingScheduler voteScheduler() {
        return voteScheduler;
    }

    void registerTeamOfOnlinePlayers(String teamName, OnlinePlayer... players) {
        List<TeamMember> members = new ArrayList<>();
        for (OnlinePlayer player : players) {
            members.add(new TeamMember(player.player().getUniqueId(), player.player().getName()));
        }
        teamRegistry.registerTeam(teamName, members);
    }

    Match beginUnequalTeamVote(String firstTeamName, String secondTeamName) {
        Match match = Match.regularMatch("M1", teamRegistry.findTeam(firstTeamName).orElseThrow(),
                teamRegistry.findTeam(secondTeamName).orElseThrow(), arenaRegistry.getArena());
        unequalTeamVoteService.begin(match, UnequalTeamSettings.defaults(), () -> { });
        return match;
    }

    void letPlayerLeave(OnlinePlayer player) {
        onlinePlayers.remove(player.player());
        unequalTeamVoteService.handlePlayerLeft(player.player().getUniqueId());
    }

    List<String> adminNotifications() {
        return adminNotifications;
    }

    RegistrationList registrationList() {
        return registrationList;
    }

    RegistrationService registrationService() {
        return registrationService;
    }

    int registrationSaveCount() {
        return registrationSaveCount;
    }

    RegistrationStationService registrationStationService() {
        return registrationStationService;
    }

    void registerPlayers(String... playerNames) {
        for (String playerName : playerNames) {
            registrationList.register(UUID.randomUUID(), playerName);
        }
    }

    void shuffleWith(RandomGenerator randomGenerator) {
        this.randomGenerator = randomGenerator;
    }

    private Player findOnlinePlayer(UUID playerUniqueId) {
        return onlinePlayers.stream()
                .filter(player -> player.getUniqueId().equals(playerUniqueId))
                .findFirst()
                .orElse(null);
    }

    record OnlinePlayer(Player player, List<String> messages) {
    }

    void registerTeams(String... teamNames) {
        for (String teamName : teamNames) {
            teamRegistry.registerTeam(teamName, List.of(
                    new TeamMember(UUID.randomUUID(), teamName + "-first"),
                    new TeamMember(UUID.randomUUID(), teamName + "-second")));
        }
    }

    void registerTeamOf(String teamName, String... playerNames) {
        List<TeamMember> members = new ArrayList<>();
        for (String playerName : playerNames) {
            members.add(new TeamMember(UUID.randomUUID(), playerName));
        }
        teamRegistry.registerTeam(teamName, members);
    }

    void registerNumberedTeams(int teamCount) {
        for (int teamNumber = 1; teamNumber <= teamCount; teamNumber++) {
            registerTeams(numberedTeamName(teamNumber));
        }
    }

    static String numberedTeamName(int teamNumber) {
        return String.format(Locale.ROOT, "Team%02d", teamNumber);
    }

    void setTeamSize(int teamSize) {
        tournamentManager.getTeamSizeSetting().setTeamSize(teamSize);
    }

    int teamSize() {
        return tournamentManager.getTeamSizeSetting().getTeamSize();
    }

    List<Integer> writtenTeamSizes() {
        return writtenTeamSizes;
    }

    void failTeamSizeWrites(IOException failure) {
        teamSizeWriteFailure = failure;
    }

    AutoMatchService autoMatchService() {
        return autoMatchService;
    }

    List<Boolean> writtenAutoMatchModes() {
        return writtenAutoMatchModes;
    }

    void failAutoMatchModeWrites(IOException failure) {
        autoMatchModeWriteFailure = failure;
    }

    BorderParticleService borderParticleService() {
        return borderParticleService;
    }

    List<Boolean> writtenBorderParticleSwitches() {
        return writtenBorderParticleSwitches;
    }

    void failBorderParticleSwitchWrites(IOException failure) {
        borderParticleSwitchWriteFailure = failure;
    }

    TeamChoiceService teamChoiceService() {
        return teamChoiceService;
    }

    void openTeamChoiceFor(Player player) {
        menuService.openTeamChoice(player);
    }

    TeamModeSetting teamModeSetting() {
        return teamModeSetting;
    }

    List<TeamMode> writtenTeamModes() {
        return writtenTeamModes;
    }

    void failTeamModeWrites(IOException failure) {
        teamModeWriteFailure = failure;
    }

    void makePlayersKnown(String... playerNames) {
        for (String playerName : playerNames) {
            knownPlayersByLowerCaseName.put(playerName.toLowerCase(Locale.ROOT),
                    new TeamMember(UUID.randomUUID(), playerName));
        }
    }

    TeamMember knownPlayer(String playerName) {
        return findKnownPlayer(playerName).orElseThrow();
    }

    void configureArena() {
        storeTheFightArea();
        arenaRegistry.getArena().setTribuneSpawn(new PositionSnapshot(WORLD_NAME,
                OUTSIDE_COORDINATE, GROUND_Y, OUTSIDE_COORDINATE, 0F, 0F));
    }

    void storeTheFightArea() {
        for (int coordinate = WALL_MINIMUM; coordinate <= WALL_MAXIMUM; coordinate++) {
            for (int wallY = GROUND_Y; wallY <= WALL_TOP_Y; wallY++) {
                borderMarks.mark(WORLD_NAME, coordinate, wallY, WALL_MINIMUM);
                borderMarks.mark(WORLD_NAME, coordinate, wallY, WALL_MAXIMUM);
                borderMarks.mark(WORLD_NAME, WALL_MINIMUM, wallY, coordinate);
                borderMarks.mark(WORLD_NAME, WALL_MAXIMUM, wallY, coordinate);
            }
        }
        ArenaShape shape = ArenaShapeFloodFill.calculate(WORLD_NAME, SEED_COORDINATE, GROUND_Y, SEED_COORDINATE,
                        ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT, borderMarks.createColumnReader(WORLD_NAME))
                .getShape().orElseThrow();
        arenaRegistry.getArena().setShape(shape);
    }

    void startTournament() {
        assertSuccessful(tournamentManager.startTournament(false));
    }

    void pauseTournament() {
        assertSuccessful(tournamentManager.togglePause());
    }

    void startFinal() {
        assertSuccessful(tournamentManager.startFinalMatch());
    }

    void decideRunningMatchFor(String teamName) {
        assertSuccessful(tournamentManager.forceMatchResult(runningMatch().getMatchId(), teamName));
    }

    void startMatchWithoutATournament(String firstTeamName, String secondTeamName) {
        matchService.startMatch(Match.regularMatch("M1", teamRegistry.findTeam(firstTeamName).orElseThrow(),
                teamRegistry.findTeam(secondTeamName).orElseThrow(), arenaRegistry.getArena()));
    }

    Match runningMatch() {
        return matchService.getActiveMatch().orElseThrow();
    }

    String runningFirstTeamName() {
        return runningMatch().getFirstTeam().getTeamName();
    }

    String runningSecondTeamName() {
        return runningMatch().getSecondTeam().getTeamName();
    }

    void createKit(String kitName) {
        kitLibrary.createKit(kitName, FakeSerializedKits.wearingAndCarrying(
                Map.of(KitEquipmentSlot.HEAD, "helmet", KitEquipmentSlot.CHEST, "chestplate"),
                "sword", "bow", "arrows"));
    }

    void createNumberedKits(int kitCount) {
        for (int kitNumber = 1; kitNumber <= kitCount; kitNumber++) {
            kitLibrary.createKit(numberedKitName(kitNumber), FakeSerializedKits.carrying("item" + kitNumber));
        }
    }

    static String numberedKitName(int kitNumber) {
        return String.format(Locale.ROOT, "Kit%02d", kitNumber);
    }

    List<String> describeKit() {
        return kitService.getKitDefinition().describe();
    }

    KitDefinition activeKitDefinition() {
        return kitService.getKitDefinition();
    }

    void placeInAdminInventory(int slotIndex, ItemStack itemStack) {
        adminInventoryItems.put(slotIndex, itemStack);
    }

    void clearAdminInventory() {
        adminInventoryItems.clear();
    }

    void fillAdminInventory() {
        adminInventoryFull = true;
    }

    void switchBorderSelectorModeOn() {
        assertTrue(borderSelectorModes.toggle(adminUniqueId));
    }

    void standInsideTheFightArea() {
        adminLocation = new Location(world, INSIDE_COORDINATE, GROUND_Y, INSIDE_COORDINATE);
    }

    void standOutsideTheFightArea() {
        adminLocation = new Location(world, OUTSIDE_COORDINATE, GROUND_Y, OUTSIDE_COORDINATE);
    }

    void standAt(double x, double z) {
        adminLocation = new Location(world, x, GROUND_Y, z);
    }

    void revokeAdminPermission() {
        adminPermissionGranted = false;
    }

    void openMainMenu() {
        menuService.openMainMenu(admin);
    }

    void openBracketView() {
        menuService.openBracketView(admin);
    }

    void openMenuFromTheMainMenu(int mainMenuSlot) {
        openMainMenu();
        click(mainMenuSlot);
        messages.clear();
    }

    void click(int slot) {
        menuService.handleClick(admin, lastMenu().holder(), slot);
    }

    void clickIn(AdminMenuHolder holder, int slot) {
        menuService.handleClick(admin, holder, slot);
    }

    void refillLastMenu() {
        windows.refillLastOpenedMenu(admin);
    }

    void refillLastMenuOf(Player player) {
        windows.refillLastOpenedMenu(player);
    }

    boolean typeInChat(String line) {
        return chatInputService.handleLine(admin, line);
    }

    AsyncChatEvent sendChatEvent(Player sender, String line, Set<Audience> viewers) {
        Component message = Component.text(line);
        AsyncChatEvent event = new AsyncChatEvent(true, sender, viewers, ChatRenderer.defaultRenderer(), message,
                message, SignedMessage.system(line, message));
        chatInputListener.onAsyncChat(event);
        return event;
    }

    void runNextTick() {
        List<Runnable> tasksOfThisTick = new ArrayList<>(scheduledTasks);
        scheduledTasks.clear();
        tasksOfThisTick.forEach(Runnable::run);
    }

    int scheduledTaskCount() {
        return scheduledTasks.size();
    }

    int pendingChatInputCount() {
        return chatInputRegistry.getPendingCount();
    }

    RecordingAdminMenuWindows.RecordedMenu lastMenu() {
        return windows.lastOpenedMenu(admin);
    }

    boolean isMenuClosed() {
        return windows.isMenuClosed(admin);
    }

    String describeLastMenu() {
        return describeLastMenuOf(admin);
    }

    String describeLastMenuOf(Player player) {
        RecordingAdminMenuWindows.RecordedMenu menu = windows.lastOpenedMenu(player);
        StringBuilder description = new StringBuilder(menu.title()).append(" | ").append(menu.size()).append('\n');
        for (int slot = 0; slot < menu.size(); slot++) {
            int describedSlot = slot;
            menu.findTile(slot).ifPresent(tile -> description.append(describeTile(describedSlot, tile))
                    .append('\n'));
        }
        return description.toString();
    }

    private static String describeTile(int slot, MenuTile tile) {
        String line = slot + " | " + tile.material() + " | " + tile.displayName();
        if (tile.loreLines().isEmpty()) {
            return line;
        }
        return line + " | " + String.join(" / ", tile.loreLines());
    }

    List<String> messages() {
        return messages;
    }

    Component lastSentMessage() {
        return sentMessages.get(sentMessages.size() - 1);
    }

    List<List<String>> reloadedConfigurations() {
        return reloadedConfigurations;
    }

    int teamConfigurationSaveCount() {
        return teamConfigurationSaveCount;
    }

    int arenaConfigurationSaveCount() {
        return arenaConfigurationSaveCount;
    }

    KitLibrary kitLibrary() {
        return kitLibrary;
    }

    InMemoryKitLibraryStorage kitStorage() {
        return kitStorage;
    }

    List<String> kitWriteFailures() {
        return kitWriteFailures;
    }

    List<ItemStack> handedOutItems() {
        return handedOutItems;
    }

    Player admin() {
        return admin;
    }

    TournamentManager tournamentManager() {
        return tournamentManager;
    }

    MatchService matchService() {
        return matchService;
    }

    ArenaRegistry arenaRegistry() {
        return arenaRegistry;
    }

    TeamRegistry teamRegistry() {
        return teamRegistry;
    }

    ArenaSetupService arenaSetupService() {
        return arenaSetupService;
    }

    ChatInputService chatInputService() {
        return chatInputService;
    }

    boolean isBorderSelectorModeOn() {
        return borderSelectorModes.isActive(adminUniqueId);
    }

    private ConfigurationReloadReport reloadConfiguration() {
        ConfigurationReloadReport report = ConfigurationReloadReport.of(List.of());
        reloadedConfigurations.add(report.describe());
        return report;
    }

    private Optional<TeamMember> findKnownPlayer(String playerName) {
        return Optional.ofNullable(knownPlayersByLowerCaseName.get(playerName.toLowerCase(Locale.ROOT)));
    }

    private OfflinePlayer findKnownOfflinePlayer(String playerName) {
        return findKnownPlayer(playerName)
                .map(member -> SharedBukkitServer.proxy(OfflinePlayer.class, (method, arguments) ->
                        switch (method.getName()) {
                            case "getUniqueId" -> member.uniqueId();
                            case "getName" -> member.playerName();
                            default -> throw new UnsupportedOperationException("OfflinePlayer." + method.getName());
                        }))
                .orElse(null);
    }

    private void saveTeamConfiguration() {
        teamConfigurationSaveCount++;
    }

    private void writeTeamSize(int teamSize) throws IOException {
        if (teamSizeWriteFailure != null) {
            throw teamSizeWriteFailure;
        }
        writtenTeamSizes.add(teamSize);
    }

    private void writeAutoMatchMode(boolean enabled) throws IOException {
        if (autoMatchModeWriteFailure != null) {
            throw autoMatchModeWriteFailure;
        }
        writtenAutoMatchModes.add(enabled);
    }

    private void writeBorderParticleSwitch(boolean enabled) throws IOException {
        if (borderParticleSwitchWriteFailure != null) {
            throw borderParticleSwitchWriteFailure;
        }
        writtenBorderParticleSwitches.add(enabled);
    }

    private void writeTeamMode(TeamMode teamMode) throws IOException {
        if (teamModeWriteFailure != null) {
            throw teamModeWriteFailure;
        }
        writtenTeamModes.add(teamMode);
    }

    private static void assertSuccessful(TournamentOperationResult result) {
        assertTrue(result.successful(), result.message());
    }

    private Player createAdmin() {
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> adminUniqueId;
            case "getName" -> "admin";
            case "hasPermission" -> AdminPermission.ADMIN_PERMISSION_NAME.equals(arguments[0])
                    && adminPermissionGranted;
            case "getLocation" -> adminLocation.clone();
            case "getInventory" -> adminInventory;
            case "sendMessage" -> {
                sentMessages.add((Component) arguments[0]);
                messages.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                yield null;
            }
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }

    private PlayerInventory createAdminInventory() {
        return SharedBukkitServer.proxy(PlayerInventory.class, (method, arguments) -> switch (method.getName()) {
            case "getItem" -> adminInventoryItems.get(arguments[0]);
            case "addItem" -> {
                if (adminInventoryFull) {
                    yield new HashMap<>(Map.of(0, ((ItemStack[]) arguments[0])[0]));
                }
                handedOutItems.addAll(List.of((ItemStack[]) arguments[0]));
                yield new HashMap<Integer, ItemStack>();
            }
            default -> throw new UnsupportedOperationException("PlayerInventory." + method.getName());
        });
    }

    private Plugin createPlugin(Server server) {
        return SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getServer" -> server;
            case "getLogger" -> LOGGER;
            case "getName", "namespace" -> "simplepvparena";
            case "isEnabled" -> true;
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
    }

    private Server createServer() {
        BukkitScheduler scheduler = SharedBukkitServer.proxy(BukkitScheduler.class,
                (method, arguments) -> switch (method.getName()) {
                    case "runTask" -> scheduleForTheNextTick((Runnable) arguments[1]);
                    case "runTaskLater", "runTaskTimer" -> schedule();
                    default -> throw new UnsupportedOperationException("BukkitScheduler." + method.getName());
                });
        return SharedBukkitServer.proxy(Server.class, (method, arguments) -> switch (method.getName()) {
            case "getScheduler" -> scheduler;
            case "getWorld" -> WORLD_NAME.equals(arguments[0]) ? world : null;
            case "getPlayer", "getPlayerExact" -> null;
            case "getOfflinePlayerIfCached" -> findKnownOfflinePlayer((String) arguments[0]);
            case "getLogger" -> LOGGER;
            case "broadcast" -> 0;
            default -> throw new UnsupportedOperationException("Server." + method.getName());
        });
    }

    private BukkitTask scheduleForTheNextTick(Runnable task) {
        scheduledTasks.add(task);
        return schedule();
    }

    private static BukkitTask schedule() {
        boolean[] cancelled = new boolean[1];
        return SharedBukkitServer.proxy(BukkitTask.class, (method, arguments) -> switch (method.getName()) {
            case "cancel" -> {
                cancelled[0] = true;
                yield null;
            }
            case "isCancelled" -> cancelled[0];
            case "getTaskId" -> 1;
            default -> throw new UnsupportedOperationException("BukkitTask." + method.getName());
        });
    }

    private static Logger createQuietLogger() {
        Logger logger = Logger.getLogger("AdminMenuFixture");
        logger.setUseParentHandlers(false);
        return logger;
    }

    private final class SwitchableRandomGenerator implements RandomGenerator {

        @Override
        public int nextInt(int bound) {
            return randomGenerator.nextInt(bound);
        }

        @Override
        public long nextLong() {
            return randomGenerator.nextLong();
        }
    }

    private static final class OrderKeepingRandomGenerator implements RandomGenerator {

        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("only bounded integers are drawn");
        }
    }
}
