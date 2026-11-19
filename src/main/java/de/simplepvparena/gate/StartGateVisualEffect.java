package de.simplepvparena.gate;

import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface StartGateVisualEffect {

    StartGateVisualEffect NONE = (world, encodedPositions) -> { };

    void showRemovedLayer(World world, long[] encodedPositions);
}
