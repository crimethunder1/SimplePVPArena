package de.simplepvparena.gui;

import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UseLanguage("de")
class GermanKitMessagesTest {

    private static final int MAIN_KIT_SLOT = 24;
    private static final int KITS_FIRST_KIT_SLOT = 0;
    private static final int KITS_SECOND_KIT_SLOT = 1;
    private static final int KITS_CREATE_SLOT = 51;
    private static final int KIT_DEFAULT_SLOT = 11;
    private static final int KIT_REPLACE_SLOT = 13;
    private static final int KIT_DELETE_SLOT = 15;
    private static final int CONFIRMATION_YES_SLOT = 11;
    private static final int CHOICE_SECOND_KIT_SLOT = 1;
    private static final String GERMAN_NAME_RULE = "1 bis 32 Buchstaben, Ziffern, Bindestriche oder Unterstriche";
    private static final String GERMAN_PROMPT_FOOTER = "Gib abbrechen ein, um abzubrechen, die Anfrage läuft in 60 "
            + "Sekunden ab";
    private static final String OPENING_NOTICE = "Match round1-match1 (Alpha gegen Bravo): Wähle das Kit innerhalb "
            + "von 30 Sekunden, sonst gilt das Standard-Kit Archer";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void asksForTheNameOfANewKitInGerman() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.messages().clear();

        fixture.click(KITS_CREATE_SLOT);

        assertEquals(List.of(
                "Gib im Chat den Namen des neuen Kits ein",
                "Ein Kit-Name hat " + GERMAN_NAME_RULE,
                "Dein Inventar, wie es dann ist, wird das Kit",
                GERMAN_PROMPT_FOOTER), fixture.messages());
    }

    @Test
    void namesTheNameOfTheNewKitWhenTheInputIsCancelledInGerman() {
        List<String> cancellations = new ArrayList<>();

        for (String cancelWord : List.of("abbrechen", "cancel")) {
            askForTheNameOfTheNewKit();
            typeAndRunTheNextTick(cancelWord);
            cancellations.addAll(fixture.messages());
        }

        assertEquals(List.of(
                "Die Eingabe für den Namen des neuen Kits wurde abgebrochen, nichts wurde geändert",
                "Die Eingabe für den Namen des neuen Kits wurde abgebrochen, nichts wurde geändert"), cancellations);
    }

    @Test
    void createsAKitInGerman() {
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        askForTheNameOfTheNewKit();

        typeAndRunTheNextTick("Archer");

        assertEquals(List.of("Kit Archer gespeichert: 1 Item, 0 getragen und 1 im Inventar",
                "Es wird in kits.yml geschrieben und ab der nächsten Runde ausgegeben, eine laufende Runde behält das "
                        + "Kit, das ihre Kämpfer bekommen haben"), fixture.messages());
    }

    @Test
    void refusesANewKitInGerman() {
        List<String> refusals = new ArrayList<>();
        fixture.createKit("Archer");

        askForTheNameOfTheNewKit();
        typeAndRunTheNextTick("Mage");
        refusals.addAll(fixture.messages());
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        askForTheNameOfTheNewKit();
        typeAndRunTheNextTick("mein Kit");
        refusals.addAll(fixture.messages());
        askForTheNameOfTheNewKit();
        typeAndRunTheNextTick("ARCHER");
        refusals.addAll(fixture.messages());

        assertEquals(List.of(
                "Dein Inventar ist leer, es wurde kein Kit angelegt",
                "Der Kit-Name mein Kit ist nicht erlaubt, ein Kit-Name hat " + GERMAN_NAME_RULE,
                "Ein Kit namens Archer gibt es bereits"), refusals);
    }

    @Test
    void replacesAKitInGerman() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        openTheOptionsOfKit(KITS_SECOND_KIT_SLOT);
        fixture.click(KIT_REPLACE_SLOT);
        String emptyInventory = String.join("\n", fixture.messages());
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        openTheOptionsOfKit(KITS_SECOND_KIT_SLOT);
        fixture.click(KIT_REPLACE_SLOT);
        fixture.messages().clear();

        fixture.click(CONFIRMATION_YES_SLOT);

        assertEquals("Dein Inventar ist leer, das Kit wurde nicht geändert", emptyInventory);
        assertEquals(List.of("Kit Knight gespeichert: 1 Item, 0 getragen und 1 im Inventar",
                "Es wird in kits.yml geschrieben und ausgegeben, sobald es als Standard-Kit benutzt wird"),
                fixture.messages());
    }

    @Test
    void usesAKitAsTheDefaultKitAndDeletesAKitInGerman() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        openTheOptionsOfKit(KITS_FIRST_KIT_SLOT);
        fixture.click(KIT_DEFAULT_SLOT);
        String alreadyDefault = String.join("\n", fixture.messages());
        openTheOptionsOfKit(KITS_SECOND_KIT_SLOT);
        fixture.click(KIT_DEFAULT_SLOT);
        String defaultKitSet = String.join("\n", fixture.messages());
        openTheOptionsOfKit(KITS_FIRST_KIT_SLOT);
        fixture.click(KIT_DELETE_SLOT);
        fixture.messages().clear();

        fixture.click(CONFIRMATION_YES_SLOT);

        assertEquals("Kit Archer ist bereits das Standard-Kit", alreadyDefault);
        assertEquals("Kit Knight ist jetzt das Standard-Kit, jeder Kämpfer bekommt es ab der nächsten Runde",
                defaultKitSet);
        assertEquals(List.of("Kit Archer gelöscht"), fixture.messages());
    }

    @Test
    void announcesTheKitChoiceAndItsReminderAndItsExpiryInGerman() {
        AdminMenuFixture kitChoiceFixture = beginTheKitChoice();
        List<String> noticesAtTheStart = List.copyOf(kitChoiceFixture.adminNotifications());

        kitChoiceFixture.kitChoiceScheduler().advanceSeconds(20);
        List<String> noticesAfterTwentySeconds = List.copyOf(kitChoiceFixture.adminNotifications());
        kitChoiceFixture.kitChoiceScheduler().advanceSeconds(10);

        assertEquals(List.of(OPENING_NOTICE), noticesAtTheStart);
        assertEquals(List.of(OPENING_NOTICE, "Noch 10 Sekunden, um das Kit für Match round1-match1 zu wählen"),
                noticesAfterTwentySeconds);
        assertEquals(List.of("Für Match round1-match1 wurde kein Kit gewählt, das Standard-Kit Archer gilt"),
                kitChoiceFixture.messages());
    }

    @Test
    void namesTheChosenKitInGerman() {
        AdminMenuFixture kitChoiceFixture = beginTheKitChoice();

        kitChoiceFixture.click(CHOICE_SECOND_KIT_SLOT);

        assertEquals(List.of("admin hat das Kit Knight für Match round1-match1 gewählt"), kitChoiceFixture.messages());
    }

    @Test
    void reportsTheCancelledKitChoiceAndAClickAfterTheChoiceInGerman() {
        AdminMenuFixture kitChoiceFixture = beginTheKitChoice();
        kitChoiceFixture.kitChoiceService().cancel();
        List<String> cancellation = List.copyOf(kitChoiceFixture.messages());
        kitChoiceFixture.messages().clear();

        kitChoiceFixture.click(CHOICE_SECOND_KIT_SLOT);

        assertEquals(List.of("Die Kit-Wahl wurde abgebrochen"), cancellation);
        assertEquals(List.of("Die Kit-Wahl für Match round1-match1 ist vorbei"), kitChoiceFixture.messages());
    }

    @Test
    void showsTheKitChoiceTileOfTheMainMenuInGerman() {
        AdminMenuFixture kitChoiceFixture = beginTheKitChoice();

        kitChoiceFixture.openMainMenu();

        assertEquals("23 | CHEST | Kit für das nächste Match wählen | Match round1-match1: Alpha gegen Bravo / "
                + "Das Fenster schließt sich, wenn die Zeit um ist oder ein Kit gewählt wurde",
                kitChoiceFixture.describeLastMenu().lines().filter(line -> line.startsWith("23 | "))
                        .findFirst().orElseThrow());
    }

    private AdminMenuFixture beginTheKitChoice() {
        AdminMenuFixture kitChoiceFixture = AdminMenuFixture.withKitChoice();
        kitChoiceFixture.createKit("Archer");
        kitChoiceFixture.createKit("Knight");
        kitChoiceFixture.kitChoiceService().begin("round1-match1", "Alpha", "Bravo", () -> { });
        kitChoiceFixture.messages().clear();
        return kitChoiceFixture;
    }

    private void askForTheNameOfTheNewKit() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_CREATE_SLOT);
        fixture.messages().clear();
    }

    private void openTheOptionsOfKit(int slot) {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(slot);
        fixture.messages().clear();
    }

    private void typeAndRunTheNextTick(String line) {
        fixture.typeInChat(line);
        fixture.runNextTick();
    }
}
