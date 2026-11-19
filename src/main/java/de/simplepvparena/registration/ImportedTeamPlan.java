package de.simplepvparena.registration;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

@NullMarked
public final class ImportedTeamPlan {

    private ImportedTeamPlan() {
    }

    public static <T> List<List<T>> plan(List<List<T>> groups, int teamSize, RandomGenerator randomGenerator) {
        Objects.requireNonNull(groups, "groups");
        Objects.requireNonNull(randomGenerator, "randomGenerator");
        if (teamSize < 1) {
            throw new IllegalArgumentException("the team size must be at least 1, got " + teamSize);
        }
        List<List<T>> shuffledGroups = new ArrayList<>(groups.stream().filter(group -> !group.isEmpty()).toList());
        Collections.shuffle(shuffledGroups, randomGenerator);
        shuffledGroups.sort(Comparator.comparingInt(List<T>::size).reversed());
        List<List<T>> teams = new ArrayList<>();
        for (List<T> group : shuffledGroups) {
            if (group.size() > teamSize) {
                throw new IllegalArgumentException("a group of " + group.size()
                        + " players does not fit into a team of " + teamSize);
            }
            List<T> openTeam = teams.stream()
                    .filter(team -> team.size() + group.size() <= teamSize)
                    .findFirst()
                    .orElse(null);
            if (openTeam == null) {
                teams.add(new ArrayList<>(group));
                continue;
            }
            openTeam.addAll(group);
        }
        return teams.stream().map(List::copyOf).toList();
    }
}
