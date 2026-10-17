package de.simplepvparena.kit;

import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitConfigurationLoaderTest {

    private final List<String> problems = new ArrayList<>();
    private final KitConfigurationLoader kitConfigurationLoader = new KitConfigurationLoader(problems::add,
            new FakeKitItemSerializer(), KitConfigurationLoaderTest::createItemStackLikeTheServer);

    KitConfigurationLoaderTest() {
        SharedBukkitServer.install();
    }

    @Test
    void reportsEveryUnusableItemAndKeepsTheReadableItems() throws InvalidConfigurationException {
        YamlConfiguration configuration = configurationOf("""
                kit:
                  armor:
                    head:
                      material: DIAMOND_HELMET
                    chest:
                      material: LAVA
                  items:
                    - material: DIAMOND_SWORD
                    - material: WATER
                    - material: ARROW
                      amount: 0
                    - material: GOLDEN_APPLE
                      amount: 100
                    - material: BOW
                """);

        SerializedKit serializedKit = kitConfigurationLoader.loadSerializedKit(
                configuration.getConfigurationSection("kit"));

        assertEquals(FakeSerializedKits.wearingAndCarrying(Map.of(KitEquipmentSlot.HEAD, "DIAMOND_HELMET x1"),
                "DIAMOND_SWORD x1", "BOW x1"), serializedKit);
        assertEquals(List.of("Unusable item LAVA in kit configuration: IllegalStateException",
                "Unusable item WATER in kit configuration: WATER isn't an item",
                "Unusable item ARROW in kit configuration: the amount 0 is not between 1 and 99",
                "Unusable item GOLDEN_APPLE in kit configuration: the amount 100 is not between 1 and 99"),
                problems);
    }

    @Test
    void yieldsAnEmptyKitWhenEveryItemIsUnusable() throws InvalidConfigurationException {
        YamlConfiguration configuration = configurationOf("""
                kit:
                  armor:
                    feet:
                      material: WATER
                  items:
                    - material: WATER
                    - material: DIAMOND_SWORD
                      amount: -3
                """);

        SerializedKit serializedKit = kitConfigurationLoader.loadSerializedKit(
                configuration.getConfigurationSection("kit"));

        assertEquals(SerializedKit.empty(), serializedKit);
        assertEquals(List.of("Unusable item WATER in kit configuration: WATER isn't an item",
                "Unusable item WATER in kit configuration: WATER isn't an item",
                "Unusable item DIAMOND_SWORD in kit configuration: the amount -3 is not between 1 and 99"),
                problems);
    }

    private static ItemStack createItemStackLikeTheServer(KitItem kitItem) {
        if (kitItem.material() == Material.WATER) {
            throw new IllegalArgumentException("WATER isn't an item");
        }
        if (kitItem.material() == Material.LAVA) {
            throw new IllegalStateException();
        }
        if (kitItem.amount() < 1) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return new EncodableItemStack(kitItem.material().name() + " x" + kitItem.amount());
    }

    private static YamlConfiguration configurationOf(String contents) throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(contents);
        return configuration;
    }
}
