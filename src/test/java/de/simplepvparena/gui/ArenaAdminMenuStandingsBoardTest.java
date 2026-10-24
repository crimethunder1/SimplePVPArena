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
    void theToolPlacesTheBoardOnTheClickedBlockSavesItAndTurnsTheTileIntoTheRemoval() {
        fixture.arenaSetupService().setStandingsBoardListener(() -> boardChanges.add("changed"));
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        assertEquals("23 | OAK_SIGN | Get the standings board tool | Puts the standings board tool into your "
                + "inventory / The board shows the round, the match and the placement / of a tournament",
                describeStandingsBoardTile());

        placeTheBoardWithTheTool();
        assertEquals(List.of("The standings board stands here now"), fixture.messages());
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals(Optional.of(new PositionSnapshot("admin-menu", 7.5D, 63.0D, -2.5D, 0.0F, 0.0F)),
                fixture.arenaRegistry().getArena().getStandingsBoard());
        assertEquals(1, fixture.arenaConfigurationSaveCount());
        assertEquals(List.of("changed"), boardChanges);
        assertEquals("23 | OAK_SIGN | Remove the standings board | Stands at admin-menu 7.5/63.0/-2.5 / "
                + "Click to remove it", describeStandingsBoardTile());
    }

    @Test
    void aClickOnTheTileRemovesTheBoard() {
        fixture.arenaSetupService().setStandingsBoardListener(() -> boardChanges.add("changed"));
        placeTheBoardWithTheTool();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        assertEquals(Optional.empty(), fixture.arenaRegistry().getArena().getStandingsBoard());
        assertEquals(List.of("The standings board was removed"), fixture.messages());
        assertEquals(2, fixture.arenaConfigurationSaveCount());
        assertEquals(List.of("changed", "changed"), boardChanges);
        assertEquals("23 | OAK_SIGN | Get the standings board tool | Puts the standings board tool into your "
                + "inventory / The board shows the round, the match and the placement / of a tournament",
                describeStandingsBoardTile());
    }

    @Test
    void theResetOfTheArenaRemovesTheBoardAsWell() {
        fixture.configureArena();
        fixture.arenaSetupService().setStandingsBoardListener(() -> boardChanges.add("changed"));
        placeTheBoardWithTheTool();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

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
        placeTheBoardWithTheTool();
        assertEquals(List.of("Die Platzierungstafel steht jetzt hier"), fixture.messages());
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        assertEquals("23 | OAK_SIGN | Platzierungstafel entfernen | Steht bei admin-menu 7.5/63.0/-2.5 / "
                + "Klicke, um sie zu entfernen", describeStandingsBoardTile());

        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        assertEquals(List.of("Die Platzierungstafel wurde entfernt"), fixture.messages());
        assertEquals("23 | OAK_SIGN | Werkzeug für die Platzierungstafel holen | Legt das Werkzeug für die "
                + "Platzierungstafel in dein Inventar / Die Tafel zeigt die Turnierrunde, das Match und die "
                + "Platzierung / eines Turniers", describeStandingsBoardTile());
    }

    private void placeTheBoardWithTheTool() {
        fixture.rightClickWithTheStandingsBoardTool(7, 63, -3);
    }

    private String describeStandingsBoardTile() {
        return fixture.describeLastMenu().lines()
                .filter(line -> line.startsWith(ARENA_STANDINGS_BOARD_SLOT + " | "))
                .findFirst()
                .orElseThrow();
    }
}
