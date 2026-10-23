package de.simplepvparena.placement;

import java.util.Objects;

public record AutoMatchWinCount(PlacementTeam team, int wins) {

    public AutoMatchWinCount {
        Objects.requireNonNull(team, "team");
    }
}
