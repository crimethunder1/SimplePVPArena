package de.simplepvparena.gui;

import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.DecidedMatchMessage;
import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.tournament.BracketMatch;
import de.simplepvparena.tournament.MatchDecisionKind;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class DecidedMatchesAdminMenu implements AdminMenu {

    private static final int DECIDED_MATCHES_MENU_SIZE = 54;

    private static final int DECIDED_MATCHES_PREVIOUS_PAGE_SLOT = 45;
    private static final int DECIDED_MATCHES_INFO_SLOT = 47;
    private static final int DECIDED_MATCHES_BACK_SLOT = 49;
    private static final int DECIDED_MATCHES_NEXT_PAGE_SLOT = 53;

    private final AdminMenuNavigation navigation;
    private final TournamentManager tournamentManager;

    DecidedMatchesAdminMenu(AdminMenuNavigation navigation, TournamentManager tournamentManager) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(DecidedMatchMessage.TITLE, Placeholder.of("page",
                MenuPagination.of(tournamentManager.findRevocableMatches().size(), holder.getPageIndex())
                        .getPageNumber()));
    }

    @Override
    public int size() {
        return DECIDED_MATCHES_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        holder.forgetMatchIdsOfSlots();
        List<BracketMatch> revocableMatches = tournamentManager.findRevocableMatches();
        if (revocableMatches.isEmpty()) {
            canvas.set(DECIDED_MATCHES_INFO_SLOT, MenuItems.info(Messages.text(DecidedMatchMessage.INFO_TITLE),
                    List.of(Messages.text(DecidedMatchMessage.NO_RESULT_REVOCABLE))));
            canvas.set(DECIDED_MATCHES_BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_MATCH_MENU));
            return;
        }
        MenuPagination pagination = MenuPagination.of(revocableMatches.size(), holder.getPageIndex());
        for (int slot = 0; slot < pagination.getItemCountOnPage(); slot++) {
            BracketMatch revocableMatch = revocableMatches.get(pagination.getFirstItemIndex() + slot);
            canvas.set(slot, buildMatchItem(revocableMatch));
            holder.rememberMatchIdOfSlot(slot, revocableMatch.getMatchId());
        }
        canvas.set(DECIDED_MATCHES_INFO_SLOT, MenuItems.info(Messages.text(DecidedMatchMessage.INFO_TITLE), List.of(
                Messages.plural(DecidedMatchMessage.REVOCABLE_MATCH_COUNT, revocableMatches.size()),
                Messages.text(MenuMessage.PAGE_OF_PAGES, Placeholder.of("page", pagination.getPageNumber()),
                        Placeholder.of("pageCount", pagination.getPageCount())))));
        if (pagination.hasPreviousPage()) {
            canvas.set(DECIDED_MATCHES_PREVIOUS_PAGE_SLOT, MenuItems.previousPage(pagination));
        }
        if (pagination.hasNextPage()) {
            canvas.set(DECIDED_MATCHES_NEXT_PAGE_SLOT, MenuItems.nextPage(pagination));
        }
        canvas.set(DECIDED_MATCHES_BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_MATCH_MENU));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        List<BracketMatch> revocableMatches = tournamentManager.findRevocableMatches();
        MenuPagination pagination = MenuPagination.of(revocableMatches.size(), holder.getPageIndex());
        switch (slot) {
            case DECIDED_MATCHES_PREVIOUS_PAGE_SLOT -> {
                if (pagination.hasPreviousPage()) {
                    navigation.open(player, AdminMenuHolder.decidedMatches(pagination.getPreviousPageIndex()));
                }
            }
            case DECIDED_MATCHES_NEXT_PAGE_SLOT -> {
                if (pagination.hasNextPage()) {
                    navigation.open(player, AdminMenuHolder.decidedMatches(pagination.getNextPageIndex()));
                }
            }
            case DECIDED_MATCHES_INFO_SLOT -> { }
            case DECIDED_MATCHES_BACK_SLOT -> navigation.open(player, AdminMenuHolder.match());
            default -> askForTheRevocation(player, holder, pagination, slot);
        }
    }

    void openDecidedMatchesMenu(Player player) {
        if (!tournamentManager.isTournamentInProgress()) {
            TournamentMessages.sendError(player, Messages.text(CommonMessage.NO_TOURNAMENT_IN_PROGRESS));
            return;
        }
        navigation.open(player, AdminMenuHolder.decidedMatches(0));
    }

    AdminMenuConfirmation revokeConfirmation() {
        return new AdminMenuConfirmation() {
            @Override
            public List<String> describe(AdminMenuHolder holder) {
                return describeRevocation(holder.getSubject().orElseThrow());
            }

            @Override
            public void carryOut(Player player, AdminMenuHolder holder) {
                AdminMenuReplies.reportOperation(player,
                        tournamentManager.revokeMatchResult(holder.getSubject().orElseThrow()));
            }

            @Override
            public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
                return AdminMenuHolder.decidedMatches(holder.getPageIndex());
            }
        };
    }

    private List<String> describeRevocation(String matchId) {
        Optional<String> rejection = tournamentManager.findRevokeRejection(matchId);
        if (rejection.isPresent()) {
            return AdminMenuReplies.toLoreLines(rejection.get());
        }
        String winnerTeamName = tournamentManager.getBracket()
                .flatMap(bracket -> bracket.findMatch(matchId))
                .flatMap(BracketMatch::getWinnerTeamName)
                .orElseThrow();
        return Messages.lines(DecidedMatchMessage.CONFIRMATION, Placeholder.of("team", winnerTeamName),
                Placeholder.of("match", matchId));
    }

    private void askForTheRevocation(Player player, AdminMenuHolder holder, MenuPagination pagination, int slot) {
        Optional<String> matchId = holder.findMatchIdOfSlot(slot);
        if (matchId.isEmpty()) {
            return;
        }
        Optional<String> rejection = tournamentManager.findRevokeRejection(matchId.get());
        if (rejection.isPresent()) {
            TournamentMessages.sendError(player, rejection.get());
            navigation.refresh(player, holder);
            return;
        }
        navigation.open(player, AdminMenuHolder.confirmation(ConfirmationRequest.REVOKE_RESULT, matchId.get(),
                pagination.pageIndex()));
    }

    private MenuTile buildMatchItem(BracketMatch match) {
        List<String> lore = new ArrayList<>();
        lore.add(Messages.text(DecidedMatchMessage.TEAMS,
                Placeholder.of("firstTeam", match.getFirstTeamName().orElseThrow()),
                Placeholder.of("secondTeam", match.getSecondTeamName().orElseThrow())));
        lore.add(Messages.text(DecidedMatchMessage.WINNER,
                Placeholder.of("team", match.getWinnerTeamName().orElseThrow())));
        tournamentManager.findDecisionKind(match.getMatchId())
                .ifPresent(decisionKind -> lore.add(describeDecisionKind(decisionKind)));
        lore.add(Messages.text(DecidedMatchMessage.HINT));
        return MenuItems.create(Material.PLAYER_HEAD, match.getMatchId(), lore);
    }

    private static String describeDecisionKind(MatchDecisionKind decisionKind) {
        return Messages.text(switch (decisionKind) {
            case PLAYED -> DecidedMatchMessage.PLAYED;
            case WALKOVER -> DecidedMatchMessage.WALKOVER;
            case FORCED -> DecidedMatchMessage.FORCED;
        });
    }
}
