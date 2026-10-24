package de.simplepvparena.placement;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PlacementStandings(PlacementPhase phase,
                                 int roundNumber,
                                 int roundCount,
                                 Optional<PlacementMatch> runningMatch,
                                 List<PlacementTeam> remainingTeams,
                                 List<PlacementElimination> eliminations,
                                 List<PlacementRank> ranks) {

    public PlacementStandings {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(runningMatch, "runningMatch");
        remainingTeams = List.copyOf(Objects.requireNonNull(remainingTeams, "remainingTeams"));
        eliminations = List.copyOf(Objects.requireNonNull(eliminations, "eliminations"));
        ranks = List.copyOf(Objects.requireNonNull(ranks, "ranks"));
    }

    public static PlacementStandings none() {
        return new PlacementStandings(PlacementPhase.NONE, 0, 0, Optional.empty(), List.of(), List.of(), List.of());
    }

    public static PlacementStandings tournament(PlacementPhase phase, int roundNumber, int roundCount,
                                                Optional<PlacementMatch> runningMatch,
                                                List<PlacementTeam> remainingTeams,
                                                List<PlacementElimination> eliminations,
                                                List<PlacementRank> ranks) {
        if (!phase.isTournament()) {
            throw new IllegalArgumentException("phase " + phase + " belongs to no tournament");
        }
        return new PlacementStandings(phase, roundNumber, roundCount, runningMatch, remainingTeams, eliminations,
                ranks);
    }

    public boolean hasContent() {
        return phase != PlacementPhase.NONE;
    }
}
