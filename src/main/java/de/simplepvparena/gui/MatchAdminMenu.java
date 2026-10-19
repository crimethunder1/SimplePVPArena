package de.simplepvparena.gui;

import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.CommonMessage;
import de.simplepvparena.message.MatchMenuMessage;
import de.simplepvparena.message.MenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentOperationResult;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

final class MatchAdminMenu implements AdminMenu {

    private static final int MATCH_START_SLOT = 10;
    private static final int MATCH_FORCE_START_SLOT = 11;
    private static final int MATCH_PAUSE_SLOT = 12;
    private static final int MATCH_CANCEL_SLOT = 13;
    private static final int MATCH_DECIDE_WAITING_MATCH_SLOT = 14;
    private static final int MATCH_FINAL_SLOT = 15;
    private static final int MATCH_FORCE_RESULT_SLOT = 16;

    private final AdminMenuNavigation navigation;
    private final TournamentManager tournamentManager;
    private final MatchService matchService;
    private final TournamentMenuStatus tournamentMenuStatus;
    private final ForceResultAdminMenu forceResultAdminMenu;
    private final WaitingMatchesAdminMenu waitingMatchesAdminMenu;

    MatchAdminMenu(AdminMenuNavigation navigation, TournamentManager tournamentManager, MatchService matchService,
                   TournamentMenuStatus tournamentMenuStatus, ForceResultAdminMenu forceResultAdminMenu,
                   WaitingMatchesAdminMenu waitingMatchesAdminMenu) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.tournamentMenuStatus = Objects.requireNonNull(tournamentMenuStatus, "tournamentMenuStatus");
        this.forceResultAdminMenu = Objects.requireNonNull(forceResultAdminMenu, "forceResultAdminMenu");
        this.waitingMatchesAdminMenu = Objects.requireNonNull(waitingMatchesAdminMenu, "waitingMatchesAdminMenu");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return Messages.text(MatchMenuMessage.TITLE);
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        TournamentMenuOptions options = tournamentMenuStatus.currentOptions();
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info(Messages.text(MatchMenuMessage.INFO_TITLE), List.of(
                tournamentManager.describeStatus(),
                tournamentMenuStatus.describeActiveMatchLine(),
                Messages.text(matchService.isRoundTimerPaused()
                        ? MatchMenuMessage.ROUND_TIMER_PAUSED
                        : MatchMenuMessage.ROUND_TIMER_RUNNING))));
        canvas.set(MATCH_START_SLOT, options.startOffered()
                ? MenuItems.create(MenuItems.CONFIRM_MATERIAL, Messages.text(MatchMenuMessage.START_TITLE),
                        Messages.lines(MatchMenuMessage.START_LORE))
                : MenuItems.unavailable(Messages.text(MatchMenuMessage.START_TITLE),
                        Messages.text(CommonMessage.TOURNAMENT_ALREADY_IN_PROGRESS)));
        canvas.set(MATCH_FORCE_START_SLOT, options.forceStartOffered()
                ? MenuItems.create(Material.YELLOW_WOOL, Messages.text(MatchMenuMessage.FORCE_START_TITLE),
                        Messages.lines(MatchMenuMessage.FORCE_START_LORE))
                : MenuItems.unavailable(Messages.text(MatchMenuMessage.FORCE_START_TITLE),
                        Messages.text(CommonMessage.TOURNAMENT_ALREADY_IN_PROGRESS)));
        canvas.set(MATCH_PAUSE_SLOT, buildPauseItem(options));
        canvas.set(MATCH_CANCEL_SLOT, options.cancelOffered()
                ? MenuItems.create(MenuItems.REFUSE_MATERIAL, Messages.text(MatchMenuMessage.CANCEL_TITLE),
                        Messages.text(MatchMenuMessage.CANCEL_LORE), Messages.text(CommonMessage.ASKS_FOR_CONFIRMATION))
                : MenuItems.unavailable(Messages.text(MatchMenuMessage.CANCEL_TITLE),
                        Messages.text(CommonMessage.NO_TOURNAMENT_IN_PROGRESS)));
        canvas.set(MATCH_DECIDE_WAITING_MATCH_SLOT, waitingMatchesAdminMenu.isTournamentInProgress()
                ? MenuItems.create(Material.LECTERN, Messages.text(MatchMenuMessage.DECIDE_WAITING_MATCH_TITLE),
                        Messages.lines(MatchMenuMessage.DECIDE_WAITING_MATCH_LORE))
                : MenuItems.unavailable(Messages.text(MatchMenuMessage.DECIDE_WAITING_MATCH_TITLE),
                        Messages.text(CommonMessage.NO_TOURNAMENT_IN_PROGRESS)));
        canvas.set(MATCH_FINAL_SLOT, tournamentMenuStatus.buildFinalItem());
        canvas.set(MATCH_FORCE_RESULT_SLOT, options.forceResultOffered()
                ? MenuItems.create(Material.ANVIL, Messages.text(MatchMenuMessage.FORCE_RESULT_TITLE),
                        Messages.lines(MatchMenuMessage.FORCE_RESULT_LORE))
                : MenuItems.unavailable(Messages.text(MatchMenuMessage.FORCE_RESULT_TITLE),
                        Messages.text(MatchMenuMessage.NO_MATCH_RUNNING)));
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back(MenuMessage.BACK_TO_MAIN_MENU));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        switch (slot) {
            case MATCH_START_SLOT -> runOperation(player, holder, () -> tournamentManager.startTournament(false));
            case MATCH_FORCE_START_SLOT -> runOperation(player, holder, () -> tournamentManager.startTournament(true));
            case MATCH_PAUSE_SLOT -> runOperation(player, holder, tournamentManager::togglePause);
            case MATCH_CANCEL_SLOT -> navigation.open(player,
                    AdminMenuHolder.confirmation(ConfirmationRequest.CANCEL_TOURNAMENT, null, 0));
            case MATCH_DECIDE_WAITING_MATCH_SLOT -> waitingMatchesAdminMenu.openWaitingMatchesMenu(player);
            case MATCH_FINAL_SLOT -> runOperation(player, holder, tournamentManager::startFinalMatch);
            case MATCH_FORCE_RESULT_SLOT -> forceResultAdminMenu.openForceResultMenu(player);
            case AdminMenuLayout.BACK_SLOT -> navigation.open(player, AdminMenuHolder.main());
            default -> { }
        }
    }

    AdminMenuConfirmation cancelConfirmation() {
        return new AdminMenuConfirmation() {
            @Override
            public List<String> describe(AdminMenuHolder holder) {
                return describeCancelConfirmation();
            }

            @Override
            public void carryOut(Player player, AdminMenuHolder holder) {
                cancelTournament(player);
            }

            @Override
            public AdminMenuHolder returnMenu(AdminMenuHolder holder) {
                return AdminMenuHolder.match();
            }
        };
    }

    private List<String> describeCancelConfirmation() {
        return Messages.lines(MatchMenuMessage.CANCEL_CONFIRMATION);
    }

    private void cancelTournament(Player player) {
        AdminMenuReplies.reportOperation(player, tournamentManager.cancelTournament());
    }

    private MenuTile buildPauseItem(TournamentMenuOptions options) {
        if (options.resumeOffered()) {
            return MenuItems.create(Material.CLOCK, Messages.text(MatchMenuMessage.CONTINUE_TITLE),
                    Messages.lines(MatchMenuMessage.CONTINUE_LORE));
        }
        if (options.pauseOffered()) {
            return MenuItems.create(Material.CLOCK, Messages.text(MatchMenuMessage.PAUSE_TITLE),
                    Messages.text(MatchMenuMessage.PAUSE_LORE),
                    Messages.text(CommonMessage.ROUND_TIMER_KEEPS_TIME));
        }
        return MenuItems.unavailable(Messages.text(MatchMenuMessage.PAUSE_TITLE),
                Messages.text(CommonMessage.NO_TOURNAMENT_IN_PROGRESS));
    }

    private void runOperation(Player player, AdminMenuHolder holder,
                              Supplier<TournamentOperationResult> operation) {
        AdminMenuReplies.reportOperation(player, operation.get());
        navigation.refresh(player, holder);
    }
}
