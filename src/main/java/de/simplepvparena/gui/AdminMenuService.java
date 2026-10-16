package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaDebugCellLimit;
import de.simplepvparena.arena.ArenaHeightMarkerService;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.arena.ArenaSpawnType;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.config.ConfigurationReloadReport;
import de.simplepvparena.diagnostics.DryRunReport;
import de.simplepvparena.diagnostics.TournamentDryRun;
import de.simplepvparena.gate.StartGateItemService;
import de.simplepvparena.input.BlockItemRequest;
import de.simplepvparena.input.BlockItemRequestResult;
import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.EventTeamImportService;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentOperationResult;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class AdminMenuService {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MAIN_BORDER_SELECTOR_SLOT = 13;
    private static final int MAIN_FINAL_SLOT = 14;
    private static final int MAIN_STATUS_SLOT = 15;
    private static final int MAIN_BRACKET_SLOT = 16;
    private static final int MAIN_DRY_RUN_SLOT = 18;
    private static final int MAIN_RELOAD_SLOT = 20;
    private static final int MAIN_CLOSE_SLOT = 22;
    private static final int MAIN_KIT_SLOT = 24;

    private static final int ARENA_CALCULATE_SLOT = 10;
    private static final int ARENA_DIAGNOSE_SLOT = 11;
    private static final int ARENA_FIRST_TEAM_SPAWN_SLOT = 12;
    private static final int ARENA_SECOND_TEAM_SPAWN_SLOT = 13;
    private static final int ARENA_TRIBUNE_SPAWN_SLOT = 14;
    private static final int ARENA_CLEAN_SLOT = 15;
    private static final int ARENA_RESET_SLOT = 16;
    private static final int ARENA_HEIGHT_MARKER_SLOT = 19;
    private static final int ARENA_START_GATE_SLOT = 20;
    private static final Material START_GATE_TILE_MATERIAL = Material.IRON_BARS;

    private static final int CONFIRM_YES_SLOT = 11;
    private static final int CONFIRM_NO_SLOT = 15;

    private final AdminMenuNavigator navigator;
    private final TournamentManager tournamentManager;
    private final TournamentDryRun tournamentDryRun;
    private final TeamRegistry teamRegistry;
    private final MatchService matchService;
    private final ArenaSetupService arenaSetupService;
    private final ArenaHeightMarkerService heightMarkerService;
    private final StartGateItemService gateItemService;
    private final BorderSelectorModeRegistry borderSelectorModes;
    private final KitAdminMenu kitAdminMenu;
    private final ChatInputService chatInputService;
    private final Supplier<ConfigurationReloadReport> configurationReloader;
    private final TeamActionsAdminMenu teamActionsAdminMenu;
    private final TournamentMenuStatus tournamentMenuStatus;
    private final MatchAdminMenu matchAdminMenu;
    private final TeamsAdminMenu teamsAdminMenu;

    public AdminMenuService(Plugin plugin,
                            TournamentManager tournamentManager,
                            TournamentDryRun tournamentDryRun,
                            TeamRegistry teamRegistry,
                            TeamMemberLookup teamMemberLookup,
                            EventTeamImportService eventTeamImportService,
                            MatchService matchService,
                            ArenaSetupService arenaSetupService,
                            ArenaHeightMarkerService heightMarkerService,
                            StartGateItemService gateItemService,
                            BorderSelectorModeRegistry borderSelectorModes,
                            KitSaveService kitSaveService,
                            ChatInputService chatInputService,
                            Runnable teamConfigurationSaver,
                            Supplier<ConfigurationReloadReport> configurationReloader) {
        this(plugin, tournamentManager, tournamentDryRun, teamRegistry, teamMemberLookup, eventTeamImportService,
                matchService, arenaSetupService, heightMarkerService, gateItemService, borderSelectorModes,
                kitSaveService, chatInputService, teamConfigurationSaver, configurationReloader,
                new InventoryAdminMenuWindows(plugin));
    }

    AdminMenuService(Plugin plugin,
                     TournamentManager tournamentManager,
                     TournamentDryRun tournamentDryRun,
                     TeamRegistry teamRegistry,
                     TeamMemberLookup teamMemberLookup,
                     EventTeamImportService eventTeamImportService,
                     MatchService matchService,
                     ArenaSetupService arenaSetupService,
                     ArenaHeightMarkerService heightMarkerService,
                     StartGateItemService gateItemService,
                     BorderSelectorModeRegistry borderSelectorModes,
                     KitSaveService kitSaveService,
                     ChatInputService chatInputService,
                     Runnable teamConfigurationSaver,
                     Supplier<ConfigurationReloadReport> configurationReloader,
                     AdminMenuWindows windows) {
        Objects.requireNonNull(plugin, "plugin");
        this.navigator = new AdminMenuNavigator(windows);
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.tournamentDryRun = Objects.requireNonNull(tournamentDryRun, "tournamentDryRun");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        Objects.requireNonNull(teamMemberLookup, "teamMemberLookup");
        Objects.requireNonNull(eventTeamImportService, "eventTeamImportService");
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.arenaSetupService = Objects.requireNonNull(arenaSetupService, "arenaSetupService");
        this.heightMarkerService = Objects.requireNonNull(heightMarkerService, "heightMarkerService");
        this.gateItemService = Objects.requireNonNull(gateItemService, "gateItemService");
        this.borderSelectorModes = Objects.requireNonNull(borderSelectorModes, "borderSelectorModes");
        this.kitAdminMenu = new KitAdminMenu(navigator, kitSaveService, matchService::hasActiveMatch);
        this.chatInputService = Objects.requireNonNull(chatInputService, "chatInputService");
        Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
        this.configurationReloader = Objects.requireNonNull(configurationReloader, "configurationReloader");
        this.teamActionsAdminMenu = new TeamActionsAdminMenu(navigator, teamRegistry, teamConfigurationSaver);
        ForceResultAdminMenu forceResultAdminMenu = new ForceResultAdminMenu(navigator, tournamentManager,
                matchService);
        this.tournamentMenuStatus = new TournamentMenuStatus(tournamentManager, matchService);
        this.matchAdminMenu = new MatchAdminMenu(navigator, tournamentManager, matchService, tournamentMenuStatus,
                forceResultAdminMenu);
        this.teamsAdminMenu = new TeamsAdminMenu(navigator, teamRegistry, teamMemberLookup, eventTeamImportService,
                matchService::hasActiveMatch, chatInputService, teamConfigurationSaver);
        navigator.register(AdminMenuType.MAIN, new LegacyMenu(AdminMenuLayout.SMALL_MENU_SIZE));
        navigator.register(AdminMenuType.ARENA, new LegacyMenu(AdminMenuLayout.SMALL_MENU_SIZE));
        navigator.register(AdminMenuType.KIT, kitAdminMenu);
        navigator.register(AdminMenuType.TEAMS, teamsAdminMenu);
        navigator.register(AdminMenuType.TEAM_ACTIONS, teamActionsAdminMenu);
        navigator.register(AdminMenuType.MATCH, matchAdminMenu);
        navigator.register(AdminMenuType.FORCE_RESULT, forceResultAdminMenu);
        navigator.register(AdminMenuType.CONFIRMATION, new LegacyMenu(AdminMenuLayout.SMALL_MENU_SIZE));
        navigator.requireEveryMenuRegistered();
    }

    public void openMainMenu(Player player) {
        openMenu(player, AdminMenuHolder.main());
    }

    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        if (!AdminMenuReplies.isStillAdmin(player)) {
            closeMenu(player);
            return;
        }
        navigator.menuOf(holder.getMenuType()).handleClick(player, holder, slot);
    }

    private void handleLegacyClick(Player player, AdminMenuHolder holder, int slot) {
        switch (holder.getMenuType()) {
            case MAIN -> handleMainClick(player, slot);
            case ARENA -> handleArenaClick(player, holder, slot);
            case CONFIRMATION -> handleConfirmationClick(player, holder, slot);
        }
    }

    private void handleMainClick(Player player, int slot) {
        switch (slot) {
            case MAIN_ARENA_SLOT -> openMenu(player, AdminMenuHolder.arena());
            case MAIN_TEAMS_SLOT -> openMenu(player, AdminMenuHolder.teams(0));
            case MAIN_MATCH_SLOT -> openMenu(player, AdminMenuHolder.match());
            case MAIN_BORDER_SELECTOR_SLOT -> toggleBorderSelectorMode(player);
            case MAIN_FINAL_SLOT -> runOperation(player, tournamentManager::startFinalMatch);
            case MAIN_STATUS_SLOT -> TournamentMessages.sendInfo(player, tournamentManager.describeStatus());
            case MAIN_BRACKET_SLOT -> showBracket(player);
            case MAIN_DRY_RUN_SLOT -> runDryRun(player);
            case MAIN_RELOAD_SLOT -> reloadConfiguration(player);
            case MAIN_KIT_SLOT -> openMenu(player, AdminMenuHolder.kit());
            case MAIN_CLOSE_SLOT -> closeMenu(player);
            default -> { }
        }
    }

    private void showBracket(Player player) {
        closeMenu(player);
        TournamentMessages.sendBlock(player, TournamentMessages.info("Bracket"),
                tournamentManager.describeBracket());
    }

    private void reloadConfiguration(Player player) {
        closeMenu(player);
        boolean matchWasRunning = matchService.hasActiveMatch();
        ConfigurationReloadReport report = configurationReloader.get();
        TournamentMessages.sendInfoReport(player, report.describe());
        if (matchWasRunning) {
            TournamentMessages.sendWarning(player, describeRunningMatchDelay());
        }
    }

    private void runDryRun(Player player) {
        closeMenu(player);
        DryRunReport report = tournamentDryRun.run();
        if (report.isSuccessful()) {
            TournamentMessages.sendSuccessReport(player, report.describe());
            return;
        }
        TournamentMessages.sendErrorReport(player, report.describe());
    }

    private MenuTile buildDryRunItem() {
        Optional<String> rejection = tournamentDryRun.findRejection();
        if (rejection.isPresent()) {
            return MenuItems.create(MenuItems.UNAVAILABLE_MATERIAL, "Dry run of the pipeline",
                    AdminMenuReplies.toLoreLines(rejection.get()));
        }
        return MenuItems.create(Material.OBSERVER, "Dry run of the pipeline", buildDryRunLore());
    }

    private List<String> buildDryRunLore() {
        List<String> lore = new ArrayList<>();
        lore.add("Walks the whole pipeline once, without an event");
        lore.add("Registers two test teams, builds their bracket,");
        lore.add("decides a simulated match and removes them again");
        lore.add("Checks the arena and its spawns, the kit, the");
        lore.add("countdown of the start gate and whether the");
        lore.add("cleaning after a match would start");
        lore.add("Nothing is started, taught, written or cleaned:");
        lore.add("no block, no file and no fighter is touched");
        lore.add("A real test player stays necessary for the kit");
        lore.add("and the teleport really arriving");
        lore.add("Refused while a tournament or a match runs");
        lore.add("The menu closes so you can read the answer");
        return lore;
    }

    private static String describeRunningMatchDelay() {
        return "A match is running, the new settings arrive with the next round"
                + TournamentMessages.LINE_BREAK
                + "The running round keeps its timer and its fighters keep the kit they were given";
    }

    private void fillMainMenu(Player player, AdminMenuCanvas canvas) {
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info("Tournament", buildMainInfoLore()));
        canvas.set(MAIN_ARENA_SLOT, MenuItems.create(Material.GRASS_BLOCK, "Arena",
                "Fight area, spawns, cleaning and reset",
                "Arena " + (arenaSetupService.isArenaReady() ? "ready" : "incomplete")));
        canvas.set(MAIN_TEAMS_SLOT, MenuItems.create(Material.PLAYER_HEAD, "Teams",
                "Registered teams: " + teamRegistry.getTeamCount(),
                "Shows every team, adds one and removes one"));
        canvas.set(MAIN_MATCH_SLOT, MenuItems.create(Material.DIAMOND_SWORD, "Match and tournament",
                "Start, pause, cancel and force a result",
                "Running match: " + tournamentMenuStatus.describeActiveMatch()));
        canvas.set(MAIN_BORDER_SELECTOR_SLOT, MenuItems.create(Material.BRUSH,
                "Mark border blocks by clicking them", buildBorderSelectorLore(player)));
        canvas.set(MAIN_FINAL_SLOT, tournamentMenuStatus.buildFinalItem());
        canvas.set(MAIN_STATUS_SLOT, MenuItems.create(Material.PAPER, "Tournament status",
                "Writes the state, the rounds and the running match into the chat"));
        canvas.set(MAIN_BRACKET_SLOT, MenuItems.create(Material.WRITTEN_BOOK, "Bracket",
                "Writes every round and every match into the chat",
                "A bracket is a tree, the chat shows it better than a grid",
                "The menu closes so you can read it"));
        canvas.set(MAIN_DRY_RUN_SLOT, buildDryRunItem());
        canvas.set(MAIN_RELOAD_SLOT, MenuItems.create(Material.COMPARATOR, "Reload config.yml",
                buildReloadLore()));
        canvas.set(MAIN_KIT_SLOT, MenuItems.create(Material.DIAMOND_CHESTPLATE, "Kit",
                "The items every fighter is given when a round starts",
                "Saves your own inventory as that kit"));
        canvas.set(MAIN_CLOSE_SLOT, MenuItems.create(Material.BARRIER, "Close", "Closes the menu"));
    }

    private List<String> buildReloadLore() {
        List<String> lore = new ArrayList<>();
        lore.add("Reads config.yml again and applies it live");
        lore.add("Kit, round timeout, cleaning batch sizes,");
        lore.add("cell limit, safety zone and border particles");
        lore.add("The arena, the teams and a running match stay untouched");
        if (matchService.hasActiveMatch()) {
            lore.add("A match is running: the new round timeout and");
            lore.add("the new kit arrive with the next round");
        }
        lore.add("The menu closes so you can read the answer");
        return lore;
    }

    private List<String> buildMainInfoLore() {
        List<String> lore = new ArrayList<>();
        lore.add(tournamentManager.describeStatus());
        lore.add("Registered teams: " + teamRegistry.getTeamCount());
        lore.add("Arena: " + (arenaSetupService.isArenaReady() ? "ready" : "incomplete"));
        lore.add("Running match: " + tournamentMenuStatus.describeActiveMatch());
        return lore;
    }

    private void toggleBorderSelectorMode(Player player) {
        closeMenu(player);
        if (borderSelectorModes.toggle(player.getUniqueId())) {
            TournamentMessages.sendSuccess(player, "Border selector mode is on"
                    + TournamentMessages.LINE_BREAK
                    + "Right click any block in the world to mark it as a border block, "
                    + "right click a marked one to take its mark away again"
                    + TournamentMessages.LINE_BREAK
                    + "No border item is needed: stairs, fences and everything else that already "
                    + "stands there can become part of the wall"
                    + TournamentMessages.LINE_BREAK
                    + "Every other right click is blocked while the mode is on, chests and doors included"
                    + TournamentMessages.LINE_BREAK
                    + "Open the tournament menu and click the tile again to switch it off");
            return;
        }
        TournamentMessages.sendSuccess(player, "Border selector mode is off"
                + TournamentMessages.LINE_BREAK
                + "Right clicking works the way it always did again, every mark you set stays");
    }

    private List<String> buildBorderSelectorLore(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add("Turns every right click of yours into a border mark");
        lore.add("A click marks a block, a click on a marked block");
        lore.add("takes the mark away again");
        lore.add("No border item is needed: stairs, fences and every");
        lore.add("other block that already stands there can be marked");
        lore.add("Chests, doors and everything else stay shut while it is on");
        lore.add("The menu closes so you can click the blocks");
        lore.add(borderSelectorModes.isActive(player.getUniqueId())
                ? "The mode is on, this tile switches it off"
                : "The mode is off, this tile switches it on");
        return lore;
    }

    private void handleArenaClick(Player player, AdminMenuHolder holder, int slot) {
        switch (slot) {
            case ARENA_CALCULATE_SLOT -> {
                closeMenu(player);
                arenaSetupService.calculateShape(player);
            }
            case ARENA_DIAGNOSE_SLOT -> askForDebugCellLimitLine(player);
            case ARENA_HEIGHT_MARKER_SLOT -> handOutHeightMarker(player, holder);
            case ARENA_START_GATE_SLOT -> askForStartGateItemLine(player);
            case ARENA_FIRST_TEAM_SPAWN_SLOT -> setSpawn(player, holder, ArenaSpawnType.FIRST_TEAM);
            case ARENA_SECOND_TEAM_SPAWN_SLOT -> setSpawn(player, holder, ArenaSpawnType.SECOND_TEAM);
            case ARENA_TRIBUNE_SPAWN_SLOT -> setSpawn(player, holder, ArenaSpawnType.TRIBUNE);
            case ARENA_CLEAN_SLOT -> askForCleaningConfirmation(player);
            case ARENA_RESET_SLOT -> openMenu(player,
                    AdminMenuHolder.confirmation(ConfirmationRequest.RESET_ARENA, null, 0));
            case AdminMenuLayout.BACK_SLOT -> openMenu(player, AdminMenuHolder.main());
            default -> { }
        }
    }

    private void setSpawn(Player player, AdminMenuHolder holder, ArenaSpawnType spawnType) {
        arenaSetupService.setSpawn(player, spawnType);
        refresh(player, holder);
    }

    private void handOutHeightMarker(Player player, AdminMenuHolder holder) {
        Map<Integer, ItemStack> itemsThatDidNotFit = player.getInventory()
                .addItem(heightMarkerService.createMarkerItem());
        if (!itemsThatDidNotFit.isEmpty()) {
            TournamentMessages.sendWarning(player,
                    "Your inventory is full, the height marker could not be handed out");
            return;
        }
        TournamentMessages.sendSuccess(player, "You received the height marker of the fight area"
                + TournamentMessages.LINE_BREAK + "Place a block with it at the lowest point of the area, "
                + "the item switches to the highest one by itself"
                + TournamentMessages.LINE_BREAK + "The placed block disappears again, only its height is kept");
        refresh(player, holder);
    }

    private void askForStartGateItemLine(Player player) {
        closeMenu(player);
        chatInputService.await(player, "the start gate blocks", List.of(
                        "Type: " + BlockItemRequest.USAGE + " in chat",
                        "Example: barrier 32",
                        "The amount has to be between " + BlockItemRequest.MINIMUM_AMOUNT
                                + " and " + BlockItemRequest.MAXIMUM_AMOUNT),
                line -> handleStartGateItemLine(player, line));
    }

    private void handleStartGateItemLine(Player player, String line) {
        if (!AdminMenuReplies.isStillAdmin(player)) {
            return;
        }
        BlockItemRequestResult requestResult = BlockItemRequest.parseLine(line,
                AdminMenuService::isPlaceableBlock);
        if (!requestResult.isAccepted()) {
            TournamentMessages.sendError(player, requestResult.getRejectionMessage().orElseThrow());
            openMenu(player, AdminMenuHolder.arena());
            return;
        }
        BlockItemRequest request = requestResult.getRequest().orElseThrow();
        Map<Integer, ItemStack> itemsThatDidNotFit = player.getInventory()
                .addItem(gateItemService.createGateItem(request));
        if (!itemsThatDidNotFit.isEmpty()) {
            TournamentMessages.sendWarning(player,
                    "Your inventory is full, not every gate block could be handed out");
        }
        TournamentMessages.sendSuccess(player, "You received " + request.amount() + " "
                + request.material().name() + " start gate blocks"
                + TournamentMessages.LINE_BREAK
                + "Build the box around a team spawn out of whatever your map is made of and close it "
                + "with these blocks"
                + TournamentMessages.LINE_BREAK
                + "Every one of them you place is part of the gate right away, breaking one takes it "
                + "out again"
                + TournamentMessages.LINE_BREAK
                + "Every round start puts the whole gate back and takes it apart again from the top");
    }

    private void askForCleaningConfirmation(Player player) {
        Optional<String> rejection = arenaSetupService.findCleaningRejection();
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        openMenu(player, AdminMenuHolder.confirmation(ConfirmationRequest.CLEAN_ARENA, null, 0));
    }

    private void fillArenaMenu(AdminMenuCanvas canvas) {
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info("Arena", arenaSetupService.describeArena()));
        canvas.set(ARENA_CALCULATE_SLOT, MenuItems.create(Material.SPYGLASS, "Calculate the fight area",
                "Runs the calculation for the position you stand on",
                "Stand inside the closed border wall before you click",
                "The menu closes so you can see the recognized wall"));
        canvas.set(ARENA_DIAGNOSE_SLOT, MenuItems.create(Material.REDSTONE_TORCH,
                "Diagnose with a higher cell limit",
                "One calculation with a limit you type, up to "
                        + ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT + " cells",
                "Shows how far a fill through a gap really gets",
                "config.yml stays untouched"));
        canvas.set(ARENA_HEIGHT_MARKER_SLOT, MenuItems.create(ArenaHeightMarkerService.MARKER_MATERIAL,
                "Mark the height of the fight area", buildHeightMarkerLore()));
        canvas.set(ARENA_START_GATE_SLOT, MenuItems.create(START_GATE_TILE_MATERIAL,
                "Give start gate blocks", buildStartGateLore()));
        canvas.set(ARENA_FIRST_TEAM_SPAWN_SLOT, buildSpawnItem(Material.BLUE_BED, ArenaSpawnType.FIRST_TEAM));
        canvas.set(ARENA_SECOND_TEAM_SPAWN_SLOT, buildSpawnItem(Material.RED_BED, ArenaSpawnType.SECOND_TEAM));
        canvas.set(ARENA_TRIBUNE_SPAWN_SLOT, buildSpawnItem(Material.YELLOW_BED, ArenaSpawnType.TRIBUNE));
        canvas.set(ARENA_CLEAN_SLOT, MenuItems.create(Material.TNT, "Clean the arena",
                buildCleaningLore("Asks for a confirmation first")));
        canvas.set(ARENA_RESET_SLOT, MenuItems.create(MenuItems.DESTRUCTIVE_MATERIAL, "Reset the arena",
                "Drops the fight area and all three spawns",
                "The blocks and the border blocks stay untouched",
                "Asks for a confirmation first"));
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back("the main menu"));
    }

    private List<String> buildHeightMarkerLore() {
        List<String> lore = new ArrayList<>();
        lore.add("Hands you the item that marks the lowest and the");
        lore.add("highest point of the fight area");
        lore.add("Place a block with it, only its height is kept");
        lore.add("and the block disappears again");
        lore.add("The next calculation takes them over as the floor");
        lore.add("and the ceiling of the fight area, also below the");
        lore.add("border wall, and uses both marks up");
        lore.addAll(heightMarkerService.describeMarkers());
        return lore;
    }

    private List<String> buildStartGateLore() {
        List<String> lore = new ArrayList<>();
        lore.add("Asks for a material and an amount in the chat and");
        lore.add("hands you that many start gate blocks");
        lore.add("Build a box around each team spawn and close it");
        lore.add("with them: every placed one is part of the gate,");
        lore.add("breaking one takes it out again");
        lore.add("Every round start puts the whole gate back and takes");
        lore.add("it apart again from the top, with a countdown");
        lore.add("A gate nobody placed is skipped, the round still starts");
        lore.addAll(gateItemService.describeGate());
        return lore;
    }

    private List<String> buildCleaningLore(String... closingLines) {
        List<String> lore = new ArrayList<>(arenaSetupService.describeCleaningEffect());
        lore.add(arenaSetupService.countCleaningBlocks() + " blocks are affected");
        lore.addAll(List.of(closingLines));
        return lore;
    }

    private MenuTile buildSpawnItem(Material material, ArenaSpawnType spawnType) {
        return MenuItems.create(material, "Set the " + spawnType.getDisplayName(),
                "Sets it to the position you stand on",
                "Stand " + spawnType.getPlacementHint(),
                "A position on the wrong side of the fight area",
                "is refused once the area is calculated");
    }

    private void handleConfirmationClick(Player player, AdminMenuHolder holder, int slot) {
        if (slot != CONFIRM_YES_SLOT && slot != CONFIRM_NO_SLOT) {
            return;
        }
        if (slot == CONFIRM_YES_SLOT) {
            carryOutConfirmedAction(player, holder);
        }
        openReturnMenu(player, holder);
    }

    private void carryOutConfirmedAction(Player player, AdminMenuHolder holder) {
        switch (holder.getConfirmationRequest().orElseThrow()) {
            case CLEAN_ARENA -> arenaSetupService.startCleaning(player);
            case RESET_ARENA -> arenaSetupService.clearConfiguration(player);
            case SAVE_KIT -> kitAdminMenu.saveKit(player);
            case REMOVE_TEAM -> teamActionsAdminMenu.removeTeam(player, holder.getSubject().orElseThrow());
            case IMPORT_TEAMS -> teamsAdminMenu.importTeams(player);
            case CANCEL_TOURNAMENT -> matchAdminMenu.cancelTournament(player);
        }
    }

    private void openReturnMenu(Player player, AdminMenuHolder holder) {
        switch (holder.getConfirmationRequest().orElseThrow().getReturnMenuType()) {
            case ARENA -> openMenu(player, AdminMenuHolder.arena());
            case KIT -> openMenu(player, AdminMenuHolder.kit());
            case TEAMS -> openMenu(player, AdminMenuHolder.teams(holder.getPageIndex()));
            default -> openMenu(player, AdminMenuHolder.match());
        }
    }

    private void fillConfirmationMenu(AdminMenuCanvas canvas, AdminMenuHolder holder) {
        ConfirmationRequest request = holder.getConfirmationRequest().orElseThrow();
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info(request.getTitle(),
                buildConfirmationLore(request, holder)));
        canvas.set(CONFIRM_YES_SLOT, MenuItems.create(MenuItems.CONFIRM_MATERIAL, "Yes",
                "Carries the action out"));
        canvas.set(CONFIRM_NO_SLOT, MenuItems.create(MenuItems.REFUSE_MATERIAL, "No",
                "Changes nothing and returns"));
    }

    private List<String> buildConfirmationLore(ConfirmationRequest request, AdminMenuHolder holder) {
        return switch (request) {
            case CLEAN_ARENA -> buildCleaningLore("The change cannot be undone",
                    "The border wall around it stays untouched");
            case SAVE_KIT -> kitAdminMenu.describeSaveConfirmation();
            case RESET_ARENA -> List.of(
                    "The fight area and all three spawns are dropped",
                    "The blocks of the arena and the border blocks stay untouched",
                    "No tournament can be started until it is calculated again");
            case REMOVE_TEAM -> List.of(
                    "Team " + holder.getSubject().orElseThrow() + " is removed from the registry",
                    "A running match of that team is not stopped by it");
            case IMPORT_TEAMS -> teamsAdminMenu.describeImportConfirmation();
            case CANCEL_TOURNAMENT -> matchAdminMenu.describeCancelConfirmation();
        };
    }

    private static boolean isPlaceableBlock(Material material) {
        return material.isBlock() && material.isItem() && !material.isAir();
    }

    private void askForDebugCellLimitLine(Player player) {
        closeMenu(player);
        chatInputService.await(player, "the diagnosis cell limit", List.of(
                        "Type: " + ArenaDebugCellLimit.USAGE + " in chat",
                        "A number between " + ArenaDebugCellLimit.MINIMUM_CELL_COUNT + " and "
                                + ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT,
                        "Stand inside the border wall, the calculation runs from where you stand",
                        "config.yml is not changed by it"),
                line -> handleDebugCellLimitLine(player, line));
    }

    private void handleDebugCellLimitLine(Player player, String line) {
        if (!AdminMenuReplies.isStillAdmin(player)) {
            return;
        }
        ArenaDebugCellLimit debugCellLimit = ArenaDebugCellLimit.parseLine(line);
        if (!debugCellLimit.isAccepted()) {
            TournamentMessages.sendError(player, debugCellLimit.getRejectionMessage().orElseThrow());
            openMenu(player, AdminMenuHolder.arena());
            return;
        }
        arenaSetupService.calculateShape(player, debugCellLimit.getCellCount());
    }

    private void runOperation(Player player, Supplier<TournamentOperationResult> operation) {
        AdminMenuReplies.reportOperation(player, operation.get());
    }

    private void openMenu(Player player, AdminMenuHolder holder) {
        navigator.open(player, holder);
    }

    private void closeMenu(Player player) {
        navigator.close(player);
    }

    private void refresh(Player player, AdminMenuHolder holder) {
        navigator.refresh(player, holder);
    }

    private void fillMenu(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        switch (holder.getMenuType()) {
            case MAIN -> fillMainMenu(player, canvas);
            case ARENA -> fillArenaMenu(canvas);
            case CONFIRMATION -> fillConfirmationMenu(canvas, holder);
        }
    }

    private String titleOf(AdminMenuHolder holder) {
        return switch (holder.getMenuType()) {
            case MAIN -> "Tournament administration";
            case ARENA -> "Arena";
            case CONFIRMATION -> holder.getConfirmationRequest().orElseThrow().getTitle();
            default -> throw new IllegalStateException("the " + holder.getMenuType() + " menu has its own class");
        };
    }

    private final class LegacyMenu implements AdminMenu {

        private final int size;

        private LegacyMenu(int size) {
            this.size = size;
        }

        @Override
        public String title(AdminMenuHolder holder) {
            return titleOf(holder);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
            fillMenu(player, holder, canvas);
        }

        @Override
        public void handleClick(Player player, AdminMenuHolder holder, int slot) {
            handleLegacyClick(player, holder, slot);
        }
    }
}
