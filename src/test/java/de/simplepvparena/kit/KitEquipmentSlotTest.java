package de.simplepvparena.kit;

import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitEquipmentSlotTest {

    @Test
    void keepsTheKeysTheWrittenKitAlwaysUsed() {
        assertEquals(KitEquipmentSlot.HEAD, KitEquipmentSlot.findByKey("head").orElseThrow());
        assertEquals(KitEquipmentSlot.CHEST, KitEquipmentSlot.findByKey("chest").orElseThrow());
        assertEquals(KitEquipmentSlot.LEGS, KitEquipmentSlot.findByKey("legs").orElseThrow());
        assertEquals(KitEquipmentSlot.FEET, KitEquipmentSlot.findByKey("feet").orElseThrow());
        assertEquals(KitEquipmentSlot.OFF_HAND, KitEquipmentSlot.findByKey("offhand").orElseThrow());
    }

    @Test
    void readsAKeyAnAdminTypedWithCapitalsOrSpaces() {
        assertEquals(KitEquipmentSlot.HEAD, KitEquipmentSlot.findByKey(" Head ").orElseThrow());
    }

    @Test
    void findsNoSlotForAKeyThatIsNoneOfThem() {
        assertTrue(KitEquipmentSlot.findByKey("belt").isEmpty());
        assertTrue(KitEquipmentSlot.findByKey("").isEmpty());
        assertTrue(KitEquipmentSlot.findByKey(null).isEmpty());
    }

    @Test
    void carriesADifferentKeyAndADifferentBukkitSlotForEachOfThem() {
        Set<String> configurationKeys = new HashSet<>();
        Set<EquipmentSlot> equipmentSlots = new HashSet<>();
        for (KitEquipmentSlot slot : KitEquipmentSlot.values()) {
            configurationKeys.add(slot.getConfigurationKey());
            equipmentSlots.add(slot.getEquipmentSlot());
        }

        assertEquals(KitEquipmentSlot.values().length, configurationKeys.size());
        assertEquals(KitEquipmentSlot.values().length, equipmentSlots.size());
    }

    @Test
    void leavesTheMainHandToTheNumberedSlots() {
        for (KitEquipmentSlot slot : KitEquipmentSlot.values()) {
            assertTrue(slot.getEquipmentSlot() != EquipmentSlot.HAND,
                    "the main hand is slot 0 of the inventory and is stored as that slot");
        }
    }
}
