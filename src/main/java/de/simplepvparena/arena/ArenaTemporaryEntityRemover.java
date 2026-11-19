package de.simplepvparena.arena;

import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ArenaTemporaryEntityRemover {

    ArenaTemporaryEntityRemover NONE = (world, shape) -> 0;

    int removeTemporaryEntities(World world, ArenaShape shape);
}
