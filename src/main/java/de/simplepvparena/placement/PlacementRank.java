package de.simplepvparena.placement;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public record PlacementRank(int place, PlacementTeam team) {

    public PlacementRank {
        Objects.requireNonNull(team, "team");
    }
}
