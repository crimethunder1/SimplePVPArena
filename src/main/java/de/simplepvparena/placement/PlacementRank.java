package de.simplepvparena.placement;

import java.util.Objects;

public record PlacementRank(int place, PlacementTeam team) {

    public PlacementRank {
        Objects.requireNonNull(team, "team");
    }
}
