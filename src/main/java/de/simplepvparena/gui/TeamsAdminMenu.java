package de.simplepvparena.gui;

import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamCreationRequest;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import de.simplepvparena.team.TeamSizeWriter;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;

final class TeamsAdminMenu implements AdminMenu {

    private static final int TEAM_MENU_SIZE = 54;

    private static final int TEAMS_PREVIOUS_PAGE_SLOT = 45;
    private static final int TEAMS_SMALLER_SLOT = 46;
    private static final int TEAMS_INFO_SLOT = 47;
    private static final int TEAMS_LARGER_SLOT = 48;
    private static final int TEAMS_BACK_SLOT = 49;
    private static final int TEAMS_ADD_SLOT = 51;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;

    private static final String SMALLEST_TEAM_SIZE_REASON = "The smallest team size is " + Team.MINIMUM_MEMBER_COUNT;
    private static final String LARGEST_TEAM_SIZE_REASON = "The largest team size is " + Team.MAXIMUM_MEMBER_COUNT;

    private final AdminMenuNavigation navigation;
    private final TeamRegistry teamRegistry;
    private final TeamMemberLookup teamMemberLookup;
    private final ChatInputService chatInputService;
    private final Runnable teamConfigurationSaver;
    private final TeamSizeSetting teamSizeSetting;
    private final BooleanSupplier tournamentInProgress;
    private final TeamSizeWriter teamSizeWriter;

    TeamsAdminMenu(AdminMenuNavigation navigation, TeamRegistry teamRegistry, TeamMemberLookup teamMemberLookup,
                   ChatInputService chatInputService, Runnable teamConfigurationSaver,
                   TeamSizeSetting teamSizeSetting, BooleanSupplier tournamentInProgress,
                   TeamSizeWriter teamSizeWriter) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.teamMemberLookup = Objects.requireNonNull(teamMemberLookup, "teamMemberLookup");
        this.chatInputService = Objects.requireNonNull(chatInputService, "chatInputService");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
        this.teamSizeSetting = Objects.requireNonNull(teamSizeSetting, "teamSizeSetting");
        this.tournamentInProgress = Objects.requireNonNull(tournamentInProgress, "tournamentInProgress");
        this.teamSizeWriter = Objects.requireNonNull(teamSizeWriter, "teamSizeWriter");
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
        int teamSize = teamSizeSetting.getTeamSize();
        canvas.set(TEAMS_INFO_SLOT, MenuItems.info("Teams", List.of(
                "Registered teams: " + teams.size(),
                "Page " + pagination.getPageNumber() + " of " + pagination.getPageCount(),
                "Team size: " + TeamSizeSetting.describeMatchUp(teamSize),
                "A click on a team opens its options")));
        canvas.set(TEAMS_SMALLER_SLOT, buildSmallerTeamsItem(teamSize));
        canvas.set(TEAMS_LARGER_SLOT, buildLargerTeamsItem(teamSize));
        canvas.set(TEAMS_ADD_SLOT, MenuItems.create(MenuItems.CONFIRM_MATERIAL, "Add a team",
                "Asks for " + TeamCreationRequest.USAGE + " in the chat",
                "Player names cannot be typed into an inventory",
                "The menu closes so you can type"));
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
            case TEAMS_SMALLER_SLOT -> makeTeamsSmaller(player, holder);
            case TEAMS_LARGER_SLOT -> makeTeamsLarger(player, holder);
            case TEAMS_INFO_SLOT -> { }
            case TEAMS_BACK_SLOT -> navigation.open(player, AdminMenuHolder.main());
            default -> openTeamActions(player, teams, pagination, slot);
        }
    }

    private MenuTile buildSmallerTeamsItem(int teamSize) {
        if (teamSize <= Team.MINIMUM_MEMBER_COUNT) {
            return MenuItems.unavailable("Smaller teams", SMALLEST_TEAM_SIZE_REASON);
        }
        return MenuItems.create(Material.RED_DYE, "Smaller teams",
                "Click to make teams of " + describePlayerCount(teamSize - 1));
    }

    private MenuTile buildLargerTeamsItem(int teamSize) {
        if (teamSize >= Team.MAXIMUM_MEMBER_COUNT) {
            return MenuItems.unavailable("Larger teams", LARGEST_TEAM_SIZE_REASON);
        }
        return MenuItems.create(Material.LIME_DYE, "Larger teams",
                "Click to make teams of " + describePlayerCount(teamSize + 1));
    }

    private void makeTeamsSmaller(Player player, AdminMenuHolder holder) {
        int teamSize = teamSizeSetting.getTeamSize();
        if (teamSize <= Team.MINIMUM_MEMBER_COUNT) {
            TournamentMessages.sendError(player, SMALLEST_TEAM_SIZE_REASON);
            return;
        }
        changeTeamSize(player, holder, teamSize - 1);
    }

    private void makeTeamsLarger(Player player, AdminMenuHolder holder) {
        int teamSize = teamSizeSetting.getTeamSize();
        if (teamSize >= Team.MAXIMUM_MEMBER_COUNT) {
            TournamentMessages.sendError(player, LARGEST_TEAM_SIZE_REASON);
            return;
        }
        changeTeamSize(player, holder, teamSize + 1);
    }

    private void changeTeamSize(Player player, AdminMenuHolder holder, int newTeamSize) {
        Optional<String> rejection = TeamSizeSetting.findChangeRejection(teamSizeSetting.getTeamSize(),
                newTeamSize, tournamentInProgress.getAsBoolean(), teamRegistry);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        teamSizeSetting.setTeamSize(newTeamSize);
        String announcement = "Team size is now " + TeamSizeSetting.describeMatchUp(newTeamSize);
        try {
            teamSizeWriter.write(newTeamSize);
            TournamentMessages.sendSuccess(player, announcement);
        } catch (IOException failure) {
            TournamentMessages.sendWarning(player, announcement + ", but config.yml could not be written: "
                    + describeFailure(failure));
        }
        navigation.refresh(player, holder);
    }

    private static String describePlayerCount(int playerCount) {
        return playerCount + (playerCount == 1 ? " player" : " players");
    }

    private static String describeFailure(IOException failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    private void openTeamActions(Player player, List<Team> teams, MenuPagination pagination, int slot) {
        OptionalInt teamIndex = pagination.findItemIndex(slot);
        if (teamIndex.isEmpty()) {
            return;
        }
        navigation.open(player, AdminMenuHolder.teamActions(teams.get(teamIndex.getAsInt()).getTeamName(),
                pagination.pageIndex()));
    }

    private void askForTeamLine(Player player, int pageIndex) {
        navigation.close(player);
        chatInputService.await(player, "the team to add", List.of(
                        "Type: " + TeamCreationRequest.USAGE + " in chat",
                        "A team has " + TeamSizeSetting.describePlayerRange(teamSizeSetting.getTeamSize())
                                + ", a team name has " + TeamRegistry.NAME_RULE,
                        "Example: Alpha Steve Alex",
                        "Example: Bravo Notch Jeb_ Dinnerbone",
                        "Every player has to be online or known to the server"),
                line -> handleTeamLine(player, pageIndex, line));
    }

    private void handleTeamLine(Player player, int pageIndex, String line) {
        if (!AdminMenuReplies.isStillAdmin(player)) {
            return;
        }
        TeamCreationRequest request = TeamCreationRequest.parse(line, teamSizeSetting.getTeamSize());
        if (!request.isAccepted()) {
            TournamentMessages.sendError(player, request.getRejectionMessage().orElseThrow());
            navigation.open(player, AdminMenuHolder.teams(pageIndex));
            return;
        }
        registerTeam(player, request);
        navigation.open(player, AdminMenuHolder.teams(pageIndex));
    }

    private void registerTeam(Player player, TeamCreationRequest request) {
        List<TeamMember> members = new ArrayList<>();
        for (String playerName : request.getPlayerNames()) {
            Optional<TeamMember> member = teamMemberLookup.find(playerName);
            if (member.isEmpty()) {
                TournamentMessages.sendError(player, AdminMenuReplies.describeUnknownPlayer(playerName));
                return;
            }
            members.add(member.get());
        }
        try {
            Team team = teamRegistry.registerTeam(request.getTeamName(), members);
            teamConfigurationSaver.run();
            TournamentMessages.sendSuccess(player, "Team " + team.getTeamName() + " registered with "
                    + team.getMemberNamesJoined());
        } catch (IllegalArgumentException | IllegalStateException failure) {
            TournamentMessages.sendError(player, describeRegistrationFailure(request.getTeamName(), failure));
        }
    }

    private String describeRegistrationFailure(String teamName, RuntimeException failure) {
        return "Team " + teamName + " could not be registered"
                + TournamentMessages.LINE_BREAK + AdminMenuReplies.startWithCapitalLetter(failure.getMessage());
    }

    private List<Team> collectTeams() {
        return new ArrayList<>(teamRegistry.getTeams());
    }
}
