package de.simplepvparena.kit;

import de.simplepvparena.message.MessageCatalogLoader;
import de.simplepvparena.message.Messages;
import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.InMemoryKitLibraryStorage;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KitTextsTest {

    private static final String ENGLISH_NAME_RULE = "1 to 32 letters, digits, hyphens or underscores";
    private static final String GERMAN_NAME_RULE = "1 bis 32 Buchstaben, Ziffern, Bindestriche oder Unterstriche";

    private final InMemoryKitLibraryStorage storage = new InMemoryKitLibraryStorage();
    private final KitService kitService = new KitService();
    private final List<String> writeFailures = new ArrayList<>();
    private final FakeKitItemSerializer kitItemSerializer = new FakeKitItemSerializer();
    private final KitLibrary kitLibrary = new KitLibrary(storage, kitService, kitItemSerializer, problem -> { },
            writeFailures::add);
    private final KitSaveService kitSaveService = new KitSaveService(kitLibrary, kitItemSerializer);
    private final SerializedKit bow = FakeSerializedKits.carrying("bow");

    KitTextsTest() {
        SharedBukkitServer.install();
    }

    @AfterEach
    void restoreTheShippedEnglishTexts() {
        Messages.reset();
    }

    @Test
    @UseLanguage("en")
    void everyRefusalOfTheKitLibraryReadsInEnglish() {
        assertEquals(List.of(
                "A kit needs a name, a kit name has " + ENGLISH_NAME_RULE,
                "The kit name two words is not allowed, a kit name has " + ENGLISH_NAME_RULE,
                "A kit named Archer exists already",
                "Kit Archer is the default kit already",
                "Kit Archer is the last kit and cannot be deleted",
                "Kit Archer is the default kit and cannot be deleted",
                "Unknown kit Ghost",
                "The kit name Archer is used twice",
                "No default kit is named although there are kits",
                "The default kit Ghost is not one of the kits",
                "The kits could not be written into kits.yml (the disk is full), the change counts until the server "
                        + "stops and is written again with the next change of a kit"), collectRefusals());
        assertEquals(ENGLISH_NAME_RULE, KitLibrary.describeNameRule());
    }

    @Test
    @UseLanguage("de")
    void everyRefusalOfTheKitLibraryReadsInGerman() {
        assertEquals(List.of(
                "Ein Kit braucht einen Namen, ein Kit-Name hat " + GERMAN_NAME_RULE,
                "Der Kit-Name two words ist nicht erlaubt, ein Kit-Name hat " + GERMAN_NAME_RULE,
                "Ein Kit namens Archer gibt es bereits",
                "Kit Archer ist bereits das Standard-Kit",
                "Kit Archer ist das letzte Kit und lässt sich nicht löschen",
                "Kit Archer ist das Standard-Kit und lässt sich nicht löschen",
                "Unbekanntes Kit Ghost",
                "Der Kit-Name Archer kommt zweimal vor",
                "Es ist kein Standard-Kit angegeben, obwohl es Kits gibt",
                "Das Standard-Kit Ghost ist keins der Kits",
                "Die Kits konnten nicht in kits.yml geschrieben werden (the disk is full), die Änderung gilt bis zum "
                        + "Stopp des Servers und wird mit der nächsten Änderung eines Kits erneut geschrieben"),
                collectRefusals());
        assertEquals(GERMAN_NAME_RULE, KitLibrary.describeNameRule());
    }

    @Test
    @UseLanguage("en")
    void theExceptionsOfTheKitLibraryReadInEnglish() {
        assertEquals(List.of(
                "A kit named Archer exists already",
                "The kit name two words is not allowed, a kit name has " + ENGLISH_NAME_RULE,
                "Unknown kit Ghost",
                "Unknown kit Ghost",
                "Kit Archer is the last kit and cannot be deleted",
                "Unknown kit Ghost",
                "Unknown kit Ghost",
                "No default kit is named although there are kits"), collectExceptionMessages());
    }

    @Test
    @UseLanguage("de")
    void theExceptionsOfTheKitLibraryReadInGerman() {
        assertEquals(List.of(
                "Ein Kit namens Archer gibt es bereits",
                "Der Kit-Name two words ist nicht erlaubt, ein Kit-Name hat " + GERMAN_NAME_RULE,
                "Unbekanntes Kit Ghost",
                "Unbekanntes Kit Ghost",
                "Kit Archer ist das letzte Kit und lässt sich nicht löschen",
                "Unbekanntes Kit Ghost",
                "Unbekanntes Kit Ghost",
                "Es ist kein Standard-Kit angegeben, obwohl es Kits gibt"), collectExceptionMessages());
    }

    @Test
    void theKitLibraryWritesTheMessageOfAnExceptionInTheLanguageSetWhenItIsThrown() {
        kitLibrary.createKit("Archer", bow);

        Messages.use(MessageCatalogLoader.shippedCatalog("en"));
        String englishMessage = assertThrows(IllegalStateException.class, () -> kitLibrary.deleteKit("Archer"))
                .getMessage();
        Messages.use(MessageCatalogLoader.shippedCatalog("de"));
        String germanMessage = assertThrows(IllegalStateException.class, () -> kitLibrary.deleteKit("Archer"))
                .getMessage();

        assertEquals("Kit Archer is the last kit and cannot be deleted", englishMessage);
        assertEquals("Kit Archer ist das letzte Kit und lässt sich nicht löschen", germanMessage);
    }

    @Test
    @UseLanguage("en")
    void theKitDescriptionsCountOneAndTwoInEnglish() {
        assertEquals(List.of(
                "The kit is empty, no fighter is given anything",
                "Worn items: 1 of 5",
                "Inventory items: 1 of 36 slots",
                "Worn items: 2 of 5",
                "Inventory items: 2 of 36 slots",
                "Kit Knight saved: 1 item, 0 worn and 1 in the inventory",
                "It is written into kits.yml and is handed out from the next round on, a running round keeps the kit "
                        + "its fighters were given",
                "1 item could not be read back and is no part of the kit, the log names it",
                "Kit Knight saved: 2 items, 1 worn and 1 in the inventory",
                "It is written into kits.yml and is handed out once it is used as the default kit",
                "2 items could not be read back and are no part of the kit, the log names them"),
                collectKitDescriptions());
    }

    @Test
    @UseLanguage("de")
    void theKitDescriptionsCountOneAndTwoInGerman() {
        assertEquals(List.of(
                "Das Kit ist leer, kein Kämpfer bekommt etwas",
                "Getragene Items: 1 von 5",
                "Items im Inventar: 1 von 36 Plätzen",
                "Getragene Items: 2 von 5",
                "Items im Inventar: 2 von 36 Plätzen",
                "Kit Knight gespeichert: 1 Item, 0 getragen und 1 im Inventar",
                "Es wird in kits.yml geschrieben und ab der nächsten Runde ausgegeben, eine laufende Runde behält das "
                        + "Kit, das ihre Kämpfer bekommen haben",
                "1 Item ließ sich nicht zurücklesen und gehört nicht zum Kit, das Log nennt es",
                "Kit Knight gespeichert: 2 Items, 1 getragen und 1 im Inventar",
                "Es wird in kits.yml geschrieben und ausgegeben, sobald es als Standard-Kit benutzt wird",
                "2 Items ließen sich nicht zurücklesen und gehören nicht zum Kit, das Log nennt sie"),
                collectKitDescriptions());
    }

    @Test
    @UseLanguage("en")
    void theRefusalsOfTheSaveServiceReadInEnglish() {
        assertEquals(List.of(
                "Your inventory is empty, no kit was created",
                "Your inventory is empty, the kit was not changed",
                "Unknown kit Ghost",
                "A kit named Archer exists already",
                "Your inventory is empty, the kit was not changed"), collectSaveRefusals());
    }

    @Test
    @UseLanguage("de")
    void theRefusalsOfTheSaveServiceReadInGerman() {
        assertEquals(List.of(
                "Dein Inventar ist leer, es wurde kein Kit angelegt",
                "Dein Inventar ist leer, das Kit wurde nicht geändert",
                "Unbekanntes Kit Ghost",
                "Ein Kit namens Archer gibt es bereits",
                "Dein Inventar ist leer, das Kit wurde nicht geändert"), collectSaveRefusals());
    }

    private List<String> collectRefusals() {
        List<String> refusals = new ArrayList<>();
        refusals.add(KitLibrary.findNameRejection(" ").orElseThrow());
        refusals.add(KitLibrary.findNameRejection("two words").orElseThrow());
        kitLibrary.createKit("Archer", bow);
        refusals.add(kitLibrary.findCreationRejection("ARCHER").orElseThrow());
        refusals.add(kitLibrary.findDefaultRejection("Archer").orElseThrow());
        refusals.add(kitLibrary.findDeletionRejection("Archer").orElseThrow());
        kitLibrary.createKit("Knight", bow);
        refusals.add(kitLibrary.findDeletionRejection("archer").orElseThrow());
        refusals.add(kitLibrary.findDefaultRejection("Ghost").orElseThrow());
        refusals.add(KitLibrary.findInconsistency(new SavedKitLibrary(List.of(new SavedKit("archer", bow),
                new SavedKit("Archer", bow)), Optional.of("Archer"))).orElseThrow());
        refusals.add(KitLibrary.findInconsistency(new SavedKitLibrary(List.of(new SavedKit("Archer", bow)),
                Optional.empty())).orElseThrow());
        refusals.add(KitLibrary.findInconsistency(new SavedKitLibrary(List.of(new SavedKit("Archer", bow)),
                Optional.of("Ghost"))).orElseThrow());
        storage.failSaves(true);
        kitLibrary.createKit("Mage", bow);
        refusals.addAll(writeFailures);
        return refusals;
    }

    private List<String> collectExceptionMessages() {
        kitLibrary.createKit("Archer", bow);
        return List.of(
                assertThrows(IllegalArgumentException.class, () -> kitLibrary.createKit("Archer", bow)).getMessage(),
                assertThrows(IllegalArgumentException.class, () -> kitLibrary.createKit("two words", bow))
                        .getMessage(),
                assertThrows(IllegalArgumentException.class, () -> kitLibrary.replaceKit("Ghost", bow)).getMessage(),
                assertThrows(IllegalArgumentException.class, () -> kitLibrary.deleteKit("Ghost")).getMessage(),
                assertThrows(IllegalStateException.class, () -> kitLibrary.deleteKit("Archer")).getMessage(),
                assertThrows(IllegalArgumentException.class, () -> kitLibrary.setDefaultKit("Ghost")).getMessage(),
                assertThrows(IllegalArgumentException.class, () -> kitLibrary.activate("Ghost")).getMessage(),
                assertThrows(IllegalArgumentException.class, () -> kitLibrary.restore(new SavedKitLibrary(
                        List.of(new SavedKit("Archer", bow)), Optional.empty()))).getMessage());
    }

    private static List<String> collectKitDescriptions() {
        List<String> descriptions = new ArrayList<>(KitDefinition.empty().describe());
        descriptions.addAll(new KitDefinition(Map.of(KitEquipmentSlot.HEAD, new EncodableItemStack("helmet")),
                Map.of(0, new EncodableItemStack("sword"))).describe());
        descriptions.addAll(new KitDefinition(Map.of(KitEquipmentSlot.HEAD, new EncodableItemStack("helmet"),
                KitEquipmentSlot.FEET, new EncodableItemStack("boots")),
                Map.of(0, new EncodableItemStack("sword"), 1, new EncodableItemStack("bow"))).describe());
        descriptions.addAll(new KitSaveResult("Knight", true, 0, 1, 1).describe());
        descriptions.addAll(new KitSaveResult("Knight", false, 1, 1, 2).describe());
        return descriptions;
    }

    private List<String> collectSaveRefusals() {
        kitLibrary.createKit("Archer", bow);
        return List.of(
                String.join("\n", kitSaveService.createFromInventory(createPlayerWithAnEmptyInventory(), "Mage")
                        .describe()),
                String.join("\n", kitSaveService.replaceFromInventory(createPlayerWithAnEmptyInventory(), "Archer")
                        .describe()),
                String.join("\n", kitSaveService.replaceFromInventory(createPlayerWithAnEmptyInventory(), "Ghost")
                        .describe()),
                String.join("\n", kitSaveService.createFromInventory(createPlayerWithAnEmptyInventory(), "archer")
                        .describe()),
                KitSaveService.describeEmptyInventoryReplacement());
    }

    private static Player createPlayerWithAnEmptyInventory() {
        PlayerInventory inventory = SharedBukkitServer.proxy(PlayerInventory.class, (method, arguments) ->
                switch (method.getName()) {
                    case "getItem" -> (ItemStack) null;
                    default -> throw new UnsupportedOperationException("PlayerInventory." + method.getName());
                });
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getInventory" -> inventory;
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }
}
