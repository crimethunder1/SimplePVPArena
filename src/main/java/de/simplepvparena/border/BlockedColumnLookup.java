package de.simplepvparena.border;

import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface BlockedColumnLookup {

    BlockedColumnLookup NOTHING_BLOCKED = (blockX, blockZ) -> false;

    boolean isBlockedColumn(int blockX, int blockZ);
}
