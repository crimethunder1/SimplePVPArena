package de.simplepvparena.gui;

import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.MatchMenuMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
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
            return MenuItems.unavailable(Messages.text(MatchMenuMessage.FINAL_TITLE), describeUnavailableFinal());
        }
        return MenuItems.create(Material.NETHER_STAR, Messages.text(MatchMenuMessage.FINAL_TITLE),
                Messages.lines(MatchMenuMessage.FINAL_LORE));
    }

    private String describeUnavailableFinal() {
        MessageKey reason = switch (tournamentManager.getFinalMatchStage()) {
            case PENDING -> MatchMenuMessage.FINAL_PENDING;
            case READY -> MatchMenuMessage.FINAL_READY;
            case RUNNING -> MatchMenuMessage.FINAL_RUNNING;
            case FINISHED -> MatchMenuMessage.FINAL_OVER;
        };
        return Messages.text(reason);
    }

    TournamentMenuOptions currentOptions() {
        return TournamentMenuOptions.of(tournamentManager.getState(), tournamentManager.getFinalMatchStage(),
                matchService.hasActiveMatch());
    }

    String describeActiveMatchLine() {
        return matchService.getActiveMatch()
                .map(match -> Messages.text(MatchMenuMessage.RUNNING_MATCH, Placeholder.of("match", match.describe())))
                .orElseGet(() -> Messages.text(MatchMenuMessage.NO_RUNNING_MATCH));
    }
}
