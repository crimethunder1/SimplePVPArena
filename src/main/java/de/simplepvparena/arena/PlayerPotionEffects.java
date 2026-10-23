package de.simplepvparena.arena;

import org.bukkit.entity.Player;

import java.util.List;

public interface PlayerPotionEffects {

    List<SavedPotionEffect> read(Player player);

    void removeAll(Player player);

    void apply(Player player, List<SavedPotionEffect> potionEffects);
}
