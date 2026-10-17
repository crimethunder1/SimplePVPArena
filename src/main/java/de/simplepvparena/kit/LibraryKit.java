package de.simplepvparena.kit;

import java.util.Objects;

public record LibraryKit(String name, SerializedKit serializedKit, KitDefinition kitDefinition) {

    public LibraryKit {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(serializedKit, "serializedKit");
        Objects.requireNonNull(kitDefinition, "kitDefinition");
    }

    public int wornItemCount() {
        return kitDefinition.equipmentItems().size();
    }

    public int inventoryItemCount() {
        return kitDefinition.inventoryItems().size();
    }

    public int itemCount() {
        return kitDefinition.itemCount();
    }
}
