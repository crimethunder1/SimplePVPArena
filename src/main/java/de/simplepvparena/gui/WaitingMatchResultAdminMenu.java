package de.simplepvparena.gui;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.BracketMatch;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class WaitingMatchResultAdminMenu implements AdminMenu {

    private static final int WAITING_MATCH_RESULT_FIRST_TEAM_SLOT = 11;
    private static final int WAITING_MATCH_RESULT_SECOND_TEAM_SLOT = 15;

    private final AdminMenuNavigation navigation;
    private final TournamentManager tournamentManager;
    private final TeamRegistry teamRegistry;

    WaitingMatchResultAdminMenu(AdminMenuNavigation navigation, TournamentManager tournamentManager,
                                TeamRegistry teamRegistry) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return "Result of " + holder.getSubject().orElseThrow();
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        String matchId = holder.getSubject().orElseThrow();
        Optional<BracketMatch> waitingMatch = findMatchOfMenu(holder);
        if (waitingMatch.isEmpty()) {
            canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info("Decide a waiting match",
                    List.of("Match " + matchId + " is not waiting any more")));
            canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back("the waiting matches"));
            return;
        }
        BracketMatch match = waitingMatch.get();
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info("Decide a waiting match", List.of(
                "Match " + matchId + " (" + WaitingMatchesAdminMenu.describeTeams(match) + ")",
                WaitingMatchesAdminMenu.describeRound(match),
                "A click decides the match for that team")));
        canvas.set(WAITING_MATCH_RESULT_FIRST_TEAM_SLOT, buildWinnerItem(match.getFirstTeamName().orElseThrow()));
        canvas.set(WAITING_MATCH_RESULT_SECOND_TEAM_SLOT, buildWinnerItem(match.getSecondTeamName().orElseThrow()));
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back("the waiting matches"));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        if (slot == AdminMenuLayout.BACK_SLOT) {
            openWaitingMatches(player, holder);
            return;
        }
        Optional<BracketMatch> waitingMatch = findMatchOfMenu(holder);
        if (waitingMatch.isEmpty()) {
            TournamentMessages.sendWarning(player,
                    "Match " + holder.getSubject().orElseThrow() + " is not waiting any more");
            openWaitingMatches(player, holder);
            return;
        }
        Optional<String> winningTeamName = findWinningTeamName(waitingMatch.get(), slot);
        if (winningTeamName.isEmpty()) {
            return;
        }
        AdminMenuReplies.reportOperation(player, tournamentManager.forceMatchResult(
                waitingMatch.get().getMatchId(), winningTeamName.get()));
        openWaitingMatches(player, holder);
    }

    private void openWaitingMatches(Player player, AdminMenuHolder holder) {
        int pageIndex = MenuPagination.of(tournamentManager.findWaitingMatches().size(), holder.getPageIndex())
                .pageIndex();
        navigation.open(player, AdminMenuHolder.waitingMatches(pageIndex));
    }

    private Optional<String> findWinningTeamName(BracketMatch match, int slot) {
        return switch (slot) {
            case WAITING_MATCH_RESULT_FIRST_TEAM_SLOT -> match.getFirstTeamName();
            case WAITING_MATCH_RESULT_SECOND_TEAM_SLOT -> match.getSecondTeamName();
            default -> Optional.empty();
        };
    }

    private MenuTile buildWinnerItem(String teamName) {
        List<String> lore = new ArrayList<>();
        Optional<Team> team = teamRegistry.findTeam(teamName);
        if (team.isPresent()) {
            for (TeamMember member : team.get().getMembers()) {
                lore.add(member.playerName());
            }
        } else {
            lore.add("No longer registered");
        }
        lore.add("Decides the match for this team");
        return MenuItems.create(Material.PLAYER_HEAD, teamName, lore);
    }

    private Optional<BracketMatch> findMatchOfMenu(AdminMenuHolder holder) {
        String matchId = holder.getSubject().orElseThrow();
        return tournamentManager.findWaitingMatches().stream()
                .filter(match -> match.getMatchId().equals(matchId))
                .findFirst();
    }
}
