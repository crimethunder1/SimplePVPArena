package de.simplepvparena.message;

import java.util.Set;

public enum ArenaGapMessage implements MessageKey {

    SEED_INSIDE_BORDER("arena-gap.seed-inside-border"),
    STOPPED_AT_CELL_LIMIT("arena-gap.cell-limit.stopped", "count", "path"),
    GAP_OR_TOO_LARGE("arena-gap.cell-limit.gap-or-too-large"),
    SPREAD("arena-gap.cell-limit.spread", "direction", "boundingBox", "front"),
    NOT_SPREAD("arena-gap.cell-limit.not-spread", "boundingBox", "front"),
    WALK_ALONG_TOWARDS("arena-gap.cell-limit.walk-along-towards", "direction", "path", "action"),
    WALK_ALONG("arena-gap.cell-limit.walk-along", "path", "action"),
    DIAGNOSE_HINT("arena-gap.cell-limit.diagnose-hint", "action", "count"),
    HEIGHT_MISMATCH_ALL_LISTED("arena-gap.height-mismatch.all-listed", "count", "columns"),
    HEIGHT_MISMATCH_NEAREST_LISTED("arena-gap.height-mismatch.nearest-listed", "count", "listedCount", "columns"),
    HEIGHT_MISMATCH_EXPLANATION("arena-gap.height-mismatch.explanation", "action"),
    HEIGHT_MISMATCH_COLUMN("arena-gap.height-mismatch.column", "blockX", "blockZ", "lowestBorderY", "heightAboveSeed"),
    DIRECTION_EAST("arena-gap.direction.east"),
    DIRECTION_WEST("arena-gap.direction.west"),
    DIRECTION_SOUTH("arena-gap.direction.south"),
    DIRECTION_NORTH("arena-gap.direction.north"),
    DIRECTION_NORTH_EAST("arena-gap.direction.north-east"),
    DIRECTION_NORTH_WEST("arena-gap.direction.north-west"),
    DIRECTION_SOUTH_EAST("arena-gap.direction.south-east"),
    DIRECTION_SOUTH_WEST("arena-gap.direction.south-west"),
    DIRECTION_NOWHERE("arena-gap.direction.nowhere"),
    ESCAPE_NOWHERE("arena-gap.escape.nowhere"),
    ESCAPE_DIRECTION("arena-gap.escape.direction", "direction", "horizontal", "vertical", "farthestX", "farthestZ"),
    COLUMNS_EAST("arena-gap.columns.east", "count"),
    COLUMNS_WEST("arena-gap.columns.west", "count"),
    COLUMNS_SOUTH("arena-gap.columns.south", "count"),
    COLUMNS_NORTH("arena-gap.columns.north", "count"),
    BOUNDING_BOX("arena-gap.bounding-box", "minimumX", "maximumX", "minimumZ", "maximumZ", "width", "length"),
    FILL_FRONT_LORE("arena-gap.reason.fill-front.lore"),
    FILL_FRONT_HAND_OUT_HINT("arena-gap.reason.fill-front.hand-out-hint"),
    FILL_FRONT_ARRIVAL_HINT("arena-gap.reason.fill-front.arrival-hint"),
    BORDER_TOO_HIGH_LORE("arena-gap.reason.border-too-high.lore"),
    BORDER_TOO_HIGH_HAND_OUT_HINT("arena-gap.reason.border-too-high.hand-out-hint"),
    BORDER_TOO_HIGH_ARRIVAL_HINT("arena-gap.reason.border-too-high.arrival-hint"),
    TARGET("arena-gap.target", "blockX", "blockY", "blockZ", "world"),
    REACHED("arena-gap.reached", "target", "hint"),
    COMPASS_NAME("arena-gap.compass.name"),
    COMPASS_LORE("arena-gap.compass.lore", "target", "reason");

    private final String path;
    private final Set<String> placeholderNames;

    ArenaGapMessage(String path, String... placeholderNames) {
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
