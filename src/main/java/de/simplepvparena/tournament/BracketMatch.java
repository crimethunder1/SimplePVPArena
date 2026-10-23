package de.simplepvparena.tournament;

import de.simplepvparena.team.TeamRegistry;

import java.util.Objects;
import java.util.Optional;

public final class BracketMatch {

    public static final int FIRST_SLOT_INDEX = 0;
    public static final int SECOND_SLOT_INDEX = 1;

    private final String matchId;
    private final int roundIndex;
    private final int positionIndex;

    private String firstTeamName;
    private String secondTeamName;
    private String winnerTeamName;

    BracketMatch(String matchId, int roundIndex, int positionIndex) {
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.roundIndex = roundIndex;
        this.positionIndex = positionIndex;
    }

    public String getMatchId() {
        return matchId;
    }

    public int getRoundIndex() {
        return roundIndex;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public Optional<String> getFirstTeamName() {
        return Optional.ofNullable(firstTeamName);
    }

    public Optional<String> getSecondTeamName() {
        return Optional.ofNullable(secondTeamName);
    }

    public Optional<String> getWinnerTeamName() {
        return Optional.ofNullable(winnerTeamName);
    }

    public boolean hasBothParticipants() {
        return firstTeamName != null && secondTeamName != null;
    }

    public boolean hasExactlyOneParticipant() {
        return firstTeamName != null ^ secondTeamName != null;
    }

    public boolean isFirstRoundBye() {
        return roundIndex == 0 && hasExactlyOneParticipant();
    }

    public boolean isDecided() {
        return winnerTeamName != null;
    }

    public boolean isPlayable() {
        return hasBothParticipants() && !isDecided();
    }

    public boolean containsTeam(String teamName) {
        return findTeamName(teamName).isPresent();
    }

    public Optional<String> findTeamName(String teamName) {
        if (TeamRegistry.isSameTeamName(teamName, firstTeamName)) {
            return getFirstTeamName();
        }
        if (TeamRegistry.isSameTeamName(teamName, secondTeamName)) {
            return getSecondTeamName();
        }
        return Optional.empty();
    }

    public Optional<String> getOpponentOf(String teamName) {
        if (TeamRegistry.isSameTeamName(teamName, firstTeamName)) {
            return getSecondTeamName();
        }
        if (TeamRegistry.isSameTeamName(teamName, secondTeamName)) {
            return getFirstTeamName();
        }
        return Optional.empty();
    }

    void assignParticipant(int slotIndex, String teamName) {
        Objects.requireNonNull(teamName, "teamName");
        if (slotIndex == FIRST_SLOT_INDEX) {
            firstTeamName = teamName;
            return;
        }
        if (slotIndex == SECOND_SLOT_INDEX) {
            secondTeamName = teamName;
            return;
        }
        throw new IllegalArgumentException("slotIndex must be " + FIRST_SLOT_INDEX + " or " + SECOND_SLOT_INDEX);
    }

    void setWinnerTeamName(String teamName) {
        winnerTeamName = Objects.requireNonNull(teamName, "teamName");
    }

    void clearWinnerTeamName() {
        winnerTeamName = null;
    }

    void clearParticipant(int slotIndex) {
        if (slotIndex == FIRST_SLOT_INDEX) {
            firstTeamName = null;
            return;
        }
        if (slotIndex == SECOND_SLOT_INDEX) {
            secondTeamName = null;
            return;
        }
        throw new IllegalArgumentException("slotIndex must be " + FIRST_SLOT_INDEX + " or " + SECOND_SLOT_INDEX);
    }

    String getSingleParticipant() {
        return firstTeamName != null ? firstTeamName : secondTeamName;
    }

    @Override
    public String toString() {
        return matchId + " " + getFirstTeamName().orElse("-") + " vs " + getSecondTeamName().orElse("-");
    }
}
