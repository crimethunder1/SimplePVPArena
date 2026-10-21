package de.simplepvparena.match;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DeathProtection;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class PaperFightStartTotems implements FightStartTotems {

    public static final String MARKER_KEY_NAME = "fight_start_totem";

    private static final byte MARKER_VALUE = 1;
    private static final int TOTEM_AMOUNT = 1;

    private final NamespacedKey markerKey;

    public PaperFightStartTotems(Plugin plugin) {
        this.markerKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), MARKER_KEY_NAME);
    }

    @Override
    public ItemStack createTotem(ItemStack customItem) {
        ItemStack totem = customItem.clone();
        totem.setAmount(TOTEM_AMOUNT);
        totem.setData(DataComponentTypes.DEATH_PROTECTION, DeathProtection.deathProtection());
        totem.editPersistentDataContainer(container -> container.set(markerKey, PersistentDataType.BYTE,
                MARKER_VALUE));
        return totem;
    }

    @Override
    public boolean isTotem(ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty() && itemStack.getPersistentDataContainer().has(markerKey);
    }
}
