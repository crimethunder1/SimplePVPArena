package de.simplepvparena.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record KitSaveResult(String kitName, boolean defaultKit, int equipmentItemCount, int inventoryItemCount,
                            int unreadableItemCount, String rejectionMessage) {

    public KitSaveResult(String kitName, boolean defaultKit, int equipmentItemCount, int inventoryItemCount,
                         int unreadableItemCount) {
        this(Objects.requireNonNull(kitName, "kitName"), defaultKit, equipmentItemCount, inventoryItemCount,
                unreadableItemCount, null);
    }

    public static KitSaveResult of(String kitName, boolean defaultKit, SerializedKit storedKit,
                                   KitDefinition appliedKit) {
        return new KitSaveResult(kitName, defaultKit, appliedKit.equipmentItems().size(),
                appliedKit.inventoryItems().size(), Math.max(0, storedKit.itemCount() - appliedKit.itemCount()));
    }

    public static KitSaveResult rejected(String rejectionMessage) {
        return new KitSaveResult(null, false, 0, 0, 0, Objects.requireNonNull(rejectionMessage, "rejectionMessage"));
    }

    public boolean isSaved() {
        return rejectionMessage == null;
    }

    public int itemCount() {
        return equipmentItemCount + inventoryItemCount;
    }

    public List<String> describe() {
        if (!isSaved()) {
            return List.of(rejectionMessage);
        }
        List<String> lines = new ArrayList<>();
        lines.add("Kit " + kitName + " saved: " + itemCount() + " " + items(itemCount()) + ", " + equipmentItemCount
                + " worn and " + inventoryItemCount + " in the inventory");
        if (defaultKit) {
            lines.add("It is written into " + FileKitLibraryStorage.FILE_NAME + " and is handed out from the next "
                    + "round on, a running round keeps the kit its fighters were given");
        } else {
            lines.add("It is written into " + FileKitLibraryStorage.FILE_NAME + " and is handed out once it is "
                    + "used as the default kit");
        }
        if (unreadableItemCount > 0) {
            lines.add(unreadableItemCount + " " + items(unreadableItemCount)
                    + " could not be read back and are no part of the kit, the log names them");
        }
        return lines;
    }

    private static String items(int itemCount) {
        return itemCount == 1 ? "item" : "items";
    }
}
