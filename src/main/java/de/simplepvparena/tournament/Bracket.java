package de.simplepvparena.tournament;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Bracket {

    public static final int MINIMUM_TEAM_COUNT = 2;

    private final List<List<BracketMatch>> rounds;
    private final Map<String, BracketMatch> matchesById;
    private final List<String> participatingTeamNames;
    private final Set<String> teamNamesWithFirstRoundBye;

    private Bracket(List<List<BracketMatch>> rounds,
                    List<String> participatingTeamNames,
                    Set<String> teamNamesWithFirstRoundBye) {
        this.rounds = rounds;
        this.participatingTeamNames = List.copyOf(participatingTeamNames);
        this.teamNamesWithFirstRoundBye = Collections.unmodifiableSet(new LinkedHashSet<>(teamNamesWithFirstRoundBye));
        this.matchesById = new LinkedHashMap<>();
        for (List<BracketMatch> round : rounds) {
            for (BracketMatch match : round) {
                matchesById.put(match.getMatchId(), match);
            }
        }
    }

    public static Bracket createSingleElimination(List<String> teamNames) {
        Objects.requireNonNull(teamNames, "teamNames");
        if (teamNames.size() < MINIMUM_TEAM_COUNT) {
            throw new IllegalArgumentException("at least " + MINIMUM_TEAM_COUNT + " teams are required");
        }
        if (new LinkedHashSet<>(teamNames).size() != teamNames.size()) {
            throw new IllegalArgumentException("team names must be unique");
        }

        int bracketSize = smallestPowerOfTwoAtLeast(teamNames.size());
        List<List<BracketMatch>> rounds = new ArrayList<>();
        int matchesInRound = bracketSize / 2;
        int roundIndex = 0;
        while (matchesInRound >= 1) {
            List<BracketMatch> round = new ArrayList<>(matchesInRound);
            for (int positionIndex = 0; positionIndex < matchesInRound; positionIndex++) {
                round.add(new BracketMatch(buildMatchId(roundIndex, positionIndex), roundIndex, positionIndex));
            }
            rounds.add(round);
            matchesInRound /= 2;
            roundIndex++;
        }

        List<BracketMatch> firstRound = rounds.get(0);
        List<Integer> seedingOrder = buildSeedingOrder(bracketSize);
        for (int slotPosition = 0; slotPosition < seedingOrder.size(); slotPosition++) {
            int seed = seedingOrder.get(slotPosition);
            if (seed <= teamNames.size()) {
                BracketMatch match = firstRound.get(slotPosition / 2);
                match.assignParticipant(slotPosition % 2, teamNames.get(seed - 1));
            }
        }

        Bracket bracket = new Bracket(rounds, teamNames, collectFirstRoundByeTeams(firstRound));
        bracket.resolveFirstRoundByes();
        return bracket;
    }

    public List<List<BracketMatch>> getRounds() {
        return rounds.stream().map(List::copyOf).toList();
    }

    public int getRoundCount() {
        return rounds.size();
    }

    public List<BracketMatch> getAllMatches() {
        return List.copyOf(matchesById.values());
    }

    public List<BracketMatch> getPlayableMatches() {
        return matchesById.values().stream()
                .filter(BracketMatch::isPlayable)
                .toList();
    }

    public Optional<BracketMatch> findMatch(String matchId) {
        return Optional.ofNullable(matchesById.get(matchId));
    }

    public List<String> getParticipatingTeamNames() {
        return participatingTeamNames;
    }

    public Set<String> getTeamNamesWithFirstRoundBye() {
        return teamNamesWithFirstRoundBye;
    }

    public void recordWinner(String matchId, String winnerTeamName) {
        BracketMatch match = matchesById.get(matchId);
        if (match == null) {
            throw new IllegalArgumentException("unknown match " + matchId);
        }
        if (match.isDecided()) {
            throw new IllegalStateException("match " + matchId + " is already decided");
        }
        if (!match.containsTeam(winnerTeamName)) {
            throw new IllegalArgumentException(winnerTeamName + " does not take part in match " + matchId);
        }
        match.setWinnerTeamName(winnerTeamName);
        advanceWinner(match);
    }

    public boolean isComplete() {
        return getFinalMatch().isDecided();
    }

    public Optional<String> getChampionTeamName() {
        return getFinalMatch().getWinnerTeamName();
    }

    public BracketMatch getFinalMatch() {
        List<BracketMatch> lastRound = rounds.get(rounds.size() - 1);
        return lastRound.get(0);
    }

    public boolean isFinalMatch(BracketMatch match) {
        return match != null && getFinalMatch().getMatchId().equals(match.getMatchId());
    }

    private void resolveFirstRoundByes() {
        for (BracketMatch match : rounds.get(0)) {
            if (match.hasExactlyOneParticipant()) {
                match.setWinnerTeamName(match.getSingleParticipant());
                advanceWinner(match);
            }
        }
    }

    private void advanceWinner(BracketMatch match) {
        int nextRoundIndex = match.getRoundIndex() + 1;
        if (nextRoundIndex >= rounds.size()) {
            return;
        }
        BracketMatch followUpMatch = rounds.get(nextRoundIndex).get(match.getPositionIndex() / 2);
        followUpMatch.assignParticipant(match.getPositionIndex() % 2, match.getWinnerTeamName().orElseThrow());
    }

    private static Set<String> collectFirstRoundByeTeams(List<BracketMatch> firstRound) {
        Set<String> byeTeams = new LinkedHashSet<>();
        for (BracketMatch match : firstRound) {
            if (match.hasExactlyOneParticipant()) {
                byeTeams.add(match.getSingleParticipant());
            }
        }
        return byeTeams;
    }

    static List<Integer> buildSeedingOrder(int bracketSize) {
        List<Integer> seedingOrder = List.of(1);
        while (seedingOrder.size() < bracketSize) {
            int doubledSize = seedingOrder.size() * 2;
            List<Integer> doubledSeedingOrder = new ArrayList<>(doubledSize);
            for (int seed : seedingOrder) {
                doubledSeedingOrder.add(seed);
                doubledSeedingOrder.add(doubledSize + 1 - seed);
            }
            seedingOrder = doubledSeedingOrder;
        }
        return seedingOrder;
    }

    private static String buildMatchId(int roundIndex, int positionIndex) {
        return "round" + (roundIndex + 1) + "-match" + (positionIndex + 1);
    }

    private static int smallestPowerOfTwoAtLeast(int value) {
        int result = 1;
        while (result < value) {
            result *= 2;
        }
        return result;
    }
}
