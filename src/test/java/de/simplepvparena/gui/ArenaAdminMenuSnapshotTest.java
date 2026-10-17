package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaAdminMenuSnapshotTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int ARENA_CLEAN_SLOT = 15;
    private static final int ARENA_RESET_SLOT = 16;
    private static final int CONFIRM_YES_SLOT = 11;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheArenaMenuOfAnIncompleteArena() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("""
                Arena | 27
                4 | BOOK | Arena | Arena incomplete /   fight area: not calculated / \
                  first team spawn: not configured /   second team spawn: not configured / \
                  tribune spawn: not configured /   running match: none
                10 | SPYGLASS | Calculate the fight area | Runs the calculation for the position you stand on / \
                Stand inside the closed border wall before you click / \
                The menu closes so you can see the recognized wall
                11 | REDSTONE_TORCH | Diagnose with a higher cell limit | One calculation with a limit you type, up to \
                5000000 cells / Shows how far a fill through a gap really gets / config.yml stays untouched
                12 | BLUE_BED | Set the first team spawn | Sets it to the position you stand on / \
                Stand inside the border wall, where the first team starts / \
                A position on the wrong side of the fight area / is refused once the area is calculated
                13 | RED_BED | Set the second team spawn | Sets it to the position you stand on / \
                Stand inside the border wall, where the second team starts / \
                A position on the wrong side of the fight area / is refused once the area is calculated
                14 | YELLOW_BED | Set the tribune spawn | Sets it to the position you stand on / \
                Stand outside the border wall, where the spectators wait / \
                A position on the wrong side of the fight area / is refused once the area is calculated
                15 | TNT | Clean the arena | No fight area is calculated, there is nothing to clean
                16 | BARRIER | Reset the arena | Drops the fight area and all three spawns / \
                The blocks and the border blocks stay untouched / Asks for a confirmation first
                19 | TARGET | Mark the height of the fight area | Hands you the item that marks the lowest and the / \
                highest point of the fight area / Place a block with it, only its height is kept / \
                and the block disappears again / The next calculation takes them over as the floor / \
                and the ceiling of the fight area, also below the / border wall, and uses both marks up / \
                No height is marked / The next placed block marks the lowest point / \
                Both ends have to be marked, a single mark is not used
                20 | IRON_BARS | Give start gate blocks | Asks for a material and an amount in the chat and / \
                hands you that many start gate blocks / Build a box around each team spawn and close it / \
                with them: every placed one is part of the gate, / breaking one takes it out again / \
                Every round start puts the whole gate back and takes / \
                it apart again from the top, with a countdown / \
                A gate nobody placed is skipped, the round still starts / The gate holds no block yet / \
                It comes down over 10 seconds
                22 | OAK_DOOR | Back | Returns to the main menu
                """, fixture.describeLastMenu());
    }

    @Test
    void showsOnlyTheMissingFightAreaOnTheCleanTileAfterTheArenaWasReset() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(ARENA_RESET_SLOT);
        fixture.click(CONFIRM_YES_SLOT);

        assertEquals("15 | TNT | Clean the arena | No fight area is calculated, there is nothing to clean",
                describeTile(ARENA_CLEAN_SLOT));
    }

    @Test
    void keepsTheCleanTileOfACalculatedFightAreaWithoutSpawns() {
        fixture.storeTheFightArea();

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("""
                15 | TNT | Clean the arena | No baseline is stored, every block of the / fight area is set to air / \
                A baseline is taken by the calculation and is / gone after a server restart / \
                Covers y 64 to y 67, the height of the fight area / 100 blocks are affected / \
                Asks for a confirmation first""", describeTile(ARENA_CLEAN_SLOT));
    }

    @Test
    void showsTheArenaMenuOfAReadyArena() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("""
                Arena | 27
                4 | BOOK | Arena | Arena ready /   fight area: admin-menu with 25 cells between 1/1 and 5/5, from y 64 \
                to y 67, calculated at y 64 /   first team spawn: not configured / \
                  second team spawn: not configured /   tribune spawn: admin-menu 11.5/64.0/11.5 /   running match: none
                10 | SPYGLASS | Calculate the fight area | Runs the calculation for the position you stand on / \
                Stand inside the closed border wall before you click / \
                The menu closes so you can see the recognized wall
                11 | REDSTONE_TORCH | Diagnose with a higher cell limit | One calculation with a limit you type, up to \
                5000000 cells / Shows how far a fill through a gap really gets / config.yml stays untouched
                12 | BLUE_BED | Set the first team spawn | Sets it to the position you stand on / \
                Stand inside the border wall, where the first team starts / \
                A position on the wrong side of the fight area / is refused once the area is calculated
                13 | RED_BED | Set the second team spawn | Sets it to the position you stand on / \
                Stand inside the border wall, where the second team starts / \
                A position on the wrong side of the fight area / is refused once the area is calculated
                14 | YELLOW_BED | Set the tribune spawn | Sets it to the position you stand on / \
                Stand outside the border wall, where the spectators wait / \
                A position on the wrong side of the fight area / is refused once the area is calculated
                15 | TNT | Clean the arena | No baseline is stored, every block of the / fight area is set to air / \
                A baseline is taken by the calculation and is / gone after a server restart / \
                Covers y 64 to y 67, the height of the fight area / 100 blocks are affected / \
                Asks for a confirmation first
                16 | BARRIER | Reset the arena | Drops the fight area and all three spawns / \
                The blocks and the border blocks stay untouched / Asks for a confirmation first
                19 | TARGET | Mark the height of the fight area | Hands you the item that marks the lowest and the / \
                highest point of the fight area / Place a block with it, only its height is kept / \
                and the block disappears again / The next calculation takes them over as the floor / \
                and the ceiling of the fight area, also below the / border wall, and uses both marks up / \
                No height is marked / The next placed block marks the lowest point / \
                Both ends have to be marked, a single mark is not used
                20 | IRON_BARS | Give start gate blocks | Asks for a material and an amount in the chat and / \
                hands you that many start gate blocks / Build a box around each team spawn and close it / \
                with them: every placed one is part of the gate, / breaking one takes it out again / \
                Every round start puts the whole gate back and takes / \
                it apart again from the top, with a countdown / \
                A gate nobody placed is skipped, the round still starts / The gate holds no block yet / \
                It comes down over 10 seconds
                22 | OAK_DOOR | Back | Returns to the main menu
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheArenaMenuWhileAMatchStandsOnTheArenaAndTheCleaningIsRefused() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("""
                Arena | 27
                4 | BOOK | Arena | Arena ready /   fight area: admin-menu with 25 cells between 1/1 and 5/5, from y 64 \
                to y 67, calculated at y 64 /   first team spawn: not configured / \
                  second team spawn: not configured /   tribune spawn: admin-menu 11.5/64.0/11.5 / \
                  running match: M1 (Alpha 0 : 0 Bravo)
                10 | SPYGLASS | Calculate the fight area | Runs the calculation for the position you stand on / \
                Stand inside the closed border wall before you click / \
                The menu closes so you can see the recognized wall
                11 | REDSTONE_TORCH | Diagnose with a higher cell limit | One calculation with a limit you type, up to \
                5000000 cells / Shows how far a fill through a gap really gets / config.yml stays untouched
                12 | BLUE_BED | Set the first team spawn | Sets it to the position you stand on / \
                Stand inside the border wall, where the first team starts / \
                A position on the wrong side of the fight area / is refused once the area is calculated
                13 | RED_BED | Set the second team spawn | Sets it to the position you stand on / \
                Stand inside the border wall, where the second team starts / \
                A position on the wrong side of the fight area / is refused once the area is calculated
                14 | YELLOW_BED | Set the tribune spawn | Sets it to the position you stand on / \
                Stand outside the border wall, where the spectators wait / \
                A position on the wrong side of the fight area / is refused once the area is calculated
                15 | TNT | Clean the arena | No baseline is stored, every block of the / fight area is set to air / \
                A baseline is taken by the calculation and is / gone after a server restart / \
                Covers y 64 to y 67, the height of the fight area / 100 blocks are affected / \
                Asks for a confirmation first
                16 | BARRIER | Reset the arena | Drops the fight area and all three spawns / \
                The blocks and the border blocks stay untouched / Asks for a confirmation first
                19 | TARGET | Mark the height of the fight area | Hands you the item that marks the lowest and the / \
                highest point of the fight area / Place a block with it, only its height is kept / \
                and the block disappears again / The next calculation takes them over as the floor / \
                and the ceiling of the fight area, also below the / border wall, and uses both marks up / \
                No height is marked / The next placed block marks the lowest point / \
                Both ends have to be marked, a single mark is not used
                20 | IRON_BARS | Give start gate blocks | Asks for a material and an amount in the chat and / \
                hands you that many start gate blocks / Build a box around each team spawn and close it / \
                with them: every placed one is part of the gate, / breaking one takes it out again / \
                Every round start puts the whole gate back and takes / \
                it apart again from the top, with a countdown / \
                A gate nobody placed is skipped, the round still starts / The gate holds no block yet / \
                It comes down over 10 seconds
                22 | OAK_DOOR | Back | Returns to the main menu
                """, fixture.describeLastMenu());
    }

    private String describeTile(int slot) {
        return fixture.describeLastMenu().lines()
                .filter(line -> line.startsWith(slot + " | "))
                .findFirst()
                .orElseThrow();
    }
}
