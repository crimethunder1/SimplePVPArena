package de.simplepvparena.tournament;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
    private final List<BracketMatch> matchesInDecisionOrder = new ArrayList<>();

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
        List<List<BracketMatch>> rounds = buildRounds(bracketSize);

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

    public static Bracket restore(List<SavedFirstRoundMatch> savedFirstRound, List<SavedMatchResult> savedResults) {
        Objects.requireNonNull(savedFirstRound, "savedFirstRound");
        Objects.requireNonNull(savedResults, "savedResults");
        int firstRoundMatchCount = savedFirstRound.size();
        if (Integer.bitCount(firstRoundMatchCount) != 1) {
            throw new IllegalArgumentException("the first round holds " + firstRoundMatchCount
                    + " matches, a bracket needs a power of two");
        }

        List<List<BracketMatch>> rounds = buildRounds(firstRoundMatchCount * 2);
        List<BracketMatch> firstRound = rounds.get(0);
        Map<String, BracketMatch> firstRoundMatchesById = new LinkedHashMap<>();
        for (BracketMatch match : firstRound) {
            firstRoundMatchesById.put(match.getMatchId(), match);
        }
        Set<String> placedMatchIds = new HashSet<>();
        Set<String> placedTeamKeys = new HashSet<>();
        List<String> teamNames = new ArrayList<>();
        for (SavedFirstRoundMatch savedMatch : savedFirstRound) {
            BracketMatch match = firstRoundMatchesById.get(savedMatch.matchId());
            if (match == null) {
                throw new IllegalArgumentException("the first round names unknown match " + savedMatch.matchId());
            }
            if (!placedMatchIds.add(match.getMatchId())) {
                throw new IllegalArgumentException("the first round lists match " + match.getMatchId() + " twice");
            }
            if (savedMatch.firstTeamName().isEmpty() && savedMatch.secondTeamName().isEmpty()) {
                throw new IllegalArgumentException("match " + match.getMatchId() + " of the first round has no team");
            }
            placeFirstRoundTeam(match, BracketMatch.FIRST_SLOT_INDEX, savedMatch.firstTeamName(), placedTeamKeys,
                    teamNames);
            placeFirstRoundTeam(match, BracketMatch.SECOND_SLOT_INDEX, savedMatch.secondTeamName(), placedTeamKeys,
                    teamNames);
        }
        if (teamNames.size() < MINIMUM_TEAM_COUNT) {
            throw new IllegalArgumentException("at least " + MINIMUM_TEAM_COUNT + " teams are required");
        }

        Bracket bracket = new Bracket(rounds, teamNames, collectFirstRoundByeTeams(firstRound));
        bracket.resolveFirstRoundByes();
        bracket.replayResults(savedResults);
        return bracket;
    }

    public List<SavedFirstRoundMatch> describeFirstRound() {
        return rounds.get(0).stream()
                .map(match -> new SavedFirstRoundMatch(match.getMatchId(), match.getFirstTeamName(),
                        match.getSecondTeamName()))
                .toList();
    }

    public List<BracketMatch> getMatchesInDecisionOrder() {
        return List.copyOf(matchesInDecisionOrder);
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
        matchesInDecisionOrder.add(match);
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

    private void replayResults(List<SavedMatchResult> savedResults) {
        for (SavedMatchResult savedResult : savedResults) {
            BracketMatch match = matchesById.get(savedResult.matchId());
            if (match == null) {
                throw new IllegalArgumentException("a result names unknown match " + savedResult.matchId());
            }
            if (match.isDecided()) {
                throw new IllegalArgumentException(matchesInDecisionOrder.contains(match)
                        ? "match " + match.getMatchId() + " is decided twice"
                        : "match " + match.getMatchId() + " is decided by a bye and takes no result");
            }
            if (!match.hasBothParticipants()) {
                throw new IllegalArgumentException("match " + match.getMatchId()
                        + " is decided before both of its teams are known");
            }
            if (!match.containsTeam(savedResult.winnerTeamName())) {
                throw new IllegalArgumentException(savedResult.winnerTeamName() + " does not take part in match "
                        + match.getMatchId());
            }
            recordWinner(match.getMatchId(), savedResult.winnerTeamName());
        }
    }

    private static void placeFirstRoundTeam(BracketMatch match, int slotIndex, Optional<String> teamName,
                                            Set<String> placedTeamKeys, List<String> teamNames) {
        if (teamName.isEmpty()) {
            return;
        }
        if (teamName.get().isBlank()) {
            throw new IllegalArgumentException("match " + match.getMatchId() + " holds a team without a name");
        }
        if (!placedTeamKeys.add(teamName.get().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("team " + teamName.get() + " stands in the first round twice");
        }
        match.assignParticipant(slotIndex, teamName.get());
        teamNames.add(teamName.get());
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

    private static List<List<BracketMatch>> buildRounds(int bracketSize) {
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
        return rounds;
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
