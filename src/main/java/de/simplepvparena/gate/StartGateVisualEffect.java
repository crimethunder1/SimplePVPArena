package de.simplepvparena.gate;

import org.bukkit.World;

public interface StartGateVisualEffect {

    StartGateVisualEffect NONE = (world, encodedPositions) -> { };

    void showRemovedLayer(World world, long[] encodedPositions);
}
