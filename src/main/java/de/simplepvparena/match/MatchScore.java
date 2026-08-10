package de.simplepvparena.match;

import java.util.Objects;
import java.util.Optional;

public final class MatchScore {

    public static final int POINTS_REQUIRED_FOR_BEST_OF_THREE = 2;

    private final String firstTeamName;
    private final String secondTeamName;
    private final int pointsRequiredToWin;

    private int firstTeamPoints;
    private int secondTeamPoints;

    public MatchScore(String firstTeamName, String secondTeamName) {
        this(firstTeamName, secondTeamName, POINTS_REQUIRED_FOR_BEST_OF_THREE);
    }

    public MatchScore(String firstTeamName, String secondTeamName, int pointsRequiredToWin) {
        this.firstTeamName = Objects.requireNonNull(firstTeamName, "firstTeamName");
        this.secondTeamName = Objects.requireNonNull(secondTeamName, "secondTeamName");
        if (this.firstTeamName.equals(this.secondTeamName)) {
            throw new IllegalArgumentException("a match needs two different teams");
        }
        if (pointsRequiredToWin < 1) {
            throw new IllegalArgumentException("pointsRequiredToWin must be positive");
        }
        this.pointsRequiredToWin = pointsRequiredToWin;
    }

    public void awardPoint(String teamName) {
        if (isDecided()) {
            throw new IllegalStateException("the match is already decided");
        }
        if (firstTeamName.equals(teamName)) {
            firstTeamPoints++;
            return;
        }
        if (secondTeamName.equals(teamName)) {
            secondTeamPoints++;
            return;
        }
        throw new IllegalArgumentException(teamName + " does not take part in this match");
    }

    public int getPoints(String teamName) {
        if (firstTeamName.equals(teamName)) {
            return firstTeamPoints;
        }
        if (secondTeamName.equals(teamName)) {
            return secondTeamPoints;
        }
        throw new IllegalArgumentException(teamName + " does not take part in this match");
    }

    public int getPointsRequiredToWin() {
        return pointsRequiredToWin;
    }

    public boolean isDecided() {
        return firstTeamPoints >= pointsRequiredToWin || secondTeamPoints >= pointsRequiredToWin;
    }

    public Optional<String> getWinnerTeamName() {
        if (firstTeamPoints >= pointsRequiredToWin) {
            return Optional.of(firstTeamName);
        }
        if (secondTeamPoints >= pointsRequiredToWin) {
            return Optional.of(secondTeamName);
        }
        return Optional.empty();
    }

    public String getFirstTeamName() {
        return firstTeamName;
    }

    public String getSecondTeamName() {
        return secondTeamName;
    }

    public String describe() {
        return firstTeamName + " " + firstTeamPoints + " : " + secondTeamPoints + " " + secondTeamName;
    }
}
