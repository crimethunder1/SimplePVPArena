package de.simplepvparena.config;

import java.util.ArrayList;
import java.util.List;

public record ConfigurationReloadReport(List<String> problems) {

    public static final List<String> RELOADED_CATEGORIES = List.of(
            "the kit every fighter receives",
            "the round reset delay, the round timeout and the sudden death damage",
            "the cleaning and baseline batch sizes",
            "the cell limit of the arena calculation",
            "the radius of the safety zone",
            "the border particles, their limits and the confirmed border marker");

    private static final String UNTOUCHED_PARTS =
            "The calculated fight area, the spawns, the border blocks, the teams and a running match "
                    + "stay untouched";

    public ConfigurationReloadReport {
        problems = List.copyOf(problems);
    }

    public static ConfigurationReloadReport of(List<String> problems) {
        return new ConfigurationReloadReport(problems);
    }

    public boolean isClean() {
        return problems.isEmpty();
    }

    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        lines.add("config.yml was reloaded and applied");
        for (String category : RELOADED_CATEGORIES) {
            lines.add("  " + category);
        }
        lines.add(UNTOUCHED_PARTS);
        if (isClean()) {
            return List.copyOf(lines);
        }
        lines.add("Entries that could not be used and fell back to their default:");
        for (String problem : problems) {
            lines.add("  " + problem);
        }
        return List.copyOf(lines);
    }
}
