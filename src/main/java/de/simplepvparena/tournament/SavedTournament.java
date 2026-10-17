package de.simplepvparena.tournament;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record SavedTournament(Instant startedAt,
                              Instant savedAt,
                              TournamentState state,
                              List<SavedFirstRoundMatch> firstRound,
                              List<SavedMatchResult> results) {

    public SavedTournament {
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(savedAt, "savedAt");
        Objects.requireNonNull(state, "state");
        if (state != TournamentState.RUNNING && state != TournamentState.PAUSED) {
            throw new IllegalArgumentException("the state of a saved tournament is " + TournamentState.RUNNING
                    + " or " + TournamentState.PAUSED + ", not " + state);
        }
        firstRound = List.copyOf(firstRound);
        results = List.copyOf(results);
    }

    public static SavedTournament capture(Bracket bracket, TournamentState state, Instant startedAt,
                                          Instant savedAt, Map<String, MatchDecisionKind> decisionKindsByMatchId) {
        List<SavedMatchResult> results = bracket.getMatchesInDecisionOrder().stream()
                .map(match -> new SavedMatchResult(match.getMatchId(), match.getWinnerTeamName().orElseThrow(),
                        Optional.ofNullable(decisionKindsByMatchId.get(match.getMatchId()))))
                .toList();
        return new SavedTournament(startedAt, savedAt, state, bracket.describeFirstRound(), results);
    }

    public Bracket restoreBracket() {
        Bracket bracket = Bracket.restore(firstRound, results);
        if (bracket.isComplete()) {
            throw new IllegalArgumentException("the results already decide the final, nothing is left to continue");
        }
        return bracket;
    }
}
