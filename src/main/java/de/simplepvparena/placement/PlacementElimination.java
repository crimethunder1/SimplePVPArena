package de.simplepvparena.placement;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Objects;

@NullMarked
public record PlacementElimination(int roundNumber, List<PlacementTeam> teams) {

    public PlacementElimination {
        teams = List.copyOf(Objects.requireNonNull(teams, "teams"));
    }
}
