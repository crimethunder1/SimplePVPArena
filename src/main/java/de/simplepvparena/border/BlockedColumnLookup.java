package de.simplepvparena.border;

@FunctionalInterface
public interface BlockedColumnLookup {

    BlockedColumnLookup NOTHING_BLOCKED = (blockX, blockZ) -> false;

    boolean isBlockedColumn(int blockX, int blockZ);
}
