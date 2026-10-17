package de.simplepvparena.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpectatorBorderListenerTest {

    @Test
    void refusesToBeBuiltWithoutItsServices() {
        ArenaMatchHarness harness = new ArenaMatchHarness();

        NullPointerException missingMatchService = assertThrows(NullPointerException.class,
                () -> new SpectatorBorderListener(null, harness.getSpectatorService(), harness.getBorderService()));
        NullPointerException missingSpectatorService = assertThrows(NullPointerException.class,
                () -> new SpectatorBorderListener(harness.getMatchService(), null, harness.getBorderService()));
        NullPointerException missingBorderService = assertThrows(NullPointerException.class,
                () -> new SpectatorBorderListener(harness.getMatchService(), harness.getSpectatorService(), null));

        assertEquals("matchService", missingMatchService.getMessage());
        assertEquals("spectatorService", missingSpectatorService.getMessage());
        assertEquals("spectatorBorderService", missingBorderService.getMessage());
    }
}
