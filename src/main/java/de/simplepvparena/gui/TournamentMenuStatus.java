package de.simplepvparena.gui;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.tournament.FinalMatchStage;
import de.simplepvparena.tournament.TournamentManager;
import org.bukkit.Material;

import java.util.Objects;

final class TournamentMenuStatus {

    private final TournamentManager tournamentManager;
    private final MatchService matchService;

    TournamentMenuStatus(TournamentManager tournamentManager, MatchService matchService) {
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.matchService = Objects.requireNonNull(matchService, "matchService");
    }

    MenuTile buildFinalItem() {
        if (!currentOptions().finalStartOffered()) {
            return MenuItems.unavailable("Start the final", describeUnavailableFinal());
        }
        return MenuItems.create(Material.NETHER_STAR, "Start the final",
                "Both finalists are known", "Starts the final on the arena");
    }

    private String describeUnavailableFinal() {
        FinalMatchStage finalMatchStage = tournamentManager.getFinalMatchStage();
        return switch (finalMatchStage) {
            case RUNNING -> "The final is running";
            case FINISHED -> "The final is over";
            default -> "The final is not ready, its stage is " + finalMatchStage;
        };
    }

    TournamentMenuOptions currentOptions() {
        return TournamentMenuOptions.of(tournamentManager.getState(), tournamentManager.getFinalMatchStage(),
                matchService.hasActiveMatch());
    }

    String describeActiveMatch() {
        return matchService.getActiveMatch().map(Match::describe).orElse("none");
    }
}
