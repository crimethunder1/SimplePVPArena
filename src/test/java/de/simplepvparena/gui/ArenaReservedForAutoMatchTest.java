package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaSetupService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArenaReservedForAutoMatchTest {

    private static final String RESERVED_FOR_AUTO_MATCH = "An auto match is about to start, the arena is reserved "
            + "for it";
    private static final String RESERVED_FOR_KIT_CHOICE = "The kit for the next match is being chosen, the arena is "
            + "reserved for that match until it starts";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    private boolean autoMatchAnnounced;
    private boolean kitChoicePending;

    @Test
    void refusesEveryLockedArenaChangeWhileAnAutoMatchIsAnnouncedAndAllowsItAgainAfterwards() {
        ArenaSetupService arenaSetupService = prepareArena();
        autoMatchAnnounced = true;

        assertEquals(Optional.of(RESERVED_FOR_AUTO_MATCH), arenaSetupService.findCleaningRejection());
        assertEquals(Optional.of(RESERVED_FOR_AUTO_MATCH), arenaSetupService.findResetRejection());
        assertFalse(arenaSetupService.startCleaning(fixture.admin()));
        arenaSetupService.calculateShape(fixture.admin());

        assertEquals(List.of(RESERVED_FOR_AUTO_MATCH, RESERVED_FOR_AUTO_MATCH), fixture.messages());
        assertEquals(0, fixture.arenaConfigurationSaveCount());

        autoMatchAnnounced = false;
        fixture.messages().clear();

        assertEquals(Optional.empty(), arenaSetupService.findCleaningRejection());
        assertEquals(Optional.empty(), arenaSetupService.findResetRejection());
        arenaSetupService.calculateShape(fixture.admin());
        assertEquals(1, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void anOpenKitChoiceKeepsItsOwnSentence() {
        ArenaSetupService arenaSetupService = prepareArena();
        autoMatchAnnounced = true;
        kitChoicePending = true;

        assertEquals(Optional.of(RESERVED_FOR_KIT_CHOICE), arenaSetupService.findCleaningRejection());
        assertEquals(Optional.of(RESERVED_FOR_KIT_CHOICE), arenaSetupService.findResetRejection());
    }

    private ArenaSetupService prepareArena() {
        fixture.configureArena();
        fixture.standInsideTheFightArea();
        ArenaSetupService arenaSetupService = fixture.arenaSetupService();
        arenaSetupService.setArenaReservedForMatch(() -> kitChoicePending);
        arenaSetupService.setArenaReservedForAutoMatch(() -> autoMatchAnnounced);
        fixture.messages().clear();
        return arenaSetupService;
    }
}
