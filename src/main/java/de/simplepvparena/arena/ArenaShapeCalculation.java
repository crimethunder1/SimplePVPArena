package de.simplepvparena.arena;

import java.util.Optional;

public final class ArenaShapeCalculation {

    public enum Status {
        FILLED,
        SEED_INSIDE_BORDER,
        CELL_LIMIT_REACHED
    }

    private final Status status;
    private final ArenaShape shape;
    private final String rejectionMessage;
    private final ArenaFillDiagnostics diagnostics;

    private ArenaShapeCalculation(Status status, ArenaShape shape, String rejectionMessage,
                                  ArenaFillDiagnostics diagnostics) {
        this.status = status;
        this.shape = shape;
        this.rejectionMessage = rejectionMessage;
        this.diagnostics = diagnostics;
    }

    public static ArenaShapeCalculation filled(ArenaShape shape) {
        return new ArenaShapeCalculation(Status.FILLED, shape, null, null);
    }

    public static ArenaShapeCalculation seedInsideBorder() {
        return new ArenaShapeCalculation(Status.SEED_INSIDE_BORDER, null,
                "You are standing in a border block column, run the command inside the area the border "
                        + "wall encloses", null);
    }

    public static ArenaShapeCalculation stoppedAtCellLimit(int maximumCellCount,
                                                           ArenaFillDiagnostics diagnostics) {
        String searchDirection = diagnostics.hasSpread()
                ? " towards " + diagnostics.getEscapeDirectionName()
                : "";
        return new ArenaShapeCalculation(Status.CELL_LIMIT_REACHED, null,
                "The fight area could not be calculated, the flood fill was stopped at " + maximumCellCount
                        + " cells, the limit of " + ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH
                        + ". " + describeHeightMismatch(diagnostics)
                        + "Either your border wall has a gap the fill ran through, or the area you "
                        + "enclosed is larger than that limit - the calculation cannot tell the two apart, "
                        + "it would have to keep filling to do so. The fill spread furthest "
                        + diagnostics.describeEscapeDirection() + ", it covers "
                        + diagnostics.describeBoundingBox() + ", and the last columns it reached are "
                        + diagnostics.describeFront() + ". Walk along your wall" + searchDirection
                        + " and look for a missing green connection line between two border blocks, and if "
                        + "the wall is closed everywhere raise "
                        + ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH + " in config.yml and use "
                        + Arena.CALCULATE_ACTION + " again. " + Arena.DIAGNOSE_ACTION
                        + " runs a single calculation with a higher limit, up to "
                        + ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT
                        + " cells, without changing config.yml, so you can see how far the fill really gets",
                diagnostics);
    }

    private static String describeHeightMismatch(ArenaFillDiagnostics diagnostics) {
        if (!diagnostics.hasHeightMismatch()) {
            return "";
        }
        int mismatchCount = diagnostics.getHeightMismatchColumnCount();
        int listedCount = Math.min(ArenaFillDiagnostics.REPORTED_HEIGHT_MISMATCH_COUNT, mismatchCount);
        String finding = mismatchCount == 1
                ? "One of the columns the fill walked through does hold a border block, and it sits above "
                        + "the height you ran the calculation at, so it never barred its column. It is at "
                : mismatchCount + " of the columns the fill walked through do hold a border block, and every "
                        + "one of those blocks sits above the height you ran the calculation at, so none of "
                        + "them barred its column. " + (listedCount == mismatchCount
                                ? "They are at "
                                : "The " + listedCount + " nearest of them are at ");
        return finding + diagnostics.describeHeightMismatchColumns()
                + ". A border block only bars its column from its own height upwards, so the wall is built "
                + "there and the fill still walked underneath it - that is what it looks like when you run "
                + "the calculation standing in a dip while that part of the wall stands on a rise. Build the "
                + "wall down to your own height there, or use " + Arena.CALCULATE_ACTION + " again while "
                + "standing at least as high as the wall does. ";
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSuccessful() {
        return status == Status.FILLED;
    }

    public Optional<ArenaShape> getShape() {
        return Optional.ofNullable(shape);
    }

    public Optional<String> getRejectionMessage() {
        return Optional.ofNullable(rejectionMessage);
    }

    public Optional<ArenaFillDiagnostics> getDiagnostics() {
        return Optional.ofNullable(diagnostics);
    }
}
