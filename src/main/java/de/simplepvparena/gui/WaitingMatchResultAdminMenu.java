package de.simplepvparena.gui;

import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.message.WaitingMatchMessage;
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
        return Messages.text(WaitingMatchMessage.RESULT_MENU_TITLE,
                Placeholder.of("match", holder.getSubject().orElseThrow()));
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
            canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info(Messages.text(WaitingMatchMessage.RESULT_INFO_TITLE),
                    List.of(describeMatchNotWaiting(matchId))));
            canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_WAITING_MATCHES));
            return;
        }
        BracketMatch match = waitingMatch.get();
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info(Messages.text(WaitingMatchMessage.RESULT_INFO_TITLE),
                List.of(Messages.text(WaitingMatchMessage.RESULT_INFO_MATCH, Placeholder.of("match", matchId),
                                Placeholder.of("firstTeam", match.getFirstTeamName().orElseThrow()),
                                Placeholder.of("secondTeam", match.getSecondTeamName().orElseThrow())),
                        WaitingMatchesAdminMenu.describeRound(match),
                        Messages.text(WaitingMatchMessage.RESULT_INFO_HINT))));
        canvas.set(WAITING_MATCH_RESULT_FIRST_TEAM_SLOT, buildWinnerItem(match.getFirstTeamName().orElseThrow()));
        canvas.set(WAITING_MATCH_RESULT_SECOND_TEAM_SLOT, buildWinnerItem(match.getSecondTeamName().orElseThrow()));
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_WAITING_MATCHES));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        if (slot == AdminMenuLayout.BACK_SLOT) {
            openWaitingMatches(player, holder);
            return;
        }
        Optional<BracketMatch> waitingMatch = findMatchOfMenu(holder);
        if (waitingMatch.isEmpty()) {
            TournamentMessages.sendWarning(player, describeMatchNotWaiting(holder.getSubject().orElseThrow()));
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
            lore.add(Messages.text(WaitingMatchMessage.RESULT_TEAM_NOT_REGISTERED));
        }
        lore.add(Messages.text(WaitingMatchMessage.RESULT_TEAM_LORE));
        return MenuItems.create(Material.PLAYER_HEAD, teamName, lore);
    }

    private static String describeMatchNotWaiting(String matchId) {
        return Messages.text(WaitingMatchMessage.RESULT_MATCH_NOT_WAITING, Placeholder.of("match", matchId));
    }

    private Optional<BracketMatch> findMatchOfMenu(AdminMenuHolder holder) {
        String matchId = holder.getSubject().orElseThrow();
        return tournamentManager.findWaitingMatches().stream()
                .filter(match -> match.getMatchId().equals(matchId))
                .findFirst();
    }
}
