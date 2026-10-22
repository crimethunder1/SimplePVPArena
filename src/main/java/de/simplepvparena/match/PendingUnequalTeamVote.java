package de.simplepvparena.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.random.RandomGenerator;

public final class PendingUnequalTeamVote {

    private final String matchId;
    private final String votingTeamName;
    private final String opponentTeamName;
    private final int fighterCount;
    private final Map<UUID, String> voterNamesByUniqueId;
    private final Map<UUID, UUID> candidatesByVoter = new LinkedHashMap<>();

    private int secondsLeft;

    PendingUnequalTeamVote(String matchId, String votingTeamName, String opponentTeamName, int fighterCount,
                           Map<UUID, String> voterNamesByUniqueId, int secondsLeft) {
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.votingTeamName = Objects.requireNonNull(votingTeamName, "votingTeamName");
        this.opponentTeamName = Objects.requireNonNull(opponentTeamName, "opponentTeamName");
        if (fighterCount < 1) {
            throw new IllegalArgumentException("at least one player of the voting team fights");
        }
        this.fighterCount = fighterCount;
        this.voterNamesByUniqueId = new LinkedHashMap<>(voterNamesByUniqueId);
        this.secondsLeft = secondsLeft;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getVotingTeamName() {
        return votingTeamName;
    }

    public String getOpponentTeamName() {
        return opponentTeamName;
    }

    public int getFighterCount() {
        return fighterCount;
    }

    public int getVoterCount() {
        return voterNamesByUniqueId.size();
    }

    public int getSecondsLeft() {
        return secondsLeft;
    }

    public List<UUID> getVoterUniqueIds() {
        return List.copyOf(voterNamesByUniqueId.keySet());
    }

    public String findVoterName(UUID voterUniqueId) {
        return voterNamesByUniqueId.getOrDefault(voterUniqueId, voterUniqueId.toString());
    }

    public boolean isVoter(UUID playerUniqueId) {
        return voterNamesByUniqueId.containsKey(playerUniqueId);
    }

    public Optional<UUID> findVoteOf(UUID voterUniqueId) {
        return Optional.ofNullable(candidatesByVoter.get(voterUniqueId));
    }

    public int countVotesFor(UUID candidateUniqueId) {
        return (int) candidatesByVoter.values().stream().filter(candidateUniqueId::equals).count();
    }

    boolean hasEveryVoterVoted() {
        return candidatesByVoter.keySet().containsAll(voterNamesByUniqueId.keySet());
    }

    boolean castVote(UUID voterUniqueId, UUID candidateUniqueId) {
        if (!isVoter(voterUniqueId) || !isVoter(candidateUniqueId)) {
            return false;
        }
        candidatesByVoter.put(voterUniqueId, candidateUniqueId);
        return true;
    }

    boolean removeVoter(UUID voterUniqueId) {
        if (voterNamesByUniqueId.remove(voterUniqueId) == null) {
            return false;
        }
        candidatesByVoter.remove(voterUniqueId);
        candidatesByVoter.values().removeIf(voterUniqueId::equals);
        return true;
    }

    void countDownOneSecond() {
        secondsLeft = Math.max(0, secondsLeft - 1);
    }

    List<UUID> chooseFighters(RandomGenerator random) {
        TreeMap<Integer, List<UUID>> votersByVoteCount = new TreeMap<>(Collections.reverseOrder());
        for (UUID voterUniqueId : voterNamesByUniqueId.keySet()) {
            votersByVoteCount.computeIfAbsent(countVotesFor(voterUniqueId), voteCount -> new ArrayList<>())
                    .add(voterUniqueId);
        }
        List<UUID> fighters = new ArrayList<>();
        for (List<UUID> sameVoteCount : votersByVoteCount.values()) {
            int freePlaces = fighterCount - fighters.size();
            if (freePlaces <= 0) {
                break;
            }
            if (sameVoteCount.size() <= freePlaces) {
                fighters.addAll(sameVoteCount);
                continue;
            }
            List<UUID> drawnOrder = new ArrayList<>(sameVoteCount);
            Collections.shuffle(drawnOrder, random);
            fighters.addAll(drawnOrder.subList(0, freePlaces));
        }
        return List.copyOf(fighters);
    }
}
