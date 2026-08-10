package de.simplepvparena.arena;

import java.util.Optional;

public final class ArenaShapeCalculation {

    public enum Status {
        FILLED,
        SEED_INSIDE_BORDER,
        LEAKING
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

    public static ArenaShapeCalculation leaking(int visitedCellCount) {
        return new ArenaShapeCalculation(Status.LEAKING, null,
                "The border wall is not closed or too large, the arena could not be calculated. The "
                        + "calculation was stopped after " + visitedCellCount + " cells, close the wall "
                        + "around your position and run the command again");
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
