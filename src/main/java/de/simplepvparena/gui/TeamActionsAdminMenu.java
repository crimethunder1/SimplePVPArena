package de.simplepvparena.gui;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamRegistry;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class TeamActionsAdminMenu implements AdminMenu {

    private static final int TEAM_REMOVE_SLOT = 13;

    private final AdminMenuNavigation navigation;
    private final TeamRegistry teamRegistry;
    private final Runnable teamConfigurationSaver;

    TeamActionsAdminMenu(AdminMenuNavigation navigation, TeamRegistry teamRegistry,
                         Runnable teamConfigurationSaver) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
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
                .map(registeredTeam -> List.of("Members: " + registeredTeam.getMemberNamesJoined()))
                .orElse(List.of("This team is not registered any more"))));
        canvas.set(TEAM_REMOVE_SLOT, MenuItems.create(MenuItems.REFUSE_MATERIAL, "Remove the team",
                "Removes " + teamName + " from the registry",
                "Asks for a confirmation first"));
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back("the team list"));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        String teamName = holder.getSubject().orElseThrow();
        switch (slot) {
            case TEAM_REMOVE_SLOT -> navigation.open(player,
                    AdminMenuHolder.confirmation(ConfirmationRequest.REMOVE_TEAM, teamName, holder.getPageIndex()));
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
                        "A running match of that team is not stopped by it");
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

    private void removeTeam(Player player, String teamName) {
        if (!teamRegistry.removeTeam(teamName)) {
            TournamentMessages.sendError(player, "Unknown team " + teamName);
            return;
        }
        teamConfigurationSaver.run();
        TournamentMessages.sendSuccess(player, "Team " + teamName + " removed");
    }
}
