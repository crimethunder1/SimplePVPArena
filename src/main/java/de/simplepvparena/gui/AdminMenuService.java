package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaDebugCellLimit;
import de.simplepvparena.arena.ArenaHeightMarkerService;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.arena.ArenaSpawnType;
import de.simplepvparena.border.BorderItemRequest;
import de.simplepvparena.border.BorderItemRequestResult;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.config.ConfigurationReloadReport;
import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamCreationRequest;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentOperationResult;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

public final class AdminMenuService {

    private static final int SMALL_MENU_SIZE = 27;
    private static final int TEAM_MENU_SIZE = 54;

    private static final int INFO_SLOT = 4;

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MAIN_BORDER_SLOT = 13;
    private static final int MAIN_FINAL_SLOT = 14;
    private static final int MAIN_STATUS_SLOT = 15;
    private static final int MAIN_BRACKET_SLOT = 16;
    private static final int MAIN_RELOAD_SLOT = 20;
    private static final int MAIN_CLOSE_SLOT = 22;
    private static final int MAIN_KIT_SLOT = 24;

    private static final int KIT_SAVE_SLOT = 13;

    private static final int ARENA_CALCULATE_SLOT = 10;
    private static final int ARENA_DIAGNOSE_SLOT = 11;
    private static final int ARENA_FIRST_TEAM_SPAWN_SLOT = 12;
    private static final int ARENA_SECOND_TEAM_SPAWN_SLOT = 13;
    private static final int ARENA_TRIBUNE_SPAWN_SLOT = 14;
    private static final int ARENA_CLEAN_SLOT = 15;
    private static final int ARENA_RESET_SLOT = 16;
    private static final int ARENA_HEIGHT_MARKER_SLOT = 19;

    private static final int TEAMS_PREVIOUS_PAGE_SLOT = 45;
    private static final int TEAMS_INFO_SLOT = 47;
    private static final int TEAMS_BACK_SLOT = 49;
    private static final int TEAMS_ADD_SLOT = 51;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;

    private static final int TEAM_REMOVE_SLOT = 13;

    private static final int MATCH_START_SLOT = 10;
    private static final int MATCH_FORCE_START_SLOT = 11;
    private static final int MATCH_PAUSE_SLOT = 12;
    private static final int MATCH_CANCEL_SLOT = 13;
    private static final int MATCH_FINAL_SLOT = 15;
    private static final int MATCH_FORCE_RESULT_SLOT = 16;

    private static final int FORCE_RESULT_FIRST_TEAM_SLOT = 11;
    private static final int FORCE_RESULT_SECOND_TEAM_SLOT = 15;

    private static final int CONFIRM_YES_SLOT = 11;
    private static final int CONFIRM_NO_SLOT = 15;

    private static final int BACK_SLOT = 22;

    private final Plugin plugin;
    private final TournamentManager tournamentManager;
    private final TeamRegistry teamRegistry;
    private final TeamMemberLookup teamMemberLookup;
    private final MatchService matchService;
    private final ArenaSetupService arenaSetupService;
    private final ArenaHeightMarkerService heightMarkerService;
    private final SpectatorBorderService spectatorBorderService;
    private final KitSaveService kitSaveService;
    private final ChatInputService chatInputService;
    private final Runnable teamConfigurationSaver;
    private final Supplier<ConfigurationReloadReport> configurationReloader;

    public AdminMenuService(Plugin plugin,
                            TournamentManager tournamentManager,
                            TeamRegistry teamRegistry,
                            TeamMemberLookup teamMemberLookup,
                            MatchService matchService,
                            ArenaSetupService arenaSetupService,
                            ArenaHeightMarkerService heightMarkerService,
                            SpectatorBorderService spectatorBorderService,
                            KitSaveService kitSaveService,
                            ChatInputService chatInputService,
                            Runnable teamConfigurationSaver,
                            Supplier<ConfigurationReloadReport> configurationReloader) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.teamMemberLookup = Objects.requireNonNull(teamMemberLookup, "teamMemberLookup");
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.arenaSetupService = Objects.requireNonNull(arenaSetupService, "arenaSetupService");
        this.heightMarkerService = Objects.requireNonNull(heightMarkerService, "heightMarkerService");
        this.spectatorBorderService = Objects.requireNonNull(spectatorBorderService, "spectatorBorderService");
        this.kitSaveService = Objects.requireNonNull(kitSaveService, "kitSaveService");
        this.chatInputService = Objects.requireNonNull(chatInputService, "chatInputService");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
        this.configurationReloader = Objects.requireNonNull(configurationReloader, "configurationReloader");
    }

    public void openMainMenu(Player player) {
        openMenu(player, AdminMenuHolder.main());
    }

    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        if (!isStillAdmin(player)) {
            closeMenu(player);
            return;
        }
        switch (holder.getMenuType()) {
            case MAIN -> handleMainClick(player, slot);
            case ARENA -> handleArenaClick(player, holder, slot);
            case KIT -> handleKitClick(player, slot);
            case TEAMS -> handleTeamsClick(player, holder, slot);
            case TEAM_ACTIONS -> handleTeamActionsClick(player, holder, slot);
            case MATCH -> handleMatchClick(player, holder, slot);
            case FORCE_RESULT -> handleForceResultClick(player, holder, slot);
            case CONFIRMATION -> handleConfirmationClick(player, holder, slot);
        }
    }

    private void handleMainClick(Player player, int slot) {
        switch (slot) {
            case MAIN_ARENA_SLOT -> openMenu(player, AdminMenuHolder.arena());
            case MAIN_TEAMS_SLOT -> openMenu(player, AdminMenuHolder.teams(0));
            case MAIN_MATCH_SLOT -> openMenu(player, AdminMenuHolder.match());
            case MAIN_BORDER_SLOT -> askForBorderItemLine(player);
            case MAIN_FINAL_SLOT -> runOperation(player, tournamentManager::startFinalMatch);
            case MAIN_STATUS_SLOT -> TournamentMessages.sendInfo(player, tournamentManager.describeStatus());
            case MAIN_BRACKET_SLOT -> showBracket(player);
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

    private static String describeRunningMatchDelay() {
        return "A match is running, the new settings arrive with the next round"
                + TournamentMessages.LINE_BREAK
                + "The running round keeps its timer and its fighters keep the kit they were given";
    }

    private void fillMainMenu(Inventory inventory) {
        inventory.setItem(INFO_SLOT, MenuItems.info("Tournament", buildMainInfoLore()));
        inventory.setItem(MAIN_ARENA_SLOT, MenuItems.create(Material.GRASS_BLOCK, "Arena",
                "Fight area, spawns, cleaning and reset",
                "Arena " + (arenaSetupService.isArenaReady() ? "ready" : "incomplete")));
        inventory.setItem(MAIN_TEAMS_SLOT, MenuItems.create(Material.PLAYER_HEAD, "Teams",
                "Registered teams: " + teamRegistry.getTeamCount(),
                "Shows every team, adds one and removes one"));
        inventory.setItem(MAIN_MATCH_SLOT, MenuItems.create(Material.DIAMOND_SWORD, "Match and tournament",
                "Start, pause, cancel and force a result",
                "Running match: " + describeActiveMatch()));
        inventory.setItem(MAIN_BORDER_SLOT, MenuItems.create(Material.GLASS, "Border blocks",
                "Hands you the blocks the arena wall is built of",
                "Every block placed with them stops everybody who is not fighting",
                "Asks for " + BorderItemRequest.USAGE + " in the chat"));
        inventory.setItem(MAIN_FINAL_SLOT, buildFinalItem());
        inventory.setItem(MAIN_STATUS_SLOT, MenuItems.create(Material.PAPER, "Tournament status",
                "Writes the state, the rounds and the running match into the chat"));
        inventory.setItem(MAIN_BRACKET_SLOT, MenuItems.create(Material.WRITTEN_BOOK, "Bracket",
                "Writes every round and every match into the chat",
                "A bracket is a tree, the chat shows it better than a grid",
                "The menu closes so you can read it"));
        inventory.setItem(MAIN_RELOAD_SLOT, MenuItems.create(Material.COMPARATOR, "Reload config.yml",
                buildReloadLore()));
        inventory.setItem(MAIN_KIT_SLOT, MenuItems.create(Material.DIAMOND_CHESTPLATE, "Kit",
                "The items every fighter is given when a round starts",
                "Saves your own inventory as that kit"));
        inventory.setItem(MAIN_CLOSE_SLOT, MenuItems.create(Material.BARRIER, "Close", "Closes the menu"));
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
        lore.add("Running match: " + describeActiveMatch());
        return lore;
    }

    private ItemStack buildFinalItem() {
        if (!currentOptions().finalStartOffered()) {
            return MenuItems.unavailable("Start the final",
                    "The final is not ready, its stage is " + tournamentManager.getFinalMatchStage());
        }
        return MenuItems.create(Material.NETHER_STAR, "Start the final",
                "Both finalists are known", "Starts the final on the arena");
    }

    private void handleArenaClick(Player player, AdminMenuHolder holder, int slot) {
        switch (slot) {
            case ARENA_CALCULATE_SLOT -> {
                closeMenu(player);
                arenaSetupService.calculateShape(player);
            }
            case ARENA_DIAGNOSE_SLOT -> askForDebugCellLimitLine(player);
            case ARENA_HEIGHT_MARKER_SLOT -> handOutHeightMarker(player, holder);
            case ARENA_FIRST_TEAM_SPAWN_SLOT -> setSpawn(player, holder, ArenaSpawnType.FIRST_TEAM);
            case ARENA_SECOND_TEAM_SPAWN_SLOT -> setSpawn(player, holder, ArenaSpawnType.SECOND_TEAM);
            case ARENA_TRIBUNE_SPAWN_SLOT -> setSpawn(player, holder, ArenaSpawnType.TRIBUNE);
            case ARENA_CLEAN_SLOT -> askForCleaningConfirmation(player);
            case ARENA_RESET_SLOT -> openMenu(player,
                    AdminMenuHolder.confirmation(ConfirmationRequest.RESET_ARENA, null, 0));
            case BACK_SLOT -> openMenu(player, AdminMenuHolder.main());
            default -> { }
        }
    }

    private void setSpawn(Player player, AdminMenuHolder holder, ArenaSpawnType spawnType) {
        arenaSetupService.setSpawn(player, spawnType);
        refresh(holder);
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
        refresh(holder);
    }

    private void askForCleaningConfirmation(Player player) {
        Optional<String> rejection = arenaSetupService.findCleaningRejection();
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        openMenu(player, AdminMenuHolder.confirmation(ConfirmationRequest.CLEAN_ARENA, null, 0));
    }

    private void fillArenaMenu(Inventory inventory) {
        inventory.setItem(INFO_SLOT, MenuItems.info("Arena", arenaSetupService.describeArena()));
        inventory.setItem(ARENA_CALCULATE_SLOT, MenuItems.create(Material.SPYGLASS, "Calculate the fight area",
                "Runs the calculation for the position you stand on",
                "Stand inside the closed border wall before you click",
                "The menu closes so you can see the recognized wall"));
        inventory.setItem(ARENA_DIAGNOSE_SLOT, MenuItems.create(Material.REDSTONE_TORCH,
                "Diagnose with a higher cell limit",
                "One calculation with a limit you type, up to "
                        + ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT + " cells",
                "Shows how far a fill through a gap really gets",
                "config.yml stays untouched"));
        inventory.setItem(ARENA_HEIGHT_MARKER_SLOT, MenuItems.create(ArenaHeightMarkerService.MARKER_MATERIAL,
                "Mark the height of the fight area", buildHeightMarkerLore()));
        inventory.setItem(ARENA_FIRST_TEAM_SPAWN_SLOT, buildSpawnItem(Material.BLUE_BED, ArenaSpawnType.FIRST_TEAM));
        inventory.setItem(ARENA_SECOND_TEAM_SPAWN_SLOT, buildSpawnItem(Material.RED_BED, ArenaSpawnType.SECOND_TEAM));
        inventory.setItem(ARENA_TRIBUNE_SPAWN_SLOT, buildSpawnItem(Material.YELLOW_BED, ArenaSpawnType.TRIBUNE));
        inventory.setItem(ARENA_CLEAN_SLOT, MenuItems.create(Material.TNT, "Clean the arena",
                buildCleaningLore("Asks for a confirmation first")));
        inventory.setItem(ARENA_RESET_SLOT, MenuItems.create(MenuItems.DESTRUCTIVE_MATERIAL, "Reset the arena",
                "Drops the fight area and all three spawns",
                "The blocks and the border blocks stay untouched",
                "Asks for a confirmation first"));
        inventory.setItem(BACK_SLOT, MenuItems.back("the main menu"));
    }

    private List<String> buildHeightMarkerLore() {
        List<String> lore = new ArrayList<>();
        lore.add("Hands you the item that marks the lowest and the");
        lore.add("highest point of the fight area");
        lore.add("Place a block with it, only its height is kept");
        lore.add("and the block disappears again");
        lore.add("The next calculation cleans exactly between them,");
        lore.add("also below the border wall, and uses both marks up");
        lore.addAll(heightMarkerService.describeMarkers());
        return lore;
    }

    private List<String> buildCleaningLore(String... closingLines) {
        List<String> lore = new ArrayList<>(arenaSetupService.describeCleaningEffect());
        lore.add(arenaSetupService.countCleaningBlocks() + " blocks are affected");
        lore.addAll(List.of(closingLines));
        return lore;
    }

    private ItemStack buildSpawnItem(Material material, ArenaSpawnType spawnType) {
        return MenuItems.create(material, "Set the " + spawnType.getDisplayName(),
                "Sets it to the position you stand on",
                "Stand " + spawnType.getPlacementHint(),
                "A position on the wrong side of the fight area",
                "is refused once the area is calculated");
    }

    private void handleKitClick(Player player, int slot) {
        switch (slot) {
            case KIT_SAVE_SLOT -> openMenu(player,
                    AdminMenuHolder.confirmation(ConfirmationRequest.SAVE_KIT, null, 0));
            case BACK_SLOT -> openMenu(player, AdminMenuHolder.main());
            default -> { }
        }
    }

    private void saveKit(Player player) {
        TournamentMessages.sendSuccessReport(player, kitSaveService.saveFromInventory(player).describe());
    }

    private void fillKitMenu(Inventory inventory) {
        inventory.setItem(INFO_SLOT, MenuItems.info("Kit", buildKitInfoLore()));
        inventory.setItem(KIT_SAVE_SLOT, MenuItems.create(Material.CHEST, "Save your inventory as the kit",
                buildKitSaveLore("Asks for a confirmation first")));
        inventory.setItem(BACK_SLOT, MenuItems.back("the main menu"));
    }

    private List<String> buildKitInfoLore() {
        List<String> lore = new ArrayList<>(kitSaveService.describeKit());
        lore.add("Every fighter is given it when a round starts");
        return lore;
    }

    private List<String> buildKitSaveLore(String... closingLines) {
        List<String> lore = new ArrayList<>();
        lore.add("Takes your helmet, chestplate, leggings, boots,");
        lore.add("your offhand and the 36 slots of your inventory");
        lore.add("Enchantments, names, lore and damage are kept exactly");
        lore.add("An empty slot stays empty in the kit");
        lore.add("The item on your cursor is not taken, it lies in no slot");
        lore.add("It replaces the kit section of config.yml");
        if (matchService.hasActiveMatch()) {
            lore.add("A match is running: its fighters keep their items,");
            lore.add("the new kit is handed out from the next round on");
        }
        lore.addAll(List.of(closingLines));
        return lore;
    }

    private void handleTeamsClick(Player player, AdminMenuHolder holder, int slot) {
        List<Team> teams = collectTeams();
        MenuPagination pagination = MenuPagination.of(teams.size(), holder.getPageIndex());
        switch (slot) {
            case TEAMS_PREVIOUS_PAGE_SLOT -> {
                if (pagination.hasPreviousPage()) {
                    openMenu(player, AdminMenuHolder.teams(pagination.getPreviousPageIndex()));
                }
            }
            case TEAMS_NEXT_PAGE_SLOT -> {
                if (pagination.hasNextPage()) {
                    openMenu(player, AdminMenuHolder.teams(pagination.getNextPageIndex()));
                }
            }
            case TEAMS_ADD_SLOT -> askForTeamLine(player, pagination.pageIndex());
            case TEAMS_INFO_SLOT -> { }
            case TEAMS_BACK_SLOT -> openMenu(player, AdminMenuHolder.main());
            default -> openTeamActions(player, teams, pagination, slot);
        }
    }

    private void openTeamActions(Player player, List<Team> teams, MenuPagination pagination, int slot) {
        OptionalInt teamIndex = pagination.findItemIndex(slot);
        if (teamIndex.isEmpty()) {
            return;
        }
        openMenu(player, AdminMenuHolder.teamActions(teams.get(teamIndex.getAsInt()).getTeamName(),
                pagination.pageIndex()));
    }

    private void fillTeamsMenu(Inventory inventory, int pageIndex) {
        List<Team> teams = collectTeams();
        MenuPagination pagination = MenuPagination.of(teams.size(), pageIndex);
        for (int slot = 0; slot < pagination.getItemCountOnPage(); slot++) {
            Team team = teams.get(pagination.getFirstItemIndex() + slot);
            inventory.setItem(slot, MenuItems.create(Material.PLAYER_HEAD, team.getTeamName(),
                    "Members: " + team.getMemberNamesJoined(), "Click for the options of this team"));
        }
        inventory.setItem(TEAMS_INFO_SLOT, MenuItems.info("Teams", List.of(
                "Registered teams: " + teams.size(),
                "Page " + pagination.getPageNumber() + " of " + pagination.getPageCount(),
                "A click on a team opens its options")));
        inventory.setItem(TEAMS_ADD_SLOT, MenuItems.create(MenuItems.CONFIRM_MATERIAL, "Add a team",
                "Asks for " + TeamCreationRequest.USAGE + " in the chat",
                "Two player names cannot be typed into an inventory",
                "The menu closes so you can type"));
        if (pagination.hasPreviousPage()) {
            inventory.setItem(TEAMS_PREVIOUS_PAGE_SLOT, MenuItems.create(MenuItems.PAGE_MATERIAL, "Previous page",
                    "Page " + pagination.getPageNumber() + " of " + pagination.getPageCount()));
        }
        if (pagination.hasNextPage()) {
            inventory.setItem(TEAMS_NEXT_PAGE_SLOT, MenuItems.create(MenuItems.PAGE_MATERIAL, "Next page",
                    "Page " + pagination.getPageNumber() + " of " + pagination.getPageCount()));
        }
        inventory.setItem(TEAMS_BACK_SLOT, MenuItems.back("the main menu"));
    }

    private void handleTeamActionsClick(Player player, AdminMenuHolder holder, int slot) {
        String teamName = holder.getSubject().orElseThrow();
        switch (slot) {
            case TEAM_REMOVE_SLOT -> openMenu(player, AdminMenuHolder.confirmation(ConfirmationRequest.REMOVE_TEAM,
                    teamName, holder.getPageIndex()));
            case BACK_SLOT -> openMenu(player, AdminMenuHolder.teams(holder.getPageIndex()));
            default -> { }
        }
    }

    private void fillTeamActionsMenu(Inventory inventory, String teamName) {
        Optional<Team> team = teamRegistry.findTeam(teamName);
        inventory.setItem(INFO_SLOT, MenuItems.info(teamName, team
                .map(registeredTeam -> List.of("Members: " + registeredTeam.getMemberNamesJoined()))
                .orElse(List.of("This team is not registered any more"))));
        inventory.setItem(TEAM_REMOVE_SLOT, MenuItems.create(MenuItems.REFUSE_MATERIAL, "Remove the team",
                "Removes " + teamName + " from the registry",
                "Asks for a confirmation first"));
        inventory.setItem(BACK_SLOT, MenuItems.back("the team list"));
    }

    private void handleMatchClick(Player player, AdminMenuHolder holder, int slot) {
        switch (slot) {
            case MATCH_START_SLOT -> runOperation(player, holder, () -> tournamentManager.startTournament(false));
            case MATCH_FORCE_START_SLOT -> runOperation(player, holder, () -> tournamentManager.startTournament(true));
            case MATCH_PAUSE_SLOT -> runOperation(player, holder, tournamentManager::togglePause);
            case MATCH_CANCEL_SLOT -> openMenu(player,
                    AdminMenuHolder.confirmation(ConfirmationRequest.CANCEL_TOURNAMENT, null, 0));
            case MATCH_FINAL_SLOT -> runOperation(player, holder, tournamentManager::startFinalMatch);
            case MATCH_FORCE_RESULT_SLOT -> openForceResultMenu(player);
            case BACK_SLOT -> openMenu(player, AdminMenuHolder.main());
            default -> { }
        }
    }

    private void openForceResultMenu(Player player) {
        Optional<Match> activeMatch = matchService.getActiveMatch();
        if (activeMatch.isEmpty()) {
            TournamentMessages.sendError(player, "No match is running");
            return;
        }
        openMenu(player, AdminMenuHolder.forceResult(activeMatch.get().getMatchId()));
    }

    private void fillMatchMenu(Inventory inventory) {
        TournamentMenuOptions options = currentOptions();
        inventory.setItem(INFO_SLOT, MenuItems.info("Tournament", List.of(
                tournamentManager.describeStatus(),
                "Running match: " + describeActiveMatch(),
                "Round timer: " + (matchService.isRoundTimerPaused() ? "paused" : "running"))));
        inventory.setItem(MATCH_START_SLOT, options.startOffered()
                ? MenuItems.create(MenuItems.CONFIRM_MATERIAL, "Start the tournament",
                        "Needs an even number of teams")
                : MenuItems.unavailable("Start the tournament", "A tournament is already in progress"));
        inventory.setItem(MATCH_FORCE_START_SLOT, options.forceStartOffered()
                ? MenuItems.create(Material.YELLOW_WOOL, "Force start",
                        "Starts the tournament with an uneven number of teams")
                : MenuItems.unavailable("Force start", "A tournament is already in progress"));
        inventory.setItem(MATCH_PAUSE_SLOT, buildPauseItem(options));
        inventory.setItem(MATCH_CANCEL_SLOT, options.cancelOffered()
                ? MenuItems.create(MenuItems.REFUSE_MATERIAL, "Cancel the tournament",
                        "Stops the running match and the bracket", "Asks for a confirmation first")
                : MenuItems.unavailable("Cancel the tournament", "No tournament is in progress"));
        inventory.setItem(MATCH_FINAL_SLOT, buildFinalItem());
        inventory.setItem(MATCH_FORCE_RESULT_SLOT, options.forceResultOffered()
                ? MenuItems.create(Material.ANVIL, "Force a result",
                        "Decides the running match for one of its two teams")
                : MenuItems.unavailable("Force a result", "No match is running"));
        inventory.setItem(BACK_SLOT, MenuItems.back("the main menu"));
    }

    private ItemStack buildPauseItem(TournamentMenuOptions options) {
        if (options.resumeOffered()) {
            return MenuItems.create(Material.CLOCK, "Continue the tournament",
                    "The round timer continues with its remaining time");
        }
        if (options.pauseOffered()) {
            return MenuItems.create(Material.CLOCK, "Pause the tournament",
                    "No further match is started until it continues",
                    "The round timer keeps its remaining time");
        }
        return MenuItems.unavailable("Pause the tournament", "No tournament is in progress");
    }

    private void handleForceResultClick(Player player, AdminMenuHolder holder, int slot) {
        if (slot == BACK_SLOT) {
            openMenu(player, AdminMenuHolder.match());
            return;
        }
        Optional<Match> activeMatch = findMatchOfMenu(holder);
        if (activeMatch.isEmpty()) {
            TournamentMessages.sendWarning(player,
                    "Match " + holder.getSubject().orElseThrow() + " is not running any more");
            openMenu(player, AdminMenuHolder.match());
            return;
        }
        Optional<Team> winningTeam = findWinningTeam(activeMatch.get(), slot);
        if (winningTeam.isEmpty()) {
            return;
        }
        reportOperation(player, tournamentManager.forceMatchResult(activeMatch.get().getMatchId(),
                winningTeam.get().getTeamName()));
        openMenu(player, AdminMenuHolder.match());
    }

    private Optional<Team> findWinningTeam(Match match, int slot) {
        return switch (slot) {
            case FORCE_RESULT_FIRST_TEAM_SLOT -> Optional.of(match.getFirstTeam());
            case FORCE_RESULT_SECOND_TEAM_SLOT -> Optional.of(match.getSecondTeam());
            default -> Optional.empty();
        };
    }

    private void fillForceResultMenu(Inventory inventory, String matchId) {
        Optional<Match> activeMatch = matchService.findMatchById(matchId);
        if (activeMatch.isEmpty()) {
            inventory.setItem(INFO_SLOT, MenuItems.info("Force a result",
                    List.of("Match " + matchId + " is not running any more")));
            inventory.setItem(BACK_SLOT, MenuItems.back("the match menu"));
            return;
        }
        Match match = activeMatch.get();
        inventory.setItem(INFO_SLOT, MenuItems.info("Force a result", List.of(
                "Match " + match.describe(),
                "Round " + match.getRoundNumber(),
                "A click decides the match for that team")));
        inventory.setItem(FORCE_RESULT_FIRST_TEAM_SLOT, buildWinnerItem(match.getFirstTeam()));
        inventory.setItem(FORCE_RESULT_SECOND_TEAM_SLOT, buildWinnerItem(match.getSecondTeam()));
        inventory.setItem(BACK_SLOT, MenuItems.back("the match menu"));
    }

    private ItemStack buildWinnerItem(Team team) {
        List<String> lore = new ArrayList<>();
        for (TeamMember member : team.getMembers()) {
            lore.add(member.playerName());
        }
        lore.add("Decides the match for this team");
        return MenuItems.create(Material.PLAYER_HEAD, team.getTeamName(), lore);
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
            case SAVE_KIT -> saveKit(player);
            case REMOVE_TEAM -> removeTeam(player, holder.getSubject().orElseThrow());
            case CANCEL_TOURNAMENT -> reportOperation(player, tournamentManager.cancelTournament());
        }
    }

    private void removeTeam(Player player, String teamName) {
        if (!teamRegistry.removeTeam(teamName)) {
            TournamentMessages.sendError(player, "Unknown team " + teamName);
            return;
        }
        teamConfigurationSaver.run();
        TournamentMessages.sendSuccess(player, "Team " + teamName + " removed");
    }

    private void openReturnMenu(Player player, AdminMenuHolder holder) {
        switch (holder.getConfirmationRequest().orElseThrow().getReturnMenuType()) {
            case ARENA -> openMenu(player, AdminMenuHolder.arena());
            case KIT -> openMenu(player, AdminMenuHolder.kit());
            case TEAMS -> openMenu(player, AdminMenuHolder.teams(holder.getPageIndex()));
            default -> openMenu(player, AdminMenuHolder.match());
        }
    }

    private void fillConfirmationMenu(Inventory inventory, AdminMenuHolder holder) {
        ConfirmationRequest request = holder.getConfirmationRequest().orElseThrow();
        inventory.setItem(INFO_SLOT, MenuItems.info(request.getTitle(), buildConfirmationLore(request, holder)));
        inventory.setItem(CONFIRM_YES_SLOT, MenuItems.create(MenuItems.CONFIRM_MATERIAL, "Yes",
                "Carries the action out"));
        inventory.setItem(CONFIRM_NO_SLOT, MenuItems.create(MenuItems.REFUSE_MATERIAL, "No",
                "Changes nothing and returns"));
    }

    private List<String> buildConfirmationLore(ConfirmationRequest request, AdminMenuHolder holder) {
        return switch (request) {
            case CLEAN_ARENA -> buildCleaningLore("The change cannot be undone",
                    "The border wall around it stays untouched");
            case SAVE_KIT -> buildKitSaveLore("The kit that stands in config.yml now is gone afterwards");
            case RESET_ARENA -> List.of(
                    "The fight area and all three spawns are dropped",
                    "The blocks of the arena and the border blocks stay untouched",
                    "No tournament can be started until it is calculated again");
            case REMOVE_TEAM -> List.of(
                    "Team " + holder.getSubject().orElseThrow() + " is removed from the registry",
                    "A running match of that team is not stopped by it");
            case CANCEL_TOURNAMENT -> List.of(
                    "The running tournament is cancelled",
                    "The running match is stopped and its bracket is dropped");
        };
    }

    private void askForTeamLine(Player player, int pageIndex) {
        closeMenu(player);
        chatInputService.await(player, "the team to add", List.of(
                        "Type: " + TeamCreationRequest.USAGE + " in chat",
                        "Example: Alpha Steve Alex",
                        "Both players have to be online or known to the server"),
                line -> handleTeamLine(player, pageIndex, line));
    }

    private void handleTeamLine(Player player, int pageIndex, String line) {
        if (!isStillAdmin(player)) {
            return;
        }
        TeamCreationRequest request = TeamCreationRequest.parse(line);
        if (!request.isAccepted()) {
            TournamentMessages.sendError(player, request.getRejectionMessage().orElseThrow());
            openMenu(player, AdminMenuHolder.teams(pageIndex));
            return;
        }
        registerTeam(player, request);
        openMenu(player, AdminMenuHolder.teams(pageIndex));
    }

    private void registerTeam(Player player, TeamCreationRequest request) {
        Optional<TeamMember> firstMember = teamMemberLookup.find(request.getFirstPlayerName());
        if (firstMember.isEmpty()) {
            TournamentMessages.sendError(player, describeUnknownPlayer(request.getFirstPlayerName()));
            return;
        }
        Optional<TeamMember> secondMember = teamMemberLookup.find(request.getSecondPlayerName());
        if (secondMember.isEmpty()) {
            TournamentMessages.sendError(player, describeUnknownPlayer(request.getSecondPlayerName()));
            return;
        }
        try {
            Team team = teamRegistry.registerTeam(request.getTeamName(), firstMember.get(), secondMember.get());
            teamConfigurationSaver.run();
            TournamentMessages.sendSuccess(player, "Team " + team.getTeamName() + " registered with "
                    + team.getMemberNamesJoined());
        } catch (IllegalArgumentException | IllegalStateException failure) {
            TournamentMessages.sendError(player, failure.getMessage());
        }
    }

    private static boolean isPlaceableBlock(Material material) {
        return material.isBlock() && material.isItem() && !material.isAir();
    }

    private String describeUnknownPlayer(String playerName) {
        return "Unknown player " + playerName
                + TournamentMessages.LINE_BREAK + "The player has to be online or known to the server";
    }

    private void askForBorderItemLine(Player player) {
        closeMenu(player);
        chatInputService.await(player, "the border blocks", List.of(
                        "Type: " + BorderItemRequest.USAGE + " in chat",
                        "Example: glass 16",
                        "The amount has to be between " + BorderItemRequest.MINIMUM_AMOUNT
                                + " and " + BorderItemRequest.MAXIMUM_AMOUNT),
                line -> handleBorderItemLine(player, line));
    }

    private void handleBorderItemLine(Player player, String line) {
        if (!isStillAdmin(player)) {
            return;
        }
        BorderItemRequestResult requestResult = BorderItemRequest.parseLine(line,
                AdminMenuService::isPlaceableBlock);
        if (!requestResult.isAccepted()) {
            TournamentMessages.sendError(player, requestResult.getRejectionMessage().orElseThrow());
            openMainMenu(player);
            return;
        }
        BorderItemRequest request = requestResult.getRequest().orElseThrow();
        ItemStack borderItem = spectatorBorderService.createBorderItem(request);
        Map<Integer, ItemStack> itemsThatDidNotFit = player.getInventory().addItem(borderItem);
        if (!itemsThatDidNotFit.isEmpty()) {
            TournamentMessages.sendWarning(player,
                    "Your inventory is full, not every border block could be handed out");
        }
        TournamentMessages.sendSuccess(player, "You received " + request.amount() + " "
                + request.material().name() + " border blocks"
                + TournamentMessages.LINE_BREAK
                + "Every block you place with them stops everybody who is not fighting");
        openMainMenu(player);
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
        if (!isStillAdmin(player)) {
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

    private boolean isStillAdmin(Player player) {
        if (player.hasPermission(TournamentCommand.ADMIN_PERMISSION)) {
            return true;
        }
        TournamentMessages.sendError(player, "You are not allowed to use the tournament menu");
        return false;
    }

    private void runOperation(Player player, Supplier<TournamentOperationResult> operation) {
        reportOperation(player, operation.get());
    }

    private static void reportOperation(Player player, TournamentOperationResult result) {
        if (result.successful()) {
            TournamentMessages.sendSuccess(player, result.message());
            return;
        }
        TournamentMessages.sendError(player, result.message());
    }

    private void runOperation(Player player, AdminMenuHolder holder,
                              Supplier<TournamentOperationResult> operation) {
        runOperation(player, operation);
        refresh(holder);
    }

    private TournamentMenuOptions currentOptions() {
        return TournamentMenuOptions.of(tournamentManager.getState(), tournamentManager.getFinalMatchStage(),
                matchService.hasActiveMatch());
    }

    private String describeActiveMatch() {
        return matchService.getActiveMatch().map(Match::describe).orElse("none");
    }

    private Optional<Match> findMatchOfMenu(AdminMenuHolder holder) {
        return matchService.findMatchById(holder.getSubject().orElseThrow());
    }

    private List<Team> collectTeams() {
        return new ArrayList<>(teamRegistry.getTeams());
    }

    private void openMenu(Player player, AdminMenuHolder holder) {
        Inventory inventory = plugin.getServer().createInventory(holder, sizeOf(holder.getMenuType()),
                Component.text(titleOf(holder)));
        holder.setInventory(inventory);
        fill(holder);
        plugin.getServer().getScheduler().runTask(plugin, () -> player.openInventory(inventory));
    }

    private void closeMenu(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> player.closeInventory());
    }

    private void refresh(AdminMenuHolder holder) {
        fill(holder);
    }

    private void fill(AdminMenuHolder holder) {
        Inventory inventory = holder.getInventory();
        inventory.clear();
        switch (holder.getMenuType()) {
            case MAIN -> fillMainMenu(inventory);
            case ARENA -> fillArenaMenu(inventory);
            case KIT -> fillKitMenu(inventory);
            case TEAMS -> fillTeamsMenu(inventory, holder.getPageIndex());
            case TEAM_ACTIONS -> fillTeamActionsMenu(inventory, holder.getSubject().orElseThrow());
            case MATCH -> fillMatchMenu(inventory);
            case FORCE_RESULT -> fillForceResultMenu(inventory, holder.getSubject().orElseThrow());
            case CONFIRMATION -> fillConfirmationMenu(inventory, holder);
        }
    }

    private static int sizeOf(AdminMenuType menuType) {
        return menuType == AdminMenuType.TEAMS ? TEAM_MENU_SIZE : SMALL_MENU_SIZE;
    }

    private String titleOf(AdminMenuHolder holder) {
        return switch (holder.getMenuType()) {
            case MAIN -> "Tournament administration";
            case ARENA -> "Arena";
            case KIT -> "Kit";
            case TEAMS -> "Teams (page "
                    + MenuPagination.of(teamRegistry.getTeamCount(), holder.getPageIndex()).getPageNumber() + ")";
            case TEAM_ACTIONS -> "Team " + holder.getSubject().orElseThrow();
            case MATCH -> "Match and tournament";
            case FORCE_RESULT -> "Result of " + holder.getSubject().orElseThrow();
            case CONFIRMATION -> holder.getConfirmationRequest().orElseThrow().getTitle();
        };
    }
}
