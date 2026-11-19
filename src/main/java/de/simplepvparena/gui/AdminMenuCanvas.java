package de.simplepvparena.gui;

import org.jspecify.annotations.NullMarked;

@NullMarked
interface AdminMenuCanvas {

    void set(int slot, MenuTile tile);

    void clear();
}
