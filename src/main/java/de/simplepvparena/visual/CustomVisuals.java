package de.simplepvparena.visual;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public interface CustomVisuals {

    Optional<String> findFontImage(String identifier);

    Optional<ItemStack> findItem(String identifier);

    boolean playTotemAnimation(Player player, String identifier);

    boolean isReady();
}
