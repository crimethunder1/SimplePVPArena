package de.simplepvparena.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record KitSaveResult(int equipmentItemCount, int inventoryItemCount, int unreadableItemCount,
                            String rejectionMessage) {

    public KitSaveResult(int equipmentItemCount, int inventoryItemCount, int unreadableItemCount) {
        this(equipmentItemCount, inventoryItemCount, unreadableItemCount, null);
    }

    public static KitSaveResult of(SerializedKit storedKit, KitDefinition appliedKit) {
        return new KitSaveResult(appliedKit.equipmentItems().size(), appliedKit.inventoryItems().size(),
                Math.max(0, storedKit.itemCount() - appliedKit.itemCount()));
    }

    public static KitSaveResult rejected(String rejectionMessage) {
        return new KitSaveResult(0, 0, 0, Objects.requireNonNull(rejectionMessage, "rejectionMessage"));
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
        lines.add("Kit saved: " + itemCount() + " " + items(itemCount()) + ", " + equipmentItemCount + " worn and "
                + inventoryItemCount + " in the inventory");
        lines.add("It is written into config.yml and is handed out from the next round on, "
                + "a running round keeps the kit its fighters were given");
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
