package de.simplepvparena.gui;

import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.message.WaitingMatchMessage;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.BracketMatch;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

final class WaitingMatchesAdminMenu implements AdminMenu {

    private static final int WAITING_MATCHES_MENU_SIZE = 54;

    private static final int WAITING_MATCHES_PREVIOUS_PAGE_SLOT = 45;
    private static final int WAITING_MATCHES_INFO_SLOT = 47;
    private static final int WAITING_MATCHES_BACK_SLOT = 49;
    private static final int WAITING_MATCHES_NEXT_PAGE_SLOT = 53;

    private final AdminMenuNavigation navigation;
    private final TournamentManager tournamentManager;
    private final TeamRegistry teamRegistry;

    WaitingMatchesAdminMenu(AdminMenuNavigation navigation, TournamentManager tournamentManager,
                            TeamRegistry teamRegistry) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(WaitingMatchMessage.TITLE, Placeholder.of("page",
                MenuPagination.of(tournamentManager.findWaitingMatches().size(), holder.getPageIndex())
                        .getPageNumber()));
    }

    @Override
    public int size() {
        return WAITING_MATCHES_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        List<BracketMatch> waitingMatches = tournamentManager.findWaitingMatches();
        if (waitingMatches.isEmpty()) {
            canvas.set(WAITING_MATCHES_INFO_SLOT, MenuItems.info(Messages.text(WaitingMatchMessage.INFO_TITLE),
                    List.of(Messages.text(WaitingMatchMessage.NO_MATCH_WAITING))));
            canvas.set(WAITING_MATCHES_BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_MATCH_MENU));
            return;
        }
        MenuPagination pagination = MenuPagination.of(waitingMatches.size(), holder.getPageIndex());
        for (int slot = 0; slot < pagination.getItemCountOnPage(); slot++) {
            canvas.set(slot, buildMatchItem(waitingMatches.get(pagination.getFirstItemIndex() + slot)));
        }
        canvas.set(WAITING_MATCHES_INFO_SLOT, MenuItems.info(Messages.text(WaitingMatchMessage.INFO_TITLE), List.of(
                Messages.plural(WaitingMatchMessage.WAITING_MATCH_COUNT, waitingMatches.size()),
                Messages.text(MenuMessage.PAGE_OF_PAGES, Placeholder.of("page", pagination.getPageNumber()),
                        Placeholder.of("pageCount", pagination.getPageCount())),
                Messages.text(WaitingMatchMessage.INFO_HINT))));
        if (pagination.hasPreviousPage()) {
            canvas.set(WAITING_MATCHES_PREVIOUS_PAGE_SLOT, MenuItems.previousPage(pagination));
        }
        if (pagination.hasNextPage()) {
            canvas.set(WAITING_MATCHES_NEXT_PAGE_SLOT, MenuItems.nextPage(pagination));
        }
        canvas.set(WAITING_MATCHES_BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_MATCH_MENU));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        List<BracketMatch> waitingMatches = tournamentManager.findWaitingMatches();
        MenuPagination pagination = MenuPagination.of(waitingMatches.size(), holder.getPageIndex());
        switch (slot) {
            case WAITING_MATCHES_PREVIOUS_PAGE_SLOT -> {
                if (pagination.hasPreviousPage()) {
                    navigation.open(player, AdminMenuHolder.waitingMatches(pagination.getPreviousPageIndex()));
                }
            }
            case WAITING_MATCHES_NEXT_PAGE_SLOT -> {
                if (pagination.hasNextPage()) {
                    navigation.open(player, AdminMenuHolder.waitingMatches(pagination.getNextPageIndex()));
                }
            }
            case WAITING_MATCHES_INFO_SLOT -> { }
            case WAITING_MATCHES_BACK_SLOT -> navigation.open(player, AdminMenuHolder.match());
            default -> openMatchResult(player, waitingMatches, pagination, slot);
        }
    }

    void openWaitingMatchesMenu(Player player) {
        if (!tournamentManager.isTournamentInProgress()) {
            TournamentMessages.sendError(player, Messages.text(CommonMessage.NO_TOURNAMENT_IN_PROGRESS));
            return;
        }
        navigation.open(player, AdminMenuHolder.waitingMatches(0));
    }

    static String describeRound(BracketMatch match) {
        return Messages.text(WaitingMatchMessage.ROUND, Placeholder.of("round", match.getRoundIndex() + 1));
    }

    private void openMatchResult(Player player, List<BracketMatch> waitingMatches, MenuPagination pagination,
                                 int slot) {
        OptionalInt matchIndex = pagination.findItemIndex(slot);
        if (matchIndex.isEmpty()) {
            return;
        }
        navigation.open(player, AdminMenuHolder.waitingMatchResult(
                waitingMatches.get(matchIndex.getAsInt()).getMatchId(), pagination.pageIndex()));
    }

    private MenuTile buildMatchItem(BracketMatch match) {
        List<String> lore = new ArrayList<>();
        lore.add(Messages.text(WaitingMatchMessage.TEAMS,
                Placeholder.of("firstTeam", match.getFirstTeamName().orElseThrow()),
                Placeholder.of("secondTeam", match.getSecondTeamName().orElseThrow())));
        lore.add(describeRound(match));
        for (String teamName : List.of(match.getFirstTeamName().orElseThrow(),
                match.getSecondTeamName().orElseThrow())) {
            if (teamRegistry.findTeam(teamName).isEmpty()) {
                lore.add(Messages.text(WaitingMatchMessage.TEAM_NOT_REGISTERED, Placeholder.of("team", teamName)));
            }
        }
        return MenuItems.create(Material.PLAYER_HEAD, match.getMatchId(), lore);
    }
}
