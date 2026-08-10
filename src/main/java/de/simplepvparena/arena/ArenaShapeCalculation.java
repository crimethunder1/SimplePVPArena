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

    private ArenaShapeCalculation(Status status, ArenaShape shape, String rejectionMessage) {
        this.status = status;
        this.shape = shape;
        this.rejectionMessage = rejectionMessage;
    }

    public static ArenaShapeCalculation filled(ArenaShape shape) {
        return new ArenaShapeCalculation(Status.FILLED, shape, null);
    }

    public static ArenaShapeCalculation seedInsideBorder() {
        return new ArenaShapeCalculation(Status.SEED_INSIDE_BORDER, null,
                "You are standing in a border block column, run the command inside the area the border "
                        + "wall encloses");
    }

    public static ArenaShapeCalculation stoppedAtCellLimit(int maximumCellCount) {
        return new ArenaShapeCalculation(Status.CELL_LIMIT_REACHED, null,
                "The fight area could not be calculated, the flood fill was stopped at " + maximumCellCount
                        + " cells, the limit of " + ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH
                        + ". Either your border wall has a gap the fill ran through, or the area you "
                        + "enclosed is larger than that limit - the calculation cannot tell the two apart, "
                        + "it would have to keep filling to do so. Walk along your wall and look for a "
                        + "missing green connection line between two border blocks, and if the wall is "
                        + "closed everywhere raise " + ArenaShapeFloodFill.MAXIMUM_CELL_COUNT_CONFIGURATION_PATH
                        + " in config.yml and run " + Arena.CALCULATE_COMMAND + " again");
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
}
