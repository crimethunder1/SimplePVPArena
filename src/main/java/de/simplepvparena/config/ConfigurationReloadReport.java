package de.simplepvparena.config;

import de.simplepvparena.message.ConfigMessage;
import de.simplepvparena.message.Messages;

import java.util.ArrayList;
import java.util.List;

public record ConfigurationReloadReport(List<String> problems) {

    private static final List<ConfigMessage> RELOADED_CATEGORIES = List.of(
            ConfigMessage.LANGUAGE_CATEGORY,
            ConfigMessage.ROUND_CATEGORY,
            ConfigMessage.START_GATE_CATEGORY,
            ConfigMessage.COUNTDOWN_CATEGORY,
            ConfigMessage.RECONNECT_CATEGORY,
            ConfigMessage.UNEQUAL_TEAMS_CATEGORY,
            ConfigMessage.CLEAN_BETWEEN_ROUNDS_CATEGORY,
            ConfigMessage.TEAM_SIZE_CATEGORY,
            ConfigMessage.AUTO_MATCH_CATEGORY,
            ConfigMessage.TEAM_MODE_CATEGORY,
            ConfigMessage.CLEANING_CATEGORY,
            ConfigMessage.BASELINE_DEPTH_CATEGORY,
            ConfigMessage.CELL_LIMIT_CATEGORY,
            ConfigMessage.SAFETY_ZONE_CATEGORY,
            ConfigMessage.EFFECT_MARGIN_CATEGORY,
            ConfigMessage.TELEPORT_ON_JOIN_CATEGORY,
            ConfigMessage.AREA_INVENTORY_CATEGORY,
            ConfigMessage.BORDER_CATEGORY,
            ConfigMessage.ITEMS_ADDER_CATEGORY,
            ConfigMessage.PLACEMENT_CATEGORY);

    private static final String ENTRY_INDENT = "  ";

    public ConfigurationReloadReport {
        problems = List.copyOf(problems);
    }

    public static ConfigurationReloadReport of(List<String> problems) {
        return new ConfigurationReloadReport(problems);
    }

    public boolean isClean() {
        return problems.isEmpty();
    }

    public static List<String> describeReloadedCategories() {
        return RELOADED_CATEGORIES.stream().map(Messages::text).toList();
    }

    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        lines.add(Messages.text(ConfigMessage.RELOADED));
        for (String category : describeReloadedCategories()) {
            lines.add(ENTRY_INDENT + category);
        }
        lines.add(Messages.text(ConfigMessage.UNTOUCHED));
        if (isClean()) {
            return List.copyOf(lines);
        }
        lines.add(Messages.text(ConfigMessage.UNUSABLE_ENTRIES));
        for (String problem : problems) {
            lines.add(ENTRY_INDENT + problem);
        }
        return List.copyOf(lines);
    }
}
