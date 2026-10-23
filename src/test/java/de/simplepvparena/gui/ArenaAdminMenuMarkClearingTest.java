package de.simplepvparena.gui;

import de.simplepvparena.arena.ArenaShapeCalculation;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaAdminMenuMarkClearingTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int CLEAR_BORDER_MARKS_SLOT = 17;
    private static final int CLEAR_START_GATE_MARKS_SLOT = 25;
    private static final int START_GATE_SLOT = 20;
    private static final int CONFIRM_YES_SLOT = 11;
    private static final int CONFIRM_NO_SLOT = 15;
    private static final int BORDER_MARKS_OF_THE_ARENA = 96;
    private static final int CELL_LIMIT = 500;
    private static final String WORLD_NAME = "admin-menu";

    private static final String NOTHING_MARKED = "Nothing is marked";
    private static final String RUNNING_MATCH = "A match is running, the arena can only be reset between matches";
    private static final String BEING_CLEANED = "The arena is still being cleaned. Try again in a moment.";
    private static final String BORDER_MARKS_TILE = "17 | TNT_MINECART | Clear the border marks | "
            + "Forgets every marked border block / The blocks in the world stay as they are / "
            + "The calculated fight area stays, / a new calculation needs marked border blocks again / "
            + "96 border blocks are marked / Asks for a confirmation first";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsBothTilesWithTheNumberOfTheMarks() {
        fixture.configureArena();
        fixture.markStartGateBlocks(2);

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals(BORDER_MARKS_TILE, describeTile(CLEAR_BORDER_MARKS_SLOT));
        assertEquals("25 | IRON_CHAIN | Clear the start gate marks | Forgets every marked start gate block / "
                + "The blocks in the world stay as they are, / the rounds start without a start gate / "
                + "A cleaning resets these blocks like any other block / "
                + "2 start gate blocks are marked / Asks for a confirmation first",
                describeTile(CLEAR_START_GATE_MARKS_SLOT));
    }

    @Test
    void namesASingleMarkInTheSingular() {
        fixture.markStartGateBlocks(1);

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("25 | IRON_CHAIN | Clear the start gate marks | Forgets every marked start gate block / "
                + "The blocks in the world stay as they are, / the rounds start without a start gate / "
                + "A cleaning resets these blocks like any other block / "
                + "1 start gate block is marked / Asks for a confirmation first",
                describeTile(CLEAR_START_GATE_MARKS_SLOT));
    }

    @Test
    void greysBothTilesOutWithoutMarks() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("17 | GRAY_DYE | Clear the border marks | Nothing is marked",
                describeTile(CLEAR_BORDER_MARKS_SLOT));
        assertEquals("25 | GRAY_DYE | Clear the start gate marks | Nothing is marked",
                describeTile(CLEAR_START_GATE_MARKS_SLOT));
    }

    @Test
    void greysBothTilesOutWithTheReasonWhileAMatchRuns() {
        fixture.configureArena();
        fixture.markStartGateBlocks(2);
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("17 | GRAY_DYE | Clear the border marks | " + RUNNING_MATCH,
                describeTile(CLEAR_BORDER_MARKS_SLOT));
        assertEquals("25 | GRAY_DYE | Clear the start gate marks | " + RUNNING_MATCH,
                describeTile(CLEAR_START_GATE_MARKS_SLOT));
    }

    @Test
    void greysBothTilesOutWithTheReasonWhileTheArenaIsCleaned() {
        fixture.configureArena();
        fixture.markStartGateBlocks(2);
        fixture.standInsideTheFightArea();
        assertTrue(fixture.arenaSetupService().startCleaning(fixture.admin()));

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("17 | GRAY_DYE | Clear the border marks | " + BEING_CLEANED,
                describeTile(CLEAR_BORDER_MARKS_SLOT));
        assertEquals("25 | GRAY_DYE | Clear the start gate marks | " + BEING_CLEANED,
                describeTile(CLEAR_START_GATE_MARKS_SLOT));
    }

    @Test
    void reportsThatNothingIsMarkedOnAClickAndKeepsTheMenuOpen() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        AdminMenuHolder arenaHolder = fixture.lastMenu().holder();

        fixture.click(CLEAR_BORDER_MARKS_SLOT);
        fixture.click(CLEAR_START_GATE_MARKS_SLOT);

        assertEquals(List.of(NOTHING_MARKED, NOTHING_MARKED), fixture.messages());
        assertSame(arenaHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
    }

    @Test
    void reportsTheRunningMatchOnAClickAndOpensNoConfirmation() {
        fixture.configureArena();
        fixture.markStartGateBlocks(2);
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        AdminMenuHolder arenaHolder = fixture.lastMenu().holder();

        fixture.click(CLEAR_BORDER_MARKS_SLOT);
        fixture.click(CLEAR_START_GATE_MARKS_SLOT);

        assertEquals(List.of(RUNNING_MATCH, RUNNING_MATCH), fixture.messages());
        assertSame(arenaHolder, fixture.lastMenu().holder());
        assertEquals(BORDER_MARKS_OF_THE_ARENA, fixture.borderMarks().getMarkedBlockCount());
        assertEquals(2, fixture.gateMarks().getMarkedBlockCount());
    }

    @Test
    void asksBeforeTheBorderMarksAreCleared() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(CLEAR_BORDER_MARKS_SLOT);

        assertConfirmationOpened(ConfirmationRequest.CLEAR_BORDER_MARKS);
        assertEquals("""
                Clear the border marks? | 27
                4 | BOOK | Clear the border marks? | 96 border marks are forgotten / \
                The blocks in the world stay as they are
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
        assertEquals(BORDER_MARKS_OF_THE_ARENA, fixture.borderMarks().getMarkedBlockCount());
    }

    @Test
    void asksBeforeTheStartGateMarksAreCleared() {
        fixture.markStartGateBlocks(2);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(CLEAR_START_GATE_MARKS_SLOT);

        assertConfirmationOpened(ConfirmationRequest.CLEAR_START_GATE_MARKS);
        assertEquals("""
                Clear the start gate marks? | 27
                4 | BOOK | Clear the start gate marks? | 2 start gate marks are forgotten / \
                The blocks in the world stay as they are
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
        assertEquals(2, fixture.gateMarks().getMarkedBlockCount());
    }

    @Test
    void namesASingleStartGateMarkInTheSingularInTheConfirmation() {
        fixture.markStartGateBlocks(1);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(CLEAR_START_GATE_MARKS_SLOT);

        assertEquals(List.of("1 start gate mark is forgotten", "The blocks in the world stay as they are"),
                fixture.lastMenu().findTile(AdminMenuLayout.INFO_SLOT).orElseThrow().loreLines());
    }

    @Test
    void namesTheMissingMarksInTheConfirmationWhenTheyWereClearedInTheMeantime() {
        fixture.configureArena();
        fixture.markStartGateBlocks(2);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_BORDER_MARKS_SLOT);
        fixture.borderMarks().clear();
        fixture.refillLastMenu();
        List<String> borderLore = fixture.lastMenu().findTile(AdminMenuLayout.INFO_SLOT).orElseThrow().loreLines();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_START_GATE_MARKS_SLOT);
        fixture.gateMarks().clear();
        fixture.refillLastMenu();

        assertEquals(List.of("No border blocks are marked", "The blocks in the world stay as they are"), borderLore);
        assertEquals(List.of("No start gate blocks are marked", "The blocks in the world stay as they are"),
                fixture.lastMenu().findTile(AdminMenuLayout.INFO_SLOT).orElseThrow().loreLines());
    }

    @Test
    void clearsTheBorderMarksOnYesAndKeepsTheFightAreaAndTheStartGate() {
        fixture.configureArena();
        fixture.markStartGateBlocks(2);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_BORDER_MARKS_SLOT);

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("96 border marks were forgotten"), fixture.messages());
        assertEquals(0, fixture.borderMarks().getMarkedBlockCount());
        assertEquals(1, fixture.borderMarkWriteCount());
        assertEquals(2, fixture.gateMarks().getMarkedBlockCount());
        assertEquals(0, fixture.startGateMarkWriteCount());
        assertTrue(fixture.arenaRegistry().getArena().isFullyConfigured(), "the fight area stays");
        assertEquals(0, fixture.arenaConfigurationSaveCount());
        assertTrue(fixture.borderParticleService().isConfirmedParticleEnabled());
        assertEquals(List.of(), fixture.markClearingWarnings());
        assertEquals(AdminMenuType.ARENA, fixture.lastMenu().holder().getMenuType());
        assertEquals("17 | GRAY_DYE | Clear the border marks | Nothing is marked",
                describeTile(CLEAR_BORDER_MARKS_SLOT));
    }

    @Test
    void clearsTheStartGateMarksOnYesAndKeepsTheBorderMarks() {
        fixture.configureArena();
        fixture.markStartGateBlocks(3);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_START_GATE_MARKS_SLOT);

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("3 start gate marks were forgotten"), fixture.messages());
        assertTrue(fixture.gateMarks().isEmpty());
        assertEquals(1, fixture.startGateMarkWriteCount());
        assertEquals(BORDER_MARKS_OF_THE_ARENA, fixture.borderMarks().getMarkedBlockCount());
        assertEquals(0, fixture.borderMarkWriteCount());
        assertTrue(fixture.arenaRegistry().getArena().isFullyConfigured());
        assertEquals(AdminMenuType.ARENA, fixture.lastMenu().holder().getMenuType());
        assertEquals("25 | GRAY_DYE | Clear the start gate marks | Nothing is marked",
                describeTile(CLEAR_START_GATE_MARKS_SLOT));
        assertTrue(describeTile(START_GATE_SLOT).endsWith(
                "The gate holds no block yet / It comes down over 10 seconds"), describeTile(START_GATE_SLOT));
    }

    @Test
    void namesASingleForgottenMarkInTheSingular() {
        fixture.markStartGateBlocks(1);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_START_GATE_MARKS_SLOT);

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("1 start gate mark was forgotten"), fixture.messages());
    }

    @Test
    void changesNothingOnNo() {
        fixture.configureArena();
        fixture.markStartGateBlocks(2);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_BORDER_MARKS_SLOT);
        fixture.click(CONFIRM_NO_SLOT);
        fixture.click(CLEAR_START_GATE_MARKS_SLOT);

        fixture.click(CONFIRM_NO_SLOT);

        assertEquals(List.of(), fixture.messages());
        assertEquals(BORDER_MARKS_OF_THE_ARENA, fixture.borderMarks().getMarkedBlockCount());
        assertEquals(2, fixture.gateMarks().getMarkedBlockCount());
        assertEquals(0, fixture.borderMarkWriteCount());
        assertEquals(0, fixture.startGateMarkWriteCount());
        assertEquals(AdminMenuType.ARENA, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void refusesTheYesWhenAMatchStartedWhileTheConfirmationWasOpen() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_BORDER_MARKS_SLOT);
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of(RUNNING_MATCH), fixture.messages());
        assertEquals(BORDER_MARKS_OF_THE_ARENA, fixture.borderMarks().getMarkedBlockCount());
        assertEquals(0, fixture.borderMarkWriteCount());
        assertEquals(AdminMenuType.ARENA, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void refusesTheYesWhenTheMarksWereClearedWhileTheConfirmationWasOpen() {
        fixture.markStartGateBlocks(2);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_START_GATE_MARKS_SLOT);
        fixture.gateMarks().clear();

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of(NOTHING_MARKED), fixture.messages());
        assertEquals(0, fixture.startGateMarkWriteCount());
    }

    @Test
    void namesTheNumberOfTheMomentOfTheYes() {
        fixture.markStartGateBlocks(2);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_START_GATE_MARKS_SLOT);
        fixture.markStartGateBlocks(5);

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("5 start gate marks were forgotten"), fixture.messages());
        assertTrue(fixture.gateMarks().isEmpty());
    }

    @Test
    void warnsTheAdminWhenTheFileCannotBeWrittenAndKeepsTheMarksForgotten() {
        fixture.configureArena();
        fixture.markStartGateBlocks(2);
        fixture.failMarkWrites(new IOException("disk full"));
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_BORDER_MARKS_SLOT);
        fixture.click(CONFIRM_YES_SLOT);
        fixture.click(CLEAR_START_GATE_MARKS_SLOT);

        fixture.click(CONFIRM_YES_SLOT);

        List<String> warnings = List.of(
                "96 border marks were forgotten, but borders.yml could not be written: disk full",
                "2 start gate marks were forgotten, but startgates.yml could not be written: disk full");
        assertEquals(warnings, fixture.messages());
        assertEquals(warnings, fixture.markClearingWarnings());
        assertEquals(0, fixture.borderMarks().getMarkedBlockCount());
        assertTrue(fixture.gateMarks().isEmpty());
        assertEquals("17 | GRAY_DYE | Clear the border marks | Nothing is marked",
                describeTile(CLEAR_BORDER_MARKS_SLOT));
    }

    @Test
    void leavesNoBorderForTheNextCalculationOnceTheBorderMarksAreCleared() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_BORDER_MARKS_SLOT);
        fixture.click(CONFIRM_YES_SLOT);

        ArenaShapeCalculation calculation = ArenaShapeFloodFill.calculate(WORLD_NAME, 3, 64, 3, CELL_LIMIT,
                fixture.borderMarks().createColumnReader(WORLD_NAME));

        assertEquals(ArenaShapeCalculation.Status.CELL_LIMIT_REACHED, calculation.getStatus());
        assertTrue(fixture.arenaRegistry().getArena().isFullyConfigured(), "the stored fight area stays");
    }

    private void assertConfirmationOpened(ConfirmationRequest request) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.CONFIRMATION, menu.holder().getMenuType());
        assertEquals(Optional.of(request), menu.holder().getConfirmationRequest());
        assertTrue(menu.holder().getSubject().isEmpty());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    private String describeTile(int slot) {
        return fixture.describeLastMenu().lines()
                .filter(line -> line.startsWith(slot + " | "))
                .findFirst()
                .orElseThrow();
    }
}
