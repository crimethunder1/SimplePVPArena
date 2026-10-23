package de.simplepvparena.placement;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchScore;

import java.util.Objects;

public record PlacementMatch(PlacementTeam firstTeam, int firstTeamPoints, PlacementTeam secondTeam,
                             int secondTeamPoints) {

    public PlacementMatch {
        Objects.requireNonNull(firstTeam, "firstTeam");
        Objects.requireNonNull(secondTeam, "secondTeam");
    }

    public static PlacementMatch of(Match match) {
        MatchScore score = match.getScore();
        return new PlacementMatch(PlacementTeam.of(match.getFirstTeam()),
                score.getPoints(score.getFirstTeamName()),
                PlacementTeam.of(match.getSecondTeam()),
                score.getPoints(score.getSecondTeamName()));
    }
}
