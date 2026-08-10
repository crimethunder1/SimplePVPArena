package de.simplepvparena.arena;

import org.bukkit.World;

@FunctionalInterface
public interface CleanedArenaHandler {

    int onArenaCleaned(World world, ArenaShape shape);
}
