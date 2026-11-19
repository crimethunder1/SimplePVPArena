package de.simplepvparena.match;

import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface MatchCompletionHandler {

    void onMatchCompleted(Match match, String winnerTeamName);
}
