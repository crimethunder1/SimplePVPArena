package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.tournament.AutoMatchState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchAdminMenuAutoMatchTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int AUTO_MATCH_SLOT = 17;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheModeSwitchedOff() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("17 | COMPARATOR | Auto match mode | The mode is off / Two teams with a player each online get "
                + "a match without a tournament / A running tournament has priority / Click to switch it on",
                describeAutoMatchTile());
    }

    @Test
    void showsTheModeSwitchedOn() {
        fixture.autoMatchService().setEnabled(true);

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("17 | COMPARATOR | Auto match mode | The mode is on / Two teams with a player each online get "
                + "a match without a tournament / A running tournament has priority / Click to switch it off",
                describeAutoMatchTile());
    }

    @Test
    void namesARunningTournamentThatComesFirst() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.autoMatchService().setEnabled(true);

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("17 | COMPARATOR | Auto match mode | The mode is on / Two teams with a player each online get "
                + "a match without a tournament / A running tournament has priority / A tournament is running, the "
                + "mode takes effect when it is over / Click to switch it off", describeAutoMatchTile());
    }

    @Test
    void namesAPausedTournamentAsWell() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.pauseTournament();

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertTrue(describeAutoMatchTile().contains(" / A tournament is running, the mode takes effect when it is "
                + "over / "), describeAutoMatchTile());
    }

    @Test
    void aClickSwitchesTheModeOnWritesItReportsItAndRefreshesTheMenu() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(AUTO_MATCH_SLOT);

        assertTrue(fixture.autoMatchService().isEnabled());
        assertEquals(List.of(true), fixture.writtenAutoMatchModes());
        assertEquals(List.of("Auto match mode is on"), fixture.messages());
        assertEquals(AdminMenuType.MATCH, fixture.lastMenu().holder().getMenuType());
        assertTrue(describeAutoMatchTile().startsWith("17 | COMPARATOR | Auto match mode | The mode is on / "),
                describeAutoMatchTile());

        fixture.messages().clear();
        fixture.click(AUTO_MATCH_SLOT);

        assertFalse(fixture.autoMatchService().isEnabled());
        assertEquals(List.of(true, false), fixture.writtenAutoMatchModes());
        assertEquals(List.of("Auto match mode is off"), fixture.messages());
        assertTrue(describeAutoMatchTile().endsWith(" / Click to switch it on"), describeAutoMatchTile());
    }

    @Test
    void theTileCanBeClickedWhileATournamentRunsAndTheModeWaitsForItsEnd() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(AUTO_MATCH_SLOT);

        assertTrue(fixture.autoMatchService().isEnabled());
        assertEquals(List.of(true), fixture.writtenAutoMatchModes());
        assertEquals(List.of("Auto match mode is on"), fixture.messages());
        assertEquals(AutoMatchState.IDLE, fixture.autoMatchService().getState());
        assertEquals("round1-match1", fixture.runningMatch().getMatchId());
    }

    @Test
    void aFailureToWriteKeepsTheModeInMemoryAndNamesTheReason() {
        fixture.failAutoMatchModeWrites(new IOException("config.yml is read only"));
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(AUTO_MATCH_SLOT);

        assertTrue(fixture.autoMatchService().isEnabled());
        assertEquals(List.of(), fixture.writtenAutoMatchModes());
        assertEquals(List.of("Auto match mode is on, but config.yml could not be written: config.yml is read only"),
                fixture.messages());
        assertTrue(describeAutoMatchTile().startsWith("17 | COMPARATOR | Auto match mode | The mode is on / "),
                describeAutoMatchTile());

        fixture.messages().clear();
        fixture.click(AUTO_MATCH_SLOT);

        assertFalse(fixture.autoMatchService().isEnabled());
        assertEquals(List.of("Auto match mode is off, but config.yml could not be written: config.yml is read "
                + "only"), fixture.messages());
    }

    @Test
    @UseLanguage("de")
    void showsTheWholeMatchMenuWithTheModeSwitchedOnInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.autoMatchService().setEnabled(true);

        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        assertEquals("""
                Match und Turnier | 27
                4 | BOOK | Turnier | Turnierstatus: läuft | Turnierrunden: 2 | laufendes Match: round1-match1 | \
                Finale: wartet auf Finalisten / Laufendes Match: round1-match1 (%1$s 0 : 0 %2$s) / Rundenzeit: läuft
                10 | GRAY_DYE | Turnier starten | Es läuft bereits ein Turnier
                11 | GRAY_DYE | Start erzwingen | Es läuft bereits ein Turnier
                12 | CLOCK | Turnier pausieren | Bis zur Fortsetzung startet kein weiteres Match / \
                Die Rundenzeit behält ihre verbleibende Zeit
                13 | RED_WOOL | Turnier abbrechen | Stoppt das laufende Match und den Turnierbaum / \
                Fragt vorher nach einer Bestätigung
                14 | LECTERN | Wartendes Match entscheiden | \
                Entscheidet ein Match, das nicht läuft, für eines seiner zwei Teams
                15 | GRAY_DYE | Finale starten | Das Finale ist nicht bereit, seine Finalisten stehen noch nicht fest
                16 | ANVIL | Ergebnis erzwingen | Entscheidet das laufende Match für eines seiner zwei Teams
                17 | COMPARATOR | Auto-Match-Modus | Der Modus ist an / \
                Zwei Teams mit je einem Spieler online bekommen ein Match ohne Turnier / \
                Ein laufendes Turnier hat Vorrang / \
                Ein Turnier läuft, der Modus wirkt, wenn es vorbei ist / \
                Klicke, um ihn auszuschalten
                22 | OAK_DOOR | Zurück | Zurück zum Hauptmenü
                """.formatted(fixture.runningFirstTeamName(), fixture.runningSecondTeamName()),
                fixture.describeLastMenu());
    }

    @Test
    @UseLanguage("de")
    void reportsTheClicksAndAFailureToWriteInGerman() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(AUTO_MATCH_SLOT);
        fixture.click(AUTO_MATCH_SLOT);
        fixture.failAutoMatchModeWrites(new IOException("kein Platz"));
        fixture.click(AUTO_MATCH_SLOT);
        fixture.click(AUTO_MATCH_SLOT);

        assertEquals(List.of("Der Auto-Match-Modus ist an", "Der Auto-Match-Modus ist aus",
                "Der Auto-Match-Modus ist an, aber config.yml konnte nicht geschrieben werden: kein Platz",
                "Der Auto-Match-Modus ist aus, aber config.yml konnte nicht geschrieben werden: kein Platz"),
                fixture.messages());
        assertEquals("17 | COMPARATOR | Auto-Match-Modus | Der Modus ist aus / Zwei Teams mit je einem Spieler "
                + "online bekommen ein Match ohne Turnier / Ein laufendes Turnier hat Vorrang / Klicke, um ihn "
                + "einzuschalten", describeAutoMatchTile());
    }

    private String describeAutoMatchTile() {
        return fixture.describeLastMenu().lines()
                .filter(line -> line.startsWith(AUTO_MATCH_SLOT + " | "))
                .findFirst()
                .orElseThrow();
    }
}
