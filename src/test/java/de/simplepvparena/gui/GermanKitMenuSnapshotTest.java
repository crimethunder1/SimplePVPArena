package de.simplepvparena.gui;

import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UseLanguage("de")
class GermanKitMenuSnapshotTest {

    private static final int MAIN_KIT_SLOT = 24;
    private static final int KITS_FIRST_KIT_SLOT = 0;
    private static final int KITS_SECOND_KIT_SLOT = 1;
    private static final int KITS_NEXT_PAGE_SLOT = 53;
    private static final int KIT_REPLACE_SLOT = 13;
    private static final int KIT_DELETE_SLOT = 15;
    private static final String CREATION_TILE = "51 | LIME_WOOL | Kit aus deinem Inventar anlegen | "
            + "Fragt im Chat nach dem Namen des neuen Kits / Nimmt Helm, Brustpanzer, Hose und Stiefel, / "
            + "die Zweithand und die 36 Plätze deines Inventars / "
            + "Verzauberungen, Namen, Beschreibungen und Abnutzung bleiben genau erhalten / "
            + "Ein leerer Platz bleibt im Kit leer / "
            + "Das Item an deinem Mauszeiger wird nicht genommen, es liegt in keinem Platz / "
            + "Das erste Kit wird das Standard-Kit / Das Menü schließt sich, damit du tippen kannst";
    private static final String INVENTORY_CAPTURE = "Nimmt Helm, Brustpanzer, Hose und Stiefel, / "
            + "die Zweithand und die 36 Plätze deines Inventars / "
            + "Verzauberungen, Namen, Beschreibungen und Abnutzung bleiben genau erhalten / "
            + "Ein leerer Platz bleibt im Kit leer / "
            + "Das Item an deinem Mauszeiger wird nicht genommen, es liegt in keinem Platz";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheEmptyKitListInGerman() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        assertEquals("""
                Kits (Seite 1) | 54
                47 | BOOK | Kits | Kits: 0 / Standard-Kit: keins / Seite 1 von 1 / \
                Ein Klick auf ein Kit öffnet seine Optionen
                49 | OAK_DOOR | Zurück | Zurück zum Hauptmenü
                """ + CREATION_TILE + "\n", fixture.describeLastMenu());
    }

    @Test
    void showsTheFirstPageOfSeveralKitsInGerman() {
        fixture.createNumberedKits(46);

        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        String description = fixture.describeLastMenu();
        assertEquals("Kits (Seite 1) | 54", description.lines().findFirst().orElseThrow());
        assertEquals("0 | CHEST | Kit01 | Getragene Items: 0 von 5 / Items im Inventar: 1 von 36 / Standard-Kit / "
                + "Klicke für die Optionen dieses Kits", lineOfSlot(description, 0));
        assertEquals("44 | CHEST | Kit45 | Getragene Items: 0 von 5 / Items im Inventar: 1 von 36 / "
                + "Klicke für die Optionen dieses Kits", lineOfSlot(description, 44));
        assertEquals("47 | BOOK | Kits | Kits: 46 / Standard-Kit: Kit01 / Seite 1 von 2 / "
                + "Ein Klick auf ein Kit öffnet seine Optionen", lineOfSlot(description, 47));
        assertEquals("49 | OAK_DOOR | Zurück | Zurück zum Hauptmenü", lineOfSlot(description, 49));
        assertEquals(CREATION_TILE, lineOfSlot(description, 51));
        assertEquals("53 | ARROW | Nächste Seite | Seite 2 von 2", lineOfSlot(description, 53));
    }

    @Test
    void showsTheSecondPageOfSeveralKitsInGerman() {
        fixture.createNumberedKits(46);
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(KITS_NEXT_PAGE_SLOT);

        assertEquals("""
                Kits (Seite 2) | 54
                0 | CHEST | Kit46 | Getragene Items: 0 von 5 / Items im Inventar: 1 von 36 / \
                Klicke für die Optionen dieses Kits
                45 | ARROW | Vorherige Seite | Seite 1 von 2
                47 | BOOK | Kits | Kits: 46 / Standard-Kit: Kit01 / Seite 2 von 2 / \
                Ein Klick auf ein Kit öffnet seine Optionen
                49 | OAK_DOOR | Zurück | Zurück zum Hauptmenü
                """ + CREATION_TILE + "\n", fixture.describeLastMenu());
    }

    @Test
    void showsTheOptionsOfTheDefaultKitInGerman() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(KITS_FIRST_KIT_SLOT);

        assertEquals("""
                Kit Archer | 27
                4 | BOOK | Archer | Getragene Items: 2 von 5 / Items im Inventar: 3 von 36 / \
                Standard-Kit: jeder Kämpfer bekommt es zum Start einer Runde
                11 | GRAY_DYE | Das ist das Standard-Kit | Kit Archer ist bereits das Standard-Kit
                13 | CHEST | Durch dein Inventar ersetzen | Dein Inventar wird das Kit Archer / \
                """ + INVENTORY_CAPTURE + """
                 / Fragt vorher nach einer Bestätigung
                15 | GRAY_DYE | Kit löschen | Kit Archer ist das Standard-Kit und lässt sich nicht löschen
                22 | OAK_DOOR | Zurück | Zurück zur Kit-Liste
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheOptionsOfAKitThatIsNotTheDefaultKitInGerman() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(KITS_SECOND_KIT_SLOT);

        assertEquals("""
                Kit Knight | 27
                4 | BOOK | Knight | Getragene Items: 2 von 5 / Items im Inventar: 3 von 36 / \
                Nicht das Standard-Kit, kein Kämpfer bekommt es
                11 | NETHER_STAR | Als Standard-Kit benutzen | Jeder Kämpfer bekommt es ab der nächsten Runde / \
                Eine laufende Runde behält das Kit, das ihre Kämpfer bekommen haben
                13 | CHEST | Durch dein Inventar ersetzen | Dein Inventar wird das Kit Knight / \
                """ + INVENTORY_CAPTURE + """
                 / Fragt vorher nach einer Bestätigung
                15 | RED_WOOL | Kit löschen | Entfernt das Kit Knight / Fragt vorher nach einer Bestätigung
                22 | OAK_DOOR | Zurück | Zurück zur Kit-Liste
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheOnlyKitAndAKitThatWasDeletedMeanwhileInGerman() {
        fixture.createKit("Archer");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_FIRST_KIT_SLOT);
        String onlyKit = lineOfSlot(fixture.describeLastMenu(), 15);
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_SECOND_KIT_SLOT);
        fixture.kitLibrary().deleteKit("Knight");

        fixture.refillLastMenu();

        String description = fixture.describeLastMenu();
        assertEquals("15 | GRAY_DYE | Kit löschen | Kit Archer ist das letzte Kit und lässt sich nicht löschen",
                onlyKit);
        assertEquals("4 | BOOK | Knight | Dieses Kit gibt es nicht mehr", lineOfSlot(description, 4));
        assertEquals("11 | GRAY_DYE | Als Standard-Kit benutzen | Unbekanntes Kit Knight", lineOfSlot(description, 11));
        assertEquals("15 | GRAY_DYE | Kit löschen | Unbekanntes Kit Knight", lineOfSlot(description, 15));
    }

    @Test
    void showsTheReplacementOfTheDefaultKitWhileAMatchRunsInGerman() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.createKit("Archer");
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_FIRST_KIT_SLOT);

        fixture.click(KIT_REPLACE_SLOT);

        assertEquals("""
                Kit durch dein Inventar ersetzen? | 27
                4 | BOOK | Kit durch dein Inventar ersetzen? | Dein Inventar wird das Kit Archer / \
                """ + INVENTORY_CAPTURE + """
                 / Ein Match läuft: seine Kämpfer behalten ihre Items, / \
                das neue Kit wird ab der nächsten Runde ausgegeben / \
                Die Items, die das Kit jetzt enthält, sind danach weg
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheDeletionOfAKitInGerman() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_SECOND_KIT_SLOT);

        fixture.click(KIT_DELETE_SLOT);

        assertEquals("""
                Kit löschen? | 27
                4 | BOOK | Kit löschen? | Kit Knight wird gelöscht / Seine Items sind danach weg
                11 | LIME_WOOL | Ja | Führt die Aktion aus
                15 | RED_WOOL | Nein | Ändert nichts und kehrt zurück
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheKitChoiceInGerman() {
        AdminMenuFixture kitChoiceFixture = AdminMenuFixture.withKitChoice();
        kitChoiceFixture.createKit("Archer");
        kitChoiceFixture.createKit("Knight");

        kitChoiceFixture.kitChoiceService().begin("round1-match1", "Alpha", "Bravo", () -> { });
        String openChoice = kitChoiceFixture.describeLastMenu();
        kitChoiceFixture.kitChoiceScheduler().advanceSeconds(29);
        kitChoiceFixture.refillLastMenu();
        String lastSecond = lineOfSlot(kitChoiceFixture.describeLastMenu(), 49);
        kitChoiceFixture.kitChoiceScheduler().advanceSeconds(1);
        kitChoiceFixture.refillLastMenu();

        assertEquals("""
                Kit für round1-match1 wählen | 54
                0 | CHEST | Archer | Getragene Items: 2 von 5 / Items im Inventar: 3 von 36 / Standard-Kit / \
                Klicke, um es für dieses Match zu benutzen
                1 | CHEST | Knight | Getragene Items: 2 von 5 / Items im Inventar: 3 von 36 / \
                Klicke, um es für dieses Match zu benutzen
                49 | BOOK | Kit-Wahl | Match round1-match1: Alpha gegen Bravo / Standard-Kit: Archer / \
                Restzeit: 30 Sekunden / Das Standard-Kit gilt, wenn niemand rechtzeitig wählt
                """, openChoice);
        assertEquals("49 | BOOK | Kit-Wahl | Match round1-match1: Alpha gegen Bravo / Standard-Kit: Archer / "
                + "Restzeit: 1 Sekunde / Das Standard-Kit gilt, wenn niemand rechtzeitig wählt", lastSecond);
        assertEquals("49 | BOOK | Kit-Wahl | Match round1-match1 / Die Kit-Wahl für Match round1-match1 ist vorbei",
                lineOfSlot(kitChoiceFixture.describeLastMenu(), 49));
    }

    private static String lineOfSlot(String description, int slot) {
        return description.lines().filter(line -> line.startsWith(slot + " | ")).findFirst().orElse("");
    }
}
