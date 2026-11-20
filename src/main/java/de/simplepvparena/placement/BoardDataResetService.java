package de.simplepvparena.placement;

import de.simplepvparena.gui.BoardDataReset;
import de.simplepvparena.message.ArenaMenuMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentState;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

@NullMarked
public final class BoardDataResetService implements BoardDataReset {

    private final AutoMatchLeaderboard autoMatchLeaderboard;
    private final TournamentManager tournamentManager;
    private final BooleanSupplier matchRunning;
    private final Runnable boardRefresher;

    public BoardDataResetService(AutoMatchLeaderboard autoMatchLeaderboard, TournamentManager tournamentManager,
                                 BooleanSupplier matchRunning, Runnable boardRefresher) {
        this.autoMatchLeaderboard = Objects.requireNonNull(autoMatchLeaderboard, "autoMatchLeaderboard");
        this.tournamentManager = Objects.requireNonNull(tournamentManager, "tournamentManager");
        this.matchRunning = Objects.requireNonNull(matchRunning, "matchRunning");
        this.boardRefresher = Objects.requireNonNull(boardRefresher, "boardRefresher");
    }

    @Override
    public String resetStandings() {
        autoMatchLeaderboard.clear();
        if (!mayDropTheTournament()) {
            boardRefresher.run();
            return Messages.text(ArenaMenuMessage.STANDINGS_DATA_RESET_TOURNAMENT_KEPT);
        }
        dropTheTournament();
        boardRefresher.run();
        return Messages.text(ArenaMenuMessage.STANDINGS_DATA_RESET);
    }

    @Override
    public Optional<String> findBracketRejection() {
        if (!mayDropTheTournament()) {
            return Optional.of(Messages.text(ArenaMenuMessage.BRACKET_DATA_RESET_TOURNAMENT_RUNNING));
        }
        return Optional.empty();
    }

    @Override
    public String resetBracket() {
        dropTheTournament();
        boardRefresher.run();
        return Messages.text(ArenaMenuMessage.BRACKET_DATA_RESET);
    }

    private boolean mayDropTheTournament() {
        TournamentState state = tournamentManager.getState();
        return state != TournamentState.RUNNING && !(state == TournamentState.PAUSED && matchRunning.getAsBoolean());
    }

    private void dropTheTournament() {
        if (tournamentManager.getState() == TournamentState.PAUSED) {
            tournamentManager.cancelTournament();
            return;
        }
        tournamentManager.forgetFinishedTournament();
    }
}
