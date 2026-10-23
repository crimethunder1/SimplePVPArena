package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaAdminMenuEffectMarginTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int SMALLER_EFFECT_AREA_SLOT = 3;
    private static final int LARGER_EFFECT_AREA_SLOT = 5;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void aClickOnTheLargerTileWidensTheMarginByEightBlocksWritesItAndRefreshesTheMenu() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(LARGER_EFFECT_AREA_SLOT);

        assertEquals(104, fixture.arenaRegistry().getEffectMargin());
        assertEquals("107.5", fixture.arenaRegistry().findEffectArea().orElseThrow().formatRadius());
        assertEquals(List.of(104), fixture.writtenEffectMargins());
        assertEquals(List.of("The margin is now 104 blocks, the area of effect has a radius of 107.5 blocks"),
                fixture.messages());
        assertEquals(AdminMenuType.ARENA, fixture.lastMenu().holder().getMenuType());
        assertEquals("5 | LIME_DYE | Larger area of effect | Margin around the fight area: 104 blocks / "
                + "The area of effect has a radius of 107.5 blocks / Click: 8 blocks, shift click: 1 block",
                describeTile(LARGER_EFFECT_AREA_SLOT));
    }

    @Test
    void aShiftClickChangesTheMarginByOneBlock() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.shiftClick(SMALLER_EFFECT_AREA_SLOT);
        fixture.shiftClick(LARGER_EFFECT_AREA_SLOT);
        fixture.shiftClick(LARGER_EFFECT_AREA_SLOT);

        assertEquals(97, fixture.arenaRegistry().getEffectMargin());
        assertEquals(List.of(95, 96, 97), fixture.writtenEffectMargins());
        assertEquals(List.of("The margin is now 95 blocks, the area of effect has a radius of 98.5 blocks",
                "The margin is now 96 blocks, the area of effect has a radius of 99.5 blocks",
                "The margin is now 97 blocks, the area of effect has a radius of 100.5 blocks"), fixture.messages());
    }

    @Test
    void aClickOnTheSmallerTileNarrowsTheMarginByEightBlocks() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(SMALLER_EFFECT_AREA_SLOT);

        assertEquals(88, fixture.arenaRegistry().getEffectMargin());
        assertEquals("91.5", fixture.arenaRegistry().findEffectArea().orElseThrow().formatRadius());
        assertEquals(List.of("The margin is now 88 blocks, the area of effect has a radius of 91.5 blocks"),
                fixture.messages());
    }

    @Test
    void withoutACalculatedFightAreaTheMarginChangesWithoutARadius() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(LARGER_EFFECT_AREA_SLOT);

        assertEquals(104, fixture.arenaRegistry().getEffectMargin());
        assertEquals(List.of(104), fixture.writtenEffectMargins());
        assertEquals(List.of("The margin is now 104 blocks"), fixture.messages());
        assertEquals("3 | RED_DYE | Smaller area of effect | Margin around the fight area: 104 blocks / "
                + "Click: 8 blocks, shift click: 1 block", describeTile(SMALLER_EFFECT_AREA_SLOT));
    }

    @Test
    void theMarginStopsAtZeroAndTheSmallerTileTurnsGrey() {
        fixture.configureArena();
        fixture.arenaRegistry().setEffectMargin(3);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(SMALLER_EFFECT_AREA_SLOT);

        assertEquals(0, fixture.arenaRegistry().getEffectMargin());
        assertEquals(List.of("The margin is now 0 blocks, the area of effect has a radius of 3.5 blocks"),
                fixture.messages());
        assertEquals("3 | GRAY_DYE | Smaller area of effect | The margin cannot get smaller than 0 blocks",
                describeTile(SMALLER_EFFECT_AREA_SLOT));

        fixture.messages().clear();
        fixture.shiftClick(SMALLER_EFFECT_AREA_SLOT);

        assertEquals(0, fixture.arenaRegistry().getEffectMargin());
        assertEquals(List.of(0), fixture.writtenEffectMargins());
        assertEquals(List.of("The margin cannot get smaller than 0 blocks"), fixture.messages());
    }

    @Test
    void theMarginStopsAtFiveHundredTwelveAndTheLargerTileTurnsGrey() {
        fixture.configureArena();
        fixture.arenaRegistry().setEffectMargin(510);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(LARGER_EFFECT_AREA_SLOT);

        assertEquals(512, fixture.arenaRegistry().getEffectMargin());
        assertEquals("5 | GRAY_DYE | Larger area of effect | The margin cannot get larger than 512 blocks",
                describeTile(LARGER_EFFECT_AREA_SLOT));

        fixture.messages().clear();
        fixture.click(LARGER_EFFECT_AREA_SLOT);

        assertEquals(512, fixture.arenaRegistry().getEffectMargin());
        assertEquals(List.of(512), fixture.writtenEffectMargins());
        assertEquals(List.of("The margin cannot get larger than 512 blocks"), fixture.messages());
    }

    @Test
    void aMarginOfOneBlockIsNamedInTheSingular() {
        fixture.arenaRegistry().setEffectMargin(2);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.shiftClick(SMALLER_EFFECT_AREA_SLOT);

        assertEquals(List.of("The margin is now 1 block"), fixture.messages());
        assertEquals("3 | RED_DYE | Smaller area of effect | Margin around the fight area: 1 block / "
                + "Click: 8 blocks, shift click: 1 block", describeTile(SMALLER_EFFECT_AREA_SLOT));
    }

    @Test
    void whileAMatchRunsTheTilesStayClickableButTheChangeIsRefused() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(LARGER_EFFECT_AREA_SLOT);
        fixture.shiftClick(SMALLER_EFFECT_AREA_SLOT);

        assertEquals(96, fixture.arenaRegistry().getEffectMargin());
        assertEquals(List.of(), fixture.writtenEffectMargins());
        assertEquals(List.of("A match is running, the arena can only be reset between matches",
                "A match is running, the arena can only be reset between matches"), fixture.messages());
        assertEquals("5 | LIME_DYE | Larger area of effect | Margin around the fight area: 96 blocks / "
                + "The area of effect has a radius of 99.5 blocks / Click: 8 blocks, shift click: 1 block",
                describeTile(LARGER_EFFECT_AREA_SLOT));
    }

    @Test
    void aFailureToWriteKeepsTheMarginInMemoryAndNamesTheReason() {
        fixture.configureArena();
        fixture.failEffectMarginWrites(new IOException("config.yml is read only"));
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(LARGER_EFFECT_AREA_SLOT);

        assertEquals(104, fixture.arenaRegistry().getEffectMargin());
        assertEquals("107.5", fixture.arenaRegistry().findEffectArea().orElseThrow().formatRadius());
        assertEquals(List.of(), fixture.writtenEffectMargins());
        assertEquals(List.of("The margin is now 104 blocks, the area of effect has a radius of 107.5 blocks, "
                + "but config.yml could not be written: config.yml is read only"), fixture.messages());
    }

    @Test
    @UseLanguage("de")
    void namesTheChangeAndTheLimitsInGerman() {
        fixture.configureArena();
        fixture.arenaRegistry().setEffectMargin(8);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(SMALLER_EFFECT_AREA_SLOT);
        fixture.click(SMALLER_EFFECT_AREA_SLOT);
        fixture.arenaRegistry().setEffectMargin(512);
        fixture.click(LARGER_EFFECT_AREA_SLOT);
        fixture.refillLastMenu();

        assertEquals(List.of("Der Rand ist jetzt 0 Blöcke, der Wirkungsbereich hat einen Radius von 3.5 Blöcken",
                "Der Rand kann nicht kleiner als 0 Blöcke werden",
                "Der Rand kann nicht größer als 512 Blöcke werden"), fixture.messages());
        assertEquals("5 | GRAY_DYE | Größerer Wirkungsbereich | Der Rand kann nicht größer als 512 Blöcke werden",
                describeTile(LARGER_EFFECT_AREA_SLOT));
    }

    private String describeTile(int slot) {
        return fixture.describeLastMenu().lines()
                .filter(line -> line.startsWith(slot + " | "))
                .findFirst()
                .orElseThrow();
    }
}
