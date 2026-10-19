package de.simplepvparena.gui;

import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TeamActionsMessage;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class TeamActionsAdminMenu implements AdminMenu {

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
        return Messages.text(TeamActionsMessage.TITLE, Placeholder.of("team", holder.getSubject().orElseThrow()));
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
                .map(registeredTeam -> List.of(
                        Messages.text(TeamActionsMessage.INFO_MEMBERS,
                                Placeholder.of("members", registeredTeam.getMemberNamesJoined())),
                        Messages.plural(TeamActionsMessage.INFO_PLAYER_COUNT, registeredTeam.getMembers().size(),
                                Placeholder.of("teamSize", teamSizeSetting.getTeamSize()))))
                .orElse(List.of(Messages.text(TeamActionsMessage.INFO_NOT_REGISTERED)))));
        String renameTitle = Messages.text(TeamActionsMessage.RENAME_TITLE);
        canvas.set(TEAM_RENAME_SLOT, findRenameRejection(teamName)
                .map(rejection -> MenuItems.unavailable(renameTitle, rejection))
                .orElseGet(() -> MenuItems.create(Material.NAME_TAG, renameTitle,
                        withMenuClosingLine(Messages.lines(TeamActionsMessage.RENAME_LORE)))));
        String addPlayerTitle = Messages.text(TeamActionsMessage.ADD_PLAYER_TITLE);
        canvas.set(TEAM_ADD_PLAYER_SLOT, findAddPlayerTileRejection(teamName)
                .map(rejection -> MenuItems.unavailable(addPlayerTitle, rejection))
                .orElseGet(() -> MenuItems.create(Material.LIME_DYE, addPlayerTitle,
                        withMenuClosingLine(Messages.lines(TeamActionsMessage.ADD_PLAYER_LORE)))));
        String removePlayerTitle = Messages.text(TeamActionsMessage.REMOVE_PLAYER_TITLE);
        canvas.set(TEAM_REMOVE_PLAYER_SLOT, findRemovePlayerRejection(teamName)
                .map(rejection -> MenuItems.unavailable(removePlayerTitle, rejection))
                .orElseGet(() -> MenuItems.create(Material.RED_DYE, removePlayerTitle,
                        Messages.lines(TeamActionsMessage.REMOVE_PLAYER_LORE, Placeholder.of("team", teamName)))));
        String removeTitle = Messages.text(TeamActionsMessage.REMOVE_TITLE);
        canvas.set(TEAM_REMOVE_SLOT, tournamentManager.findTeamChangeRejection(teamName)
                .map(rejection -> MenuItems.unavailable(removeTitle, rejection))
                .orElseGet(() -> MenuItems.create(MenuItems.REFUSE_MATERIAL, removeTitle,
                        Messages.text(TeamActionsMessage.REMOVE_LORE, Placeholder.of("team", teamName)),
                        Messages.text(CommonMessage.ASKS_FOR_CONFIRMATION))));
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_TEAM_LIST));
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
                return Messages.lines(TeamActionsMessage.REMOVE_CONFIRMATION,
                        Placeholder.of("team", holder.getSubject().orElseThrow()));
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

    static String describeLastPlayerRejection() {
        return AdminMenuReplies.startWithCapitalLetter(TeamRegistry.describeLastMemberRejection());
    }

    private Optional<String> findRenameRejection(String teamName) {
        return findUnknownTeamRejection(teamName).or(() -> tournamentManager.findTeamRenameRejection(teamName));
    }

    private Optional<String> findChangeRejection(String teamName) {
        return findUnknownTeamRejection(teamName).or(() -> tournamentManager.findTeamChangeRejection(teamName));
    }

    private Optional<String> findAddPlayerTileRejection(String teamName) {
        return findChangeRejection(teamName).or(() -> findFullTeam(teamName)
                .map(team -> Messages.plural(TeamActionsMessage.ADD_PLAYER_TEAM_FULL, team.getMembers().size(),
                        Placeholder.of("teamSize", teamSizeSetting.getTeamSize()))));
    }

    private Optional<String> findAddPlayerRejection(String teamName) {
        return findChangeRejection(teamName).or(() -> findFullTeam(teamName)
                .map(team -> Messages.plural(TeamActionsMessage.TEAM_FULL, team.getMembers().size(),
                        Placeholder.of("team", teamName),
                        Placeholder.of("teamSize", teamSizeSetting.getTeamSize()))));
    }

    private Optional<Team> findFullTeam(String teamName) {
        return teamRegistry.findTeam(teamName)
                .filter(team -> team.getMembers().size() >= teamSizeSetting.getTeamSize());
    }

    private Optional<String> findRemovePlayerRejection(String teamName) {
        return findChangeRejection(teamName).or(() -> teamRegistry.findTeam(teamName)
                .filter(team -> team.getMembers().size() <= Team.MINIMUM_MEMBER_COUNT)
                .map(team -> describeLastPlayerRejection()));
    }

    private Optional<String> findUnknownTeamRejection(String teamName) {
        if (teamRegistry.findTeam(teamName).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(Messages.text(TeamActionsMessage.NOT_REGISTERED));
    }

    private static List<String> withMenuClosingLine(List<String> loreLines) {
        List<String> lines = new ArrayList<>(loreLines);
        lines.add(Messages.text(CommonMessage.MENU_CLOSES_TO_TYPE));
        return lines;
    }

    private void askForNewName(Player player, String teamName, int pageIndex) {
        Optional<String> rejection = findRenameRejection(teamName);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        navigation.close(player);
        chatInputService.await(player,
                Messages.text(TeamActionsMessage.RENAME_INPUT_DESCRIPTION, Placeholder.of("team", teamName)),
                Messages.lines(TeamActionsMessage.RENAME_INPUT_PROMPT,
                        Placeholder.of("rule", TeamRegistry.describeNameRule())),
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
            TournamentMessages.sendSuccess(player, Messages.text(TeamActionsMessage.TEAM_RENAMED,
                    Placeholder.of("formerName", formerName), Placeholder.of("newName", renamedTeam.getTeamName())));
            navigation.open(player, AdminMenuHolder.teamActions(renamedTeam.getTeamName(), pageIndex));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            TournamentMessages.sendError(player, Messages.text(TeamActionsMessage.RENAME_FAILED,
                    Placeholder.of("team", formerName),
                    Placeholder.of("reason", AdminMenuReplies.startWithCapitalLetter(failure.getMessage()))));
            reopen(player, formerName, pageIndex);
        }
    }

    private void askForPlayerToAdd(Player player, String teamName, int pageIndex) {
        Optional<String> rejection = findAddPlayerRejection(teamName);
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        Team team = teamRegistry.findTeam(teamName).orElseThrow();
        navigation.close(player);
        List<String> promptLines = new ArrayList<>();
        promptLines.add(Messages.text(TeamActionsMessage.ADD_PLAYER_INPUT_TYPE));
        promptLines.add(Messages.plural(TeamActionsMessage.ADD_PLAYER_INPUT_PLAYER_COUNT,
                teamSizeSetting.getTeamSize(), Placeholder.of("team", team.getTeamName()),
                Placeholder.of("memberCount", team.getMembers().size())));
        promptLines.addAll(Messages.lines(TeamActionsMessage.ADD_PLAYER_INPUT_HINTS));
        chatInputService.await(player,
                Messages.text(TeamActionsMessage.ADD_PLAYER_INPUT_DESCRIPTION, Placeholder.of("team", teamName)),
                promptLines, line -> handlePlayerToAdd(player, teamName, pageIndex, line));
    }

    private void handlePlayerToAdd(Player player, String teamName, int pageIndex, String line) {
        if (!AdminMenuReplies.isStillAdmin(player)) {
            return;
        }
        Optional<String> rejection = findAddPlayerRejection(teamName);
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
            TournamentMessages.sendSuccess(player, Messages.text(TeamActionsMessage.PLAYER_JOINED,
                    Placeholder.of("player", member.get().playerName()),
                    Placeholder.of("team", changedTeam.getTeamName())));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            TournamentMessages.sendError(player, Messages.text(TeamActionsMessage.JOIN_FAILED,
                    Placeholder.of("player", member.get().playerName()), Placeholder.of("team", teamName),
                    Placeholder.of("reason", AdminMenuReplies.startWithCapitalLetter(failure.getMessage()))));
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
            TournamentMessages.sendError(player, Messages.text(TeamActionsMessage.UNKNOWN_TEAM,
                    Placeholder.of("team", teamName)));
            return;
        }
        teamConfigurationSaver.run();
        TournamentMessages.sendSuccess(player, Messages.text(TeamActionsMessage.TEAM_REMOVED,
                Placeholder.of("team", teamName)));
    }
}
