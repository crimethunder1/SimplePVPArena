package de.simplepvparena.arena;

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
        TournamentMessages.sendSuccess(admin, "The " + result.markedEnd().getDisplayName()
                + " of the fight area is marked at y " + blockY
                + TournamentMessages.LINE_BREAK + heightMarkers.describe());
        TournamentMessages.sendInfo(admin, result.hasMovedOn()
                ? "The marker now marks the " + result.nextEnd().getDisplayName()
                        + ", it moved on by itself. Place a block with it at that height as well"
                        + TournamentMessages.LINE_BREAK + "A single mark changes nothing about a calculation"
                : describeCompletePair());
        return result;
    }

    public ItemStack switchMarkedEnd(Player admin, ItemStack itemStack) {
        ArenaHeightMarkerMode nextEnd = heightMarkers.switchNextEnd();
        TournamentMessages.sendInfo(admin, "The marker now marks the " + nextEnd.getDisplayName()
                + " of the fight area"
                + TournamentMessages.LINE_BREAK + "Place a block with it at that height, it becomes "
                + nextEnd.getMeaning() + " and disappears again"
                + TournamentMessages.LINE_BREAK + heightMarkers.describe());
        relabelAfterThePlacement(admin, EquipmentSlot.HAND);
        return labelled(itemStack);
    }

    private String describeCompletePair() {
        return "Both heights are set. The next " + Arena.CALCULATE_ACTION + " takes y "
                + heightMarkers.getLowestY().orElseThrow() + " and y "
                + heightMarkers.getHighestY().orElseThrow() + " over as the floor and the ceiling of the "
                + "fight area instead of the height of the border wall"
                + TournamentMessages.LINE_BREAK + "They are used up by that calculation and are gone "
                + "when the server restarts";
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
        lines.add("The next placed block marks the "
                + heightMarkers.getNextEnd().getDisplayName());
        lines.add(heightMarkers.isComplete()
                ? "The next calculation takes them as floor and ceiling"
                : "Both ends have to be marked, a single mark is not used");
        return lines;
    }

    private ItemStack labelled(ItemStack itemStack) {
        ArenaHeightMarkerMode nextEnd = heightMarkers.getNextEnd();
        ItemStack markerItem = itemStack.clone();
        ItemMeta itemMeta = markerItem.getItemMeta();
        itemMeta.displayName(Component.text("Height marker: " + nextEnd.getDisplayName())
                .decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(List.of(
                lore("Marks " + nextEnd.getMeaning()),
                lore("Place it at that height, the block disappears again"),
                lore("It moves on to the " + nextEnd.toggle().getDisplayName()
                        + " by itself once this end is marked"),
                lore("Right click in the air marks an end again"),
                lore("Which end is marked is kept by the plugin, this"),
                lore("name only shows it - both ends are taken over by"),
                lore("the next calculation")));
        itemMeta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, MARKER_KEY_VALUE);
        markerItem.setItemMeta(itemMeta);
        return markerItem;
    }

    private static Component lore(String text) {
        return Component.text(text).decoration(TextDecoration.ITALIC, false);
    }
}
