package de.simplepvparena.arena;

import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface BorderColumnReader {

    int[] NO_BORDER_BLOCKS = new int[0];

    int[] readBorderHeights(int blockX, int blockZ);
}
