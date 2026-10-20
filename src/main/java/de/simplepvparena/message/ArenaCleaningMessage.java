package de.simplepvparena.message;

import java.util.Set;

public enum ArenaCleaningMessage implements MessageKey {

    BASELINE_TAKEN("arena-cleaning.baseline.taken", "baseline", "cleanAction"),
    BASELINE_NOT_TAKEN("arena-cleaning.baseline.not-taken", "reason", "cleanAction", "calculateAction"),
    BASELINE_WORLD_NOT_LOADED("arena-cleaning.baseline.world-not-loaded", "world"),
    BASELINE_BLOCKS_UNREADABLE("arena-cleaning.baseline.blocks-unreadable", "cause"),
    BASELINE_TOO_MANY_BLOCK_STATES("arena-cleaning.baseline.too-many-block-states", "maximum"),
    BASELINE_DESCRIPTION("arena-cleaning.baseline-description.summary", "blocks", "materials", "notAir", "contents"),
    BASELINE_DESCRIPTION_BLOCKS("arena-cleaning.baseline-description.blocks", "count", "depth"),
    BASELINE_DESCRIPTION_DEPTH("arena-cleaning.baseline-description.depth", "count"),
    BASELINE_DESCRIPTION_MATERIALS("arena-cleaning.baseline-description.materials", "count", "blockStates"),
    BASELINE_DESCRIPTION_BLOCK_STATES("arena-cleaning.baseline-description.block-states", "count"),
    BASELINE_DESCRIPTION_NOT_AIR("arena-cleaning.baseline-description.not-air", "count"),
    BASELINE_DESCRIPTION_CONTENTS("arena-cleaning.baseline-description.contents", "count"),
    BASELINE_MISMATCH("arena-cleaning.baseline-mismatch", "baselineArea", "area", "action"),
    ANIMATION_SWITCHED_OFF("arena-cleaning.animation.switched-off"),
    ANIMATION_RUNNING("arena-cleaning.animation.running", "seconds"),
    CLEANING_WORLD_NOT_LOADED("arena-cleaning.world-not-loaded", "world"),
    REPORT_CLEAN("arena-cleaning.report.clean", "report"),
    REPORT_STOPPED("arena-cleaning.report.stopped", "reason", "report", "action"),
    REPORT_UNCHANGED("arena-cleaning.report.unchanged", "count", "borderMarks"),
    REPORT_CHANGED("arena-cleaning.report.changed", "blocks", "borderMarks"),
    REPORT_SET_TO_AIR("arena-cleaning.report.set-to-air", "count", "checkedCount"),
    REPORT_RESTORED("arena-cleaning.report.restored", "count", "checkedCount"),
    REPORT_BORDER_MARKS_REMOVED("arena-cleaning.report.border-marks-removed", "count");

    private final String path;
    private final Set<String> placeholderNames;

    ArenaCleaningMessage(String path, String... placeholderNames) {
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
