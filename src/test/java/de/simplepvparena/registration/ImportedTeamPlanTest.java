package de.simplepvparena.registration;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportedTeamPlanTest {

    @Test
    void keepsDuosTogetherAndPairsTheSoloPlayers() {
        List<List<String>> teams = ImportedTeamPlan.plan(List.of(List.of("Steve", "Alex"), List.of("Notch"),
                List.of("Herobrine", "Jeb_"), List.of("Dinnerbone")), 2, new Random(7));

        assertEquals(3, teams.size());
        assertTrue(teams.contains(List.of("Steve", "Alex")));
        assertTrue(teams.contains(List.of("Herobrine", "Jeb_")));
        assertTrue(teams.stream().anyMatch(team -> new HashSet<>(team).equals(Set.of("Notch", "Dinnerbone"))));
    }

    @Test
    void leavesTheLastSoloPlayerAloneWhenTheNumberIsOdd() {
        List<List<String>> teams = ImportedTeamPlan.plan(List.of(List.of("A1a"), List.of("B2b"), List.of("C3c")),
                2, new Random(3));

        assertEquals(List.of(2, 1), teams.stream().map(List::size).toList());
    }

    @Test
    void fillsLargerTeamsAroundTheGroups() {
        List<List<String>> teams = ImportedTeamPlan.plan(List.of(List.of("A1a", "A2a"), List.of("B1b", "B2b"),
                List.of("C1c"), List.of("D1d")), 3, new Random(11));

        assertEquals(List.of(3, 3), teams.stream().map(List::size).toList());
        assertTrue(teams.stream().allMatch(team -> team.containsAll(List.of("A1a", "A2a"))
                || team.containsAll(List.of("B1b", "B2b"))));
    }

    @Test
    void placesEveryPlayerExactlyOnce() {
        List<List<String>> groups = List.of(List.of("P01"), List.of("P02", "P03"), List.of("P04"), List.of("P05"),
                List.of("P06", "P07"), List.of("P08"), List.of("P09"));

        List<String> placed = ImportedTeamPlan.plan(groups, 2, new Random(42)).stream()
                .flatMap(List::stream).toList();

        assertEquals(9, placed.size());
        assertEquals(9, new HashSet<>(placed).size());
    }

    @Test
    void refusesAGroupLargerThanTheTeamSize() {
        assertThrows(IllegalArgumentException.class,
                () -> ImportedTeamPlan.plan(List.of(List.of("A1a", "B2b", "C3c")), 2, new Random(1)));
    }
}
