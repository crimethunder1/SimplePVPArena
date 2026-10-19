package de.simplepvparena.message;

import java.util.Set;

public enum ArenaMenuMessage implements MessageKey {

    TITLE("arena-menu.title"),
    INFO_TITLE("arena-menu.tile.info.title"),
    CALCULATE_TITLE("arena-menu.tile.calculate.title"),
    CALCULATE_LORE("arena-menu.tile.calculate.lore"),
    DIAGNOSE_TITLE("arena-menu.tile.diagnose.title"),
    DIAGNOSE_LIMIT("arena-menu.tile.diagnose.limit", "count"),
    DIAGNOSE_LORE("arena-menu.tile.diagnose.lore"),
    FIRST_TEAM_SPAWN_TITLE("arena-menu.tile.first-team-spawn.title"),
    SECOND_TEAM_SPAWN_TITLE("arena-menu.tile.second-team-spawn.title"),
    TRIBUNE_SPAWN_TITLE("arena-menu.tile.tribune-spawn.title"),
    SPAWN_LORE("arena-menu.tile.spawn.lore", "placement"),
    CLEAN_TITLE("arena-menu.tile.clean.title"),
    CLEAN_WITHOUT_FIGHT_AREA("arena-menu.tile.clean.without-fight-area"),
    RESET_TITLE("arena-menu.tile.reset.title"),
    RESET_LORE("arena-menu.tile.reset.lore"),
    HEIGHT_MARKER_TITLE("arena-menu.tile.height-marker.title"),
    HEIGHT_MARKER_LORE("arena-menu.tile.height-marker.lore"),
    START_GATE_TITLE("arena-menu.tile.start-gate.title"),
    START_GATE_LORE("arena-menu.tile.start-gate.lore"),
    AFFECTED_BLOCKS("arena-menu.cleaning.affected-blocks", "count"),
    CLEAN_CONFIRMATION("arena-menu.clean-confirmation"),
    RESET_CONFIRMATION("arena-menu.reset-confirmation"),
    HEIGHT_MARKER_INVENTORY_FULL("arena-menu.height-marker.inventory-full"),
    HEIGHT_MARKER_HANDED_OUT("arena-menu.height-marker.handed-out"),
    START_GATE_INPUT_DESCRIPTION("arena-menu.start-gate-input.description"),
    START_GATE_INPUT_PROMPT("arena-menu.start-gate-input.prompt", "usage", "minimum", "maximum"),
    START_GATE_INVENTORY_FULL("arena-menu.start-gate.inventory-full"),
    START_GATE_HANDED_OUT("arena-menu.start-gate.handed-out", "count", "material"),
    DIAGNOSIS_INPUT_DESCRIPTION("arena-menu.diagnosis-input.description"),
    DIAGNOSIS_INPUT_PROMPT("arena-menu.diagnosis-input.prompt", "usage", "minimum", "maximum");

    private final String path;
    private final Set<String> placeholderNames;

    ArenaMenuMessage(String path, String... placeholderNames) {
        this.path = path;
        this.placeholderNames = Set.of(placeholderNames);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Set<String> placeholderNames() {
        return placeholderNames;
    }
}
