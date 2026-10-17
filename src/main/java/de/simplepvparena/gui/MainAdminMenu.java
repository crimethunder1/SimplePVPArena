package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.config.ConfigurationReloadReport;
import de.simplepvparena.diagnostics.DryRunReport;
import de.simplepvparena.diagnostics.TournamentDryRun;
import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentOperationResult;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

final class MainAdminMenu implements AdminMenu {

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

    private final AdminMenuNavigation navigation;
    private final TournamentManager tournamentManager;
    private final TournamentDryRun tournamentDryRun;
    private final TeamRegistry teamRegistry;
    private final MatchService matchService;
    private final ArenaSetupService arenaSetupService;
    private final BorderSelectorModeRegistry borderSelectorModes;
    private final Supplier<ConfigurationReloadReport> configurationReloader;
    private final TournamentMenuStatus tournamentMenuStatus;
    private final KitLibrary kitLibrary;

    MainAdminMenu(AdminMenuNavigation navigation, TournamentManager tournamentManager,
                  TournamentDryRun tournamentDryRun, TeamRegistry teamRegistry, MatchService matchService,
                  ArenaSetupService arenaSetupService, BorderSelectorModeRegistry borderSelectorModes,
                  Supplier<ConfigurationReloadReport> configurationReloader,
                  TournamentMenuStatus tournamentMenuStatus, KitLibrary kitLibrary) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.tournamentDryRun = Objects.requireNonNull(tournamentDryRun, "tournamentDryRun");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.arenaSetupService = Objects.requireNonNull(arenaSetupService, "arenaSetupService");
        this.borderSelectorModes = Objects.requireNonNull(borderSelectorModes, "borderSelectorModes");
        this.configurationReloader = Objects.requireNonNull(configurationReloader, "configurationReloader");
        this.tournamentMenuStatus = Objects.requireNonNull(tournamentMenuStatus, "tournamentMenuStatus");
        this.kitLibrary = Objects.requireNonNull(kitLibrary, "kitLibrary");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return "Tournament administration";
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
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
        canvas.set(MAIN_KIT_SLOT, MenuItems.create(Material.DIAMOND_CHESTPLATE, "Kits",
                "Kits: " + kitLibrary.getKitCount(),
                "Default kit: " + kitLibrary.getDefaultKitName().orElse("none"),
                "The items every fighter is given when a round starts",
                "Create, replace and delete kits"));
        canvas.set(MAIN_CLOSE_SLOT, MenuItems.create(Material.BARRIER, "Close", "Closes the menu"));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        switch (slot) {
            case MAIN_ARENA_SLOT -> navigation.open(player, AdminMenuHolder.arena());
            case MAIN_TEAMS_SLOT -> navigation.open(player, AdminMenuHolder.teams(0));
            case MAIN_MATCH_SLOT -> navigation.open(player, AdminMenuHolder.match());
            case MAIN_BORDER_SELECTOR_SLOT -> toggleBorderSelectorMode(player);
            case MAIN_FINAL_SLOT -> runOperation(player, tournamentManager::startFinalMatch);
            case MAIN_STATUS_SLOT -> TournamentMessages.sendInfo(player, tournamentManager.describeStatus());
            case MAIN_BRACKET_SLOT -> showBracket(player);
            case MAIN_DRY_RUN_SLOT -> runDryRun(player);
            case MAIN_RELOAD_SLOT -> reloadConfiguration(player);
            case MAIN_KIT_SLOT -> navigation.open(player, AdminMenuHolder.kit(0));
            case MAIN_CLOSE_SLOT -> navigation.close(player);
            default -> { }
        }
    }

    private void showBracket(Player player) {
        navigation.close(player);
        TournamentMessages.sendBlock(player, TournamentMessages.info("Bracket"),
                tournamentManager.describeBracket());
    }

    private void reloadConfiguration(Player player) {
        navigation.close(player);
        boolean matchWasRunning = matchService.hasActiveMatch();
        ConfigurationReloadReport report = configurationReloader.get();
        TournamentMessages.sendInfoReport(player, report.describe());
        if (matchWasRunning) {
            TournamentMessages.sendWarning(player, describeRunningMatchDelay());
        }
    }

    private void runDryRun(Player player) {
        navigation.close(player);
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

    private List<String> buildReloadLore() {
        List<String> lore = new ArrayList<>();
        lore.add("Reads config.yml again and applies it live");
        lore.add("Round timeout, cleaning batch sizes, cell limit,");
        lore.add("safety zone and border particles");
        lore.add("The arena, the teams and a running match stay untouched");
        if (matchService.hasActiveMatch()) {
            lore.add("A match is running: the new round timeout");
            lore.add("arrives with the next round");
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
        navigation.close(player);
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

    private void runOperation(Player player, Supplier<TournamentOperationResult> operation) {
        AdminMenuReplies.reportOperation(player, operation.get());
    }
}
