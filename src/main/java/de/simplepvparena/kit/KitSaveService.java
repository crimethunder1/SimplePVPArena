package de.simplepvparena.kit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class KitSaveService {

    private final KitService kitService;
    private final Consumer<SerializedKit> kitConfigurationSaver;
    private final Consumer<String> problemReporter;

    public KitSaveService(KitService kitService, Consumer<SerializedKit> kitConfigurationSaver,
                          Consumer<String> problemReporter) {
        this.kitService = Objects.requireNonNull(kitService, "kitService");
        this.kitConfigurationSaver = Objects.requireNonNull(kitConfigurationSaver, "kitConfigurationSaver");
        this.problemReporter = Objects.requireNonNull(problemReporter, "problemReporter");
    }

    public KitSaveResult saveFromInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        SerializedKit serializedKit = KitInventoryCapture.capture(
                equipmentSlot -> KitSerialization.encode(inventory.getItem(equipmentSlot.getEquipmentSlot())),
                slotIndex -> KitSerialization.encode(inventory.getItem(slotIndex)));
        kitConfigurationSaver.accept(serializedKit);
        KitDefinition kitDefinition = KitSerialization.toDefinition(serializedKit, problemReporter);
        kitService.setKitDefinition(kitDefinition);
        return KitSaveResult.of(serializedKit, kitDefinition);
    }

    public List<String> describeKit() {
        return kitService.getKitDefinition().describe();
    }
}
