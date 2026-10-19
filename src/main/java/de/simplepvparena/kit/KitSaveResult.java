package de.simplepvparena.kit;

import de.simplepvparena.message.KitMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;

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
        lines.add(Messages.plural(KitMessage.SAVED, itemCount(), Placeholder.of("kit", kitName),
                Placeholder.of("wornCount", equipmentItemCount), Placeholder.of("inventoryCount", inventoryItemCount)));
        Placeholder file = Placeholder.of("file", FileKitLibraryStorage.FILE_NAME);
        lines.add(defaultKit
                ? Messages.text(KitMessage.SAVED_DEFAULT_KIT_WRITTEN, file)
                : Messages.text(KitMessage.SAVED_OTHER_KIT_WRITTEN, file));
        if (unreadableItemCount > 0) {
            lines.add(Messages.plural(KitMessage.UNREADABLE_ITEMS, unreadableItemCount));
        }
        return lines;
    }
}
