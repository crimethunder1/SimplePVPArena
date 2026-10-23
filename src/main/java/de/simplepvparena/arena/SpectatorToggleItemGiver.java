package de.simplepvparena.arena;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface SpectatorToggleItemGiver {

    void giveSpectatorToggleItemIfMissing(Player player);
}
