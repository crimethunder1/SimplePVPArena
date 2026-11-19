package de.simplepvparena.arena;

import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface CleanedArenaHandler {

    int onArenaCleaned(World world, ArenaShape shape);
}
