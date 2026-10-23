package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaHeightMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ArenaHeightMarkerService {

    public static final Material MARKER_MATERIAL = Material.TARGET;

    public static final int MARKER_AMOUNT = 16;

    public static final String MARKER_KEY_NAME = "arena_height_marker";

    private static final String MARKER_KEY_VALUE = "height";

    private final Plugin plugin;
    private final NamespacedKey markerKey;
    private final ArenaHeightMarkers heightMarkers;

    public ArenaHeightMarkerService(Plugin plugin, ArenaHeightMarkers heightMarkers) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.markerKey = new NamespacedKey(plugin, MARKER_KEY_NAME);
        this.heightMarkers = Objects.requireNonNull(heightMarkers, "heightMarkers");
    }

    public ItemStack createMarkerItem() {
        return labelled(new ItemStack(MARKER_MATERIAL, MARKER_AMOUNT));
    }

    public boolean isMarkerItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != MARKER_MATERIAL || !itemStack.hasItemMeta()) {
            return false;
        }
        return MARKER_KEY_VALUE.equals(itemStack.getItemMeta().getPersistentDataContainer()
                .get(markerKey, PersistentDataType.STRING));
    }

    public void takeMarkersAway(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isMarkerItem(contents[slot])) {
                inventory.setItem(slot, null);
            }
        }
    }

    public void markPlacedBlock(Player admin, Block placedBlock, EquipmentSlot hand) {
        int blockY = placedBlock.getY();
        placedBlock.setType(Material.AIR, false);
        if (markHeight(admin, blockY).hasMovedOn()) {
            relabelAfterThePlacement(admin, hand);
        }
    }

    public ArenaHeightMarkResult markHeight(Player admin, int blockY) {
        ArenaHeightMarkResult result = heightMarkers.mark(blockY);
        if (result.getRejectionMessage().isPresent()) {
            TournamentMessages.sendError(admin, result.getRejectionMessage().get());
            return result;
        }
        TournamentMessages.sendSuccess(admin, Messages.text(
                result.markedEnd().select(ArenaHeightMessage.MARKED_LOWEST, ArenaHeightMessage.MARKED_HIGHEST),
                Placeholder.of("blockY", blockY), Placeholder.of("markers", heightMarkers.describe())));
        TournamentMessages.sendInfo(admin, result.hasMovedOn()
                ? Messages.text(result.nextEnd().select(ArenaHeightMessage.MOVED_ON_TO_LOWEST,
                        ArenaHeightMessage.MOVED_ON_TO_HIGHEST))
                : describeCompletePair());
        return result;
    }

    public ItemStack switchMarkedEnd(Player admin, ItemStack itemStack) {
        ArenaHeightMarkerMode nextEnd = heightMarkers.switchNextEnd();
        TournamentMessages.sendInfo(admin, Messages.text(
                nextEnd.select(ArenaHeightMessage.SWITCHED_TO_LOWEST, ArenaHeightMessage.SWITCHED_TO_HIGHEST),
                Placeholder.of("markers", heightMarkers.describe())));
        relabelAfterThePlacement(admin, EquipmentSlot.HAND);
        return labelled(itemStack);
    }

    private String describeCompletePair() {
        return Messages.text(ArenaHeightMessage.COMPLETE_PAIR,
                Placeholder.of("action", Arena.describeCalculateAction()),
                Placeholder.of("lowestY", heightMarkers.getLowestY().orElseThrow()),
                Placeholder.of("highestY", heightMarkers.getHighestY().orElseThrow()));
    }

    private void relabelAfterThePlacement(Player admin, EquipmentSlot hand) {
        plugin.getServer().getScheduler().runTask(plugin, () -> relabelInInventory(admin, hand));
    }

    private void relabelInInventory(Player admin, EquipmentSlot hand) {
        if (!admin.isOnline()) {
            return;
        }
        PlayerInventory inventory = admin.getInventory();
        if (isMarkerItem(inventory.getItem(hand))) {
            inventory.setItem(hand, labelled(inventory.getItem(hand)));
        }
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isMarkerItem(contents[slot])) {
                inventory.setItem(slot, labelled(contents[slot]));
            }
        }
    }

    public List<String> describeMarkers() {
        List<String> lines = new ArrayList<>();
        lines.add(heightMarkers.describe());
        lines.add(Messages.text(heightMarkers.getNextEnd().select(ArenaHeightMessage.TILE_NEXT_BLOCK_LOWEST,
                ArenaHeightMessage.TILE_NEXT_BLOCK_HIGHEST)));
        lines.add(Messages.text(heightMarkers.isComplete()
                ? ArenaHeightMessage.TILE_COMPLETE
                : ArenaHeightMessage.TILE_INCOMPLETE));
        return lines;
    }

    private ItemStack labelled(ItemStack itemStack) {
        ArenaHeightMarkerMode nextEnd = heightMarkers.getNextEnd();
        ItemStack markerItem = itemStack.clone();
        ItemMeta itemMeta = markerItem.getItemMeta();
        itemMeta.displayName(Component.text(Messages.text(ArenaHeightMessage.ITEM_NAME,
                        Placeholder.of("end", nextEnd.getDisplayName())))
                .decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(Messages.lines(nextEnd.select(ArenaHeightMessage.ITEM_LORE_LOWEST,
                        ArenaHeightMessage.ITEM_LORE_HIGHEST)).stream()
                .map(ArenaHeightMarkerService::lore)
                .toList());
        itemMeta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, MARKER_KEY_VALUE);
        markerItem.setItemMeta(itemMeta);
        return markerItem;
    }

    private static Component lore(String text) {
        return Component.text(text).decoration(TextDecoration.ITALIC, false);
    }
}
