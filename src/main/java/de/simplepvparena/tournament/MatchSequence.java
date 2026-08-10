package de.simplepvparena.tournament;

import java.util.Optional;

public final class MatchSequence {

    private MatchSequence() {
    }

    public static Optional<BracketMatch> findNextMatch(Bracket bracket) {
        return bracket.getPlayableMatches().stream()
                .filter(bracketMatch -> !bracket.isFinalMatch(bracketMatch))
                .findFirst();
    }
}
