package de.simplepvparena.message;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FailureDescriptionsTest {

    @Test
    void keepsTheMessageOfAFailureThatHasOne() {
        assertEquals("disk full", FailureDescriptions.describe(new IOException("disk full")));
    }

    @Test
    void namesAFailureWithoutAMessageByItsClass() {
        assertEquals("IOException", FailureDescriptions.describe(new IOException()));
        assertEquals("IllegalStateException", FailureDescriptions.describe(new IllegalStateException()));
    }

    @Test
    void keepsAnEmptyMessageAsItIs() {
        assertEquals("", FailureDescriptions.describe(new IOException("")));
    }
}
