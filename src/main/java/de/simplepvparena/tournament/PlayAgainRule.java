package de.simplepvparena.tournament;

import de.simplepvparena.match.Match;
import de.simplepvparena.team.Team;

public interface PlayAgainRule {

    PlayAgainRule NONE = new PlayAgainRule() {

        @Override
        public void autoMatchFinished(Match match, int playAgainSeconds) {
        }

        @Override
        public boolean isWaitingForPlayAgain(Team team) {
            return false;
        }
    };

    void autoMatchFinished(Match match, int playAgainSeconds);

    boolean isWaitingForPlayAgain(Team team);
}
