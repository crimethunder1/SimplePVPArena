package de.simplepvparena.arena;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface SpectatorToggleItemGiver {

    void giveSpectatorToggleItemIfMissing(Player player);
}
