package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaSetupService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaLockedDuringCleaningTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int RESET_SLOT = 16;
    private static final int DEBUG_CELL_LIMIT = 500;

    private static final String BEING_CLEANED = "The arena is still being cleaned. Try again in a moment.";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void refusesTheCalculationWhileTheArenaIsCleaned() {
        ArenaSetupService arenaSetupService = startCleaning();
        int saveCount = fixture.arenaConfigurationSaveCount();

        arenaSetupService.calculateShape(fixture.admin());
        arenaSetupService.calculateShape(fixture.admin(), DEBUG_CELL_LIMIT);

        assertEquals(List.of(BEING_CLEANED, BEING_CLEANED), fixture.messages());
        assertEquals(saveCount, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void refusesTheResetWhileTheArenaIsCleaned() {
        ArenaSetupService arenaSetupService = startCleaning();
        int saveCount = fixture.arenaConfigurationSaveCount();

        assertEquals(Optional.of(BEING_CLEANED), arenaSetupService.findResetRejection());
        arenaSetupService.clearConfiguration(fixture.admin());

        assertEquals(List.of(BEING_CLEANED), fixture.messages());
        assertTrue(fixture.arenaRegistry().getArena().isFullyConfigured());
        assertEquals(saveCount, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void theResetTileNamesTheCleaningAndOpensNoConfirmation() {
        startCleaning();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.messages().clear();

        fixture.click(RESET_SLOT);

        assertEquals(List.of(BEING_CLEANED), fixture.messages());
        assertEquals(Optional.empty(), fixture.lastMenu().holder().getConfirmationRequest());
    }

    private ArenaSetupService startCleaning() {
        fixture.configureArena();
        fixture.standInsideTheFightArea();
        ArenaSetupService arenaSetupService = fixture.arenaSetupService();
        assertTrue(arenaSetupService.startCleaning(fixture.admin()));
        fixture.messages().clear();
        return arenaSetupService;
    }
}
