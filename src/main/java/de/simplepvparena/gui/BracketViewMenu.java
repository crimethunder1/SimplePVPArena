package de.simplepvparena.gui;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.BracketViewMessage;
import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.tournament.Bracket;
import de.simplepvparena.tournament.BracketMatch;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class BracketViewMenu implements AdminMenu {

    static final Material WAITING_FOR_TEAMS_MATERIAL = Material.GRAY_CONCRETE;
    static final Material READY_MATERIAL = Material.YELLOW_CONCRETE;
    static final Material RUNNING_MATERIAL = Material.LIME_CONCRETE;
    static final Material DECIDED_MATERIAL = Material.GREEN_CONCRETE;

    static final int MATCH_SLOT_COUNT = 45;

    private static final int BRACKET_VIEW_MENU_SIZE = 54;
    private static final int ROUNDS_PER_PAGE = 1;

    private static final int BRACKET_VIEW_PREVIOUS_ROUND_SLOT = 45;
    private static final int BRACKET_VIEW_BACK_SLOT = 48;
    private static final int BRACKET_VIEW_INFO_SLOT = 49;
    private static final int BRACKET_VIEW_NEXT_ROUND_SLOT = 53;

    private final AdminMenuNavigation navigation;
    private final TournamentManager tournamentManager;
    private final MatchService matchService;

    BracketViewMenu(AdminMenuNavigation navigation, TournamentManager tournamentManager, MatchService matchService) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.matchService = Objects.requireNonNull(matchService, "matchService");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(BracketViewMessage.TITLE, Placeholder.of("round", roundsOf(holder).getPageNumber()));
    }

    @Override
    public int size() {
        return BRACKET_VIEW_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        if (showsBackTile(player, holder)) {
            canvas.set(BRACKET_VIEW_BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_MAIN_MENU));
        }
        Optional<Bracket> bracket = tournamentManager.getBracket();
        if (bracket.isEmpty()) {
            canvas.set(BRACKET_VIEW_INFO_SLOT, MenuItems.info(Messages.text(BracketViewMessage.INFO_TITLE),
                    List.of(Messages.text(CommonMessage.NO_TOURNAMENT_IN_PROGRESS))));
            return;
        }
        MenuPagination rounds = roundsOf(holder);
        List<BracketMatch> matches = bracket.get().getRounds().get(rounds.pageIndex());
        Optional<String> runningMatchId = findRunningMatchId();
        int shownMatchCount = Math.min(matches.size(), MATCH_SLOT_COUNT);
        for (int slot = 0; slot < shownMatchCount; slot++) {
            canvas.set(slot, buildMatchTile(matches.get(slot), runningMatchId));
        }
        canvas.set(BRACKET_VIEW_INFO_SLOT, MenuItems.info(Messages.text(BracketViewMessage.INFO_TITLE),
                buildInfoLore(bracket.get(), rounds, matches.size(), shownMatchCount)));
        if (rounds.hasPreviousPage()) {
            canvas.set(BRACKET_VIEW_PREVIOUS_ROUND_SLOT, buildRoundTile(BracketViewMessage.PREVIOUS_ROUND_TITLE,
                    rounds.getPreviousPageIndex(), rounds.getPageCount()));
        }
        if (rounds.hasNextPage()) {
            canvas.set(BRACKET_VIEW_NEXT_ROUND_SLOT, buildRoundTile(BracketViewMessage.NEXT_ROUND_TITLE,
                    rounds.getNextPageIndex(), rounds.getPageCount()));
        }
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        MenuPagination rounds = roundsOf(holder);
        switch (slot) {
            case BRACKET_VIEW_PREVIOUS_ROUND_SLOT -> {
                if (rounds.hasPreviousPage()) {
                    navigation.open(player, AdminMenuHolder.bracketView(rounds.getPreviousPageIndex(),
                            holder.isOpenedFromMainMenu()));
                }
            }
            case BRACKET_VIEW_NEXT_ROUND_SLOT -> {
                if (rounds.hasNextPage()) {
                    navigation.open(player, AdminMenuHolder.bracketView(rounds.getNextPageIndex(),
                            holder.isOpenedFromMainMenu()));
                }
            }
            case BRACKET_VIEW_BACK_SLOT -> {
                if (showsBackTile(player, holder)) {
                    navigation.open(player, AdminMenuHolder.main());
                }
            }
            default -> { }
        }
    }

    void open(Player player) {
        navigation.open(player, AdminMenuHolder.bracketView(findStartRoundIndex(), false));
    }

    void openFromMainMenu(Player player) {
        navigation.open(player, AdminMenuHolder.bracketView(findStartRoundIndex(), true));
    }

    int findStartRoundIndex() {
        Optional<Bracket> bracket = tournamentManager.getBracket();
        if (bracket.isEmpty()) {
            return 0;
        }
        if (bracket.get().isComplete()) {
            return bracket.get().getRoundCount() - 1;
        }
        return findRunningMatchId()
                .flatMap(bracket.get()::findMatch)
                .or(() -> bracket.get().getPlayableMatches().stream().findFirst())
                .map(BracketMatch::getRoundIndex)
                .orElse(0);
    }

    private static boolean showsBackTile(Player player, AdminMenuHolder holder) {
        return holder.isOpenedFromMainMenu() && AdminPermission.isAdmin(player);
    }

    private MenuPagination roundsOf(AdminMenuHolder holder) {
        int roundCount = tournamentManager.getBracket().map(Bracket::getRoundCount).orElse(0);
        return new MenuPagination(roundCount, ROUNDS_PER_PAGE, holder.getPageIndex());
    }

    private Optional<String> findRunningMatchId() {
        return matchService.getActiveMatch().map(Match::getMatchId);
    }

    private List<String> buildInfoLore(Bracket bracket, MenuPagination rounds, int matchCount, int shownMatchCount) {
        List<String> lore = new ArrayList<>();
        lore.add(Messages.text(BracketViewMessage.INFO_STATE,
                Placeholder.of("state", tournamentManager.getState().describe())));
        lore.add(describeRound(rounds.pageIndex(), rounds.getPageCount()));
        bracket.getChampionTeamName().ifPresent(champion -> lore.add(Messages.text(BracketViewMessage.INFO_CHAMPION,
                Placeholder.of("team", champion))));
        if (shownMatchCount < matchCount) {
            lore.add(Messages.text(BracketViewMessage.INFO_MATCHES_CUT_OFF, Placeholder.of("shown", shownMatchCount),
                    Placeholder.of("total", matchCount)));
        }
        return lore;
    }

    private static MenuTile buildRoundTile(MessageKey title, int targetRoundIndex, int roundCount) {
        return MenuItems.create(MenuItems.PAGE_MATERIAL, Messages.text(title),
                describeRound(targetRoundIndex, roundCount));
    }

    private static String describeRound(int roundIndex, int roundCount) {
        return Messages.text(BracketViewMessage.ROUND_OF_ROUNDS, Placeholder.of("round", roundIndex + 1),
                Placeholder.of("roundCount", roundCount));
    }

    private static MenuTile buildMatchTile(BracketMatch match, Optional<String> runningMatchId) {
        if (match.isFirstRoundBye()) {
            return MenuItems.create(DECIDED_MATERIAL, match.getMatchId(), Messages.text(BracketViewMessage.MATCH_BYE,
                    Placeholder.of("team", match.getWinnerTeamName().orElseThrow())));
        }
        String teams = describeTeams(match);
        if (match.isDecided()) {
            return MenuItems.create(DECIDED_MATERIAL, match.getMatchId(), teams,
                    Messages.text(BracketViewMessage.MATCH_WON_BY,
                            Placeholder.of("team", match.getWinnerTeamName().orElseThrow())));
        }
        if (runningMatchId.filter(match.getMatchId()::equals).isPresent()) {
            return MenuItems.create(RUNNING_MATERIAL, match.getMatchId(), teams,
                    Messages.text(BracketViewMessage.MATCH_RUNNING));
        }
        if (match.isPlayable()) {
            return MenuItems.create(READY_MATERIAL, match.getMatchId(), teams,
                    Messages.text(BracketViewMessage.MATCH_READY));
        }
        return MenuItems.create(WAITING_FOR_TEAMS_MATERIAL, match.getMatchId(), teams,
                Messages.text(BracketViewMessage.MATCH_WAITING_FOR_TEAMS));
    }

    private static String describeTeams(BracketMatch match) {
        String openTeam = Messages.text(BracketViewMessage.MATCH_OPEN_TEAM);
        return Messages.text(BracketViewMessage.MATCH_TEAMS,
                Placeholder.of("firstTeam", match.getFirstTeamName().orElse(openTeam)),
                Placeholder.of("secondTeam", match.getSecondTeamName().orElse(openTeam)));
    }
}
