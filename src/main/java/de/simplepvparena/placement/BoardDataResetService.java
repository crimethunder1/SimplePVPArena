package de.simplepvparena.placement;

import de.simplepvparena.gui.BoardDataReset;
import de.simplepvparena.message.ArenaMenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.tournament.TournamentManager;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;

@NullMarked
public final class BoardDataResetService implements BoardDataReset {

    private final AutoMatchLeaderboard autoMatchLeaderboard;
    private final TournamentManager tournamentManager;
    private final Runnable boardRefresher;

    public BoardDataResetService(AutoMatchLeaderboard autoMatchLeaderboard, TournamentManager tournamentManager,
                                 Runnable boardRefresher) {
        this.autoMatchLeaderboard = Objects.requireNonNull(autoMatchLeaderboard, "autoMatchLeaderboard");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.boardRefresher = Objects.requireNonNull(boardRefresher, "boardRefresher");
    }

    @Override
    public String resetStandings() {
        autoMatchLeaderboard.clear();
        if (tournamentManager.isTournamentInProgress()) {
            boardRefresher.run();
            return Messages.text(ArenaMenuMessage.STANDINGS_DATA_RESET_TOURNAMENT_KEPT);
        }
        tournamentManager.forgetFinishedTournament();
        boardRefresher.run();
        return Messages.text(ArenaMenuMessage.STANDINGS_DATA_RESET);
    }

    @Override
    public Optional<String> findBracketRejection() {
        if (tournamentManager.isTournamentInProgress()) {
            return Optional.of(Messages.text(ArenaMenuMessage.BRACKET_DATA_RESET_TOURNAMENT_RUNNING));
        }
        return Optional.empty();
    }

    @Override
    public String resetBracket() {
        tournamentManager.forgetFinishedTournament();
        boardRefresher.run();
        return Messages.text(ArenaMenuMessage.BRACKET_DATA_RESET);
    }
}
