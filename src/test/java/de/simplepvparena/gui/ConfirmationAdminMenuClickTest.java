package de.simplepvparena.gui;

import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.tournament.TournamentState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmationAdminMenuClickTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MAIN_KIT_SLOT = 24;

    private static final int ARENA_CLEAN_SLOT = 15;
    private static final int ARENA_RESET_SLOT = 16;
    private static final int KITS_FIRST_KIT_SLOT = 0;
    private static final int KITS_SECOND_KIT_SLOT = 1;
    private static final int KITS_NEXT_PAGE_SLOT = 53;
    private static final int KIT_REPLACE_SLOT = 13;
    private static final int KIT_DELETE_SLOT = 15;
    private static final int TEAMS_FIRST_TEAM_SLOT = 0;
    private static final int TEAMS_IMPORT_SLOT = 50;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;
    private static final int TEAM_REMOVE_SLOT = 13;
    private static final int MATCH_CANCEL_SLOT = 13;

    private static final int EMPTY_SLOT = 0;
    private static final int INFO_SLOT = 4;
    private static final int YES_SLOT = 11;
    private static final int NO_SLOT = 15;
    private static final int BACK_SLOT_OF_THE_OTHER_MENUS = 22;

    private static final String IMPORT_FILE = """
            { "teams": [
              { "team_name": "Echo", "players": ["Steve", "Alex"] },
              { "team_name": "Foxtrot", "players": ["Notch", "Jeb_"] }
            ] }
            """;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void leavesTheConfirmationAsItIsWhenAnotherSlotIsClicked() {
        openTheResetConfirmation();

        for (int slot : List.of(EMPTY_SLOT, INFO_SLOT, BACK_SLOT_OF_THE_OTHER_MENUS)) {
            AdminMenuHolder confirmationHolder = fixture.lastMenu().holder();

            fixture.click(slot);

            assertSame(confirmationHolder, fixture.lastMenu().holder(), "slot " + slot);
            assertFalse(fixture.isMenuClosed());
            assertEquals(0, fixture.lastMenu().refreshCount());
            assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
            assertTrue(fixture.arenaRegistry().getArena().isFullyConfigured());
        }
    }

    @Test
    void startsTheCleaningAfterYesAndReturnsToTheArenaMenu() {
        openTheCleaningConfirmation();

        fixture.click(YES_SLOT);

        assertEquals(List.of(
                "No baseline of the fight area is stored, every block of it is set to air instead\n"
                        + "A baseline is taken by \"Calculate the fight area\" in the arena menu of /tournament "
                        + "and does not survive a server restart",
                "Cleaning the arena, 100 blocks between y 64 and y 67\n"
                        + "Processed in batches of 4096 per tick\n"
                        + "Layer by layer from the floor up: the red flood announces the volume, the green clearing "
                        + "writes it, 6.0 seconds for both together"), fixture.messages());
        assertEquals(Optional.of("The arena is already being cleaned"),
                fixture.arenaSetupService().findCleaningRejection());
        assertReturnedTo(AdminMenuType.ARENA, "Arena", 0);
    }

    @Test
    void keepsTheArenaAfterNoAndReturnsToTheArenaMenu() {
        openTheCleaningConfirmation();

        fixture.click(NO_SLOT);

        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(Optional.empty(), fixture.arenaSetupService().findCleaningRejection());
        assertReturnedTo(AdminMenuType.ARENA, "Arena", 0);
    }

    @Test
    void resetsTheArenaAfterYesAndReturnsToTheArenaMenuThatShowsTheResetArena() {
        openTheResetConfirmation();

        fixture.click(YES_SLOT);

        assertEquals(List.of("The arena configuration was removed\nIts blocks and the border blocks stay untouched"),
                fixture.messages());
        assertFalse(fixture.arenaRegistry().getArena().hasShape());
        assertFalse(fixture.arenaRegistry().getArena().isFullyConfigured());
        assertEquals(1, fixture.arenaConfigurationSaveCount());
        assertReturnedTo(AdminMenuType.ARENA, "Arena", 0);
        assertEquals("Arena incomplete", infoLore().get(0));
    }

    @Test
    void keepsTheArenaAfterNoInsteadOfTheReset() {
        openTheResetConfirmation();

        fixture.click(NO_SLOT);

        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertTrue(fixture.arenaRegistry().getArena().isFullyConfigured());
        assertEquals(0, fixture.arenaConfigurationSaveCount());
        assertReturnedTo(AdminMenuType.ARENA, "Arena", 0);
        assertEquals("Arena ready", infoLore().get(0));
    }

    @Test
    void keepsTheKitWhenTheInventoryWasEmptiedBeforeYesAndReturnsToTheKitOptions() {
        fixture.createKit("Archer");
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        openTheReplacementConfirmationOfTheFirstKit();
        fixture.clearAdminInventory();

        fixture.click(YES_SLOT);

        assertEquals(List.of("Your inventory is empty, the kit was not changed"), fixture.messages());
        assertEquals(1, fixture.kitStorage().savedLibraries().size());
        assertEquals(List.of("Worn items: 2 of 5", "Inventory items: 3 of 36 slots"), fixture.describeKit());
        assertReturnedToTheKitOptions("Archer", 0);
    }

    @Test
    void replacesTheDefaultKitWithAFilledInventoryAfterYesAndReturnsToTheKitOptions() {
        fixture.createKit("Archer");
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        openTheReplacementConfirmationOfTheFirstKit();

        fixture.click(YES_SLOT);

        assertEquals(List.of("Kit Archer saved: 1 item, 0 worn and 1 in the inventory",
                        "It is written into kits.yml and is handed out from the next round on, "
                                + "a running round keeps the kit its fighters were given"),
                fixture.messages());
        assertEquals(2, fixture.kitStorage().savedLibraries().size());
        assertEquals(1, fixture.kitStorage().lastSavedLibrary().kits().get(0).serializedKit().itemCount());
        assertEquals(List.of("Worn items: 0 of 5", "Inventory items: 1 of 36 slots"), fixture.describeKit());
        assertReturnedToTheKitOptions("Archer", 0);
        assertEquals(List.of("Worn items: 0 of 5", "Inventory items: 1 of 36",
                "Default kit: every fighter is given it when a round starts"), infoLore());
    }

    @Test
    void replacesAKitThatIsNotTheDefaultKitWithoutHandingItOut() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.placeInAdminInventory(0, new EncodableItemStack("axe"));
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_SECOND_KIT_SLOT);
        fixture.click(KIT_REPLACE_SLOT);

        fixture.click(YES_SLOT);

        assertEquals(List.of("Kit Knight saved: 1 item, 0 worn and 1 in the inventory",
                "It is written into kits.yml and is handed out once it is used as the default kit"),
                fixture.messages());
        assertEquals(1, fixture.kitLibrary().findKit("Knight").orElseThrow().itemCount());
        assertEquals(List.of("Worn items: 2 of 5", "Inventory items: 3 of 36 slots"), fixture.describeKit(),
                "the default kit Archer is still handed out");
        assertReturnedToTheKitOptions("Knight", 0);
    }

    @Test
    void keepsTheKitAfterNoOnTheReplacementAndReturnsToTheKitOptions() {
        fixture.createKit("Archer");
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        openTheReplacementConfirmationOfTheFirstKit();

        fixture.click(NO_SLOT);

        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(1, fixture.kitStorage().savedLibraries().size());
        assertEquals(List.of("Worn items: 2 of 5", "Inventory items: 3 of 36 slots"), fixture.describeKit());
        assertReturnedToTheKitOptions("Archer", 0);
    }

    @Test
    void deletesTheKitAfterYesAndReturnsToTheKitList() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_SECOND_KIT_SLOT);
        fixture.click(KIT_DELETE_SLOT);
        assertEquals(Optional.of(ConfirmationRequest.DELETE_KIT), fixture.lastMenu().holder().getConfirmationRequest());

        fixture.click(YES_SLOT);

        assertEquals(List.of("Kit Knight deleted"), fixture.messages());
        assertEquals(List.of("Archer"), fixture.kitLibrary().getKitNames());
        assertEquals(List.of("Archer"), fixture.kitStorage().lastSavedLibrary().kits().stream()
                .map(savedKit -> savedKit.name()).toList());
        assertReturnedTo(AdminMenuType.KIT, "Kits (page 1)", 0);
        assertEquals("Archer", fixture.lastMenu().findTile(KITS_FIRST_KIT_SLOT).orElseThrow().displayName());
        assertTrue(fixture.lastMenu().findTile(KITS_SECOND_KIT_SLOT).isEmpty());
    }

    @Test
    void deletesTheKitAfterYesAndReturnsWithTheSecondPageToAKitListThatHasOnlyOnePageLeft() {
        fixture.createNumberedKits(46);
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_NEXT_PAGE_SLOT);
        fixture.click(KITS_FIRST_KIT_SLOT);
        fixture.click(KIT_DELETE_SLOT);

        fixture.click(YES_SLOT);

        assertEquals(List.of("Kit Kit46 deleted"), fixture.messages());
        assertEquals(45, fixture.kitLibrary().getKitCount());
        assertReturnedTo(AdminMenuType.KIT, "Kits (page 1)", 1);
        assertEquals("Kit01", fixture.lastMenu().findTile(KITS_FIRST_KIT_SLOT).orElseThrow().displayName());
    }

    @Test
    void keepsTheKitAfterNoOnTheDeletionAndReturnsToTheKitList() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_SECOND_KIT_SLOT);
        fixture.click(KIT_DELETE_SLOT);

        fixture.click(NO_SLOT);

        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(List.of("Archer", "Knight"), fixture.kitLibrary().getKitNames());
        assertReturnedTo(AdminMenuType.KIT, "Kits (page 1)", 0);
    }

    @Test
    void refusesTheDeletionAfterYesWhenTheKitBecameTheDefaultKitMeanwhile() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_SECOND_KIT_SLOT);
        fixture.click(KIT_DELETE_SLOT);
        fixture.kitLibrary().setDefaultKit("Knight");

        fixture.click(YES_SLOT);

        assertEquals(List.of("Kit Knight is the default kit and cannot be deleted"), fixture.messages());
        assertEquals(List.of("Archer", "Knight"), fixture.kitLibrary().getKitNames());
        assertReturnedTo(AdminMenuType.KIT, "Kits (page 1)", 0);
    }

    @Test
    void removesTheTeamAfterYesAndReturnsToTheFirstPageThatShowsTheRemainingTeams() {
        fixture.registerTeams("Alpha", "Bravo");
        openTheRemovalConfirmationOfTheFirstTeam();

        fixture.click(YES_SLOT);

        assertEquals(List.of("Team Alpha removed"), fixture.messages());
        assertEquals(List.of("Bravo"), fixture.teamRegistry().getTeamNames());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertReturnedTo(AdminMenuType.TEAMS, "Teams (page 1)", 0);
        assertEquals("Bravo", fixture.lastMenu().findTile(TEAMS_FIRST_TEAM_SLOT).orElseThrow().displayName());
    }

    @Test
    void removesTheTeamAfterYesAndReturnsWithTheSecondPageToATeamListThatHasOnlyOnePageLeft() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_NEXT_PAGE_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);
        fixture.click(TEAM_REMOVE_SLOT);

        fixture.click(YES_SLOT);

        assertEquals(List.of("Team Team46 removed"), fixture.messages());
        assertEquals(45, fixture.teamRegistry().getTeamCount());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertReturnedTo(AdminMenuType.TEAMS, "Teams (page 1)", 1);
        assertEquals("Team01", fixture.lastMenu().findTile(TEAMS_FIRST_TEAM_SLOT).orElseThrow().displayName());
        assertEquals(List.of("Registered teams: 45", "Page 1 of 1", "A click on a team opens its options"),
                fixture.lastMenu().findTile(47).orElseThrow().loreLines());
    }

    @Test
    void reportsAnUnknownTeamAfterYesWhenTheTeamWasRemovedMeanwhile() {
        fixture.registerTeams("Alpha", "Bravo");
        openTheRemovalConfirmationOfTheFirstTeam();
        assertTrue(fixture.teamRegistry().removeTeam("Alpha"));

        fixture.click(YES_SLOT);

        assertEquals(List.of("Unknown team Alpha"), fixture.messages());
        assertEquals(0, fixture.teamConfigurationSaveCount());
        assertReturnedTo(AdminMenuType.TEAMS, "Teams (page 1)", 0);
    }

    @Test
    void keepsTheTeamAfterNoAndReturnsToTheTeamListWithTheSecondPage() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_NEXT_PAGE_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);
        fixture.click(TEAM_REMOVE_SLOT);

        fixture.click(NO_SLOT);

        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(46, fixture.teamRegistry().getTeamCount());
        assertEquals(0, fixture.teamConfigurationSaveCount());
        assertReturnedTo(AdminMenuType.TEAMS, "Teams (page 2)", 1);
    }

    @Test
    void importsTheTeamsAfterYesAndReturnsToTheFirstPageThatShowsTheImportedTeams() {
        fixture.registerTeams("Alpha", "Bravo");
        fixture.makePlayersKnown("Steve", "Alex", "Notch", "Jeb_");
        fixture.placeImportFile(IMPORT_FILE);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_IMPORT_SLOT);

        fixture.click(YES_SLOT);

        assertEquals(List.of("2 teams imported from event.json, the 2 teams that were registered before are gone",
                "Registered now: Echo, Foxtrot",
                "teams.yml was written, the imported teams survive a restart"), fixture.messages());
        assertEquals(List.of("Echo", "Foxtrot"), fixture.teamRegistry().getTeamNames());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertReturnedTo(AdminMenuType.TEAMS, "Teams (page 1)", 0);
        assertEquals("Echo", fixture.lastMenu().findTile(TEAMS_FIRST_TEAM_SLOT).orElseThrow().displayName());
    }

    @Test
    void importsTheTeamsAfterYesAndReturnsWithTheSecondPageToATeamListThatHasOnlyOnePageLeft() {
        fixture.registerNumberedTeams(46);
        fixture.makePlayersKnown("Steve", "Alex", "Notch", "Jeb_");
        fixture.placeImportFile(IMPORT_FILE);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_NEXT_PAGE_SLOT);
        fixture.click(TEAMS_IMPORT_SLOT);

        fixture.click(YES_SLOT);

        assertEquals(List.of("Echo", "Foxtrot"), fixture.teamRegistry().getTeamNames());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertReturnedTo(AdminMenuType.TEAMS, "Teams (page 1)", 1);
        assertEquals("Echo", fixture.lastMenu().findTile(TEAMS_FIRST_TEAM_SLOT).orElseThrow().displayName());
    }

    @Test
    void reportsAFailedImportAfterYesAndKeepsTheTeams() {
        fixture.registerTeams("Alpha", "Bravo");
        fixture.placeImportFile(IMPORT_FILE);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_IMPORT_SLOT);

        fixture.click(YES_SLOT);

        assertEquals(List.of("Not a single team of event.json could be imported",
                "The registered teams stay exactly as they are",
                "2 entries of the file could not be used:",
                "  Team Echo: unknown player Steve, the player has to be online or known to the server",
                "  Team Foxtrot: unknown player Notch, the player has to be online or known to the server"),
                fixture.messages());
        assertEquals(List.of("Alpha", "Bravo"), fixture.teamRegistry().getTeamNames());
        assertEquals(0, fixture.teamConfigurationSaveCount());
        assertReturnedTo(AdminMenuType.TEAMS, "Teams (page 1)", 0);
    }

    @Test
    void keepsTheTeamsAfterNoInsteadOfTheImport() {
        fixture.registerTeams("Alpha", "Bravo");
        fixture.makePlayersKnown("Steve", "Alex", "Notch", "Jeb_");
        fixture.placeImportFile(IMPORT_FILE);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_IMPORT_SLOT);

        fixture.click(NO_SLOT);

        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(List.of("Alpha", "Bravo"), fixture.teamRegistry().getTeamNames());
        assertEquals(0, fixture.teamConfigurationSaveCount());
        assertReturnedTo(AdminMenuType.TEAMS, "Teams (page 1)", 0);
    }

    @Test
    void cancelsTheTournamentAfterYesAndReturnsToTheMatchMenuThatShowsTheCancelledTournament() {
        openTheCancelConfirmationOfARunningTournament();

        fixture.click(YES_SLOT);

        assertEquals(List.of("Tournament cancelled"), fixture.messages());
        assertEquals(TournamentState.CANCELLED, fixture.tournamentManager().getState());
        assertFalse(fixture.matchService().hasActiveMatch());
        assertReturnedTo(AdminMenuType.MATCH, "Match and tournament", 0);
        assertEquals(List.of("Tournament state: CANCELLED", "Running match: none", "Round timer: running"),
                infoLore());
    }

    @Test
    void reportsAfterYesThatNoTournamentIsInProgressWhenThereIsNoneToCancel() {
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_CANCEL_SLOT);

        fixture.click(YES_SLOT);

        assertEquals(List.of("No tournament is in progress"), fixture.messages());
        assertEquals(TournamentState.IDLE, fixture.tournamentManager().getState());
        assertReturnedTo(AdminMenuType.MATCH, "Match and tournament", 0);
    }

    @Test
    void keepsTheTournamentRunningAfterNoAndReturnsToTheMatchMenu() {
        openTheCancelConfirmationOfARunningTournament();

        fixture.click(NO_SLOT);

        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
        assertTrue(fixture.matchService().hasActiveMatch());
        assertReturnedTo(AdminMenuType.MATCH, "Match and tournament", 0);
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        openTheResetConfirmation();
        AdminMenuHolder confirmationHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(YES_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(confirmationHolder, fixture.lastMenu().holder());
        assertTrue(fixture.arenaRegistry().getArena().isFullyConfigured());
        assertEquals(0, fixture.arenaConfigurationSaveCount());
    }

    private void openTheCleaningConfirmation() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(ARENA_CLEAN_SLOT);
        assertEquals(Optional.of(ConfirmationRequest.CLEAN_ARENA), fixture.lastMenu().holder().getConfirmationRequest());
    }

    private void openTheResetConfirmation() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(ARENA_RESET_SLOT);
        assertEquals(Optional.of(ConfirmationRequest.RESET_ARENA), fixture.lastMenu().holder().getConfirmationRequest());
    }

    private void openTheReplacementConfirmationOfTheFirstKit() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_FIRST_KIT_SLOT);
        fixture.click(KIT_REPLACE_SLOT);
        assertEquals(Optional.of(ConfirmationRequest.REPLACE_KIT), fixture.lastMenu().holder().getConfirmationRequest());
        fixture.messages().clear();
    }

    private void openTheRemovalConfirmationOfTheFirstTeam() {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);
        fixture.click(TEAM_REMOVE_SLOT);
        assertEquals(Optional.of(ConfirmationRequest.REMOVE_TEAM), fixture.lastMenu().holder().getConfirmationRequest());
    }

    private void openTheCancelConfirmationOfARunningTournament() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_CANCEL_SLOT);
        assertEquals(Optional.of(ConfirmationRequest.CANCEL_TOURNAMENT),
                fixture.lastMenu().holder().getConfirmationRequest());
    }

    private List<String> infoLore() {
        return fixture.lastMenu().findTile(INFO_SLOT).orElseThrow().loreLines();
    }

    private void assertReturnedToTheKitOptions(String kitName, int pageIndex) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.KIT_ACTIONS, menu.holder().getMenuType());
        assertEquals("Kit " + kitName, menu.title());
        assertEquals(Optional.of(kitName), menu.holder().getSubject());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertTrue(menu.holder().getConfirmationRequest().isEmpty());
        assertFalse(fixture.isMenuClosed());
    }

    private void assertReturnedTo(AdminMenuType menuType, String title, int pageIndex) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(menuType, menu.holder().getMenuType());
        assertEquals(title, menu.title());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertTrue(menu.holder().getSubject().isEmpty());
        assertTrue(menu.holder().getConfirmationRequest().isEmpty());
        assertFalse(fixture.isMenuClosed());
    }
}
