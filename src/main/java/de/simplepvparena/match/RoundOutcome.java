package de.simplepvparena.match;

import java.util.Objects;
import java.util.Optional;

public record RoundOutcome(Optional<String> winnerTeamName, boolean fromSuddenDeath) {

    public RoundOutcome {
        Objects.requireNonNull(winnerTeamName, "winnerTeamName");
    }

    public static RoundOutcome victory(String teamName) {
        return new RoundOutcome(Optional.of(teamName), false);
    }

    public static RoundOutcome draw() {
        return new RoundOutcome(Optional.empty(), false);
    }

    public static RoundOutcome suddenDeathDraw() {
        return new RoundOutcome(Optional.empty(), true);
    }

    public RoundOutcome markedAsSuddenDeath() {
        return fromSuddenDeath ? this : new RoundOutcome(winnerTeamName, true);
    }

    public boolean isDraw() {
        return winnerTeamName.isEmpty();
    }

    public boolean isSuddenDeathDraw() {
        return isDraw() && fromSuddenDeath;
    }
}
