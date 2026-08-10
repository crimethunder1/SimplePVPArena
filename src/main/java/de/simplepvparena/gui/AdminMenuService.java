package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.ArenaSpawnType;
import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
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
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

public final class AdminMenuService {

    private static final int SMALL_MENU_SIZE = 27;
    private static final int TEAM_MENU_SIZE = 54;

    private static final int INFO_SLOT = 4;

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int MAIN_TEAMS_SLOT = 12;
    private static final int MAIN_MATCH_SLOT = 14;
    private static final int MAIN_FINAL_SLOT = 16;
    private static final int MAIN_CLOSE_SLOT = 22;

    private static final int ARENA_CALCULATE_SLOT = 10;
    private static final int ARENA_FIRST_TEAM_SPAWN_SLOT = 11;
    private static final int ARENA_SECOND_TEAM_SPAWN_SLOT = 12;
    private static final int ARENA_TRIBUNE_SPAWN_SLOT = 13;
    private static final int ARENA_CLEAN_SLOT = 15;
    private static final int ARENA_RESET_SLOT = 16;

    private static final int TEAMS_INFO_SLOT = 47;
    private static final int TEAMS_PREVIOUS_PAGE_SLOT = 45;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;
    private static final int TEAMS_BACK_SLOT = 49;

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
    private final MatchService matchService;
    private final ArenaSetupService arenaSetupService;
    private final Runnable teamConfigurationSaver;

    public AdminMenuService(Plugin plugin,
                            TournamentManager tournamentManager,
                            TeamRegistry teamRegistry,
                            MatchService matchService,
                            ArenaSetupService arenaSetupService,
                            Runnable teamConfigurationSaver) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.arenaSetupService = Objects.requireNonNull(arenaSetupService, "arenaSetupService");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
    }

    public void openMainMenu(Player player) {
        openMenu(player, AdminMenuHolder.main());
    }

    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        if (!player.hasPermission(TournamentCommand.ADMIN_PERMISSION)) {
            closeMenu(player);
            player.sendMessage("You are not allowed to use the tournament menu");
            return;
        }
        switch (holder.getMenuType()) {
            case MAIN -> handleMainClick(player, slot);
            case ARENA -> handleArenaClick(player, holder, slot);
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
            case MAIN_FINAL_SLOT -> runOperation(player, tournamentManager::startFinalMatch);
            case MAIN_CLOSE_SLOT -> closeMenu(player);
            default -> { }
        }
    }

    private void fillMainMenu(Inventory inventory) {
        inventory.setItem(INFO_SLOT, MenuItems.info("Tournament", buildMainInfoLore()));
        inventory.setItem(MAIN_ARENA_SLOT, MenuItems.create(Material.GRASS_BLOCK, "Arena",
                "Fight area, spawns and cleaning",
                "Arena " + (arenaSetupService.isArenaReady() ? "ready" : "incomplete")));
        inventory.setItem(MAIN_TEAMS_SLOT, MenuItems.create(Material.PLAYER_HEAD, "Teams",
                "Registered teams: " + teamRegistry.getTeamCount(),
                "Shows every team and removes one of them"));
        inventory.setItem(MAIN_MATCH_SLOT, MenuItems.create(Material.DIAMOND_SWORD, "Match and tournament",
                "Start, pause, cancel and force a result",
                "Running match: " + describeActiveMatch()));
        inventory.setItem(MAIN_FINAL_SLOT, buildFinalItem());
        inventory.setItem(MAIN_CLOSE_SLOT, MenuItems.create(Material.BARRIER, "Close", "Closes the menu"));
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
            case ARENA_FIRST_TEAM_SPAWN_SLOT -> setSpawn(player, holder, ArenaSpawnType.FIRST_TEAM);
            case ARENA_SECOND_TEAM_SPAWN_SLOT -> setSpawn(player, holder, ArenaSpawnType.SECOND_TEAM);
            case ARENA_TRIBUNE_SPAWN_SLOT -> setSpawn(player, holder, ArenaSpawnType.TRIBUNE);
            case ARENA_CLEAN_SLOT -> askForCleaningConfirmation(player);
            case ARENA_RESET_SLOT -> {
                arenaSetupService.clearConfiguration(player);
                refresh(holder);
            }
            case BACK_SLOT -> openMenu(player, AdminMenuHolder.main());
            default -> { }
        }
    }

    private void setSpawn(Player player, AdminMenuHolder holder, ArenaSpawnType spawnType) {
        arenaSetupService.setSpawn(player, spawnType);
        refresh(holder);
    }

    private void askForCleaningConfirmation(Player player) {
        Optional<String> rejection = arenaSetupService.findCleaningRejection();
        if (rejection.isPresent()) {
            player.sendMessage(rejection.get());
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
        inventory.setItem(ARENA_FIRST_TEAM_SPAWN_SLOT, buildSpawnItem(Material.BLUE_BED, ArenaSpawnType.FIRST_TEAM));
        inventory.setItem(ARENA_SECOND_TEAM_SPAWN_SLOT, buildSpawnItem(Material.RED_BED, ArenaSpawnType.SECOND_TEAM));
        inventory.setItem(ARENA_TRIBUNE_SPAWN_SLOT, buildSpawnItem(Material.YELLOW_BED, ArenaSpawnType.TRIBUNE));
        inventory.setItem(ARENA_CLEAN_SLOT, MenuItems.create(Material.TNT, "Clean the arena",
                "Sets every block of the fight area to air",
                arenaSetupService.countCleaningBlocks() + " blocks are affected",
                "Asks for a confirmation first"));
        inventory.setItem(ARENA_RESET_SLOT, MenuItems.create(MenuItems.DESTRUCTIVE_MATERIAL, "Reset the arena",
                "Drops the fight area and all three spawns",
                "The blocks and the border blocks stay untouched"));
        inventory.setItem(BACK_SLOT, MenuItems.back("the main menu"));
    }

    private ItemStack buildSpawnItem(Material material, ArenaSpawnType spawnType) {
        return MenuItems.create(material, "Set the " + spawnType.getDisplayName(),
                "Sets it to the position you stand on",
                "Same as /tournament arena setspawn " + spawnType.getArgumentName());
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
                "A team is added in the chat:",
                "/tournament team add <teamName> <firstPlayer> <secondPlayer>",
                "Two player names cannot be typed into an inventory")));
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
            player.sendMessage("No match is running");
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
            player.sendMessage("Match " + holder.getSubject().orElseThrow() + " is not running any more");
            openMenu(player, AdminMenuHolder.match());
            return;
        }
        Optional<Team> winningTeam = findWinningTeam(activeMatch.get(), slot);
        if (winningTeam.isEmpty()) {
            return;
        }
        player.sendMessage(tournamentManager.forceMatchResult(activeMatch.get().getMatchId(),
                winningTeam.get().getTeamName()).message());
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
            case REMOVE_TEAM -> removeTeam(player, holder.getSubject().orElseThrow());
            case CANCEL_TOURNAMENT -> player.sendMessage(tournamentManager.cancelTournament().message());
        }
    }

    private void removeTeam(Player player, String teamName) {
        if (!teamRegistry.removeTeam(teamName)) {
            player.sendMessage("Unknown team " + teamName);
            return;
        }
        teamConfigurationSaver.run();
        player.sendMessage("Team " + teamName + " removed");
    }

    private void openReturnMenu(Player player, AdminMenuHolder holder) {
        switch (holder.getConfirmationRequest().orElseThrow().getReturnMenuType()) {
            case ARENA -> openMenu(player, AdminMenuHolder.arena());
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
            case CLEAN_ARENA -> List.of(
                    "Every block of the calculated fight area is set to air",
                    arenaSetupService.countCleaningBlocks() + " blocks are affected",
                    "The change cannot be undone",
                    "The border wall around it stays untouched");
            case REMOVE_TEAM -> List.of(
                    "Team " + holder.getSubject().orElseThrow() + " is removed from the registry",
                    "A running match of that team is not stopped by it");
            case CANCEL_TOURNAMENT -> List.of(
                    "The running tournament is cancelled",
                    "The running match is stopped and its bracket is dropped");
        };
    }

    private void runOperation(Player player, Supplier<TournamentOperationResult> operation) {
        player.sendMessage(operation.get().message());
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
            case TEAMS -> "Teams (page "
                    + MenuPagination.of(teamRegistry.getTeamCount(), holder.getPageIndex()).getPageNumber() + ")";
            case TEAM_ACTIONS -> "Team " + holder.getSubject().orElseThrow();
            case MATCH -> "Match and tournament";
            case FORCE_RESULT -> "Result of " + holder.getSubject().orElseThrow();
            case CONFIRMATION -> holder.getConfirmationRequest().orElseThrow().getTitle();
        };
    }
}
