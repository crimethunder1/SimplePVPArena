package de.simplepvparena.gui;

import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.FailureDescriptions;
import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TeamMenuMessage;
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
import java.util.function.IntSupplier;

final class TeamsAdminMenu implements AdminMenu {

    private static final int TEAM_MENU_SIZE = 54;

    private static final int TEAMS_PREVIOUS_PAGE_SLOT = 45;
    private static final int TEAMS_SMALLER_SLOT = 46;
    private static final int TEAMS_INFO_SLOT = 47;
    private static final int TEAMS_LARGER_SLOT = 48;
    private static final int TEAMS_BACK_SLOT = 49;
    private static final int TEAMS_ADD_SLOT = 51;
    private static final int TEAMS_REGISTRATIONS_SLOT = 52;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;

    private static final Material REGISTRATIONS_MATERIAL = Material.WRITABLE_BOOK;

    private final AdminMenuNavigation navigation;
    private final TeamRegistry teamRegistry;
    private final TeamMemberLookup teamMemberLookup;
    private final ChatInputService chatInputService;
    private final Runnable teamConfigurationSaver;
    private final TeamSizeSetting teamSizeSetting;
    private final BooleanSupplier tournamentInProgress;
    private final TeamSizeWriter teamSizeWriter;
    private final IntSupplier registeredPlayerCount;

    TeamsAdminMenu(AdminMenuNavigation navigation, TeamRegistry teamRegistry, TeamMemberLookup teamMemberLookup,
                   ChatInputService chatInputService, Runnable teamConfigurationSaver,
                   TeamSizeSetting teamSizeSetting, BooleanSupplier tournamentInProgress,
                   TeamSizeWriter teamSizeWriter, IntSupplier registeredPlayerCount) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.teamMemberLookup = Objects.requireNonNull(teamMemberLookup, "teamMemberLookup");
        this.chatInputService = Objects.requireNonNull(chatInputService, "chatInputService");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
        this.teamSizeSetting = Objects.requireNonNull(teamSizeSetting, "teamSizeSetting");
        this.tournamentInProgress = Objects.requireNonNull(tournamentInProgress, "tournamentInProgress");
        this.teamSizeWriter = Objects.requireNonNull(teamSizeWriter, "teamSizeWriter");
        this.registeredPlayerCount = Objects.requireNonNull(registeredPlayerCount, "registeredPlayerCount");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(TeamMenuMessage.TITLE, Placeholder.of("page",
                MenuPagination.of(teamRegistry.getTeamCount(), holder.getPageIndex()).getPageNumber()));
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
                    Messages.text(TeamMenuMessage.TEAM_MEMBERS, Placeholder.of("members", team.getMemberNamesJoined())),
                    Messages.text(TeamMenuMessage.TEAM_LORE)));
        }
        int teamSize = teamSizeSetting.getTeamSize();
        canvas.set(TEAMS_INFO_SLOT, MenuItems.info(Messages.text(TeamMenuMessage.INFO_TITLE), List.of(
                Messages.plural(TeamMenuMessage.REGISTERED_TEAMS, teams.size()),
                Messages.text(MenuMessage.PAGE_OF_PAGES, Placeholder.of("page", pagination.getPageNumber()),
                        Placeholder.of("pageCount", pagination.getPageCount())),
                Messages.text(TeamMenuMessage.TEAM_SIZE,
                        Placeholder.of("matchUp", TeamSizeSetting.describeMatchUp(teamSize))),
                Messages.text(TeamMenuMessage.INFO_HINT))));
        canvas.set(TEAMS_SMALLER_SLOT, buildSmallerTeamsItem(teamSize));
        canvas.set(TEAMS_LARGER_SLOT, buildLargerTeamsItem(teamSize));
        List<String> addLore = new ArrayList<>(Messages.lines(TeamMenuMessage.ADD_LORE,
                Placeholder.of("usage", TeamCreationRequest.describeUsage())));
        addLore.add(Messages.text(CommonMessage.MENU_CLOSES_TO_TYPE));
        canvas.set(TEAMS_ADD_SLOT, MenuItems.create(MenuItems.CONFIRM_MATERIAL,
                Messages.text(TeamMenuMessage.ADD_TITLE), addLore));
        canvas.set(TEAMS_REGISTRATIONS_SLOT, MenuItems.create(REGISTRATIONS_MATERIAL,
                Messages.text(TeamMenuMessage.REGISTRATIONS_TITLE),
                RegistrationsAdminMenu.describeRegisteredPlayers(registeredPlayerCount.getAsInt()),
                Messages.text(TeamMenuMessage.REGISTRATIONS_LORE)));
        if (pagination.hasPreviousPage()) {
            canvas.set(TEAMS_PREVIOUS_PAGE_SLOT, MenuItems.previousPage(pagination));
        }
        if (pagination.hasNextPage()) {
            canvas.set(TEAMS_NEXT_PAGE_SLOT, MenuItems.nextPage(pagination));
        }
        canvas.set(TEAMS_BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_MAIN_MENU));
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
            case TEAMS_REGISTRATIONS_SLOT -> navigation.open(player, AdminMenuHolder.registrations(0));
            case TEAMS_SMALLER_SLOT -> makeTeamsSmaller(player, holder);
            case TEAMS_LARGER_SLOT -> makeTeamsLarger(player, holder);
            case TEAMS_INFO_SLOT -> { }
            case TEAMS_BACK_SLOT -> navigation.open(player, AdminMenuHolder.main());
            default -> openTeamActions(player, teams, pagination, slot);
        }
    }

    private MenuTile buildSmallerTeamsItem(int teamSize) {
        if (teamSize <= Team.MINIMUM_MEMBER_COUNT) {
            return MenuItems.unavailable(Messages.text(TeamMenuMessage.SMALLER_TITLE), describeSmallestTeamSize());
        }
        return MenuItems.create(Material.RED_DYE, Messages.text(TeamMenuMessage.SMALLER_TITLE),
                Messages.plural(TeamMenuMessage.SMALLER_LORE, teamSize - 1));
    }

    private MenuTile buildLargerTeamsItem(int teamSize) {
        if (teamSize >= Team.MAXIMUM_MEMBER_COUNT) {
            return MenuItems.unavailable(Messages.text(TeamMenuMessage.LARGER_TITLE), describeLargestTeamSize());
        }
        return MenuItems.create(Material.LIME_DYE, Messages.text(TeamMenuMessage.LARGER_TITLE),
                Messages.plural(TeamMenuMessage.LARGER_LORE, teamSize + 1));
    }

    private void makeTeamsSmaller(Player player, AdminMenuHolder holder) {
        int teamSize = teamSizeSetting.getTeamSize();
        if (teamSize <= Team.MINIMUM_MEMBER_COUNT) {
            TournamentMessages.sendError(player, describeSmallestTeamSize());
            return;
        }
        changeTeamSize(player, holder, teamSize - 1);
    }

    private void makeTeamsLarger(Player player, AdminMenuHolder holder) {
        int teamSize = teamSizeSetting.getTeamSize();
        if (teamSize >= Team.MAXIMUM_MEMBER_COUNT) {
            TournamentMessages.sendError(player, describeLargestTeamSize());
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
        Placeholder matchUp = Placeholder.of("matchUp", TeamSizeSetting.describeMatchUp(newTeamSize));
        try {
            teamSizeWriter.write(newTeamSize);
            TournamentMessages.sendSuccess(player, Messages.text(TeamMenuMessage.TEAM_SIZE_CHANGED, matchUp));
        } catch (IOException failure) {
            TournamentMessages.sendWarning(player, Messages.text(TeamMenuMessage.TEAM_SIZE_NOT_WRITTEN, matchUp,
                    Placeholder.of("reason", FailureDescriptions.describe(failure))));
        }
        navigation.refresh(player, holder);
    }

    private static String describeSmallestTeamSize() {
        return Messages.text(TeamMenuMessage.SMALLEST_TEAM_SIZE, Placeholder.of("minimum", Team.MINIMUM_MEMBER_COUNT));
    }

    private static String describeLargestTeamSize() {
        return Messages.text(TeamMenuMessage.LARGEST_TEAM_SIZE, Placeholder.of("maximum", Team.MAXIMUM_MEMBER_COUNT));
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
        List<String> promptLines = new ArrayList<>();
        promptLines.add(Messages.text(TeamMenuMessage.ADD_INPUT_TYPE,
                Placeholder.of("usage", TeamCreationRequest.describeUsage())));
        promptLines.add(describeTeamRule(teamSizeSetting.getTeamSize()));
        promptLines.addAll(Messages.lines(TeamMenuMessage.ADD_INPUT_EXAMPLES));
        promptLines.add(Messages.text(TeamMenuMessage.ADD_INPUT_PLAYERS_KNOWN));
        chatInputService.await(player, Messages.text(TeamMenuMessage.ADD_INPUT_DESCRIPTION), promptLines,
                line -> handleTeamLine(player, pageIndex, line));
    }

    private static String describeTeamRule(int teamSize) {
        Placeholder nameRule = Placeholder.of("rule", TeamRegistry.describeNameRule());
        if (teamSize == Team.MINIMUM_MEMBER_COUNT) {
            return Messages.plural(TeamMenuMessage.ADD_INPUT_EXACT_SIZE, teamSize, nameRule);
        }
        return Messages.text(TeamMenuMessage.ADD_INPUT_SIZE_RANGE, Placeholder.of("minimum", Team.MINIMUM_MEMBER_COUNT),
                Placeholder.of("maximum", teamSize), nameRule);
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
            TournamentMessages.sendSuccess(player, Messages.text(TeamMenuMessage.TEAM_REGISTERED,
                    Placeholder.of("team", team.getTeamName()),
                    Placeholder.of("members", team.getMemberNamesJoined())));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            TournamentMessages.sendError(player, describeRegistrationFailure(request.getTeamName(), failure));
        }
    }

    private String describeRegistrationFailure(String teamName, RuntimeException failure) {
        return Messages.text(TeamMenuMessage.REGISTRATION_FAILED, Placeholder.of("team", teamName),
                Placeholder.of("reason", AdminMenuReplies.startWithCapitalLetter(failure.getMessage())));
    }

    private List<Team> collectTeams() {
        return new ArrayList<>(teamRegistry.getTeams());
    }
}
