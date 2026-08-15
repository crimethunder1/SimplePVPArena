package de.simplepvparena.border;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderMarkToggleTest {

    @Test
    void marksABlockThatIsNotABorderBlockYet() {
        BorderMarkToggle toggle = BorderMarkToggle.decide(false);

        assertEquals(BorderMarkToggle.MARKED, toggle);
        assertTrue(toggle.marks());
    }

    @Test
    void unmarksABlockThatIsAlreadyABorderBlock() {
        BorderMarkToggle toggle = BorderMarkToggle.decide(true);

        assertEquals(BorderMarkToggle.UNMARKED, toggle);
        assertFalse(toggle.marks());
    }

    @Test
    void takesBackItsOwnMarkWithTheNextClick() {
        boolean markedBlock = false;

        BorderMarkToggle firstClick = BorderMarkToggle.decide(markedBlock);
        markedBlock = firstClick.marks();
        BorderMarkToggle secondClick = BorderMarkToggle.decide(markedBlock);
        markedBlock = secondClick.marks();

        assertEquals(BorderMarkToggle.MARKED, firstClick);
        assertEquals(BorderMarkToggle.UNMARKED, secondClick);
        assertFalse(markedBlock);
    }

    @Test
    void namesTheTwoOutcomesApart() {
        assertNotEquals(BorderMarkToggle.MARKED.describe(), BorderMarkToggle.UNMARKED.describe());
        assertFalse(BorderMarkToggle.MARKED.describe().isBlank());
        assertFalse(BorderMarkToggle.UNMARKED.describe().isBlank());
    }
}
