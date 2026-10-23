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
    BORDER_PARTICLES_TITLE("arena-menu.tile.border-particles.title"),
    BORDER_PARTICLES_STATE_SHOWN("arena-menu.tile.border-particles.state-shown"),
    BORDER_PARTICLES_STATE_HIDDEN("arena-menu.tile.border-particles.state-hidden"),
    BORDER_PARTICLES_LORE("arena-menu.tile.border-particles.lore"),
    BORDER_PARTICLES_CLICK_TO_HIDE("arena-menu.tile.border-particles.click-to-hide"),
    BORDER_PARTICLES_CLICK_TO_SHOW("arena-menu.tile.border-particles.click-to-show"),
    STANDINGS_BOARD_PLACE_TITLE("arena-menu.tile.standings-board.place-title"),
    STANDINGS_BOARD_PLACE_LORE("arena-menu.tile.standings-board.place-lore"),
    STANDINGS_BOARD_REMOVE_TITLE("arena-menu.tile.standings-board.remove-title"),
    STANDINGS_BOARD_REMOVE_LORE("arena-menu.tile.standings-board.remove-lore", "position"),
    CLEAR_BORDER_MARKS_TITLE("arena-menu.tile.clear-border-marks.title"),
    CLEAR_BORDER_MARKS_LORE("arena-menu.tile.clear-border-marks.lore"),
    CLEAR_BORDER_MARKS_MARKED("arena-menu.tile.clear-border-marks.marked", "count"),
    CLEAR_BORDER_MARKS_NONE_MARKED("arena-menu.tile.clear-border-marks.none-marked"),
    CLEAR_START_GATE_MARKS_TITLE("arena-menu.tile.clear-start-gate-marks.title"),
    CLEAR_START_GATE_MARKS_LORE("arena-menu.tile.clear-start-gate-marks.lore"),
    CLEAR_START_GATE_MARKS_MARKED("arena-menu.tile.clear-start-gate-marks.marked", "count"),
    CLEAR_START_GATE_MARKS_NONE_MARKED("arena-menu.tile.clear-start-gate-marks.none-marked"),
    SMALLER_EFFECT_AREA_TITLE("arena-menu.tile.smaller-effect-area.title"),
    LARGER_EFFECT_AREA_TITLE("arena-menu.tile.larger-effect-area.title"),
    EFFECT_MARGIN_STATE("arena-menu.tile.effect-margin.state", "count"),
    EFFECT_AREA_RADIUS("arena-menu.tile.effect-margin.radius", "radius"),
    EFFECT_MARGIN_STEPS("arena-menu.tile.effect-margin.steps"),
    SMALLEST_EFFECT_MARGIN("arena-menu.effect-margin.smallest", "count"),
    LARGEST_EFFECT_MARGIN("arena-menu.effect-margin.largest", "count"),
    EFFECT_MARGIN_CHANGED("arena-menu.effect-margin.changed", "count", "radius"),
    EFFECT_MARGIN_CHANGED_WITHOUT_FIGHT_AREA("arena-menu.effect-margin.changed-without-fight-area", "count"),
    EFFECT_MARGIN_NOT_WRITTEN("arena-menu.effect-margin.not-written", "change", "reason"),
    STANDINGS_BOARD_PLACED("arena-menu.standings-board.placed"),
    STANDINGS_BOARD_REMOVED("arena-menu.standings-board.removed"),
    BORDER_PARTICLES_SWITCHED_TO_SHOWN("arena-menu.border-particles.switched-to-shown"),
    BORDER_PARTICLES_SWITCHED_TO_HIDDEN("arena-menu.border-particles.switched-to-hidden"),
    BORDER_PARTICLES_SWITCHED_TO_SHOWN_NOT_WRITTEN("arena-menu.border-particles.switched-to-shown-not-written",
            "reason"),
    BORDER_PARTICLES_SWITCHED_TO_HIDDEN_NOT_WRITTEN("arena-menu.border-particles.switched-to-hidden-not-written",
            "reason"),
    AFFECTED_BLOCKS("arena-menu.cleaning.affected-blocks", "count"),
    CLEAN_CONFIRMATION("arena-menu.clean-confirmation"),
    RESET_CONFIRMATION("arena-menu.reset-confirmation"),
    CLEAR_BORDER_MARKS_CONFIRMATION("arena-menu.clear-marks-confirmation.border-forgotten", "count"),
    CLEAR_START_GATE_MARKS_CONFIRMATION("arena-menu.clear-marks-confirmation.start-gate-forgotten", "count"),
    CLEAR_MARKS_CONFIRMATION_BLOCKS_STAY("arena-menu.clear-marks-confirmation.blocks-stay"),
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
