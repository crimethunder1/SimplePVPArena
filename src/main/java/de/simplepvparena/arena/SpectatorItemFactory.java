package de.simplepvparena.arena;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.SpectatorMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class SpectatorItemFactory implements SpectatorItems {

    private static final Material SPECTATOR_TOGGLE_MATERIAL = Material.COMPASS;
    private static final String MARKER_VALUE = "spectator-toggle";

    private final NamespacedKey markerKey;

    public SpectatorItemFactory(Plugin plugin) {
        this.markerKey = new NamespacedKey(plugin, "spectator-toggle");
    }

    @Override
    public ItemStack createSpectatorToggleItem() {
        return labelled(new ItemStack(SPECTATOR_TOGGLE_MATERIAL));
    }

    ItemStack labelled(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(Messages.text(SpectatorMessage.ITEM_NAME))
                .decoration(TextDecoration.ITALIC, false));
        itemMeta.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, MARKER_VALUE);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @Override
    public boolean isSpectatorToggleItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != SPECTATOR_TOGGLE_MATERIAL || !itemStack.hasItemMeta()) {
            return false;
        }
        return MARKER_VALUE.equals(itemStack.getItemMeta().getPersistentDataContainer()
                .get(markerKey, PersistentDataType.STRING));
    }
}
