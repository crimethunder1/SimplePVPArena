package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaBorderConfirmationTest {

    private boolean calculatedShapePresent = true;

    private final ArenaBorderConfirmation confirmation =
            new ArenaBorderConfirmation(() -> calculatedShapePresent);

    @Test
    void startsUnconfirmed() {
        assertFalse(confirmation.isConfirmed());
    }

    @Test
    void isConfirmedByASuccessfulCalculation() {
        confirmation.confirmCalculation();

        assertTrue(confirmation.isConfirmed());
    }

    @Test
    void isDroppedByAChangedBorderMark() {
        confirmation.confirmCalculation();

        confirmation.invalidate();

        assertFalse(confirmation.isConfirmed());
    }

    @Test
    void isConfirmedAgainByTheNextCalculation() {
        confirmation.confirmCalculation();
        confirmation.invalidate();

        confirmation.confirmCalculation();

        assertTrue(confirmation.isConfirmed());
    }

    @Test
    void staysDroppedWhileTheBorderKeepsChanging() {
        confirmation.confirmCalculation();
        confirmation.invalidate();
        confirmation.invalidate();
        confirmation.invalidate();

        assertFalse(confirmation.isConfirmed());
    }

    @Test
    void isNotConfirmedWithoutACalculatedShape() {
        confirmation.confirmCalculation();

        calculatedShapePresent = false;

        assertFalse(confirmation.isConfirmed());
    }

    @Test
    void isNotConfirmedByACalculatedShapeAlone() {
        assertTrue(calculatedShapePresent);

        assertFalse(confirmation.isConfirmed());
    }

    @Test
    void staysUnconfirmedWhenAShapeReturnsWithoutACalculation() {
        confirmation.confirmCalculation();
        calculatedShapePresent = false;
        confirmation.invalidate();

        calculatedShapePresent = true;

        assertFalse(confirmation.isConfirmed());
    }
}
