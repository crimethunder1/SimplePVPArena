package de.simplepvparena.border;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface BorderVisualEffect {

    void showAt(Player player, Location borderBlockCenter);

    void showConnection(Player player, Location pointBetweenBorderBlocks);
}
