package de.simplepvparena.gui;

import de.simplepvparena.arena.ActiveArenaState;
import de.simplepvparena.arena.ArenaBaselineRegistry;
import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaBorderConfirmation;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaGapGuide;
import de.simplepvparena.arena.ArenaHeightMarkerService;
import de.simplepvparena.arena.ArenaHeightMarkers;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.border.BorderVisualEffect;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.diagnostics.PluginDryRunArena;
import de.simplepvparena.diagnostics.TournamentDryRun;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.gate.StartGateItemService;
import de.simplepvparena.gate.StartGateMarks;
import de.simplepvparena.gate.StartGates;
import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.team.EventTeamImportFile;
import de.simplepvparena.team.EventTeamImportService;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.AutomaticArenaCleanup;
import de.simplepvparena.tournament.TournamentManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminMenuSeamTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int MAIN_CLOSE_SLOT = 22;
    private static final int TEAMS_BACK_SLOT = 49;

    private static final Logger LOGGER = Logger.getLogger("AdminMenuSeamTest");

    private final RecordingAdminMenuWindows windows = new RecordingAdminMenuWindows();
    private final List<String> messages = new ArrayList<>();

    private AdminMenuService menuService;
    private Player player;
    private boolean adminPermissionGranted = true;

    @BeforeEach
    void buildTheMenuService(@TempDir Path pluginFolder) {
        Server server = proxy(Server.class, (method, arguments) -> {
            throw new UnsupportedOperationException("Server." + method.getName());
        });
        Plugin plugin = proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getServer" -> server;
            case "getLogger" -> LOGGER;
            case "getName", "namespace" -> "simplepvparena";
            case "isEnabled" -> true;
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
        BorderVisualEffect silentVisualEffect = proxy(BorderVisualEffect.class, (method, arguments) -> null);
        TeamRegistry teamRegistry = new TeamRegistry();
        ArenaRegistry arenaRegistry = new ArenaRegistry();
        ArenaHeightMarkers heightMarkers = new ArenaHeightMarkers();
        KitService kitService = new KitService();
        StartGateMarks gateMarks = new StartGateMarks();
        StartGateDuration gateDuration = new StartGateDuration();
        MatchService matchService = new MatchService(plugin, kitService, null, null, StartGates.NONE);
        ArenaCleaningService cleaningService = new ArenaCleaningService(plugin, (world, shape) -> 0);
        ArenaSetupService arenaSetupService = new ArenaSetupService(
                new ActiveArenaState(arenaRegistry, heightMarkers), matchService,
                new SpectatorBorderService(new BorderMarkIndex(), () -> { }),
                new BorderWallPreview(plugin, silentVisualEffect), new ArenaGapGuide(plugin), cleaningService,
                new ArenaBaselineService(plugin, new ArenaBaselineRegistry()),
                new ArenaBorderConfirmation(arenaRegistry.getArena()::hasShape), () -> { });
        TournamentManager tournamentManager = new TournamentManager(plugin, teamRegistry, arenaRegistry,
                matchService, new AutomaticArenaCleanup(arenaSetupService, cleaningService), message -> { });
        TournamentDryRun tournamentDryRun = new TournamentDryRun(tournamentManager::getState,
                matchService::hasActiveMatch, teamRegistry,
                new PluginDryRunArena(arenaRegistry.getArena(), arenaSetupService), kitService, gateMarks,
                gateDuration);
        menuService = new AdminMenuService(plugin, tournamentManager, tournamentDryRun, teamRegistry,
                new TeamMemberLookup(server),
                new EventTeamImportService(pluginFolder.resolve(EventTeamImportFile.FILE_NAME).toFile(),
                        teamRegistry, playerName -> Optional.empty(), () -> { }),
                matchService, arenaSetupService, new ArenaHeightMarkerService(plugin, heightMarkers),
                new StartGateItemService(plugin, gateMarks, gateDuration, () -> { }),
                new BorderSelectorModeRegistry(),
                new KitSaveService(kitService, serializedKit -> { }, problem -> { }),
                new ChatInputService(plugin), () -> { },
                () -> {
                    throw new UnsupportedOperationException("the configuration is not reloaded in this test");
                },
                windows);
        player = createPlayer();
    }

    @Test
    void recordsTheMainMenuWithoutAServer() {
        menuService.openMainMenu(player);

        RecordingAdminMenuWindows.RecordedMenu menu = windows.lastOpenedMenu(player);
        assertEquals(AdminMenuType.MAIN, menu.holder().getMenuType());
        assertEquals("Tournament administration", menu.title());
        assertEquals(27, menu.size());
        assertEquals("Teams", menu.findTile(MAIN_TEAMS_SLOT).orElseThrow().displayName(),
                menu.describeTiles().toString());
        assertFalse(windows.isMenuClosed(player));
    }

    @Test
    void opensTheTeamsMenuFromTheMainMenu() {
        menuService.openMainMenu(player);

        clickInLastOpenedMenu(MAIN_TEAMS_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = windows.lastOpenedMenu(player);
        assertEquals(AdminMenuType.TEAMS, menu.holder().getMenuType());
        assertEquals(0, menu.holder().getPageIndex());
        assertEquals(54, menu.size());
        assertEquals("Teams (page 1)", menu.title());
    }

    @Test
    void returnsFromTheTeamsMenuToTheMainMenu() {
        menuService.openMainMenu(player);
        clickInLastOpenedMenu(MAIN_TEAMS_SLOT);
        AdminMenuHolder teamsHolder = windows.lastOpenedMenu(player).holder();

        clickInLastOpenedMenu(TEAMS_BACK_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = windows.lastOpenedMenu(player);
        assertEquals(AdminMenuType.MAIN, menu.holder().getMenuType());
        assertEquals("Tournament administration", menu.title());
        assertEquals(27, menu.size());
        assertNotSame(teamsHolder, menu.holder(), "the main menu is opened with a new holder");
    }

    @Test
    void closesTheMenuFromTheCloseTile() {
        menuService.openMainMenu(player);
        AdminMenuHolder mainHolder = windows.lastOpenedMenu(player).holder();

        clickInLastOpenedMenu(MAIN_CLOSE_SLOT);

        assertTrue(windows.isMenuClosed(player));
        assertSame(mainHolder, windows.lastOpenedMenu(player).holder(), "no other menu was opened");
        assertTrue(messages.isEmpty(), messages.toString());
    }

    @Test
    void closesTheMenuAndRefusesTheClickOfAPlayerWithoutThePermission() {
        menuService.openMainMenu(player);
        AdminMenuHolder mainHolder = windows.lastOpenedMenu(player).holder();
        adminPermissionGranted = false;

        clickInLastOpenedMenu(MAIN_TEAMS_SLOT);

        assertTrue(windows.isMenuClosed(player));
        assertSame(mainHolder, windows.lastOpenedMenu(player).holder(), "the teams menu was not opened");
        assertEquals(List.of("You are not allowed to use the tournament menu"), messages);
    }

    private void clickInLastOpenedMenu(int slot) {
        menuService.handleClick(player, windows.lastOpenedMenu(player).holder(), slot);
    }

    private Player createPlayer() {
        UUID uniqueId = UUID.randomUUID();
        return proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> uniqueId;
            case "getName" -> "admin";
            case "hasPermission" -> AdminPermission.ADMIN_PERMISSION_NAME.equals(arguments[0])
                    && adminPermissionGranted;
            case "sendMessage" -> {
                messages.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                yield null;
            }
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }

    private static <T> T proxy(Class<T> type, FakeCall call) {
        InvocationHandler handler = (proxyInstance, method, arguments) -> switch (method.getName()) {
            case "equals" -> proxyInstance == arguments[0];
            case "hashCode" -> System.identityHashCode(proxyInstance);
            case "toString" -> "fake " + type.getSimpleName();
            default -> call.invoke(method, arguments);
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    @FunctionalInterface
    private interface FakeCall {

        Object invoke(Method method, Object[] arguments);
    }
}
