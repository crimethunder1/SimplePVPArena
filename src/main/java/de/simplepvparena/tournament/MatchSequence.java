package de.simplepvparena.tournament;

import org.jspecify.annotations.NullMarked;

import java.util.Optional;

@NullMarked
public final class MatchSequence {

    private MatchSequence() {
    }

    public static Optional<BracketMatch> findNextMatch(Bracket bracket) {
        return bracket.getPlayableMatches().stream()
                .filter(bracketMatch -> !bracket.isFinalMatch(bracketMatch))
                .findFirst();
    }
}
