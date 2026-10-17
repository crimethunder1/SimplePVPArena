package de.simplepvparena.gui;

import de.simplepvparena.support.EncodableItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfirmationAdminMenuSnapshotTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MAIN_KIT_SLOT = 24;

    private static final int ARENA_CLEAN_SLOT = 15;
    private static final int ARENA_RESET_SLOT = 16;
    private static final int KITS_FIRST_KIT_SLOT = 0;
    private static final int KITS_SECOND_KIT_SLOT = 1;
    private static final int KIT_REPLACE_SLOT = 13;
    private static final int KIT_DELETE_SLOT = 15;
    private static final int TEAMS_FIRST_TEAM_SLOT = 0;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;
    private static final int TEAM_REMOVE_SLOT = 13;
    private static final int MATCH_CANCEL_SLOT = 13;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheConfirmationOfTheCleaning() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(ARENA_CLEAN_SLOT);

        assertEquals("""
                Clean the arena? | 27
                4 | BOOK | Clean the arena? | No baseline is stored, every block of the / fight area is set to air / \
                A baseline is taken by the calculation and is / gone after a server restart / \
                Covers y 64 to y 67, the height of the fight area / 100 blocks are affected / \
                The change cannot be undone / The border wall around it stays untouched
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
        assertEquals(0, fixture.lastMenu().holder().getPageIndex());
    }

    @Test
    void showsTheConfirmationOfTheReset() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(ARENA_RESET_SLOT);

        assertEquals("""
                Reset the arena? | 27
                4 | BOOK | Reset the arena? | The fight area and all three spawns are dropped / \
                The blocks of the arena and the border blocks stay untouched / \
                No tournament can be started until it is calculated again
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
        assertEquals(0, fixture.lastMenu().holder().getPageIndex());
    }

    @Test
    void showsTheConfirmationOfTheReplacementOfAKit() {
        fixture.createKit("Archer");
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_FIRST_KIT_SLOT);

        fixture.click(KIT_REPLACE_SLOT);

        assertEquals("""
                Replace the kit with your inventory? | 27
                4 | BOOK | Replace the kit with your inventory? | Your inventory becomes the kit Archer / \
                Takes your helmet, chestplate, leggings, boots, / your offhand and the 36 slots of your inventory / \
                Enchantments, names, lore and damage are kept exactly / An empty slot stays empty in the kit / \
                The item on your cursor is not taken, it lies in no slot / \
                The items the kit holds now are gone afterwards
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
        assertEquals(Optional.of("Archer"), fixture.lastMenu().holder().getSubject());
        assertEquals(0, fixture.lastMenu().holder().getPageIndex());
    }

    @Test
    void showsTheConfirmationOfTheReplacementOfTheDefaultKitWhileAMatchRuns() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.createKit("Archer");
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_FIRST_KIT_SLOT);

        fixture.click(KIT_REPLACE_SLOT);

        assertEquals(List.of("Your inventory becomes the kit Archer",
                "Takes your helmet, chestplate, leggings, boots,",
                "your offhand and the 36 slots of your inventory",
                "Enchantments, names, lore and damage are kept exactly",
                "An empty slot stays empty in the kit",
                "The item on your cursor is not taken, it lies in no slot",
                "A match is running: its fighters keep their items,",
                "the new kit is handed out from the next round on",
                "The items the kit holds now are gone afterwards"),
                fixture.lastMenu().findTile(4).orElseThrow().loreLines());
    }

    @Test
    void showsTheConfirmationOfTheDeletionOfAKit() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_SECOND_KIT_SLOT);

        fixture.click(KIT_DELETE_SLOT);

        assertEquals("""
                Delete the kit? | 27
                4 | BOOK | Delete the kit? | Kit Knight is deleted / Its items are gone afterwards
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
        assertEquals(Optional.of("Knight"), fixture.lastMenu().holder().getSubject());
        assertEquals(0, fixture.lastMenu().holder().getPageIndex());
    }

    @Test
    void showsTheConfirmationOfTheRemovalOfATeamOnTheFirstPage() {
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);

        fixture.click(TEAM_REMOVE_SLOT);

        assertEquals("""
                Remove the team? | 27
                4 | BOOK | Remove the team? | Team Alpha is removed from the registry / \
                A team that plays a match cannot be removed
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
        assertEquals(0, fixture.lastMenu().holder().getPageIndex());
    }

    @Test
    void showsTheConfirmationOfTheRemovalOfATeamOnTheSecondPage() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_NEXT_PAGE_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);

        fixture.click(TEAM_REMOVE_SLOT);

        assertEquals("""
                Remove the team? | 27
                4 | BOOK | Remove the team? | Team Team46 is removed from the registry / \
                A team that plays a match cannot be removed
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
        assertEquals(1, fixture.lastMenu().holder().getPageIndex());
    }

    @Test
    void showsTheConfirmationOfTheCancelOfTheTournament() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(MATCH_CANCEL_SLOT);

        assertEquals("""
                Cancel the tournament? | 27
                4 | BOOK | Cancel the tournament? | The running tournament is cancelled / \
                The running match is stopped and its bracket is dropped
                11 | LIME_WOOL | Yes | Carries the action out
                15 | RED_WOOL | No | Changes nothing and returns
                """, fixture.describeLastMenu());
        assertEquals(0, fixture.lastMenu().holder().getPageIndex());
    }
}
