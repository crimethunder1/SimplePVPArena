package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaSpawnType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaResetLockTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int RESET_SLOT = 16;
    private static final int CONFIRM_YES_SLOT = 11;

    private static final String RESET_REFUSED_BY_A_RUNNING_MATCH = "A match is running, the arena can only be "
            + "reset between matches";
    private static final String RESERVED_MESSAGE = "The kit for the next match is being chosen, the arena is "
            + "reserved for that match until it starts";

    private final AdminMenuFixture fixture = AdminMenuFixture.withKitChoice();

    @Test
    void refusesTheResetFromTheArenaMenuWhileAMatchRuns() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        assertResetRefusedOnTheTile(RESET_REFUSED_BY_A_RUNNING_MATCH);
    }

    @Test
    void refusesTheResetFromTheArenaMenuWhileTheKitIsChosen() {
        startTheTournamentWithAKitChoice();

        assertResetRefusedOnTheTile(RESERVED_MESSAGE);
        assertTrue(fixture.kitChoiceService().isPending());
    }

    @Test
    void refusesTheResetWhenAMatchStartedWhileTheConfirmationWasOpen() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(RESET_SLOT);
        assertEquals(Optional.of(ConfirmationRequest.RESET_ARENA),
                fixture.lastMenu().holder().getConfirmationRequest());
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of(RESET_REFUSED_BY_A_RUNNING_MATCH), fixture.messages());
        assertTrue(fixture.arenaRegistry().getArena().isFullyConfigured());
        assertEquals(0, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void refusesTheResetThroughTheServiceWhileAMatchRunsOrTheKitIsChosen() {
        startTheTournamentWithAKitChoice();

        assertEquals(Optional.of(RESERVED_MESSAGE), fixture.arenaSetupService().findResetRejection());
        fixture.arenaSetupService().clearConfiguration(fixture.admin());

        assertEquals(List.of(RESERVED_MESSAGE), fixture.messages());

        assertTrue(fixture.kitChoiceService().choose("Knight", "admin"));
        assertTrue(fixture.matchService().hasActiveMatch());
        fixture.messages().clear();
        assertEquals(Optional.of(RESET_REFUSED_BY_A_RUNNING_MATCH), fixture.arenaSetupService().findResetRejection());
        fixture.arenaSetupService().clearConfiguration(fixture.admin());

        assertEquals(List.of(RESET_REFUSED_BY_A_RUNNING_MATCH), fixture.messages());
        assertTrue(fixture.arenaRegistry().getArena().isFullyConfigured());
        assertEquals(0, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void allowsTheResetBetweenMatches() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals(Optional.empty(), fixture.arenaSetupService().findResetRejection());
        fixture.click(RESET_SLOT);
        fixture.click(CONFIRM_YES_SLOT);

        assertFalse(fixture.arenaRegistry().getArena().hasShape());
        assertEquals(List.of("The arena configuration was removed\nIts blocks and the border blocks stay untouched"),
                fixture.messages());
        assertEquals(1, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void stillLetsTheAdminSetTheSpawnsWhileAMatchRuns() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.standInsideTheFightArea();

        fixture.arenaSetupService().setSpawn(fixture.admin(), ArenaSpawnType.FIRST_TEAM);
        fixture.standAt(50.5D, 3.5D);
        fixture.arenaSetupService().setSpawn(fixture.admin(), ArenaSpawnType.TRIBUNE);

        assertTrue(fixture.messages().contains("Set the first team spawn to admin-menu 3.5/64.0/3.5"),
                fixture.messages().toString());
        assertTrue(fixture.messages().contains("Set the tribune spawn to admin-menu 50.5/64.0/3.5"),
                fixture.messages().toString());
        assertEquals(2, fixture.arenaConfigurationSaveCount());
    }

    private void assertResetRefusedOnTheTile(String expectedMessage) {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        AdminMenuHolder arenaHolder = fixture.lastMenu().holder();

        fixture.click(RESET_SLOT);

        assertEquals(List.of(expectedMessage), fixture.messages());
        assertSame(arenaHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.arenaRegistry().getArena().isFullyConfigured());
        assertEquals(0, fixture.arenaConfigurationSaveCount());
    }

    private void startTheTournamentWithAKitChoice() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.startTournament();
        assertTrue(fixture.tournamentManager().isKitChoicePending());
        fixture.messages().clear();
    }
}
