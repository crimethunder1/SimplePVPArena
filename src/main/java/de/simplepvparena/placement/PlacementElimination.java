package de.simplepvparena.placement;

import java.util.List;
import java.util.Objects;

public record PlacementElimination(int roundNumber, List<PlacementTeam> teams) {

    public PlacementElimination {
        teams = List.copyOf(Objects.requireNonNull(teams, "teams"));
    }
}
