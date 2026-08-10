package de.simplepvparena.arena;

import org.bukkit.World;

@FunctionalInterface
public interface CleanedRegionHandler {

    int onRegionCleaned(World world, RegionBoundary region);
}
