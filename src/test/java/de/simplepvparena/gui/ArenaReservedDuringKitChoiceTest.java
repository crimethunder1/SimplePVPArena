package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.ArenaShape;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaReservedDuringKitChoiceTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int CALCULATE_SLOT = 10;
    private static final int CLEAN_SLOT = 15;

    private static final String RESERVED_MESSAGE = "The kit for the next match is being chosen, the arena is "
            + "reserved for that match until it starts";
    private static final String CLEANING_REFUSED_BY_A_RUNNING_MATCH = "A match is running, the arena can only be "
            + "cleaned between matches";
    private static final String CALCULATION_REFUSED_BY_A_RUNNING_MATCH = "A match is running, the fight area can "
            + "only be calculated between matches";

    private final AdminMenuFixture fixture = AdminMenuFixture.withKitChoice();

    private int matchStartCount;

    @Test
    void refusesTheCleaningFromTheArenaMenuWhileTheKitIsChosenAndAgainWhileTheMatchRuns() {
        startTheTournamentWithAKitChoice();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        AdminMenuHolder arenaHolder = fixture.lastMenu().holder();

        fixture.click(CLEAN_SLOT);

        assertEquals(List.of(RESERVED_MESSAGE), fixture.messages());
        assertSame(arenaHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertFalse(fixture.matchService().hasActiveMatch());
        assertTrue(fixture.kitChoiceService().isPending());

        assertTrue(fixture.kitChoiceService().choose("Knight", "admin"));
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAN_SLOT);

        assertEquals("round1-match1", fixture.runningMatch().getMatchId());
        assertEquals(List.of(CLEANING_REFUSED_BY_A_RUNNING_MATCH), fixture.messages());
        assertEquals(AdminMenuType.ARENA, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void refusesTheCalculationFromTheArenaMenuWhileTheKitIsChosen() {
        startTheTournamentWithAKitChoice();
        Optional<ArenaShape> shapeBefore = fixture.arenaRegistry().getArena().getShape();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.standInsideTheFightArea();

        fixture.click(CALCULATE_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(List.of(RESERVED_MESSAGE), fixture.messages());
        assertEquals(shapeBefore, fixture.arenaRegistry().getArena().getShape());
        assertEquals(0, fixture.arenaConfigurationSaveCount());
        assertFalse(fixture.matchService().hasActiveMatch());
    }

    @Test
    void refusesEveryLockedArenaChangeWhileAChoiceIsOpenAndAllowsItAgainWhenTheChoiceIsOver() {
        fixture.configureArena();
        createArcherAndKnight();
        fixture.standInsideTheFightArea();
        ArenaSetupService arenaSetupService = fixture.arenaSetupService();
        assertEquals(Optional.empty(), arenaSetupService.findCleaningRejection());
        fixture.kitChoiceService().begin("round1-match1", "Alpha", "Bravo", () -> matchStartCount++);
        fixture.messages().clear();

        assertEquals(Optional.of(RESERVED_MESSAGE), arenaSetupService.findCleaningRejection());
        assertFalse(arenaSetupService.startCleaning(fixture.admin()));
        assertEquals(Optional.of(RESERVED_MESSAGE), arenaSetupService.startAutomaticCleaning(outcome -> { }));
        arenaSetupService.calculateShape(fixture.admin());
        arenaSetupService.calculateShape(fixture.admin(), 60000);

        assertEquals(List.of(RESERVED_MESSAGE, RESERVED_MESSAGE, RESERVED_MESSAGE), fixture.messages());
        assertEquals(0, fixture.arenaConfigurationSaveCount());
        assertFalse(fixture.matchService().hasActiveMatch());

        fixture.kitChoiceScheduler().advanceSeconds(30);
        fixture.messages().clear();

        assertEquals(1, matchStartCount);
        assertFalse(fixture.kitChoiceService().isPending());
        assertEquals(Optional.empty(), arenaSetupService.findCleaningRejection());
        arenaSetupService.calculateShape(fixture.admin());
        assertEquals(1, fixture.arenaConfigurationSaveCount());
        assertTrue(fixture.messages().getFirst().startsWith("The fight area was calculated from the border blocks"),
                fixture.messages().toString());
    }

    @Test
    void keepsTheRefusalsOfARunningMatchAsTheyWere() {
        startTheTournamentWithAKitChoice();
        assertTrue(fixture.kitChoiceService().choose("Knight", "admin"));
        fixture.messages().clear();
        fixture.standInsideTheFightArea();

        fixture.arenaSetupService().calculateShape(fixture.admin());

        assertEquals(Optional.of(CLEANING_REFUSED_BY_A_RUNNING_MATCH),
                fixture.arenaSetupService().findCleaningRejection());
        assertEquals(List.of(CALCULATION_REFUSED_BY_A_RUNNING_MATCH), fixture.messages());
    }

    private void startTheTournamentWithAKitChoice() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        createArcherAndKnight();
        fixture.startTournament();
        assertTrue(fixture.tournamentManager().isKitChoicePending());
        fixture.messages().clear();
    }

    private void createArcherAndKnight() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
    }
}
