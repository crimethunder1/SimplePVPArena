package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaHeightMarkerService;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.config.ConfigurationReloadReport;
import de.simplepvparena.diagnostics.TournamentDryRun;
import de.simplepvparena.gate.StartGateItemService;
import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.kit.KitLibrary;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.team.EventTeamImportService;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.Supplier;

public final class AdminMenuService {

    private final AdminMenuNavigator navigator;

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
                            KitLibrary kitLibrary,
                            KitSaveService kitSaveService,
                            ChatInputService chatInputService,
                            Runnable teamConfigurationSaver,
                            Supplier<ConfigurationReloadReport> configurationReloader) {
        this(plugin, tournamentManager, tournamentDryRun, teamRegistry, teamMemberLookup, eventTeamImportService,
                matchService, arenaSetupService, heightMarkerService, gateItemService, borderSelectorModes,
                kitLibrary, kitSaveService, chatInputService, teamConfigurationSaver, configurationReloader,
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
                     KitLibrary kitLibrary,
                     KitSaveService kitSaveService,
                     ChatInputService chatInputService,
                     Runnable teamConfigurationSaver,
                     Supplier<ConfigurationReloadReport> configurationReloader,
                     AdminMenuWindows windows) {
        Objects.requireNonNull(plugin, "plugin");
        this.navigator = new AdminMenuNavigator(windows);
        Objects.requireNonNull(tournamentManager, "tournamentManager");
        Objects.requireNonNull(tournamentDryRun, "tournamentDryRun");
        Objects.requireNonNull(teamRegistry, "teamRegistry");
        Objects.requireNonNull(teamMemberLookup, "teamMemberLookup");
        Objects.requireNonNull(eventTeamImportService, "eventTeamImportService");
        Objects.requireNonNull(matchService, "matchService");
        Objects.requireNonNull(arenaSetupService, "arenaSetupService");
        Objects.requireNonNull(heightMarkerService, "heightMarkerService");
        Objects.requireNonNull(gateItemService, "gateItemService");
        Objects.requireNonNull(borderSelectorModes, "borderSelectorModes");
        Objects.requireNonNull(chatInputService, "chatInputService");
        KitAdminMenu kitAdminMenu = new KitAdminMenu(navigator, kitLibrary, kitSaveService, chatInputService);
        KitActionsAdminMenu kitActionsAdminMenu = new KitActionsAdminMenu(navigator, kitLibrary, kitSaveService,
                matchService::hasActiveMatch);
        Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
        Objects.requireNonNull(configurationReloader, "configurationReloader");
        TeamActionsAdminMenu teamActionsAdminMenu = new TeamActionsAdminMenu(navigator, teamRegistry,
                teamConfigurationSaver);
        ForceResultAdminMenu forceResultAdminMenu = new ForceResultAdminMenu(navigator, tournamentManager,
                matchService);
        WaitingMatchesAdminMenu waitingMatchesAdminMenu = new WaitingMatchesAdminMenu(navigator, tournamentManager,
                teamRegistry);
        WaitingMatchResultAdminMenu waitingMatchResultAdminMenu = new WaitingMatchResultAdminMenu(navigator,
                tournamentManager, teamRegistry);
        TournamentMenuStatus tournamentMenuStatus = new TournamentMenuStatus(tournamentManager, matchService);
        MatchAdminMenu matchAdminMenu = new MatchAdminMenu(navigator, tournamentManager, matchService,
                tournamentMenuStatus, forceResultAdminMenu, waitingMatchesAdminMenu);
        TeamsAdminMenu teamsAdminMenu = new TeamsAdminMenu(navigator, teamRegistry, teamMemberLookup,
                eventTeamImportService, matchService::hasActiveMatch, chatInputService, teamConfigurationSaver);
        ArenaAdminMenu arenaAdminMenu = new ArenaAdminMenu(navigator, arenaSetupService, heightMarkerService,
                gateItemService, chatInputService);
        MainAdminMenu mainAdminMenu = new MainAdminMenu(navigator, tournamentManager, tournamentDryRun, teamRegistry,
                matchService, arenaSetupService, borderSelectorModes, configurationReloader, tournamentMenuStatus,
                kitLibrary);
        ConfirmationAdminMenu confirmationAdminMenu = new ConfirmationAdminMenu(navigator);
        confirmationAdminMenu.register(ConfirmationRequest.CLEAN_ARENA, arenaAdminMenu.cleanConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.RESET_ARENA, arenaAdminMenu.resetConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.REPLACE_KIT, kitActionsAdminMenu.replaceConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.DELETE_KIT, kitActionsAdminMenu.deleteConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.REMOVE_TEAM, teamActionsAdminMenu.removeConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.IMPORT_TEAMS, teamsAdminMenu.importConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.CANCEL_TOURNAMENT, matchAdminMenu.cancelConfirmation());
        confirmationAdminMenu.requireEveryConfirmationRegistered();
        navigator.register(AdminMenuType.MAIN, mainAdminMenu);
        navigator.register(AdminMenuType.ARENA, arenaAdminMenu);
        navigator.register(AdminMenuType.KIT, kitAdminMenu);
        navigator.register(AdminMenuType.KIT_ACTIONS, kitActionsAdminMenu);
        navigator.register(AdminMenuType.TEAMS, teamsAdminMenu);
        navigator.register(AdminMenuType.TEAM_ACTIONS, teamActionsAdminMenu);
        navigator.register(AdminMenuType.MATCH, matchAdminMenu);
        navigator.register(AdminMenuType.FORCE_RESULT, forceResultAdminMenu);
        navigator.register(AdminMenuType.WAITING_MATCHES, waitingMatchesAdminMenu);
        navigator.register(AdminMenuType.WAITING_MATCH_RESULT, waitingMatchResultAdminMenu);
        navigator.register(AdminMenuType.CONFIRMATION, confirmationAdminMenu);
        navigator.requireEveryMenuRegistered();
    }

    public void openMainMenu(Player player) {
        navigator.open(player, AdminMenuHolder.main());
    }

    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        if (!AdminMenuReplies.isStillAdmin(player)) {
            navigator.close(player);
            return;
        }
        navigator.menuOf(holder.getMenuType()).handleClick(player, holder, slot);
    }
}
