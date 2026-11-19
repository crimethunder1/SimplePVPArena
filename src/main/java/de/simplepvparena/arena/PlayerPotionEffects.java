package de.simplepvparena.arena;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public interface PlayerPotionEffects {

    List<SavedPotionEffect> read(Player player);

    void removeAll(Player player);

    void apply(Player player, List<SavedPotionEffect> potionEffects);
}
