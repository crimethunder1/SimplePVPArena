package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.config.ConfigurationReloadReport;
import de.simplepvparena.diagnostics.DryRunReport;
import de.simplepvparena.diagnostics.TournamentDryRun;
import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.MainMenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.KitChoiceService;
import de.simplepvparena.tournament.PendingKitChoice;
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
    private static final int MAIN_KIT_CHOICE_SLOT = 23;
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
    private final KitChoiceService kitChoiceService;

    MainAdminMenu(AdminMenuNavigation navigation, TournamentManager tournamentManager,
                  TournamentDryRun tournamentDryRun, TeamRegistry teamRegistry, MatchService matchService,
                  ArenaSetupService arenaSetupService, BorderSelectorModeRegistry borderSelectorModes,
                  Supplier<ConfigurationReloadReport> configurationReloader,
                  TournamentMenuStatus tournamentMenuStatus, KitLibrary kitLibrary,
                  KitChoiceService kitChoiceService) {
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
        this.kitChoiceService = Objects.requireNonNull(kitChoiceService, "kitChoiceService");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(MainMenuMessage.TITLE);
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info(Messages.text(MainMenuMessage.INFO_TITLE),
                buildMainInfoLore()));
        canvas.set(MAIN_ARENA_SLOT, MenuItems.create(Material.GRASS_BLOCK, Messages.text(MainMenuMessage.ARENA_TITLE),
                Messages.text(MainMenuMessage.ARENA_LORE),
                Messages.text(arenaSetupService.isArenaReady()
                        ? MainMenuMessage.ARENA_READY
                        : MainMenuMessage.ARENA_INCOMPLETE)));
        canvas.set(MAIN_TEAMS_SLOT, MenuItems.create(Material.PLAYER_HEAD, Messages.text(MainMenuMessage.TEAMS_TITLE),
                describeRegisteredTeams(),
                Messages.text(MainMenuMessage.TEAMS_LORE)));
        canvas.set(MAIN_MATCH_SLOT, MenuItems.create(Material.DIAMOND_SWORD, Messages.text(MainMenuMessage.MATCH_TITLE),
                Messages.text(MainMenuMessage.MATCH_LORE),
                tournamentMenuStatus.describeActiveMatchLine()));
        canvas.set(MAIN_BORDER_SELECTOR_SLOT, MenuItems.create(Material.BRUSH,
                Messages.text(MainMenuMessage.BORDER_SELECTOR_TITLE), buildBorderSelectorLore(player)));
        canvas.set(MAIN_FINAL_SLOT, tournamentMenuStatus.buildFinalItem());
        canvas.set(MAIN_STATUS_SLOT, MenuItems.create(Material.PAPER, Messages.text(MainMenuMessage.STATUS_TITLE),
                Messages.lines(MainMenuMessage.STATUS_LORE)));
        canvas.set(MAIN_BRACKET_SLOT, MenuItems.create(Material.WRITTEN_BOOK,
                Messages.text(MainMenuMessage.BRACKET_TITLE), Messages.lines(MainMenuMessage.BRACKET_LORE)));
        canvas.set(MAIN_DRY_RUN_SLOT, buildDryRunItem());
        canvas.set(MAIN_RELOAD_SLOT, MenuItems.create(Material.COMPARATOR, Messages.text(MainMenuMessage.RELOAD_TITLE),
                buildReloadLore()));
        canvas.set(MAIN_KIT_SLOT, MenuItems.create(Material.DIAMOND_CHESTPLATE,
                Messages.text(MainMenuMessage.KITS_TITLE), buildKitLore()));
        kitChoiceService.getPending().ifPresent(choice -> canvas.set(MAIN_KIT_CHOICE_SLOT,
                buildKitChoiceItem(choice)));
        canvas.set(MAIN_CLOSE_SLOT, MenuItems.create(Material.BARRIER, Messages.text(MainMenuMessage.CLOSE_TITLE),
                Messages.lines(MainMenuMessage.CLOSE_LORE)));
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
            case MAIN_KIT_CHOICE_SLOT -> kitChoiceService.getPending().ifPresent(choice ->
                    navigation.open(player, AdminMenuHolder.kitChoice(choice.matchId(), 0)));
            case MAIN_CLOSE_SLOT -> navigation.close(player);
            default -> { }
        }
    }

    private void showBracket(Player player) {
        navigation.close(player);
        TournamentMessages.sendBlock(player, TournamentMessages.info(Messages.text(MainMenuMessage.BRACKET_HEADING)),
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

    private static MenuTile buildKitChoiceItem(PendingKitChoice choice) {
        return MenuItems.create(Material.CHEST, Messages.text(MainMenuMessage.KIT_CHOICE_TITLE),
                Messages.text(MainMenuMessage.KIT_CHOICE_MATCH, Placeholder.of("match", choice.matchId()),
                        Placeholder.of("firstTeam", choice.firstTeamName()),
                        Placeholder.of("secondTeam", choice.secondTeamName())),
                Messages.text(MainMenuMessage.KIT_CHOICE_LORE));
    }

    private MenuTile buildDryRunItem() {
        Optional<String> rejection = tournamentDryRun.findRejection();
        if (rejection.isPresent()) {
            return MenuItems.create(MenuItems.UNAVAILABLE_MATERIAL, Messages.text(MainMenuMessage.DRY_RUN_TITLE),
                    AdminMenuReplies.toLoreLines(rejection.get()));
        }
        return MenuItems.create(Material.OBSERVER, Messages.text(MainMenuMessage.DRY_RUN_TITLE), buildDryRunLore());
    }

    private List<String> buildDryRunLore() {
        List<String> lore = new ArrayList<>(Messages.lines(MainMenuMessage.DRY_RUN_LORE));
        lore.add(Messages.text(CommonMessage.MENU_CLOSES_TO_READ));
        return lore;
    }

    private static String describeRunningMatchDelay() {
        return Messages.text(MainMenuMessage.RELOAD_RUNNING_MATCH_DELAY);
    }

    private List<String> buildReloadLore() {
        List<String> lore = new ArrayList<>(Messages.lines(MainMenuMessage.RELOAD_LORE));
        if (matchService.hasActiveMatch()) {
            lore.addAll(Messages.lines(MainMenuMessage.RELOAD_MATCH_RUNNING));
        }
        lore.add(Messages.text(CommonMessage.MENU_CLOSES_TO_READ));
        return lore;
    }

    private List<String> buildKitLore() {
        List<String> lore = new ArrayList<>();
        lore.add(Messages.plural(MainMenuMessage.KIT_COUNT, kitLibrary.getKitCount()));
        lore.add(kitLibrary.getDefaultKitName()
                .map(kitName -> Messages.text(MainMenuMessage.DEFAULT_KIT, Placeholder.of("kit", kitName)))
                .orElseGet(() -> Messages.text(MainMenuMessage.NO_DEFAULT_KIT)));
        lore.addAll(Messages.lines(MainMenuMessage.KITS_LORE));
        return lore;
    }

    private List<String> buildMainInfoLore() {
        List<String> lore = new ArrayList<>();
        lore.add(tournamentManager.describeStatus());
        lore.add(describeRegisteredTeams());
        lore.add(Messages.text(arenaSetupService.isArenaReady()
                ? MainMenuMessage.INFO_ARENA_READY
                : MainMenuMessage.INFO_ARENA_INCOMPLETE));
        lore.add(tournamentMenuStatus.describeActiveMatchLine());
        return lore;
    }

    private String describeRegisteredTeams() {
        return Messages.plural(MainMenuMessage.REGISTERED_TEAMS, teamRegistry.getTeamCount());
    }

    private void toggleBorderSelectorMode(Player player) {
        navigation.close(player);
        if (borderSelectorModes.toggle(player.getUniqueId())) {
            TournamentMessages.sendSuccess(player, Messages.text(MainMenuMessage.BORDER_SELECTOR_SWITCHED_ON));
            return;
        }
        TournamentMessages.sendSuccess(player, Messages.text(MainMenuMessage.BORDER_SELECTOR_SWITCHED_OFF));
    }

    private List<String> buildBorderSelectorLore(Player player) {
        List<String> lore = new ArrayList<>(Messages.lines(MainMenuMessage.BORDER_SELECTOR_LORE));
        lore.add(Messages.text(borderSelectorModes.isActive(player.getUniqueId())
                ? MainMenuMessage.BORDER_SELECTOR_MODE_ON
                : MainMenuMessage.BORDER_SELECTOR_MODE_OFF));
        return lore;
    }

    private void runOperation(Player player, Supplier<TournamentOperationResult> operation) {
        AdminMenuReplies.reportOperation(player, operation.get());
    }
}
