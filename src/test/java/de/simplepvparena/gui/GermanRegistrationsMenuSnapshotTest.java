package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UseLanguage("de")
class GermanRegistrationsMenuSnapshotTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_REGISTRATIONS_SLOT = 52;
    private static final int TOOL_SLOT = 46;
    private static final int FORM_SLOT = 48;
    private static final int CLEAR_SLOT = 51;
    private static final int CONFIRM_YES_SLOT = 11;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheRegistrationsInGerman() {
        fixture.registerPlayers("Steve", "Alex", "Notch");

        openTheRegistrations();

        assertEquals("""
                Anmeldungen (Seite 1) | 54
                0 | PLAYER_HEAD | Steve | Klicke, um die Anmeldung zu entfernen
                1 | PLAYER_HEAD | Alex | Klicke, um die Anmeldung zu entfernen
                2 | PLAYER_HEAD | Notch | Klicke, um die Anmeldung zu entfernen
                46 | BLAZE_ROD | Werkzeug für die Anmelde-Station holen | \
                Legt das Werkzeug für die Anmelde-Station in dein Inventar
                47 | BOOK | Anmeldungen | 3 Spieler sind angemeldet / Bei der Teamgröße 2 ergibt das 2 Teams / \
                Seite 1 von 1
                48 | NETHER_STAR | Zufallsteams bilden | Bildet aus allen Angemeldeten zufällig Teams in der Teamgröße / \
                Fragt vorher nach einer Bestätigung
                49 | OAK_DOOR | Zurück | Zurück zur Team-Liste
                51 | BARRIER | Anmeldungen leeren | \
                Entfernt alle Anmeldungen und alle Teams mit einer Farbe, die Teams ohne Farbe bleiben / \
                Fragt vorher nach einer Bestätigung
                52 | RED_BED | Teammodus | Die Spieler wählen ihr Team / Klicke zum Umschalten
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheEmptyRegistrationsInGerman() {
        openTheRegistrations();

        assertEquals("""
                Anmeldungen (Seite 1) | 54
                46 | BLAZE_ROD | Werkzeug für die Anmelde-Station holen | \
                Legt das Werkzeug für die Anmelde-Station in dein Inventar
                47 | BOOK | Anmeldungen | 0 Spieler sind angemeldet / Bei der Teamgröße 2 ergibt das 0 Teams / \
                Seite 1 von 1
                48 | GRAY_DYE | Zufallsteams bilden | Es braucht mindestens 2 Teams, angemeldet sind 0 Spieler
                49 | OAK_DOOR | Zurück | Zurück zur Team-Liste
                51 | GRAY_DYE | Anmeldungen leeren | Es ist kein Spieler angemeldet
                52 | RED_BED | Teammodus | Die Spieler wählen ihr Team / Klicke zum Umschalten
                """, fixture.describeLastMenu());
    }

    @Test
    void asksTheConfirmationsInGerman() {
        fixture.registerPlayers("Steve", "Alex", "Notch");
        openTheRegistrations();

        fixture.click(FORM_SLOT);

        assertEquals("""
                Zufallsteams bilden? | 27
                4 | BOOK | Zufallsteams bilden? | Alle vorhandenen Teams werden ersetzt / Es entstehen 2 Teams
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """, fixture.describeLastMenu());
        openTheRegistrations();
        fixture.click(CLEAR_SLOT);
        assertEquals("""
                Anmeldungen leeren? | 27
                4 | BOOK | Anmeldungen leeren? | 3 Anmeldungen werden entfernt / \
                Alle Teams mit einer Farbe werden entfernt, die Teams ohne Farbe bleiben
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """, fixture.describeLastMenu());
    }

    @Test
    void reportsTheFormedTeamsAndTheToolInGerman() {
        fixture.registerPlayers("Steve", "Alex", "Notch");
        openTheRegistrations();

        fixture.click(TOOL_SLOT);
        fixture.click(FORM_SLOT);
        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Das Werkzeug für die Anmelde-Station liegt in deinem Inventar",
                "Aus 3 angemeldeten Spielern wurden 2 Teams gebildet"), fixture.messages());
        assertEquals(List.of("Rot", "Blau"), fixture.teamRegistry().getTeamNames());
    }

    private void openTheRegistrations() {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_REGISTRATIONS_SLOT);
        fixture.messages().clear();
    }
}
