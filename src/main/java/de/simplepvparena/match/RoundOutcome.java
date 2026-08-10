package de.simplepvparena.match;

import java.util.Optional;

public record RoundOutcome(Optional<String> winnerTeamName) {

    public static RoundOutcome victory(String teamName) {
        return new RoundOutcome(Optional.of(teamName));
    }

    public static RoundOutcome draw() {
        return new RoundOutcome(Optional.empty());
    }

    public boolean isDraw() {
        return winnerTeamName.isEmpty();
    }
}
