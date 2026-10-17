package de.simplepvparena.kit;

import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitSerializationTest {

    private final KitSerialization kitSerialization = new KitSerialization(new UnstorableLabelSerializer());
    private final List<String> problems = new ArrayList<>();

    KitSerializationTest() {
        SharedBukkitServer.install();
    }

    @Test
    void storesEveryItemOfAKitDefinitionInItsSlot() {
        KitDefinition kitDefinition = new KitDefinition(
                Map.of(KitEquipmentSlot.HEAD, new EncodableItemStack("helmet"),
                        KitEquipmentSlot.OFF_HAND, new EncodableItemStack("shield")),
                Map.of(0, new EncodableItemStack("sword"), 1, new EncodableItemStack("bow")));

        SerializedKit serializedKit = kitSerialization.toSerializedKit(kitDefinition, problems::add);

        assertEquals(FakeSerializedKits.wearingAndCarrying(Map.of(KitEquipmentSlot.HEAD, "helmet",
                KitEquipmentSlot.OFF_HAND, "shield"), "sword", "bow"), serializedKit);
        assertEquals(List.of(), problems);
    }

    @Test
    void reportsAnItemThatCannotBeStoredAndLeavesItOut() {
        KitDefinition kitDefinition = new KitDefinition(
                Map.of(KitEquipmentSlot.HEAD, new EncodableItemStack("unstorable helmet")),
                Map.of(0, new EncodableItemStack("sword"), 1, new EncodableItemStack("unstorable bow")));

        SerializedKit serializedKit = kitSerialization.toSerializedKit(kitDefinition, problems::add);

        assertEquals(FakeSerializedKits.carrying("sword"), serializedKit);
        assertEquals(List.of("The kit item of the slot head could not be stored and is left out of the kit",
                "The kit item of the inventory slot 1 could not be stored and is left out of the kit"), problems);
    }

    private static final class UnstorableLabelSerializer implements KitItemSerializer {

        private final FakeKitItemSerializer fakeKitItemSerializer = new FakeKitItemSerializer();

        @Override
        public byte[] encode(ItemStack itemStack) {
            if (((EncodableItemStack) itemStack).getLabel().startsWith("unstorable")) {
                throw new IllegalStateException("unstorable item");
            }
            return fakeKitItemSerializer.encode(itemStack);
        }

        @Override
        public ItemStack decode(byte[] itemBytes) {
            return fakeKitItemSerializer.decode(itemBytes);
        }
    }
}
