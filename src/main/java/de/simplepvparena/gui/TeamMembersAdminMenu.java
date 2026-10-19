package de.simplepvparena.gui;

import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TeamActionsMessage;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class TeamMembersAdminMenu implements AdminMenu {

    private static final int FIRST_MEMBER_SLOT = 9;

    private final AdminMenuNavigation navigation;
    private final TeamRegistry teamRegistry;
    private final Runnable teamConfigurationSaver;
    private final TournamentManager tournamentManager;
    private final TeamSizeSetting teamSizeSetting;

    TeamMembersAdminMenu(AdminMenuNavigation navigation, TeamRegistry teamRegistry, Runnable teamConfigurationSaver,
                         TournamentManager tournamentManager) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.teamSizeSetting = tournamentManager.getTeamSizeSetting();
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(TeamActionsMessage.MEMBERS_TITLE,
                Placeholder.of("team", holder.getSubject().orElseThrow()));
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
                        Messages.plural(TeamActionsMessage.MEMBERS_INFO_PLAYER_COUNT,
                                registeredTeam.getMembers().size(),
                                Placeholder.of("teamSize", teamSizeSetting.getTeamSize())),
                        Messages.text(TeamActionsMessage.MEMBERS_INFO_HINT)))
                .orElse(List.of(Messages.text(TeamActionsMessage.MEMBERS_INFO_NOT_REGISTERED)))));
        List<TeamMember> members = team.map(Team::getMembers).orElse(List.of());
        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            canvas.set(FIRST_MEMBER_SLOT + memberIndex, MenuItems.create(Material.PLAYER_HEAD,
                    members.get(memberIndex).playerName(), Messages.text(TeamActionsMessage.MEMBER_LORE)));
        }
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_TEAM_OPTIONS,
                Placeholder.of("team", teamName)));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        String teamName = holder.getSubject().orElseThrow();
        if (slot == AdminMenuLayout.BACK_SLOT) {
            navigation.open(player, AdminMenuHolder.teamActions(teamName, holder.getPageIndex()));
            return;
        }
        Optional<Team> team = teamRegistry.findTeam(teamName);
        int memberIndex = slot - FIRST_MEMBER_SLOT;
        if (team.isEmpty() || memberIndex < 0 || memberIndex >= team.get().getMembers().size()) {
            return;
        }
        removeMember(player, holder, team.get(), team.get().getMembers().get(memberIndex));
    }

    private void removeMember(Player player, AdminMenuHolder holder, Team team, TeamMember member) {
        Optional<String> rejection = tournamentManager.findTeamChangeRejection(team.getTeamName());
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            return;
        }
        if (team.getMembers().size() <= Team.MINIMUM_MEMBER_COUNT) {
            TournamentMessages.sendError(player, TeamActionsAdminMenu.describeLastPlayerRejection());
            return;
        }
        try {
            Team changedTeam = teamRegistry.removeMember(team.getTeamName(), member.uniqueId());
            teamConfigurationSaver.run();
            TournamentMessages.sendSuccess(player, Messages.text(TeamActionsMessage.PLAYER_LEFT,
                    Placeholder.of("player", member.playerName()), Placeholder.of("team", changedTeam.getTeamName())));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            TournamentMessages.sendError(player, Messages.text(TeamActionsMessage.LEAVE_FAILED,
                    Placeholder.of("player", member.playerName()), Placeholder.of("team", team.getTeamName()),
                    Placeholder.of("reason", AdminMenuReplies.startWithCapitalLetter(failure.getMessage()))));
            return;
        }
        navigation.open(player, AdminMenuHolder.teamMembers(holder.getSubject().orElseThrow(),
                holder.getPageIndex()));
    }
}
