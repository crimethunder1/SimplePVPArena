package de.simplepvparena.message;

import java.util.Set;

public enum ArenaSetupMessage implements MessageKey {

    ARENA_MENU_REFERENCE("arena.menu-reference", "tile"),
    MISSING_FIGHT_AREA("arena.missing.fight-area", "action"),
    MISSING_TRIBUNE_SPAWN("arena.missing.tribune-spawn", "action"),
    SPAWN_MUST_LIE_INSIDE("arena.spawn-placement.must-lie-inside", "spawn", "position"),
    SPAWN_MUST_LIE_OUTSIDE("arena.spawn-placement.must-lie-outside", "spawn", "position"),
    SPAWN_SET_AGAIN("arena.spawn-placement.set-again", "placement"),
    SPAWN_CLICK_TILE_AGAIN("arena.spawn-placement.click-tile-again", "placement"),
    TRIBUNE_OUTSIDE_OF_THE_EFFECT_AREA("arena.tribune-outside-of-effect-area", "position", "distance"),
    EFFECT_AREA_DESCRIPTION("arena.effect-area.description", "centreX", "centreZ", "radius"),
    EFFECT_AREA_DISTANCE("arena.effect-area.distance", "distance", "effectArea"),
    EFFECT_AREA_IN_OTHER_WORLD("arena.effect-area.other-world", "world", "areaWorld", "effectArea"),
    DEBUG_CELL_LIMIT_USAGE("arena.debug-cell-limit.usage"),
    DEBUG_CELL_LIMIT_NOT_A_NUMBER("arena.debug-cell-limit.not-a-number", "value"),
    DEBUG_CELL_LIMIT_OUT_OF_RANGE("arena.debug-cell-limit.out-of-range", "minimum", "maximum"),
    ARENA_RESERVED_FOR_MATCH("arena.reserved-for-match"),
    ARENA_RESERVED_FOR_AUTO_MATCH("arena.reserved-for-auto-match"),
    CONFIGURATION_NOT_WRITTEN("arena.configuration-not-written", "reason"),
    CALCULATION_DURING_MATCH("arena.calculation.during-match"),
    DIAGNOSIS_RUN("arena.calculation.diagnosis-run", "count", "maximumCellCount", "path"),
    CALCULATED("arena.calculation.calculated", "fightArea"),
    TRIBUNE_OUTSIDE_AFTER_CALCULATION("arena.calculation.tribune-outside-of-effect-area", "warning", "path"),
    MARKED_HEIGHTS_TAKEN_OVER("arena.calculation.marked-heights-taken-over", "floorY", "topY", "action"),
    MARKED_HEIGHTS_KEPT("arena.calculation.marked-heights-kept", "floorY", "topY", "action"),
    MARKED_HEIGHTS_DROPPED("arena.calculation.marked-heights-dropped", "markedFloorY", "markedTopY", "floorY",
            "topY", "action"),
    DIAGNOSIS_LIMIT_TOO_LOW("arena.calculation.diagnosis-limit-too-low", "path", "cellCount"),
    RECOGNIZED_WALL_SHOWN("arena.calculation.recognized-wall-shown", "outline"),
    GAP_GUIDANCE("arena.gap.guidance", "target", "hint"),
    GAP_COMPASS_RECEIVED("arena.gap.compass-received", "target"),
    GAP_COMPASS_NOT_HANDED_OVER("arena.gap.compass-not-handed-over", "target"),
    BASELINE_TOO_LARGE("arena.baseline.too-large", "count", "maximum", "action"),
    BASELINE_TOO_LARGE_WITH_LAYERS("arena.baseline.too-large-with-layers", "count", "maximum", "action"),
    BASELINE_READING("arena.baseline.reading", "count", "floorY", "topY", "action", "batchSize"),
    BASELINE_READING_WITH_LAYERS("arena.baseline.reading-with-layers", "count", "floorY", "topY", "action",
            "batchSize"),
    SPAWN_SET("arena.spawn-set", "spawn", "position"),
    SPAWN_OUTSIDE_OF_THE_EFFECT_AREA("arena.spawn-outside-of-effect-area", "spawn", "position", "distance", "path"),
    RESET_DURING_MATCH("arena.reset.during-match"),
    CONFIGURATION_REMOVED("arena.reset.configuration-removed"),
    CLEANING_WITHOUT_FIGHT_AREA("arena.cleaning.without-fight-area", "action"),
    CLEANING_DURING_MATCH("arena.cleaning.during-match"),
    CLEANING_ALREADY_RUNNING("arena.cleaning.already-running"),
    CLEANING_DURING_BASELINE_CAPTURE("arena.cleaning.during-baseline-capture"),
    CLEANING_EFFECT_WITH_BASELINE("arena.cleaning.effect.with-baseline"),
    CLEANING_EFFECT_WITHOUT_BASELINE("arena.cleaning.effect.without-baseline"),
    CLEANING_COVERS_FIGHT_AREA("arena.cleaning.effect.covers-fight-area", "floorY", "topY"),
    CLEANING_COVERS_LAYERS("arena.cleaning.effect.covers-layers", "count", "floorY", "topY"),
    CLEANING_NOT_STARTED("arena.cleaning.not-started"),
    CLEANING_RESTORES_BASELINE("arena.cleaning.restores-baseline", "baseline"),
    CLEANING_WITHOUT_BASELINE("arena.cleaning.without-baseline", "action"),
    CLEANING_STARTED("arena.cleaning.started", "count", "floorY", "topY", "batchSize", "animation"),
    AUTOMATIC_CLEANING_WITHOUT_BASELINE("arena.cleaning.automatic-without-baseline", "action"),
    AUTOMATIC_CLEANING_NOT_STARTED("arena.cleaning.automatic-not-started"),
    DESCRIPTION_READY("arena.description.ready"),
    DESCRIPTION_INCOMPLETE("arena.description.incomplete"),
    DESCRIPTION_FIGHT_AREA("arena.description.fight-area", "fightArea"),
    DESCRIPTION_NO_FIGHT_AREA("arena.description.no-fight-area"),
    DESCRIPTION_SPAWN("arena.description.spawn", "spawn", "position"),
    DESCRIPTION_SPAWN_NOT_CONFIGURED("arena.description.spawn-not-configured", "spawn"),
    DESCRIPTION_EFFECT_AREA("arena.description.effect-area", "effectArea"),
    DESCRIPTION_NO_EFFECT_AREA("arena.description.no-effect-area"),
    DESCRIPTION_RUNNING_MATCH("arena.description.running-match", "match"),
    DESCRIPTION_NO_RUNNING_MATCH("arena.description.no-running-match"),
    STILL_MISSING("arena.still-missing"),
    SHAPE_DESCRIPTION("arena.shape-description", "count", "world", "minimumX", "minimumZ", "maximumX", "maximumZ",
            "floorY", "topY", "referenceY");

    private final String path;
    private final Set<String> placeholderNames;

    ArenaSetupMessage(String path, String... placeholderNames) {
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
