package de.simplepvparena.gui;

import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class TeamActionsAdminMenu implements AdminMenu {

    static final String NOT_REGISTERED_REASON = "This team is not registered any more";
    static final String LAST_PLAYER_REASON = AdminMenuReplies.startWithCapitalLetter(
            TeamRegistry.LAST_MEMBER_REJECTION);

    private static final int TEAM_RENAME_SLOT = 10;
    private static final int TEAM_ADD_PLAYER_SLOT = 11;
    private static final int TEAM_REMOVE_PLAYER_SLOT = 12;
    private static final int TEAM_REMOVE_SLOT = 13;

    private final AdminMenuNavigation navigation;
    private final TeamRegistry teamRegistry;
    private final TeamMemberLookup teamMemberLookup;
    private final ChatInputService chatInputService;
    private final Runnable teamConfigurationSaver;
    private final TournamentManager tournamentManager;
    private final TeamSizeSetting teamSizeSetting;

    TeamActionsAdminMenu(AdminMenuNavigation navigation, TeamRegistry teamRegistry,
                         TeamMemberLookup teamMemberLookup, ChatInputService chatInputService,
                         Runnable teamConfigurationSaver, TournamentManager tournamentManager) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.teamMemberLookup = Objects.requireNonNull(teamMemberLookup, "teamMemberLookup");
        this.chatInputService = Objects.requireNonNull(chatInputService, "chatInputService");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.teamSizeSetting = tournamentManager.getTeamSizeSetting();
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return "Team " + holder.getSubject().orElseThrow();
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        String teamName = holder.getSubject().orElseThrow();
        Optional<Team> team = teamRegistry.findTeam(teamName);
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info(teamName, team
                .map(registeredTeam -> List.of("Members: " + registeredTeam.getMemberNamesJoined(),
                        describePlayerCount(registeredTeam, teamSizeSetting.getTeamSize())))
                .orElse(List.of(NOT_REGISTERED_REASON))));
        canvas.set(TEAM_RENAME_SLOT, findRenameRejection(teamName)
                .map(rejection -> MenuItems.unavailable("Rename the team", rejection))
                .orElseGet(() -> MenuItems.create(Material.NAME_TAG, "Rename the team",
                        "Asks for the new name in the chat",
                        "The team keeps its place in the team list",
                        "The menu closes so you can type")));
        canvas.set(TEAM_ADD_PLAYER_SLOT, findAddPlayerRejection(teamName, "The team")
                .map(rejection -> MenuItems.unavailable("Add a player", rejection))
                .orElseGet(() -> MenuItems.create(Material.LIME_DYE, "Add a player",
                        "Asks for the name of the player in the chat",
                        "The menu closes so you can type")));
        canvas.set(TEAM_REMOVE_PLAYER_SLOT, findRemovePlayerRejection(teamName)
                .map(rejection -> MenuItems.unavailable("Remove a player", rejection))
                .orElseGet(() -> MenuItems.create(Material.RED_DYE, "Remove a player",
                        "Shows the players of " + teamName,
                        "A click on a player removes the player")));
        canvas.set(TEAM_REMOVE_SLOT, tournamentManager.findTeamChangeRejection(teamName)
                .map(rejection -> MenuItems.unavailable("Remove the team", rejection))
                .orElseGet(() -> MenuItems.create(MenuItems.REFUSE_MATERIAL, "Remove the team",
                        "Removes " + teamName + " from the registry",
                        "Asks for a confirmation first")));
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back("the team list"));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        String teamName = holder.getSubject().orElseThrow();
        switch (slot) {
            case TEAM_RENAME_SLOT -> askForNewName(player, teamName, holder.getPageIndex());
            case TEAM_ADD_PLAYER_SLOT -> askForPlayerToAdd(player, teamName, holder.getPageIndex());
            case TEAM_REMOVE_PLAYER_SLOT -> openTeamMembers(player, teamName, holder.getPageIndex());
            case TEAM_REMOVE_SLOT -> askForRemovalConfirmation(player, teamName, holder.getPageIndex());
            case AdminMenuLayout.BACK_SLOT -> navigation.open(player, AdminMenuHolder.teams(holder.getPageIndex()));
            default -> { }
        }
    }

    AdminMenuConfirmation removeConfirmation() {
        return new AdminMenuConfirmation() {
            @Override
            public List<String> describe(AdminMenuHolder holder) {
                return List.of(
                        "Team " + holder.getSubject().orElseThrow() + " is removed from the registry",
                        "A team that plays a match cannot be removed");
            }

            @Override
            public void carryOut(Player player, AdminMenuHolder holder) {
                removeTeam(player, holder.getSubject().orElseThrow());
            }

            @Override
            public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
                return AdminMenuHolder.teams(holder.getPageIndex());
            }
        };
    }

    static String describePlayerCount(Team team, int teamSize) {
        return "Players: " + team.getMembers().size() + " of " + teamSize;
    }

    private Optional<String> findRenameRejection(String teamName) {
        return findUnknownTeamRejection(teamName).or(() -> tournamentManager.findTeamRenameRejection(teamName));
    }

    private Optional<String> findChangeRejection(String teamName) {
        return findUnknownTeamRejection(teamName).or(() -> tournamentManager.findTeamChangeRejection(teamName));
    }

    private Optional<String> findAddPlayerRejection(String teamName, String teamDescription) {
        return findChangeRejection(teamName).or(() -> teamRegistry.findTeam(teamName)
                .filter(team -> team.getMembers().size() >= teamSizeSetting.getTeamSize())
                .map(team -> teamDescription + " already has " + describeMembers(team.getMembers().size())
                        + ", the team size is " + teamSizeSetting.getTeamSize()));
    }

    private Optional<String> findRemovePlayerRejection(String teamName) {
        return findChangeRejection(teamName).or(() -> teamRegistry.findTeam(teamName)
                .filter(team -> team.getMembers().size() <= Team.MINIMUM_MEMBER_COUNT)
                .map(team -> LAST_PLAYER_REASON));
    }

    private Optional<String> findUnknownTeamRejection(String teamName) {
        if (teamRegistry.findTeam(teamName).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(NOT_REGISTERED_REASON);
    }

    private static String describeMembers(int memberCount) {
        return memberCount + (memberCount == 1 ? " player" : " players");
    }

    private void askForNewName(Player player, String teamName, int pageIndex) {
        Optional<String> rejection = findRenameRejection(teamName);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        navigation.close(player);
        chatInputService.await(player, "the new name of team " + teamName, List.of(
                        "Type: <newName> in chat",
                        "A team name has " + TeamRegistry.NAME_RULE,
                        "Example: Phoenix",
                        "The team keeps its players and its place in the team list"),
                line -> handleNewName(player, teamName, pageIndex, line));
    }

    private void handleNewName(Player player, String teamName, int pageIndex, String line) {
        if (!AdminMenuReplies.isStillAdmin(player)) {
            return;
        }
        Optional<String> rejection = findRenameRejection(teamName);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            reopen(player, teamName, pageIndex);
            return;
        }
        String formerName = teamRegistry.findTeam(teamName).orElseThrow().getTeamName();
        try {
            Team renamedTeam = teamRegistry.renameTeam(formerName, line.trim());
            teamConfigurationSaver.run();
            TournamentMessages.sendSuccess(player, "Team " + formerName + " is now called "
                    + renamedTeam.getTeamName());
            navigation.open(player, AdminMenuHolder.teamActions(renamedTeam.getTeamName(), pageIndex));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            TournamentMessages.sendError(player, "Team " + formerName + " could not be renamed"
                    + TournamentMessages.LINE_BREAK + AdminMenuReplies.startWithCapitalLetter(failure.getMessage()));
            reopen(player, formerName, pageIndex);
        }
    }

    private void askForPlayerToAdd(Player player, String teamName, int pageIndex) {
        Optional<String> rejection = findAddPlayerRejection(teamName, "Team " + teamName);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        Team team = teamRegistry.findTeam(teamName).orElseThrow();
        navigation.close(player);
        chatInputService.await(player, "the player to add to team " + teamName, List.of(
                        "Type: <player> in chat",
                        "Team " + team.getTeamName() + " has " + team.getMembers().size() + " of "
                                + teamSizeSetting.getTeamSize() + " players",
                        "Example: Steve",
                        "Every player has to be online or known to the server"),
                line -> handlePlayerToAdd(player, teamName, pageIndex, line));
    }

    private void handlePlayerToAdd(Player player, String teamName, int pageIndex, String line) {
        if (!AdminMenuReplies.isStillAdmin(player)) {
            return;
        }
        Optional<String> rejection = findAddPlayerRejection(teamName, "Team " + teamName);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            reopen(player, teamName, pageIndex);
            return;
        }
        String playerName = line.trim();
        Optional<TeamMember> member = teamMemberLookup.find(playerName);
        if (member.isEmpty()) {
            TournamentMessages.sendError(player, AdminMenuReplies.describeUnknownPlayer(playerName));
            reopen(player, teamName, pageIndex);
            return;
        }
        try {
            Team changedTeam = teamRegistry.addMember(teamName, member.get());
            teamConfigurationSaver.run();
            TournamentMessages.sendSuccess(player, member.get().playerName() + " joined team "
                    + changedTeam.getTeamName());
        } catch (IllegalArgumentException | IllegalStateException failure) {
            TournamentMessages.sendError(player, member.get().playerName() + " could not join team " + teamName
                    + TournamentMessages.LINE_BREAK + AdminMenuReplies.startWithCapitalLetter(failure.getMessage()));
        }
        reopen(player, teamName, pageIndex);
    }

    private void openTeamMembers(Player player, String teamName, int pageIndex) {
        Optional<String> rejection = findRemovePlayerRejection(teamName);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        navigation.open(player, AdminMenuHolder.teamMembers(teamName, pageIndex));
    }

    private void askForRemovalConfirmation(Player player, String teamName, int pageIndex) {
        Optional<String> rejection = tournamentManager.findTeamChangeRejection(teamName);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        navigation.open(player, AdminMenuHolder.confirmation(ConfirmationRequest.REMOVE_TEAM, teamName, pageIndex));
    }

    private void reopen(Player player, String teamName, int pageIndex) {
        if (teamRegistry.findTeam(teamName).isEmpty()) {
            navigation.open(player, AdminMenuHolder.teams(pageIndex));
            return;
        }
        navigation.open(player, AdminMenuHolder.teamActions(teamName, pageIndex));
    }

    private void removeTeam(Player player, String teamName) {
        Optional<String> rejection = tournamentManager.findTeamChangeRejection(teamName);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        if (!teamRegistry.removeTeam(teamName)) {
            TournamentMessages.sendError(player, "Unknown team " + teamName);
            return;
        }
        teamConfigurationSaver.run();
        TournamentMessages.sendSuccess(player, "Team " + teamName + " removed");
    }
}
