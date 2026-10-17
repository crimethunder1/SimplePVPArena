package de.simplepvparena.kit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class KitSaveService {

    private static final String EMPTY_INVENTORY_MESSAGE = "Your inventory is empty, the kit was not changed";

    private final KitService kitService;
    private final Consumer<SerializedKit> kitConfigurationSaver;
    private final Consumer<String> problemReporter;
    private final KitSerialization kitSerialization;

    public KitSaveService(KitService kitService, Consumer<SerializedKit> kitConfigurationSaver,
                          Consumer<String> problemReporter, KitItemSerializer kitItemSerializer) {
        this.kitService = Objects.requireNonNull(kitService, "kitService");
        this.kitConfigurationSaver = Objects.requireNonNull(kitConfigurationSaver, "kitConfigurationSaver");
        this.problemReporter = Objects.requireNonNull(problemReporter, "problemReporter");
        this.kitSerialization = new KitSerialization(kitItemSerializer);
    }

    public KitSaveResult saveFromInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (holdsNoItem(inventory)) {
            return KitSaveResult.rejected(EMPTY_INVENTORY_MESSAGE);
        }
        SerializedKit serializedKit = KitInventoryCapture.capture(
                equipmentSlot -> kitSerialization.encode(inventory.getItem(equipmentSlot.getEquipmentSlot())),
                slotIndex -> kitSerialization.encode(inventory.getItem(slotIndex)));
        kitConfigurationSaver.accept(serializedKit);
        KitDefinition kitDefinition = kitSerialization.toDefinition(serializedKit, problemReporter);
        kitService.setKitDefinition(kitDefinition);
        return KitSaveResult.of(serializedKit, kitDefinition);
    }

    public List<String> describeKit() {
        return kitService.getKitDefinition().describe();
    }

    private static boolean holdsNoItem(PlayerInventory inventory) {
        for (KitEquipmentSlot equipmentSlot : KitEquipmentSlot.values()) {
            if (isItem(inventory.getItem(equipmentSlot.getEquipmentSlot()))) {
                return false;
            }
        }
        for (int slotIndex = 0; slotIndex < SerializedKit.INVENTORY_SLOT_COUNT; slotIndex++) {
            if (isItem(inventory.getItem(slotIndex))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isItem(ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty();
    }
}
