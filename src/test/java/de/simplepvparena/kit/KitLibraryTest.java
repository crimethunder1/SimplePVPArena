package de.simplepvparena.kit;

import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.InMemoryKitLibraryStorage;
import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitLibraryTest {

    private static final String UNDECODABLE_LABEL = "broken";
    private static final String NAME_RULE_HINT = "a kit name has 1 to 32 letters, digits, hyphens or underscores";

    private final InMemoryKitLibraryStorage storage = new InMemoryKitLibraryStorage();
    private final KitService kitService = new KitService();
    private final List<String> itemProblems = new ArrayList<>();
    private final List<String> writeFailures = new ArrayList<>();
    private final KitLibrary kitLibrary = new KitLibrary(storage, kitService,
            new FakeKitItemSerializer(UNDECODABLE_LABEL), itemProblems::add, writeFailures::add);

    KitLibraryTest() {
        SharedBukkitServer.install();
    }

    @Test
    void startsWithoutAKitAndWithoutADefaultKit() {
        assertEquals(List.of(), kitLibrary.getKitNames());
        assertEquals(0, kitLibrary.getKitCount());
        assertEquals(Optional.empty(), kitLibrary.getDefaultKitName());
        assertTrue(kitService.getKitDefinition().isEmpty());
    }

    @Test
    void makesTheFirstCreatedKitTheDefaultKitAndHandsItOut() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow", "arrows"));

        assertEquals(Optional.of("Archer"), kitLibrary.getDefaultKitName());
        assertTrue(kitLibrary.isDefaultKit("archer"));
        assertEquals(List.of("bow", "arrows"), inventoryLabelsOf(kitService.getKitDefinition()));
    }

    @Test
    void keepsTheDefaultKitWhenAFurtherKitIsCreated() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));

        kitLibrary.createKit("Knight", FakeSerializedKits.carrying("sword"));

        assertEquals(Optional.of("Archer"), kitLibrary.getDefaultKitName());
        assertFalse(kitLibrary.isDefaultKit("Knight"));
        assertEquals(List.of("bow"), inventoryLabelsOf(kitService.getKitDefinition()));
    }

    @Test
    void listsTheKitsInTheOrderTheyWereCreatedWithTheNameAsItWasTyped() {
        kitLibrary.createKit("zeta", FakeSerializedKits.carrying("a"));
        kitLibrary.createKit("Alpha_1", FakeSerializedKits.carrying("b"));
        kitLibrary.createKit("MIDDLE-kit", FakeSerializedKits.carrying("c"));

        assertEquals(List.of("zeta", "Alpha_1", "MIDDLE-kit"), kitLibrary.getKitNames());
        assertEquals(3, kitLibrary.getKitCount());
        assertEquals("MIDDLE-kit", kitLibrary.findKit("middle-KIT").orElseThrow().name());
    }

    @Test
    void refusesANameThatDiffersFromAnExistingOneOnlyInItsCase() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> kitLibrary.createKit("ARCHER", FakeSerializedKits.carrying("sword")));

        assertEquals("A kit named Archer exists already", failure.getMessage());
        assertEquals(List.of("Archer"), kitLibrary.getKitNames());
        assertEquals(1, storage.savedLibraries().size());
    }

    @Test
    void acceptsNamesOfOneToThirtyTwoLettersDigitsHyphensAndUnderscores() {
        for (String kitName : List.of("a", "Z", "7", "-", "_", "Bow-and_Arrow_2", "a".repeat(32))) {
            assertEquals(Optional.empty(), KitLibrary.findNameRejection(kitName), kitName);
        }
    }

    @Test
    void acceptsLettersAndDigitsOfEveryScript() {
        for (String kitName : List.of("Bogenschütze", "Ärger", "ß-Kit", "日本語", "Лучник", "a-b", "a_b", "٣",
                "ü".repeat(32), "\uD835\uDD04".repeat(32))) {
            assertEquals(Optional.empty(), KitLibrary.findNameRejection(kitName), kitName);
        }
    }

    @Test
    void createsAKitWithUmlautsAndKeepsTheNameAsItWasTyped() {
        kitLibrary.createKit("Bogenschütze", FakeSerializedKits.carrying("bow"));

        assertEquals(List.of("Bogenschütze"), kitLibrary.getKitNames());
        assertEquals(Optional.of("Bogenschütze"), kitLibrary.getDefaultKitName());
        assertEquals("Bogenschütze", kitLibrary.findKit("BOGENSCHÜTZE").orElseThrow().name());
        assertEquals(Optional.of("Bogenschütze"), storage.lastSavedLibrary().defaultKitName());
    }

    @Test
    void refusesANameWithUmlautsThatDiffersFromAnExistingOneOnlyInItsCase() {
        kitLibrary.createKit("ärger", FakeSerializedKits.carrying("bow"));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> kitLibrary.createKit("ÄRGER", FakeSerializedKits.carrying("sword")));

        assertEquals("A kit named ärger exists already", failure.getMessage());
        assertEquals(Optional.of("A kit named ärger exists already"), kitLibrary.findCreationRejection("Ärger"));
        assertTrue(kitLibrary.isDefaultKit("ÄRGER"));
        assertEquals(List.of("ärger"), kitLibrary.getKitNames());
        assertEquals(1, storage.savedLibraries().size());
    }

    @Test
    void refusesAnEmptyNameAndNamesTheRule() {
        for (String kitName : List.of("", "   ")) {
            assertEquals(Optional.of("A kit needs a name, a kit name has 1 to 32 letters, digits, hyphens or "
                    + "underscores"), KitLibrary.findNameRejection(kitName));
        }
        assertEquals(Optional.of("A kit needs a name, a kit name has 1 to 32 letters, digits, hyphens or "
                + "underscores"), KitLibrary.findNameRejection(null));
    }

    @Test
    void refusesANameThatIsTooLongOrHoldsOtherCharactersAndNamesTheRule() {
        for (String kitName : List.of("a".repeat(33), "ü".repeat(33), "\uD835\uDD04".repeat(33), "two words",
                "dot.ted", "colon:", "slash/", "bow\uD83C\uDFF9", "\uD83D\uDE00", "tab\tname", " Archer")) {
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> kitLibrary.createKit(kitName, FakeSerializedKits.carrying("bow")), kitName);

            assertEquals("The kit name " + kitName + " is not allowed, " + NAME_RULE_HINT, failure.getMessage());
        }
        assertEquals(List.of(), kitLibrary.getKitNames());
        assertEquals(List.of(), storage.savedLibraries());
    }

    @Test
    void writesTheLibraryWithEveryChange() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));
        kitLibrary.createKit("Knight", FakeSerializedKits.carrying("sword"));
        kitLibrary.replaceKit("knight", FakeSerializedKits.carrying("axe"));
        kitLibrary.setDefaultKit("Knight");
        kitLibrary.deleteKit("Archer");

        assertEquals(5, storage.savedLibraries().size());
        assertEquals(new SavedKitLibrary(List.of(new SavedKit("Knight", FakeSerializedKits.carrying("axe"))),
                Optional.of("Knight")), storage.lastSavedLibrary());
    }

    @Test
    void replacesTheItemsOfAKitAndKeepsItsNameAndItsPlace() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));
        kitLibrary.createKit("Knight", FakeSerializedKits.carrying("sword"));

        LibraryKit replacedKit = kitLibrary.replaceKit("KNIGHT", FakeSerializedKits.carrying("axe", "shield"));

        assertEquals("Knight", replacedKit.name());
        assertEquals(List.of("Archer", "Knight"), kitLibrary.getKitNames());
        assertEquals(List.of("axe", "shield"), inventoryLabelsOf(kitLibrary.findKit("Knight").orElseThrow()
                .kitDefinition()));
        assertEquals(List.of("bow"), inventoryLabelsOf(kitService.getKitDefinition()),
                "a kit that is not the default kit is not handed out");
    }

    @Test
    void handsOutTheDefaultKitAgainWhenItIsReplaced() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));

        kitLibrary.replaceKit("Archer", FakeSerializedKits.carrying("crossbow"));

        assertEquals(List.of("crossbow"), inventoryLabelsOf(kitService.getKitDefinition()));
    }

    @Test
    void refusesToReplaceAnUnknownKit() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> kitLibrary.replaceKit("Ghost", FakeSerializedKits.carrying("bow")));

        assertEquals("Unknown kit Ghost", failure.getMessage());
        assertEquals(List.of(), storage.savedLibraries());
    }

    @Test
    void handsOutTheKitThatIsSetAsTheDefaultKit() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));
        kitLibrary.createKit("Knight", FakeSerializedKits.carrying("sword"));

        kitLibrary.setDefaultKit("knight");

        assertEquals(Optional.of("Knight"), kitLibrary.getDefaultKitName());
        assertEquals(List.of("sword"), inventoryLabelsOf(kitService.getKitDefinition()));
        assertEquals(Optional.of("Knight"), storage.lastSavedLibrary().defaultKitName());
    }

    @Test
    void namesTheReasonWhyTheDefaultKitCannotBeSetAgain() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));

        assertEquals(Optional.of("Kit Archer is the default kit already"), kitLibrary.findDefaultRejection("archer"));
        assertEquals(Optional.of("Unknown kit Ghost"), kitLibrary.findDefaultRejection("Ghost"));
        kitLibrary.setDefaultKit("Archer");
        assertEquals(1, storage.savedLibraries().size(), "setting the default kit again writes nothing");
    }

    @Test
    void refusesToSetAnUnknownKitAsTheDefaultKit() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> kitLibrary.setDefaultKit("Ghost"));

        assertEquals("Unknown kit Ghost", failure.getMessage());
        assertEquals(Optional.of("Archer"), kitLibrary.getDefaultKitName());
    }

    @Test
    void deletesAKitThatIsNeitherTheDefaultKitNorTheLastKit() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));
        kitLibrary.createKit("Knight", FakeSerializedKits.carrying("sword"));
        kitLibrary.createKit("Mage", FakeSerializedKits.carrying("staff"));

        assertEquals(Optional.empty(), kitLibrary.findDeletionRejection("knight"));
        kitLibrary.deleteKit("knight");

        assertEquals(List.of("Archer", "Mage"), kitLibrary.getKitNames());
        assertEquals(Optional.empty(), kitLibrary.findKit("Knight"));
        assertEquals(List.of("bow"), inventoryLabelsOf(kitService.getKitDefinition()));
    }

    @Test
    void refusesToDeleteTheLastKit() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> kitLibrary.deleteKit("Archer"));

        assertEquals("Kit Archer is the last kit and cannot be deleted", failure.getMessage());
        assertEquals(Optional.of(failure.getMessage()), kitLibrary.findDeletionRejection("archer"));
        assertEquals(List.of("Archer"), kitLibrary.getKitNames());
        assertEquals(1, storage.savedLibraries().size());
    }

    @Test
    void refusesToDeleteTheDefaultKitWhileFurtherKitsExist() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));
        kitLibrary.createKit("Knight", FakeSerializedKits.carrying("sword"));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> kitLibrary.deleteKit("ARCHER"));

        assertEquals("Kit Archer is the default kit and cannot be deleted", failure.getMessage());
        assertEquals(Optional.of(failure.getMessage()), kitLibrary.findDeletionRejection("Archer"));
        assertEquals(List.of("Archer", "Knight"), kitLibrary.getKitNames());
        assertEquals(2, storage.savedLibraries().size());
    }

    @Test
    void refusesToDeleteAnUnknownKit() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> kitLibrary.deleteKit("Ghost"));

        assertEquals("Unknown kit Ghost", failure.getMessage());
        assertEquals(Optional.of("Unknown kit Ghost"), kitLibrary.findDeletionRejection("Ghost"));
    }

    @Test
    void keepsTheChangeInMemoryWhenTheFileCannotBeWrittenAndReportsIt() {
        storage.failSaves(true);

        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));

        assertEquals(List.of("Archer"), kitLibrary.getKitNames());
        assertEquals(Optional.of("Archer"), kitLibrary.getDefaultKitName());
        assertEquals(List.of("bow"), inventoryLabelsOf(kitService.getKitDefinition()));
        assertEquals(List.of("The kits could not be written into kits.yml (the disk is full), the change counts "
                + "until the server stops and is written again with the next change of a kit"), writeFailures);
        assertEquals(List.of(), storage.savedLibraries());
    }

    @Test
    void writesTheWholeLibraryWithTheNextChangeAfterAFailedWrite() {
        storage.failSaves(true);
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));
        storage.failSaves(false);

        kitLibrary.createKit("Knight", FakeSerializedKits.carrying("sword"));

        assertEquals(1, storage.failedSaveCount());
        assertEquals(1, writeFailures.size());
        assertEquals(new SavedKitLibrary(List.of(new SavedKit("Archer", FakeSerializedKits.carrying("bow")),
                new SavedKit("Knight", FakeSerializedKits.carrying("sword"))), Optional.of("Archer")),
                storage.lastSavedLibrary());
    }

    @Test
    void reportsAnItemThatCannotBeReadBackAndLeavesItOutOfTheHandedOutKit() {
        LibraryKit kit = kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow", UNDECODABLE_LABEL));

        assertEquals(2, kit.serializedKit().itemCount());
        assertEquals(1, kit.itemCount());
        assertEquals(List.of("bow"), inventoryLabelsOf(kitService.getKitDefinition()));
        assertEquals(List.of("Kit Archer: The stored kit item of the inventory slot 1 could not be read and is "
                + "left out of the kit"), itemProblems);
    }

    @Test
    void countsTheWornAndTheCarriedItemsOfAKit() {
        LibraryKit kit = kitLibrary.createKit("Knight", FakeSerializedKits.wearingAndCarrying(
                Map.of(KitEquipmentSlot.HEAD, "helmet", KitEquipmentSlot.OFF_HAND, "shield"),
                "sword", "bow", "arrows"));

        assertEquals(2, kit.wornItemCount());
        assertEquals(3, kit.inventoryItemCount());
        assertEquals(5, kit.itemCount());
    }

    @Test
    void restoresASavedLibraryWithoutWritingItAndHandsOutItsDefaultKit() {
        kitLibrary.restore(new SavedKitLibrary(List.of(
                new SavedKit("Archer", FakeSerializedKits.carrying("bow")),
                new SavedKit("Knight", FakeSerializedKits.carrying("sword"))), Optional.of("Knight")));

        assertEquals(List.of("Archer", "Knight"), kitLibrary.getKitNames());
        assertEquals(Optional.of("Knight"), kitLibrary.getDefaultKitName());
        assertEquals(List.of("sword"), inventoryLabelsOf(kitService.getKitDefinition()));
        assertEquals(List.of(), storage.savedLibraries());
    }

    @Test
    void restoresAnEmptyLibraryWithAnEmptyKit() {
        kitService.setKitDefinition(new KitDefinition(Map.of(), Map.of(0, new EncodableItemStack("old"))));

        kitLibrary.restore(SavedKitLibrary.empty());

        assertEquals(List.of(), kitLibrary.getKitNames());
        assertTrue(kitService.getKitDefinition().isEmpty());
    }

    @Test
    void refusesToRestoreALibraryThatDoesNotFitTogether() {
        SerializedKit bow = FakeSerializedKits.carrying("bow");
        assertEquals(Optional.of("The kit name Archer is used twice"), KitLibrary.findInconsistency(
                new SavedKitLibrary(List.of(new SavedKit("archer", bow), new SavedKit("Archer", bow)),
                        Optional.of("archer"))));
        assertEquals(Optional.of("The kit name two words is not allowed, " + NAME_RULE_HINT),
                KitLibrary.findInconsistency(new SavedKitLibrary(List.of(new SavedKit("two words", bow)),
                        Optional.of("two words"))));
        assertEquals(Optional.of("The default kit Ghost is not one of the kits"), KitLibrary.findInconsistency(
                new SavedKitLibrary(List.of(new SavedKit("Archer", bow)), Optional.of("Ghost"))));
        assertEquals(Optional.of("No default kit is named although there are kits"), KitLibrary.findInconsistency(
                new SavedKitLibrary(List.of(new SavedKit("Archer", bow)), Optional.empty())));
        assertEquals(Optional.of("The default kit Ghost is not one of the kits"), KitLibrary.findInconsistency(
                new SavedKitLibrary(List.of(), Optional.of("Ghost"))));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> kitLibrary.restore(
                new SavedKitLibrary(List.of(new SavedKit("Archer", bow)), Optional.of("Ghost"))));

        assertEquals("The default kit Ghost is not one of the kits", failure.getMessage());
        assertEquals(List.of(), kitLibrary.getKitNames());
    }

    @Test
    void keepsTheChosenKitWhenAnotherKitIsSetAsTheDefaultKit() {
        createArcherKnightAndMage();
        kitLibrary.activate("Knight");

        kitLibrary.setDefaultKit("Mage");

        assertEquals(Optional.of("Mage"), kitLibrary.getDefaultKitName());
        assertSame(kitDefinitionOf("Knight"), kitService.getKitDefinition());
    }

    @Test
    void keepsTheChosenKitWhenTheDefaultKitIsReplaced() {
        createArcherKnightAndMage();
        kitLibrary.activate("Knight");

        kitLibrary.replaceKit("Archer", FakeSerializedKits.carrying("crossbow"));

        assertSame(kitDefinitionOf("Knight"), kitService.getKitDefinition());
        assertEquals(List.of("crossbow"), inventoryLabelsOf(kitDefinitionOf("Archer")));
    }

    @Test
    void keepsTheChosenKitWhenAFurtherKitIsCreated() {
        createArcherKnightAndMage();
        kitLibrary.activate("Knight");

        kitLibrary.createKit("Rogue", FakeSerializedKits.carrying("dagger"));

        assertSame(kitDefinitionOf("Knight"), kitService.getKitDefinition());
    }

    @Test
    void handsOutTheNewItemsOfTheChosenKitWhenItIsReplaced() {
        createArcherKnightAndMage();
        kitLibrary.activate("knight");

        kitLibrary.replaceKit("KNIGHT", FakeSerializedKits.carrying("axe", "shield"));

        assertEquals(List.of("axe", "shield"), inventoryLabelsOf(kitService.getKitDefinition()));
        assertSame(kitDefinitionOf("Knight"), kitService.getKitDefinition());
        assertEquals(Optional.of("Archer"), kitLibrary.getDefaultKitName());
    }

    @Test
    void endsTheChosenKitAndHandsOutTheDefaultKitWhenTheChosenKitIsDeleted() {
        createArcherKnightAndMage();
        kitLibrary.activate("Knight");

        kitLibrary.deleteKit("Knight");

        assertSame(kitDefinitionOf("Archer"), kitService.getKitDefinition());
        kitLibrary.setDefaultKit("Mage");
        assertSame(kitDefinitionOf("Mage"), kitService.getKitDefinition());
    }

    @Test
    void keepsTheChosenKitWhenAnotherKitIsDeleted() {
        createArcherKnightAndMage();
        kitLibrary.activate("Knight");

        kitLibrary.deleteKit("Mage");

        assertSame(kitDefinitionOf("Knight"), kitService.getKitDefinition());
    }

    @Test
    void endsTheChosenKitWhenTheDefaultKitIsActivated() {
        createArcherKnightAndMage();
        kitLibrary.activate("Knight");

        kitLibrary.activateDefaultKit();

        assertSame(kitDefinitionOf("Archer"), kitService.getKitDefinition());
        kitLibrary.setDefaultKit("Mage");
        assertSame(kitDefinitionOf("Mage"), kitService.getKitDefinition());
    }

    @Test
    void endsTheChosenKitWhenTheLibraryIsRestored() {
        createArcherKnightAndMage();
        kitLibrary.activate("Knight");

        kitLibrary.restore(new SavedKitLibrary(List.of(
                new SavedKit("Archer", FakeSerializedKits.carrying("bow")),
                new SavedKit("Knight", FakeSerializedKits.carrying("sword"))), Optional.of("Archer")));

        assertSame(kitDefinitionOf("Archer"), kitService.getKitDefinition());
        kitLibrary.replaceKit("Knight", FakeSerializedKits.carrying("axe"));
        assertSame(kitDefinitionOf("Archer"), kitService.getKitDefinition());
    }

    @Test
    void handsOutTheEmptyKitAndThenTheFirstCreatedKitWhenTheChosenKitIsGoneWithAllKits() {
        createArcherKnightAndMage();
        kitLibrary.activate("Knight");

        kitLibrary.restore(SavedKitLibrary.empty());

        assertTrue(kitService.getKitDefinition().isEmpty());
        kitLibrary.createKit("Knight", FakeSerializedKits.carrying("lance"));
        assertEquals(List.of("lance"), inventoryLabelsOf(kitService.getKitDefinition()));
        kitLibrary.createKit("Rogue", FakeSerializedKits.carrying("dagger"));
        kitLibrary.setDefaultKit("Rogue");
        assertSame(kitDefinitionOf("Rogue"), kitService.getKitDefinition());
    }

    @Test
    void handsOutTheSameDefinitionThatTheLibraryHolds() {
        LibraryKit kit = kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));

        assertSame(kit.kitDefinition(), kitService.getKitDefinition());
    }

    private void createArcherKnightAndMage() {
        kitLibrary.createKit("Archer", FakeSerializedKits.carrying("bow"));
        kitLibrary.createKit("Knight", FakeSerializedKits.carrying("sword"));
        kitLibrary.createKit("Mage", FakeSerializedKits.carrying("staff"));
    }

    private KitDefinition kitDefinitionOf(String kitName) {
        return kitLibrary.findKit(kitName).orElseThrow().kitDefinition();
    }

    private static List<String> inventoryLabelsOf(KitDefinition kitDefinition) {
        List<String> labels = new ArrayList<>();
        for (ItemStack itemStack : kitDefinition.inventoryItems().values()) {
            labels.add(((EncodableItemStack) itemStack).getLabel());
        }
        return labels;
    }
}
