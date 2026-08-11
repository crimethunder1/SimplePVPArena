package de.simplepvparena.kit;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializedKitTest {

    private static final String KIT_PATH = "kit";

    @Test
    void keepsEveryWornSlotAndEverySlotNumberThroughTheFile() {
        SerializedKit savedKit = kitOf(Map.of(KitEquipmentSlot.HEAD, "head-item",
                        KitEquipmentSlot.OFF_HAND, "offhand-item"),
                Map.of(0, "sword", 8, "arrows", 35, "backpack"));
        MemoryConfiguration configuration = new MemoryConfiguration();

        savedKit.writeInto(configuration, KIT_PATH);
        SerializedKit readKit = SerializedKit.read(configuration.getConfigurationSection(KIT_PATH), failOnProblem());

        assertEquals(savedKit, readKit);
        assertEquals("head-item", readKit.equipmentItems().get(KitEquipmentSlot.HEAD));
        assertEquals("offhand-item", readKit.equipmentItems().get(KitEquipmentSlot.OFF_HAND));
        assertEquals("arrows", readKit.inventoryItems().get(8));
        assertEquals("backpack", readKit.inventoryItems().get(35));
    }

    @Test
    void skipsTheSlotsThatHoldNoItem() {
        SerializedKit savedKit = kitOf(Map.of(KitEquipmentSlot.CHEST, "chestplate"), Map.of(2, "bow"));
        MemoryConfiguration configuration = new MemoryConfiguration();

        savedKit.writeInto(configuration, KIT_PATH);

        assertEquals(List.of("chest"),
                new ArrayList<>(configuration.getConfigurationSection(KIT_PATH + ".equipment").getKeys(false)));
        assertEquals(List.of("2"),
                new ArrayList<>(configuration.getConfigurationSection(KIT_PATH + ".slots").getKeys(false)));
        assertEquals(2, SerializedKit.read(configuration.getConfigurationSection(KIT_PATH), failOnProblem())
                .itemCount());
    }

    @Test
    void writesTheInventorySlotsInTheOrderAPlayerSeesThem() {
        MemoryConfiguration configuration = new MemoryConfiguration();

        kitOf(Map.of(), Map.of(30, "third", 4, "second", 0, "first")).writeInto(configuration, KIT_PATH);

        assertEquals(List.of("0", "4", "30"),
                new ArrayList<>(configuration.getConfigurationSection(KIT_PATH + ".slots").getKeys(false)));
    }

    @Test
    void replacesTheKitThatStoodInTheFileBefore() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(KIT_PATH + ".armor.head.material", "DIAMOND_HELMET");
        configuration.set(KIT_PATH + ".items", List.of(Map.of("material", "DIAMOND_SWORD")));

        kitOf(Map.of(KitEquipmentSlot.FEET, "boots"), Map.of()).writeInto(configuration, KIT_PATH);

        assertNull(configuration.get(KIT_PATH + ".armor"));
        assertNull(configuration.get(KIT_PATH + ".items"));
        assertEquals(SerializedKit.FORMAT_NAME, configuration.getString(KIT_PATH + ".format"));
    }

    @Test
    void leavesTheRestOfTheFileAlone() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("match.round-timeout-seconds", 300);

        kitOf(Map.of(), Map.of(0, "sword")).writeInto(configuration, KIT_PATH);

        assertEquals(300, configuration.getInt("match.round-timeout-seconds"));
    }

    @Test
    void staysThisFormatForAnInventoryWithoutASingleItem() {
        MemoryConfiguration configuration = new MemoryConfiguration();

        SerializedKit.empty().writeInto(configuration, KIT_PATH);

        assertTrue(SerializedKit.isWrittenIn(configuration.getConfigurationSection(KIT_PATH)));
        assertTrue(SerializedKit.read(configuration.getConfigurationSection(KIT_PATH), failOnProblem()).isEmpty());
    }

    @Test
    void doesNotClaimTheKitThatWasWrittenByHand() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(KIT_PATH + ".armor.head.material", "DIAMOND_HELMET");
        configuration.set(KIT_PATH + ".items", List.of(Map.of("material", "DIAMOND_SWORD")));

        assertFalse(SerializedKit.isWrittenIn(configuration.getConfigurationSection(KIT_PATH)));
        assertFalse(SerializedKit.isWrittenIn(null));
    }

    @Test
    void skipsTheEntriesItCannotUseAndReportsEachOfThem() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(KIT_PATH + ".equipment.head", "helmet");
        configuration.set(KIT_PATH + ".equipment.belt", "there is no belt slot");
        configuration.set(KIT_PATH + ".equipment.chest", "");
        configuration.set(KIT_PATH + ".slots.0", "sword");
        configuration.set(KIT_PATH + ".slots.hotbar", "not a slot number");
        configuration.set(KIT_PATH + ".slots.36", "one slot too far");
        configuration.set(KIT_PATH + ".slots.-1", "before the first slot");
        List<String> problems = new ArrayList<>();

        SerializedKit readKit = SerializedKit.read(configuration.getConfigurationSection(KIT_PATH), problems::add);

        assertEquals(Map.of(KitEquipmentSlot.HEAD, "helmet"), readKit.equipmentItems());
        assertEquals(Map.of(0, "sword"), readKit.inventoryItems());
        assertEquals(5, problems.size(), "every skipped entry has to be reported: " + problems);
    }

    @Test
    void readsNoKitAtAllOutOfAMissingSection() {
        assertTrue(SerializedKit.read(null, failOnProblem()).isEmpty());
        assertTrue(SerializedKit.empty().isEmpty());
        assertEquals(0, SerializedKit.empty().itemCount());
    }

    @Test
    void keepsTheItemsItWasBuiltWith() {
        Map<KitEquipmentSlot, String> equipmentItems = new EnumMap<>(KitEquipmentSlot.class);
        equipmentItems.put(KitEquipmentSlot.HEAD, "helmet");
        Map<Integer, String> inventoryItems = new TreeMap<>(Map.of(0, "sword"));

        SerializedKit savedKit = new SerializedKit(equipmentItems, inventoryItems);
        equipmentItems.put(KitEquipmentSlot.FEET, "boots");
        inventoryItems.put(1, "bow");

        assertEquals(1, savedKit.equipmentItems().size());
        assertEquals(1, savedKit.inventoryItems().size());
    }

    private static SerializedKit kitOf(Map<KitEquipmentSlot, String> equipmentItems,
                                       Map<Integer, String> inventoryItems) {
        return new SerializedKit(equipmentItems, inventoryItems);
    }

    private static java.util.function.Consumer<String> failOnProblem() {
        return problem -> {
            throw new AssertionError("nothing of this kit should have been skipped: " + problem);
        };
    }
}
