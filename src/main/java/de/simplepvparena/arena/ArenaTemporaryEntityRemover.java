package de.simplepvparena.arena;

import org.bukkit.World;

public interface ArenaTemporaryEntityRemover {

    ArenaTemporaryEntityRemover NONE = (world, shape) -> 0;

    int removeTemporaryEntities(World world, ArenaShape shape);
}
