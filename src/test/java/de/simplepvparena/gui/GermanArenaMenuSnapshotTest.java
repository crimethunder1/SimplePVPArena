package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UseLanguage("de")
class GermanArenaMenuSnapshotTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int ARENA_CLEAN_SLOT = 15;
    private static final int ARENA_RESET_SLOT = 16;
    private static final String FIXED_TILES = """
            10 | SPYGLASS | Kampffläche berechnen | Berechnet die Fläche ab der Stelle, an der du stehst / \
            Steh vor dem Klick innerhalb der geschlossenen Arenawand / \
            Das Menü schließt sich, damit du die erkannte Wand sehen kannst
            11 | REDSTONE_TORCH | Diagnose mit höherem Zellenlimit | Eine Berechnung mit einem Limit, das du \
            eingibst, bis zu 5000000 Zellen / Zeigt, wie weit eine Füllung durch eine Lücke wirklich kommt / \
            config.yml bleibt unberührt
            12 | BLUE_BED | Spawn des ersten Teams setzen | Setzt ihn auf die Stelle, an der du stehst / \
            Steh innerhalb der Arenawand, wo das erste Team startet / \
            Eine Stelle auf der falschen Seite der Kampffläche / wird abgelehnt, sobald die Fläche berechnet ist
            13 | RED_BED | Spawn des zweiten Teams setzen | Setzt ihn auf die Stelle, an der du stehst / \
            Steh innerhalb der Arenawand, wo das zweite Team startet / \
            Eine Stelle auf der falschen Seite der Kampffläche / wird abgelehnt, sobald die Fläche berechnet ist
            14 | YELLOW_BED | Tribünen-Spawn setzen | Setzt ihn auf die Stelle, an der du stehst / \
            Steh außerhalb der Arenawand, wo die Zuschauer warten / \
            Eine Stelle auf der falschen Seite der Kampffläche / wird abgelehnt, sobald die Fläche berechnet ist
            """;
    private static final String CLEAN_TILE_OF_A_CALCULATED_ARENA = """
            15 | TNT | Arena reinigen | Es ist kein Ausgangszustand gespeichert, jeder Block / \
            der Kampffläche wird auf Luft gesetzt / Einen Ausgangszustand nimmt die Berechnung auf, er ist / \
            nach einem Neustart des Servers weg / Umfasst y 64 bis y 67, die Höhe der Kampffläche / \
            100 Blöcke sind betroffen / Fragt vorher nach einer Bestätigung
            """;
    private static final String TILES_AFTER_THE_CLEAN_TILE = """
            16 | BARRIER | Arena zurücksetzen | Verwirft die Kampffläche und alle drei Spawns / \
            Die Blöcke und die Wandblöcke bleiben unberührt / Fragt vorher nach einer Bestätigung
            19 | TARGET | Höhe der Kampffläche markieren | Gibt dir das Item, das den tiefsten und den / \
            höchsten Punkt der Kampffläche markiert / Setze damit einen Block, nur seine Höhe wird gemerkt / \
            und der Block verschwindet wieder / Die nächste Berechnung übernimmt sie als Boden / \
            und Decke der Kampffläche, auch unterhalb der / Arenawand, und verbraucht beide Markierungen / \
            No height is marked / The next placed block marks the lowest point / \
            Both ends have to be marked, a single mark is not used
            20 | IRON_BARS | Starttor-Blöcke geben | Fragt im Chat nach einem Material und einer Menge und / \
            gibt dir so viele Starttor-Blöcke / Baue um jeden Team-Spawn eine Box und schließe sie / \
            mit ihnen: Jeder gesetzte gehört zum Starttor, / ein abgebauter ist wieder draußen / \
            Jeder Rundenstart stellt das ganze Starttor wieder her / \
            und baut es mit einem Countdown von oben wieder ab / \
            Ein nicht gebautes Starttor wird übersprungen, die Runde startet trotzdem / \
            The gate holds no block yet / It comes down over 10 seconds
            22 | OAK_DOOR | Zurück | Zurück zum Hauptmenü
            """;
    private static final String INFO_OF_A_READY_ARENA = "4 | BOOK | Arena | Arena bereit / "
            + "  Kampffläche: admin-menu with 25 cells between 1/1 and 5/5, from y 64 to y 67, calculated at y 64 / "
            + "  Spawn des ersten Teams: nicht gesetzt /   Spawn des zweiten Teams: nicht gesetzt / "
            + "  Tribünen-Spawn: admin-menu 11.5/64.0/11.5 /   Wirkungsbereich: Mitte 3.5/3.5, Radius 99.5 Blöcke / ";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheArenaMenuOfAnIncompleteArenaInGerman() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("Arena | 27\n"
                + "4 | BOOK | Arena | Arena unvollständig /   Kampffläche: nicht berechnet / "
                + "  Spawn des ersten Teams: nicht gesetzt /   Spawn des zweiten Teams: nicht gesetzt / "
                + "  Tribünen-Spawn: nicht gesetzt /   Wirkungsbereich: keiner, bis die Kampffläche berechnet ist / "
                + "  Laufendes Match: keins\n"
                + FIXED_TILES
                + "15 | TNT | Arena reinigen | Es ist keine Kampffläche berechnet, es gibt nichts zu reinigen\n"
                + TILES_AFTER_THE_CLEAN_TILE, fixture.describeLastMenu());
    }

    @Test
    void showsTheArenaMenuOfAReadyArenaInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("Arena | 27\n"
                + INFO_OF_A_READY_ARENA + "  Laufendes Match: keins\n"
                + FIXED_TILES + CLEAN_TILE_OF_A_CALCULATED_ARENA + TILES_AFTER_THE_CLEAN_TILE,
                fixture.describeLastMenu());
    }

    @Test
    void showsTheArenaMenuWhileAMatchRunsInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("Arena | 27\n"
                + INFO_OF_A_READY_ARENA + "  Laufendes Match: M1 (Alpha 0 : 0 Bravo)\n"
                + FIXED_TILES + CLEAN_TILE_OF_A_CALCULATED_ARENA + TILES_AFTER_THE_CLEAN_TILE,
                fixture.describeLastMenu());
    }

    @Test
    void showsTheConfirmationOfTheCleaningInGerman() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(ARENA_CLEAN_SLOT);

        assertEquals("""
                Arena reinigen? | 27
                4 | BOOK | Arena reinigen? | Es ist kein Ausgangszustand gespeichert, jeder Block / \
                der Kampffläche wird auf Luft gesetzt / Einen Ausgangszustand nimmt die Berechnung auf, er ist / \
                nach einem Neustart des Servers weg / Umfasst y 64 bis y 67, die Höhe der Kampffläche / \
                100 Blöcke sind betroffen / Die Änderung lässt sich nicht rückgängig machen / \
                Die Arenawand um sie herum bleibt unberührt
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheConfirmationOfTheResetInGerman() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(ARENA_RESET_SLOT);

        assertEquals("""
                Arena zurücksetzen? | 27
                4 | BOOK | Arena zurücksetzen? | Die Kampffläche und alle drei Spawns werden verworfen / \
                Die Blöcke der Arena und die Wandblöcke bleiben unberührt / \
                Bis zur nächsten Berechnung lässt sich kein Turnier starten
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """, fixture.describeLastMenu());
    }
}
