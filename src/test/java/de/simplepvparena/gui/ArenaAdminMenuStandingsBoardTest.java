package de.simplepvparena.gui;

import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaAdminMenuStandingsBoardTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int ARENA_RESET_SLOT = 16;
    private static final int ARENA_STANDINGS_BOARD_SLOT = 23;
    private static final int CONFIRM_YES_SLOT = 11;

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final List<String> boardChanges = new ArrayList<>();

    @Test
    void aClickPlacesTheBoardOnTheBlockOfTheAdminSavesItAndTurnsTheTileIntoTheRemoval() {
        fixture.arenaSetupService().setStandingsBoardListener(() -> boardChanges.add("changed"));
        fixture.standAt(7.8D, -2.2D);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        assertEquals("23 | OAK_SIGN | Place the standings board here | Puts the board two blocks above the block / "
                + "you stand on, best at the tribune / It shows the round, the match and the placement",
                describeStandingsBoardTile());

        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        assertEquals(Optional.of(new PositionSnapshot("admin-menu", 7.5D, 64.0D, -2.5D, 0.0F, 0.0F)),
                fixture.arenaRegistry().getArena().getStandingsBoard());
        assertEquals(List.of("The standings board stands here now"), fixture.messages());
        assertEquals(1, fixture.arenaConfigurationSaveCount());
        assertEquals(List.of("changed"), boardChanges);
        assertEquals("23 | OAK_SIGN | Remove the standings board | Stands at admin-menu 7.5/64.0/-2.5 / "
                + "Click to remove it", describeStandingsBoardTile());
    }

    @Test
    void theSecondClickRemovesTheBoard() {
        fixture.arenaSetupService().setStandingsBoardListener(() -> boardChanges.add("changed"));
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        assertEquals(Optional.empty(), fixture.arenaRegistry().getArena().getStandingsBoard());
        assertEquals(List.of("The standings board stands here now", "The standings board was removed"),
                fixture.messages());
        assertEquals(2, fixture.arenaConfigurationSaveCount());
        assertEquals(List.of("changed", "changed"), boardChanges);
        assertEquals("23 | OAK_SIGN | Place the standings board here | Puts the board two blocks above the block / "
                + "you stand on, best at the tribune / It shows the round, the match and the placement",
                describeStandingsBoardTile());
    }

    @Test
    void theResetOfTheArenaRemovesTheBoardAsWell() {
        fixture.configureArena();
        fixture.arenaSetupService().setStandingsBoardListener(() -> boardChanges.add("changed"));
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        fixture.click(ARENA_RESET_SLOT);
        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(Optional.empty(), fixture.arenaRegistry().getArena().getStandingsBoard());
        assertEquals(List.of("changed", "changed"), boardChanges);
        MemoryConfiguration configuration = new MemoryConfiguration();
        fixture.arenaRegistry().saveToConfiguration(configuration);
        assertEquals(List.of(), List.copyOf(configuration.getConfigurationSection("arena.standings-board")
                .getKeys(false)));
    }

    @Test
    @UseLanguage("de")
    void placesAndRemovesTheBoardInGerman() {
        fixture.standAt(7.8D, -2.2D);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(ARENA_STANDINGS_BOARD_SLOT);
        assertEquals("23 | OAK_SIGN | Platzierungstafel entfernen | Steht bei admin-menu 7.5/64.0/-2.5 / "
                + "Klicke, um sie zu entfernen", describeStandingsBoardTile());
        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        assertEquals(List.of("Die Platzierungstafel steht jetzt hier", "Die Platzierungstafel wurde entfernt"),
                fixture.messages());
        assertEquals("23 | OAK_SIGN | Platzierungstafel hier aufstellen | Stellt die Tafel zwei Blöcke über dem "
                + "Block auf, / auf dem du stehst, am besten an der Tribüne / Sie zeigt die Turnierrunde, das Match "
                + "und die Platzierung", describeStandingsBoardTile());
    }

    private String describeStandingsBoardTile() {
        return fixture.describeLastMenu().lines()
                .filter(line -> line.startsWith(ARENA_STANDINGS_BOARD_SLOT + " | "))
                .findFirst()
                .orElseThrow();
    }
}
