package de.simplepvparena.kit;

import de.simplepvparena.message.KitMessage;
import de.simplepvparena.message.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Objects;
import java.util.Optional;

public final class KitSaveService {

    private final KitLibrary kitLibrary;
    private final KitSerialization kitSerialization;

    public KitSaveService(KitLibrary kitLibrary, KitItemSerializer kitItemSerializer) {
        this.kitLibrary = Objects.requireNonNull(kitLibrary, "kitLibrary");
        this.kitSerialization = new KitSerialization(kitItemSerializer);
    }

    public static String describeEmptyInventoryReplacement() {
        return Messages.text(KitMessage.EMPTY_INVENTORY_REPLACEMENT);
    }

    public KitSaveResult createFromInventory(Player player, String kitName) {
        Optional<String> rejection = kitLibrary.findCreationRejection(kitName);
        if (rejection.isPresent()) {
            return KitSaveResult.rejected(rejection.get());
        }
        if (holdsNoItem(player)) {
            return KitSaveResult.rejected(Messages.text(KitMessage.EMPTY_INVENTORY_CREATION));
        }
        return describe(kitLibrary.createKit(kitName, capture(player.getInventory())));
    }

    public KitSaveResult replaceFromInventory(Player player, String kitName) {
        if (kitLibrary.findKit(kitName).isEmpty()) {
            return KitSaveResult.rejected(KitLibrary.describeUnknownKit(kitName));
        }
        if (holdsNoItem(player)) {
            return KitSaveResult.rejected(describeEmptyInventoryReplacement());
        }
        return describe(kitLibrary.replaceKit(kitName, capture(player.getInventory())));
    }

    public boolean holdsNoItem(Player player) {
        PlayerInventory inventory = player.getInventory();
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

    private SerializedKit capture(PlayerInventory inventory) {
        return KitInventoryCapture.capture(
                equipmentSlot -> kitSerialization.encode(inventory.getItem(equipmentSlot.getEquipmentSlot())),
                slotIndex -> kitSerialization.encode(inventory.getItem(slotIndex)));
    }

    private KitSaveResult describe(LibraryKit kit) {
        return KitSaveResult.of(kit.name(), kitLibrary.isDefaultKit(kit.name()), kit.serializedKit(),
                kit.kitDefinition());
    }

    private static boolean isItem(ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty();
    }
}
