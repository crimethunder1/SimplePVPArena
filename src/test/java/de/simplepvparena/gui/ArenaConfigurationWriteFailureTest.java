package de.simplepvparena.gui;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaConfigurationWriteFailureTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int ARENA_TRIBUNE_SPAWN_SLOT = 14;
    private static final int ARENA_RESET_SLOT = 16;
    private static final int ARENA_STANDINGS_BOARD_SLOT = 23;
    private static final int CONFIRM_YES_SLOT = 11;

    private static final String NOT_WRITTEN = "The change holds, but config.yml could not be written: disk full";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void tellsTheAdminWhoSetsASpawnThatConfigYmlWasNotWritten() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.messages().clear();
        fixture.failArenaConfigurationWrites(new IOException("disk full"));

        fixture.click(ARENA_TRIBUNE_SPAWN_SLOT);

        assertTrue(fixture.messages().contains(NOT_WRITTEN), fixture.messages().toString());
        assertTrue(fixture.arenaRegistry().getArena().getTribuneSpawn().isPresent(), "the spawn holds in memory");
    }

    @Test
    void tellsTheAdminWhoCalculatesTheFightAreaThatConfigYmlWasNotWritten() {
        fixture.configureArena();
        fixture.standInsideTheFightArea();
        fixture.messages().clear();
        fixture.failArenaConfigurationWrites(new IOException("disk full"));

        fixture.arenaSetupService().calculateShape(fixture.admin());

        assertEquals(NOT_WRITTEN, fixture.messages().get(1), fixture.messages().toString());
        assertTrue(fixture.messages().getFirst().startsWith("The fight area was calculated"),
                fixture.messages().toString());
    }

    @Test
    void tellsTheAdminWhoResetsTheArenaThatConfigYmlWasNotWritten() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(ARENA_RESET_SLOT);
        fixture.messages().clear();
        fixture.failArenaConfigurationWrites(new IOException("disk full"));

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(NOT_WRITTEN, fixture.messages().getLast());
        assertEquals(TournamentMessages.warning(NOT_WRITTEN), fixture.lastSentMessage());
    }

    @Test
    void tellsTheAdminWhoPlacesOrRemovesTheStandingsBoardThatConfigYmlWasNotWritten() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.messages().clear();
        fixture.failArenaConfigurationWrites(new IOException("disk full"));

        fixture.click(ARENA_STANDINGS_BOARD_SLOT);
        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        assertEquals(List.of(NOT_WRITTEN, "The standings board stands here now", NOT_WRITTEN,
                "The standings board was removed"), fixture.messages());
    }

    @Test
    @UseLanguage("de")
    void tellsTheAdminInGerman() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.messages().clear();
        fixture.failArenaConfigurationWrites(new IOException("Platte voll"));

        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        assertEquals("Die Änderung gilt, aber config.yml konnte nicht geschrieben werden: Platte voll",
                fixture.messages().getFirst());
    }
}
