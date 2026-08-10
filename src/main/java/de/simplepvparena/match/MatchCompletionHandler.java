package de.simplepvparena.match;

@FunctionalInterface
public interface MatchCompletionHandler {

    void onMatchCompleted(Match match, String winnerTeamName);
}
