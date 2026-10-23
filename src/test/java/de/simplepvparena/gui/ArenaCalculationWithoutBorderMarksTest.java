package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaCalculationWithoutBorderMarksTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int CLEAR_BORDER_MARKS_SLOT = 17;
    private static final int CONFIRM_YES_SLOT = 11;
    private static final int DIAGNOSIS_CELL_LIMIT = 60000;
    private static final String NO_BORDER_BLOCK_MARKED = "No border block is marked, mark the border blocks first";
    private static final String GERMAN_NO_BORDER_BLOCK_MARKED =
            "Es ist kein Wandblock markiert, markiere zuerst die Wandblöcke";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void refusesTheCalculationAtOnceWithoutASingleMarkedBorderBlock() {
        fixture.standInsideTheFightArea();

        fixture.arenaSetupService().calculateShape(fixture.admin());

        assertEquals(List.of(NO_BORDER_BLOCK_MARKED), fixture.messages());
        assertFalse(fixture.arenaRegistry().getArena().hasShape());
        assertEquals(0, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void refusesTheDiagnosisRunAtOnceWithoutASingleMarkedBorderBlock() {
        fixture.standInsideTheFightArea();

        fixture.arenaSetupService().calculateShape(fixture.admin(), DIAGNOSIS_CELL_LIMIT);

        assertEquals(List.of(NO_BORDER_BLOCK_MARKED), fixture.messages());
        assertFalse(fixture.arenaRegistry().getArena().hasShape());
        assertEquals(0, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void refusesTheCalculationAfterTheBorderMarksWereClearedAndKeepsTheStoredFightArea() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_BORDER_MARKS_SLOT);
        fixture.click(CONFIRM_YES_SLOT);
        fixture.messages().clear();
        fixture.standInsideTheFightArea();

        fixture.arenaSetupService().calculateShape(fixture.admin());

        assertEquals(List.of(NO_BORDER_BLOCK_MARKED), fixture.messages());
        assertTrue(fixture.arenaRegistry().getArena().isFullyConfigured(), "the stored fight area stays");
        assertEquals(0, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void keepsTheRefusalOfARunningMatchBeforeTheMissingBorderMarks() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.borderMarks().clear();
        fixture.messages().clear();
        fixture.standInsideTheFightArea();

        fixture.arenaSetupService().calculateShape(fixture.admin(), DIAGNOSIS_CELL_LIMIT);

        assertEquals(List.of("A match is running, the fight area can only be calculated between matches"),
                fixture.messages());
    }

    @Test
    void calculatesAgainOnceABorderIsMarked() {
        fixture.standInsideTheFightArea();
        fixture.arenaSetupService().calculateShape(fixture.admin());
        fixture.storeTheFightArea();
        fixture.messages().clear();

        fixture.arenaSetupService().calculateShape(fixture.admin());

        assertTrue(fixture.messages().getFirst().startsWith("The fight area was calculated from the border blocks"),
                fixture.messages().toString());
        assertEquals(1, fixture.arenaConfigurationSaveCount());
    }

    @Test
    @UseLanguage("de")
    void refusesBothRunsInGerman() {
        fixture.standInsideTheFightArea();

        fixture.arenaSetupService().calculateShape(fixture.admin());
        fixture.arenaSetupService().calculateShape(fixture.admin(), DIAGNOSIS_CELL_LIMIT);

        assertEquals(List.of(GERMAN_NO_BORDER_BLOCK_MARKED, GERMAN_NO_BORDER_BLOCK_MARKED), fixture.messages());
    }
}
