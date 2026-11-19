package de.simplepvparena.registration;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@NullMarked
public record TeamImportFile(List<List<String>> groups, List<TeamImportProblem> problems) {

    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final Pattern NAME_SEPARATOR = Pattern.compile("[\\s,;]+");

    public TeamImportFile {
        groups = groups.stream().map(List::copyOf).toList();
        problems = List.copyOf(problems);
    }

    public static TeamImportFile parse(List<String> lines, int teamSize) {
        Objects.requireNonNull(lines, "lines");
        if (teamSize < 1) {
            throw new IllegalArgumentException("the team size must be at least 1, got " + teamSize);
        }
        List<List<String>> groups = new ArrayList<>();
        List<TeamImportProblem> problems = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        for (int index = 0; index < lines.size(); index++) {
            int lineNumber = index + 1;
            String line = lines.get(index).strip();
            if (index == 0 && line.startsWith("﻿")) {
                line = line.substring(1).strip();
            }
            if (line.isEmpty()) {
                continue;
            }
            List<String> group = new ArrayList<>();
            for (String name : NAME_SEPARATOR.split(line)) {
                if (name.isEmpty()) {
                    continue;
                }
                if (!PLAYER_NAME.matcher(name).matches()) {
                    problems.add(new TeamImportProblem(TeamImportProblem.Kind.INVALID_NAME, lineNumber, name));
                    continue;
                }
                if (!seenNames.add(name.toLowerCase(Locale.ROOT))) {
                    problems.add(new TeamImportProblem(TeamImportProblem.Kind.NAMED_TWICE, lineNumber, name));
                    continue;
                }
                group.add(name);
            }
            if (group.size() > teamSize) {
                problems.add(new TeamImportProblem(TeamImportProblem.Kind.GROUP_TOO_LARGE, lineNumber,
                        String.join(" ", group)));
                for (int first = 0; first < group.size(); first += teamSize) {
                    groups.add(group.subList(first, Math.min(first + teamSize, group.size())));
                }
                continue;
            }
            if (!group.isEmpty()) {
                groups.add(group);
            }
        }
        return new TeamImportFile(groups, problems);
    }

    public List<String> playerNames() {
        return groups.stream().flatMap(List::stream).toList();
    }
}
