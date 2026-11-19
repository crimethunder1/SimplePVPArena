package de.simplepvparena.arena;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface EffectAreaPresence {

    void reconcile(Player player, Location location);

    default void reconcile(Player player) {
        reconcile(player, player.getLocation());
    }
}
