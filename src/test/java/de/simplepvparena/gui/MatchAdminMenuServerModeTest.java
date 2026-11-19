package de.simplepvparena.gui;

import de.simplepvparena.config.ServerMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchAdminMenuServerModeTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int AUTO_MATCH_SLOT = 17;
    private static final int SERVER_MODE_SLOT = 21;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheAutoModeWithTheWayBackToTheEventMode() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("21 | HOPPER | Server mode | Auto: the arena on a running server / Auto matches and inventory "
                + "hand-in when entering the area / Click to switch to event mode", describeTile(SERVER_MODE_SLOT));
    }

    @Test
    void aClickSwitchesToTheEventModeAndGreysOutTheAutoMatchTile() {
        fixture.autoMatchService().setEnabled(true);
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(SERVER_MODE_SLOT);

        assertEquals(List.of(ServerMode.EVENT), fixture.switchedServerModes());
        assertEquals(ServerMode.EVENT, fixture.serverModeSetting().getServerMode());
        assertFalse(fixture.autoMatchService().isEnabled());
        assertEquals(List.of("The server runs in event mode now: no auto matches, inventories stay with the "
                + "players, joining players go to the tribune"), fixture.messages());
        assertEquals("21 | BEACON | Server mode | Event: a server of its own for the event / No auto matches, no "
                + "inventory hand-in, joining players go to the tribune / Click to switch to auto mode",
                describeTile(SERVER_MODE_SLOT));
        assertEquals("17 | GRAY_DYE | Auto match mode | Only available in auto mode, the server runs in event mode",
                describeTile(AUTO_MATCH_SLOT));
    }

    @Test
    void theAutoMatchTileRefusesTheClickInTheEventMode() {
        fixture.serverModeSetting().setServerMode(ServerMode.EVENT);
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(AUTO_MATCH_SLOT);

        assertFalse(fixture.autoMatchService().isEnabled());
        assertEquals(List.of(), fixture.writtenAutoMatchModes());
        assertEquals(List.of("Only available in auto mode, the server runs in event mode"), fixture.messages());
    }

    @Test
    void aSecondClickSwitchesBackToTheAutoMode() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(SERVER_MODE_SLOT);
        fixture.click(SERVER_MODE_SLOT);

        assertEquals(List.of(ServerMode.EVENT, ServerMode.AUTO), fixture.switchedServerModes());
        assertEquals(ServerMode.AUTO, fixture.serverModeSetting().getServerMode());
        assertTrue(describeTile(AUTO_MATCH_SLOT).startsWith("17 | COMPARATOR | Auto match mode | "),
                describeTile(AUTO_MATCH_SLOT));
    }

    @Test
    void aFailureToWriteKeepsTheModeAndNamesTheReason() {
        fixture.failTheServerModeSwitch(new IOException("config.yml is read only"));
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(SERVER_MODE_SLOT);

        assertEquals(ServerMode.AUTO, fixture.serverModeSetting().getServerMode());
        assertEquals(List.of("The server mode stays as it was, config.yml could not be written: config.yml is read "
                + "only"), fixture.messages());
    }

    private String describeTile(int slot) {
        return fixture.describeLastMenu().lines()
                .filter(line -> line.startsWith(slot + " | "))
                .findFirst()
                .orElseThrow();
    }
}
