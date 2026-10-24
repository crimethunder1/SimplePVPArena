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
import de.simplepvparena.match.PendingUnequalTeamVote;
import de.simplepvparena.match.UnequalTeamVoteService;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.message.UnequalTeamVoteMessage;
import de.simplepvparena.registration.RegistrationService;
import de.simplepvparena.registration.TeamChoiceService;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeWriter;
import de.simplepvparena.tournament.KitChoiceService;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class AdminMenuService {

    private final AdminMenuNavigator navigator;
    private final BracketViewMenu bracketViewMenu;
    private final UnequalTeamVoteService unequalTeamVoteService;
    private final UnequalTeamVoteMenuPresenter unequalTeamVoteMenuPresenter;
    private final TeamChoiceService teamChoiceService;
    private final TeamChoiceMenuPresenter teamChoiceMenuPresenter;

    public AdminMenuService(AdminMenuDependencies dependencies) {
        this(dependencies, new InventoryAdminMenuWindows(dependencies.plugin()),
                () -> dependencies.plugin().getServer().getOnlinePlayers());
    }

    AdminMenuService(AdminMenuDependencies dependencies,
                     AdminMenuWindows windows,
                     Supplier<Collection<? extends Player>> onlinePlayers) {
        Plugin plugin = Objects.requireNonNull(dependencies.plugin(), "plugin");
        AdminMenuDependencies.TournamentServices tournament = dependencies.tournament();
        AdminMenuDependencies.TeamServices teams = dependencies.teams();
        AdminMenuDependencies.ArenaServices arena = dependencies.arena();
        AdminMenuDependencies.KitServices kits = dependencies.kits();
        AdminMenuDependencies.RegistrationServices registration = dependencies.registration();
        this.navigator = new AdminMenuNavigator(windows);
        TournamentManager tournamentManager = Objects.requireNonNull(tournament.tournamentManager(),
                "tournamentManager");
        TournamentDryRun tournamentDryRun = Objects.requireNonNull(tournament.tournamentDryRun(), "tournamentDryRun");
        TeamRegistry teamRegistry = Objects.requireNonNull(teams.teamRegistry(), "teamRegistry");
        TeamMemberLookup teamMemberLookup = Objects.requireNonNull(teams.teamMemberLookup(), "teamMemberLookup");
        MatchService matchService = Objects.requireNonNull(tournament.matchService(), "matchService");
        ArenaSetupService arenaSetupService = Objects.requireNonNull(arena.arenaSetupService(), "arenaSetupService");
        ArenaHeightMarkerService heightMarkerService = Objects.requireNonNull(arena.heightMarkerService(),
                "heightMarkerService");
        StartGateItemService gateItemService = Objects.requireNonNull(arena.gateItemService(), "gateItemService");
        BorderSelectorModeRegistry borderSelectorModes = Objects.requireNonNull(arena.borderSelectorModes(),
                "borderSelectorModes");
        ChatInputService chatInputService = Objects.requireNonNull(dependencies.chatInputService(),
                "chatInputService");
        KitLibrary kitLibrary = kits.kitLibrary();
        KitSaveService kitSaveService = kits.kitSaveService();
        KitAdminMenu kitAdminMenu = new KitAdminMenu(navigator, kitLibrary, kitSaveService, chatInputService);
        KitActionsAdminMenu kitActionsAdminMenu = new KitActionsAdminMenu(navigator, kitLibrary, kitSaveService,
                matchService::hasActiveMatch);
        KitChoiceService kitChoiceService = tournamentManager.getKitChoiceService();
        KitChoiceAdminMenu kitChoiceAdminMenu = new KitChoiceAdminMenu(navigator, kitLibrary, kitChoiceService);
        kitChoiceService.setPresenter(new KitChoiceAdminMenuPresenter(navigator, onlinePlayers));
        unequalTeamVoteService = matchService.getUnequalTeamVoteService();
        UnequalTeamVoteMenu unequalTeamVoteMenu = new UnequalTeamVoteMenu(navigator, unequalTeamVoteService);
        unequalTeamVoteMenuPresenter = new UnequalTeamVoteMenuPresenter(navigator, onlinePlayers);
        unequalTeamVoteService.setPresenter(unequalTeamVoteMenuPresenter);
        this.teamChoiceService = Objects.requireNonNull(registration.teamChoiceService(), "teamChoiceService");
        TeamChoiceMenu teamChoiceMenu = new TeamChoiceMenu(navigator, teamChoiceService);
        teamChoiceMenuPresenter = new TeamChoiceMenuPresenter(navigator, teamChoiceService, onlinePlayers,
                task -> plugin.getServer().getScheduler().runTask(plugin, task));
        teamChoiceService.setPresenter(teamChoiceMenuPresenter);
        Runnable teamConfigurationSaver = Objects.requireNonNull(teams.teamConfigurationSaver(),
                "teamConfigurationSaver");
        Supplier<ConfigurationReloadReport> configurationReloader = Objects.requireNonNull(
                dependencies.configurationReloader(), "configurationReloader");
        TeamSizeWriter teamSizeWriter = Objects.requireNonNull(teams.teamSizeWriter(), "teamSizeWriter");
        RegistrationService registrationService = registration.registrationService();
        TeamActionsAdminMenu teamActionsAdminMenu = new TeamActionsAdminMenu(navigator, teamRegistry,
                teamMemberLookup, chatInputService, teamConfigurationSaver, tournamentManager);
        TeamMembersAdminMenu teamMembersAdminMenu = new TeamMembersAdminMenu(navigator, teamRegistry,
                teamConfigurationSaver, tournamentManager);
        ForceResultAdminMenu forceResultAdminMenu = new ForceResultAdminMenu(navigator, tournamentManager,
                matchService);
        WaitingMatchesAdminMenu waitingMatchesAdminMenu = new WaitingMatchesAdminMenu(navigator, tournamentManager,
                teamRegistry);
        WaitingMatchResultAdminMenu waitingMatchResultAdminMenu = new WaitingMatchResultAdminMenu(navigator,
                tournamentManager, teamRegistry);
        DecidedMatchesAdminMenu decidedMatchesAdminMenu = new DecidedMatchesAdminMenu(navigator, tournamentManager);
        TournamentMenuStatus tournamentMenuStatus = new TournamentMenuStatus(tournamentManager, matchService);
        MatchAdminMenu matchAdminMenu = new MatchAdminMenu(navigator, tournamentManager, matchService,
                tournamentMenuStatus, forceResultAdminMenu, waitingMatchesAdminMenu, decidedMatchesAdminMenu,
                tournament.autoMatchService(), tournament.autoMatchModeWriter());
        TeamsAdminMenu teamsAdminMenu = new TeamsAdminMenu(navigator, teamRegistry, teamMemberLookup,
                chatInputService, teamConfigurationSaver, tournamentManager.getTeamSizeSetting(),
                tournamentManager::isTournamentInProgress, teamSizeWriter, registrationService::getCount);
        RegistrationsAdminMenu registrationsAdminMenu = new RegistrationsAdminMenu(navigator, registrationService,
                registration.registrationStationService(), registration.randomTeamFormation(),
                tournamentManager.getTeamSizeSetting(), registration.teamModeSetting(), registration.teamModeWriter(),
                teamChoiceService);
        ArenaAdminMenu arenaAdminMenu = new ArenaAdminMenu(navigator, arenaSetupService, heightMarkerService,
                gateItemService, chatInputService, arena.borderParticleService(),
                arena.confirmedBorderParticleWriter(), arena.borderMarkClearing(), arena.startGateMarkClearing(),
                arena.effectMarginSetting(), arena.effectMarginWriter(), arena.standingsBoardToolService());
        bracketViewMenu = new BracketViewMenu(navigator, tournamentManager, matchService);
        MainAdminMenu mainAdminMenu = new MainAdminMenu(navigator, tournamentManager, tournamentDryRun, teamRegistry,
                matchService, arenaSetupService, borderSelectorModes, configurationReloader, tournamentMenuStatus,
                kitLibrary, kitChoiceService, bracketViewMenu);
        ConfirmationAdminMenu confirmationAdminMenu = new ConfirmationAdminMenu(navigator);
        confirmationAdminMenu.register(ConfirmationRequest.CLEAN_ARENA, arenaAdminMenu.cleanConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.RESET_ARENA, arenaAdminMenu.resetConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.CLEAR_BORDER_MARKS,
                arenaAdminMenu.clearBorderMarksConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.CLEAR_START_GATE_MARKS,
                arenaAdminMenu.clearStartGateMarksConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.REPLACE_KIT, kitActionsAdminMenu.replaceConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.DELETE_KIT, kitActionsAdminMenu.deleteConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.REMOVE_TEAM, teamActionsAdminMenu.removeConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.CANCEL_TOURNAMENT, matchAdminMenu.cancelConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.RESTART_MATCH, matchAdminMenu.restartConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.FORM_RANDOM_TEAMS,
                registrationsAdminMenu.formConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.CLEAR_REGISTRATIONS,
                registrationsAdminMenu.clearConfirmation());
        confirmationAdminMenu.register(ConfirmationRequest.REVOKE_RESULT,
                decidedMatchesAdminMenu.revokeConfirmation());
        confirmationAdminMenu.requireEveryConfirmationRegistered();
        navigator.register(AdminMenuType.MAIN, mainAdminMenu);
        navigator.register(AdminMenuType.ARENA, arenaAdminMenu);
        navigator.register(AdminMenuType.KIT, kitAdminMenu);
        navigator.register(AdminMenuType.KIT_ACTIONS, kitActionsAdminMenu);
        navigator.register(AdminMenuType.KIT_CHOICE, kitChoiceAdminMenu);
        navigator.register(AdminMenuType.TEAMS, teamsAdminMenu);
        navigator.register(AdminMenuType.TEAM_ACTIONS, teamActionsAdminMenu);
        navigator.register(AdminMenuType.TEAM_MEMBERS, teamMembersAdminMenu);
        navigator.register(AdminMenuType.REGISTRATIONS, registrationsAdminMenu);
        navigator.register(AdminMenuType.MATCH, matchAdminMenu);
        navigator.register(AdminMenuType.FORCE_RESULT, forceResultAdminMenu);
        navigator.register(AdminMenuType.WAITING_MATCHES, waitingMatchesAdminMenu);
        navigator.register(AdminMenuType.WAITING_MATCH_RESULT, waitingMatchResultAdminMenu);
        navigator.register(AdminMenuType.DECIDED_MATCHES, decidedMatchesAdminMenu);
        navigator.register(AdminMenuType.CONFIRMATION, confirmationAdminMenu);
        navigator.register(AdminMenuType.BRACKET_VIEW, bracketViewMenu);
        navigator.register(AdminMenuType.UNEQUAL_TEAM_VOTE, unequalTeamVoteMenu);
        navigator.register(AdminMenuType.TEAM_CHOICE, teamChoiceMenu);
        navigator.requireEveryMenuRegistered();
    }

    public void openMainMenu(Player player) {
        navigator.open(player, AdminMenuHolder.main());
    }

    public void openBracketView(Player player) {
        bracketViewMenu.open(player);
    }

    public void openUnequalTeamVote(Player player) {
        Optional<PendingUnequalTeamVote> vote = unequalTeamVoteService.getPending()
                .filter(pendingVote -> pendingVote.isVoter(player.getUniqueId()));
        if (vote.isEmpty()) {
            TournamentMessages.sendError(player, Messages.text(UnequalTeamVoteMessage.NOT_VOTING));
            return;
        }
        unequalTeamVoteMenuPresenter.open(player, vote.get());
    }

    public void openTeamChoice(Player player) {
        Optional<String> rejection = teamChoiceService.findAccessRejection(player.getUniqueId());
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        teamChoiceMenuPresenter.open(player);
    }

    public boolean mayChooseTeam(Player player) {
        return teamChoiceService.findAccessRejection(player.getUniqueId()).isEmpty();
    }

    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        handleClick(player, holder, slot, false);
    }

    public void handleClick(Player player, AdminMenuHolder holder, int slot, boolean shiftClick) {
        boolean memberOfTheVotingTeam = unequalTeamVoteService.isVoter(player.getUniqueId());
        if (!AdminMenuReplies.mayStillUse(player, holder.getMenuType(), memberOfTheVotingTeam,
                () -> teamChoiceService.findAccessRejection(player.getUniqueId()))) {
            navigator.close(player);
            return;
        }
        navigator.menuOf(holder.getMenuType()).handleClick(player, holder, slot, shiftClick);
    }
}
