package de.simplepvparena.gui;

import de.simplepvparena.match.MatchService;
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
    private static final int MATCH_FINAL_SLOT = 15;
    private static final int MATCH_FORCE_RESULT_SLOT = 16;

    private final AdminMenuNavigation navigation;
    private final TournamentManager tournamentManager;
    private final MatchService matchService;
    private final TournamentMenuStatus tournamentMenuStatus;
    private final ForceResultAdminMenu forceResultAdminMenu;

    MatchAdminMenu(AdminMenuNavigation navigation, TournamentManager tournamentManager, MatchService matchService,
                   TournamentMenuStatus tournamentMenuStatus, ForceResultAdminMenu forceResultAdminMenu) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.tournamentMenuStatus = Objects.requireNonNull(tournamentMenuStatus, "tournamentMenuStatus");
        this.forceResultAdminMenu = Objects.requireNonNull(forceResultAdminMenu, "forceResultAdminMenu");
    }

    @Override
    public String title(AdminMenuHolder holder) {
        return "Match and tournament";
    }

    @Override
    public int size() {
        return AdminMenuLayout.SMALL_MENU_SIZE;
    }

    @Override
    public void fill(Player player, AdminMenuHolder holder, AdminMenuCanvas canvas) {
        TournamentMenuOptions options = tournamentMenuStatus.currentOptions();
        canvas.set(AdminMenuLayout.INFO_SLOT, MenuItems.info("Tournament", List.of(
                tournamentManager.describeStatus(),
                "Running match: " + tournamentMenuStatus.describeActiveMatch(),
                "Round timer: " + (matchService.isRoundTimerPaused() ? "paused" : "running"))));
        canvas.set(MATCH_START_SLOT, options.startOffered()
                ? MenuItems.create(MenuItems.CONFIRM_MATERIAL, "Start the tournament",
                        "Needs an even number of teams")
                : MenuItems.unavailable("Start the tournament", "A tournament is already in progress"));
        canvas.set(MATCH_FORCE_START_SLOT, options.forceStartOffered()
                ? MenuItems.create(Material.YELLOW_WOOL, "Force start",
                        "Starts the tournament with an uneven number of teams")
                : MenuItems.unavailable("Force start", "A tournament is already in progress"));
        canvas.set(MATCH_PAUSE_SLOT, buildPauseItem(options));
        canvas.set(MATCH_CANCEL_SLOT, options.cancelOffered()
                ? MenuItems.create(MenuItems.REFUSE_MATERIAL, "Cancel the tournament",
                        "Stops the running match and the bracket", "Asks for a confirmation first")
                : MenuItems.unavailable("Cancel the tournament", "No tournament is in progress"));
        canvas.set(MATCH_FINAL_SLOT, tournamentMenuStatus.buildFinalItem());
        canvas.set(MATCH_FORCE_RESULT_SLOT, options.forceResultOffered()
                ? MenuItems.create(Material.ANVIL, "Force a result",
                        "Decides the running match for one of its two teams")
                : MenuItems.unavailable("Force a result", "No match is running"));
        canvas.set(AdminMenuLayout.BACK_SLOT, MenuItems.back("the main menu"));
    }

    @Override
    public void handleClick(Player player, AdminMenuHolder holder, int slot) {
        switch (slot) {
            case MATCH_START_SLOT -> runOperation(player, holder, () -> tournamentManager.startTournament(false));
            case MATCH_FORCE_START_SLOT -> runOperation(player, holder, () -> tournamentManager.startTournament(true));
            case MATCH_PAUSE_SLOT -> runOperation(player, holder, tournamentManager::togglePause);
            case MATCH_CANCEL_SLOT -> navigation.open(player,
                    AdminMenuHolder.confirmation(ConfirmationRequest.CANCEL_TOURNAMENT, null, 0));
            case MATCH_FINAL_SLOT -> runOperation(player, holder, tournamentManager::startFinalMatch);
            case MATCH_FORCE_RESULT_SLOT -> forceResultAdminMenu.openForceResultMenu(player);
            case AdminMenuLayout.BACK_SLOT -> navigation.open(player, AdminMenuHolder.main());
            default -> { }
        }
    }

    List<String> describeCancelConfirmation() {
        return List.of(
                "The running tournament is cancelled",
                "The running match is stopped and its bracket is dropped");
    }

    void cancelTournament(Player player) {
        AdminMenuReplies.reportOperation(player, tournamentManager.cancelTournament());
    }

    private MenuTile buildPauseItem(TournamentMenuOptions options) {
        if (options.resumeOffered()) {
            return MenuItems.create(Material.CLOCK, "Continue the tournament",
                    "The round timer continues with its remaining time");
        }
        if (options.pauseOffered()) {
            return MenuItems.create(Material.CLOCK, "Pause the tournament",
                    "No further match is started until it continues",
                    "The round timer keeps its remaining time");
        }
        return MenuItems.unavailable("Pause the tournament", "No tournament is in progress");
    }

    private void runOperation(Player player, AdminMenuHolder holder,
                              Supplier<TournamentOperationResult> operation) {
        AdminMenuReplies.reportOperation(player, operation.get());
        navigation.refresh(player, holder);
    }
}
