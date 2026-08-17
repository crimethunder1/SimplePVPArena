package de.simplepvparena.gate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StartGateDurationTest {

    @Test
    void shipsWithTenSeconds() {
        StartGateDuration duration = new StartGateDuration();

        assertEquals(10, duration.getSeconds());
        assertEquals(200L, duration.getTotalTicks());
        assertEquals("10 seconds", duration.describe());
    }

    @Test
    void takesTheSecondsOfAReloadedFile() {
        StartGateDuration duration = new StartGateDuration();

        duration.setSeconds(5);

        assertEquals(5, duration.getSeconds());
        assertEquals(100L, duration.getTotalTicks());
        assertEquals("5 seconds", duration.describe());
    }

    @Test
    void clampsADurationBelowASecond() {
        assertEquals(StartGateDuration.MINIMUM_SECONDS, new StartGateDuration(0).getSeconds());
        assertEquals(StartGateDuration.MINIMUM_SECONDS, new StartGateDuration(-30).getSeconds());
        assertEquals("1 second", new StartGateDuration(1).describe());
    }
}
