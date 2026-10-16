package de.simplepvparena.gui;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.PositionSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaAdminMenuClickTest {

    private static final int MAIN_ARENA_SLOT = 10;

    private static final int INFO_SLOT = 4;
    private static final int EMPTY_SLOT = 0;
    private static final int CALCULATE_SLOT = 10;
    private static final int DIAGNOSE_SLOT = 11;
    private static final int FIRST_TEAM_SPAWN_SLOT = 12;
    private static final int SECOND_TEAM_SPAWN_SLOT = 13;
    private static final int TRIBUNE_SPAWN_SLOT = 14;
    private static final int CLEAN_SLOT = 15;
    private static final int RESET_SLOT = 16;
    private static final int START_GATE_SLOT = 20;
    private static final int BACK_SLOT = 22;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileIsClicked() {
        openTheArenaMenuOfAReadyArena();

        assertNothingHappensOnAClickOn(INFO_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenAnEmptySlotIsClicked() {
        openTheArenaMenuOfAReadyArena();

        assertNothingHappensOnAClickOn(EMPTY_SLOT);
    }

    @Test
    void closesTheMenuAndRunsTheCalculationWhichIsRefusedWhileAMatchStandsOnTheArena() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        Optional<ArenaShape> shapeBefore = arena().getShape();

        fixture.click(CALCULATE_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(List.of("A match is running, the fight area can only be calculated between matches"),
                fixture.messages());
        assertEquals(shapeBefore, arena().getShape());
    }

    @Test
    void closesTheMenuAndAsksForTheDiagnosisCellLimitInTheChat() {
        openTheArenaMenuOfAReadyArena();

        fixture.click(DIAGNOSE_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertTrue(fixture.chatInputService().isWaiting(fixture.admin()));
        assertEquals(List.of(
                "Type: <cells> in chat",
                "A number between 1 and 5000000",
                "Stand inside the border wall, the calculation runs from where you stand",
                "config.yml is not changed by it",
                "Type cancel to abort, the request expires in 60 seconds"), fixture.messages());
    }

    @Test
    void closesTheMenuAndAsksForTheStartGateBlocksInTheChat() {
        openTheArenaMenuOfAReadyArena();

        fixture.click(START_GATE_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertTrue(fixture.chatInputService().isWaiting(fixture.admin()));
        assertEquals(List.of(
                "Type: <material> <amount> in chat",
                "Example: barrier 32",
                "The amount has to be between 1 and 64",
                "Type cancel to abort, the request expires in 60 seconds"), fixture.messages());
    }

    @Test
    void setsTheFirstTeamSpawnOfAnIncompleteArenaAndRefreshesTheMenu() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        AdminMenuHolder arenaHolder = fixture.lastMenu().holder();
        fixture.standInsideTheFightArea();

        fixture.click(FIRST_TEAM_SPAWN_SLOT);

        assertEquals(List.of(
                "Set the first team spawn to admin-menu 3.5/64.0/3.5",
                "The arena still misses:",
                "the calculated fight area (\"Calculate the fight area\" in the arena menu of /tournament)",
                "the tribune spawn (\"Set the tribune spawn\" in the arena menu of /tournament)"),
                fixture.messages());
        assertEquals(Optional.of("admin-menu 3.5/64.0/3.5"),
                arena().getConfiguredFirstTeamSpawn().map(PositionSnapshot::describe));
        assertRefreshedOnce(arenaHolder);
        assertEquals("  first team spawn: admin-menu 3.5/64.0/3.5",
                fixture.lastMenu().findTile(INFO_SLOT).orElseThrow().loreLines().get(2));
    }

    @Test
    void setsTheSecondTeamSpawnOfAReadyArenaAndRefreshesTheMenu() {
        openTheArenaMenuOfAReadyArena();
        AdminMenuHolder arenaHolder = fixture.lastMenu().holder();
        fixture.standInsideTheFightArea();

        fixture.click(SECOND_TEAM_SPAWN_SLOT);

        assertEquals(List.of("Set the second team spawn to admin-menu 3.5/64.0/3.5"), fixture.messages());
        assertEquals(Optional.of("admin-menu 3.5/64.0/3.5"),
                arena().getConfiguredSecondTeamSpawn().map(PositionSnapshot::describe));
        assertRefreshedOnce(arenaHolder);
    }

    @Test
    void setsTheTribuneSpawnOutsideOfTheFightAreaAndRefreshesTheMenu() {
        fixture.storeTheFightArea();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        AdminMenuHolder arenaHolder = fixture.lastMenu().holder();
        fixture.standOutsideTheFightArea();

        fixture.click(TRIBUNE_SPAWN_SLOT);

        assertEquals(List.of("Set the tribune spawn to admin-menu 11.5/64.0/11.5"), fixture.messages());
        assertTrue(arena().isFullyConfigured());
        assertRefreshedOnce(arenaHolder);
        assertEquals("Arena ready", fixture.lastMenu().findTile(INFO_SLOT).orElseThrow().loreLines().get(0));
    }

    @Test
    void refusesATribuneSpawnInsideTheFightAreaAndStillRefreshesTheMenu() {
        openTheArenaMenuOfAReadyArena();
        AdminMenuHolder arenaHolder = fixture.lastMenu().holder();
        fixture.standInsideTheFightArea();

        fixture.click(TRIBUNE_SPAWN_SLOT);

        assertEquals(List.of(lines(
                "The tribune spawn must lie outside of the calculated fight area, admin-menu 3.5/64.0/3.5 "
                        + "lies inside it",
                "Stand outside the border wall, where the spectators wait, then set it again. Nothing was stored")),
                fixture.messages());
        assertEquals(Optional.of("admin-menu 11.5/64.0/11.5"),
                arena().getConfiguredTribuneSpawn().map(PositionSnapshot::describe));
        assertRefreshedOnce(arenaHolder);
    }

    @Test
    void refusesTheCleaningOfAnArenaWithoutAFightAreaAndKeepsTheMenuOpen() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertCleaningRefused(lines(
                "The fight area is not calculated, there is nothing to clean",
                "Use \"Calculate the fight area\" in the arena menu of /tournament while standing inside "
                        + "the border wall first"));
    }

    @Test
    void refusesTheCleaningWhileAMatchStandsOnTheArenaAndKeepsTheMenuOpen() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertCleaningRefused("A match is running, the arena can only be cleaned between matches");
    }

    @Test
    void asksForTheConfirmationOfTheCleaning() {
        openTheArenaMenuOfAReadyArena();

        fixture.click(CLEAN_SLOT);

        assertConfirmationOpened(ConfirmationRequest.CLEAN_ARENA, "Clean the arena?");
        assertTrue(arena().hasShape());
    }

    @Test
    void asksForTheConfirmationOfTheReset() {
        openTheArenaMenuOfAReadyArena();

        fixture.click(RESET_SLOT);

        assertConfirmationOpened(ConfirmationRequest.RESET_ARENA, "Reset the arena?");
        assertTrue(arena().isFullyConfigured(), "nothing is dropped before the confirmation");
    }

    @Test
    void returnsToTheMainMenu() {
        openTheArenaMenuOfAReadyArena();

        fixture.click(BACK_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.MAIN, menu.holder().getMenuType());
        assertEquals("Tournament administration", menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        openTheArenaMenuOfAReadyArena();
        AdminMenuHolder arenaHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(RESET_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(arenaHolder, fixture.lastMenu().holder());
    }

    private void openTheArenaMenuOfAReadyArena() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
    }

    private Arena arena() {
        return fixture.arenaRegistry().getArena();
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder arenaHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(arenaHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    private void assertRefreshedOnce(AdminMenuHolder arenaHolder) {
        assertSame(arenaHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(1, fixture.lastMenu().refreshCount());
    }

    private void assertCleaningRefused(String expectedMessage) {
        AdminMenuHolder arenaHolder = fixture.lastMenu().holder();

        fixture.click(CLEAN_SLOT);

        assertEquals(List.of(expectedMessage), fixture.messages());
        assertSame(arenaHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
    }

    private void assertConfirmationOpened(ConfirmationRequest request, String title) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.CONFIRMATION, menu.holder().getMenuType());
        assertEquals(Optional.of(request), menu.holder().getConfirmationRequest());
        assertTrue(menu.holder().getSubject().isEmpty());
        assertEquals(0, menu.holder().getPageIndex());
        assertEquals(title, menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    private static String lines(String... lines) {
        return String.join("\n", lines);
    }
}
