package de.simplepvparena.gui;

import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UseLanguage("de")
class GermanArenaMenuSnapshotTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int ARENA_CLEAN_SLOT = 15;
    private static final int ARENA_RESET_SLOT = 16;
    private static final int INFO_SLOT = 4;
    private static final int CLEAR_BORDER_MARKS_SLOT = 17;
    private static final int CLEAR_START_GATE_MARKS_SLOT = 25;
    private static final int CONFIRM_YES_SLOT = 11;
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
    private static final String SMALLER_EFFECT_AREA_TILE_WITHOUT_FIGHT_AREA = """
            3 | RED_DYE | Kleinerer Wirkungsbereich | Rand um die Kampffläche: 96 Blöcke / \
            Klick: 8 Blöcke, Umschalt-Klick: 1 Block
            """;
    private static final String LARGER_EFFECT_AREA_TILE_WITHOUT_FIGHT_AREA = """
            5 | LIME_DYE | Größerer Wirkungsbereich | Rand um die Kampffläche: 96 Blöcke / \
            Klick: 8 Blöcke, Umschalt-Klick: 1 Block
            """;
    private static final String SMALLER_EFFECT_AREA_TILE_OF_A_CALCULATED_ARENA = """
            3 | RED_DYE | Kleinerer Wirkungsbereich | Rand um die Kampffläche: 96 Blöcke / \
            Der Wirkungsbereich hat einen Radius von 99.5 Blöcken / Klick: 8 Blöcke, Umschalt-Klick: 1 Block
            """;
    private static final String LARGER_EFFECT_AREA_TILE_OF_A_CALCULATED_ARENA = """
            5 | LIME_DYE | Größerer Wirkungsbereich | Rand um die Kampffläche: 96 Blöcke / \
            Der Wirkungsbereich hat einen Radius von 99.5 Blöcken / Klick: 8 Blöcke, Umschalt-Klick: 1 Block
            """;
    private static final String CLEAN_TILE_OF_A_CALCULATED_ARENA = """
            15 | TNT | Arena reinigen | Es ist kein Ausgangszustand gespeichert, jeder Block / \
            der Kampffläche wird auf Luft gesetzt / Einen Ausgangszustand nimmt die Berechnung auf, er ist / \
            nach einem Neustart des Servers weg / Umfasst y 64 bis y 67, die Höhe der Kampffläche / \
            100 Blöcke sind betroffen / Fragt vorher nach einer Bestätigung
            """;
    private static final String RESET_TILE = """
            16 | BARRIER | Arena zurücksetzen | Verwirft die Kampffläche, alle drei Spawns und die Platzierungstafel / \
            Die Blöcke und die Wandblöcke bleiben unberührt / Fragt vorher nach einer Bestätigung
            """;
    private static final String BORDER_MARKS_TILE_WITHOUT_MARKS = """
            17 | GRAY_DYE | Wandmarkierungen löschen | Es ist nichts markiert
            """;
    private static final String BORDER_MARKS_TILE_OF_A_CALCULATED_ARENA = """
            17 | TNT_MINECART | Wandmarkierungen löschen | Vergisst jeden markierten Wandblock / \
            Die Blöcke in der Welt bleiben, wie sie sind / \
            Die berechnete Kampffläche bleibt, für eine neue / Berechnung müssen Wandblöcke neu markiert werden / \
            96 Wandblöcke sind markiert / Fragt vorher nach einer Bestätigung
            """;
    private static final String BORDER_MARKS_TILE_WHILE_A_MATCH_RUNS = """
            17 | GRAY_DYE | Wandmarkierungen löschen | \
            Ein Match läuft, die Arena lässt sich nur zwischen den Matches zurücksetzen
            """;
    private static final String TILES_FROM_THE_HEIGHT_MARKER_TILE = """
            19 | TARGET | Höhe der Kampffläche markieren | Gibt dir das Item, das den tiefsten und den / \
            höchsten Punkt der Kampffläche markiert / Setze damit einen Block, nur seine Höhe wird gemerkt / \
            und der Block verschwindet wieder / Die nächste Berechnung übernimmt sie als Boden / \
            und Decke der Kampffläche, auch unterhalb der / Arenawand, und verbraucht beide Markierungen / \
            Es ist keine Höhe markiert / Der nächste gesetzte Block markiert den tiefsten Punkt / \
            Beide Enden müssen markiert sein, eine einzelne Markierung wird nicht verwendet
            20 | IRON_BARS | Starttor-Blöcke geben | Fragt im Chat nach einem Material und einer Menge und / \
            gibt dir so viele Starttor-Blöcke / Baue um jeden Team-Spawn eine Box und schließe sie / \
            mit ihnen: Jeder gesetzte gehört zum Starttor, / ein abgebauter ist wieder draußen / \
            Jeder Rundenstart stellt das ganze Starttor wieder her / \
            und baut es mit einem Countdown von oben wieder ab / \
            Ein nicht gebautes Starttor wird übersprungen, die Runde startet trotzdem / \
            Das Starttor hat noch keinen Block / Es wird über 10 Sekunden abgebaut
            21 | BLAZE_POWDER | Wandpartikel | Die Partikel werden angezeigt / \
            Nur die Partikel an der bestätigten Arenawand / Klicke, um sie auszublenden
            22 | OAK_DOOR | Zurück | Zurück zum Hauptmenü
            23 | OAK_SIGN | Platzierungstafel hier aufstellen | Stellt die Tafel zwei Blöcke über dem Block auf, / \
            auf dem du stehst, am besten an der Tribüne / Sie zeigt die Turnierrunde, das Match und die Platzierung
            25 | GRAY_DYE | Starttor-Markierungen löschen | Es ist nichts markiert
            """;
    private static final String INFO_OF_A_READY_ARENA = "4 | BOOK | Arena | Arena bereit / "
            + "  Kampffläche: admin-menu mit 25 Zellen zwischen 1/1 und 5/5, von y 64 bis y 67, berechnet auf y 64 / "
            + "  Spawn des ersten Teams: nicht gesetzt /   Spawn des zweiten Teams: nicht gesetzt / "
            + "  Tribünen-Spawn: admin-menu 11.5/64.0/11.5 /   Wirkungsbereich: Mitte 3.5/3.5, Radius 99.5 Blöcke / ";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheArenaMenuOfAnIncompleteArenaInGerman() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("Arena | 27\n"
                + SMALLER_EFFECT_AREA_TILE_WITHOUT_FIGHT_AREA
                + "4 | BOOK | Arena | Arena unvollständig /   Kampffläche: nicht berechnet / "
                + "  Spawn des ersten Teams: nicht gesetzt /   Spawn des zweiten Teams: nicht gesetzt / "
                + "  Tribünen-Spawn: nicht gesetzt /   Wirkungsbereich: keiner, bis die Kampffläche berechnet ist / "
                + "  Laufendes Match: keins\n"
                + LARGER_EFFECT_AREA_TILE_WITHOUT_FIGHT_AREA + FIXED_TILES
                + "15 | TNT | Arena reinigen | Es ist keine Kampffläche berechnet, es gibt nichts zu reinigen\n"
                + RESET_TILE + BORDER_MARKS_TILE_WITHOUT_MARKS + TILES_FROM_THE_HEIGHT_MARKER_TILE,
                fixture.describeLastMenu());
    }

    @Test
    void showsTheArenaMenuOfAReadyArenaInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("Arena | 27\n" + SMALLER_EFFECT_AREA_TILE_OF_A_CALCULATED_ARENA
                + INFO_OF_A_READY_ARENA + "  Laufendes Match: keins\n"
                + LARGER_EFFECT_AREA_TILE_OF_A_CALCULATED_ARENA + FIXED_TILES + CLEAN_TILE_OF_A_CALCULATED_ARENA + RESET_TILE
                + BORDER_MARKS_TILE_OF_A_CALCULATED_ARENA + TILES_FROM_THE_HEIGHT_MARKER_TILE,
                fixture.describeLastMenu());
    }

    @Test
    void showsTheArenaMenuWhileAMatchRunsInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals("Arena | 27\n" + SMALLER_EFFECT_AREA_TILE_OF_A_CALCULATED_ARENA
                + INFO_OF_A_READY_ARENA + "  Laufendes Match: M1 (Alpha 0 : 0 Bravo)\n"
                + LARGER_EFFECT_AREA_TILE_OF_A_CALCULATED_ARENA + FIXED_TILES + CLEAN_TILE_OF_A_CALCULATED_ARENA + RESET_TILE
                + BORDER_MARKS_TILE_WHILE_A_MATCH_RUNS + TILES_FROM_THE_HEIGHT_MARKER_TILE,
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
    void showsTheTilesOfTheMarksInGerman() {
        fixture.configureArena();
        fixture.markStartGateBlocks(2);

        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        assertEquals(BORDER_MARKS_TILE_OF_A_CALCULATED_ARENA.strip(), describeTile(CLEAR_BORDER_MARKS_SLOT));
        assertEquals("25 | IRON_CHAIN | Starttor-Markierungen löschen | Vergisst jeden markierten Starttor-Block / "
                + "Die Blöcke in der Welt bleiben, wie sie sind, / die Runden beginnen ohne Starttor / "
                + "Eine Reinigung setzt diese Blöcke wie jeden / anderen Block zurück / "
                + "2 Starttor-Blöcke sind markiert / Fragt vorher nach einer Bestätigung",
                describeTile(CLEAR_START_GATE_MARKS_SLOT));
    }

    @Test
    void namesASingleMarkInTheSingularInGerman() {
        fixture.markStartGateBlocks(1);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        String tile = describeTile(CLEAR_START_GATE_MARKS_SLOT);

        fixture.click(CLEAR_START_GATE_MARKS_SLOT);
        List<String> confirmationLore = fixture.lastMenu().findTile(INFO_SLOT).orElseThrow().loreLines();
        fixture.click(CONFIRM_YES_SLOT);

        assertTrue(tile.contains(" / 1 Starttor-Block ist markiert / "), tile);
        assertEquals(List.of("1 Starttor-Markierung wird vergessen", "Die Blöcke in der Welt bleiben, wie sie sind"),
                confirmationLore);
        assertEquals(List.of("1 Starttor-Markierung wurde vergessen"), fixture.messages());
    }

    @Test
    void showsTheConfirmationOfClearingTheBorderMarksInGermanAndReportsTheYes() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(CLEAR_BORDER_MARKS_SLOT);
        String confirmation = fixture.describeLastMenu();
        fixture.click(CONFIRM_YES_SLOT);

        assertEquals("""
                Wandmarkierungen löschen? | 27
                4 | BOOK | Wandmarkierungen löschen? | 96 Wandmarkierungen werden vergessen / \
                Die Blöcke in der Welt bleiben, wie sie sind
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """, confirmation);
        assertEquals(List.of("96 Wandmarkierungen wurden vergessen"), fixture.messages());
        assertEquals("17 | GRAY_DYE | Wandmarkierungen löschen | Es ist nichts markiert",
                describeTile(CLEAR_BORDER_MARKS_SLOT));
    }

    @Test
    void showsTheConfirmationOfClearingTheStartGateMarksInGermanAndReportsAFailedWrite() {
        fixture.markStartGateBlocks(2);
        fixture.failMarkWrites(new IOException("kein Platz"));
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(CLEAR_START_GATE_MARKS_SLOT);
        String confirmation = fixture.describeLastMenu();
        fixture.click(CONFIRM_YES_SLOT);

        assertEquals("""
                Starttor-Markierungen löschen? | 27
                4 | BOOK | Starttor-Markierungen löschen? | 2 Starttor-Markierungen werden vergessen / \
                Die Blöcke in der Welt bleiben, wie sie sind
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """, confirmation);
        assertEquals(List.of("2 Starttor-Markierungen wurden vergessen, aber startgates.yml konnte nicht "
                + "geschrieben werden: kein Platz"), fixture.messages());
    }

    @Test
    void reportsThatNothingIsMarkedAndNamesTheMissingBorderMarksInTheConfirmationInGerman() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_START_GATE_MARKS_SLOT);
        fixture.click(CLEAR_BORDER_MARKS_SLOT);
        fixture.borderMarks().clear();
        fixture.refillLastMenu();

        assertEquals(List.of("Es ist nichts markiert"), fixture.messages());
        assertEquals(List.of("Es ist kein Wandblock markiert", "Die Blöcke in der Welt bleiben, wie sie sind"),
                fixture.lastMenu().findTile(INFO_SLOT).orElseThrow().loreLines());
    }

    @Test
    void namesTheMissingStartGateMarksInTheConfirmationInGerman() {
        fixture.markStartGateBlocks(1);
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        fixture.click(CLEAR_START_GATE_MARKS_SLOT);
        fixture.gateMarks().clear();
        fixture.refillLastMenu();

        assertEquals(List.of("Es ist kein Starttor-Block markiert", "Die Blöcke in der Welt bleiben, wie sie sind"),
                fixture.lastMenu().findTile(INFO_SLOT).orElseThrow().loreLines());
    }

    @Test
    void showsTheConfirmationOfTheResetInGerman() {
        fixture.configureArena();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(ARENA_RESET_SLOT);

        assertEquals("""
                Arena zurücksetzen? | 27
                4 | BOOK | Arena zurücksetzen? | Die Kampffläche, alle drei Spawns und die Platzierungstafel \
                werden verworfen / \
                Die Blöcke der Arena und die Wandblöcke bleiben unberührt / \
                Bis zur nächsten Berechnung lässt sich kein Turnier starten
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """, fixture.describeLastMenu());
    }

    private String describeTile(int slot) {
        return fixture.describeLastMenu().lines()
                .filter(line -> line.startsWith(slot + " | "))
                .findFirst()
                .orElseThrow();
    }
}
