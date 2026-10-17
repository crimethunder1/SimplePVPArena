package de.simplepvparena.gui;

import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.EventTeamImportFile;
import de.simplepvparena.team.EventTeamImportResult;
import de.simplepvparena.team.EventTeamImportService;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamCreationRequest;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;

final class TeamsAdminMenu implements AdminMenu {

    private static final int TEAM_MENU_SIZE = 54;

    private static final int TEAMS_PREVIOUS_PAGE_SLOT = 45;
    private static final int TEAMS_INFO_SLOT = 47;
    private static final int TEAMS_BACK_SLOT = 49;
    private static final int TEAMS_IMPORT_SLOT = 50;
    private static final int TEAMS_ADD_SLOT = 51;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;

    private final AdminMenuNavigation navigation;
    private final TeamRegistry teamRegistry;
    private final TeamMemberLookup teamMemberLookup;
    private final EventTeamImportService eventTeamImportService;
    private final BooleanSupplier matchRunning;
    private final ChatInputService chatInputService;
    private final Runnable teamConfigurationSaver;

    TeamsAdminMenu(AdminMenuNavigation navigation, TeamRegistry teamRegistry, TeamMemberLookup teamMemberLookup,
                   EventTeamImportService eventTeamImportService, BooleanSupplier matchRunning,
                   ChatInputService chatInputService, Runnable teamConfigurationSaver) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.teamMemberLookup = Objects.requireNonNull(teamMemberLookup, "teamMemberLookup");
        this.eventTeamImportService = Objects.requireNonNull(eventTeamImportService, "eventTeamImportService");
        this.matchRunning = Objects.requireNonNull(matchRunning, "matchRunning");
        this.chatInputService = Objects.requireNonNull(chatInputService, "chatInputService");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return "Teams (page "
                + MenuPagination.of(teamRegistry.getTeamCount(), holder.getPageIndex()).getPageNumber() + ")";
    }

    @Override
    public int size() {
        return TEAM_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        List<Team> teams = collectTeams();
        MenuPagination pagination = MenuPagination.of(teams.size(), holder.getPageIndex());
        for (int slot = 0; slot < pagination.getItemCountOnPage(); slot++) {
            Team team = teams.get(pagination.getFirstItemIndex() + slot);
            canvas.set(slot, MenuItems.create(Material.PLAYER_HEAD, team.getTeamName(),
                    "Members: " + team.getMemberNamesJoined(), "Click for the options of this team"));
        }
        canvas.set(TEAMS_INFO_SLOT, MenuItems.info("Teams", List.of(
                "Registered teams: " + teams.size(),
                "Page " + pagination.getPageNumber() + " of " + pagination.getPageCount(),
                "A click on a team opens its options")));
        canvas.set(TEAMS_ADD_SLOT, MenuItems.create(MenuItems.CONFIRM_MATERIAL, "Add a team",
                "Asks for " + TeamCreationRequest.USAGE + " in the chat",
                "Two player names cannot be typed into an inventory",
                "The menu closes so you can type"));
        canvas.set(TEAMS_IMPORT_SLOT, MenuItems.create(Material.WRITABLE_BOOK,
                "Load the teams from " + EventTeamImportFile.FILE_NAME, buildTeamImportLore(teams.size(),
                        "Asks for a confirmation first")));
        if (pagination.hasPreviousPage()) {
            canvas.set(TEAMS_PREVIOUS_PAGE_SLOT, MenuItems.create(MenuItems.PAGE_MATERIAL, "Previous page",
                    "Page " + (pagination.getPreviousPageIndex() + 1) + " of " + pagination.getPageCount()));
        }
        if (pagination.hasNextPage()) {
            canvas.set(TEAMS_NEXT_PAGE_SLOT, MenuItems.create(MenuItems.PAGE_MATERIAL, "Next page",
                    "Page " + (pagination.getNextPageIndex() + 1) + " of " + pagination.getPageCount()));
        }
        canvas.set(TEAMS_BACK_SLOT, MenuItems.back("the main menu"));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        List<Team> teams = collectTeams();
        MenuPagination pagination = MenuPagination.of(teams.size(), holder.getPageIndex());
        switch (slot) {
            case TEAMS_PREVIOUS_PAGE_SLOT -> {
                if (pagination.hasPreviousPage()) {
                    navigation.open(player, AdminMenuHolder.teams(pagination.getPreviousPageIndex()));
                }
            }
            case TEAMS_NEXT_PAGE_SLOT -> {
                if (pagination.hasNextPage()) {
                    navigation.open(player, AdminMenuHolder.teams(pagination.getNextPageIndex()));
                }
            }
            case TEAMS_ADD_SLOT -> askForTeamLine(player, pagination.pageIndex());
            case TEAMS_IMPORT_SLOT -> askForTeamImportConfirmation(player, pagination.pageIndex());
            case TEAMS_INFO_SLOT -> { }
            case TEAMS_BACK_SLOT -> navigation.open(player, AdminMenuHolder.main());
            default -> openTeamActions(player, teams, pagination, slot);
        }
    }

    AdminMenuConfirmation importConfirmation() {
        return new AdminMenuConfirmation() {
            @Override
            public List<String> describe(AdminMenuHolder holder) {
                return describeImportConfirmation();
            }

            @Override
            public void carryOut(Player player, AdminMenuHolder holder) {
                importTeams(player);
            }

            @Override
            public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
                return AdminMenuHolder.teams(holder.getPageIndex());
            }
        };
    }

    private List<String> describeImportConfirmation() {
        return buildTeamImportLore(teamRegistry.getTeamCount(),
                "The teams that are registered now are gone afterwards",
                "Nothing is removed if not a single entry can be imported");
    }

    private void importTeams(Player player) {
        EventTeamImportResult result = eventTeamImportService.importTeams();
        if (result.isImported()) {
            TournamentMessages.sendSuccessReport(player, result.describe());
            return;
        }
        TournamentMessages.sendErrorReport(player, result.describe());
    }

    private void openTeamActions(Player player, List<Team> teams, MenuPagination pagination, int slot) {
        OptionalInt teamIndex = pagination.findItemIndex(slot);
        if (teamIndex.isEmpty()) {
            return;
        }
        navigation.open(player, AdminMenuHolder.teamActions(teams.get(teamIndex.getAsInt()).getTeamName(),
                pagination.pageIndex()));
    }

    private void askForTeamImportConfirmation(Player player, int pageIndex) {
        if (!eventTeamImportService.isImportFileAvailable()) {
            TournamentMessages.sendError(player, eventTeamImportService.describeMissingImportFile());
            return;
        }
        navigation.open(player, AdminMenuHolder.confirmation(ConfirmationRequest.IMPORT_TEAMS, null, pageIndex));
    }

    private List<String> buildTeamImportLore(int registeredTeamCount, String... closingLines) {
        List<String> lore = new ArrayList<>();
        lore.add("Reads " + EventTeamImportFile.FILE_NAME + " of the plugin folder");
        lore.add("Every one of the " + registeredTeamCount + " registered teams is replaced by it");
        lore.add("An entry that cannot be used is skipped and named,");
        lore.add("the teams around it are still imported");
        lore.addAll(eventTeamImportService.describeImportFile());
        if (matchRunning.getAsBoolean()) {
            lore.add("A match is running: a team that is gone afterwards");
            lore.add("hands its opponent a walkover, like every removal");
        }
        lore.addAll(List.of(closingLines));
        return lore;
    }

    private void askForTeamLine(Player player, int pageIndex) {
        navigation.close(player);
        chatInputService.await(player, "the team to add", List.of(
                        "Type: " + TeamCreationRequest.USAGE + " in chat",
                        "Example: Alpha Steve Alex",
                        "Both players have to be online or known to the server"),
                line -> handleTeamLine(player, pageIndex, line));
    }

    private void handleTeamLine(Player player, int pageIndex, String line) {
        if (!AdminMenuReplies.isStillAdmin(player)) {
            return;
        }
        TeamCreationRequest request = TeamCreationRequest.parse(line);
        if (!request.isAccepted()) {
            TournamentMessages.sendError(player, request.getRejectionMessage().orElseThrow());
            navigation.open(player, AdminMenuHolder.teams(pageIndex));
            return;
        }
        registerTeam(player, request);
        navigation.open(player, AdminMenuHolder.teams(pageIndex));
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
            TournamentMessages.sendError(player, describeRegistrationFailure(request.getTeamName(), failure));
        }
    }

    private String describeUnknownPlayer(String playerName) {
        return "Unknown player " + playerName
                + TournamentMessages.LINE_BREAK + "The player has to be online or known to the server";
    }

    private String describeRegistrationFailure(String teamName, RuntimeException failure) {
        return "Team " + teamName + " could not be registered"
                + TournamentMessages.LINE_BREAK + startWithCapitalLetter(failure.getMessage());
    }

    private String startWithCapitalLetter(String text) {
        if (text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private List<Team> collectTeams() {
        return new ArrayList<>(teamRegistry.getTeams());
    }
}
