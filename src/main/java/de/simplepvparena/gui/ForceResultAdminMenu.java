package de.simplepvparena.gui;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class ForceResultAdminMenu implements AdminMenu {

    private static final int FORCE_RESULT_FIRST_TEAM_SLOT = 11;
    private static final int FORCE_RESULT_SECOND_TEAM_SLOT = 15;

    private final AdminMenuNavigation navigation;
    private final TournamentManager tournamentManager;
    private final MatchService matchService;

    ForceResultAdminMenu(AdminMenuNavigation navigation, TournamentManager tournamentManager,
                         MatchService matchService) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.matchService = Objects.requireNonNull(matchService, "matchService");
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
        Optional<Match> activeMatch = matchService.findMatchById(matchId);
        if (activeMatch.isEmpty()) {
            canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info("Force a result",
                    List.of("Match " + matchId + " is not running any more")));
            canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_MATCH_MENU));
            return;
        }
        Match match = activeMatch.get();
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info("Force a result", List.of(
                "Match " + match.describe(),
                "Round " + match.getRoundNumber(),
                "A click decides the match for that team")));
        canvas.set(FORCE_RESULT_FIRST_TEAM_SLOT, buildWinnerItem(match.getFirstTeam()));
        canvas.set(FORCE_RESULT_SECOND_TEAM_SLOT, buildWinnerItem(match.getSecondTeam()));
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_MATCH_MENU));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        if (slot == AdminMenuLayout.BACK_SLOT) {
            navigation.open(player, AdminMenuHolder.match());
            return;
        }
        Optional<Match> activeMatch = findMatchOfMenu(holder);
        if (activeMatch.isEmpty()) {
            TournamentMessages.sendWarning(player,
                    "Match " + holder.getSubject().orElseThrow() + " is not running any more");
            navigation.open(player, AdminMenuHolder.match());
            return;
        }
        Optional<Team> winningTeam = findWinningTeam(activeMatch.get(), slot);
        if (winningTeam.isEmpty()) {
            return;
        }
        AdminMenuReplies.reportOperation(player, tournamentManager.forceMatchResult(activeMatch.get().getMatchId(),
                winningTeam.get().getTeamName()));
        navigation.open(player, AdminMenuHolder.match());
    }

    void openForceResultMenu(Player player) {
        Optional<Match> activeMatch = matchService.getActiveMatch();
        if (activeMatch.isEmpty()) {
            TournamentMessages.sendError(player, "No match is running");
            return;
        }
        navigation.open(player, AdminMenuHolder.forceResult(activeMatch.get().getMatchId()));
    }

    private Optional<Team> findWinningTeam(Match match, int slot) {
        return switch (slot) {
            case FORCE_RESULT_FIRST_TEAM_SLOT -> Optional.of(match.getFirstTeam());
            case FORCE_RESULT_SECOND_TEAM_SLOT -> Optional.of(match.getSecondTeam());
            default -> Optional.empty();
        };
    }

    private MenuTile buildWinnerItem(Team team) {
        List<String> lore = new ArrayList<>();
        for (TeamMember member : team.getMembers()) {
            lore.add(member.playerName());
        }
        lore.add("Decides the match for this team");
        return MenuItems.create(Material.PLAYER_HEAD, team.getTeamName(), lore);
    }

    private Optional<Match> findMatchOfMenu(AdminMenuHolder holder) {
        return matchService.findMatchById(holder.getSubject().orElseThrow());
    }
}
