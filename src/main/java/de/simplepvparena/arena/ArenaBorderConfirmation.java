package de.simplepvparena.arena;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class ArenaBorderConfirmation {

    private final BooleanSupplier calculatedShapePresent;

    private boolean unchangedSinceCalculation;

    public ArenaBorderConfirmation(BooleanSupplier calculatedShapePresent) {
        this.calculatedShapePresent = Objects.requireNonNull(calculatedShapePresent, "calculatedShapePresent");
    }

    public void confirmCalculation() {
        unchangedSinceCalculation = true;
    }

    public void invalidate() {
        unchangedSinceCalculation = false;
    }

    public boolean isConfirmed() {
        return unchangedSinceCalculation && calculatedShapePresent.getAsBoolean();
    }
}
